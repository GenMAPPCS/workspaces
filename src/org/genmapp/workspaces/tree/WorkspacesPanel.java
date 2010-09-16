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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.CyNetwork;
import cytoscape.CyNetworkTitleChange;
import cytoscape.Cytoscape;
import cytoscape.actions.ApplyVisualStyleAction;
import cytoscape.actions.CreateNetworkViewAction;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
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
public class WorkspacesPanel extends JPanel
		implements
			PropertyChangeListener,
			TreeSelectionListener,
			SelectEventListener,
			ChangeListener {

	private static final long serialVersionUID = -7102083850894612840L;

	private static final int DEF_DEVIDER_LOCATION = 280;
	private static final int PANEL_PREFFERED_WIDTH = 250;

	private static final int DEF_ROW_HEIGHT = 20;

	// Make this panel as a source of events.
	private final SwingPropertyChangeSupport pcs;

	private final JTreeTable nTreeTable;
//	private final JTreeTable dTreeTable;
	private final GenericTreeNode nroot;
//	private final GenericTreeNode droot;

	private JPanel navigatorPanel;

	private JPanel networkTreePanel;
	private DatasetPanel datasetTreePanel;
	private JPanel criteriaTreePanel;
	private JPanel analysisTreePanel;

	private JPopupMenu nPopup;
//	private JPopupMenu dPopup;
	private PopupActionListener nPopupActionListener;
//	private PopupActionListener dPopupActionListener;

	private JMenuItem createViewItem;
	private JMenuItem destroyViewItem;
	private JMenuItem destroyNetworkItem;
	private JMenuItem editNetworkTitle;
	private JMenuItem applyVisualStyleMenu;

//	private JMenuItem destroyDatasetItem;
//	private JMenuItem editDatasetTitle;
//	private JMenuItem reloadDataset;
//	private JMenuItem createNetwork;

	private BiModalJSplitPane split;

	private final NetworkTreeTableModel networkTreeTableModel;
//	private final DatasetTreeTableModel datasetTreeTableModel;
	private final CytoscapeDesktop cytoscapeDesktop;

	/**
	 * Constructor for the Network Panel.
	 * 
	 * @param desktop
	 */
	public WorkspacesPanel() {
		super();
		this.cytoscapeDesktop = Cytoscape.getDesktop();

		nroot = new GenericTreeNode("Network Root", "nroot");
//		droot = new GenericTreeNode("Dataset Root", "droot");
		networkTreeTableModel = new NetworkTreeTableModel(nroot);
//		datasetTreeTableModel = new DatasetTreeTableModel(droot);

		nTreeTable = new JTreeTable(networkTreeTableModel);
		nTreeTable
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
//		dTreeTable = new JTreeTable(datasetTreeTableModel);
//		dTreeTable
//				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		initialize();

		/*
		 * Remove CTR-A for enabling select all function in the main window.
		 */
		for (KeyStroke listener : nTreeTable.getRegisteredKeyStrokes()) {
			if (listener.toString().equals("ctrl pressed A")) {
				final InputMap map = nTreeTable.getInputMap();
				map.remove(listener);
				nTreeTable.setInputMap(WHEN_FOCUSED, map);
				nTreeTable.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map);
			}
		}
//		for (KeyStroke listener : dTreeTable.getRegisteredKeyStrokes()) {
//			if (listener.toString().equals("ctrl pressed A")) {
//				final InputMap map = dTreeTable.getInputMap();
//				map.remove(listener);
//				dTreeTable.setInputMap(WHEN_FOCUSED, map);
//				dTreeTable.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map);
//			}
//		}

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
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(PANEL_PREFFERED_WIDTH, 700));

		networkTreePanel = new JPanel();
		networkTreePanel.setLayout(new BoxLayout(networkTreePanel,
				BoxLayout.Y_AXIS));
		nTreeTable.getTree().addTreeSelectionListener(this);
		nTreeTable.getTree().setRootVisible(false);
		ToolTipManager.sharedInstance().registerComponent(nTreeTable);
		nTreeTable.getTree().setCellRenderer(new TreeCellRenderer());

//		datasetTreePanel = new JPanel();
//		datasetTreePanel.setLayout(new BoxLayout(datasetTreePanel,
//				BoxLayout.Y_AXIS));
//
//		dTreeTable.getTree().addTreeSelectionListener(this);
//		dTreeTable.getTree().setRootVisible(false);
//		ToolTipManager.sharedInstance().registerComponent(dTreeTable);
//		dTreeTable.getTree().setCellRenderer(new TreeCellRenderer());

		datasetTreePanel = new DatasetPanel();
		criteriaTreePanel = new CriteriaPanel();
		
		resetTable();

		navigatorPanel = new JPanel();
		navigatorPanel.setMinimumSize(new Dimension(180, 180));
		navigatorPanel.setMaximumSize(new Dimension(180, 180));
		navigatorPanel.setPreferredSize(new Dimension(180, 180));

		JScrollPane scroll = new JScrollPane(nTreeTable);
		networkTreePanel.add(scroll);

//		scroll = new JScrollPane(dTreeTable);
//		datasetTreePanel.add(scroll);

		JPanel wsPanel = new JPanel();
		wsPanel.setLayout(new GridLayout(3, 1, 0, 0));
		wsPanel.add(networkTreePanel);
		wsPanel.add(datasetTreePanel);
		wsPanel.add(criteriaTreePanel);

		split = new BiModalJSplitPane(cytoscapeDesktop,
				JSplitPane.VERTICAL_SPLIT, BiModalJSplitPane.MODE_SHOW_SPLIT,
				wsPanel, navigatorPanel);
		split.setResizeWeight(1);
		split.setDividerLocation(DEF_DEVIDER_LOCATION);
		add(split);

		// this mouse listener listens for the right-click event and will show
		// the pop-up
		// window when that occurrs
		nTreeTable.addMouseListener(new PopupListener());
//		dTreeTable.addMouseListener(new PopupListener());

		// NETWORKS: create and populate the popup window
		nPopup = new JPopupMenu();
		editNetworkTitle = new JMenuItem(PopupActionListener.EDIT_NETWORK_TITLE);
		createViewItem = new JMenuItem(PopupActionListener.CREATE_VIEW);
		destroyViewItem = new JMenuItem(PopupActionListener.DESTROY_VIEW);
		destroyNetworkItem = new JMenuItem(PopupActionListener.DESTROY_NETWORK);
		applyVisualStyleMenu = new JMenu(PopupActionListener.APPLY_VISUAL_STYLE);
		// action listener which performs the tasks associated with the popup
		nPopupActionListener = new PopupActionListener();
		editNetworkTitle.addActionListener(nPopupActionListener);
		createViewItem.addActionListener(nPopupActionListener);
		destroyViewItem.addActionListener(nPopupActionListener);
		destroyNetworkItem.addActionListener(nPopupActionListener);
		applyVisualStyleMenu.addActionListener(nPopupActionListener);
		nPopup.add(editNetworkTitle);
		nPopup.add(createViewItem);
		nPopup.add(destroyViewItem);
		nPopup.add(destroyNetworkItem);
		nPopup.addSeparator();
		nPopup.add(applyVisualStyleMenu);

		// DATASETS: create and populate the popup window
//		dPopup = new JPopupMenu();
//		destroyDatasetItem = new JMenuItem(PopupActionListener.DESTROY_DATASET);
//		editDatasetTitle = new JMenuItem(PopupActionListener.EDIT_DATASET_TITLE);
//		reloadDataset = new JMenuItem(PopupActionListener.RELOAD_DATA);
//		createNetwork = new JMenuItem(PopupActionListener.CREATE_NETWORK);
//		// action listener which performs the tasks associated with the popup
//		dPopupActionListener = new PopupActionListener();
//		destroyDatasetItem.addActionListener(dPopupActionListener);
//		editDatasetTitle.addActionListener(dPopupActionListener);
//		reloadDataset.addActionListener(dPopupActionListener);
//		createNetwork.addActionListener(dPopupActionListener);
//		dPopup.add(destroyDatasetItem);
//		dPopup.add(editDatasetTitle);
//		dPopup.add(reloadDataset);
//		dPopup.addSeparator();
//		dPopup.add(createNetwork);
	}

	private void resetTable() {
		// NETWORKS
		nTreeTable.getColumn(ColumnTypes.NETWORK.getDisplayName())
				.setPreferredWidth(170);
		nTreeTable.getColumn(ColumnTypes.NODES.getDisplayName())
				.setPreferredWidth(45);
		nTreeTable.getColumn(ColumnTypes.EDGES.getDisplayName())
				.setPreferredWidth(45);
		nTreeTable.setRowHeight(DEF_ROW_HEIGHT);
		// DATASETS
//		dTreeTable.getColumn(ColumnTypes.DATASET.getDisplayName())
//				.setPreferredWidth(215);
//		dTreeTable.getColumn(ColumnTypes.ROWS.getDisplayName())
//				.setPreferredWidth(45);
//		dTreeTable.setRowHeight(DEF_ROW_HEIGHT);
		
//		((CriteriaPanel) criteriaTreePanel).resetTable();
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

	// /**
	// * This is used by Session writer.
	// *
	// * @return
	// */
	// public JTreeTable getTreeTable() {
	// return nTreeTable;
	// }
	//
	// /**
	// * DOCUMENT ME!
	// *
	// * @return DOCUMENT ME!
	// */
	// public JPanel getNavigatorPanel() {
	// return navigatorPanel;
	// }
	//
	// /**
	// * DOCUMENT ME!
	// *
	// * @return DOCUMENT ME!
	// */
	// public SwingPropertyChangeSupport getSwingPropertyChangeSupport() {
	// return pcs;
	// }

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
			nroot.add(child);
		}

		Cytoscape.getNetwork(network_id).removeSelectEventListener(this);
		node.removeFromParent();
		nTreeTable.getTree().updateUI();
		nTreeTable.doLayout();
	}

	/**
	 * Remove a dataset from the panel.
	 * 
	 * @param dataset_id
	 */
//	public void removeDataset(final String dataset_id) {
//		final GenericTreeNode node = getDatasetTreeNode(dataset_id);
//		if (node == null)
//			return;
//
//		final Enumeration<GenericTreeNode> children = node.children();
//		GenericTreeNode child = null;
//		final List removed_children = new ArrayList();
//
//		while (children.hasMoreElements()) {
//			removed_children.add(children.nextElement());
//		}
//
//		for (Iterator i = removed_children.iterator(); i.hasNext();) {
//			child = (GenericTreeNode) i.next();
//			child.removeFromParent();
//			droot.add(child);
//		}
//
//		// Cytoscape.getNetwork(dataset_id).removeSelectEventListener(this);
//		node.removeFromParent();
//		dTreeTable.getTree().updateUI();
//		dTreeTable.doLayout();
//	}

	/**
	 * update a network title
	 */
	public void updateTitle(final CyNetwork network) {
		// updates the title in the network or dataset panel
		if (nTreeTable.getTree().getSelectionPath() != null) {
			// user has selected a network
			networkTreeTableModel.setValueAt(network.getTitle(), nTreeTable
					.getTree().getSelectionPath().getLastPathComponent(), 0);
		} else {
			// no selection, means the title has been changed programmatically
			GenericTreeNode node = getNetworkTreeNode(network.getIdentifier());
			networkTreeTableModel.setValueAt(network.getTitle(), node, 0);
		}
		nTreeTable.getTree().updateUI();
		nTreeTable.doLayout();

		// updates the title in the networkViewMap
		Cytoscape.getDesktop().getNetworkViewManager().updateNetworkTitle(
				network);
	}

	// /**
	// * DOCUMENT ME!
	// *
	// * @param event
	// * DOCUMENT ME!
	// */
	// public void onSelectEvent(SelectEvent event) {
	// // TODO is this the right method to call?
	// nTreeTable.getTree().updateUI();
	// }

	/**
	 * @return the datasetTreePanel
	 */
	public DatasetPanel getDatasetTreePanel() {
		return datasetTreePanel;
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
		// first see if it exists
		if (getNetworkTreeNode(network_id) == null) {
			// logger.info("NetworkPanel: addNetwork " + network_id);
			GenericTreeNode dmtn = new GenericTreeNode(Cytoscape.getNetwork(
					network_id).getTitle(), network_id);
			Cytoscape.getNetwork(network_id).addSelectEventListener(this);

			if (parent_id != null && getNetworkTreeNode(parent_id) != null) {
				getNetworkTreeNode(parent_id).add(dmtn);
			} else {
				nroot.add(dmtn);
			}

			// apparently this doesn't fire valueChanged
			nTreeTable.getTree().collapsePath(
					new TreePath(new TreeNode[]{nroot}));

			nTreeTable.getTree().updateUI();
			TreePath path = new TreePath(dmtn.getPath());
			nTreeTable.getTree().expandPath(path);
			nTreeTable.getTree().scrollPathToVisible(path);
			nTreeTable.doLayout();

			// this is necessary because valueChanged is not fired above
			focusNetworkNode(network_id);
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param dataset_id
	 *            DOCUMENT ME!
	 * @param parent_id
	 *            DOCUMENT ME!
	 */
//	public void addDataset(String dataset_id, String parent_id) {
//		// first see if it exists
//		if (getDatasetTreeNode(dataset_id) == null) {
//			GenericTreeNode dmtn = new GenericTreeNode(dataset_id, dataset_id);
//
//			if (parent_id != null && getDatasetTreeNode(parent_id) != null) {
//				getDatasetTreeNode(parent_id).add(dmtn);
//			} else {
//				droot.add(dmtn);
//			}
//
//			// apparently this doesn't fire valueChanged
//			dTreeTable.getTree().collapsePath(
//					new TreePath(new TreeNode[]{droot}));
//
//			dTreeTable.getTree().updateUI();
//			TreePath path = new TreePath(dmtn.getPath());
//			dTreeTable.getTree().expandPath(path);
//			dTreeTable.getTree().scrollPathToVisible(path);
//			dTreeTable.doLayout();
//
//			// this is necessary because valueChanged is not fired above
//			focusDatasetNode(dataset_id);
//		}
//	}

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
			nTreeTable.getTree().getSelectionModel().setSelectionPath(
					new TreePath(node.getPath()));
			nTreeTable.getTree().scrollPathToVisible(
					new TreePath(node.getPath()));
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param dataset_id
	 *            DOCUMENT ME!
	 */
//	public void focusDatasetNode(String dataset_id) {
//		// logger.info("NetworkPanel: focus network node");
//		DefaultMutableTreeNode node = getDatasetTreeNode(dataset_id);
//
//		if (node != null) {
//			// fires valueChanged if the network isn't already selected
//			dTreeTable.getTree().getSelectionModel().setSelectionPath(
//					new TreePath(node.getPath()));
//			dTreeTable.getTree().scrollPathToVisible(
//					new TreePath(node.getPath()));
//		}
//	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param network_id
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public GenericTreeNode getNetworkTreeNode(String network_id) {
		Enumeration tree_node_enum = nroot.breadthFirstEnumeration();

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
	 * DOCUMENT ME!
	 * 
	 * @param dataset_id
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
//	public GenericTreeNode getDatasetTreeNode(String dataset_id) {
//		Enumeration tree_node_enum = droot.breadthFirstEnumeration();
//
//		while (tree_node_enum.hasMoreElements()) {
//			GenericTreeNode node = (GenericTreeNode) tree_node_enum
//					.nextElement();
//
//			if ((String) node.getID() == dataset_id) {
//				return node;
//			}
//		}
//
//		return null;
//	}

	/**
	 * This method highlights a network or dataset in the Workspace Panel.
	 * 
	 * @param e
	 *            DOCUMENT ME!
	 */
	public void valueChanged(TreeSelectionEvent e) {
		// TODO: Every time user select a network name, this method will be
		// called 3 times!

		/*
		 * Support concurrent selections across panels
		 */
		// first handle NETWORKS
		JTree mtree = nTreeTable.getTree();

		// sets the "current" network based on last node in the tree selected
		GenericTreeNode node = (GenericTreeNode) mtree
				.getLastSelectedPathComponent();
		if (node == null || node.getUserObject() == null)
			return;

		pcs.firePropertyChange(new PropertyChangeEvent(this,
				CytoscapeDesktop.NETWORK_VIEW_FOCUS, null, (String) node
						.getID()));

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
		}

		// Then, in parallel, handle DATASETS
//		mtree = dTreeTable.getTree();
//
//		// sets the "current" dataset based on last node in the tree selected
//		node = (GenericTreeNode) mtree.getLastSelectedPathComponent();
//		if (node == null || node.getUserObject() == null)
//			return;
//
//		// creates a list of all selected datasets
//		final List<String> datasetList = new LinkedList<String>();
//		try {
//			for (int i = mtree.getMinSelectionRow(); i <= mtree
//					.getMaxSelectionRow(); i++) {
//				GenericTreeNode n = (GenericTreeNode) mtree.getPathForRow(i)
//						.getLastPathComponent();
//				if (n != null && n.getUserObject() != null
//						&& mtree.isRowSelected(i))
//					datasetList.add(n.getID());
//			}
//		} catch (Exception ex) {
//			CyLogger.getLogger().warn(
//					"Exception handling dataset panel change: "
//							+ ex.getMessage());
//			ex.printStackTrace();
//		}
//
//		if (datasetList.size() > 0) {
//			CyDataset.setSelectedDataset(datasetList);
//		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param e
	 *            DOCUMENT ME!
	 */
	public void propertyChange(PropertyChangeEvent e) {
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
			// Network "0" is the default and does not appear in the netowrk
			// panel
			if (_network != null && !_network.getIdentifier().equals("0"))
				updateTitle(_network);
		} else if (Cytoscape.CYTOSCAPE_INITIALIZED.equals(e.getPropertyName())) {
			updateVSMenu();
		}
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
				final int[] nselected = nTreeTable.getSelectedRows();
//				final int[] dselected = dTreeTable.getSelectedRows();
				
//				if (e.isShiftDown()){ // TODO:fake for DATASETS
//				//if (dselected.length > nselected.length) {
//					if (dselected != null && dselected.length != 0) {
//						final int selectedItemCount = dselected.length;
//
//						// Edit title command will be enabled only when ONE
//						// network
//						// is selected.
//						if (selectedItemCount == 1) {
//							editDatasetTitle.setEnabled(true);
//						} else
//							editDatasetTitle.setEnabled(false);
//
//						// At least one selected network has a view.
//						destroyDatasetItem.setEnabled(true);
//						reloadDataset.setEnabled(true);
//						createNetwork.setEnabled(true);
//
//						dPopup.show(e.getComponent(), e.getX(), e.getY());
//					}
//				} else {

					if (nselected != null && nselected.length != 0) {
						boolean enableViewRelatedMenu = false;
						final int selectedItemCount = nselected.length;
						CyNetwork cyNetwork = null;
						final JTree tree = nTreeTable.getTree();
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

						// Edit title command will be enabled only when ONE
						// network
						// is selected.
						if (selectedItemCount == 1) {
							editNetworkTitle.setEnabled(true);
							nPopupActionListener.setActiveNetwork(cyNetwork);
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

						nPopup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
//			}
		}
	}

	public void stateChanged(ChangeEvent e) {
		updateVSMenu();
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

	public void onSelectEvent(SelectEvent arg0) {
		// TODO Auto-generated method stub

	}
}

/**
 * This class listens for actions from the popup menu, it is responsible for
 * performing actions related to destroying and creating views, and destroying
 * the network.
 */
class PopupActionListener implements ActionListener {

	// NETWORKS
	public static final String DESTROY_VIEW = "Destroy View";
	public static final String CREATE_VIEW = "Create View";
	public static final String DESTROY_NETWORK = "Destroy Network";
	public static final String EDIT_NETWORK_TITLE = "Edit Network Title";
	public static final String APPLY_VISUAL_STYLE = "Apply Visual Style";

	// DATASETS
	public static final String DESTROY_DATASET = "Destroy Dataset";
	public static final String EDIT_DATASET_TITLE = "Edit Dataset Title";
	public static final String RELOAD_DATA = "Reload Dataset";
	public static final String CREATE_NETWORK = "Create Network from Dataset";

	/**
	 * This is the network which originated the mouse-click event (more
	 * appropriately, the network associated with the ID associated with the row
	 * associated with the JTable that originated the popup event
	 */
	protected CyNetwork cyNetwork;

	/**
	 * Based on the action event, destroy or create a view, or destroy a network
	 */
	public void actionPerformed(ActionEvent ae) {
		final String label = ((JMenuItem) ae.getSource()).getText();

		if (DESTROY_VIEW.equals(label)) {
			final List<CyNetwork> selected = Cytoscape.getSelectedNetworks();
			for (final CyNetwork network : selected) {
				final CyNetworkView targetView = Cytoscape
						.getNetworkView(network.getIdentifier());
				if (targetView != Cytoscape.getNullNetworkView()) {
					Cytoscape.destroyNetworkView(targetView);
				}
			}
		} else if (CREATE_VIEW.equals(label)) {
			final List<CyNetwork> selected = Cytoscape.getSelectedNetworks();

			for (CyNetwork network : selected) {
				if (!Cytoscape.viewExists(network.getIdentifier()))
					CreateNetworkViewAction
							.createViewFromCurrentNetwork(network);
			}
		} else if (DESTROY_NETWORK.equals(label)) {
			final List<CyNetwork> selected = Cytoscape.getSelectedNetworks();
			for (CyNetwork network : selected)
				Cytoscape.destroyNetwork(network);
		} else if (EDIT_NETWORK_TITLE.equals(label)) {
			CyNetworkNaming.editNetworkTitle(cyNetwork);
			Cytoscape.getDesktop().getNetworkPanel().updateTitle(cyNetwork);
		} else if (RELOAD_DATA.equals(label)) {
			List<String> selectedDatasets = CyDataset.getSelectedDatasets();
			for (String dataset : selectedDatasets) {
				URL url = CyDataset.datasetUrlMap.get(dataset);
				Map<String, Object> args = new HashMap<String, Object>();
				args.put("source", url);
				try {
					CyCommandResult result = CyCommandManager.execute(
							"genmappimporter", "reimport", args);
				} catch (CyCommandException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RuntimeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			CyLogger.getLogger().warn("Unexpected network panel popup option");
		}
	}

	/**
	 * Right before the popup menu is displayed, this function is called so we
	 * know which network the user is clicking on to call for the popup menu
	 */
	public void setActiveNetwork(final CyNetwork cyNetwork) {
		this.cyNetwork = cyNetwork;
	}
}
