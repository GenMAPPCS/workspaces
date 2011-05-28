package org.genmapp.workspaces.tree;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.util.swing.AbstractTreeTableModel;

/**
 * Inner class that extends the AbstractTreeTableModel
 */
public final class NetworkTreeTableModel extends AbstractTreeTableModel {

	private List<GenericColumnTypes> columnNames;
	private final Map<String, Icon> networkIcons;

	public NetworkTreeTableModel(Object root) {
		super(root);
		columnNames = new ArrayList<GenericColumnTypes>();
		columnNames.add(GenericColumnTypes.NETWORK);
		columnNames.add(GenericColumnTypes.NODES);
		columnNames.add(GenericColumnTypes.EDGES);

		networkIcons = new HashMap<String, Icon>();
	}

	public void addColumn(final GenericColumnTypes model, final int idx) {
		columnNames.add(idx, model);
	}

	public void removeColumn(final int idx) {
		columnNames.remove(idx);
	}

	public Object getChild(Object parent, int index) {
		final Enumeration<?> tree_node_enum = ((DefaultMutableTreeNode) getRoot())
				.breadthFirstEnumeration();

		while (tree_node_enum.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree_node_enum
					.nextElement();

			if (node == parent) {
				return node.getChildAt(index);
			}
		}

		return null;
	}

	public int getChildCount(Object parent) {
		Enumeration<?> tree_node_enum = ((DefaultMutableTreeNode) getRoot())
				.breadthFirstEnumeration();

		while (tree_node_enum.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree_node_enum
					.nextElement();

			if (node == parent) {
				return node.getChildCount();
			}
		}

		return 0;
	}

	public int getColumnCount() {
		return columnNames.size();
	}

	public String getColumnName(int column) {
		return columnNames.get(column).getDisplayName();
	}

	public Class<?> getColumnClass(int column) {
		return columnNames.get(column).getType();
	}

	public Object getValueAt(Object node, int column) {
		if (columnNames.get(column).equals(GenericColumnTypes.NETWORK))
			return ((DefaultMutableTreeNode) node).getUserObject();
		else if (columnNames.get(column).equals(GenericColumnTypes.NODES)) {
			CyNetwork cyNetwork = Cytoscape.getNetwork(((GenericTreeNode) node)
					.getID());
			return "" + cyNetwork.getNodeCount() + "("
					+ cyNetwork.getSelectedNodes().size() + ")";
		} else if (columnNames.get(column).equals(GenericColumnTypes.EDGES)) {
			CyNetwork cyNetwork = Cytoscape.getNetwork(((GenericTreeNode) node)
					.getID());
			return "" + cyNetwork.getEdgeCount() + "("
					+ cyNetwork.getSelectedEdges().size() + ")";
		} else if (columnNames.get(column).equals(
				GenericColumnTypes.NETWORK_ICONS)) {
			return networkIcons.get(((GenericTreeNode) node).getID());
		}
		return "";
	}

	public void setValueAt(Object aValue, Object node, int column) {
		if (columnNames.get(column).equals(GenericColumnTypes.NETWORK)) {
			((DefaultMutableTreeNode) node).setUserObject(aValue);
		} else if (columnNames.get(column).equals(
				GenericColumnTypes.NETWORK_ICONS)
				&& aValue instanceof Icon) {
			networkIcons.put(((GenericTreeNode) node).getID(), new ImageIcon(
					getClass().getResource("network.png")));
		}
	}

	/**
	 * Overriding this method from AbstractTreeTableModel in order to prevent
	 * NullPointerException occurring with every network selection. None of the
	 * cells in this table are editable, so this is an easy fix.
	 */
	public boolean isCellEditable(Object node, int column) {
		return false;
	}
}