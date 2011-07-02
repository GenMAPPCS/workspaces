package org.genmapp.workspaces.objects;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JTabbedPane;

import org.genmapp.workspaces.GenMAPPWorkspaces;

public class CyResult {

	private String name;
	private String componentLabel;
	private JTabbedPane subTabbedPane;
	private String subComponentLabel;
	private boolean green;

	// Map of rname, CyResult objects
	public static Map<String, CyResult> resultNameMap = new HashMap<String, CyResult>();
	// Map of rname, componentLabel (label of component in CytoPanel EAST)
	// public static Map<String, String> resultComponentMap = new
	// HashMap<String, String>();
	// //Map of rname, subTabbedPane (subTabbedPane within component)
	// public static Map<String, JTabbedPane> resultTabbedPaneMap = new
	// HashMap<String, JTabbedPane>();
	// //Map of rname, subComponentLabel (label of subComponent within
	// subTabbedPane)
	// public static Map<String, String> resultSubComponentMap = new
	// HashMap<String, String>();

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

		resultNameMap.put(r, this);

		GenMAPPWorkspaces.wsPanel.getResultsTreePanel().addItem(r, "rroot");
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
	 * @return the subComponentLabel
	 */
	public String getSubComponentLabel() {
		return subComponentLabel;
	}

	/**
	 * @param subComponentLabel
	 *            the subComponentLabel to set
	 */
	public void setSubComponentLabel(String subComponentLabel) {
		this.subComponentLabel = subComponentLabel;
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
	}

	/**
 	 *
	 */
	public void deleteResult() {
		
		// TODO: delete component
		
		resultNameMap.remove(this.getName());
	}

}
