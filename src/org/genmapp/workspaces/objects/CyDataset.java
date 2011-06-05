package org.genmapp.workspaces.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.tree.DatasetPanel;
import org.genmapp.workspaces.utils.DatasetMapping;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.groups.CyGroup;
import cytoscape.groups.CyGroupManager;

public class CyDataset {

	private String commandString;
	private String source;
	public static boolean isMappedToNetwork;

	private static String name;
	private String keyType;
	private List<Integer> nodes;
	private List<String> attrs;
	private int rows;

	public static Map<String, CyDataset> datasetNameMap = new HashMap<String, CyDataset>();
	// public static List<String> selectedDatasets = new ArrayList<String>();

	/**
	 * Analogous to CyNetwork, this is the base class of all dataset objects in
	 * the workspaces panel. Called by data importers.
	 * 
	 * @param n
	 * @param t
	 * @param nl
	 * @param al
	 */
	public CyDataset(String n, String t, List<Integer> nl, List<String> al) {
		this(n, t, nl, al, true);
	}

	public CyDataset(String n, String t, List<Integer> nl, List<String> al,
			boolean performDatasetMapping) {
		this.name = n;
		this.keyType = t;
		this.nodes = nl;
		this.attrs = al;

		datasetNameMap.put(n, this);

		this.rows = nl.size();

		// add to dataset panel
		GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().addItem(n, "droot");

		// perform dataset mapping
		if (performDatasetMapping) {
			DatasetMapping.performDatasetMapping(this, null, true);
		}
		// set color of entry in panel
		setCurrentHighlight();
	}

	/**
	 * 
	 */
	public static void setCurrentHighlight() {

		List<String> datasetList = Cytoscape.getNetworkAttributes()
				.getListAttribute(
						Cytoscape.getCurrentNetwork().getIdentifier(),
						DatasetMapping.NET_ATTR_DATASETS);

		if (datasetList != null) {
			if (datasetList.contains(name))
				isMappedToNetwork = true;
			else
				isMappedToNetwork = false;
		} else if (Cytoscape.getNetworkSet().size() < 1) {
			// go ahead and paint green if no networks loaded at all
			isMappedToNetwork = true;
		} else {
			// current network has NO datasets mapped
			isMappedToNetwork = false;
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

	// /**
	// * @return the selectedDatasets
	// */
	// public static List<String> getSelectedDatasets() {
	// return selectedDatasets;
	// }

	// /**
	// * @param selectedDatasets
	// * the selectedDatasets to set
	// */
	// public static void setSelectedDataset(List<String> selectedDatasets) {
	// CyDataset.selectedDatasets = selectedDatasets;
	// }

	/**
	 * @return
	 */
	public String getName() {
		return name;
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
	 * Returns a list of node indexes representing all imported dataset nodes.
	 * 
	 * @return
	 */
	public static int[] getAllDatasetNodes() {
		List<Integer> temp = new ArrayList<Integer>();
		for (CyDataset dset : datasetNameMap.values()) {
			temp.addAll(dset.getNodes());
		}
		int[] dsetNodes = new int[temp.size()];
	    Iterator<Integer> iterator = temp.iterator();
	    for (int i = 0; i < dsetNodes.length; i++)
	    {
	    	dsetNodes[i] = iterator.next().intValue();
	    }
	    return dsetNodes;
	}

	/**
	 * @return the attrs
	 */
	public List<String> getAttrs() {
		return attrs;
	}

	/**
	 * @return the attrs
	 */
	public String getAttrString() {
		String attrStr = "";
		for (String a : this.attrs) {
			attrStr = attrStr + a + ", ";
		}
		return attrStr.substring(0, attrStr.length() - 2);
	}
	public static List<String> attrStringToAttrList(String attrString) {
		List<String> l = new ArrayList<String>();

		String[] sArray = attrString.split(", ");
		for (String a : sArray) {
			l.add(a);
		}
		return (l);

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

	/**
	 * Clears all dataset info, including dataset-specific nodes and attributes,
	 * network attributes, Workspaces panel and internal HashMap.
	 * 
	 */
	public void deleteCyDataset() {
		String dname = this.getName();

		/*
		 * If called during a session reset (i.e. new session or open session),
		 * then some of these functions are redundant with Cytoscape's
		 * network-level cleanup and can be skipped.
		 */
		if (Cytoscape.getSessionstate() != Cytoscape.SESSION_OPENED) {

			// remove dataset-specific nodes and group associations
			for (int ni : nodes) {
				CyNode cn = (CyNode) Cytoscape.getRootGraph().getNode(ni);
				// System.out.println("NODE: " + cn.getIdentifier());
				List<CyGroup> gnList = CyGroupManager.getGroup(cn);
				if (null != gnList) {
					for (CyGroup gn : gnList) {
						// TODO: hmm, this appears to delete group node when
						// last child is removed... and CyNode
						gn.removeNode(cn);
						// woops! this actually deletes the CyNode as well.
						// Not good.
						// if (gn.getNodes().size() <= 0) {
						// CyGroupManager.removeGroup(gn);
						// }
					}
				}
				Cytoscape.getRootGraph().removeNode(ni);
			}

			// remove from network attrs
			for (CyNetwork cnet : Cytoscape.getNetworkSet()) {
				List<String> dlist = Cytoscape.getNetworkAttributes()
						.getListAttribute(cnet.getIdentifier(),
								DatasetMapping.NET_ATTR_DATASETS);
				// System.out.println("NET: " + dlist.toString());
				if (null == dlist)
					continue;
				if (dlist.contains(dname)) {
					dlist.remove(dname);
					Cytoscape.getNetworkAttributes().deleteAttribute(
							cnet.getIdentifier(),
							DatasetMapping.NET_ATTR_DATASET_PREFIX + dname);
				}
				String netId = cnet.getIdentifier();
				WorkspacesCommandHandler.allMetanodesOperation(netId,
						WorkspacesCommandHandler.EXPAND_ALL);
				WorkspacesCommandHandler.allMetanodesOperation(netId,
						WorkspacesCommandHandler.COLLAPSE_ALL);
			}

			// TODO: remove empty attrs and assess dependent criteriasets

		}

		// remove from panel
		GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().removeItem(
				this.getName());

		// remove internal representation
		datasetNameMap.remove(this.getName());

	}
}
