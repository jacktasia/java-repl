package com.github.jacktasia.javarepl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A awful utility class for text stuff.
 * mainly helper regex functions.
 */
public final class StringUtil {


	/**
	 * disable external instantiation.
	 */
	private StringUtil() {
	}

	/**
	 * Get regex match, if one, or blank string.
	 *
	 * @param  pattern the regex to find match
	 * @param  content the string to apply pattern to
	 * @return the matching string (or empty string)
	 */
	public static String getMatch(final String pattern,
								  final String content) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(content);

		if (m.find()) {
			return m.group(1);
		} else {
			return "";
		}
	}

	/**
	 * Find out if pattern matches content string.
	 *
	 * @param  pattern the regex to find match
	 * @param  content the string to apply pattern to
	 * @return true if match found, false if not
	 */
	public static boolean isMatch(final String pattern, final String content) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(content);
		return m.find();
	}

	/**
	 * Get regex matches, if any, or blank String[].
	 *
	 * @param  pattern the regex to find match
	 * @param  content the string to apply pattern to
	 * @return the matching strings (or empty string)
	 */
	public static String[] getMatches(final String pattern,
									  final String content) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(content);
		Integer matchCount = m.groupCount();
		String[] groups = new String[matchCount];

		if (m.find()) {
			for (Integer i = 0; i < matchCount; i++) {
				groups[i] = m.group(i + 1);
			}

			return groups;
		}

		return new String[] {};
	}

	/**
	 * Output some random text as a test.
	 *
	 */
	public static void test() {
		System.out.println("The Txt test passed:" + randText());
	}

	/**
	 * Get some random text.
	 *
	 * @return random double as a string
	 */
	public static String randText() {
		return Double.toString(Math.random());
	}

}
