package org.genmapp.workspaces.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.tree.DatasetPanel;
import org.genmapp.workspaces.utils.DatasetMapping;

import cytoscape.Cytoscape;

public class CyDataset {

	private String commandString;
	private String source;
	public boolean isMappedToNetwork;

	private String displayName;
	private String keyType;
	private List<Integer> nodes;
	private List<String> attrs;
	private int rows;

	public static Map<String, CyDataset> datasetNameMap = new HashMap<String, CyDataset>();
	public static List<String> selectedDatasets = new ArrayList<String>();

	/**
	 * Analogous to CyNetwork, this is the base class of all dataset objects in
	 * the workspaces panel. Called by data importers.
	 * 
	 * @param n = name
	 * @param k = keyType
	 * @param nl = node list
	 * @param al = attribute list
	 */
	public CyDataset(String n, String t, List<Integer> nl, List<String> al) {
		this.displayName = n;
		this.keyType = t;
		this.nodes = nl;
		this.attrs = al;

		datasetNameMap.put(n, this);

		this.rows = nl.size();

		// add to dataset panel
		GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().addItem(n, "droot");

		// perform dataset mapping
		DatasetMapping.performDatasetMapping(this);

		// set color of entry in panel
		setCurrentHighlight();
	}

	/**
	 * Called when loading session file containing datasets.
	 * 
	 * @param n
	 * @param al
	 */
	public CyDataset(String n, List<String> al) {
		this.displayName = n;
		this.attrs = al;

		datasetNameMap.put(n, this);

		/*
		 * Once all nodes are loaded from xGMML, then scan all nodes to collect
		 * this.keyType and this.nodes based on nodeAtts. Don't need to run
		 * performDatasetMapping.
		 */

		this.rows = this.nodes.size();

		// add to dataset panel
		GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().addItem(n, "droot");

		// set color of entry in panel
		setCurrentHighlight();

	}

	/**
	 * 
	 */
	private void setCurrentHighlight() {

		List<String> datasetList = Cytoscape.getNetworkAttributes()
				.getListAttribute(
						Cytoscape.getCurrentNetwork().getIdentifier(),
						DatasetMapping.NET_ATTR_DATASETS);
		
		if (datasetList != null) {
			if (datasetList.contains(this.displayName))
				this.isMappedToNetwork = true;
			else
				this.isMappedToNetwork = false;
		} else if (null == Cytoscape.getCurrentNetwork()) {
			//go ahead and paint green if no networks loaded at all
			this.isMappedToNetwork = true;
		} else 	{
			// current network has NO datasets mapped
			this.isMappedToNetwork = false;
		}

		DatasetPanel.getTreeTable().getTree().updateUI();
	}

	/**
	 * 
	 */
	public void extractRowCount() {
		String s = null;
		Pattern p = Pattern.compile("rows=\"(\\d+)\"");
		Matcher m = p.matcher(this.commandString);
		while (m.find())
			s = m.group(1);

		this.rows = Integer.decode(s);
	}

	/**
	 * @return the selectedDatasets
	 */
	public static List<String> getSelectedDatasets() {
		return selectedDatasets;
	}

	/**
	 * @param selectedDatasets
	 *            the selectedDatasets to set
	 */
	public static void setSelectedDataset(List<String> selectedDatasets) {
		CyDataset.selectedDatasets = selectedDatasets;
	}

	/**
	 * @return
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @return
	 */
	public int getRows() {
		return rows;
	}

	/**
	 * @return the keyType
	 */
	public String getKeyType() {
		return keyType;
	}

	/**
	 * @return the nodes
	 */
	public List<Integer> getNodes() {
		return nodes;
	}

	/**
	 * @return the attrs
	 */
	public List<String> getAttrs() {
		return attrs;
	}

	/**
	 * @return the commandString
	 */
	public String getCommandString() {
		return commandString;
	}

	/**
	 * @return the url
	 */
	public String getSource() {
		return this.source;
	}
}
