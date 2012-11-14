/*
  File: DatasetAttributesReader.java

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

// Javadocs for Cytoscape 2.8.3 can be found at: http://chianti.ucsd.edu/Cyto-2_8_3/javadoc/index.html?cytoscape/CyMain.html

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
	private static int numBadDecodes;
	private static final int MAX_NUM_BAD_DECODES_TO_REPORT = 10; // Only report this many (or fewer) bad-decoding errors.

	private static final byte FAILURE_TO_INFER_TYPE = -1;
	private static final int SPLIT_ALLOW_BLANK_CELLS_AT_END = -1; // This must be NEGATIVE ONE and it means "we need to allow blank cells at the end of the string being delimited." Look up String split for more info.

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
			final BufferedReader inStream = new BufferedReader(fileIn);
			for (String line = inStream.readLine(); line != null; line = inStream.readLine()) {
				lineNum++;
				final String row[] = line.split(GenMAPPWorkspaces.DELIMITER_REGEXP, SPLIT_ALLOW_BLANK_CELLS_AT_END); // Split the comma-or-whatever-separated values
				// logger.debug("Line " + lineNum + " is length " + row.length + " when split up by delimiter [" + GenMAPPWorkspaces.DELIMITER_REGEXP + "]: [[[" + line + "]]]");
				if (bufferCols == null) { // <-- Initialize this the FIRST TIME only!
					totalNumHeaderColumns = row.length;
					bufferCols = new Vector<Vector<String>>(totalNumHeaderColumns);
					for (int i = 0; i < totalNumHeaderColumns; i++) {
						bufferCols.add(new Vector<String>()); // <-- Adds a new COLUMN!!! This is NOT A SINGLE ELEMENT, and it is NOT A ROW despite the fact that that might seem reasonable.
						// This is a VERY UNUSUAL way to read a file (essentially, into columns instead of rows), so watch out!
						// Don't get confused---it seems like we REQUIRE the ability to get column slices
						// easily later on, which is why the data is read into columns instead of rows.
					}
					logger.info("Line " + lineNum + ": init bufferCols: colTotal =  " + totalNumHeaderColumns);
					logger.info("Line " + lineNum + ": bufferCols inited = " + bufferCols + " with length " + bufferCols.size());
				}

				if (row.length != totalNumHeaderColumns) {
					logger.error("POSSIBLE ERROR IN SAVE FILE: Line " + lineNum + ": has a DIFFERENT number of elements from the header---uneven column total while loading attributes file. The number of columns is SUPPOSED to be " + totalNumHeaderColumns + ", but we encountered line " + lineNum + " with " + row.length
							+ " delimiter-separated columns. We are going to attempt to just deal with this, but it probably means there is somewhing weird about the way the file that you are loading was saved.");
				}
				for (int col = 0; col < totalNumHeaderColumns; col++) {
					// For each item in this row, we're going to append that item to the end of the appropriate column.
					// Note that this is a SUPER UNUSUAL way to append things---we aren't just adding a row,
					// we're actually lengthening a whole bunch of columns.
					final String itemToAdd = (col < row.length) ? row[col] : null;
					// About: if this row is NOT LONG ENOUGH, we will add a DUMMY null value!
					bufferCols.get(col).add(itemToAdd); // <-- adds either a String or null
				}
			}
			logger.info("Finished reading attributes. Total number of columns reads is: " + totalNumHeaderColumns);
			WorkspacesCommandHandler.showMessage("Read file with " + totalNumHeaderColumns + " columns ");
		} catch (Exception e) {
			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			logger.error("ERROR PRINTWRITER 97: while processing line " + lineNum + ". Now, Alex found a problem at: " + writer.toString() + "\n. There was also the exception being reported as " + e);
		}
		try {
			// for each col, send to loadAttributesInternal
			for (int i = 0; i < totalNumHeaderColumns; i++) { // <-- note that i is INTENTIONALLY starting from zero here.
				// Note that this ALWAYS passed in "bufferCols.get(0)" , which is the NAMES column.
				// It is INTENTIONAL that the first call to this is with i = 0. Do not change it to i = 1!
				loadAttributesINTERNAL(cyAttrs, bufferCols.get(0), bufferCols.get(i));
			}
			WorkspacesCommandHandler.showMessage("DatasetAttributesReader: loadAttributes is [DONE]!");
		} catch (Exception e) {
			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			logger.error("ERROR EXCEPTION 42: " + "Found a problem involving an un-caught-earlier exception at: " + writer.toString() + "\n. The exception is reported as: " + e);
		}
	}

	private static byte getTypeFromClassNameString(final String className) {
		// Just a local convenient function for mapping from the weird type names to the single-byte type variable.
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

	private static void setCyAttrForListTypesOnly(final CyAttributes cyAttrs, String key, final String val, final String attributeName, byte type, int lineNum) throws IOException {
		// if it starts with DatasetAttributesWriter.LIST_START_STRING, then that means it's a LIST TYPE. APPARENTLY. Seems dubious.
		final int amtToTrimFromStart = DatasetAttributesWriter.LIST_START_STRING.length();
		final int amtToTrimFromEnd = DatasetAttributesWriter.LIST_END_STRING.length();
		if (val.length() < (amtToTrimFromStart + amtToTrimFromEnd)) {
			CyLogger.getLogger().error("loadAttrHandleListType: Well, this is messed up, we got a supposed 'list' type, but it wasn't actually a list type probably, as it was of length LESS THAN the length of the start and end list delimiters. Here it is: " + val);
			// End EARLY if we got an invalid data item!
		} else if (!val.startsWith(DatasetAttributesWriter.LIST_START_STRING) || !val.endsWith(DatasetAttributesWriter.LIST_START_STRING)) {
			CyLogger.getLogger().error("loadAttrHandleListType: Well, this is messed up, we got a supposed 'list' type, but it apparently either doesn't start with the list delimiter or it doesn't end with the list delimiter, like it's supposed to. Here it is: " + val);
			// End EARLY if we got an invalid data item!
		} else {
			// If we got here, we SHOULD have a valid list!
			ArrayList<Object> elmsBuff = new ArrayList<Object>(); // <-- note that this ArrayList may get MODIFIED in place to convert data types from Strings to (say) Integers or Doubles. Or they could remain Strings, depending on the desired final data type!
			final String trimmedValWithNoSurroundingParens = val.substring(amtToTrimFromStart, (val.length() - amtToTrimFromEnd)).trim(); // Chop away leading '(' and trailing ')'. -- Removes the FIRST AND LAST characters from "val" and then trim off whitespace.
			for (final String vs : (String[]) trimmedValWithNoSurroundingParens.split(DatasetAttributesWriter.LIST_SEPARATOR, SPLIT_ALLOW_BLANK_CELLS_AT_END)) {
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
				} else if (type == FAILURE_TO_INFER_TYPE) {
					CyLogger.getLogger().error("Ran into an element where we couldn't infer the type---couldn't properly decide the type of element at index " + i + " on line " + lineNum + ". I guess we're just going to try to add it without processing it specially.");
				} else {
					// Probably a string; do nothing---the thing in it is ALREADY a string, so don't change it!
					// If it's not a string, then, uh, I don't know what will happen here!
					// Looks like we DO NOTHING TO IT! It's ALREADY in the correct format.
				}
			}
			cyAttrs.setListAttribute(key, attributeName, elmsBuff);
			// Requirement for setListAttribute, from the Oracle Javadocs:
			// "All items within the list are ******of the same type******, and are chosen from one of the following: Boolean, Integer, Double or String."
			// Maybe this creates the node??? Unclear.
		}
	}

	private static void setOurCyAttributes(CyAttributes cyAttrsToSet, final String theKey, final String theValBeforeDecoding, final String attributeName, final byte type, final int lineNum) throws IOException {
		// Alex Williams (Sept 2012): Handles the annoying type-based dispatching for adding to cyAttrsToSet. As far as I know, there's no "more elegant" way to do this.

		if ((theValBeforeDecoding == null) || (theValBeforeDecoding.length() == 0)) {
			return; // Don't do ANYTHING if there's a zero-length string or a null string!
		}

		final boolean isListType = (theValBeforeDecoding != null) && theValBeforeDecoding.startsWith(DatasetAttributesWriter.LIST_START_STRING) && theValBeforeDecoding.endsWith(DatasetAttributesWriter.LIST_END_STRING);
		// Specially handle a LIST data type.
		// Alex Williams: Is this ACTUALLY a reliable way of detecting lists? I very much doubt it!!!
		// What if a value starts with a LIST_START_STRING but is NOT a list? Like a gene description in parentheses or something?
		// That would NOT be a list data type, but it would be (incorrectly) parsed as one. I think this is probably a bug but I don't actually know 100%.
		// To do / todo:

		final String decodedVal = decodeSlashEscapes(decodeString(theValBeforeDecoding));
		
		if (decodedVal == null) {
			// do nothing
		} else if (isListType) {
			setCyAttrForListTypesOnly(cyAttrsToSet, theKey, decodedVal, attributeName, type, lineNum);
		} else if (type == MultiHashMapDefinition.TYPE_INTEGER) {
			cyAttrsToSet.setAttribute(theKey, attributeName, new Integer(decodedVal));
		} else if (type == MultiHashMapDefinition.TYPE_BOOLEAN) {
			cyAttrsToSet.setAttribute(theKey, attributeName, new Boolean(decodedVal));
		} else if (type == MultiHashMapDefinition.TYPE_FLOATING_POINT) {
			cyAttrsToSet.setAttribute(theKey, attributeName, new Double(decodedVal));
		} else if (type == FAILURE_TO_INFER_TYPE) {
			CyLogger.getLogger().error("Ran into an element where we couldn't infer the type---couldn't properly decide the type of attribute [[" + attributeName + "]] with key//value as [[" + theKey + " // " + decodedVal + "]]. We are going to treat it as a String.");
			cyAttrsToSet.setAttribute(theKey, attributeName, (String) decodedVal); // <-- If there was a decoding failure, try to pretend the type is a String anyway.
		} else {
			cyAttrsToSet.setAttribute(theKey, attributeName, (String) decodedVal);
		}
	}

	// nodeNames and col are parallel arrays
	public static void loadAttributesINTERNAL(CyAttributes cyAttrs, final Vector<String> nodeName, final Vector<String> nodeData) throws IOException {
		final int ATTR_NAME_LINE_ROW_INDEX = 0; // First line: attribute NAMES
		final int ATTR_TYPE_LINE_ROW_INDEX = 1; // Second line: attribute TYPES
		final int FIRST_NON_HEADER_LINE_INDEX = 2; // '2' means the THIRD line (two header lines, '0' and '1')
		WorkspacesCommandHandler.showMessage("loadAttributesInternal " + nodeData.get(0));
		CyLogger.getLogger().debug("loadAttributesInternal " + nodeData.get(0));
		numBadDecodes = 0;
		int nnn = -1; // <-- line number
		try {
			final String attrName = nodeData.get(ATTR_NAME_LINE_ROW_INDEX);
			final String className = nodeData.get(ATTR_TYPE_LINE_ROW_INDEX);
			final byte type = getTypeFromClassNameString(className);
			CyLogger.getLogger().debug("className: " + className);
			// TODO: it's screwed up here on line 2!!!!!!!!!!!!!!!!!!!!!!!
			for (nnn = FIRST_NON_HEADER_LINE_INDEX; nnn < nodeData.size(); nnn++) {
				// nodeName is the key, nodeData is the value!
				setOurCyAttributes(cyAttrs, nodeName.get(nnn), nodeData.get(nnn), attrName, type, nnn);
			}
		} catch (final Exception e) {
			final String message = "Failed to parse attributes file at line " + nnn + ", with exception: " + e.getMessage();
			CyLogger.getLogger(CyAttributesReader.class).warn(message, e);
			throw new IOException(message);
		}
	}

	private static String decodeString(final String in) throws IOException {
		if (in == null) {
			return null; // <-- detect a null input and also return null!
		}
		try {
			return ((String) URLDecoder.decode(in, CyAttributesWriter.ENCODING_SCHEME));
		} catch (final IllegalArgumentException iae) {
			if (numBadDecodes < MAX_NUM_BAD_DECODES_TO_REPORT) {
				CyLogger.getLogger().warn("DECODING PROBLEM #" + numBadDecodes + ": Couldn't decode the input string \'" + in + "\'! Additional message: " + iae);
				numBadDecodes++;
				if (numBadDecodes == MAX_NUM_BAD_DECODES_TO_REPORT) {
					CyLogger.getLogger().warn("We will not be reporting addtiaional decoding problems, as we have reached the maximum number to report (" + MAX_NUM_BAD_DECODES_TO_REPORT + ")");
				}
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
	}

}