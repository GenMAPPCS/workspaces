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
import org.genmapp.workspaces.utils.DatasetAttributesReader;
import org.genmapp.workspaces.utils.DatasetAttributesWriter;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.cytoscape.GpmlPlugin;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.data.attr.CountedIterator;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.visual.VisualPropertyDependency;

public class GenMAPPWorkspaces extends CytoscapePlugin {

	public static WorkspacesPanel wsPanel;
	private CyLogger logger;
	final static String propFileName = "GenMAPPWorkspaces.props";
	final static String nodeAttributeFileName = "GenMAPPWorkspaces.nodeAttr";
	public static final String ATTR_PATHWAY_URL = "wikipathways.url";

	/**
	 * 
	 */
	public GenMAPPWorkspaces() {

		logger = CyLogger.getLogger(GenMAPPWorkspaces.class);
		logger.setDebug(true);

		// create workspaces panel
		CytoPanel cytoPanel1 = Cytoscape.getDesktop().getCytoPanel(
				SwingConstants.WEST);

		wsPanel = new WorkspacesPanel(logger);

		cytoPanel1.add("GenMAPP-CS", new ImageIcon(getClass().getResource(
				"images/genmappcs.png")), wsPanel, "Workspaces Panel", 0);
		cytoPanel1.setSelectedIndex(0);
		// cytoPanel.remove(1);

		// set properties
		// set view thresholds to handle "overview" xGMMLs
		CytoscapeInit.getProperties().setProperty("viewThreshold", "50000");
		CytoscapeInit.getProperties().setProperty("render.coarseDetailThreshold", "100000");
		CytoscapeInit.getProperties().setProperty("render.nodeLabelThreshold", "50000");

		// set default node width/height lock to avoid dependency issues
		Cytoscape.getVisualMappingManager().getVisualStyle().getDependency()
				.set(VisualPropertyDependency.Definition.NODE_SIZE_LOCKED,
						false);

		/*
		 * Clear out all org.genmapp.criteriaset properties that may have been
		 * "saved as default." This happens right after the plugin is loaded,
		 * well before properties are added from session files. This way we can
		 * utilize props for storing criteriasets with sessions without allowing
		 * them to be recalled from .cytoscape/cytoscape.props
		 */
		Properties props = CytoscapeInit.getProperties();
		if (props.containsKey(WorkspacesCommandHandler.PROPERTY_SETS)) {
			CytoscapeInit.getProperties().remove(
					WorkspacesCommandHandler.PROPERTY_SETS);
			List<String> keylist = new ArrayList<String>();
			for (Object key : props.keySet()) {
				if (((String) key)
						.startsWith(WorkspacesCommandHandler.PROPERTY_SET_PREFIX)) {
					keylist.add((String) key);

				}
			}
			logger
					.debug("Clearing org.genmapp.criteriaset properties from saved defaults...");
			for (String key : keylist) {
				CytoscapeInit.getProperties().remove(key);
				logger.debug(key + " removed");
			}
		}

		// cycommands
		new WorkspacesCommandHandler(logger);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cytoscape.plugin.CytoscapePlugin#restoreSessionState(java.util.List)
	 */
	public void restoreSessionState(List<File> fileList) {
		logger.debug("loadSessionState");
		final String tmpDir = System.getProperty("java.io.tmpdir");
		File propFile = null;
		// TODO: can there be more than one?
		File nodeAttributeFile = null;
		List<File> gpmlFileList = new ArrayList<File>();

		try {
			for (File f : fileList) {
				logger.debug(f.getName());
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
					logger.warn("Ignored " + f.getName());
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

						logger.info("GPML file " + title
								+ " restored from session file.");

					} catch (ConverterException e) {
						logger.error("Error reading GPML file: ", e);
					}
				} else {
					logger.warn("GPML plugin not detected. "
							+ gpmlFile.getName() + " not restored.");
				}

			}

			/*
			 * first process the node attribute file so as to populate missing
			 * RootGraph nodes properties file for GenMAPPWorkspaces
			 * 
			 * this will read the properties directly into the Cytoscape node
			 * attributes structure, (hopefully) overwriting any duplicates
			 */
			logger.info("Loading node attributes for CyDataset nodes");
			DatasetAttributesReader.loadAttributes(Cytoscape.getNodeAttributes(),
					new FileReader(nodeAttributeFile), logger);

			/*
			 * Display a mini table of node attributes, if in debug mode
			 */
			if (logger.isDebugging()) {
				CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
				String s = "object keys: ";

				HashMap<String, String> display = new HashMap<String, String>();
				for (String n : nodeAttr.getAttributeNames()) {
					CountedIterator i = nodeAttr.getMultiHashMap()
							.getObjectKeys(n);
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

				logger.debug("DISPLAY: " + displayStr);
			}

			// now, you can create your CyDatasets
			Properties props = new Properties();
			props.load(new FileReader(propFile));

			int numCyDatasets = new Integer(props.getProperty("numCyDatasets"))
					.intValue();
			logger.debug("loading " + numCyDatasets + " CyDatasets");

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
					nodeRootIdList.add(Cytoscape.getCyNode(cyNodeID, true)
							.getRootGraphIndex());
				}
				logger.debug(nodeRootIdList + "");
				/*
				 * Create a new CyDataset object, but with no automatic mapping
				 * since that has already been done and loaded directly as node
				 * attributes.
				 */
				new CyDataset(name, keyType, nodeRootIdList,
						attrList, false, logger);
				logger.info("CyDataset " + name
						+ " restored from session file.");
			}

			/*
			 * Load Criteriasets
			 */
			logger.debug("loading CyCriteriasets");
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
					logger.debug(cs+" to be restored...");
				}
			}
			// create csets
			for (String csetname : full) {
				String setParameters = CytoscapeInit.getProperties()
						.getProperty(
								WorkspacesCommandHandler.PROPERTY_SET_PREFIX
										+ csetname);
				new CyCriteriaset(csetname, setParameters);
				logger.info("CyCriteriaset "+csetname+" restored from session file.");
			}
			// restore network-criteria map from prop file
			for (CyNetwork net : Cytoscape.getNetworkSet()) {
				String csetname = props.getProperty("cs." + net.getTitle());
				CyCriteriaset cset = CyCriteriaset.criteriaNameMap
						.get(csetname);
				// track last applied cset per network
				CyCriteriaset.setNetworkCriteriaset(net, cset);
				logger.debug("setting "+csetname+" for "+net.getTitle());
				// apply them after SESSION_LOADED
			}
			// end try read files
		} catch (Exception e) {
			logger.debug("restoreSessionState Exception: " + e);
		}
		logger.debug("done with all session restoring tasks");
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cytoscape.plugin.CytoscapePlugin#saveSessionStateFiles(java.util.List)
	 */
	public void saveSessionStateFiles(List<File> fileList) {
		logger.debug("saveSessionState");

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
				logger.debug(name+" to be saved...");
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
					logger.debug("saving " +cset.getName()+" for "+ net.getTitle());
				}
			}

			// writes property file to disk
			logger.debug("writing props to file: "+propFile.getName());
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
			// TODO: this can be improved
			logger.debug("writing node attribtues");
			DatasetAttributesWriter.writeAttributes(Cytoscape.getNodeAttributes(),
					nodeAttributeFile, logger);
			fileList.add(nodeAttributeFile);

		} catch (IOException ex) {
			logger.debug("saveSessionState: IOException " + ex);

		}

		// Save GPMLs
		for (CyNetwork net : Cytoscape.getNetworkSet()) {
			String wpurl = Cytoscape.getNetworkAttributes().getStringAttribute(
					net.getIdentifier(), ATTR_PATHWAY_URL);
			if (null != wpurl) {
				final File gpmlFile = new File(tmpDir, net.getTitle() + ".gpml");
				GpmlPlugin gp = GpmlPlugin.getInstance();
				logger.debug("detected gpml network to be saved");
				
				// if GPML plugin is loaded, then attempt save pathway
				if (null != gp) {
					try {
						gp.writeToFile(Cytoscape.getNetworkView(net
								.getIdentifier()), gpmlFile);
						logger.debug("Saved "+gpmlFile.getName());
					} catch (ConverterException e) {
						logger.error("gpml writeToFile ConverterException: "+e);
					}
					fileList.add(gpmlFile);
				}
				logger.warn("Ignored saving "+gpmlFile.getName()+ " as GPML in session file.");
			}

		}
		logger.debug("done with all session saving tasks");
	}

}