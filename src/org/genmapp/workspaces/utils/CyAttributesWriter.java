package org.genmapp.workspaces.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import cytoscape.data.CyAttributes;

// we wrap the core library of the same name to do the dirty work
public class CyAttributesWriter {

  // attributes are saved one column at a time but together into a single file
  // this separates attributes from one another
  public static String RECORD_DELIMITER = "~!@#$%^&*";

  public static void writeAttributes( CyAttributes attrib, File attribFile ) throws IOException
  {
		org.genmapp.workspaces.GenMAPPWorkspaces.showMessage( "delimited write attrib" );
    
		PrintWriter fw = new PrintWriter( new FileWriter( attribFile, false ) ); // erases existing file first
    	fw.close();
		for( String aName : attrib.getAttributeNames() )
		{
			//if ( aName.equals( "hiddenLabel" ) ) { continue; }
	    	// for some reason the cyattributewriter closes the file at the end of each write operation so we must reopen each time
			// write for append
			fw = new PrintWriter( new FileWriter( attribFile, true ) );
			
			cytoscape.data.writers.CyAttributesWriter w = new cytoscape.data.writers.CyAttributesWriter( attrib, aName, fw );	    			
			w.writeAttributes();
			
			fw = new PrintWriter( new FileWriter( attribFile, true ) );
			fw.println( RECORD_DELIMITER );
			fw.close();
		}
  }
}
