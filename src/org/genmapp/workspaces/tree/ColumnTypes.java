package org.genmapp.workspaces.tree;

import javax.swing.Icon;

import cytoscape.util.swing.TreeTableModel;

public enum ColumnTypes {
	NETWORK("Network", TreeTableModel.class), 
	NETWORK_ICONS("Overview", Icon.class), 
	NODES("Nodes", String.class), 
	EDGES("Edges", String.class),
	DATASET("Dataset", TreeTableModel.class), 
	DATASET_ICONS("Overview", Icon.class), 
	ROWS("Rows", String.class);

	private final String displayName;
	private final Class<?> type;

	private ColumnTypes(final String displayName, final Class<?> type) {
		this.displayName = displayName;
		this.type = type;
	}

	public Class<?> getType() {
		return type;
	}

	public String getDisplayName() {
		return displayName;
	}
}
