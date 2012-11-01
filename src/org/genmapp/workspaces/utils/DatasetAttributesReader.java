/*
  File: CyAttributesReader.java

  Copyright (c) 2006, 2010, The Cytoscape Consortium (www.cytoscape.org)

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package org.genmapp.workspaces.utils;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;

import cytoscape.data.CyAttributes;
import cytoscape.data.attr.MultiHashMapDefinition;
import cytoscape.data.readers.CyAttributesReader;
import cytoscape.data.writers.CyAttributesWriter;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.PluginManager;

import org.genmapp.workspaces.utils.DatasetAttributesWriter;
import org.genmapp.workspaces.GenMAPPWorkspaces; // Required for loading the COLUMN delimiter string!

public class DatasetAttributesReader {
	public static final String DECODE_PROPERTY = "cytoscape.decode.attributes";
	static boolean badDecode = false;

	static final byte FAILURE_TO_INFER_TYPE = -1;

	public static void loadAttributes(CyAttributes cyAttrs, final FileReader fileIn, final CyLogger logger) throws IOException {
		WorkspacesCommandHandler.showMessage("loadAttributes");
		logger.info("DatasetAttributesReader: loadAttributes starting now...");
		// read entire file and store in 2-dim array, where each column is a separate sub-array ( 0 = attrname, 1 = attrtype, 2 ... n = values )
		// Read in the csv file...
		int lineNum = 0;
		int totalNumHeaderColumns = 0;
		Vector<Vector<String>> bufferCols = null; // <-- 2 dimensional "array"
		try {
			// Bizarrely, bufferCols is actually COLUMNS and not how you'd normally expect.
			// So this thing gets populated in a super-non-normal way;
			// each top-level element in the thing is a COLUMN and not a row!
			// Note that this is BACKWARDS from how you almost certainly expect to read a line-by-line file!!!!!
			BufferedReader inStream = new BufferedReader(fileIn);
			for (String line = inStream.readLine(); line != null; line = inStream.readLine()) {
				lineNum++;
				final String row[] = line.split(GenMAPPWorkspaces.DELIMITER_STRING); // Split the comma-separated values
				if (bufferCols == null) { // <-- Initialize this the FIRST TIME only!
					totalNumHeaderColumns = row.length;
					bufferCols = new Vector<Vector<String>>(totalNumHeaderColumns);
					for (int i = 0; i < totalNumHeaderColumns; i++) {
						bufferCols.add(new Vector<String>()); // <-- Adds a new COLUMN!!! This is NOT A SINGLE ELEMENT, and it is NOT A ROW despite the fact that that might seem reasonable.
						// This is a VERY UNUSUAL way to read a file (essentially, into columns instead of rows), so watch out!
						// Don't get confused---it seems like we REQUIRE the ability to get column slices
						// easily later on, which is why the data is read into columns instead of rows.
					}
					logger.debug("Line " + lineNum + ": init bufferCols: colTotal =  " + totalNumHeaderColumns);
					logger.debug("Line " + lineNum + ": bufferCols inited = " + bufferCols + " with length " + bufferCols.size());
				}

				if (row.length != totalNumHeaderColumns) {
					logger.error("WARNING: Line " + lineNum + ": has a DIFFERENT number of elements from the header---uneven column total while loading attributes file: # of columns is SUPPOSED to be " + totalNumHeaderColumns + ", but we encountered line " + lineNum + " with " + row.length + " comma-separated columns. We are going to attempt to just deal with this, but it probably means there is somewhing weird about the way the file that you are loading was saved.");
				}

				for (int col = 0; col < totalNumHeaderColumns; col++) {
					// For each item in this row, we're going to append that item to the end of the appropriate column.
					// Note that this is a SUPER UNUSUAL way to append things---we aren't just adding a row,
					// we're actually lengthening a whole bunch of columns.
					final String itemToAdd = (col < row.length) ? row[col] : null;
					// About: if this row is NOT LONG ENOUGH, we will need to add a "dummy" value.
					// Should we add a null value, or should it be something like a blank string instead?
					// For now, it's a null value.
					bufferCols.get(col).add(itemToAdd); // <-- adds either a String or null
				}
			}
			logger.info("Finished reading. Total # columns is: " + totalNumHeaderColumns + " columns");
			WorkspacesCommandHandler.showMessage("Read file with " + totalNumHeaderColumns + " columns ");
		} catch (Exception e) {
			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			logger.error("ERROR HEDGEHOG 97: while processing line " + lineNum + ". Now, Alex found a problem at: " + writer.toString() + "\n. There was also the exception being reported as " + e);
		}
		try {
			// for each col, send to loadAttributesInternal
			for (int i = 0; i < totalNumHeaderColumns; i++) {
				// Apparently the FIRST column (bufferCols.get(0)) has the types
				// not sure if this should really start counting at zero... seems kind of weird and wrong actually!
				// probably this should start at (i = 1) intead of i = 0!!!!!!!!!!!!!!!!!!!!!!!!!!
				// but apparently this is actually how it should work. Weird!
				// TODO: fix this crazy stuff!!!
				loadAttributesINTERNAL(cyAttrs, bufferCols.get(0), bufferCols.get(i));
			}
			WorkspacesCommandHandler.showMessage("DatasetAttributesReader: loadAttributes is [DONE]!");
		} catch (Exception e) {
			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			logger.error("ERROR CAPYBARA 42: " + "Now, Alex found a problem at: " + writer.toString() + "\n. There was also the exception being reported as " + e);
		}
	}

	private static byte getTypeFromClassNameString(final String className) {
		// Just a local convenient function for mapping from the weird type names to the single-byte type variable
		// that we apparently like to use below.
		byte type;
		if (className.equalsIgnoreCase("java.lang.String") || className.equalsIgnoreCase("String")) {
			type = MultiHashMapDefinition.TYPE_STRING;
		} else if (className.equalsIgnoreCase("java.lang.Boolean") || className.equalsIgnoreCase("Boolean")) {
			type = MultiHashMapDefinition.TYPE_BOOLEAN;
		} else if (className.equalsIgnoreCase("java.lang.Integer") || className.equalsIgnoreCase("Integer")) {
			type = MultiHashMapDefinition.TYPE_INTEGER;
		} else if (className.equalsIgnoreCase("java.lang.Double") || className.equalsIgnoreCase("Double") || className.equalsIgnoreCase("java.lang.Float") || className.equalsIgnoreCase("Float")) {
			type = MultiHashMapDefinition.TYPE_FLOATING_POINT;
		} else {
			type = FAILURE_TO_INFER_TYPE;
		}
		return type;
	}

	private static void loadAttrHandleListType(final CyAttributes cyAttrs, String key, final String val, final String attributeName, byte type, int lineNum) throws IOException {
		// if it starts with "(", then that means it's a LIST TYPE. APPARENTLY. Seems dubious.
		if (val.length() < 2) {
			CyLogger.getLogger().error("loadAttrHandleListType: Well, this is messed up, we got a supposed 'list' type, but it wasn't actually a list type probably, as it was of length < 2. Here it is: " + val);
		}
		if (!val.startsWith("(") || !val.endsWith(")")) {
			CyLogger.getLogger().error("loadAttrHandleListType: Well, this is messed up, we got a supposed 'list' type, but it apparently either doesn't start with an open-paren, or doesn't end with a close-paren, like it's supposed to. Here it is: " + val);
		}
		ArrayList<Object> elmsBuff = new ArrayList<Object>(); // <-- note that this ArrayList may get MODIFIED in place to convert data types from Strings to (say) Integers or Doubles. Or they could remain Strings, depending on the desired final data type!
		final String trimmedValNoParens = val.substring(1, (val.length() - 1)).trim(); // Chop away leading '(' and trailing ')'. -- Removes the FIRST AND LAST characters from "val" and then trim off whitespace.
		for (final String vs : (String[]) trimmedValNoParens.split(DatasetAttributesWriter.LIST_SEPARATOR)) {
			elmsBuff.add(decodeSlashEscapes(decodeString(vs))); // Note: might be NULL! Always a string at this point, but takes NON STRING objects later, which is kind of interesting and weird.
		}
		for (int i = 0; i < elmsBuff.size(); i++) {
			if (elmsBuff.get(i) == null) {
				CyLogger.getLogger().error("Ran into a null element---couldn't properly decode element at index " + i + " on line " + lineNum + ". I guess we're just going to keep it as NULL instead of trying to do anything fancy.");
			} else if (type == MultiHashMapDefinition.TYPE_INTEGER) {
				elmsBuff.set(i, new Integer((String) elmsBuff.get(i))); // Note: adds an INTEGER to the data structure.
			} else if (type == MultiHashMapDefinition.TYPE_BOOLEAN) {
				elmsBuff.set(i, new Boolean((String) elmsBuff.get(i))); // Note: adds a BOOLEAN to the data structure.
			} else if (type == MultiHashMapDefinition.TYPE_FLOATING_POINT) {
				elmsBuff.set(i, new Double((String) elmsBuff.get(i))); // Note: adds a DOUBLE to the data structure. (Note: must NOT be a a "Float"!)
			} else {
				// Probably a string; do nothing---the thing in it is ALREADY a string, so don't change it!
				// If it's not a string, then, uh, I don't know what will happen here!
			}
		}
		cyAttrs.setListAttribute(key, attributeName, elmsBuff); // <-- From the Oracle Javadocs: Requirements for setListAttribute: "All items within the list are of the same type, and are chosen from one of the following: Boolean, Integer, Double or String."
		// Maybe this creates the node???
	}

	private static void setCyAttrBasedOnType(final byte type, CyAttributes cyAttrsToSet, final String theKey, final String attributeName, final String setToThing) {
		// Alex Williams (Sept 2012): Handles the annoying type-based dispatching for adding to cyAttrsToSet. As far as I know, there's no "more elegant" way to do this.
		if (type == MultiHashMapDefinition.TYPE_INTEGER) {
			cyAttrsToSet.setAttribute(theKey, attributeName, new Integer(setToThing));
		} else if (type == MultiHashMapDefinition.TYPE_BOOLEAN) {
			cyAttrsToSet.setAttribute(theKey, attributeName, new Boolean(setToThing));
		} else if (type == MultiHashMapDefinition.TYPE_FLOATING_POINT) {
			cyAttrsToSet.setAttribute(theKey, attributeName, new Double(setToThing));
		} else {
			cyAttrsToSet.setAttribute(theKey, attributeName, (String) setToThing);
		}
	}

	// nodeNames and col are parallel arrays
	public static void loadAttributesINTERNAL(CyAttributes cyAttrs, final Vector<String> nodeName, final Vector<String> nodeData) throws IOException {
		final int ATTR_NAME_LINE_ROW_INDEX = 0; // First line: attribute NAMES
		final int ATTR_TYPE_LINE_ROW_INDEX = 1; // Second line: attribute TYPES
		final int FIRST_NON_HEADER_LINE_INDEX = 2; // '2' means the THIRD line (two header lines, '0' and '1')
		WorkspacesCommandHandler.showMessage("loadAttributesInternal " + nodeData.get(0));
		CyLogger.getLogger().debug("loadAttributesInternal " + nodeData.get(0));
		badDecode = false; // <-- class-wide variable. Really not sure why it gets set to false here... seems kind of weird!
		int nnn = -1; // <-- line number
		try {
			final String attrName = nodeData.get(ATTR_NAME_LINE_ROW_INDEX);
			final String className = nodeData.get(ATTR_TYPE_LINE_ROW_INDEX);
			final byte type = getTypeFromClassNameString(className);
			CyLogger.getLogger().debug("className: " + className);
			// TODO: it's screwed up here on line 2!!!!!!!!!!!!!!!!!!!!!!!
			for (nnn = FIRST_NON_HEADER_LINE_INDEX; nnn < nodeData.size(); nnn++) {
				// Note that the header is TWO LINES instead of just one.
				final String key = nodeName.get(nnn);
				final String val = nodeData.get(nnn);
				if (val.startsWith("(")) {
					// Specially handle a LIST data type, which we expect to start with a parenthesis.
					// Is this ACTUALLY a reliable way of detecting lists? I very much doubt it!!!
					loadAttrHandleListType(cyAttrs, key, val, attrName, type, nnn);
				} else {
					// Not a list data type!
					final String decodedVal = decodeSlashEscapes(decodeString(val));
					if (decodedVal == null) {
						CyLogger.getLogger().info("Couldn't successfully decode the value \'" + val + "\' to anything but NULL. On line " + nnn);
						// I guess we don't bother setting an attribute if it's NULL? Not sure what that would even mean exactly.
					} else {
						setCyAttrBasedOnType(type, cyAttrs, key, attrName, decodedVal); // set the attribute for this non-null key/value pair
					}
				}
			}
		} catch (Exception e) {
			final String message = "Failed to parse attributes file at line " + nnn + ", with exception: " + e.getMessage();
			CyLogger.getLogger(CyAttributesReader.class).warn(message, e);
			throw new IOException(message);
		}
	}

	private static String decodeString(String in) throws IOException {
		if (in == null) {
			return null; // Handle null specially
		}
		try {
			return ((String) URLDecoder.decode(in, CyAttributesWriter.ENCODING_SCHEME));
		} catch (IllegalArgumentException iae) {
			if (!badDecode) {
				// The first time we encounter a problem, set the "badDecode" class-wide flag.
				// Weirdly, this "badDecode" flag still gets set back to "false" periodically. Not sure why!
				CyLogger.getLogger().warn("DECODING PROBLEM: Couldn't decode the input string \'" + in + "\'! Additional message: " + iae);
				badDecode = true; // set this CLASS-WIDE variable if the decoding was messed up somehow.
			}
			return null;
		}
	}

	private static String decodeSlashEscapes(final String in) {
		if (in == null) {
			return null; // <-- detect a null input and also return null!
		}
		final char BACKSLASH_CHARACTER = '\\';
		StringBuilder stringMaker = new StringBuilder();
		for (int i = 0; i < in.length(); i++) {
			final char ch = in.charAt(i);
			if (ch == BACKSLASH_CHARACTER) { // <-- found the SPECIAL backslash character!
				if ((i + 1) >= in.length()) {
					// The string ends in a backslash (\), but with nothing else afterward! Just ignore it.
					// (Don't go off the end of the string.)
				} else {
					i++; // <-- Now that we know the previous one was a special character, advance the "cursor" and move on to the next one...
					final char charAfterBackslash = in.charAt(i);
					switch (charAfterBackslash) {
						case 'n':
							stringMaker.append('\n'); // newline (special)
						break;
						case 't':
							stringMaker.append('\t'); // tab (special)
						break;
						case 'b':
							stringMaker.append('\b'); // \b, whatever that is
						break;
						case 'r':
							stringMaker.append('\r'); // return (like a newline)
						break;
						case 'f':
							stringMaker.append('\f'); // \f, whatever that is
						break;
						default:
							stringMaker.append(charAfterBackslash); // <-- Otherwise, just return the LITERAL character. i.e., "\a" is just a letter 'a', nothing special.
					}
				}
			} else {
				stringMaker.append(ch); // <-- Just a regular character, handle it regular-style
			}
		}
		return stringMaker.toString();
		// TODO: (probably written by Isaac maybe?) we should detect when "null" string is used as Numeric attribute
		// place holder and convert simply skip writing those lines
		// Note from Alex Williams (Sept 2012): I don't know what this means, but it seems like it might be important?
	}

}