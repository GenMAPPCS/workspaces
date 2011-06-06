package org.genmapp.workspaces.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.generated.Node;
import cytoscape.plugin.PluginManager;
import java.io.IOException;
import java.io.BufferedReader;
import javax.swing.JOptionPane;
import org.biojava.bio.program.homologene.SimpleOrthoPairSet.Iterator;

// we wrap the core library of the same name to do the dirty work
public class CyAttributesWriter {

  // attributes are saved one column at a time but together into a single file
  // this separates attributes from one another
  public static String RECORD_DELIMITER = "~!@#$%^&*";

  
  public static String headerRowDummyNodeName = "CyAttributesWriter.ColumnHeader.DummyNode";
  

  public static void showMessage( String message )
  {
	//JOptionPane.showMessageDialog(  Cytoscape.getDesktop(), 
	//		message, 
	//		"", 
	//		JOptionPane.ERROR_MESSAGE ); 
  }
  public static void writeAttributes( CyAttributes attrib, File attribFile ) throws IOException
  {	
    int cols = attrib.getAttributeNames().length;
	showMessage( "write Attributes to " + attribFile + " cols = " + cols );
	
	
	{
	    String s = attrib.getAttributeNames() + "\n";
		  
		  for( Object node :  Cytoscape.getCyNodesList())
	      {		
			String nodeId = ( ( CyNode ) node ).getIdentifier();
			for ( String a : attrib.getAttributeNames() )
			{
			  s += attrib.getAttribute( nodeId, a ) + ",";
			}
			s += "\n";  
	      }
		  showMessage( s );
    }
	
	// the keys are node ids
    // the values are attribute values for that node
    // the first key is called "_DUMMY.CyAttributesWriter.ColumnHeader" and stores the attribute names in the columns
	java.util.HashMap< String, String[] > masterValues = new java.util.HashMap< String, String[]>();
	
	// store attr names on first row of masterValues table
	// we fix the ordering of the attribute names here
	// note that using a set iterator does not guarantee order so we cannot iterate this way
	//   in the future
	{
		int col = 0;
	
	    masterValues.put( headerRowDummyNodeName, new String[ cols ] );
	    showMessage( "setting header row" );

	    // 
		for( String n: attrib.getAttributeNames() )
		{
	       masterValues.get( headerRowDummyNodeName )[ col ] = n;
		   
		   col++;
		}
	}
	// criteriaSet/criteria were selected
	String pluginDir = PluginManager.getPluginManager()
			.getPluginManageDirectory().getCanonicalPath()
			+ "/Workspaces";
	if ( !new File( pluginDir ).exists() )
	{
		new File( pluginDir ).mkdir();
	}
	File tempFile = new File( pluginDir + "/dummy.temp" ); // XXX put in proper path
    showMessage( "tempFile = " + tempFile );
    
    tempFile.delete();
	
	// This code may seem very convoluted, but there is a logic here:
	// we want to reuse as much of the core Cytoscape.data.writers/readers CyAttributesReader/Writer code as possible.
	// This is preferable because the code of serializing various types of attributes ( not just strings, but potentially nested structures ) is complex.
	//
	// Those classes are designed with different goals in mind, and thus, they behave as follows:
	// - CyAttributesWriter writes the list of values of a single attribute to a file, starting with a header row that shows the attribute name and type;
	//   it then closes the output file
	// - CyAttributesReader reads the output format of the writer: note it only supports one attribute at a time
	//
	// What we want though is to read/write the entire slew of attributes to a column-delimited spreadsheet-style file.
	//
	// To do this, we go through each attribute one at a time, using the core classes to save each one to a temp file.  This allows us to 
	//    leverage the core classes to do the fancy footwork of converting attribute value objects into strings.
	//    We then read in the temp file manually as key value pairs, and store them in a big hash of arrays which serves as our csv file template.
	//    At the end, we serialize the hash of arrays.
	// 
    // In the future we will request that the core cytoscape readers/writers be much more flexible/accessible
    //    which would obviate this type of hack
  
    // attrCol = the column in the masterValues hash of columns that pertains to the current attribute
	for( int attrCol = 0; attrCol < cols; attrCol++ ) 
	{      
		String a = masterValues.get( headerRowDummyNodeName )[ attrCol ];
		showMessage( "writing attrib " + a );
    	// for some reason the cyattributewriter closes the file at the end of each write operation so we must reopen each time
		// write for append
		PrintWriter fw = new PrintWriter( new FileWriter( tempFile, true ) );
		cytoscape.data.writers.CyAttributesWriter w = new cytoscape.data.writers.CyAttributesWriter( attrib, a, fw );	    			
		w.writeAttributes();

		BufferedReader tempReader = new BufferedReader( new FileReader( tempFile ));
		
		String l = "";
		boolean bHeaderProcessed = false;
		while ((l = tempReader.readLine()) != null) 
		{
		    // we replace the attribute column header with a fuller one that includes type info
		    // this is needed by cytoscape.data.readers.CyAttributesReader to reconstruct attributes
		    if ( !bHeaderProcessed ) { 
				bHeaderProcessed = true; 		    
				showMessage( "replacing header for attrib " + a + "[ " + l + "]" );
			    masterValues.get( headerRowDummyNodeName )[ attrCol ] = l;
				continue; 
			} 
			 
			// format:  <nodeID> = <attribValue>
		    String[] s = l.split( " = " );
		    String nodeId = s[ 0 ];
		    String attrValue = s[ 1 ];
		    
		
		    if ( masterValues.get( nodeId ) == null )
		    {
		    	masterValues.put( nodeId, new String[ cols ] );
		    }
		    
		    //showMessage( "setting " + nodeId + ", " + attrCol + " = " + attrValue );
		    masterValues.get( nodeId )[ attrCol ] = attrValue;
		    //showMessage( "done setting " + nodeId + ", " + attrCol  + " = " + attrValue );

		}
		tempReader.close();
		showMessage( "done with attr " + a );
	    tempFile.delete();
	}

	// Now dump to csv file
	showMessage( "Dumpying csv to " + attribFile );
	{
	    String s = "[";
	
	    for ( String x : masterValues.get( headerRowDummyNodeName ) )
	    {  
	    	s += x + ",";
	    }
	    s+="]\n";
	  
	  for( String nodeId : masterValues.keySet() )
      {		
		s += nodeId + " - ";

		showMessage( "key: " + nodeId );  
  		if ( nodeId.equals( headerRowDummyNodeName ) )
		{
		  continue;
	    }
		for ( String a : attrib.getAttributeNames() )
		{
		  s += attrib.getAttribute( nodeId, a ) + ",";
		}
		s += "\n";  
      }
	  showMessage( s );
	}
	PrintWriter csvWriter = new PrintWriter( new FileWriter( attribFile ) );
	java.util.Iterator<String> i = masterValues.keySet().iterator();
	String nodeID = headerRowDummyNodeName;  // first, write the header to csv
	boolean bIsProcessingInitialHeader = true;
	while ( i.hasNext() )
	{
	  // processing a single 'row' of the masterValues hash
      if ( !bIsProcessingInitialHeader )
      {    	
		nodeID = ( String ) i.next(); // once done with the header, then iterate randomly over the node ids
   	    if ( nodeID.equals( headerRowDummyNodeName ) ) //|| nodeID.equals( "Source") || nodeID.equals( "Target" ) ) 
	    {   
    	  showMessage( "skip row for ID " + nodeID );
    	  continue; 
	    }
      }
      else
      {
    	  assert( nodeID.equals( headerRowDummyNodeName ) );
      }

      showMessage( "processing " + nodeID );
      String csvLine = "";      
      
      // iterate over all the columns of the given row
      for ( int col2 = 0; col2 < attrib.getAttributeNames().length; col2++ )
      {
    	  //showMessage( "getting " + nodeID + " col= " + col2 );
    	  String x = masterValues.get( nodeID )[ col2 ];
       	  //showMessage( "getting " + nodeID + " col= " + col2 + " = " + x );

    	  csvLine += x;
    	  if ( col2 < attrib.getAttributeNames().length - 1 )
    	  {
    		  csvLine += ",";
    	  }
      }
      csvWriter.println( csvLine  );
   
      bIsProcessingInitialHeader = false ;
	}
	csvWriter.close();
  }
}

