package org.genmapp.workspaces.objects;

import java.util.HashMap;
import java.util.Map;

public class CyAction {

	private boolean isDoable;
	private String title;
	private String description;
	private String requirements;
	
	public static Map<String, CyAction> actionNameMap = new HashMap<String, CyAction>();


	/**
	 * Action item objects to set "doable" status and other custom information.
	 */
	public CyAction(String title) {
		this.title = title;
		
		actionNameMap.put(title, this);
	}

	/**
	 * @return the isDoable
	 */
	public boolean isDoable() {
		return isDoable;
	}

	/**
	 * @param isDoable
	 *            the isDoable to set
	 */
	public void setDoable(boolean isDoable) {
		this.isDoable = isDoable;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the requirements
	 */
	public String getRequirements() {
		return requirements;
	}

	/**
	 * @param requirements
	 *            the requirements to set
	 */
	public void setRequirements(String requirements) {
		this.requirements = requirements;
	}

	// Override standard toString method to give a useful result
	public String toString() {
		return title;
	}

}
