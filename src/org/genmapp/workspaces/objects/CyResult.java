package org.genmapp.workspaces.objects;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JTabbedPane;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.tree.WorkspacesPanel;

public class CyResult {

	private String name;
	private String componentLabel;
	private JTabbedPane subTabbedPane;
	private int subComponentIndex;
	private boolean green;

	// Map of rname, CyResult objects
	public static Map<String, CyResult> resultNameMap = new HashMap<String, CyResult>();

	/**
	 * Analogous to CyNetwork, this is the base class of all result objects in
	 * the workspaces panel.
	 * 
	 * @param r
	 *            display name for result
	 */
	public CyResult(String r, String label) {
		this.name = r;
		this.componentLabel = label;
		this.subTabbedPane = null; //default
		this.subComponentIndex = 0; //default

		resultNameMap.put(r, this);

		WorkspacesPanel.getResultsTreePanel().addItem(r, "rroot");
	}

	public String getName() {
		return name;
	}

	/**
	 * @return the componentLabel
	 */
	public String getComponentLabel() {
		return componentLabel;
	}

	/**
	 * @param componentLabel
	 *            the componentLabel to set
	 */
	public void setComponentLabel(String componentLabel) {
		this.componentLabel = componentLabel;
	}

	/**
	 * @return the subTabbedPane
	 */
	public JTabbedPane getSubTabbedPane() {
		return subTabbedPane;
	}

	/**
	 * @param subTabbedPane
	 *            the subTabbedPane to set
	 */
	public void setSubTabbedPane(JTabbedPane subTabbedPane) {
		this.subTabbedPane = subTabbedPane;
	}

	/**
	 * @return the subComponentIndex
	 */
	public int getSubComponentIndex() {
		return subComponentIndex;
	}

	/**
	 * @param subComponentIndex
	 *            the subComponentIndex to set
	 */
	public void setSubComponentIndex(int subComponentIndex) {
		this.subComponentIndex = subComponentIndex;
	}

	/**
	 * @return the green
	 */
	public boolean isGreen() {
		return green;
	}

	/**
	 * @param green
	 *            the green to set
	 */
	public void setGreen(boolean green) {
		this.green = green;
		WorkspacesPanel.getResultsTreePanel().updateUI();
	}

	/**
 	 *
	 */
	public void deleteResult() {
		
		// TODO: delete component
		
		resultNameMap.remove(this.getName());
	}

}
