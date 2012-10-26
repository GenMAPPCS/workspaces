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

import java.util.HashSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
//import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.tree.WorkspacesPanel;
import org.genmapp.workspaces.utils.DatasetAttributesReader;
import org.genmapp.workspaces.utils.DatasetAttributesWriter;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.cytoscape.GpmlPlugin;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
//import cytoscape.data.CyAttributes;
//import cytoscape.data.attr.CountedIterator;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.visual.VisualPropertyDependency;

public class GenMAPPWorkspaces extends CytoscapePlugin {

	public static final String DELIMITER_STRING = " ||--GENMAPP--|| "; // Note: this gets used in DatasetAttributesReader as well!

	public static WorkspacesPanel wsPanel;
	private CyLogger logger;
	final static String propFileName = "GenMAPPWorkspaces.props";
	final static String nodeAttributeFileName = "GenMAPPWorkspaces.nodeAttr";
	public static final String ATTR_PATHWAY_URL = "wikipathways.url";

	private static final String CSET_PREFIX = "cs.";
	private static final String PROP_DS = "ds.";
	private static final String KEY_TYPE = ".keyType";
	private static final String PROP_NAME = ".name";
	private static final String NODE_ID_LIST = ".nodeIdList";
	private static final String ATTR_LIST = ".attrList";

	public GenMAPPWorkspaces() {
		
		logger = CyLogger.getLogger(GenMAPPWorkspaces.class);
		logger.setDebug(true);

		logger.error("ALEX WILLIAMS: testing genmapp load/save");

		CytoPanel cytoPanel1 = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST); // Create workspaces panel
		wsPanel = new WorkspacesPanel(logger);
		cytoPanel1.add("GenMAPP-CS", new ImageIcon(getClass().getResource("images/genmappcs.png")), wsPanel, "Workspaces Panel", 0);
		cytoPanel1.setSelectedIndex(0);
		// cytoPanel.remove(1);
		CytoscapeInit.getProperties().setProperty("viewThreshold", "30000");
		CytoscapeInit.getProperties().setProperty("render.coarseDetailThreshold", "100000");
		CytoscapeInit.getProperties().setProperty("render.nodeLabelThreshold", "30000");

		// set default node width/height lock to avoid dependency issues
		Cytoscape.getVisualMappingManager().getVisualStyle().getDependency().set(VisualPropertyDependency.Definition.NODE_SIZE_LOCKED, false);

		// Clear out all org.genmapp.criteriaset properties that may have been "saved as default." This happens right after the plugin is loaded,
		// well before properties are added from session files. This way we can utilize props for storing criteriasets with sessions without allowing
		// them to be recalled from .cytoscape/cytoscape.props
		final Properties props = CytoscapeInit.getProperties();
		if (props.containsKey(WorkspacesCommandHandler.PROPERTY_SETS)) {
			CytoscapeInit.getProperties().remove(WorkspacesCommandHandler.PROPERTY_SETS);
			List<String> keylist = new ArrayList<String>();
			for (Object key : props.keySet()) {
				if (((String) key).startsWith(WorkspacesCommandHandler.PROPERTY_SET_PREFIX)) {
					keylist.add((String) key);
				}
			}
			logger.debug("Clearing org.genmapp.criteriaset properties from saved defaults...");
			for (final String key : keylist) {
				CytoscapeInit.getProperties().remove(key);
				logger.debug(key + " removed");
			}
		}
		// CyCommands
		new WorkspacesCommandHandler(logger);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cytoscape.plugin.CytoscapePlugin#restoreSessionState(java.util.List)
	 */
	public void restoreSessionState(List<File> fileList) {

		logger.error("ALEX WILLIAMS: RETURNING EARLY FROM RESTORE SESSION STATE. NOT ACTUALLY RESTORING SESSION IN WORKSPACE PLUGIN");

		int TESTAGW = 1;
		if (TESTAGW == 1) {
			return;
		}

		logger.debug("loadSessionState");
		try {
			File nodeAttributeFile = null; // <-- There's only exactly ONE of these files
			File propFile = null; // <-- There's only exactly ONE of these files
			List<File> gpmlFileList = new ArrayList<File>(); // <-- Can be MANY of these files!
			for (final File f : fileList) {
				// Go through the files to restore, and figure out which files are which.
				if (f.getName().contains(propFileName)) {
					logger.debug("propfile = " + f);
					propFile = f;
				} else if (f.getName().contains(nodeAttributeFileName)) {
					logger.debug("node attribute file = " + f);
					nodeAttributeFile = f;
				} else if (f.getName().endsWith(".gpml")) {
					logger.debug("gpml file = " + f);
					gpmlFileList.add(f);
				} else {
					logger.warn("Ignored the file <" + f.getName() + ">. We couldn't determine if it was a GPML file, a property file, or an attribute file! So we skipped it.");
				}
			}

			if (nodeAttributeFile == null) {
				logger.error("ERROR 'ATTRIBUTE FILE NOT FOUND': We were not able to find a node attributes file! We were looking for a file with the name <" + nodeAttributeFileName + ">.");
			}
			if (propFile == null) {
				logger.error("ERROR 'PROP FILE NOT FOUND': We were not able to find a properties file! We were hoping to find one with the name <" + propFileName + ">.");
			}

			// Ok, now we've figured out which files are:
			// * node attribute files (in noteAttributeFile -- only one of these at most!)
			// * properties files (in propFile -- only one of these at most!)
			// * GPML files (in gpmlFileList)

			final GpmlPlugin gp = GpmlPlugin.getInstance();
			// Load GPML files
			if (null == gp) {
				logger.warn("WARNING OF TYPE 'GPML PLUGIN MISSING': The GPML plugin was not detected. As a result, the GPML networks were SKIPPED, and not successfully restored from the save file.");
			} else {
				for (final File gpmlFile : gpmlFileList) {
					final String gpmlNetworkTitle = gpmlFile.getName().replaceFirst("GenMAPPWorkspaces_", "").replaceAll(".gpml", "");
					// JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "GPML pathway graphics will not be saved/restored.\nThis feature will be support in future versions of GenMAPP-CS.", "Detected GPML: " + title, JOptionPane.WARNING_MESSAGE);
					// if GPML plugin is loaded, then attempt to load the (previously saved, if any) pathway
					try {
						Cytoscape.destroyNetwork(gpmlNetworkTitle); // <-- First, destroy the OLD network, if one with the same name exists
						// Now, we're clear to add a new network with the GPML name.
						Pathway pathway = new Pathway();
						pathway.readFromXml(gpmlFile, true);
						CyNetwork gpmlNet = gp.load(pathway, true);
						gpmlNet.setTitle(gpmlNetworkTitle); // Set the NEW network to have the name of the GPLM pathway
						// Ok, now we've made a new gpml network

						List<String> netlist = new ArrayList<String>();
						netlist.add(gpmlNet.getIdentifier());

						Cytoscape.setSelectedNetworks(netlist);
						WorkspacesPanel.getNetworkTreePanel().updateTitle(gpmlNet);

						Cytoscape.getNetworkAttributes().setAttribute(gpmlNet.getIdentifier(), ATTR_PATHWAY_URL, gpmlFile.getPath());

						// And let the world know. The Object[2] is Cytoscape convention
						// Object[] new_value = new Object[2];
						// new_value[0] = net;
						// new_value[1] = net.getIdentifier();
						// Cytoscape.firePropertyChange(Cytoscape.NETWORK_LOADED, null, new_value);

						logger.info("No problems in restoring GPML file [" + gpmlFile.getName() + "] into a new network named [" + gpmlNetworkTitle + "].");
					} catch (ConverterException e) {
						logger.error("Error reading GPML file [" + gpmlFile.getName() + "]: " + e);
					}

				}
			}

			// Ok, guess we're done with the GPML plugins!
			// first process the node attribute file so as to populate missing
			// RootGraph nodes properties file for GenMAPPWorkspaces
			// this will read the properties directly into the Cytoscape nodeAttributes structure, (hopefully) overwriting any duplicates

			logger.error("restoreSessionState: [Starting] loading node attributes for CyDataset nodes...");
			logger.error("Alex Williams: problem tracked down to this location!");
			// There is a problem here!
			DatasetAttributesReader.loadAttributes(Cytoscape.getNodeAttributes(), new FileReader(nodeAttributeFile), logger);
			logger.error("restoreSessionState: [Done] Loading node attributes for CyDataset nodes");

			/*
			 * if (logger.isDebugging()) { // END OF DEBUGGING
			 * final CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
			 * HashMap<String, String> display = new HashMap<String, String>();
			 * for (String n : nodeAttr.getAttributeNames()) {
			 * CountedIterator i = nodeAttr.getMultiHashMap().getObjectKeys(n);
			 * while (i.hasNext()) {
			 * final String id = (String) i.next();
			 * boolean displayIdIsNull = (display.get(id) != null);
			 * String theStr = ((displayIdIsNull) ? "" : display.get(id)) + nodeAttr.getAttribute(id, n) + "---";
			 * display.put(id, theStr);
			 * }
			 * }
			 * String displayStr = "";
			 * for (String k : display.keySet()) {
			 * displayStr += display.get(k) + "\n";
			 * }
			 * logger.debug("DISPLAY: " + displayStr);
			 * } // END OF DEBUGGING
			 */

			// now, you can create your CyDatasets
			Properties props = new Properties();
			props.load(new FileReader(propFile)); // <-- apparently this is some method that reads properties from the file

			final int numCyDatasets = new Integer(props.getProperty("numCyDatasets")).intValue(); // This is an integer that should have been found in the saved "<something>.props" file
			logger.debug("Loading " + numCyDatasets + " CyDatasets (that's how many the input .props file said there would be, at least)");
			for (int i = 0; i < numCyDatasets; i++) {
				final String name = props.getProperty(PROP_DS + i + PROP_NAME);
				final String keyType = props.getProperty(PROP_DS + i + KEY_TYPE);
				final String nodeIdListString = props.getProperty(PROP_DS + i + NODE_ID_LIST);
				final List<String> attrList = CyDataset.attrStringToAttrList(props.getProperty(PROP_DS + i + ATTR_LIST));
				final String[] nodeIdsAsStr = nodeIdListString.split(DELIMITER_STRING);
				final List<Integer> nodeRootIdList = new ArrayList<Integer>();
				for (final String cyNodeID : nodeIdsAsStr) {
					nodeRootIdList.add(Cytoscape.getCyNode(cyNodeID, true).getRootGraphIndex());
				}
				logger.debug(nodeRootIdList + "");
				// Create a new CyDataset object, but with no automatic mapping
				// since that has already been done and loaded directly as node attributes.
				new CyDataset(name, keyType, nodeRootIdList, attrList, false, logger);
				logger.info("CyDataset " + name + " restored from session file.");
			}

			// Load Criteria sets
			logger.debug("loading CyCriteriasets");
			final String setsString = CytoscapeInit.getProperties().getProperty(WorkspacesCommandHandler.PROPERTY_SETS);
			// extract cset names
			// String[] csetList = { "" };
			ArrayList<String> full = new ArrayList<String>();
			if (null != setsString && setsString.length() > 2) {
				// no clue what should happen if the length is 1... maybe that doesn't happen?
				// Ha, this is a hackish way to get the elements out of the "[" and "]" delimiting!
				// setsString really IS just a string!
				final String setsStringWithoutFirstAndLastElements = setsString.substring(1, setsString.length() - 1); // umm... no clue what's up here. Looks like we are getting rid of the first and last element for some reason.
				final String[] splitUpStringArray = setsStringWithoutFirstAndLastElements.split("\\]\\[");
				for (String cs : splitUpStringArray) {
					full.add(cs);
					logger.debug(cs + " to be restored...");
				}
			}
			// create csets
			for (final String csetname : full) {
				final String setParameters = CytoscapeInit.getProperties().getProperty(WorkspacesCommandHandler.PROPERTY_SET_PREFIX + csetname);
				new CyCriteriaset(csetname, setParameters);
				logger.info("CyCriteriaset " + csetname + " restored from session file.");
			}
			// restore network-criteria map from prop file
			for (final CyNetwork net : Cytoscape.getNetworkSet()) {
				final String csetname = props.getProperty(CSET_PREFIX + net.getTitle());
				CyCriteriaset cset = CyCriteriaset.criteriaNameMap.get(csetname);
				// track last applied cset per network
				CyCriteriaset.setNetworkCriteriaset(net, cset);
				logger.debug("setting " + csetname + " for " + net.getTitle());
				// apply them after SESSION_LOADED
			}
			// end try read files
		} catch (Exception e) {
			logger.warn("WARNING: restoreSessionState Exception: " + e);
			// Uh... looks like this swallows up all the exceptions? Hmm. Might make debugging hard!
		}
		logger.debug("done with all session restoring tasks");
	}

	private void writePropFile(final Properties theProperties, final File thePropFile, List<File> theParentFileList) {
		try {
			// Write the FIRST file (propFile): CyDataset member info
			// for each dataset, write ds_name, ds_keyType ( cannot be found elsewhere? ), list of nodeids
			logger.debug("writePropFile: writing props to file: " + thePropFile.getName());
			theProperties.store(new FileOutputStream(thePropFile), null); // writes property file to disk
			theParentFileList.add(thePropFile); // <-- Tell GenMAPP / Cytoscape about this new file!
		} catch (IOException ex) {
			logger.debug("writePropFile: saveSessionState: IOException " + ex);
		}
	}

	private void writeNodeAttrFile(final File nodeAttrFile, List<File> theParentFileList) {
		try {
			// Write the SECOND file (nodeAttributeFile)
			// Now write the second file: this one stores all the node attributes for all nodes in the rootgraph.
			// This is needed because the rootgraph is not fully stored in the session: in particular, any nodes that belong to NO networks will not be stored.
			// The problem is that these nodes may belong to CyDatasets---so we must store them ourselves (Cytoscape won't do it, since they are not in any networks).
			// To keep things simple (though with some redundancy), we simply store ALL the nodes in the rootgraph and allow mergers/collisions to happen.
			logger.debug("writeNodeAttrFile: writing node attributes to the file " + nodeAttrFile + "");
			DatasetAttributesWriter.writeAttributes(Cytoscape.getNodeAttributes(), nodeAttrFile, logger);
			theParentFileList.add(nodeAttrFile); // Tell GenMAPP / Cytoscape about this new file!
			logger.debug("writeNodeAttrFile: writing node attributes was APPARENTLY successful.");
		} catch (IOException ex) {
			logger.debug("writeNodeAttrFile: saveSessionState: IOException " + ex);
		}
	}

	private void writeGPMLFiles(final String theTempDir, List<File> theParentFileList) {
		// Save GPMLs: The GPML files contain the data that is used to make the WikiPathways images with annotation / lines / boxes / etc
		// on top of the graph. It goes beyond the Cytoscape 2.x "nodes & edges only" saving.
		for (final CyNetwork net : Cytoscape.getNetworkSet()) {
			final String wpurl = Cytoscape.getNetworkAttributes().getStringAttribute(net.getIdentifier(), ATTR_PATHWAY_URL);
			if (null != wpurl) {
				final GpmlPlugin gp = GpmlPlugin.getInstance();
				// Old message indicating that this stuff DIDN'T WORK: JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "GPML pathway graphics will not be saved/restored.\nThis feature will be support in future versions of GenMAPP-CS.", "Detected GPML: " + net.getTitle(),
				// JOptionPane.WARNING_MESSAGE);
				if (null != gp) { // If GPML plugin is loaded, then attempt save pathway
					final File gpmlFile = new File(theTempDir, net.getTitle() + ".gpml"); // <-- Network names are SUPPOSED to be UNIQUE! Cytoscape enforces this in network renaming, but not elsewhere.
					try {
						gp.writeToFile(Cytoscape.getNetworkView(net.getIdentifier()), gpmlFile);
						theParentFileList.add(gpmlFile); // <-- Tell GenMAPP / Cytoscape about this new file!
						logger.debug("writeGPMLFiles: Saved the GPML file \"" + gpmlFile.getName() + "\"");
					} catch (ConverterException e) { // Requires import org.pathvisio.core.model.ConverterException
						logger.error("writeGPMLFiles: FAILURE ENCOUNTERED when trying to save the GPML file named " + gpmlFile.getName() + ". The ConverterException was: " + e);
					}
				} else {
					logger.warn("writeGPMLFiles: Decided to skip a GPML document for the network " + net.getTitle() + " in the session file, because 'gp' was null");
				}
			}
		}
		// Alex Williams: TODO: Check to see if weird invalid characters are allowed in network names: answer, they ARE!!!
		// Alex Williams: You can actually mess up file saving by having really strange network names:
		// - Example: Make a network named "\n\n\\\::\\:"
		// - Then make a network named "1n1n11133113"
		// When you try to save, there will be a problem, because those network names, while unique to Cytoscape, are mapped to the same files!
		// (Specifically, you will get a java.util.zip.ZipException error with "duplicate entry" as the message.)
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cytoscape.plugin.CytoscapePlugin#saveSessionStateFiles(java.util.List)
	 */
	public void saveSessionStateFiles(List<File> fileList) {
		// Alex Williams: Note: it may initially seem WEIRD that these files are written to a TEMP directory and (seemingly) not to any real location---
		// but actually, these are just TEMPORARILY saved in the temp dir, and then copied to the final save file by Cytoscape, where the files are zipped up.
		// That's why the fileList is important---it tells Cytoscape which files need to be copied. I think the temp files MAY remain afterward... not sure about this!
		logger.debug("saveSessionStateFiles: starting...");
		Properties props = new Properties();
		props.setProperty("numCyDatasets", CyDataset.datasetNameMap.keySet().size() + "");
		int idx = 0;
		for (final String name : CyDataset.datasetNameMap.keySet()) {
			logger.debug("saveSessionStateFiles: " + name + " to be saved...");
			final CyDataset ds = CyDataset.datasetNameMap.get(name);
			props.setProperty(PROP_DS + idx + PROP_NAME, name);
			props.setProperty(PROP_DS + idx + KEY_TYPE, ds.getKeyType());
			String nodeIdListString = "";
			for (final Integer rootID : ds.getNodes()) {
				String cyNodeID = ((CyNode) Cytoscape.getRootGraph().getNode(rootID)).getIdentifier();
				nodeIdListString += cyNodeID + ","; // note that this adds an EXTRA comma to the end of the string, too.
				// TODO: should this last comma NOT be included? Maybe it doesn't matter.
			}
			props.setProperty(PROP_DS + idx + ATTR_LIST, ds.getAttrString());
			props.setProperty(PROP_DS + idx + NODE_ID_LIST, nodeIdListString);
			idx++;
		}
		// CyCriteriaset props (network map)
		for (final CyNetwork net : Cytoscape.getNetworkSet()) {
			final CyCriteriaset cset = CyCriteriaset.getNetworkCriteriaset(net);
			if (null != cset) {
				props.setProperty(CSET_PREFIX + net.getTitle(), cset.getName());
				logger.debug("saveSessionStateFiles: saving " + cset.getName() + " for " + net.getTitle());
			}
		}


		// Now actually write all the files! These functions are located directly above.

		logger.error("Danger: removed GPML file saving here! Uncomment these lines soon!");
		
		final String tmpDir = System.getProperty("java.io.tmpdir"); // Looks like files are WRITTEN to an intermediate temporary directory and are then COPIED to the actual save file.
		
		logger.error("Alex Williams: here are ALL the nodes that Cytoscape knows about: " + namesOfAllCyNodesInCollection(Cytoscape.getCyNodesList()) + ". That is the full set.");
		logger.error("Alex Williams: here are all the 'orphan' CyNodes that aren't in any network: " + namesOfAllCyNodesInCollection(setOfOrphanNodesNotInAnyNetwork()) + ". That is the full set.");
		
		
		writePropFile(props, new File(tmpDir, propFileName), fileList);
		writeNodeAttrFile(new File(tmpDir, nodeAttributeFileName), fileList);
		// writeGPMLFiles(tmpDir, fileList);
		// Done writing all the files.

		logger.debug("saveSessionStateFiles: [DONE]");
	}

	private HashSet<CyNode> setOfAllNodes() {
		// Added in Oct 2012 by Alex Williams
		// Returns a new HashSet of all nodes that Cytoscape knows about, even if they aren't in a network!
		
		// Note about "@SuppressWarnings" below: getCyNodesList returns a "List" and not a "List<CyNode>", so we suppress the unchecked conversion. This would break if (hypothetically) Cytoscape code changed so that non-CyNode items were returned in the list.
		@SuppressWarnings("unchecked")
		final List<CyNode> nodeList = (List<CyNode>) Cytoscape.getCyNodesList();
		return (new HashSet<CyNode>(nodeList));
	}

	private HashSet<CyNode> setOfNodesThatAreInAtLeastOneNetwork() {
		// Added in Oct 2012 by Alex Williams
		// Returns a new HashSet of all nodes that are in AT LEAST ONE NETWORK.
		final HashSet<CyNetwork> allNetworks = (HashSet<CyNetwork>) Cytoscape.getNetworkSet();
		HashSet<CyNode> nodesInAnyNetwork = new HashSet<CyNode>();
		for (final CyNetwork net : allNetworks) { // Go through each network
			final List<CyNode> nodes = (List<CyNode>) net.nodesList();
			nodesInAnyNetwork.addAll(nodes); // <-- Keep track of the nodes that we find in a network
		}
		return (nodesInAnyNetwork); // All the nodes that were in at least ONE network.
	}
	
	private HashSet<CyNode> setOfOrphanNodesNotInAnyNetwork() {
		// Added in Oct 2012 by Alex Williams
		// Returns a set of all the nodes that are NOT in any network.
		// Why this is useful: We want this set because, as it turns out, Cytoscape itself only saves nodes that are IN A NETWORK.
		// So the "orphan" nodes that GenMAPP loads, but aren't in a network, get ignored, UNLESS we save them specifically.
		HashSet<CyNode> orphanNodes = setOfAllNodes(); // Start with ALL nodes
		orphanNodes.removeAll(setOfNodesThatAreInAtLeastOneNetwork()); // Remove all those nodes that ARE in a network, leaving only the "orphan" nodes without a network behind.
		return (orphanNodes); // "orphan" nodes don't have a network
	}

	private String namesOfAllCyNodesInCollection(final Collection c) {
		// Added in Oct 2012 by Alex Williams.
		// This function just takes a collection (of CyNodes, usually), and goes through it, calling "toString" on each node.
		// This can occasionally be useful for debugging.
		String s = "";
		for (Iterator iter = c.iterator(); iter.hasNext(); ) {
		    CyNode nnn = (CyNode) iter.next();
		    s += nnn.toString() + ", ";
		}
		return ("[" + s + "]");
	}
	
}