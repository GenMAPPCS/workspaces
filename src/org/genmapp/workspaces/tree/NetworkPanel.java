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

import giny.model.Node;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
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
import org.genmapp.workspaces.objects.CyCriteria;
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.utils.DatasetMapping;

import cytoscape.CyNetwork;
import cytoscape.CyNetworkEvent;
import cytoscape.CyNetworkListener;
import cytoscape.CyNetworkTitleChange;
import cytoscape.Cytoscape;
import cytoscape.actions.ApplyVisualStyleAction;
import cytoscape.actions.CreateNetworkViewAction;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;
import cytoscape.logger.CyLogger;
import cytoscape.util.CyNetworkNaming;
import cytoscape.util.swing.JTreeTable;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.BiModalJSplitPane;

/**
 * GUI component for managing network list in current session.
 */
public class NetworkPanel extends JPanel
		implements
			PropertyChangeListener,
			TreeSelectionListener,
			SelectEventListener,
			ChangeListener {

	private static final long serialVersionUID = -4655014551140885915L;

	private static final int DEF_DEVIDER_LOCATION = 280;
	private static final int PANEL_PREFFERED_WIDTH = 250;

	private static final int DEF_ROW_HEIGHT = 20;

	// Make this panel as a source of events.
	private final SwingPropertyChangeSupport pcs;

	private final JTreeTable treeTable;
	private final GenericTreeNode root;

	private JPopupMenu popup;
	private PopupActionListener popupActionListener;

	private JMenuItem createViewItem;
	private JMenuItem destroyViewItem;
	private JMenuItem destroyNetworkItem;
	private JMenuItem editNetworkTitle;
	private JMenuItem applyVisualStyleMenu;

	private BiModalJSplitPane split;

	private boolean doNotEnterValueChanged = false;

	private final NetworkTreeTableModel networkTreeTableModel;
	// public final JPanel oriNetworkPanel = (JPanel)
	// Cytoscape.getDesktop().getCytoPanel(
	// SwingConstants.WEST).getComponentAt(1);

	/**
	 * Constructor for the Network Panel.
	 * 
	 * @param desktop
	 */
	public NetworkPanel() {
		super();

		root = new GenericTreeNode("Network Root", "nroot");
		networkTreeTableModel = new NetworkTreeTableModel(root);

		treeTable = new JTreeTable(networkTreeTableModel);
		treeTable
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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

		pcs = Cytoscape.getSwingPropertyChangeSupport(); // new
															// SwingPropertyChangeSupport
															// (this);

		// Make this a prop change listener for Cytoscape global events.
		Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(this);
		Cytoscape.getDesktop().getSwingPropertyChangeSupport()
				.addPropertyChangeListener(
						Cytoscape.getDesktop().NETWORK_VIEW_FOCUSED, this);

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

		// NETWORKS: create and populate the popup window
		popup = new JPopupMenu();
		editNetworkTitle = new JMenuItem(PopupActionListener.EDIT_NETWORK_TITLE);
		createViewItem = new JMenuItem(PopupActionListener.CREATE_VIEW);
		destroyViewItem = new JMenuItem(PopupActionListener.DESTROY_VIEW);
		destroyNetworkItem = new JMenuItem(PopupActionListener.DESTROY_NETWORK);
		applyVisualStyleMenu = new JMenu(PopupActionListener.APPLY_VISUAL_STYLE);
		// action listener which performs the tasks associated with the popup
		popupActionListener = new PopupActionListener();
		editNetworkTitle.addActionListener(popupActionListener);
		createViewItem.addActionListener(popupActionListener);
		destroyViewItem.addActionListener(popupActionListener);
		destroyNetworkItem.addActionListener(popupActionListener);
		applyVisualStyleMenu.addActionListener(popupActionListener);
		popup.add(editNetworkTitle);
		popup.add(createViewItem);
		popup.add(destroyViewItem);
		popup.add(destroyNetworkItem);
		popup.addSeparator();
		popup.add(applyVisualStyleMenu);

	}

	private void resetTable() {
		treeTable.getColumn(GenericColumnTypes.NETWORK.getDisplayName())
				.setPreferredWidth(170);
		treeTable.getColumn(GenericColumnTypes.NODES.getDisplayName())
				.setPreferredWidth(45);
		treeTable.getColumn(GenericColumnTypes.EDGES.getDisplayName())
				.setPreferredWidth(45);
		treeTable.setRowHeight(DEF_ROW_HEIGHT);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public SwingPropertyChangeSupport getSwingPropertyChangeSupport() {
		return pcs;
	}

	/**
	 * Remove a network from the panel.
	 * 
	 * @param network_id
	 */
	public void removeNetwork(final String network_id) {
		final GenericTreeNode node = getNetworkTreeNode(network_id);
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

		Cytoscape.getNetwork(network_id).removeSelectEventListener(this);
		node.removeFromParent();
		treeTable.getTree().updateUI();
		treeTable.doLayout();

		// reset view and network-dependent actions
		if (networkTreeTableModel.getChildCount(root) < 1) {
			this.setVisible(false);
			CyAction.actionNameMap.get(ActionPanel.RUN_CLUSTERMAKER).setDoable(
					false);
			CyAction.actionNameMap.get(ActionPanel.EXPORT_GRAPHICS).setDoable(
					false);
		}

	}

	/**
	 * update a network title
	 */
	public void updateTitle(final CyNetwork network) {
		// updates the title in the network or dataset panel
		if (treeTable.getTree().getSelectionPath() != null) {
			// user has selected a network
			networkTreeTableModel.setValueAt(network.getTitle(), treeTable
					.getTree().getSelectionPath().getLastPathComponent(), 0);
		} else {
			// no selection, means the title has been changed programmatically
			GenericTreeNode node = getNetworkTreeNode(network.getIdentifier());
			networkTreeTableModel.setValueAt(network.getTitle(), node, 0);
		}
		treeTable.getTree().updateUI();
		treeTable.doLayout();

		// updates the title in the networkViewMap
		Cytoscape.getDesktop().getNetworkViewManager().updateNetworkTitle(
				network);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param network_id
	 *            DOCUMENT ME!
	 * @param parent_id
	 *            DOCUMENT ME!
	 */
	public void addNetwork(String network_id, String parent_id) {
		// activate panel
		this.setVisible(true);
		// activate network-dependent actions
		CyAction.actionNameMap.get(ActionPanel.RUN_CLUSTERMAKER)
				.setDoable(true);
		CyAction.actionNameMap.get(ActionPanel.EXPORT_GRAPHICS).setDoable(true);
		// prompt next action
		if (CyDataset.datasetNameMap.isEmpty() && !ActionPanel.workflowState)
			ActionPanel.actionCombobox.setSelectedItem(CyAction.actionNameMap
					.get(ActionPanel.NEW_DATASET_TABLE));

		// first see if it exists
		if (getNetworkTreeNode(network_id) == null) {
			// logger.info("NetworkPanel: addNetwork " + network_id);
			GenericTreeNode dmtn = new GenericTreeNode(Cytoscape.getNetwork(
					network_id).getTitle(), network_id);
			Cytoscape.getNetwork(network_id).addSelectEventListener(this);

			if (parent_id != null && getNetworkTreeNode(parent_id) != null) {
				getNetworkTreeNode(parent_id).add(dmtn);
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

			// this is necessary because valueChanged is not fired above
			focusNetworkNode(network_id);
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param network_id
	 *            DOCUMENT ME!
	 */
	public void focusNetworkNode(String network_id) {
		// logger.info("NetworkPanel: focus network node");
		DefaultMutableTreeNode node = getNetworkTreeNode(network_id);

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
	 * @param network_id
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public GenericTreeNode getNetworkTreeNode(String network_id) {
		Enumeration tree_node_enum = root.breadthFirstEnumeration();

		while (tree_node_enum.hasMoreElements()) {
			GenericTreeNode node = (GenericTreeNode) tree_node_enum
					.nextElement();

			if ((String) node.getID() == network_id) {
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
		// TODO: Every time user select a network name, this method will be
		// called 3 times and run code twice!
		if (doNotEnterValueChanged)
			return;

		JTree mtree = treeTable.getTree();

		// sets the "current" network based on last node in the tree selected
		GenericTreeNode node = (GenericTreeNode) mtree
				.getLastSelectedPathComponent();
		if (node == null || node.getUserObject() == null)
			return;

		// System.out.println("NET_FOCUS: "+node.getID());
		pcs.firePropertyChange(new PropertyChangeEvent(this,
				CytoscapeDesktop.NETWORK_VIEW_FOCUS, null, node.getID()));
		// Cytoscape.getDesktop().setFocus(node.getID());

		// creates a list of all selected networks
		final List<String> networkList = new LinkedList<String>();
		try {
			for (int i = mtree.getMinSelectionRow(); i <= mtree
					.getMaxSelectionRow(); i++) {
				GenericTreeNode n = (GenericTreeNode) mtree.getPathForRow(i)
						.getLastPathComponent();
				if (n != null && n.getUserObject() != null
						&& mtree.isRowSelected(i))
					networkList.add(n.getID());
			}
		} catch (Exception ex) {
			CyLogger.getLogger().warn(
					"Exception handling network panel change: "
							+ ex.getMessage());
			ex.printStackTrace();
		}

		if (networkList.size() > 0) {
			Cytoscape.setSelectedNetworks(networkList);
			Cytoscape.setSelectedNetworkViews(networkList);

			// update dataset and criteriaset highlighting
			for (String net : networkList) {
				List<String> datasetList = Cytoscape
						.getNetworkAttributes()
						.getListAttribute(net, DatasetMapping.NET_ATTR_DATASETS);
				if (null != datasetList) {
					for (String dataset : CyDataset.datasetNameMap.keySet()) {
						if (datasetList.contains(dataset))
							CyDataset.datasetNameMap.get(dataset).isMappedToNetwork = true;
						else
							CyDataset.datasetNameMap.get(dataset).isMappedToNetwork = false;
					}
					DatasetPanel.getTreeTable().getTree().updateUI();
				}
				List<String> criteriasetList = Cytoscape.getNetworkAttributes()
						.getListAttribute(net, CyCriteria.NET_ATTR_SETS);
				if (null != criteriasetList){
					CyCriteriaset cset = CyCriteriaset.criteriaNameMap.get(criteriasetList.get(0));
					WorkspacesCommandHandler.applyCriteriasetToNetwork(cset, Cytoscape.getNetwork(net));
					CriteriasetPanel.getTreeTable().getTree().updateUI();
				}
			}
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param e
	 *            DOCUMENT ME!
	 */
	public void propertyChange(PropertyChangeEvent e) {
		// System.out.println("HEARD: " +e.getPropertyName());
		if (Cytoscape.NETWORK_CREATED.equals(e.getPropertyName())) {
			addNetwork((String) e.getNewValue(), (String) e.getOldValue());
		} else if (Cytoscape.NETWORK_DESTROYED.equals(e.getPropertyName())) {
			removeNetwork((String) e.getNewValue());
		} else if (CytoscapeDesktop.NETWORK_VIEW_FOCUSED.equals(e
				.getPropertyName())) {
			if (e.getSource() != this)
				focusNetworkNode((String) e.getNewValue());
		} else if (Cytoscape.NETWORK_TITLE_MODIFIED.equals(e.getPropertyName())) {
			CyNetworkTitleChange cyNetworkTitleChange = (CyNetworkTitleChange) e
					.getNewValue();
			String newID = cyNetworkTitleChange.getNetworkIdentifier();
			// String newTitle = cyNetworkTitleChange.getNetworkTitle();
			CyNetwork _network = Cytoscape.getNetwork(newID);
			// Network "0" is the default and does not appear in the panel
			if (_network != null && !_network.getIdentifier().equals("0"))
				updateTitle(_network);
		} else if (Cytoscape.CYTOSCAPE_INITIALIZED.equals(e.getPropertyName())) {
			updateVSMenu();
		}

		// and refresh
		treeTable.getTree().updateUI();
	}

	private void updateVSMenu() {
		applyVisualStyleMenu.removeAll();

		final Set<String> vsNames = new TreeSet<String>(Cytoscape
				.getVisualMappingManager().getCalculatorCatalog()
				.getVisualStyleNames());
		for (String name : vsNames) {
			final JMenuItem styleMenu = new JMenuItem(name);
			styleMenu.setAction(new ApplyVisualStyleAction(name));
			applyVisualStyleMenu.add(styleMenu);
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
		if (event.getTargetType() == SelectEvent.SINGLE_NODE
				|| event.getTargetType() == SelectEvent.NODE_SET) {
			final Set<Node> selectedNodes = (Set<Node>) Cytoscape
					.getCurrentNetwork().getSelectedNodes();
			final List<String> selectedNestedNetworkIDs = new ArrayList<String>();
			for (final Node node : selectedNodes) {
				final CyNetwork nestedNetwork = (CyNetwork) node
						.getNestedNetwork();
				if (nestedNetwork != null)
					selectedNestedNetworkIDs.add(nestedNetwork.getIdentifier());
			}

			doNotEnterValueChanged = true;
			try {
				final TreePath[] treePaths = new TreePath[selectedNestedNetworkIDs
						.size() + 1];
				int index = 0;
				final String currentNetworkID = Cytoscape.getCurrentNetwork()
						.getIdentifier();
				TreePath currentPath = null;
				final JTree tree = treeTable.getTree();
				for (int row = 0; row < tree.getRowCount(); ++row) {
					final TreePath path = tree.getPathForRow(row);
					final String ID = ((GenericTreeNode) path
							.getLastPathComponent()).getID();
					if (ID.equals(currentNetworkID))
						currentPath = path;
					else if (selectedNestedNetworkIDs.contains(ID))
						treePaths[index++] = path;
				}

				Cytoscape.setSelectedNetworks(selectedNestedNetworkIDs);

				treePaths[index] = currentPath;
				tree.getSelectionModel().setSelectionPaths(treePaths);
				tree.scrollPathToVisible(currentPath);
			} finally {
				doNotEnterValueChanged = false;
			}
		}

		treeTable.getTree().updateUI();

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
				// get the row where the mouse-click originated
				final int[] nselected = treeTable.getSelectedRows();

				if (nselected != null && nselected.length != 0) {
					boolean enableViewRelatedMenu = false;
					final int selectedItemCount = nselected.length;
					CyNetwork cyNetwork = null;
					final JTree tree = treeTable.getTree();
					for (int i = 0; i < selectedItemCount; i++) {

						final TreePath treePath = tree
								.getPathForRow(nselected[i]);
						final String networkID = (String) ((GenericTreeNode) treePath
								.getLastPathComponent()).getID();

						cyNetwork = Cytoscape.getNetwork(networkID);
						if (Cytoscape.viewExists(networkID)) {
							enableViewRelatedMenu = true;
						}
					}

					/*
					 * Edit title command will be enabled only when ONE network
					 * is selected.
					 */
					if (selectedItemCount == 1) {
						editNetworkTitle.setEnabled(true);
						popupActionListener.setActiveNetwork(cyNetwork);
					} else
						editNetworkTitle.setEnabled(false);

					if (enableViewRelatedMenu) {
						// At least one selected network has a view.
						createViewItem.setEnabled(true);
						destroyViewItem.setEnabled(true);
						applyVisualStyleMenu.setEnabled(true);
					} else {
						// None of the selected networks has view.
						createViewItem.setEnabled(true);
						destroyViewItem.setEnabled(false);
						applyVisualStyleMenu.setEnabled(false);
					}

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
	class PopupActionListener implements ActionListener {
		public static final String DESTROY_VIEW = "Destroy View";
		public static final String CREATE_VIEW = "Create View";
		public static final String DESTROY_NETWORK = "Destroy Network";
		public static final String EDIT_NETWORK_TITLE = "Edit Network Title";
		public static final String APPLY_VISUAL_STYLE = "Apply Visual Style";

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

			if (DESTROY_VIEW.equals(label)) {
				final List<CyNetwork> selected = Cytoscape
						.getSelectedNetworks();
				for (final CyNetwork network : selected) {
					final CyNetworkView targetView = Cytoscape
							.getNetworkView(network.getIdentifier());
					if (targetView != Cytoscape.getNullNetworkView()) {
						Cytoscape.destroyNetworkView(targetView);
					}
				}
			} else if (CREATE_VIEW.equals(label)) {
				final List<CyNetwork> selected = Cytoscape
						.getSelectedNetworks();

				for (CyNetwork network : selected) {
					if (!Cytoscape.viewExists(network.getIdentifier()))
						CreateNetworkViewAction
								.createViewFromCurrentNetwork(network);
				}
			} else if (DESTROY_NETWORK.equals(label)) {
				final List<CyNetwork> selected = Cytoscape
						.getSelectedNetworks();
				for (CyNetwork network : selected)
					Cytoscape.destroyNetwork(network);
			} else if (EDIT_NETWORK_TITLE.equals(label)) {
				CyNetworkNaming.editNetworkTitle(cyNetwork);
				Cytoscape.getDesktop().getNetworkPanel().updateTitle(cyNetwork);
			} else {
				CyLogger.getLogger().warn(
						"Unexpected network panel popup option");
			}
		}

		/**
		 * Right before the popup menu is displayed, this function is called so
		 * we know which network the user is clicking on to call for the popup
		 * menu
		 */
		public void setActiveNetwork(final CyNetwork cyNetwork) {
			this.cyNetwork = cyNetwork;
		}
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

			if (hasView(value)) {
				setBackgroundNonSelectionColor(java.awt.Color.green.brighter());
				setBackgroundSelectionColor(java.awt.Color.green.darker());
			} else {
				setBackgroundNonSelectionColor(java.awt.Color.red.brighter());
				setBackgroundSelectionColor(java.awt.Color.red.darker());
			}

			return this;
		}

		private boolean hasView(Object value) {
			GenericTreeNode node = (GenericTreeNode) value;
			setToolTipText(Cytoscape.getNetwork(node.getID()).getTitle());

			return Cytoscape.viewExists(node.getID());
		}

	}

}