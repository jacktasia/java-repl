package com.github.jacktasia.javarepl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.primitives.Ints;

/** LineParser for commands entered into Java Repl. */
public final class LineParser {

	/** line entered in to JavaRepl prompt. */
	private final String line;

	/** "space" cmd (e.g. addjar) (if any). */
	private String spaceCmd = "";

	/** is line a "space" cmd (e.g. addjar). */
	private boolean isSpaceCmd = false;

	/** is line a "colon" cmd (e.g. "r:1"). */
	private boolean isColonCmd = false;

	/** args passed to space command if any. */
	private String[] spaceCmdArgs = new String[] {};

	/** parsed colon cmd if any). */
	private String colonCmd = "";

	/** parsed colon cmd line number (if any). */
	private String colonNum = "";

	/** parsed colon cmd code (if any). */
	private String colonCode = "";

	private final boolean isExecuteNow;

	public LineParser(final String cmdLine) {
		this(cmdLine, true);
	}

	/**
	 * LineParser constructor.
	 * 
	 * @param cmdLine
	 *            entered at JavaRepl prompt
	 */
	public LineParser(final String cmdLine, final boolean isExecuteNow) {
		this.isExecuteNow = isExecuteNow;
		line = cmdLine.trim();
		String[] scMatches = StringUtil.getMatches("^([^ ]+)[ ](.*)$", line);
		List<String> colonInfo = LineParser.parseColonLine(line);

		if (colonInfo.size() > 0) {
			colonCmd = colonInfo.get(0);
		}

		if (colonInfo.size() > 1) {
			colonNum = colonInfo.get(1);
		}

		if (colonInfo.size() > 2) {
			colonCode = colonInfo.get(2);
		}

		isColonCmd = colonInfo.size() > 0;
		isSpaceCmd = scMatches.length > 0;

		if (scMatches.length > 0) {
			spaceCmd = scMatches[0];
		} else {
			scMatches = new String[] { "" };
		}

		if (scMatches.length > 1) {
			spaceCmdArgs = Arrays.copyOfRange(scMatches, 1, scMatches.length);
		}
	}

	public boolean isExecutable() {
		return isExecuteNow;
	}

	/**
	 * Does colon cmd have a passed line number.
	 * 
	 * @return true if colon cmd has a line number
	 */
	public boolean colonHasNumArg() {
		return colonNum.length() > 0;
	}

	/**
	 * Tells you if getColonCode would actually return any code.
	 * 
	 * @return true if there was code
	 */
	public boolean colonHasCodeArg() {
		return colonCode.length() > 0;
	}

	/**
	 * Get the code passed to the colon cmd (if any). For this: r:1:Sytem.out.println("New First Line"); This function
	 * would return: Sytem.out.println("New First Line");
	 * 
	 * @return code passed to colon cmd (or blank string)
	 */
	public String getColonCode() {
		return colonCode;
	}

	/**
	 * Get the colon cmd (e.g. "r) that was used (if any). can only be "r", "i" or empty string
	 * 
	 * @return colon cmd used if any, or empty string
	 */
	public String getColonCmd() {
		return colonCmd;
	}

	/**
	 * Get the array index of passed line number (if any). this is for the interal array of valid code will be null if
	 * no line passed
	 * 
	 * @return the valid code array index if any
	 */
	public Integer getColonNumIndex() {
		final int lastLineIndex = -2;

		if (colonNum.length() == 0 || !isColonCmd()) {
			return null;
		}

		if (colonNum != null && colonNum.length() > 0 && colonNum.equals("-")) {
			return lastLineIndex;
		}

		if (colonNum != null) {
			return Ints.tryParse(colonNum) - 1;
		}

		return null;
	}

	/**
	 * Parse out the parts of a colon (e.g. "r") cmd. Potentially: the command,line num, and new code
	 * 
	 * @param cmd
	 *            the colon cmd entered
	 * @return the parts of the cmd
	 */
	public static List<String> parseColonLine(final String cmd) {
		List<String> matchList = new ArrayList<String>();
		Pattern regex = Pattern.compile("[^\\:\"']+|\"[^\"]*\"|'[^']*'");
		Matcher regexMatcher = regex.matcher(cmd);

		while (regexMatcher.find()) {
			matchList.add(regexMatcher.group());
		}

		return matchList.size() > 2 ? LineParser.forceThreeItems(matchList) : matchList;
	}

	/**
	 * Helper for parseColonLIne. Necessary due to a limition in the regex that is used to parse the line. this should
	 * not exist and regex should be improved
	 * 
	 * @param items
	 *            the exploded line, count should be >3
	 * @return the command,line num, and new code
	 */
	private static List<String> forceThreeItems(final List<String> items) {
		List<String> result = new ArrayList<String>();
		int count = 0;
		String rest = "";

		for (String s : items) {
			if (count < 2) {
				result.add(s);
			} else {
				rest += s;
			}
			count++;
		}

		if (rest.length() > 0) {
			result.add(rest);
		}

		return result;
	}

	/**
	 * get the currently entered line used.
	 * 
	 * @return the line
	 */
	public String getLine() {
		return line;
	}

	/**
	 * get the space command used.
	 * 
	 * @return the space command
	 */
	public String getSpaceCmd() {
		return spaceCmd;
	}

	/**
	 * get an array of space args.
	 * 
	 * @return the arg array
	 */
	public String[] getSpaceArgs() {
		return spaceCmdArgs;
	}

	/**
	 * is the line a space cmd (e.g. addcp).
	 * 
	 * @param index
	 *            desired index 0-based index of arg array
	 * @return the requested arg
	 */
	public String getSpaceArgs(final int index) {
		return spaceCmdArgs[index];
	}

	/**
	 * is the line a space cmd (e.g. addcp).
	 * 
	 * @return is it a space cmd
	 */
	public boolean isSpaceCmd() {
		return isSpaceCmd;
	}

	/**
	 * is the line a colon cmd.
	 * 
	 * @return is it a colon cmd
	 */
	public boolean isColonCmd() {
		return isColonCmd && (colonCmd.equals("r") || colonCmd.equals("i"));
	}

	/**
	 * is the line help.
	 * 
	 * @return true if line is "help" cmd
	 */
	public boolean isHelp() {
		return line.equals("help") || line.equals("h");
	}

	/**
	 * is the line the code command.
	 * 
	 * @return true if line is "code" cmd
	 */
	public boolean isCode() {
		return line.equals("code");
	}

	/**
	 * is the line clear.
	 * 
	 * @return true if line is "clear" cmd
	 */
	public boolean isClear() {
		return line.equals("clear");
	}

	/**
	 * is the line run.
	 * 
	 * @return true if line is "run" cmd
	 */
	public boolean isRun() {
		return line.equals("run");
	}

	/**
	 * is the line a comment.
	 * 
	 * @return true if line is a comment
	 */
	public boolean isComment() {
		return (line.length() > 0 && line.substring(0, 1).equals("#"))
				|| (line.length() > 1 && line.substring(0, 2).equals("//"));
	}

}
