// Javadocs for Cytoscape 2.8.3 can be found at: http://chianti.ucsd.edu/Cyto-2_8_3/javadoc/index.html?cytoscape/CyMain.html

package org.genmapp.workspaces.utils;

import java.util.HashSet;
import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashSet;
import java.util.List;
//import java.util.Set;
//import org.genmapp.workspaces.objects.CyDataset;

//import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.readers.CyAttributesReader;
import cytoscape.giny.CytoscapeRootGraph;
import cytoscape.logger.CyLogger;
//import cytoscape.plugin.PluginManager;

import org.genmapp.workspaces.GenMAPPWorkspaces; // Required for loading the COLUMN delimiter string!

// we wrap the core library of the same name to do the dirty work
public class DatasetAttributesWriter {

	private static final String CANONICAL_NAME = "canonicalName";
	public static final String LIST_SEPARATOR = "::"; // <-- lists have "::" between elements.

	// ELEMENTS (i.e., columns) are separated based on the string in org.genmapp.workspaces.GenMAPPWorkspaces

	private static String encodedString(String raw) {
		// I assume this is Isaac's comment: need to add in proper encoding from CyAttributesWriter
		// Note that right now, nothing happens here. I suspect this means we rely on the "toString" method.
		return (raw); // This doesn't do ANYTHING AT ALL right now! No clue what it's supposed to do; maybe escape certain "dangerous" control characters?
	}

	private static String stringFromList(final List<?> list) {
		// Alex Williams: note that this previously failed to work properly if the list is empty. It would return something like NULL] instead of [].
		String s = ""; // <-- must initial to the empty string so that "length() == 0" works properly below!
		for (Object o : list) {
			s += ((s.length() == 0) ? "" : LIST_SEPARATOR) + encodedString(o.toString()); // <-- add the LIST_SEPARATOR for all elements EXCEPT the first one
		}
		return ("[" + s + "]"); // Brackets go around the string as well!
	}

	private static String getEncodedAttribute(final String attr, final String nodeName, final CyAttributes attributes) {
		final byte type = attributes.getType(attr);
		if (!attributes.hasAttribute(nodeName, attr) || type == CyAttributes.TYPE_SIMPLE_MAP || type == CyAttributes.TYPE_COMPLEX) {
			// CyLogger.getLogger().warn("DatasetAttributesWriter.java 'getEncodedAttribute' function: we are trying to log the attribute " + nodeName + " with type " + type + ", but remember that we can't actually handle simple_maps or complex data types. I hope we don't need to save this data.");
			return ""; // Apparently we return a blank string here for simple_map, complex, and missing attributes. Not sure if this is correct, or could lead to failures down the line.
		}
		switch (type) {
			case CyAttributes.TYPE_STRING:
				return encodedString((String) attributes.getAttribute(nodeName, attr)); // Encode the string
			case CyAttributes.TYPE_SIMPLE_LIST:
				return stringFromList(attributes.getListAttribute(nodeName, attr)); // Encode the list with our CUSTOM code up above
			default:
				return attributes.getAttribute(nodeName, attr).toString(); // Hopefully "toString" can be relied on for all the other data types
		}
	}

	private static String attributeTypeAsString(final byte cyType) {
		switch (cyType) {
			case CyAttributes.TYPE_BOOLEAN:
				return ("Boolean"); // Alex Williams: note the lack of "break" statements here
			case CyAttributes.TYPE_FLOATING:
				return ("Float"); // This is because Java hates unreachable code, and break-after-return
			case CyAttributes.TYPE_INTEGER:
				return ("Integer"); // is unreachable.
			case CyAttributes.TYPE_STRING:
				return ("String"); // Interestingly, unreachable code is actually a COMPILE-TIME ERROR according to the Java spec:
			case CyAttributes.TYPE_COMPLEX:
				return ("Complex"); // * see http://docs.oracle.com/javase/specs/jls/se7/html/jls-14.html#jls-14.21
			case CyAttributes.TYPE_SIMPLE_MAP:
				return ("String");
			case CyAttributes.TYPE_UNDEFINED:
				return ("Undefined");
			default:
				return ("unsupported");
		}
	}

	private static int canonicalNameIndex(final String[] attrNamesToCheck) {
		// Returns the index of the FIRST occurrence of the canonical name, or -1 if that name was never found.
		for (int i = 0; i < attrNamesToCheck.length; i++) {
			if (attrNamesToCheck[i].equals(CANONICAL_NAME)) {
				return (i); // Find the index of CANONICAL_NAME in the attrNames
			}
		}
		return (-1); // Failure to find canonical name!
	}

	private static void writeRow(FileWriter writer, int nodeIndex, final CyAttributes attributes) throws IOException {
		final String nodeName = ((CyNode) Cytoscape.getRootGraph().getNode(nodeIndex)).getIdentifier();
		final String[] attrNames = attributes.getAttributeNames();
		final int canonicalCol = canonicalNameIndex(attrNames); // -1 if it was not found
		if (-1 == canonicalCol) { // If we STILL didn't find a canonicalName column, then that's maybe weird or something?
			CyLogger.getLogger().warn("DatasetAttributesWriter.java 'writeRow' function: The 'canonicalName' column was not found in the attributes table. This could be a problem, or it could just indicate an empty network was saved.");
		} else {
			// Ok, we've got a canonicalName column: let's deal with it!
			if (0 != canonicalCol) {
				// SWAP the columns so that canonicalName is first. Alex Williams: note that this actually RE-ORDERS things so that whatever was previously at the front is now located wherever canonicalName was.
				final String swapTemp = attrNames[0];
				attrNames[0] = attrNames[canonicalCol];
				attrNames[canonicalCol] = swapTemp;
			}
			for (int i = 0; i < attrNames.length; i++) {
				writer.write(((i == 0) ? "" : GenMAPPWorkspaces.DELIMITER_REGEXP) + getEncodedAttribute(attrNames[i], nodeName, attributes)); // No comma before the very first item, then commas BEFORE all the rest of them.
			}
			writer.write("\n");
		}
	}

	private static void writeHeader(FileWriter writer, final CyAttributes attributes) throws IOException {
		// Writes a couple of CSV-formatted header lines. CSV = comma-separated-value
		String[] attrNames = attributes.getAttributeNames();
		final int canonicalCol = canonicalNameIndex(attrNames); // -1 if it was not found
		if (canonicalCol == -1) {
			CyLogger.getLogger().warn("DatasetAttributesWriter.java 'writeHeader' function: The 'canonicalName' column was not found in the attributes table. This could be a problem, or it could just indicate an empty network was saved.");
		} else {
			if (canonicalCol != 0) {
				// SWAP the columns so that canonicalName is first.
				// Alex Williams: note that this actually RE-ORDERS things so that whatever was previously at the front is now located wherever canonicalName was
				final String swapTemp = attrNames[0];
				attrNames[0] = attrNames[canonicalCol];
				attrNames[canonicalCol] = swapTemp;
			}
			String namesRow = ""; // first line: attribute names
			String typesRow = ""; // second line: attribute types
			for (String attr : attrNames) {
				namesRow += ((namesRow.length() == 0) ? "" : GenMAPPWorkspaces.DELIMITER_REGEXP) + attr; // <-- Comma-separate every item AFTER the very first one.
				typesRow += ((typesRow.length() == 0) ? "" : GenMAPPWorkspaces.DELIMITER_REGEXP) + attributeTypeAsString(attributes.getType(attr)); // <-- Comma-separate every item AFTER the very first one.
			}
			writer.write(namesRow + "\n"); // Write the NAMES of the attributes on one line
			writer.write(typesRow + "\n"); // Then, below that, write the TYPES of the attributes (Boolean, String, etc)
		}
	}

	

	// Javadocs: http://chianti.ucsd.edu/Cyto-2_8_3/javadoc/index.html?cytoscape/CyMain.html
	public static void writeAttributesForOrphanNodesOnly(final CyAttributes nodeAttributes, final File attribFile, final CyLogger logger) throws IOException {
		// Added by Alex Williams: the idea is that this will ONLY save orphan nodes (ORPHAN nodes are ones that aren't in any network)
		// If we save the regular nodes AND ALSO let Cytoscape save the node attributes, that maybe causes problems? Unclear.
		if (0 == nodeAttributes.getAttributeNames().length) {
			// Skip writing attributes; there are none to write!
		} else {
			final FileWriter writer = new FileWriter(attribFile);
			final HashSet<Integer> orphanNodeIndexSet = GenMAPPWorkspaces.setOfOrphanNodeIndexes();		
			writeHeader(writer, nodeAttributes); // Writes two header lines. First line is the attribute NAME, second line is attribute TYPE
			for (final Integer thisNodeIndex : orphanNodeIndexSet) {
				writeRow(writer, thisNodeIndex.intValue(), nodeAttributes); // Write all the rows!
			}
			writer.close();
		}
	}
	
	public static void writeAttributes(final CyAttributes nodeAttributes, final File attribFile, final CyLogger logger) throws IOException {
		// Writes attributes for ALL nodes, even those that CYTOSCAPE also saves.
		// This could be a problem, since our fancy node-writer doesn't support simple_map or complex! So it is liable to BREAK if Cytoscape has nodes with those properties.
		// The solution to this would be to either:
		// - fix it to support simple_map and complex
		// - only save nodes we are sure aren't of those types. 
		// CyAttribute types: simple_map and complex are NOT supported
		if (nodeAttributes.getAttributeNames().length == 0) {
			// Skip writing attributes; there are none to write!
		} else {
			final FileWriter writer = new FileWriter(attribFile);
			writeHeader(writer, nodeAttributes); // Writes two header lines. First line is the attribute NAME, second line is attribute TYPE
			final int[] nodeIndexArr = Cytoscape.getRootGraph().getNodeIndicesArray();
			for (int i = 0; i < nodeIndexArr.length; i++) {
				writeRow(writer, nodeIndexArr[i], nodeAttributes);
			}
			writer.close();
		}
	}

}
