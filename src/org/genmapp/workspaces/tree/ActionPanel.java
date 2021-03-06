package org.genmapp.workspaces.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SpringLayout;
import javax.swing.border.Border;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyAction;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.ui.CyActionConfigDialog;

import cytoscape.Cytoscape;
import cytoscape.actions.ExportAsGraphicsAction;
import cytoscape.actions.ImportGraphFileAction;
import cytoscape.actions.OpenSessionAction;
import cytoscape.actions.SaveSessionAction;
import cytoscape.actions.SaveSessionAsAction;
import cytoscape.actions.WebServiceNetworkImportAction;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.layout.CyLayouts;
import cytoscape.layout.LayoutTask;
import cytoscape.logger.CyLogger;
import cytoscape.task.util.TaskManager;
import cytoscape.view.CyNetworkView;

public class ActionPanel extends JPanel implements ActionListener, MouseListener {

	private static final long serialVersionUID = 7783752875541061715L;

	public static JComboBox actionCombobox = new JComboBox();
	private JButton goButton;
	private JButton configButton;
	public static boolean workflowState;
	private static int workflowIndex;

	// Action Strings
	public final static String OPEN_SESSION_FILE = "Open session file...";
	public final static String OPEN_NETWORK_FILE = "Open network file...";
	public final static String LOAD_NETWORK_WEB = "Load network from web...";
	public final static String LAYOUT_FORCE_DIRECTED2 = "Layout force-directed...";
	public final static String NEW_NETWORK_TABLE = "Import network from table...";
	public final static String NEW_DATASET_TABLE = "Import dataset from table...";
	public final static String NEW_CRITERIA_SET = "Create new criteria set...";
	public final static String RUN_CLUSTERMAKER = "Run clusterMaker...";
	public final static String RUN_GOELITE = "Run GO-Elite...";
	public final static String EXPORT_GRAPHICS = "Export image...";
	public final static String SAVE_SESSION_FILE = "Save session...";
	public final static String SAVE_SESSION_AS_FILE = "Save session as...";

	public static CyAction[] currentActionsList;
	public static CyAction[] availableActionsList;

	private CyLogger logger;

	/**
	 * 
	 */
	public ActionPanel(CyLogger cyLogger) {
		super();
		this.logger = cyLogger;

		// initialize all actions and prepare default set
		initializeActions();

		// actionCombobox = new JComboBox(actions);
		MyCellRenderer renderer = new MyCellRenderer();
		actionCombobox.setRenderer(renderer);
		actionCombobox.setToolTipText("Select an action to perform");
		actionCombobox.addActionListener(this);

		goButton = new JButton(new ImageIcon(getClass().getResource("../images/go-green.png")));
		configButton = new JButton(new ImageIcon(getClass().getResource("../images/configure.png")));
		configButton.setToolTipText("Manually configure action list");

		goButton.addMouseListener(this);
		configButton.addMouseListener(this);

		this.add(actionCombobox);
		this.add(goButton);
		this.add(configButton);

		// trigger initial tooltips via actionPerformed
		actionCombobox.setSelectedIndex(0);
		workflowIndex = 0;

		// Layout
		SpringLayout layout = new SpringLayout();
		layout.putConstraint(SpringLayout.WEST, actionCombobox, 5, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, actionCombobox, 2, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, goButton, 1, SpringLayout.EAST, actionCombobox);
		layout.putConstraint(SpringLayout.NORTH, goButton, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, configButton, 1, SpringLayout.EAST, goButton);
		layout.putConstraint(SpringLayout.NORTH, configButton, 0, SpringLayout.NORTH, this);

		this.setLayout(layout);

		this.setMinimumSize(new Dimension(320, 80));
		this.setPreferredSize(new Dimension(320, 100));
		this.setMaximumSize(new Dimension(400, 200));

		Border etchedBdr = BorderFactory.createEtchedBorder();
		Border titledBdr = BorderFactory.createTitledBorder(etchedBdr, "Actions");
		this.setBorder(titledBdr);

		// set max width = to max width of SpeciesPanel
		this.setMaximumSize(new Dimension(400, this.getMaximumSize().height));

	}

	/**
	 * @return
	 */
	private final static void initializeActions() {
		// Create all action items
		CyAction openSessionFile = new CyAction(OPEN_SESSION_FILE);
		openSessionFile.setDoable(true);
		openSessionFile.setDescription("Select a CYS session file");
		openSessionFile.setRequirements("Your must have a CYS cytoscape session file");

		CyAction openNetworkFile = new CyAction(OPEN_NETWORK_FILE);
		openNetworkFile.setDoable(true);
		openNetworkFile.setDescription("Select an xGMML, GPML, BioPAX, SIF or other supported network file format");
		openNetworkFile.setRequirements("Your must have an xGMML, GPML, BioPAX, SIF or other supported network file format");

		CyAction loadNetworkWeb = new CyAction(LOAD_NETWORK_WEB);
		loadNetworkWeb.setDoable(true);
		loadNetworkWeb.setDescription("Search and browse content from WikiPathways, Pathway Commons and other web services");
		loadNetworkWeb.setRequirements("You must be connected to the internet");

		CyAction layoutForceDirected = new CyAction(LAYOUT_FORCE_DIRECTED2);
		layoutForceDirected.setDoable(false);
		layoutForceDirected.setDescription("Layout selected network using force-directed");
		layoutForceDirected.setRequirements("You must have a network loaded");

		CyAction newNetworkTable = new CyAction(NEW_NETWORK_TABLE);
		newNetworkTable.setDoable(true);
		newNetworkTable.setDescription("Select a text file or spreadsheet from which to import a network");
		newNetworkTable.setRequirements("You must have a file with tabular data");

		CyAction newDatasetFile = new CyAction(NEW_DATASET_TABLE);
		newDatasetFile.setDoable(true);
		newDatasetFile.setDescription("Select a text file or spreadsheet from which to import a dataset");
		newDatasetFile.setRequirements("You must have a file with tabular data");

		CyAction newCriteriaSet = new CyAction(NEW_CRITERIA_SET);
		newCriteriaSet.setDoable(false);
		newCriteriaSet.setDescription("Define criteria based on imported data to set node color");
		newCriteriaSet.setRequirements("You must first import a dataset or a network with data");

		CyAction runClustermaker = new CyAction(RUN_CLUSTERMAKER);
		runClustermaker.setDoable(false);
		runClustermaker.setDescription("Perform heirarchical clustering on a network with associated data");
		runClustermaker.setRequirements("You must have a network with data");

		CyAction runGoelite = new CyAction(RUN_GOELITE);
		runGoelite.setDoable(true);
		runGoelite.setDescription("Perform GO/Pathway overrepresentation analysis per criteria");
		runGoelite.setRequirements("You must first define criteria");

		CyAction exportGraphics = new CyAction(EXPORT_GRAPHICS);
		exportGraphics.setDoable(false);
		exportGraphics.setDescription("Export current network view as an image file, e.g., PDF, SVG, JPG");
		exportGraphics.setRequirements("You must have an active network view selected");

		CyAction saveSessionFile = new CyAction(SAVE_SESSION_FILE);
		saveSessionFile.setDoable(true);
		saveSessionFile.setDescription("Save session");
		saveSessionFile.setRequirements("");

		CyAction saveSessionAsFile = new CyAction(SAVE_SESSION_AS_FILE);
		saveSessionAsFile.setDoable(true);
		saveSessionAsFile.setDescription("Save session as new CYS file");
		saveSessionAsFile.setRequirements("");

		// prepare full list of available actions
		availableActionsList = new CyAction[] { openSessionFile, openNetworkFile, loadNetworkWeb, layoutForceDirected, newNetworkTable, newDatasetFile, newCriteriaSet, runClustermaker, runGoelite, exportGraphics, saveSessionFile, saveSessionAsFile };

		// prepare default list of actions and behavior
		CyAction actions[] = { openSessionFile, openNetworkFile, loadNetworkWeb, newDatasetFile, newCriteriaSet, runGoelite, saveSessionFile, saveSessionAsFile };
		workflowState = false;
		loadActions(actions);
	}

	/**
	 * @param actions
	 * @param workflow
	 */
	public static void loadActions(CyAction actions[]) {
		currentActionsList = actions;
		workflowIndex = 0;
		actionCombobox.removeAllItems();
		for (CyAction ca : actions) {
			actionCombobox.addItem(ca);
		}
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource().equals(actionCombobox)) {
			CyAction action = (CyAction) actionCombobox.getSelectedItem();
			if (null == action) {
				return; // reloading list
			}

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

	public static void showMessage(String message) {
		/*
		 * JOptionPane.showMessageDialog( Cytoscape.getDesktop(), message, "",
		 * JOptionPane.ERROR_MESSAGE );
		 */
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getSource().equals(goButton) && goButton.isEnabled()) {
			// do stuff
			String action = ((CyAction) actionCombobox.getSelectedItem()).toString();
			System.out.println(action);

			if (action.equals(OPEN_SESSION_FILE)) {
				WorkspacesCommandHandler.showMessage("Open session file ");
				if (Cytoscape.getNetworkSet().size() == 0 && CyDataset.datasetNameMap.size() > 0) {
					// Show warning, in cases unique to GenMAPP-CS
					final String warning = "Current session will be lost.\nDo you want to continue?";
					final int result = JOptionPane.showConfirmDialog(Cytoscape.getDesktop(), warning, "Caution!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
					if (result == JOptionPane.NO_OPTION) {
						return;
					}
				}
				CyLogger.getLogger().info("open session file: ActionPanel");
				// now, open the new session file
				OpenSessionAction osa = new OpenSessionAction();

				/*
				 * TODO: Apparently this event is triggered 3x and causes NPEs.
				 * May need to add greenlight/sleep() strategy here to avoid
				 * consecutive events. Ugh...
				 */
				osa.actionPerformed(new ActionEvent(osa, ActionEvent.ACTION_PERFORMED, action));

				WorkspacesCommandHandler.showMessage("end open session file ");

				// GenMAPPWorkspaces.wsPanel.getCriteriaTreePanel().setVisible(
				// true );

			} else if (action.equals(OPEN_NETWORK_FILE)) {
				ImportGraphFileAction igfa = new ImportGraphFileAction(Cytoscape.getDesktop().getCyMenus());
				igfa.actionPerformed(new ActionEvent(igfa, ActionEvent.ACTION_PERFORMED, action));
			} else if (action.equals(LOAD_NETWORK_WEB)) {
				WebServiceNetworkImportAction wsnia = new WebServiceNetworkImportAction();
				wsnia.actionPerformed(new ActionEvent(wsnia, ActionEvent.ACTION_PERFORMED, action));
			} else if (action.equals(LAYOUT_FORCE_DIRECTED2)) {
				CyNetworkView cnv = Cytoscape.getCurrentNetworkView();
				if (null == cnv)
					return;
				System.out.println("CNV: " + cnv.getIdentifier());
				TaskManager.executeTask(new LayoutTask(CyLayouts.getLayout("force-directed"), cnv), LayoutTask.getDefaultTaskConfig());
			} else if (action.equals(NEW_NETWORK_TABLE)) {
				// TODO
				System.out.println("coming soon...");
			} else if (action.equals(NEW_DATASET_TABLE)) {
				Map<String, Object> noargs = new HashMap<String, Object>();
				try {
					CyCommandManager.execute("genmappimporter", "open dialog", noargs);
				} catch (CyCommandException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (RuntimeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} else if (action.equals(NEW_CRITERIA_SET)) {
				Map<String, Object> noargs = new HashMap<String, Object>();
				try {
					CyCommandManager.execute("criteriamapper", "open dialog", noargs);
				} catch (CyCommandException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (RuntimeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} else if (action.equals(RUN_CLUSTERMAKER)) {
				Map<String, Object> args = new HashMap<String, Object>();
				args.put("type", "hierarchical");
				try {
					CyCommandManager.execute("clustermaker", "showDialog", args);
				} catch (CyCommandException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (RuntimeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			} else if (action.equals(RUN_GOELITE)) {
				Map<String, Object> noargs = new HashMap<String, Object>();
				try {
					CyCommandManager.execute("goelite", "open dialog", noargs);
				} catch (CyCommandException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (RuntimeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			} else if (action.equals(EXPORT_GRAPHICS)) {
				ExportAsGraphicsAction eaga = new ExportAsGraphicsAction();
				eaga.actionPerformed(new ActionEvent(eaga, ActionEvent.ACTION_PERFORMED, action));

			} else if (action.equals(SAVE_SESSION_FILE)) {
				SaveSessionAction ssa = new SaveSessionAction();
				ssa.actionPerformed(new ActionEvent(ssa, ActionEvent.ACTION_PERFORMED, action));

			} else if (action.equals(SAVE_SESSION_AS_FILE)) {
				SaveSessionAsAction ssaa = new SaveSessionAsAction();
				ssaa.actionPerformed(new ActionEvent(ssaa, ActionEvent.ACTION_PERFORMED, action));
			}

		} else if (e.getSource().equals(configButton) && configButton.isEnabled()) {

			CyActionConfigDialog actionDialog = new CyActionConfigDialog(Cytoscape.getDesktop(), true);
			actionDialog.setLocationRelativeTo(Cytoscape.getDesktop());
			actionDialog.setVisible(true);
		}

		// Set selection based on workflow
		if (e.getSource().equals(goButton) && goButton.isEnabled() && workflowState) {
			Integer nextAction = workflowIndex + 1;
			boolean clear = false;
			while (!clear) {
				if (nextAction >= actionCombobox.getItemCount()) {
					nextAction = 0;
				}
				actionCombobox.setSelectedIndex(nextAction);
				clear = ((CyAction) actionCombobox.getSelectedItem()).isDoable();
				workflowIndex = nextAction;
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

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

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
		}
		;

		setBackground(background);
		setForeground(foreground);

		return this;
	}

}
