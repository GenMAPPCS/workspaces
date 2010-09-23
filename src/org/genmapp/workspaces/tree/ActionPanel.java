package org.genmapp.workspaces.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SpringLayout;
import javax.swing.border.Border;

import org.genmapp.workspaces.utils.Downloader;

public class ActionPanel extends JPanel
		implements
			ActionListener,
			MouseListener {

	public static JComboBox actionCombobox;
	private JButton goButton;
	private JButton configButton;
	public static boolean workflowState;

	// // Action Strings
	// private final String NEW_NETWORK_FILE = "Open network from file...";
	// private final String NEW_NETWORK_WEB = "Load network from web...";
	// private final String NEW_NETWORK_TABLE = "Import network from table...";
	// private final String NEW_DATASET = "Import dataset from table...";
	// private final String NEW_CRITERIA_SET = "Create new criteria set...";
	// private final String RUN_CLUSTERMAKER = "Run clusterMaker on network...";
	// private final String RUN_GOELITE = "Run GO-Elite on criteria set...";

	// Action items
	public static ActionItem newNetworkFile = new ActionItem(
			"Open network from file...");
	public static ActionItem newNetworkWeb = new ActionItem(
			"Load network from web...");
	public static ActionItem newNetworkTable = new ActionItem(
			"Import network from table...");
	public static ActionItem newDatasetFile = new ActionItem(
			"Import dataset from table...");
	public static ActionItem newCriteriaSet = new ActionItem(
			"Create new criteria set...");
	public static ActionItem runClustermaker = new ActionItem(
			"Run clusterMaker...");
	public static ActionItem runGoelite = new ActionItem(
			"Run GO-Elite...");

	/**
	 * 
	 */
	public ActionPanel() {
		super();

		// initialize
		ActionItem actions[] = {newNetworkFile, newNetworkWeb, newNetworkTable,
				newDatasetFile, newCriteriaSet, runClustermaker, runGoelite};

		loadActions(actions, true);
		// actionCombobox = new JComboBox(actions);
		MyCellRenderer renderer = new MyCellRenderer();
		actionCombobox.setRenderer(renderer);
		actionCombobox.setToolTipText("Select an action to perform");
		actionCombobox.addActionListener(this);

		newNetworkFile.setDoable(true);
		newNetworkFile.setDescription("Select an xGMML, GPML, BioPAX, SIF or other supported network file format");
		newNetworkFile.setRequirements("Your must have an xGMML, GPML, BioPAX, SIF or other supported network file format");
		newNetworkWeb.setDoable(true);
		newNetworkWeb.setDescription("Search and browse content from WikiPathways, Pathway Commons and other web services");
		newNetworkWeb.setRequirements("You must be connected to the internet");
		newNetworkTable.setDoable(true);
		newNetworkTable.setDescription("Select a text file or spreadsheet from which to import a network");
		newNetworkTable.setRequirements("You must have a file with tabular data");
		newDatasetFile.setDoable(true);
		newDatasetFile.setDescription("Select a text file or spreadsheet from which to import a dataset");
		newDatasetFile.setRequirements("You must have a file with tabular data");
		newCriteriaSet.setDoable(false);
		newCriteriaSet.setDescription("Define criteria based on imported data to set node color");
		newCriteriaSet.setRequirements("You must first import a dataset");
		runClustermaker.setDoable(false);
		runClustermaker.setDescription("Perform heirarchical clustering on a network with associated data");
		runClustermaker.setRequirements("You must have a network with data");
		runGoelite.setDoable(false);
		runGoelite.setDescription("Perform GO/Pathway overrepresentation analysis per criteria");
		runGoelite.setRequirements("You must first define criteria");

		goButton = new JButton(new ImageIcon(getClass().getResource(
				"../images/go-green.png")));
		configButton = new JButton(new ImageIcon(getClass().getResource(
		"../images/configure.png")));
		configButton.setToolTipText("manually configure action list");
		
		goButton.addMouseListener(this);
		configButton.addMouseListener(this);

		this.add(actionCombobox);
		this.add(goButton);
		this.add(configButton);

		//trigger initial tooltips via actionPerformed
		actionCombobox.setSelectedIndex(0);

		// Layout
		SpringLayout layout = new SpringLayout();
		layout.putConstraint(SpringLayout.WEST, actionCombobox, 5,
				SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, actionCombobox, 2,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, goButton, 1,
				SpringLayout.EAST, actionCombobox);
		layout.putConstraint(SpringLayout.NORTH, goButton, 0,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, configButton, 1,
				SpringLayout.EAST, goButton);
		layout.putConstraint(SpringLayout.NORTH, configButton, 0,
				SpringLayout.NORTH, this);
		
		this.setLayout(layout);
		
		this.setMinimumSize(new Dimension(320, 58));
		this.setPreferredSize(new Dimension(320, 58));
		this.setMaximumSize(new Dimension(400, 58));
		
		Border etchedBdr = BorderFactory.createEtchedBorder();
		Border titledBdr = BorderFactory.createTitledBorder(etchedBdr,
				"Actions");
		this.setBorder(titledBdr);

		// set max width = to max width of SpeciesPanel
		this.setMaximumSize(new Dimension(400, this.getMaximumSize().height));

	}

	private void loadActions(ActionItem actions[], boolean workflow) {
		workflowState = workflow;
		actionCombobox = new JComboBox(actions);
	}

	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource().equals(actionCombobox)){			
			ActionItem action = (ActionItem) actionCombobox.getSelectedItem();
			
			// reset button optimistically
			goButton.setEnabled(true);
			goButton.setToolTipText(action.getDescription());
			
			//alter if not doable
			if (!action.isDoable()){
				goButton.setEnabled(false);
				goButton.setToolTipText(action.getRequirements());
			}
		}


	}

	public void mouseClicked(MouseEvent e) {

		if (e.getSource().equals(goButton) && goButton.isEnabled()) {
			// do stuff
			ActionItem action = (ActionItem) actionCombobox.getSelectedItem();
			System.out.println(action);
		} else if (e.getSource().equals(configButton) && configButton.isEnabled()){
			//open config dialog
			System.out.println("config");
		}

		// Set selection based on workflow
		if (e.getSource().equals(goButton) && goButton.isEnabled()
				&& workflowState) {
			Integer nextAction = actionCombobox.getSelectedIndex() + 1;
			boolean clear = false;
			while (!clear){
				if (nextAction >= actionCombobox.getItemCount()) {
					nextAction = 0;
				}
				actionCombobox.setSelectedIndex(nextAction);
				clear = ((ActionItem) actionCombobox.getSelectedItem()).isDoable();
				nextAction++;
			}
			
		}

	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}

/**
 * Action item objects to set "doable" status and other custom information.
 */
class ActionItem {
	private boolean isDoable;
	private String title;
	private String description;
	private String requirements;

	public ActionItem(String title) {
		this.title = title;
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
	 * @param description the description to set
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
	 * @param requirements the requirements to set
	 */
	public void setRequirements(String requirements) {
		this.requirements = requirements;
	}

	// Override standard toString method to give a useful result
	public String toString() {
		return title;
	}

}

/**
 * Renderer for action items.
 */
class MyCellRenderer extends JLabel implements ListCellRenderer {

	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {

		ActionItem action = (ActionItem) value;

		setText(value.toString());

		Color background;
		Color foreground;

		// check if this cell represents a doable action
		JList.DropLocation dropLocation = list.getDropLocation();
		if (!action.isDoable()) {

			background = Color.BLACK;
			foreground = Color.GRAY;

		}
		// check if this cell is selected
		else if (isSelected) {
			background = new Color(0, 0, 128); // TODO: NOT WORKING!
			foreground = new Color(0, 128, 0);

		} else {
			background = Color.WHITE;
			foreground = Color.BLACK;
		};

		setBackground(background);
		setForeground(foreground);

		return this;
	}

}
