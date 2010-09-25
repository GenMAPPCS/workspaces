package org.genmapp.workspaces.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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

import org.genmapp.workspaces.objects.CyAction;

public class ActionPanel extends JPanel
		implements
			ActionListener,
			MouseListener {

	public static JComboBox actionCombobox;
	private JButton goButton;
	private JButton configButton;
	public static boolean workflowState;

	// Action Strings
	public final static String NEW_NETWORK_FILE = "Open network from file...";
	public final static String NEW_NETWORK_WEB = "Load network from web...";
	public final static String NEW_NETWORK_TABLE = "Import network from table...";
	public final static String NEW_DATASET_TABLE = "Import dataset from table...";
	public final static String NEW_CRITERIA_SET = "Create new criteria set...";
	public final static String RUN_CLUSTERMAKER = "Run clusterMaker on network...";
	public final static String RUN_GOELITE = "Run GO-Elite on criteria set...";

	/**
	 * 
	 */
	public ActionPanel() {
		super();

		// initialize all actions and prepare default set
		CyAction actions[] = initializeActions();

		loadActions(actions, true);
		// actionCombobox = new JComboBox(actions);
		MyCellRenderer renderer = new MyCellRenderer();
		actionCombobox.setRenderer(renderer);
		actionCombobox.setToolTipText("Select an action to perform");
		actionCombobox.addActionListener(this);

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

		// trigger initial tooltips via actionPerformed
		actionCombobox.setSelectedIndex(0);

		// Layout
		SpringLayout layout = new SpringLayout();
		layout.putConstraint(SpringLayout.WEST, actionCombobox, 5,
				SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, actionCombobox, 2,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, goButton, 1, SpringLayout.EAST,
				actionCombobox);
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

	/**
	 * @return
	 */
	private final static CyAction[] initializeActions() {
		// Create all action items
		CyAction newNetworkFile = new CyAction(NEW_NETWORK_FILE);
		newNetworkFile.setDoable(true);
		newNetworkFile
				.setDescription("Select an xGMML, GPML, BioPAX, SIF or other supported network file format");
		newNetworkFile
				.setRequirements("Your must have an xGMML, GPML, BioPAX, SIF or other supported network file format");

		CyAction newNetworkWeb = new CyAction(NEW_NETWORK_WEB);
		newNetworkWeb.setDoable(true);
		newNetworkWeb
				.setDescription("Search and browse content from WikiPathways, Pathway Commons and other web services");
		newNetworkWeb.setRequirements("You must be connected to the internet");

		CyAction newNetworkTable = new CyAction(NEW_NETWORK_TABLE);
		newNetworkTable.setDoable(true);
		newNetworkTable
				.setDescription("Select a text file or spreadsheet from which to import a network");
		newNetworkTable
				.setRequirements("You must have a file with tabular data");

		CyAction newDatasetFile = new CyAction(NEW_DATASET_TABLE);
		newDatasetFile.setDoable(true);
		newDatasetFile
				.setDescription("Select a text file or spreadsheet from which to import a dataset");
		newDatasetFile
				.setRequirements("You must have a file with tabular data");

		CyAction newCriteriaSet = new CyAction(NEW_CRITERIA_SET);
		newCriteriaSet.setDoable(false);
		newCriteriaSet
				.setDescription("Define criteria based on imported data to set node color");
		newCriteriaSet.setRequirements("You must first import a dataset");

		CyAction runClustermaker = new CyAction(RUN_CLUSTERMAKER);
		runClustermaker.setDoable(false);
		runClustermaker
				.setDescription("Perform heirarchical clustering on a network with associated data");
		runClustermaker.setRequirements("You must have a network with data");

		CyAction runGoelite = new CyAction(RUN_GOELITE);
		runGoelite.setDoable(false);
		runGoelite
				.setDescription("Perform GO/Pathway overrepresentation analysis per criteria");
		runGoelite.setRequirements("You must first define criteria");


		// prepare default list of actions
		CyAction actions[] = {newNetworkFile, newNetworkWeb, newNetworkTable,
				newDatasetFile, newCriteriaSet, runClustermaker, runGoelite};
		
		return actions;
	}

	/**
	 * @param actions
	 * @param workflow
	 */
	private void loadActions(CyAction actions[], boolean workflow) {
		workflowState = workflow;
		actionCombobox = new JComboBox(actions);
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource().equals(actionCombobox)) {
			CyAction action = (CyAction) actionCombobox.getSelectedItem();

			// reset button optimistically
			goButton.setEnabled(true);
			goButton.setToolTipText(action.getDescription());

			// alter if not doable
			if (!action.isDoable()) {
				goButton.setEnabled(false);
				goButton.setToolTipText(action.getRequirements());
			}
		}

	}

	public void mouseClicked(MouseEvent e) {

		if (e.getSource().equals(goButton) && goButton.isEnabled()) {
			// do stuff
			CyAction action = (CyAction) actionCombobox.getSelectedItem();
			System.out.println(action);
		} else if (e.getSource().equals(configButton)
				&& configButton.isEnabled()) {
			// open config dialog
			System.out.println("config");
		}

		// Set selection based on workflow
		if (e.getSource().equals(goButton) && goButton.isEnabled()
				&& workflowState) {
			Integer nextAction = actionCombobox.getSelectedIndex() + 1;
			boolean clear = false;
			while (!clear) {
				if (nextAction >= actionCombobox.getItemCount()) {
					nextAction = 0;
				}
				actionCombobox.setSelectedIndex(nextAction);
				clear = ((CyAction) actionCombobox.getSelectedItem())
						.isDoable();
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
 * Renderer for action items.
 */
class MyCellRenderer extends JLabel implements ListCellRenderer {

	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {

		CyAction action = (CyAction) value;

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
