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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.attr.MultiHashMapDefinition;
import cytoscape.data.readers.EqnAttrTracker;
import cytoscape.data.writers.CyAttributesWriter;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.PluginManager;

import java.net.URLDecoder;
import java.text.MessageFormat;
import java.io.FileReader;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Popup;
import javax.swing.PopupFactory;
public class CyAttributesReader {
	public static final String DECODE_PROPERTY = "cytoscape.decode.attributes";
	private static final String badDecodeMessage =
		"Trouble when decoding attribute value, first occurence line no. {0}" +
		"\nIgnore if attributes file was created before 2.6.3 or wasn't creatad by Cytoscape." +
		"\nUse -Dcytoscape.decode.attributes=false when starting Cytoscape to turn off decoding.";

	private boolean badDecode;
	private int lineNum;
	private boolean doDecoding;
	final Map<String, Map<String, Class>> idsToAttribNameToTypeMapMap;
	private final CyLogger logger;


	CyAttributesReader() {
		logger = CyLogger.getLogger(CyAttributesReader.class);
		lineNum = 0;
		doDecoding = Boolean.valueOf(System.getProperty(DECODE_PROPERTY, "true"));
		idsToAttribNameToTypeMapMap = new HashMap<String, Map<String, Class>>();
	}
	public static void showMessage( String message )
	  {
		//JOptionPane.showMessageDialog(  Cytoscape.getDesktop(), 
		//		message, 
		//		"", 
		//		JOptionPane.ERROR_MESSAGE ); 
	  }
	public static void showMessageLarge( String message )
	{
      showMessage( "show message large" );
      JPanel panel = new JPanel();
	  //JScrollPane pane = new JScrollPane();
	  //pane.add( new JTextArea( message ) );
	  //pane.setVisible( true );
	  //pane.setSize( 400, 400 );
	  //panel.add( pane );
	  panel.setVisible( true );
	  panel.setSize( 400, 400 );
	  panel.add( new JButton( "test") );
      panel.add( new JTextArea( message ) );
      PopupFactory factory = PopupFactory.getSharedInstance();
	  Popup popup = factory.getPopup( Cytoscape.getDesktop(), panel, 100, 100);
	  popup.show(); 
      showMessage( "show message large end" );
	}
	// XXX - how do we handle null values?
	/**
	 *  DOCUMENT ME!
	 *
	 * @param cyAttrs DOCUMENT ME!
	 * @param fileIn DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
	public static void loadAttributes(final CyAttributes cyAttrs, final Reader fileIn) throws IOException 
	{
	  int row = 0;
	  java.util.HashMap< String, String[] > masterValues = new java.util.HashMap< String, String[]>();
		
	  BufferedReader fileReader = new BufferedReader( fileIn );
	  String line = null;
	  while( null != ( line = fileReader.readLine() ) )
	  {
	    String[] values = line.split( "," );
	    if ( row == 0 )
	    {
	    	masterValues.put( org.genmapp.workspaces.utils.CyAttributesWriter.headerRowDummyNodeName, values );
	    }
	    else
	    {
	    	// skip null value rows
	    	if ( values[ 0 ].equals( "null") ) { continue; }
	    	
	    	masterValues.put( values[ 0 ], values );
	    }
	    row++;
	  }
	  
	  String masterValuesContents = "";
	  for( String key : masterValues.keySet() )
	  {
		
		  masterValuesContents  += "[" + key + "]";
		  for ( String v : masterValues.get( key ) )
		  {
			  masterValuesContents  += v + ","; 
		 }
		  masterValuesContents  += "\n";
	  }
	  //showMessageLarge( masterValuesContents );		    
	  int numCols = masterValues.get( org.genmapp.workspaces.utils.CyAttributesWriter.headerRowDummyNodeName ).length;
	  // done reading file into mastervalues: now, write out a column at a time into a temp file
	  //   so that core CyAttrReader can read it ( yes, this qualifies as a hack )
	  // we are going through these convolutions because the attribute reading code is complex
	  //    and we want to reuse it as cleanly as we can.  Surprisingly this is the best way I see
	  for( int col = 0; col < numCols; col++ )
	  {
		showMessage( "processing " + masterValues.get( org.genmapp.workspaces.utils.CyAttributesWriter.headerRowDummyNodeName )[ col ] );

		// criteriaSet/criteria were selected
		String pluginDir = PluginManager.getPluginManager()
					.getPluginManageDirectory().getCanonicalPath()
					+ "/Workspaces";
		if ( !new File( pluginDir ).exists() )
		{
			new File( pluginDir ).mkdir();
		}

		File tempFile = new File( pluginDir + "/dummy.temp" ); // XXX put in proper path
		tempFile.delete();
		showMessage( "deleted tempfile" );
		
		PrintWriter tempWriter = new PrintWriter( new FileWriter( tempFile, false ) );
		// write it in format that cytoscape.data.readers.CyAttributeReader can read
		//
		tempWriter.println( masterValues.get( org.genmapp.workspaces.utils.CyAttributesWriter.headerRowDummyNodeName )[ col ] );
		
								  
		for( String nodeId : masterValues.keySet() )
		{		
			if ( nodeId.equals( org.genmapp.workspaces.utils.CyAttributesWriter.headerRowDummyNodeName ) )
			{
				continue;
			}
			tempWriter.println( nodeId + " = " + masterValues.get( nodeId )[ col ] );
		}
		tempWriter.close();
		FileReader tempReader = new FileReader( tempFile );
		
		try
		{
		cytoscape.data.readers.CyAttributesReader.loadAttributes( cyAttrs, tempReader );
		}
		catch( Exception e )
		{
			showMessage( "Error: " + e);
		}
	  }
	  showMessage( "displaying attributes");
	  String s = cyAttrs.getAttributeNames() + "\n";
	  
	  for( String nodeId : masterValues.keySet() )
      {		
  		if ( nodeId.equals( org.genmapp.workspaces.utils.CyAttributesWriter.headerRowDummyNodeName ) )
		{
		  continue;
	    }
		for ( String a : cyAttrs.getAttributeNames() )
		{
		  s += cyAttrs.getAttribute( nodeId, a ) + ",";
		}
		s += "\n";  
      }
	  //showMessageLarge( s );
	}
		      	
}