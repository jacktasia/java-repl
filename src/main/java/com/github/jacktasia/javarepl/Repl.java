package com.github.jacktasia.javarepl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.ConsoleReader;
import jline.History;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.Files;

/** Main class for the JavaRepl. */
public final class Repl {

	private final static Logger logger = Logger.getLogger(Repl.class.getName());

	/** Type of "colon"/slice cmds (i or r). */
	public enum SliceMode {
		/** Not a colon command. */
		NONE,
		/** i command. */
		INSERT,
		/** r command. */
		REPLACE
	}

	/** history storage filename. */
	private final String historyFilename = ".javarepl_history";

	/** history storage full path to file.. */
	private final String historyFilePath = System.getProperty("user.home") + File.separator + historyFilename;

	/** History cmds from last session. */
	private final List<String> oldHistory = new LinkedList<String>();

	/** code file instance for compiling/running code. */
	private final CodeFile code = new CodeFile();

	/** Are we in multiline mode. */
	private boolean multiLineMode = false;

	/** Current code in mutiline mode. */
	private String multiLineCode = "";

	/** path to default repl config file. */
	private final static String defaultConfigName = System.getProperty("user.home") + File.separator + ".javarepl";

	/** reader for jline prompt. */
	private ConsoleReader reader;

	/**
	 * Repl file instance with default repl file.
	 * 
	 */
	public Repl() {
		// outputWelcomeTitle();
		// loadConfigFile(this.defaultConfigName);
		this(defaultConfigName);
	}

	/**
	 * Repl file instance with non-default repl file.
	 * 
	 * @param replFileName
	 *            path to JavaRepl file
	 */
	public Repl(final String replFileName) {
		File replFile = new File(replFileName);

		outputWelcomeTitle();
		if (replFile.exists()) {
			loadConfigFile(replFileName);
		} else {
			Command.outputTitle("Error " + replFileName + " NOT FOUND. "
					+ (replFileName.endsWith(".javarepl") ? "" : "Falling back to .javarepl attempt."));
			// fallback to default
			loadConfigFile(defaultConfigName);
		}
	}

	/**
	 * Setup the Repl for use.
	 * 
	 * @throws IOException
	 *             for booting repl
	 */
	public void firstBoot() throws IOException {
		reader = new ConsoleReader();
		loadHistoryFile();
		code.clearFileOnExit();
		bootRepl();
	}

	/**
	 * Print out welcome title for boot.
	 * 
	 */
	private void outputWelcomeTitle() {
		Command.outputTitle("Java REPL");
		System.out.println("Hi - type 'help' for command list.");
	}

	/**
	 * Boot the JavaRepl up.
	 * 
	 */
	public void bootRepl() {
		try {
			code.generateCompileAndRun(); // to validate the config contents...
			loadRepl();
		} catch (IOException e) {
			logger.log(Level.WARNING, "error loading repl", e);
		}
	}

	/**
	 * Save history for next session.
	 * 
	 * @param h
	 *            history object
	 */
	public void saveHistoryFile(final History h) {
		File historyFile = new File(historyFilePath);
		List<String> history = getHistoryList(h);
		try {
			Files.write(Joiner.on("\n").join(history), historyFile, Charsets.UTF_8);
		} catch (IOException e) {
			System.err.println("Error saving '" + historyFilePath + "' - " + e.toString());
			logger.log(Level.WARNING, "Error saving '" + historyFilePath + "'", e);
		}
	}

	/**
	 * Load history from last session.
	 */
	public void loadHistoryFile() {
		File file = new File(historyFilePath);

		if (file.exists()) {
			Command.outputTitle("Loading history...");
			try {
				final List<String> lines = Files.readLines(file, Charset.defaultCharset());
				for (final String line : lines) {
					oldHistory.add(line);
				}

			} catch (IOException e) {
				System.err.println("Error opening '" + historyFilePath + "' - " + e.toString());
				logger.log(Level.WARNING, "Error opening '" + historyFilePath + "'", e);
			}
		}
	}

	/**
	 * Load JavaRepl .repl file.
	 * 
	 * @param fileName
	 *            location of .repl file
	 */
	public void loadConfigFile(final String fileName) {

		File file = new File(fileName);

		if (file.exists()) {
			Command.outputTitle("Loading config file...(" + fileName + ")");
			try {
				final List<String> lines = Files.readLines(file, Charset.defaultCharset());
				for (final String line : lines) {
					parseLine(line, false);
				}

			} catch (IOException e) {
				System.err.println("Error opening '" + fileName + "' - " + e.toString());
				logger.log(Level.WARNING, "Error opening '" + fileName + "'", e);
			}
		}
	}

	private Function<LineParser, Integer> isHelp(LineParser lp) {

		if (lp.isHelp()) {
			return new Function<LineParser, Integer>() {
				@Override
				public Integer apply(LineParser lp) {
					ReplCommand.outputHelpMenu();
					return 0;
				}
			};
		}
		return null;
	}

	private Function<LineParser, Integer> isClear(LineParser lp) {

		if (lp.isClear()) {
			return new Function<LineParser, Integer>() {
				@Override
				public Integer apply(LineParser lp) {
					try {
						reader.clearScreen();
					} catch (IOException e) {
						logger.log(Level.WARNING, "could not clear screen", e);
					}

					return 0;
				}
			};
		}
		return null;
	}

	private Function<LineParser, Integer> isColonCmd(LineParser lp) {

		if (lp.isColonCmd()) {

			return new Function<LineParser, Integer>() {
				@Override
				public Integer apply(LineParser lp) {
					Integer argLineNumIndex = lp.getColonNumIndex();
					if (argLineNumIndex != null && lp.colonHasCodeArg()) {
						if (lp.getColonCmd().equals("i")) {
							code.addInsertLine(argLineNumIndex, lp.getColonCode());
						} else if (lp.getColonCmd().equals("r")) {
							code.addReplaceLine(argLineNumIndex, lp.getColonCode());
						}
					} else if (lp.getColonCmd().equals("r") && argLineNumIndex != null) {
						code.removeValidCodeLine(argLineNumIndex);
					}

					if (lp.isExecutable()) {
						code.generateCompileAndRun();
					}

					return 0;
				}
			};
		}
		return null;
	}

	/**
	 * Parse non-java code line and do stuff.
	 * 
	 * @param line
	 *            text entered at prompt
	 * @param executeNow
	 *            if java code should be compiled/run
	 * @return true if JavaRepl command was found and run
	 */
	private boolean parseLine(final String line, final boolean executeNow) {

		// split out some of the larger commands with some unnecessary complexity!
		List<Function<LineParser, Integer>> blargs = new LinkedList<Function<LineParser, Integer>>();

		LineParser lp = new LineParser(line, executeNow);

		blargs.add(isHelp(lp));
		blargs.add(isClear(lp));
		blargs.add(isColonCmd(lp));

		for (Function<LineParser, Integer> f : blargs) {
			if (f != null) {
				f.apply(lp);
				return true;
			}
		}

		if (lp.isComment()) {
			return true;
		}

		if (lp.isCode()) {
			outputCodeLines();
			return true;
		}

		if (lp.isRun()) {
			code.generateCompileAndRun();
			return true;
		}

		if (lp.getSpaceCmd().equals("addjar")) {
			transmitSuccess(code.addClassPath(lp.getSpaceArgs(0)), line);
			return true;
		}

		if (lp.getSpaceCmd().equals("addcp")) {
			transmitSuccess(code.addClassPath(lp.getSpaceArgs(0)), line);
			return true;
		}

		if (lp.getSpaceCmd().equals("runonce")) {
			code.addOnceCode(lp.getSpaceArgs(0));
			if (executeNow) {
				code.generateCompileAndRun();
			}
			return true;
		}

		if (lp.getSpaceCmd().equals("addline")) {
			code.addTrialCode(lp.getSpaceArgs(0));

			if (executeNow) {
				code.generateCompileAndRun();
			}

			return true;
		}

		if (lp.getSpaceCmd().equals("import")) {
			System.out.println("______");
			transmitSuccess(code.addTrialImport(line), line);

			if (executeNow) {
				code.generateCompileAndRun();
			}

			return true;
		}

		return false;
	}

	/**
	 * Output success/failture line based on passed functions result.
	 * 
	 * @param wasSuccessful
	 *            passed function result
	 * @param line
	 *            code line that was being tested
	 */
	private void transmitSuccess(final boolean wasSuccessful, final String line) {
		if (wasSuccessful) {
			outputSuccess(line);
		} else {
			outputFailure(line);
		}
	}

	/**
	 * Output success template for line.
	 * 
	 * @param line
	 *            that was successful
	 */
	private void outputSuccess(final String line) {
		System.out.println(" Success | " + line.trim());
	}

	/**
	 * Output failure template for line.
	 * 
	 * @param line
	 *            that was a failure
	 */
	private void outputFailure(final String line) {
		System.out.println(" Failure | " + line.trim());
	}

	/**
	 * <<<<<<< HEAD Loops through cmd docs and prints them out.
	 * 
	 * ======= Display the valid code lines. what would run if we ran "run"
	 * 
	 * >>>>>>> ad9a860ec3d86af0f5e668cc5491ea5464cf4e6f
	 */
	private void outputCodeLines() { // param on if we should show numbers...
		final int lineWidth = 60;
		final int cmdLenMax = 10;
		int counter = 0;
		String lineStr;
		List<String> codeLines = code.getValidCode();
		System.out.println(" # | Code ");
		System.out.println(Strings.repeat("-", lineWidth));
		if (codeLines.size() > 0) {
			for (final String h : codeLines) {
				if (++counter < cmdLenMax) {
					lineStr = " " + Integer.toString(counter);
				} else {
					lineStr = Integer.toString(counter);
				}
				System.out.println(lineStr + " | " + h);
			}
		} else {
			System.out.println(" No code.");
		}
		System.out.println(Strings.repeat("-", lineWidth));
	}

	private boolean handleMultiLineMode(String line) {
		String lastChar;
		lastChar = line.substring(line.length() - 1);

		if (lastChar.equals("{")) {
			multiLineMode = true;
		}

		if (multiLineMode) {
			multiLineCode = multiLineCode + "\n" + line;
		}

		if (lastChar.equals("}")) {
			multiLineMode = false;
		}

		if (!multiLineMode) {
			if (multiLineCode.length() > 0) {
				code.addTrialCode(multiLineCode);
				code.generateCompileAndRun();
				multiLineCode = "";
			} else {
				if (!parseLine(line, true)) {
					code.addTrialCode(line);
					code.generateCompileAndRun();
				}
			}
			saveHistoryFile(reader.getHistory());
			return false;
		} else {
			return true;

		}

	}

	/**
	 * Load the Repl prompt and start input loop.
	 * 
	 * @throws IOException
	 *             from reading line reader
	 */
	private void loadRepl() throws IOException {
		String line;

		Boolean rebootRepl = false;

		String prompt = "java> ";

		reader.setBellEnabled(false);

		if (oldHistory != null) {
			History history = new History();
			for (final String h : oldHistory) {
				history.addToHistory(h);
			}
			reader.setHistory(history);
		}

		while ((line = reader.readLine(prompt)) != null) {
			line = line.trim();

			if (line.length() > 0) {
				if (code.isImportLine(line)) {
					code.addTrialImport(line);
					rebootRepl = true;
					break;
				} else if (line.equals("quit") || line.equals("exit")) {
					break;
				} else { // "Normal" code line (not import, not multiline)
					if (handleMultiLineMode(line)) {
						prompt = "... ";
					} else {
						prompt = "java> ";
					}
				}
			}

		}

		// new Import so lets reboot Repl so we can auto-complete the classes
		// saves our history so far and reload that too

		if (rebootRepl) {
			recordHistory(reader.getHistory());
			bootRepl();
		}
	}

	/**
	 * get history list from History object as List<String>.
	 * 
	 * @param h
	 *            console reader's history obj
	 * @return string list of history
	 */
	private List<String> getHistoryList(final History h) {
		List<String> oh = new LinkedList<String>();
		for (Object o : h.getHistoryList()) {
			oh.add((String) o);
		}

		return oh;
	}

	/**
	 * Record the history of the console reader up to now.
	 * 
	 * @param h
	 *            console reader's history obj
	 */
	private void recordHistory(final History h) {
		oldHistory.addAll(getHistoryList(h));
	}

	private static void loadLogger() {
		try {
			FileHandler handler = new FileHandler("javarepl.log");
			Logger.getLogger("").addHandler(handler); // handles all references..
			Logger.getLogger("").setLevel(Level.INFO);

			// make libs less chatty...
			// Logger.getLogger("com.amazon.carbonado").setLevel(Level.WARNING);

		} catch (IOException ex) {
			System.err.println("Whoops, couldn't even load our logger!");
		}
	}

	/**
	 * Start up the Java Repl.
	 * 
	 * @param args
	 *            .repl files to load
	 * @throws IOException
	 *             from reading line reader
	 */
	public static void main(final String[] args) throws IOException {

		loadLogger();
		Repl r;
		if (args.length > 0) {
			r = new Repl(args[0]);
		} else {
			r = new Repl();
		}

		try {
			r.firstBoot();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
