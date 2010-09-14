package org.genmapp.workspaces.tree;

import javax.swing.tree.DefaultMutableTreeNode;

public class GenericTreeNode extends DefaultMutableTreeNode {
	
	private static final long serialVersionUID = -1504239724666254584L;
	
	private String uid;

	public GenericTreeNode(Object userobj, String id) {
		super(userobj.toString());
		uid = id;
	}

	protected void setID(String id) {
		uid = id;
	}

	protected String getID() {
		return uid;
	}
}