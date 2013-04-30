package com.github.jacktasia.javarepl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests
 */
public class ReplTest extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public ReplTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(ReplTest.class);
	}

	/**
	 * Tests for help lines.
	 */
	public void testCmdHelp() {
		LineParser lp = new LineParser("h");
		LineParser lp2 = new LineParser("help");

		assertTrue(lp.isHelp());
		assertTrue(lp2.isHelp());
	}

	/**
	 * Tests for help lines.
	 */
	public void testCmdClear() {
		LineParser lp = new LineParser("clear");

		assertTrue(lp.isClear());
	}

	/**
	 * Tests for addjar lines.
	 */
	public void testCmdAddJar() {
		String path = "/home/jack/a:sdf/sdf.txt";
		LineParser lp = new LineParser("addjar " + path);
		assertTrue(lp.isSpaceCmd());
		assertTrue(lp.getSpaceCmd().equals("addjar"));
		String[] lpSpaceArgs = lp.getSpaceArgs();
		assertTrue(lpSpaceArgs[0].equals(path));
	}

	/**
	 * Tests for import lines.
	 */
	public void testCmdImport() {
		String path = "java.util.*;";
		LineParser lp = new LineParser("import " + path);
		assertTrue(lp.isSpaceCmd());
		assertTrue(lp.getSpaceCmd().equals("import"));
		String[] lpSpaceArgs = lp.getSpaceArgs();
		assertTrue(lpSpaceArgs[0].equals(path));
	}

}
