package com.github.jacktasia.javarepl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.github.jacktasia.javarepl.Repl.SliceMode;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

/** CodeFile is our abstraction for the temporary java file running the REPL code */
public final class CodeFile {

	private final static Logger logger = Logger.getLogger(CodeFile.class.getName());

	/** List of classpaths and jars to use for compiling/running. */
	private final List<String> cmdClassPaths = new LinkedList<String>();

	/** Trial import lines (compiled/run successfully moves to validImport). */
	private List<String> trialImport = new LinkedList<String>();

	/** Valid import lines (validated through trial process). */
	private final List<String> validImport = new LinkedList<String>();

	/** Trial code lines (compiled/run successfully moves to validCode). */
	private List<String> trialCode = new LinkedList<String>();

	/** Valid code lines (validated through trial process). */
	private final List<String> validCode = new LinkedList<String>();

	/** Lines of code to only be run once. */
	private List<String> onceCode = new LinkedList<String>();

	/** Code line number index for insert or replace command. */
	private int trialSliceIndex = -1;

	/** The new line of code for the insert or replace command. */
	private String trialSliceCode;

	/** The line of code that will be replaced if successfully compiled. */
	private String trialReplaceLoser;

	/**
	 * Generated java code to get the auto toString functionality. This is when you put in a var by itself and see its
	 * contents
	 */
	private String autoStringCode;

	/** Path/command for java compiler (probably javac unless windows). */
	private String javaCompilerCmd = "javac";

	/** What slice mode are we in? (i or r commands). */
	private SliceMode currentMode = SliceMode.NONE;

	/** Tmp java file class name. */
	private final String tmpClassName = "ReplTmpInstance";

	/** Directory containing tmpCompileFileName. */
	private String tmpCompileDir;

	/** Full path of tmp code file. */
	private String tmpCompilePath;

	/** Filename of temp code file (tmpClassName + .java). */
	private String tmpCompileFilename;

	/** Regex for matching an import line. */
	private final String importLineMatchRegex = "^import ([a-zA-z0-9\\.]+\\*?);";

	/**
	 * Create CodeFile instance with an auto-creted tmp file.
	 */
	public CodeFile() {
		setupFile(); // TODO should probably be setupTempFile...
	}

	/**
	 * Create CodeFile instance with a set file.
	 * 
	 * @param f
	 *            file to use for generating code.
	 */
	public CodeFile(final File f) {

	}

	/**
	 * Remove tmp directory holding the tmp code file.
	 */
	public void clearFileOnExit() {
		final String killDir = tmpCompileDir;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {

				try {
					Command.deletePath(new File(killDir));
				} catch (IOException e) {
					logger.log(Level.WARNING, "Could NOT delete temp directory: " + killDir, e);
				}

				System.out.println("\n\nBye.\n");
			}
		});
	}

	/**
	 * Add a line of code on a trial basis. code line will be moved to validCode if succesfully compiled/run
	 * 
	 * @param line
	 *            code line
	 */
	public void addTrialCode(final String line) {
		trialCode.add(line);
	}

	/**
	 * Set a line of code to only run once, even if successful.
	 * 
	 * @param line
	 *            code line
	 */
	public void addOnceCode(final String line) {
		onceCode.add(line);
	}

	/**
	 * Chanage a negative line number to a proper index. If we have 10 lines of code, and this is passed -2 we get 7
	 * (not 8, 0-based index). Any 0+ num is returned as-is
	 * 
	 * @param i
	 *            line number
	 * @return code line index to use
	 */
	private int changeNegative(final int i) {
		if (i < 0) {
			return (validCode.size() - (-1 * i)) + 1;
		}

		return i;
	}

	/**
	 * Remove valid code line by index.
	 * 
	 * @param i
	 *            line index
	 * @return true if line removed.
	 */
	public boolean removeValidCodeLine(final int i) {
		int processedIndex = changeNegative(i);
		if (validCode.size() > processedIndex) {
			validCode.remove(processedIndex);
			return true;
		}

		return false;
	}

	/**
	 * Add a trial insert line, will be valid if compiles/runs w/o errors. example: e.g.
	 * i:1:System.out.println("this is new line");
	 * 
	 * @param i
	 *            line index
	 * @param line
	 *            code code
	 */
	public void addInsertLine(final int i, final String line) {
		currentMode = SliceMode.INSERT;
		trialSliceIndex = changeNegative(i);
		trialSliceCode = line;
	}

	/**
	 * Add a trial replace line, will be valid if compiles/runs w/o errors. example: e.g.
	 * r:1:System.out.println("this replaces first line");
	 * 
	 * @param i
	 *            line index
	 * @param line
	 *            code
	 */
	public void addReplaceLine(final int i, final String line) {
		currentMode = SliceMode.REPLACE;
		trialSliceIndex = changeNegative(i);
		trialSliceCode = line;
	}

	/**
	 * Add a trial import line, will be valid if compiles w/o errors.
	 * 
	 * @param line
	 *            potential import command
	 * @return true if import matches regex and added.
	 */
	public boolean addTrialImport(final String line) {
		if (isImportLine(line)) {
			trialImport.add(line);
			return true;
		}

		return false;
	}

	/**
	 * Verifies a string looks like an import line.
	 * 
	 * @param line
	 *            potential import command
	 * @return true if import matches regex.
	 */
	public boolean isImportLine(final String line) {
		String match;

		if (line.trim().length() > 0) {
			match = StringUtil.getMatch(importLineMatchRegex, line);
			if (match.length() > 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Store class path for use if path exists.
	 * 
	 * @param line
	 *            path to add to class path
	 * @return true if class path exists and added
	 */
	public boolean addClassPath(final String line) {
		File f = new File(line);

		if (f.exists()) {
			cmdClassPaths.add(line);
			return true;
		}

		return false;
	}

	/**
	 * Setup tmp files and and java compiler.
	 * 
	 */
	private void setupFile() {
		tmpCompileDir = Files.createTempDir().getPath();
		tmpCompileFilename = tmpClassName + ".java";
		tmpCompilePath = tmpCompileDir + File.separator + tmpCompileFilename;
		cmdClassPaths.add(tmpCompileDir);
		setupJavaCompiler();
	}

	/**
	 * Search for javac if we're on windows and can't find one.
	 * 
	 */
	private void setupJavaCompiler() {
		boolean foundJavaCompiler = Command.testJavaCompiler(javaCompilerCmd);
		String cmd;

		if (!foundJavaCompiler && Command.isWindows()) {
			cmd = Command.winFindJavaCompiler();
			if (Command.testJavaCompiler(cmd)) {
				javaCompilerCmd = cmd;
				foundJavaCompiler = true;
			}
		}

		if (!foundJavaCompiler) {
			System.out.println("\nCOUND NOT FIND JAVA COMPILER\n");
			System.exit(0);
		}
	}

	/**
	 * Handle compile failure by resetting code vars.
	 * 
	 */
	private void handleCompileFailure() {
		trialImport = new LinkedList<String>();
		trialCode = new LinkedList<String>();
		onceCode = new LinkedList<String>();

		if (currentMode == SliceMode.INSERT) {
			validCode.remove(trialSliceIndex);
		} else if (currentMode == SliceMode.REPLACE) {
			validCode.set(trialSliceIndex, trialReplaceLoser);
		}

		currentMode = SliceMode.NONE;
	}

	/**
	 * Handle compile success by validating code and resetting.
	 * 
	 */
	private void handleCompileSuccess() {

		validImport.addAll(trialImport);
		trialImport = new LinkedList<String>();

		validCode.addAll(trialCode);
		trialCode = new LinkedList<String>();

		onceCode = new LinkedList<String>();

		currentMode = SliceMode.NONE;
	}

	/**
	 * Executes and generates code.
	 * 
	 */
	public void generateCompileAndRun() {
		try {
			generateCode();
			compileAndRun();
		} catch (IOException e) {
			logger.log(Level.WARNING, "CodeFile execute error", e);
			System.out.println("ERROR OUT!");
		}
	}

	/**
	 * Get list of the valid code lines. Used for writing code to screen iva command.
	 * 
	 * @return list of the valid code lines
	 */
	public List<String> getValidCode() {
		return validCode;
	}

	/**
	 * Generate code and write to tmp file.
	 * 
	 * @throws IOException
	 *             when template file isn't found.
	 */
	public void generateCode() throws IOException {

		String lastLine;
		String lastLineChar;
		List<String> runImport = new LinkedList<String>();
		LinkedList<String> runCode = new LinkedList<String>();

		runImport.addAll(validImport);
		runImport.addAll(trialImport);

		autoStringCode = "";
		if (currentMode == SliceMode.INSERT) {
			validCode.add(trialSliceIndex, trialSliceCode);
		} else if (currentMode == SliceMode.REPLACE) {
			validCode.set(trialSliceIndex, trialSliceCode);
			trialReplaceLoser = validCode.get(trialSliceIndex);
		}

		runCode.addAll(validCode);
		runCode.addAll(trialCode);
		runCode.addAll(onceCode);

		// DEBUG
		// System.out.println("MODE: " + this.currentMode);
		// System.out.println(runCode.toString());

		// hack-y handle of last line to autoString if just var...
		if (runCode.size() > 0) {
			lastLine = runCode.get(runCode.size() - 1);
			lastLineChar = lastLine.substring(lastLine.length() - 1);
			if (!lastLineChar.equals(";") && !lastLineChar.equals("}") && lastLine.indexOf(" ") == -1) {

				runCode.removeLast();
				if (trialCode.size() > 0) {
					trialCode.remove(trialCode.size() - 1);
				}

				autoStringCode = "outputToString(" + lastLine + ");";
				runCode.add(autoStringCode);
			}
		}

		// System.out.println(runCode.toString()); // DEBUG

		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

		ve.init();

		final String templatePath = "repl.vm";
		InputStream input = Repl.class.getClassLoader().getResourceAsStream(templatePath);

		if (input == null) {
			throw new IOException("Template file doesn't exist");
		}

		VelocityContext context = new VelocityContext();
		StringWriter writer = new StringWriter();

		Template template = ve.getTemplate(templatePath, "UTF-8");

		context.put("importLines", runImport);
		context.put("codeLines", runCode);

		template.merge(context, writer);

		// System.out.println(writer.toString()); // DEBUG: print out generated out

		try {
			Files.write(writer.toString(), new File(tmpCompilePath), Charsets.UTF_8);
		} catch (IOException e) {
			logger.log(Level.WARNING, "write generated code to file error", e);
		}
	}

	/**
	 * Filter and clean error output for better display.
	 * 
	 * @param output
	 *            Raw output/result text
	 * @return Filtered error output
	 */
	public String cleanErrorOutput(final String output) {
		String result = output.replace("location: class ReplTmpInstance", "");
		result = result.replace(tmpCompilePath + ":", "");
		result = result.replace("\n\n", " ");
		result = result.replace("\t", " ");
		result = result.replace(autoStringCode, "");
		result = result.replaceAll("^([0-9]+:\\s*)", "");
		return result;
	}

	/**
	 * Try to compile and run the java code.
	 */
	public void compileAndRun() {
		String classPathStr = generateClassPathArg(cmdClassPaths);
		String[] compileCommand = new String[] { javaCompilerCmd, "-cp", classPathStr, tmpCompilePath };
		String[] runCommand = new String[] { "java", "-cp", classPathStr, tmpClassName };
		Command compileResult = Command.run(compileCommand);
		Command runResult = Command.run(runCommand);

		String result;
		Boolean successfulCompile = false;

		if (!compileResult.isSuccess()) { // || cl.getResult().length() > 0
			outputError("Compile Error", cleanErrorOutput(compileResult.getResult()));
		} else {
			if (!runResult.isSuccess()) {
				outputError("Run Error", runResult.getResult());
			} else {
				result = runResult.getResult();
				if (result.length() > 0) {
					System.out.println(result);
				}
				successfulCompile = true;
				handleCompileSuccess();
			}
		}

		if (!successfulCompile) {
			handleCompileFailure();
		}
	}

	/**
	 * Generate OS specific class path arg for java commands.
	 * 
	 * @param paths
	 *            list of classpaths
	 * @return javac class path arg for host OS
	 */
	public static String generateClassPathArg(final List<String> paths) {
		return Joiner.on(System.getProperty("path.separator")).join(paths);
	}

	/**
	 * Output Error from compile/run.
	 * 
	 * @param title
	 *            title of error
	 * @param content
	 *            error message
	 */
	private static void outputError(final String title, final String content) {
		Command.outputTitle(title);
		System.out.println(content);
		Command.outputBar();
	}
}
