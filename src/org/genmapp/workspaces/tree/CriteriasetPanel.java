/*******************************************************************************
 * Copyright 2010 Alexander Pico
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.genmapp.workspaces.tree;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyAction;
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.SelectEvent;
import cytoscape.ding.DingNetworkView;
import cytoscape.logger.CyLogger;
import cytoscape.util.swing.JTreeTable;
import cytoscape.view.cytopanels.BiModalJSplitPane;

/**
 * GUI component for managing network list in current session.
 */
public class CriteriasetPanel extends JPanel
		implements
			PropertyChangeListener,
			TreeSelectionListener,
			ChangeListener {

	private static final long serialVersionUID = 3161161934074033023L;

	private static final int PANEL_PREFFERED_WIDTH = 250;

	private static final int DEF_ROW_HEIGHT = 20;

	private static boolean greenlight = true;

	// Make this panel as a source of events.
	private final SwingPropertyChangeSupport pcs;

	private static JTreeTable treeTable;
	private final GenericTreeNode root;

	private JPopupMenu popup;
	private PopupActionListener popupActionListener;

	private JMenuItem destroyCriteriaItem;
	private JMenuItem editCriteriaItem;
	private JMenuItem applyCriteriaItem;
	private JMenuItem createNetworkItem;
	private JMenuItem selectNodesItem;

	private BiModalJSplitPane split;

	private final CriteriasetTreeTableModel criteriaTreeTableModel;

	/**
	 * Constructor for the Network Panel.
	 * 
	 * @param desktop
	 */
	public CriteriasetPanel() {
		super();

		root = new GenericTreeNode("Criteria Root", "croot");
		criteriaTreeTableModel = new CriteriasetTreeTableModel(root);

		treeTable = new JTreeTable(criteriaTreeTableModel);
		treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		initialize();

		/*
		 * Remove CTR-A for enabling select all function in the main window.
		 */
		for (KeyStroke listener : treeTable.getRegisteredKeyStrokes()) {
			if (listener.toString().equals("ctrl pressed A")) {
				final InputMap map = treeTable.getInputMap();
				map.remove(listener);
				treeTable.setInputMap(WHEN_FOCUSED, map);
				treeTable.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map);
			}
		}

		pcs = new SwingPropertyChangeSupport(this);

		// Make this a prop change listener for Cytoscape global events.
		Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(this);

		// For listening to adding/removing Visual Style events.
		Cytoscape.getVisualMappingManager().addChangeListener(this);
	}

	/**
	 * Initialize GUI components
	 */
	private void initialize() {

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		treeTable.getTree().addTreeSelectionListener(this);
		treeTable.getTree().setRootVisible(false);
		ToolTipManager.sharedInstance().registerComponent(treeTable);
		treeTable.getTree().setCellRenderer(new TreeCellRenderer());

		resetTable();

		JScrollPane scroll = new JScrollPane(treeTable);
		this.add(scroll);

		// this mouse listener listens for the right-click event and will show
		// the pop-up
		// window when that occurrs
		treeTable.addMouseListener(new PopupListener());

		// create and populate the popup window
		popup = new JPopupMenu();
		destroyCriteriaItem = new JMenuItem(
				PopupActionListener.DESTROY_CRITERIA);
		editCriteriaItem = new JMenuItem(PopupActionListener.EDIT_CRITERIA);
		applyCriteriaItem = new JMenuItem(PopupActionListener.APPLY_CRITERIA);
		createNetworkItem = new JMenuItem(PopupActionListener.CREATE_NETWORK);
		selectNodesItem = new JMenuItem(PopupActionListener.SELECT_NODES);
		popupActionListener = new PopupActionListener();
		destroyCriteriaItem.addActionListener(popupActionListener);
		editCriteriaItem.addActionListener(popupActionListener);
		applyCriteriaItem.addActionListener(popupActionListener);
		createNetworkItem.addActionListener(popupActionListener);
		selectNodesItem.addActionListener(popupActionListener);
		popup.add(applyCriteriaItem);
		popup.add(editCriteriaItem);
		popup.add(destroyCriteriaItem);
		popup.addSeparator();
		popup.add(selectNodesItem);
		popup.add(createNetworkItem);
	}

	public void resetTable() {
		treeTable.getColumn(GenericColumnTypes.CRITERIA_SET.getDisplayName())
				.setPreferredWidth(190);
		treeTable.getColumn(GenericColumnTypes.CRITERIA.getDisplayName())
				.setPreferredWidth(50);
		treeTable.getColumn(GenericColumnTypes.NODES.getDisplayName())
				.setPreferredWidth(40);
		treeTable.setRowHeight(DEF_ROW_HEIGHT);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param comp
	 *            DOCUMENT ME!
	 */
	public void setNavigator(final Component comp) {
		split.setRightComponent(comp);
		split.validate();
	}

	/**
	 * Remove a item from the panel.
	 * 
	 * @param id
	 */
	public void removeItem(final String id) {
		final GenericTreeNode node = getTreeNode(id);
		if (node == null)
			return;

		final Enumeration<GenericTreeNode> children = node.children();
		GenericTreeNode child = null;
		final List removed_children = new ArrayList();

		while (children.hasMoreElements()) {
			removed_children.add(children.nextElement());
		}

		for (Iterator i = removed_children.iterator(); i.hasNext();) {
			child = (GenericTreeNode) i.next();
			child.removeFromParent();
			root.add(child);
		}

		// Cytoscape.getNetwork(dataset_id).removeSelectEventListener(this);
		node.removeFromParent();
		treeTable.getTree().updateUI();
		treeTable.doLayout();

		// reset view
		if (criteriaTreeTableModel.getChildCount(root) < 1) {
			this.setVisible(false);
			CyAction.actionNameMap.get(ActionPanel.RUN_GOELITE)
					.setDoable(false);
		}

	}

	/**
	 * 
	 * 
	 * @param id
	 * @param parent_id
	 * 
	 */
	public void addItem(String id, String parent_id) {
		// activate panel
		this.setVisible(true);
		// activate criteria-dependent actions
		CyAction.actionNameMap.get(ActionPanel.RUN_GOELITE).setDoable(true);

		// first see if it exists
		if (getTreeNode(id) == null) {
			GenericTreeNode dmtn = new GenericTreeNode(id, id);

			if (parent_id != null && getTreeNode(parent_id) != null) {
				getTreeNode(parent_id).add(dmtn);
			} else {
				root.add(dmtn);
			}

			// apparently this doesn't fire valueChanged
			treeTable.getTree()
					.collapsePath(new TreePath(new TreeNode[]{root}));

			treeTable.getTree().updateUI();
			TreePath path = new TreePath(dmtn.getPath());
			treeTable.getTree().expandPath(path);
			treeTable.getTree().scrollPathToVisible(path);
			treeTable.doLayout();

			/*
			 * This is necessary because valueChanged is not fired above. BUT,
			 * if you focus, then you engage calculation and application of
			 * criteriaset for selected network. This is not good when restoring
			 * saved session and you incidentally override last loaded network
			 * with last loaded criteriaset. SO, we just skip the table
			 * selection upon tree adding.
			 */
			// focusNode(id);
			// need to reset greenlight for criteria panel selections
			greenlight = true;
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param id
	 *            DOCUMENT ME!
	 */
	public void focusNode(String id) {
		// logger.info("CriteriasetPanel: focus criteriaset node");
		DefaultMutableTreeNode node = getTreeNode(id);

		if (node != null) {
			// fires valueChanged if the network isn't already selected
			treeTable.getTree().getSelectionModel().setSelectionPath(
					new TreePath(node.getPath()));
			treeTable.getTree().scrollPathToVisible(
					new TreePath(node.getPath()));
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param id
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public GenericTreeNode getTreeNode(String id) {
		Enumeration tree_node_enum = root.breadthFirstEnumeration();

		while (tree_node_enum.hasMoreElements()) {
			GenericTreeNode node = (GenericTreeNode) tree_node_enum
					.nextElement();

			if ((String) node.getID() == id) {
				return node;
			}
		}

		return null;
	}

	/**
	 * This method highlights an item in the Workspace Panel.
	 * 
	 * @param e
	 *            DOCUMENT ME!
	 */
	public void valueChanged(TreeSelectionEvent e) {
		/*
		 * NOTE: Every time user select an item, this method is called 3 times
		 * and the code below is run twice. Using "greenlight" hack to limit to
		 * a single run. This is ugly and the efficiency gains are debatable...
		 */
		// System.out.println("click on criteriaset: " + greenlight);
		if (greenlight) {

			// block immediate redundant calls;
			greenlight = false;

			/*
			 * Start a thread to delay reset of this code by ~300 msec, so that
			 * it's only run once.
			 */
			SwingWorker<Boolean, Void> workerA = new SwingWorker<Boolean, Void>() {

				public Boolean doInBackground() {
					// System.out.println("working");
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Set<CyNetwork> networks = new HashSet<CyNetwork>(Cytoscape
							.getSelectedNetworks());
					applyCriteriaToNetworks(networks);
					return true;
				}
			};
			workerA.execute();

		}
	}

	/**
	 * Trigger "apply set" on list of networks. Applies to current network when
	 * selected in panel; applies to all networks when context menu item
	 * selected.
	 */
	private void applyCriteriaToNetworks(Set<CyNetwork> networkList) {
		// sets the "current" criteriaset based on last node in the tree
		// selected
		GenericTreeNode node = (GenericTreeNode) treeTable.getTree()
				.getLastSelectedPathComponent();
		if (node == null || node.getUserObject() == null)
			return;

		// GenericTreeNode n = (GenericTreeNode) mtree.getSelectionPath()
		// .getLastPathComponent();

		CyCriteriaset selectedCriteriaset = CyCriteriaset.criteriaNameMap
				.get(node.getID());

		for (CyNetwork network : networkList) {
			if (Cytoscape.viewExists(network.getIdentifier())) {
				// TODO: NEED TO SYNC WITH ORIGINAL NETWORK PANEL
				Cytoscape.setCurrentNetwork(network.getIdentifier());
				Cytoscape.setCurrentNetworkView(network.getIdentifier());
				WorkspacesCommandHandler.applyCriteriasetToNetwork(
						selectedCriteriaset, network);
			}
		}
		greenlight = true;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param e
	 *            DOCUMENT ME!
	 */
	public void propertyChange(PropertyChangeEvent e) {
		// TODO: add appropriate items here
		if (Cytoscape.CYTOSCAPE_INITIALIZED.equals(e.getPropertyName())) {
			// ?
		}
	}

	public void stateChanged(ChangeEvent e) {
		// TODO ?
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param event
	 *            DOCUMENT ME!
	 */
	public void onSelectEvent(SelectEvent event) {
		// TODO is this the right method to call?
		System.out.println("onSelectEvent: " + event.getSource());
		treeTable.getTree().updateUI();
	}

	/**
	 * @return the treeTable
	 */
	public static JTreeTable getTreeTable() {
		return treeTable;
	}

	/**
	 * This class listens to mouse events from the TreeTable, if the mouse event
	 * is one that is canonically associated with a popup menu (ie, a right
	 * click) it will pop up the menu with option for destroying view, creating
	 * view, and destroying network (this is platform specific apparently)
	 */
	protected class PopupListener extends MouseAdapter {
		/**
		 * Don't know why you need both of these, but this is how they did it in
		 * the example
		 */
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		/*
		 * On windows, popup is triggered by this method, not the above one
		 */
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		/**
		 * if the mouse press is of the correct type, this function will maybe
		 * display the popup
		 */
		private void maybeShowPopup(MouseEvent e) {
			// check for the popup type
			if (e.isPopupTrigger()) {
				/*
				 * Perform integer division to set row based on right-click
				 * action
				 */
				int rowY = e.getY() / DEF_ROW_HEIGHT;
				treeTable.getTree().setSelectionRow(rowY);

				// get the row where the mouse-click originated
				final int selected = treeTable.getSelectedRow();

				if (selected >= 0) {
					editCriteriaItem.setEnabled(true);
					destroyCriteriaItem.setEnabled(true);
					applyCriteriaItem.setEnabled(true);
					createNetworkItem.setEnabled(true);

					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}
	}

	/**
	 * This class listens for actions from the popup menu, it is responsible for
	 * performing actions related to destroying and creating views, and
	 * destroying the network.
	 */
	protected class PopupActionListener implements ActionListener {

		public static final String APPLY_CRITERIA = "Apply to All Networks";
		public static final String EDIT_CRITERIA = "Edit Criteria";
		public static final String DESTROY_CRITERIA = "Destroy Criteria";
		public static final String SELECT_NODES = "Select Nodes in Network";
		public static final String CREATE_NETWORK = "Create Network from All";

		/**
		 * This is the network which originated the mouse-click event (more
		 * appropriately, the network associated with the ID associated with the
		 * row associated with the JTable that originated the popup event
		 */
		protected CyNetwork cyNetwork;

		/**
		 * Based on the action event, destroy or create a view, or destroy a
		 * network
		 */
		public void actionPerformed(ActionEvent ae) {
			final String label = ((JMenuItem) ae.getSource()).getText();

			GenericTreeNode node = (GenericTreeNode) treeTable.getTree()
					.getLastSelectedPathComponent();

			if (node == null || node.getUserObject() == null)
				return;

			if (APPLY_CRITERIA.equals(label)) {
				applyCriteriaToNetworks(Cytoscape.getNetworkSet());
			} else if (EDIT_CRITERIA.equals(label)) {
				WorkspacesCommandHandler.openCriteriaMapper(node.getID());
			} else if (DESTROY_CRITERIA.equals(label)) {
				CyCriteriaset.criteriaNameMap.get(node.getID())
						.deleteCyCriteriaset();
			} else if (CREATE_NETWORK.equals(label)) {
				System.out.println("create network");
				createNetworkFromCriteria(node.getID()); // TODO
			} else if (SELECT_NODES.equals(label)) {
				System.out.println("select ndoes");
				CyCriteriaset set = CyCriteriaset.criteriaNameMap.get(node
						.getID());
				List<CyNode> hitList = set.collectCriteriaNodes(Cytoscape
						.getRootGraph().getNodeIndicesArray());
				Cytoscape.getCurrentNetwork().unselectAllNodes();
				Cytoscape.getCurrentNetwork().setSelectedNodeState(hitList,
						true);
				Cytoscape.getCurrentNetworkView().updateView();
			} else {
				CyLogger.getLogger().warn("Unexpected panel popup option");
			}
		}
	}

	/**
	 * @param set
	 * @param keyType
	 */
	private void createNetworkFromCriteria(String criteriasetName) {

		// collect node list assembled from all CyDatasets
		List<Integer> temp = new ArrayList<Integer>();
		for (CyDataset dset : CyDataset.datasetNameMap.values()) {
			temp.addAll(dset.getNodes());
		}
		int[] dsetNodes = new int[temp.size()];
	    Iterator<Integer> iterator = temp.iterator();
	    for (int i = 0; i < dsetNodes.length; i++)
	    {
	    	dsetNodes[i] = iterator.next().intValue();
	    }
		
		CyCriteriaset set = CyCriteriaset.criteriaNameMap.get(criteriasetName);
		List<CyNode> hitList = set.collectCriteriaNodes(dsetNodes);
		List<CyEdge> edges = new ArrayList<CyEdge>();
		System.out.println("HITS: " + hitList.size());
		boolean goForIt = false;
		if (null == hitList || hitList.size() < 1) {
			System.out
					.println("Sorry, no nodes associated with this criteriaset");
		} else if (hitList.size() > 2000) {
			int n = JOptionPane.showConfirmDialog(Cytoscape.getDesktop(),
					"You are about to create a network of " + hitList.size()
							+ " nodes.", "Warning",
					JOptionPane.OK_CANCEL_OPTION);
			if (n == JOptionPane.OK_OPTION)
				goForIt = true;
		} else {
			goForIt = true;
		}

		if (goForIt) {
			CyNetwork newNetwork = Cytoscape.createNetwork(hitList, edges,
					criteriasetName);
			Object[] new_value = new Object[2];
			new_value[0] = newNetwork;
			new_value[1] = newNetwork.getIdentifier();
			Cytoscape.firePropertyChange(Cytoscape.NETWORK_LOADED, null,
					new_value);
		}
	}

	public static int[] convertIntegers(List<Integer> integers)
	{
	    int[] ret = new int[integers.size()];
	    Iterator<Integer> iterator = integers.iterator();
	    for (int i = 0; i < ret.length; i++)
	    {
	        ret[i] = iterator.next().intValue();
	    }
	    return ret;
	}

	/**
	 * This class handles the rendering of tree nodes in this panel.
	 */
	class TreeCellRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = -678559990857492912L;

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded,
					leaf, row, hasFocus);

			String nodeid = ((GenericTreeNode) value).getID();

			if (!nodeid.equals("croot")) {
				CyCriteriaset cs = CyCriteriaset.criteriaNameMap.get(nodeid);
				setToolTipText(cs.getCriteriaParamString());

				CyNetwork currnet = Cytoscape.getCurrentNetwork();
				Integer nodeCount = -1; // let null show green
				if (currnet != null) {
					Integer i = CyCriteriaset.criteriaNetworkNodesMap.get(
							nodeid).get(
							Cytoscape.getCurrentNetwork().getIdentifier());
					if (i != null)
						nodeCount = i;
				}

				if (nodeCount != 0) {
					setBackgroundNonSelectionColor(java.awt.Color.green
							.brighter());
					setBackgroundSelectionColor(java.awt.Color.green.darker());
				} else {
					setBackgroundNonSelectionColor(java.awt.Color.red
							.brighter());
					setBackgroundSelectionColor(java.awt.Color.red.darker());
				}
			}

			return this;
		}

	}
}
