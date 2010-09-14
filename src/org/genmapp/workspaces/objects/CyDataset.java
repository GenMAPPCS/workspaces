package org.genmapp.workspaces.objects;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CyDataset {

	private URL url;
	private String displayName;
	private int rows;
	
	public static Map<String, URL> datasetUrlMap = new HashMap<String, URL>();
	public static Map<String, Integer> datasetRowsMap = new HashMap<String, Integer>();
	public static List<String> selectedDatasets = new ArrayList<String>();
	
	/**
	 * Analogous to CyNetwork, this is the base class of all dataset objects in
	 * the workspaces panel.
	 * 
	 * @param u URL reference to source of dataset
	 * @param n display name for dataset
	 * @param r number of rows in dataset
	 */
	public CyDataset(String n, URL u, int r) {
		this.setUrl(u);
		this.setDisplayName(n);
		this.setRows(r);
		
		datasetUrlMap.put(n, u);
		datasetRowsMap.put(n, r);

	}

	/**
	 * @return the selectedDatasets
	 */
	public static List<String> getSelectedDatasets() {
		return selectedDatasets;
	}

	/**
	 * @param selectedDatasets the selectedDatasets to set
	 */
	public static void setSelectedDataset(List<String> selectedDatasets) {
		CyDataset.selectedDatasets = selectedDatasets;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public URL getUrl() {
		return url;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getRows() {
		return rows;
	}
}
