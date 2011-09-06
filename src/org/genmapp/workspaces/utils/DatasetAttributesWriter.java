package org.genmapp.workspaces.utils;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.giny.CytoscapeRootGraph;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.PluginManager;

// we wrap the core library of the same name to do the dirty work
public class DatasetAttributesWriter {

	public static String headerRowDummyNodeName = "CyAttributesWriter.ColumnHeader.DummyNode";
	
	private static String encode( String raw )  { return( raw ); } // XXX need to add in proper encoding from CyAttributesWriter

	private static String getEncodedAttribute(String attr, String nodeName,
			CyAttributes attributes) {
		byte type = attributes.getType(attr);
		if (!attributes.hasAttribute(nodeName, attr) || type ==
			CyAttributes.TYPE_SIMPLE_MAP || type == CyAttributes.TYPE_COMPLEX)
		{
		  return "";
		}
		switch (type) {
		  case CyAttributes.TYPE_STRING:
			 // Encode the string
		     return encode( ( String ) attributes.getAttribute(nodeName, attr));
		  case CyAttributes.TYPE_SIMPLE_LIST:
			 List attrList = attributes.getListAttribute(nodeName, attr);
			 return listEncode(attrList);
		  default:
			 return attributes.getAttribute(nodeName, attr).toString();
			 
		}
	}
	
	private static String listEncode(List list) 
	{
	    String s = null;
	    for (Object o: list) {
		   if (s == null)
		      s = "[" + encode(o.toString());
		   else
		      s += "::"+ encode(o.toString());
		 }
	     return s+"]";
	}

	public static void writeRow( FileWriter writer, int nodeIndex, CyAttributes attributes  ) throws IOException
	{
	    String nodeName = ((CyNode) Cytoscape.getRootGraph().getNode(nodeIndex)).getIdentifier();
	    String [] attrNames = attributes.getAttributeNames();
	    boolean isFirst = true;
	    for (String attr: attrNames) 
	    {
	    	String attrValue = getEncodedAttribute(attr, nodeName, attributes);
	    	if ( !isFirst ) { writer.write(","); }
	    	writer.write( attrValue);
	    	isFirst = false;
	    }
	    writer.write("\n");	
	}

	public static void writeHeader( FileWriter writer, CyAttributes attributes ) throws IOException
	{
		// format = csv
		// first line: attribute names
		String namesRow = "";
		// second line: attribute types
		String typesRow = "";
		
		String [] attrNames = attributes.getAttributeNames();
		boolean bIsFirstName = true;
		// first line: names
	    for (String attr: attrNames) 
	    {
	    	if ( !bIsFirstName ) 
	    	{ 
	    		namesRow += ","; 
	    		typesRow += ",";
	    	}
	    	else 
	    	{ 
	    		bIsFirstName = false; 
	    	}
	    	byte type = attributes.getType(attr);
	    	String typeStr = "";
	    	switch( type )
	    	{
		    	case CyAttributes.TYPE_BOOLEAN: typeStr += "Boolean"; break;
		    	case CyAttributes.TYPE_FLOATING: typeStr += "Float"; break;
		    	case CyAttributes.TYPE_INTEGER: typeStr += "Integer"; break;
		    	case CyAttributes.TYPE_STRING: typeStr += "String"; break;
		    	case CyAttributes.TYPE_COMPLEX: typeStr += "Complex"; break;
		    	case CyAttributes.TYPE_SIMPLE_MAP: typeStr += "String"; break;
		    	case CyAttributes.TYPE_UNDEFINED: typeStr += "Undefined"; break;
		    	default: typeStr += "unsupported"; break;
	    	}
	    	
	    	namesRow += attr;
	    	typesRow += typeStr;	   
	    }
	    writer.write( namesRow + "\n" );
	    writer.write( typesRow + "\n" );
	}
	
	// CyAttribute types: simple_map and complex are NOT supported
	public static void writeAttributes( CyAttributes nodeAttributes, File attribFile, CyLogger logger ) throws IOException 
	{
	    FileWriter writer = new FileWriter( attribFile );
	    CytoscapeRootGraph rootGraph = Cytoscape.getRootGraph();
	    writeHeader(writer, nodeAttributes);    // Two-line header.  First
	    										// line is name, second line is type
	    int[] nodes = rootGraph.getNodeIndicesArray();
	    for (int i = 0; i <nodes.length; i++) 
	    {
	      writeRow(writer, nodes[i], nodeAttributes);
	    }	
	    writer.close();
	}
	
	
	
	
	public static void writeAttributesHack(CyAttributes attrib, File attribFile,
			CyLogger logger) throws IOException {

		// only deal with dataset node attrs
		List<String> cytoattrs = Arrays.asList(attrib.getAttributeNames());
		List<String> dnattrs = new ArrayList<String>();
		for (CyDataset cd : CyDataset.datasetNameMap.values()) {
			for (String att : cd.getAttrs()) {
				if (cytoattrs.contains(att)) //make sure it's real
					dnattrs.add(att);
			}
		}
		dnattrs.add("canonicalName");
		
		// make unique
		Set<String> set = new HashSet<String>(dnattrs);
		ArrayList<String> dnattrsUnique = new ArrayList<String>(set);
		
		// make it an array
		String[] datasetNodeAttrib = (String[]) dnattrsUnique
				.toArray(new String[dnattrsUnique.size()]);

		int cols = datasetNodeAttrib.length;
		logger.debug("write Attributes to " + attribFile + " cols = " + cols);

		// generate list of network nodes to skip
		Set<String> allNetworkNodeIds = new HashSet<String>();
		for (CyNetwork network : Cytoscape.getNetworkSet()) {
			int[] networkNodeIndexes = network.getNodeIndicesArray();
			for (int i = 0; i < networkNodeIndexes.length; i++) {
				allNetworkNodeIds.add(((CyNode) Cytoscape.getRootGraph()
						.getNode(networkNodeIndexes[i])).getIdentifier());
			}
		}

		// the keys are node ids
		// the values are attribute values for that node
		// the first key is called "_DUMMY.CyAttributesWriter.ColumnHeader" and
		// stores the attribute names in the columns
		java.util.HashMap<String, String[]> masterValues = new java.util.HashMap<String, String[]>();

		// store attr names on first row of masterValues table
		// we fix the ordering of the attribute names here
		// note that using a set iterator does not guarantee order so we cannot
		// iterate this way down the road:
		// instead we will go off of the headerRowDummyNode in the masterValues
		// table to determine which col = which attributeName
		{
			int col = 0;

			masterValues.put(headerRowDummyNodeName, new String[cols]);
			logger.debug("setting header row");

			// store attr names on first row of masterValues table
			// we fix the ordering of the attribute names here
			// note that using a set iterator does not guarantee order so we
			// cannot
			// iterate this way
			// in the future
			for (String n : datasetNodeAttrib) {
				 if (n.equals("canonicalName") && col != 0) {
					// make sure "canonicalName" is first col: if it isn't, then
					// swap with what is there
					n = masterValues.get(headerRowDummyNodeName)[0];
					masterValues.get(headerRowDummyNodeName)[0] = "canonicalName";
				}
				masterValues.get(headerRowDummyNodeName)[col] = n;

				col++;
			}
		}

		// criteriaSet/criteria were selected
		String pluginDir = PluginManager.getPluginManager()
				.getPluginManageDirectory().getCanonicalPath()
				+ "/Workspaces";
		if (!new File(pluginDir).exists()) {
			new File(pluginDir).mkdir();
		}
		File tempFile = new File(pluginDir + "/dummy.temp"); // XXX put in
		// proper path
		logger.debug("tempFile = " + tempFile);

		tempFile.delete();

		// This code may seem very convoluted, but there is a logic here:
		// we want to reuse as much of the core Cytoscape.data.writers/readers
		// CyAttributesReader/Writer code as possible.
		// This is preferable because the code of serializing various types of
		// attributes ( not just strings, but potentially nested structures ) is
		// complex.
		//
		// Those classes are designed with different goals in mind, and thus,
		// they behave as follows:
		// - CyAttributesWriter writes the list of values of a single attribute
		// to a file, starting with a header row that shows the attribute name
		// and type;
		// it then closes the output file
		// - CyAttributesReader reads the output format of the writer: note it
		// only supports one attribute at a time
		//
		// What we want though is to read/write the entire slew of attributes to
		// a column-delimited spreadsheet-style file.
		//
		// To do this, we go through each attribute one at a time, using the
		// core classes to save each one to a temp file. This allows us to
		// leverage the core classes to do the fancy footwork of converting
		// attribute value objects into strings.
		// We then read in the temp file manually as key value pairs, and store
		// them in a big hash of arrays which serves as our csv file template.
		// At the end, we serialize the hash of arrays.
		// 
		// In the future we will request that the core cytoscape readers/writers
		// be much more flexible/accessible
		// which would obviate this type of hack

		// attrCol = the column in the masterValues hash of columns that
		// pertains to the current attribute
		for (int attrCol = 0; attrCol < cols; attrCol++) {
			String a = masterValues.get(headerRowDummyNodeName)[attrCol];
			logger.debug("writing attrib " + a);
			// for some reason the cyattributewriter closes the file at the end
			// of each write operation so we must reopen each time
			// write for append
			PrintWriter fw = new PrintWriter(new FileWriter(tempFile, true));
			cytoscape.data.writers.CyAttributesWriter w = new cytoscape.data.writers.CyAttributesWriter(
					attrib, a, fw);
			w.writeAttributes();
			BufferedReader tempReader = new BufferedReader(new FileReader(
					tempFile));
			String l = "";
			boolean bHeaderProcessed = false;
			while ((l = tempReader.readLine()) != null) {
				// we replace the attribute column header with a fuller one that
				// includes type info
				// this is needed by cytoscape.data.readers.CyAttributesReader
				// to reconstruct attributes
				if (!bHeaderProcessed) {
					bHeaderProcessed = true;
					logger.debug("replacing header for attrib " + a + "[ " + l
							+ "]");
					masterValues.get(headerRowDummyNodeName)[attrCol] = l;
					continue;
				}

				// format: <nodeID> = <attribValue>
				String[] s = l.split(" = ");
				// skip blank entries
				if (s.length != 2)
					continue;
				String nodeId = s[0];
				String attrValue = s[1];

				/*
				 * All the various reasons to skip saving this node's
				 * attributes:
				 * 
				 * 1. its in a network and therefore already being saved by
				 * xgmml
				 * 
				 * 2. its not a dataset node (e.g., some funky GPML hidden
				 * node). These will not have a node attr tagged with a valid
				 * dataset name, or they might not have the attr at all.
				 */
				if (allNetworkNodeIds.contains(nodeId))
					continue;
				if (!attrib.hasAttribute(nodeId,
						DatasetMapping.NET_ATTR_DATASETS))
					continue;
				List<String> dlist = (List<String>) attrib.getListAttribute(
						nodeId, DatasetMapping.NET_ATTR_DATASETS);
				if (null == dlist)
					continue;
				if (CyDataset.datasetNameMap.isEmpty())
					continue;
				boolean noMatches = true;
				for (String d : dlist) {
					if (CyDataset.datasetNameMap.keySet().contains(d))
						noMatches = false;
				}
				if (noMatches)
					continue;

				// get back to filling masterValues
				if (masterValues.get(nodeId) == null)
					masterValues.put(nodeId, new String[cols]);

				masterValues.get(nodeId)[attrCol] = attrValue;
			}

			tempReader.close();
			logger.debug("done with attr " + a);
			tempFile.delete();
		}

		// Now dump to csv file
		logger.debug("Dumpying csv to " + attribFile);
		{
			String s = "[";

			for (String x : masterValues.get(headerRowDummyNodeName)) {
				s += x + ",";
			}
			s += "]\n";

			for (String nodeId : masterValues.keySet()) {
				s += nodeId + " - ";

				// logger.debug("key: " + nodeId);
				if (nodeId.equals(headerRowDummyNodeName)) {
					continue;
				}
				for (String a : datasetNodeAttrib) {
					s += attrib.getAttribute(nodeId, a) + ",";
				}
				s += "\n";
			}
			logger.debug(s);
		}
		PrintWriter csvWriter = new PrintWriter(new FileWriter(attribFile));
		java.util.Iterator<String> i = masterValues.keySet().iterator();
		String nodeID = headerRowDummyNodeName; // first, write the header to
		// csv
		boolean bIsProcessingInitialHeader = true;
		logger.debug("printing to cvsWriter...");
		while (i.hasNext()) {
			// processing a single 'row' of the masterValues hash
			if (!bIsProcessingInitialHeader) {
				nodeID = (String) i.next(); // once done with the header, then
				// iterate randomly over the node
				// ids
				if (nodeID.equals(headerRowDummyNodeName)) // || nodeID.equals(
				// "Source") ||
				// nodeID.equals(
				// "Target" ) )
				{
					// logger.debug("skip row for ID " + nodeID);
					continue;
				}
			} else {
				assert (nodeID.equals(headerRowDummyNodeName));
			}

			// logger.debug("processing " + nodeID);
			String csvLine = "";

			// iterate over all the columns of the given row
			for (int col2 = 0; col2 < datasetNodeAttrib.length; col2++) {
				// logger.debug( "getting " + nodeID + " col= " + col2 );
				String x = masterValues.get(nodeID)[col2];
				// logger.debug( "getting " + nodeID + " col= " + col2 + " = " +
				// x );

				csvLine += x;
				if (col2 < datasetNodeAttrib.length - 1) {
					csvLine += ",";
				}
			}
			csvWriter.println(csvLine);

			bIsProcessingInitialHeader = false;
		}
		csvWriter.close();
	}
}
