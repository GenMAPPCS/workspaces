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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.tree.WorkspacesPanel;
import org.genmapp.workspaces.utils.CyAttributesReader;
import org.genmapp.workspaces.utils.CyAttributesWriter;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.cytoscape.GpmlPlugin;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.data.attr.CountedIterator;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.visual.VisualPropertyDependency;

public class GenMAPPWorkspaces extends CytoscapePlugin {

	public static WorkspacesPanel wsPanel;
	public static void showMessage(String message) {
		// JOptionPane.showMessageDialog( Cytoscape.getDesktop(), message, "",
		// JOptionPane.ERROR_MESSAGE );
	}
	final static String propFileName = "GenMAPPWorkspaces.props";
	final static String nodeAttributeFileName = "GenMAPPWorkspaces.nodeAttr";
	public static final String ATTR_PATHWAY_URL = "wikipathways.url";

	/*
	 * (non-Javadoc)
	 * 
	 * @see cytoscape.plugin.CytoscapePlugin#restoreSessionState(java.util.List)
	 */
	public void restoreSessionState(List<File> fileList) {
		showMessage("loadSessionState");
		final String tmpDir = System.getProperty("java.io.tmpdir");
		File propFile = null;
		// TODO: can there be more than one?
		File nodeAttributeFile = null;
		List<File> gpmlFileList = new ArrayList<File>();

		try {
			for (File f : fileList) {
				showMessage(f.getName());
				if (f.getName().contains(propFileName)) {
					showMessage("propfile = " + f);
					propFile = f;
				} else if (f.getName().contains(nodeAttributeFileName)) {
					showMessage("node attribute file = " + f);
					nodeAttributeFile = f;
				} else if (f.getName().endsWith(".gpml")) {
					showMessage("gpml file = " + f);
					gpmlFileList.add(f);
				}
			}

			// Load GPML files
			for (File gpmlFile : gpmlFileList) {
				GpmlPlugin gp = GpmlPlugin.getInstance();

				// if GPML plugin is loaded, then attempt load pathway
				if (null != gp) {
					try {
						Pathway pathway = new Pathway();
						pathway.readFromXml(gpmlFile, true);
						CyNetwork net = gp.load(pathway, true);
						String title = gpmlFile.getName();
						title = title.replaceFirst("GenMAPPWorkspaces_", "");
						title = title.replaceAll(".gpml", "");
						// destroy CyNetwork restored from xGMML; replace with
						// this GPML import
						Cytoscape.destroyNetwork(title);
						net.setTitle(title);
						Cytoscape.getNetworkAttributes().setAttribute(
								net.getIdentifier(), ATTR_PATHWAY_URL,
								gpmlFile.getPath());

						// and let the world know...
						// the Object[2] is Cytoscape convention
						// Object[] new_value = new Object[2];
						// new_value[0] = net;
						// new_value[1] = net.getIdentifier();
						//Cytoscape.firePropertyChange(Cytoscape.NETWORK_LOADED,
						// null, new_value);

					} catch (ConverterException e) {
						e.printStackTrace();
					}
				}

			}

			/* first process the node attribute file so as to populate missing
			* RootGraph nodes
			* properties file for GenMAPPWorkspaces

			* this will read the properties directly into the Cytoscape node
			* attributes structure, (hopefully) overwriting
			* any duplicates
			*/
			CyAttributesReader.loadAttributes(Cytoscape.getNodeAttributes(),
					new FileReader(nodeAttributeFile));

			// ***** for debugging: display a mini table of node attributes
			// *****
			CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
			String s = "object keys: ";
			HashMap<String, String> display = new HashMap<String, String>();
			for (String n : nodeAttr.getAttributeNames()) {
				CountedIterator i = nodeAttr.getMultiHashMap().getObjectKeys(n);
				while (i.hasNext()) {
					String id = (String) i.next();
					String existingStr = "";
					if (display.get(id) != null) {
						existingStr = (String) display.get(id);
					}

					existingStr += nodeAttr.getAttribute(id, n) + "---";
					display.put(id, existingStr);
				}
			}

			String displayStr = "";
			for (String k : display.keySet()) {
				displayStr += display.get(k) + "\n";
			}

			showMessage("DISPLAY: " + displayStr);
			// **************** end debugging display code *******************

			// create actual CyNodes, and implicitly their Nodes
			// no explicit mapping needs to be done as it has been handled
			// already by loading of all the nodeAttributes
			for (String nodeID : display.keySet()) {
				Cytoscape.getCyNode(nodeID, true);
			}

			showMessage("loading CyDatasets");

			// now, you can create your CyDatasets
			Properties props = new Properties();
			props.load(new FileReader(propFile));

			int numCyDatasets = new Integer(props.getProperty("numCyDatasets"))
					.intValue();
			showMessage("numCyDatasets:" + numCyDatasets);

			Map<String, CyDataset> datasetNameMap = new HashMap<String, CyDataset>();
			for (int i = 0; i < numCyDatasets; i++) {
				String name = props.getProperty("ds." + i + ".name");
				String keyType = props.getProperty("ds." + i + ".keyType");
				String nodeIdListString = props.getProperty("ds." + i
						+ ".nodeIdList");
				List<String> attrList = CyDataset.attrStringToAttrList(props
						.getProperty("ds." + i + ".attrList"));

				String[] nodeIdsAsStr = nodeIdListString.split(",");
				List<Integer> nodeRootIdList = new ArrayList<Integer>();
				for (String cyNodeID : nodeIdsAsStr) {

					nodeRootIdList.add(Cytoscape.getCyNode(cyNodeID)
							.getRootGraphIndex());
				}
				showMessage(nodeRootIdList + "");
				// create a new CyDataset object, but with no automatic mapping
				// ( that has been done already at the attribute level )
				CyDataset ds = new CyDataset(name, keyType, nodeRootIdList,
						attrList, false);
				showMessage(name);
				datasetNameMap.put(name, ds);
			}
			showMessage("replacing cydataset map");
			CyDataset.datasetNameMap = datasetNameMap;

			/*
			 * Load Criteriasets
			 */
			String setsString = CytoscapeInit.getProperties().getProperty(
					WorkspacesCommandHandler.PROPERTY_SETS);
			// extract cset names
			String[] csetList = {""};
			ArrayList<String> full = new ArrayList<String>();
			if (null != setsString && setsString.length() > 2) {
				setsString = setsString.substring(1, setsString.length() - 1);
				String[] temp = setsString.split("\\]\\[");

				for (String cs : temp) {
					full.add(cs);
				}
			}
			// create csets
			for (String csetname : full) {
				String setParameters = CytoscapeInit.getProperties()
						.getProperty(
								WorkspacesCommandHandler.PROPERTY_SET_PREFIX
										+ csetname);
				new CyCriteriaset(csetname, setParameters);
			}
			// restore network-criteria map from prop file
			for (CyNetwork net : Cytoscape.getNetworkSet()) {
				String csetname = props.getProperty("cs." + net.getTitle());
				CyCriteriaset cset = CyCriteriaset.criteriaNameMap
						.get(csetname);
				// track last applied cset per network
				CyCriteriaset.setNetworkCriteriaset(net, cset);
				// apply them after SESSION_LOADED
			}
			// end try read files
		} catch (Exception e) {
			showMessage("Exception: " + e);
		}

	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cytoscape.plugin.CytoscapePlugin#saveSessionStateFiles(java.util.List)
	 */
	public void saveSessionStateFiles(List<File> fileList) {
		showMessage("saveSessionState");

		final String tmpDir = System.getProperty("java.io.tmpdir");
		final File propFile = new File(tmpDir, propFileName);
		final File nodeAttributeFile = new File(tmpDir, nodeAttributeFileName);
		try {
			Properties props = new Properties();

			// Write the first file:
			// CyDataset member info

			// for each dataset, write ds_name, ds_keyType ( cannot be found
			// elsewhere? ), list of nodeids
			props.setProperty("numCyDatasets", CyDataset.datasetNameMap
					.keySet().size()
					+ "");
			int i = 0;
			for (String name : CyDataset.datasetNameMap.keySet()) {
				CyDataset ds = (CyDataset) CyDataset.datasetNameMap.get(name);
				props.setProperty("ds." + i + ".name", name);
				props.setProperty("ds." + i + ".keyType", ds.getKeyType());

				String nodeIdListString = "";
				for (Integer rootID : ds.getNodes()) {
					String cyNodeID = ((CyNode) Cytoscape.getRootGraph()
							.getNode(rootID)).getIdentifier();
					nodeIdListString += cyNodeID + ",";
				}
				props.setProperty("ds." + i + ".attrList", ds.getAttrString());
				props.setProperty("ds." + i + ".nodeIdList", nodeIdListString);
				i++;
			}

			// CyCriteriaset props (network map)
			for (CyNetwork net : Cytoscape.getNetworkSet()) {
				CyCriteriaset cset = CyCriteriaset.getNetworkCriteriaset(net);
				if (null != cset) {
					props.setProperty("cs." + net.getTitle(), cset.getName());
				}
			}

			// writes property file to disk
			props.store(new FileOutputStream(propFile), null);
			fileList.add(propFile);

			/*
			 * Now write the second file this one stores all the node attributes
			 * for all nodes in the rootgraph This is needed because the
			 * rootgraph is not fully stored in the session: in particular, any
			 * nodes that belong to no networks will not be stored. The problem
			 * is that these nodes may belong to CyDatasets so we must store
			 * them ourselves. To keep things simple ( though inefficient ), we
			 * simply store all the nodes in the rootgraph and allow
			 * mergers/collisions to happen naturally.
			 */

			CyAttributesWriter.writeAttributes(Cytoscape.getNodeAttributes(),
					nodeAttributeFile);
			fileList.add(nodeAttributeFile);

		} catch (IOException ex) {
			showMessage("saveSessionState: IOException " + ex);

		}

		// Save GPMLs
		for (CyNetwork net : Cytoscape.getNetworkSet()) {
			String wpurl = Cytoscape.getNetworkAttributes().getStringAttribute(
					net.getIdentifier(), ATTR_PATHWAY_URL);
			if (null != wpurl) {
				final File gpmlFile = new File(tmpDir, net.getTitle() + ".gpml");
				GpmlPlugin gp = GpmlPlugin.getInstance();

				// if GPML plugin is loaded, then attempt save pathway
				if (null != gp) {
					try {
						gp.writeToFile(Cytoscape.getNetworkView(net
								.getIdentifier()), gpmlFile);
					} catch (ConverterException e) {
						e.printStackTrace();
					}
					fileList.add(gpmlFile);
				}

			}

		}
		showMessage("done");
	}

	/**
	 * 
	 */
	public GenMAPPWorkspaces() {

		// create workspaces panel
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(
				SwingConstants.WEST);

		wsPanel = new WorkspacesPanel();

		cytoPanel.add("GenMAPP-CS", new ImageIcon(getClass().getResource(
				"images/genmappcs.png")), wsPanel, "Workspaces Panel", 0);
		cytoPanel.setSelectedIndex(0);
		// cytoPanel.remove(1);

		// set properties
		// set view thresholds to handle "overview" xGMMLs
		CytoscapeInit.getProperties().setProperty("viewThreshold", "100000");
		CytoscapeInit.getProperties().setProperty("secondaryViewThreshold",
				"120000");
		// set default node width/height lock to avoid dependency issues
		Cytoscape.getVisualMappingManager().getVisualStyle().getDependency()
				.set(VisualPropertyDependency.Definition.NODE_SIZE_LOCKED,
						false);

		/*
		 * Clear out all org.genmapp.criteriaset* properties that may have been
		 * "saved as default." This happens right after the plugin is loaded,
		 * well before properties are added from session files. This way we can
		 * utilize props for storing criteriasets with sessions without allowing
		 * them to be recalled from .cytoscape/cytoscape.props
		 */	
		Properties props = CytoscapeInit.getProperties();
		if (props.containsKey(WorkspacesCommandHandler.PROPERTY_SETS)) {
			CytoscapeInit.getProperties().remove(WorkspacesCommandHandler.PROPERTY_SETS);
			List<String> keylist = new ArrayList<String>();
			for (Object key : props.keySet()) {
				if (((String) key)
						.startsWith(WorkspacesCommandHandler.PROPERTY_SET_PREFIX)) {
					keylist.add((String) key);
					
				}
			}
			for (String key : keylist){
				CytoscapeInit.getProperties().remove(key);
			}
		}
		
		// cycommands
		new WorkspacesCommandHandler();
	}

}