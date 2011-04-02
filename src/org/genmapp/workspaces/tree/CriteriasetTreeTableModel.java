package org.genmapp.workspaces.tree;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import org.genmapp.workspaces.objects.CyCriteriaset;

import cytoscape.Cytoscape;
import cytoscape.util.swing.AbstractTreeTableModel;

/**
 * Inner class that extends the AbstractTreeTableModel
 */
public final class CriteriasetTreeTableModel extends AbstractTreeTableModel {

	private List<GenericColumnTypes> columnNames;
	private final Map<String, Icon> icons;

	public CriteriasetTreeTableModel(Object root) {
		super(root);
		columnNames = new ArrayList<GenericColumnTypes>();
		columnNames.add(GenericColumnTypes.CRITERIA_SET);
		columnNames.add(GenericColumnTypes.ROWS);
		columnNames.add(GenericColumnTypes.NODES);

		icons = new HashMap<String, Icon>();
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
		if (columnNames.get(column).equals(GenericColumnTypes.CRITERIA_SET))
			return ((DefaultMutableTreeNode) node).getUserObject();
		else if (columnNames.get(column).equals(GenericColumnTypes.ROWS)) {
			return CyCriteriaset.criteriaRowsMap.get(((GenericTreeNode) node).getID());
		} else if (columnNames.get(column).equals(GenericColumnTypes.NODES)){
			Map<String, Integer> networkNodes = CyCriteriaset.criteriaNetworkNodesMap.get(((GenericTreeNode) node).getID());
			return networkNodes.get(Cytoscape.getCurrentNetwork().getIdentifier());
		} else if (columnNames.get(column).equals(GenericColumnTypes.CRITERIA_SET_ICONS)) {
			return icons.get(((GenericTreeNode) node).getID());
		}
		return "";
	}

	public void setValueAt(Object aValue, Object node, int column) {
		if (columnNames.get(column).equals(GenericColumnTypes.CRITERIA_SET)) {
			((DefaultMutableTreeNode) node).setUserObject(aValue);
		} else if (columnNames.get(column).equals(GenericColumnTypes.CRITERIA_SET_ICONS)
				&& aValue instanceof Icon) {
			icons.put(((GenericTreeNode) node).getID(),
					(Icon) aValue);
		}
	}
}