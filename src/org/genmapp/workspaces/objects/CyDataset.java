package org.genmapp.workspaces.objects;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JProgressBar;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.tree.DatasetPanel;
import org.genmapp.workspaces.utils.DatasetMapping;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.groups.CyGroup;
import cytoscape.groups.CyGroupManager;
import cytoscape.logger.CyLogger;
import cytoscape.view.CyNetworkView;

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

	private static CyLogger logger;

	/**
	 * Analogous to CyNetwork, this is the base class of all dataset objects in
	 * the workspaces panel. Called by data importers.
	 * 
	 * @param n
	 * @param t
	 * @param nl
	 * @param al
	 */
	public CyDataset(String n, String t, List<Integer> nl, List<String> al,
			CyLogger cyLogger) {
		this(n, t, nl, al, true, cyLogger);
	}

	public CyDataset(String n, String t, List<Integer> nl, List<String> al,
			boolean performDatasetMapping, CyLogger cyLogger) {
		name = n;
		this.keyType = t;
		this.nodes = nl;
		this.attrs = al;
		logger = cyLogger;

		datasetNameMap.put(n, this);

		this.rows = nl.size();

		// add to dataset panel
		GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().addItem(n, "droot");

		// perform dataset mapping
		if (performDatasetMapping) {

			JProgressBar progress = GenMAPPWorkspaces.wsPanel.getProgressBar();
			progress.setVisible(true);
			progress.setValue(0);
			progress.setStringPainted(true);

			DatasetMapping.performDatasetMapping(this, null, true, logger,
					progress);

			progress.setVisible(false);
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
	public static int[] getAllDatasetNodeIndexes() {
		List<Integer> temp = new ArrayList<Integer>();
		for (CyDataset dset : datasetNameMap.values()) {
			temp.addAll(dset.getNodes());
		}
		int[] dsetNodes = new int[temp.size()];
		Iterator<Integer> iterator = temp.iterator();
		for (int i = 0; i < dsetNodes.length; i++) {
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
	 * @param self
	 *            also delete reference in name map?
	 */
	public void deleteCyDatasetInfo(boolean self) {
		String dname = this.getName();

		/*
		 * If called during a session reset (i.e. new session or open session),
		 * then some of these functions are redundant with Cytoscape's
		 * network-level cleanup and can be skipped.
		 */
		if (Cytoscape.getSessionstate() != Cytoscape.SESSION_OPENED) {

			// first, process all metanodes
			List<CyGroup> mnList = CyGroupManager.getGroupList(CyGroupManager
					.getGroupViewer("metaNode"));
			Iterator<CyGroup> it = mnList.iterator();
			while (it.hasNext()) {
				CyGroup mn = it.next();
				List<CyNode> cnList = mn.getNodes();
				for (CyNode cn : cnList) {
					int cni = cn.getRootGraphIndex();
					if (nodes.contains(cni)) {

						cn.removeFromGroup(mn);

						for (CyNetwork cnet : Cytoscape.getNetworkSet()) {
							if (cnet.getNode(mn.getGroupNode()
									.getRootGraphIndex()) != null) {
								CyNetworkView cnv = Cytoscape
										.getNetworkView(cnet.getIdentifier());

								if (mn.getNodes().size() == 0) {
									// just removed the last child
									// TODO: clear remaining group attr values
									// e.g., NumChildren, NumDes, __groupsLocal
								} else {
									// rearrange remaining children
									arrangeChildren(mn, cnv);
								}
							}
						}
					}
				}

			}

			// next, remove the nodes from root graph
			for (int ni : nodes) {
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
					Cytoscape.getNetworkAttributes().setListAttribute(
							cnet.getIdentifier(),
							DatasetMapping.NET_ATTR_DATASETS, dlist);
				}

			}

			// TODO: remove attrs unique to this dataset
			// TODO: decide what to do with shared attr values
			// TODO: assess dependent criteriasets
			// TODO: go through all nodes and clear ds node attrs
			// TODO: etc
			// OR, never active dataset deletion!

		}

		// remove from panel
		GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().removeItem(
				this.getName());

		if (self) {
			// remove internal representation
			datasetNameMap.remove(this.getName());
		}

	}

	public static void arrangeChildren(CyGroup gn, CyNetworkView cnv) {

		// collect some measurements
		Point2D o = cnv.getNodeView(gn.getGroupNode()).getOffset();
		double h = cnv.getNodeView(gn.getGroupNode()).getHeight();
		double w = cnv.getNodeView(gn.getGroupNode()).getWidth();

		/*
		 * We have to expand (int=1) in order to "recapture" added nodes. this
		 * is basically functioning as the first half of MetaNode.recollapse().
		 */
		gn.setState(1);

		// CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();

		/*
		 * Now, reposition all children nodes around original cn.
		 */
		int n = gn.getNodes().size();
		if (n == 0)
			return;
		double d = Math.sqrt(Math.pow((h / 2), 2) + Math.pow((w / 2), 2));
		d = d * n / 3;
		double t = (360 / n);
		int i = 0;

		for (CyNode child : gn.getNodes()) {
			double y = Math.cos(Math.toRadians(t * i)) * d;
			double x = Math.sin(Math.toRadians(t * i)) * d;
			cnv.getNodeView(child).setOffset(o.getX() - x, o.getY() - y);
			// nodeAttrs.setAttribute(child.getIdentifier(), "__metanodeHintX",
			// o.getX()-x);
			// nodeAttrs.setAttribute(child.getIdentifier(), "__metanodeHintY",
			// o.getY()-y);
			i++;
		}

		/*
		 * This is the second half of MetaNode.recollapse(). Without this,
		 * CyNodeViews on new group nodes go to 'null' and the next attempt to
		 * add a child crashes with NPE.
		 */
		gn.setState(2);
	}
}
