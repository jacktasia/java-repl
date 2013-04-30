package com.github.jacktasia.javarepl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import com.google.common.base.Strings;

/**
 * Output the lines that describes the cmd.
 */

public final class ReplCommand {

	/**
	 * disable external instantiation.
	 */
	private ReplCommand() {
	}

	/**
	 * map is cmd to list of def/examples from xml.
	 * 
	 */
	private static Map<String, List<String>> cmdDocs;

	/**
	 * Output the line that describes the cmd.
	 * 
	 * @param cmd
	 *            the javarepl command
	 * @param def
	 *            definitions of the command
	 * @param example
	 *            an example of the cmd in action
	 */
	public static void outputLine(final String cmd, final String def, final String example) {
		final int lineLen = 79;
		final int spacePad = 10;
		final int dashCount = 60;
		final int tabLen = 4;
		final int defLen = def.length();
		final int offsetLen = lineLen - spacePad;

		if ((defLen + spacePad) > lineLen) {
			System.out.println(Strings.padEnd(cmd, spacePad, ' ') + def.substring(0, offsetLen));
			System.out.println(Strings.repeat(" ", spacePad + tabLen) + def.substring(offsetLen));
		} else {
			System.out.println(Strings.padEnd(cmd, spacePad, ' ') + def);
		}

		if (example.length() > 0) {
			System.out.println("");
			System.out.println(Strings.repeat(" ", spacePad + tabLen) + example);
		}
		System.out.println(Strings.repeat("-", dashCount));
	}

	/**
	 * Loads cmd definitions and examples from xml if needed.
	 * 
	 * @return map of string cmds to list of defs and examples
	 */
	public static Map<String, List<String>> getCmdDocs() {

		if (ReplCommand.cmdDocs == null) {
			InputStream f = Repl.class.getClassLoader().getResourceAsStream("help.xml");
			XMLConfiguration config = new XMLConfiguration();
			String[] xmlCmds = null;
			Map<String, List<String>> tmp = new LinkedHashMap<String, List<String>>();

			try {
				config.load(f);
				xmlCmds = config.getStringArray("commands.command");

				for (String c : xmlCmds) {
					tmp.put(c,
							Arrays.asList(config.getString("defs." + c + ".description"),
									config.getString("defs." + c + ".example")));
				}

				ReplCommand.cmdDocs = Collections.unmodifiableMap(tmp);

			} catch (ConfigurationException ex) {
				ex.printStackTrace();
			}

		}

		return ReplCommand.cmdDocs;
	}

	/**
	 * Prints out the help menu for all commands.
	 * 
	 */
	public static void outputHelpMenu() {
		List<String> tmp;
		Command.outputTitle("Java REPL Command List");

		Map<String, List<String>> cmds = ReplCommand.getCmdDocs();

		for (Map.Entry<String, List<String>> entry : cmds.entrySet()) {
			tmp = entry.getValue();
			outputLine(entry.getKey(), tmp.get(0), tmp.get(1));
		}
	}

}
