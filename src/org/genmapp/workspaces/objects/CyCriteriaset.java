package org.genmapp.workspaces.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.tree.CriteriasetPanel;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class CyCriteriaset {



	private String setname;
	private String[] criteriaParams;
	private int rows;

	public static Map<String, CyCriteriaset> criteriaNameMap = new HashMap<String, CyCriteriaset>();
	public static Map<String, Integer> criteriaRowsMap = new HashMap<String, Integer>();
	public static Map<String, Map<String, Integer>> criteriaNetworkNodesMap = new HashMap<String, Map<String, Integer>>();

	/**
	 * Analogous to CyNetwork, this is the base class of all criteria objects in
	 * the workspaces panel.
	 * 
	 * @param s
	 *            display name for criteria set
	 */
	public CyCriteriaset(String s, String params) {
		this.setname = s;
		this.setCriteriaParams(params);

		criteriaNameMap.put(s, this);		
		collectNetworkCounts();

		GenMAPPWorkspaces.wsPanel.getCriteriaTreePanel().addItem(s, "croot");
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

	/**
	 * Transform string into array of criteria set parameters and the set the
	 * array.
	 * 
	 * @param paramStr
	 */
	public void setCriteriaParams(String paramStr) {
		paramStr = paramStr.substring(1, paramStr.length() - 1);
		String[] paramArray = paramStr.split("\\]\\[");

		this.setCriteriaParams(paramArray);
	}

	/**
	 * Set the array of criteria set parameters. Also extract row count from
	 * parameter array and set rows and criteriaRowsMap.
	 * 
	 * @param paramArray
	 */
	public void setCriteriaParams(String[] paramArray) {
		this.criteriaParams = paramArray;

		this.rows = paramArray.length - 1; // don't count mapTo
		criteriaRowsMap.put(this.getDisplayName(), this.rows);
		System.out.println("SET: "+ this.setname+":"+paramArray[1]);
	}

	public String[] getCriteriaParams() {
		return criteriaParams;
	}

	public String getCriteriaParamString() {
		String paramStr = "";
		for (String a : this.criteriaParams) {
			paramStr = paramStr + a + ", ";
		}
		return paramStr.substring(0, paramStr.length() - 2);
	}

	/**
	 * Cleans up HashMaps and tree panel
	 */
	public void deleteCyCriteriaset() {
		criteriaNameMap.remove(this.getDisplayName());
		criteriaRowsMap.remove(this.getDisplayName());
		criteriaNetworkNodesMap.remove(this.getDisplayName());
		GenMAPPWorkspaces.wsPanel.getCriteriaTreePanel().removeItem(
				this.getDisplayName());
	}

	public String getNodeAttribute() {
		String[] split;
		String nodeAttr = "";
		for (String c : criteriaParams) {
			split = c.split(":");
			if (split.length < 3)
				continue; // skip mapTo entry
			nodeAttr = nodeAttr + setname + "_" + split[1] + ":";
		}
		// prune final ":"
		nodeAttr = nodeAttr.substring(0, nodeAttr.length() - 1);
		return nodeAttr;

	}

	public List<CyNode> collectCriteriaNodes(List<CyNode> nodeList) {
		String nodeAttr = getNodeAttribute();
		List<CyNode> hitList = new ArrayList<CyNode>();
		CyAttributes ca = Cytoscape.getNodeAttributes();
		
		for (CyNode n : nodeList) {
			if (ca.hasAttribute(n.getIdentifier(), nodeAttr)) {
				if (rows == 1) {

					boolean b = ca.getBooleanAttribute(n.getIdentifier(),
							nodeAttr);
					if (b) {
						hitList.add(n);
					}

				} else if (rows > 1) {
					Integer i = ca.getIntegerAttribute(n.getIdentifier(),
							nodeAttr);
					if (i >= 0) {
						hitList.add(n);
					}
				}
			}
		}
		return hitList;
	}

	/**
	 * Collect node counts per network for given criteria set
	 * 
	 * @param setParams
	 */
	public void collectNetworkCounts() {
		Map<String, Integer> networkNodes = new HashMap<String, Integer>();
		for (CyNetwork net : Cytoscape.getNetworkSet()) {
			List<CyNode> hitList = collectCriteriaNodes(net.nodesList());
			networkNodes.put(net.getIdentifier(), hitList.size());
		}

		// fill map
		this.criteriaNetworkNodesMap.put(this.setname, networkNodes);

		CriteriasetPanel.getTreeTable().getTree().updateUI();
		CriteriasetPanel.getTreeTable().updateUI();
	}
}
