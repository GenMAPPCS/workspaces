/*******************************************************************************
 * Copyright 2010 Alexander Pico
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.genmapp.workspaces;

import java.io.FileReader;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import cytoscape.CyNode;
import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyCriteria;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.tree.WorkspacesPanel;

import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.data.CyAttributes;
import java.io.FileWriter;
import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.io.File;
import javax.swing.JOptionPane;
import org.genmapp.workspaces.utils.CyAttributesWriter;
import cytoscape.data.attr.CountedIterator;
import org.genmapp.workspaces.utils.CyAttributesReader;

public class GenMAPPWorkspaces extends CytoscapePlugin {
 
	public static WorkspacesPanel wsPanel;
	public static void showMessage(String message) 
	{	
		// JOptionPane.showMessageDialog( Cytoscape.getDesktop(), message, "",
		// 	JOptionPane.ERROR_MESSAGE );
	}
	final static String propFileName = "GenMAPPWorkspaces.props";
	final static String nodeAttributeFileName = "GenMAPPWorkspaces.nodeAttr";
	
	public void restoreSessionState( List< File > fileList )
	{
		showMessage( "loadSessionState" );
		final String tmpDir = System.getProperty("java.io.tmpdir");
		File propFile = null;
		File nodeAttributeFile = null;
		
		try
		{
			for( File f : fileList )
			{
				showMessage( f.getName() );
				if ( f.getName().contains( propFileName ) )
				{
					showMessage( "propfile = " + f );
					propFile = f;
				}
				else if ( f.getName().contains( nodeAttributeFileName ) )
				{
					showMessage( "node attribute file = " + f );
					nodeAttributeFile = f;
				}
			}
			
			// first process the node attribute file so as to populate missing RootGraph nodes
			// properties file for GenMAPPWorkspaces
			
			// this will read the properties directly into the Cytoscape node attributes structure, (hopefully) overwriting
			//    any duplicates			
			CyAttributesReader.loadAttributes( Cytoscape.getNodeAttributes(), new FileReader( nodeAttributeFile ) );
			
			
			// ***** for debugging: display a mini table of node attributes *****
			CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
			String s = "object keys: ";
			HashMap< String, String > display = new HashMap< String, String >();
			for( String n : nodeAttr.getAttributeNames() )
			{				
				CountedIterator i = nodeAttr.getMultiHashMap().getObjectKeys( n );
				while ( i.hasNext() )
				{
					String id = ( String ) i.next();
					String existingStr = "";
					if ( display.get( id ) != null )
					{
						existingStr = ( String ) display.get( id );
					}
					
					existingStr += nodeAttr.getAttribute( id, n ) + "---";
					display.put( id, existingStr );
				}
			}
			
			String displayStr = "";
			for( String k : display.keySet() )
			{
				displayStr += display.get( k ) + "\n";
			}
			
			showMessage( "DISPLAY: " + displayStr );
			// **************** end debugging display code *******************
			
			
			// create actual CyNodes, and implicitly their Nodes 
			// no explicit mapping needs to be done as it has been handled already by loading of all the nodeAttributes
			for( String nodeID : display.keySet() )
			{
				Cytoscape.getCyNode( nodeID, true );
			}			
			
			
			showMessage( "loading CyDatasets" );

			// now, you can create your CyDatasets
			Properties props = new Properties();
			props.load( new FileReader( propFile ) );
					
			int numCyDatasets = new Integer( props.getProperty( "numCyDatasets" ) ).intValue();
			showMessage( "numCyDatasets:" + numCyDatasets );

			Map< String, CyDataset > datasetNameMap = new HashMap< String, CyDataset >();
			for( int i = 0; i < numCyDatasets; i++ )
			{
				String name = props.getProperty( "ds." + i + ".name" );
				String keyType = props.getProperty( "ds." + i + ".keyType" );
				String nodeIdListString = props.getProperty( "ds." + i + ".nodeIdList" );
				List< String > attrList = CyDataset.attrStringToAttrList( props.getProperty( "ds." + i + ".attrList" ) );
				
				String[] nodeIdsAsStr = nodeIdListString.split( "," );
				List< Integer > nodeRootIdList = new ArrayList< Integer >();
				for( String cyNodeID : nodeIdsAsStr )
				{
					
					nodeRootIdList.add( Cytoscape.getCyNode( cyNodeID ).getRootGraphIndex() );
				}
				showMessage( nodeRootIdList + "" );
				// create a new CyDataset object, but with no automatic mapping ( that has been done already at the attribute level )
				CyDataset ds = new CyDataset( name, keyType, nodeRootIdList, attrList, false );
				showMessage( name );
				datasetNameMap.put( name, ds );
			}
			showMessage( "replacing cydataset map" );
			CyDataset.datasetNameMap = datasetNameMap;

		}
		catch( Exception e )
		{
			showMessage( "Exception: " + e );
		}
		
		//Load Criteriasets
		showMessage( "Isaac: 2");
		// call Workspaces-specific code for handling the opening of sessions
		// at this point, all the criteria-related mapping has taken been loaded up 
		// only thing left to do is update the CriteriaPanel
		
		// get all the criteriaSets ourselves from the session-level properties
		//   we don't trust the criteriamapper cycommand results b/c they are reported on a per-network basis
		//   and thus don't include those not mapped to any network
		String [] vCs = CyCriteria.getCriteriaSets();
		for( String cs : vCs )
		{
			
			Map< String, Object > args = new HashMap();
			args.put( WorkspacesCommandHandler.ARG_SETNAME, cs );
			
			try
			{
				showMessage( "Isaac: 3 + update criteriasets " + args + "[" + args.size() + "]"); 
			  CyCommandManager.execute( "workspaces", "update criteriasets", 
					args );
		    }
			catch( CyCommandException ex )
			{
				showMessage( "error" );
				showMessage( "error: " + ex.toString() );
			}
		}
		
		
	}
	public void saveSessionStateFiles( List< File > fileList )
	{
		showMessage( "saveSessionState" );
		
		final String tmpDir = System.getProperty("java.io.tmpdir");
	    final File propFile = new File(tmpDir, propFileName );	    
	    final File nodeAttributeFile = new File(tmpDir, nodeAttributeFileName);
	    try {
	        Properties props = new Properties();
	        
	        // Write the first file:
	        // CyDataset member info
	        
	        // for each dataset, write ds_name, ds_keyType ( cannot be found elsewhere? ), list of nodeids
	        props.setProperty( "numCyDatasets", CyDataset.datasetNameMap.keySet().size() + "" );
	        int i = 0;
	    	for( String name : CyDataset.datasetNameMap.keySet() )
	    	{
	    		CyDataset ds = ( CyDataset ) CyDataset.datasetNameMap.get( name );
	    		props.setProperty( "ds." + i + ".name", name );
	    		props.setProperty( "ds." + i + ".keyType", ds.getKeyType() );
	    		
	    		String nodeIdListString = "";
	    		for ( Integer rootID : ds.getNodes() )
	    		{
	    			String cyNodeID = ( ( CyNode ) Cytoscape.getRootGraph().getNode( rootID ) ).getIdentifier();
	    			nodeIdListString += cyNodeID + ",";
	    		}
	    		props.setProperty( "ds." + i + ".attrList", ds.getAttrString() );
	    		props.setProperty( "ds." + i + ".nodeIdList", nodeIdListString );
	    	}
	    	// writes property file to disk
	        props.store( new FileOutputStream(propFile), null );
	        fileList.add( propFile );
	     				    
	        // Now write the second file
	        //   this one stores all the node attributes for all nodes in the rootgraph
	    	// This is needed because the rootgraph is not fully stored in the session:  in particular, any nodes
	    	//    that belong to no networks will not be stored.  The problem is that these nodes may belong to CyDatasets
	    	//    so we must store them ourselves.  
	    	// To keep things simple ( though inefficient ), we simply store all the nodes in the rootgraph and allow mergers/collisions to happen naturally.
	    	
	        CyAttributesWriter.writeAttributes( Cytoscape.getNodeAttributes(), nodeAttributeFile );
	    	fileList.add( nodeAttributeFile );
	    	
	    } catch (IOException ex) {
	        showMessage( "saveSessionState: IOException " + ex );

	    }
		showMessage( "done" );
	}
	
	
	public GenMAPPWorkspaces() {

		//create workspaces panel
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(
				SwingConstants.WEST);
		
		wsPanel = new WorkspacesPanel();

		cytoPanel.add("GenMAPP-CS", new ImageIcon(getClass().getResource("images/genmappcs.png")), wsPanel, "Workspaces Panel", 0);	
		cytoPanel.setSelectedIndex(0);	
		//cytoPanel.remove(1);
		
		//cycommands
		new WorkspacesCommandHandler();
	}

}