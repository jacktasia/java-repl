package com.github.jacktasia.javarepl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;

/** A Command is used to execute command line program. */
public final class Command {

	/** Exit/status code of command, (!=0) is error. */
	private int exitVal;

	/** Result of command from stdout/stderr. */
	private String result = "";

	/** Running time for command. */
	private double runningTime;

	/** Result of command. */
	private BufferedReader input;

	/**
	 * Look for javac.exe on windows.
	 * 
	 * @return windows hard drive letter (e.g. C)
	 */
	public static String winDriveLetter() {
		return System.getProperty("user.home").substring(0, 1);
	}

	/**
	 * Look for javac.exe on windows.
	 * 
	 * @return true if jdk is installed
	 */
	public static String winFindJavaCompiler() {
		String winDrive = Command.winDriveLetter();
		String[] cmdStr = new String[] { "cmd.exe", "/C", "dir", "/S", "/P", "\"" + winDrive + ":/javac.exe\"" };
		Command cmd = Command.run(cmdStr);
		String result = cmd.getResult();
		String path = StringUtil.getMatch("Directory of (.+)", result);
		if (path.length() > 0) {
			path = path + File.separator + "javac.exe";
		}

		return path;
	}

	/**
	 * Check if JDK is installed.
	 * 
	 * @param cmdStr
	 *            path/command for javac
	 * @return true if jdk is installed
	 */
	public static boolean testJavaCompiler(final String cmdStr) {
		boolean foundJavaCompiler = false;
		Command cmd = Command.run(new String[] { cmdStr });
		String result = cmd.getResult();

		if (result.length() == 0) {
			return false;
		}

		if (result.contains("-cp <path>")) {
			foundJavaCompiler = true;
		}

		return foundJavaCompiler;
	}

	/**
	 * Print out bar of dashes.
	 * 
	 */
	public static void outputBar() {
		final int barLen = 60;
		System.out.println(Strings.repeat("-", barLen));
	}

	/**
	 * Print out a title string in pretty way.
	 * 
	 * @param title
	 *            the title to print
	 */
	public static void outputTitle(final String title) {
		outputBar();
		System.out.println("| " + title);
		outputBar();
	}

	/**
	 * are we running windows.
	 * 
	 * @return true if running windows
	 */
	public static boolean isWindows() {
		return System.getProperty("os.name").contains("Windows");
	}

	/**
	 * delete the file/directory.
	 * 
	 * @param f
	 *            file/directory to delete
	 * @throws IOException
	 *             if as issue with file
	 */
	public static void deletePath(final File f) throws IOException {
		if (f.isDirectory()) {
			for (final File c : f.listFiles()) {
				c.delete();
			}
		}

		f.delete();
	}

	/**
	 * Get pid processes that matches likeThis. *nix only
	 * 
	 * @param likeThis
	 *            process search needle
	 * @return pid as int or -1 if not found
	 */
	public static Integer getLikePid(final String likeThis) {
		String[] cmd = { "pgrep", "-f", "-o", likeThis };
		Command pidResponse = new Command(cmd);
		if (pidResponse.getResult().trim().matches("\\d+")) {
			return Integer.parseInt(pidResponse.getResult().trim());
		}

		return -1;
	}

	/**
	 * Kill running processes that match likeThis. *nix only
	 * 
	 * @param likeThis
	 *            process search needle
	 * @return true if succesfully killed process
	 */
	public static Boolean killLike(final String likeThis) {
		int pid = getLikePid(likeThis);
		if (pid > -1) {
			Command killPid = new Command("kill -9 " + pid);
			return killPid.isSuccess();
		}

		return false;
	}

	/**
	 * Convenience static constructor.
	 * 
	 * @param cmd
	 *            command to run
	 * @return Command instance
	 */
	public static Command run(final String[] cmd) {
		return new Command(cmd);
	}

	/**
	 * Convenience static constructor.
	 * 
	 * @param cmd
	 *            command to run
	 * @return Command instance
	 */
	public static Command run(final String cmd) {
		return new Command(cmd);
	}

	/**
	 * Constructor object with String array.
	 * 
	 * @param cmd
	 *            command to run
	 */
	public Command(final String[] cmd) {
		runCmd(cmd);
	}

	/**
	 * Constructor object with String.
	 * 
	 * @param cmd
	 *            command to run
	 */
	public Command(final String cmd) {
		String[] cmdl = { cmd };
		runCmd(cmdl);
	}

	/**
	 * Proess command result by turning InputStream to string.
	 * 
	 */
	private void processResult() {
		try {
			result = CharStreams.toString(input);
		} catch (Exception e) { // catch IOException
			result = "runCmd error 1";
		}
	}

	/**
	 * Trigger running the command on OS.
	 * 
	 * @param cmd
	 *            command to run
	 */
	private void runCmd(final String[] cmd) {
		long t1 = System.nanoTime();
		final double toDouble = 1e-6;

		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			InputStreamReader isr = new InputStreamReader(proc.getInputStream(), "UTF-8");
			input = new BufferedReader(isr);
			exitVal = proc.waitFor();

		} catch (Exception e) {
			result = "runCmd error 2";
		}

		long t2 = System.nanoTime();
		runningTime = (t2 - t1) * toDouble;
		processResult();
	}

	/**
	 * Get the result of command. What the command sent to stdout
	 * 
	 * @return result of command (stdout)
	 */
	public String getResult() {
		return result.trim();
	}

	/**
	 * Get status code returned from runnign command.
	 * 
	 * @return status code
	 */
	public int statusCode() {
		return exitVal;
	}

	/**
	 * Get the length of time it took to execute command.
	 * 
	 * @return time it took to execute command.
	 */
	public double getRunTime() {
		return runningTime;
	}

	/**
	 * Did the command run without an error code.
	 * 
	 * @return true if succesful run
	 */
	public boolean isSuccess() {
		return exitVal == 0;
	}

}
