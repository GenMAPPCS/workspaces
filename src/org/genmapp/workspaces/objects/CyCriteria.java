package org.genmapp.workspaces.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;

import cytoscape.CytoscapeInit;

public class CyCriteria {

	private String setname;
	private int rows;
	
	public static Map<String, CyCriteria> criteriaNameMap = new HashMap<String, CyCriteria>();
	public static Map<String, Integer> criteriaRowsMap = new HashMap<String, Integer>();
	public static Map<String, Map<String, Integer>> criteriaNetworkNodesMap = new HashMap<String,Map<String, Integer>>();

	static boolean bResultsMasterPanelAlreadyAdded = false; // used for results


	public static String[] getCriteriaSets()
	{
		String setsString = CytoscapeInit.getProperties().getProperty(WorkspacesCommandHandler.PROPERTY_SETS);

		String[] a = { "" };

		if ( null != setsString && setsString.length() > 2) {
			setsString = setsString.substring(1, setsString.length()-1);
			String[] temp = setsString.split("\\]\\[");
			ArrayList<String> full = new ArrayList<String>();
			for (String s : temp) {
				full.add(s);
			}
			return full.toArray(a);

		}
		return (a);
	}


	// for a given criteriaSet, return its criteria
	public static String[] getCriteria(String criteriaSet ) {
		ArrayList<String> criteriaNames = new ArrayList<String>();
		String paramString = CytoscapeInit.getProperties().getProperty(WorkspacesCommandHandler.PROPERTY_SET_PREFIX + criteriaSet);
		paramString = paramString.substring(1, paramString.length()-1);
		String[] temp = paramString.split("\\]\\[");

		// split first on "comma", then on ":"
		boolean isFirst = true;
		for (String criterion : temp) {
			// skip the first entry, it's not actually a criterion
			if (isFirst) {
				isFirst = false;
				continue;
			}

			String[] tokens = criterion.split(":");
			criteriaNames.add(tokens[1]);
		}
		return ((String[]) criteriaNames.toArray(new String[criteriaNames
				.size()]));
	}

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
