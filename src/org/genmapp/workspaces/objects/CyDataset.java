package org.genmapp.workspaces.objects;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.utils.DatasetMapping;

import cytoscape.CyNode;

public class CyDataset {

	private String commandString;
	private String source;
	public boolean isUrlAttached;
	
	private String displayName;
	private String keyType;
	private List<Integer> nodes;
	private List<String> attrs;
	private int rows;

	public static Map<String, CyDataset> datasetNameMap = new HashMap<String, CyDataset>();
	public static List<String> selectedDatasets = new ArrayList<String>();

	/**
	 * Analogous to CyNetwork, this is the base class of all dataset objects in
	 * the workspaces panel.
	 * 
	 * @param n
	 *            display name for dataset
	 * @param c
	 *            command string for dataset import
	 */
	public CyDataset(String n, String c) {
		this.displayName = n;
		this.commandString = c;

		datasetNameMap.put(n, this);

		extractRowCount();
		verifyUrl();

		// add to dataset panel
		GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().addItem(n, "droot");
	}

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
	}

	/**
	 * Use this method to verify the url is still around. The urlAttached
	 * boolean is set and can be used to inform UI elements and available
	 * functions, e.g., reimporting a datset. If the url is not found, the row
	 * count is set to 0.
	 */
	public void verifyUrl() {
		String s = null;
		Pattern p = Pattern.compile("source=\"(.+?)\"");
		Matcher m = p.matcher(this.commandString);
		while (m.find())
			s = m.group(1);

		this.source = s;

		s = s.substring(s.indexOf(":") + 1);
		File f = new File(s);
		isUrlAttached = f.exists();
		if (!isUrlAttached)
			this.rows = 0;

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
