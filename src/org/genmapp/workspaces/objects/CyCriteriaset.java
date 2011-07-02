package org.genmapp.workspaces.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.commons.lang.StringUtils;
import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.tree.CriteriasetPanel;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.VisualMappingManager;

public class CyCriteriaset {

	private String name;
	private String[] criteriaParams;
	private int rows;

	//Map of network title, setname. Used to track last cset to be applied per network. 
	public static Map<String, String> networkCriteriasetMap = new HashMap<String, String>();
	//Map of setname, CyCriteriaset objects
	public static Map<String, CyCriteriaset> criteriaNameMap = new HashMap<String, CyCriteriaset>();
	//Map of setname, criteria count (rows in a given set)
	public static Map<String, Integer> criteriaRowsMap = new HashMap<String, Integer>();
	//Map of setname, network id, node count 
	public static Map<String, Map<String, Integer>> criteriaNetworkNodesMap = new HashMap<String, Map<String, Integer>>();

	/**
	 * Analogous to CyNetwork, this is the base class of all criteria objects in
	 * the workspaces panel.
	 * 
	 * @param s
	 *            display name for criteria set
	 */
	public CyCriteriaset(String s, String params) {
		this.name = s;
		this.setCriteriaParams(params);

		criteriaNameMap.put(s, this);
		collectNetworkCounts();

		GenMAPPWorkspaces.wsPanel.getCriteriaTreePanel().addItem(s, "croot");
	}

	public String getName() {
		return name;
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
		criteriaRowsMap.put(this.getName(), this.rows);
		// System.out.println("SET: " + this.name + ":" + paramArray[1]);
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
	 * Clears all criteriaset data, including Cytoscape properties, Workspaces
	 * panel, and internal HashMaps. Also resets visual styles to "base" style
	 * and removes it from the catalog.
	 */
	public void deleteCriteriaset() {

		// remove associated visual styles and reset displays to "base"
		// style
		VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
		CalculatorCatalog catalog = vmm.getCalculatorCatalog();
		Set<String> vsNames = catalog.getVisualStyleNames();
		HashMap<String, String> removeAndSwitch = new HashMap<String, String>();
		for (String vsName : vsNames) {
			if (StringUtils.endsWith(vsName, "__" + this.getName())) {
				String vsSwitch = vsName.substring(0, vsName.indexOf("__"
						+ this.getName()));
				removeAndSwitch.put(vsName, vsSwitch);
			}
		}
		for (CyNetwork cn : Cytoscape.getNetworkSet()) {
			CyNetworkView cnv = Cytoscape.getNetworkView(cn.getIdentifier());
			String cnvVs = cnv.getVisualStyle().getName();
			if (removeAndSwitch.containsKey(cnvVs)) {
				vmm.setNetworkView(cnv);
				vmm.setVisualStyle(removeAndSwitch.get(cnvVs));
				vmm.applyAppearances();
				catalog.removeVisualStyle(cnvVs);
			}

		}

		// remove from props
		String setList = CytoscapeInit.getProperties().getProperty(
				WorkspacesCommandHandler.PROPERTY_SETS);
		if (null != setList) {
			// trim leading and trailing brackets
			setList = setList.replace("[" + this.getName() + "]", "");
			if (setList.length() > 1)
				CytoscapeInit.getProperties().setProperty(
						WorkspacesCommandHandler.PROPERTY_SETS, setList);
			else
				CytoscapeInit.getProperties().remove(
						WorkspacesCommandHandler.PROPERTY_SETS);
		}
		CytoscapeInit.getProperties().remove(
				WorkspacesCommandHandler.PROPERTY_SET_PREFIX + this.getName());

		// remove from panel
		GenMAPPWorkspaces.wsPanel.getCriteriaTreePanel().removeItem(
				this.getName());
		// remove internal representation
		criteriaNameMap.remove(this.getName());
		criteriaRowsMap.remove(this.getName());
		criteriaNetworkNodesMap.remove(this.getName());

	}

	/**
	 * @return
	 */
	public String getNodeAttribute() {
		String nodeAttr = "";
		if (rows > 1) {
			nodeAttr = name + ":composite";
		} else {
			String[] split;
			String c = criteriaParams[1];
			split = c.split("::");
			nodeAttr = nodeAttr + name + "_" + split[1];
		}
		return nodeAttr;

	}


	/**
	 * @param nodeList
	 * @return
	 */
	public List<CyNode> collectCriteriaNodes(int[] nodeList) {
		String nodeAttr = getNodeAttribute();
		List<CyNode> hitList = new ArrayList<CyNode>();
		CyAttributes ca = Cytoscape.getNodeAttributes();

		for (int ni : nodeList) {
			CyNode n = (CyNode) Cytoscape.getRootGraph().getNode(ni);
			// if (n.isaGroup())
			// continue;
			if (ca.hasAttribute(n.getIdentifier(), nodeAttr)) {
				if (rows == 1) {

					boolean b = Boolean.valueOf(ca.getStringAttribute(n.getIdentifier(),
							nodeAttr));
					if (b) {
						hitList.add(n);
					}

				} else if (rows > 1) {
					String s = ca.getStringAttribute(n.getIdentifier(),
							nodeAttr);
					if (!s.equals("null")) {
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
			List<CyNode> hitList = collectCriteriaNodes(net
					.getNodeIndicesArray());
			networkNodes.put(net.getIdentifier(), hitList.size());
		}

		// fill map
		this.criteriaNetworkNodesMap.put(this.name, networkNodes);

//		// invokeLater() avoids NPEs during busy times
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
				CriteriasetPanel.getTreeTable().getTree().updateUI();
				CriteriasetPanel.getTreeTable().updateUI();
//			}
//		});
	}

	/**
	 * @param net
	 * @param cset
	 */
	public static void setNetworkCriteriaset(CyNetwork net, CyCriteriaset cset) {
		networkCriteriasetMap.put(net.getTitle(), cset.name);
	}

	/**
	 * @param net
	 * @return
	 */
	public static CyCriteriaset getNetworkCriteriaset(CyNetwork net) {
		String csetName = networkCriteriasetMap.get(net.getTitle());
		return criteriaNameMap.get(csetName);
	}

}
