package org.genmapp.workspaces.objects;

import java.util.HashMap;
import java.util.Map;

public class CyCriteria {

	private String setname;
	private int rows;
	
	public static Map<String, CyCriteria> criteriaNameMap = new HashMap<String, CyCriteria>();
	public static Map<String, Integer> criteriaRowsMap = new HashMap<String, Integer>();
	public static Map<String, Map<String, Integer>> criteriaNetworkNodesMap = new HashMap<String,Map<String, Integer>>();

	/**
	 * Analogous to CyNetwork, this is the base class of all criteria objects in
	 * the workspaces panel.
	 * 
	 * @param s	display name for criteria set
	 * @param r number of criteria (or rows) in set
	 */
	public CyCriteria(String s, int r) {
		this.setDisplayName(s);

		criteriaRowsMap.put(s, r);
		criteriaNameMap.put(s, this);
	}


	public void setDisplayName(String displayName) {
		this.setname = displayName;
	}

	public String getDisplayName() {
		return setname;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getRows() {
		return rows;
	}
}
