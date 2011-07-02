package org.genmapp.workspaces.tree;

import javax.swing.Icon;

import cytoscape.util.swing.TreeTableModel;

public enum GenericColumnTypes {
	NETWORK("Network", TreeTableModel.class), 
	NETWORK_ICONS("Overview", Icon.class), 
	NODES("Nodes", String.class), 
	EDGES("Edges", String.class),
	DATASET("Dataset", TreeTableModel.class), 
	DATASET_ICONS("Overview", Icon.class), 
	ROWS("Rows", String.class),
	CRITERIA_SET_ICONS("Overview", Icon.class), 
	CRITERIA_SET("Criteria Set", TreeTableModel.class), 
	CRITERIA("Criteria", String.class),
	RESULTS_ICONS("Overview", Icon.class), 
	RESULTS("Results", TreeTableModel.class); 

	private final String displayName;
	private final Class<?> type;

	private GenericColumnTypes(final String displayName, final Class<?> type) {
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
