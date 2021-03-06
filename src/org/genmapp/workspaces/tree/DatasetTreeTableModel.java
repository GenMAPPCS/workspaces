package org.genmapp.workspaces.tree;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.util.swing.AbstractTreeTableModel;

/**
 * Inner class that extends the AbstractTreeTableModel
 */
public final class DatasetTreeTableModel extends AbstractTreeTableModel {

	private List<GenericColumnTypes> columnNames;
	private final Map<String, Icon> datasetIcons;

	public DatasetTreeTableModel(Object root) {
		super(root);
		columnNames = new ArrayList<GenericColumnTypes>();
		columnNames.add(GenericColumnTypes.DATASET);
		columnNames.add(GenericColumnTypes.ROWS);

		datasetIcons = new HashMap<String, Icon>();
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

			if (node.equals(parent)) {
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

			if (node.equals(parent)) {
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
		if (columnNames.get(column).equals(GenericColumnTypes.DATASET))
			return ((DefaultMutableTreeNode) node).getUserObject();
		else if (columnNames.get(column).equals(GenericColumnTypes.ROWS)) {
			int rows = CyDataset.datasetNameMap.get(((GenericTreeNode) node).getID()).getRows();
			return "" + rows;
		} else if (columnNames.get(column).equals(GenericColumnTypes.DATASET_ICONS)) {
			return datasetIcons.get(((GenericTreeNode) node).getID());
		}
		return "";
	}

	public void setValueAt(Object aValue, Object node, int column) {
		if (columnNames.get(column).equals(GenericColumnTypes.DATASET)) {
			((DefaultMutableTreeNode) node).setUserObject(aValue);
		} else if (columnNames.get(column).equals(GenericColumnTypes.DATASET_ICONS)
				&& aValue instanceof Icon) {
			datasetIcons.put(((GenericTreeNode) node).getID(),
					(Icon) aValue);
		}
	}
}