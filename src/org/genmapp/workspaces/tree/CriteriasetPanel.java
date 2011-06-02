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
import java.util.HashMap;
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
import javax.swing.SwingUtilities;
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
import cytoscape.CytoscapeInit;
import cytoscape.data.SelectEvent;
import cytoscape.logger.CyLogger;
import cytoscape.util.swing.JTreeTable;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.BiModalJSplitPane;
import cytoscape.visual.NodeShape;
import cytoscape.visual.VisualPropertyType;

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
	private JMenuItem applyCriteriaItem;
	private JMenuItem createNetworkItem;
	private JMenuItem selectNodesItem;
	private JMenuItem editCriteriaItem;
	private JMenuItem combineCriteriaItem;
	private JMenuItem clearCombinedCriteriaItem;

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

		/*
		 * This mouse listener listens for the right-click event and will show
		 * the pop-up window when that occurrs
		 */
		treeTable.addMouseListener(new PopupListener());

		// create and populate the popup window
		popup = new JPopupMenu();
		destroyCriteriaItem = new JMenuItem(
				PopupActionListener.DESTROY_CRITERIA);
		editCriteriaItem = new JMenuItem(PopupActionListener.EDIT_CRITERIA);
		applyCriteriaItem = new JMenuItem(PopupActionListener.APPLY_CRITERIA);
		createNetworkItem = new JMenuItem(PopupActionListener.CREATE_NETWORK);
		selectNodesItem = new JMenuItem(PopupActionListener.SELECT_NODES);
		combineCriteriaItem = new JMenuItem(
				PopupActionListener.COMBINE_CRITERIA);
		clearCombinedCriteriaItem = new JMenuItem(
				PopupActionListener.CLEAR_COMBINED_CRITERIA);
		popupActionListener = new PopupActionListener();
		destroyCriteriaItem.addActionListener(popupActionListener);
		editCriteriaItem.addActionListener(popupActionListener);
		applyCriteriaItem.addActionListener(popupActionListener);
		createNetworkItem.addActionListener(popupActionListener);
		selectNodesItem.addActionListener(popupActionListener);
		combineCriteriaItem.addActionListener(popupActionListener);
		clearCombinedCriteriaItem.addActionListener(popupActionListener);
		popup.add(applyCriteriaItem);
		popup.add(editCriteriaItem);
		popup.add(destroyCriteriaItem);
		popup.addSeparator();
		popup.add(selectNodesItem);
		popup.add(createNetworkItem);
		popup.addSeparator();
		popup.add(combineCriteriaItem);
		popup.add(clearCombinedCriteriaItem);
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
			 * focusNode() is necessary because valueChanged is not fired above.
			 * BUT, if you focus, then you engage calculation and application of
			 * criteriaset for selected network. This is not good when restoring
			 * saved session and you incidentally override last loaded network
			 * with last loaded criteriaset. SO, we just skip the table
			 * selection upon tree adding and leave this line commented out.
			 */
			// focusNode(id);
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param id
	 *            DOCUMENT ME!
	 */
	public void focusCriteriasetNode(String id) {
		// logger.info("CriteriasetPanel: focus criteriaset node");
		DefaultMutableTreeNode node = getTreeNode(id);

		if (node != null) {
			// fires valueChanged if the criteriaset isn't already selected
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
		 * and the code below is run twice (sometimes 2x). It's just an
		 * unfortunate fact of life...
		 */
		if (greenlight) {

			// block immediate redundant calls;
			greenlight = false;

			/*
			 * Start a thread to delay reset of this code by ~300 msec, so that
			 * it's only run once no matter how many times it's called.
			 */
			SwingWorker<Boolean, Void> workerA = new SwingWorker<Boolean, Void>() {

				public Boolean doInBackground() {
					// System.out.println("working");
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					/*
					 * invokeLater() avoids an NPE related to BasicTableUI when
					 * two consecutive calls occur outside of 300ms hold.
					 */
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							Set<CyNetwork> networks = new HashSet<CyNetwork>(
									Cytoscape.getSelectedNetworks());
							applyCriteriasetToNetworks(networks);
						}
					});
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
	private void applyCriteriasetToNetworks(Set<CyNetwork> networkList) {

		// reset hold on valueChanged() events
		greenlight = true;

		GenericTreeNode node = (GenericTreeNode) treeTable.getTree()
				.getLastSelectedPathComponent();
		if (node == null || node.getUserObject() == null)
			return;

		CyCriteriaset selectedCriteriaset = CyCriteriaset.criteriaNameMap
				.get(node.getID());

		for (CyNetwork network : networkList) {
			if (Cytoscape.viewExists(network.getIdentifier())) {
				WorkspacesCommandHandler.criteriaMapperApplySet(
						selectedCriteriaset, network);
			}
		}
		/*
		 * Reset current network and view based on NetworkPanel selection. This
		 * is necessary after "Apply To All", which leaves the last network
		 * handled as "current" even though it's not "selected". Not a good
		 * state to be in...
		 */
		String netname = WorkspacesPanel.getNetworkTreePanel()
				.getFocusNetworkNode();
		CyNetwork net = Cytoscape.getNetwork(netname);
		CyNetworkView netview = Cytoscape.getNetworkView(net.getIdentifier());
		Cytoscape.setCurrentNetwork(net.getIdentifier());
		Cytoscape.setCurrentNetworkView(netview.getIdentifier());
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
		// THIS METHOD IS NEVER CALLED!?
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
				// get the selected rows
				final int[] nselected = treeTable.getSelectedRows();

				if (nselected != null && nselected.length != 0) {

					// check for views on all selected networks
					// boolean enableViewRelatedMenu = true;
					// List<CyNetwork> networks =
					// Cytoscape.getSelectedNetworks();
					// for (CyNetwork net : networks) {
					// if (!Cytoscape.viewExists(net.getIdentifier())) {
					// enableViewRelatedMenu = false;
					// }
					// }

					/*
					 * Get clicked row and explicitly select it to cover the
					 * case of right-click. Works even if using
					 * multiple-selection model.
					 */
					int rowY = e.getY() / DEF_ROW_HEIGHT;
					treeTable.getTree().setSelectionRow(rowY);

					// set menu items defaults
					editCriteriaItem.setEnabled(true);
					destroyCriteriaItem.setEnabled(true);
					applyCriteriaItem.setEnabled(true);
					createNetworkItem.setEnabled(true);
					selectNodesItem.setEnabled(true);
					combineCriteriaItem.setEnabled(false);
					clearCombinedCriteriaItem.setEnabled(true);

					//enable items based on number of csets in panel
					if (treeTable.getTree().getRowCount() > 1){
						combineCriteriaItem.setEnabled(true);
					}
					
					
					// enable items based on multiple selection
					// if (nselected.length > 1) {
					// combineMenu.setEnabled(true);
					// clearCombinedCriteriaItem.setEnabled(true);
					// editCriteriaItem.setEnabled(false);
					// createNetworkItem.setEnabled(false);
					// selectNodesItem.setEnabled(false);
					// }

					// pop it!
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
		public static final String COMBINE_CRITERIA = "Combine All Criteria";
		public static final String CLEAR_COMBINED_CRITERIA = "Clear Combined Criteria";
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
				applyCriteriasetToNetworks(Cytoscape.getNetworkSet());
			} else if (EDIT_CRITERIA.equals(label)) {
				WorkspacesCommandHandler.openCriteriaMapper(node.getID());
			} else if (DESTROY_CRITERIA.equals(label)) {
				CyCriteriaset.criteriaNameMap.get(node.getID())
						.deleteCriteriaset();
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
			} else if (COMBINE_CRITERIA.equals(label)) {
				// double check that there are more than one cset to work with
				if (CyCriteriaset.criteriaNameMap.size() < 2){
					return;
				}
				
				// track nodes per colorlist to make more efficient cycommand
				// calls to nodecharts
				HashMap<String, List<String>> colorlistNodes = new HashMap<String, List<String>>();
				List<String> nodelist;
				List<String> colorlist = new ArrayList<String>();

				for (int ni : Cytoscape.getCurrentNetwork()
						.getNodeIndicesArray()) {
					String nodeid = Cytoscape.getRootGraph().getNode(ni)
							.getIdentifier();

					colorlist.clear();
					for (CyCriteriaset cset : CyCriteriaset.criteriaNameMap
							.values()) {
						String attr = Cytoscape.getNodeAttributes()
								.getStringAttribute(nodeid,
										cset.getNodeAttribute());
						String color = "#C0C0C0"; // default for "false" and "null"
						if (attr.equals("true")) {
							color = cset.getCriteriaParams()[1].split(":")[2];
						} else if (attr.startsWith("#")) {
							color = attr;
						}
						colorlist.add(color);
					}
					
					nodelist = colorlistNodes.get(colorlist.toString());
					if (null == nodelist) {
						nodelist = new ArrayList<String>();
					}
					nodelist.add(nodeid);
					colorlistNodes.put(colorlist.toString(), nodelist);
//					System.out.println("LIST1: "+colorlist+":"+nodelist);
					
				}

				// pie for ellipses; stripes for all others
//				NodeShape shape = NodeShape.ELLIPSE;
				NodeShape shape = (NodeShape) Cytoscape.getCurrentNetworkView()
				 .getVisualStyle()
				 .getNodeAppearanceCalculator().getDefaultAppearance()
				 .get(VisualPropertyType.NODE_SHAPE);
				System.out.println("NODE: "+colorlistNodes.size()+":"+colorlistNodes.keySet());
				
				for (String cl : colorlistNodes.keySet()) {
					if (shape.getShapeName().equals("Ellipse")) {
						WorkspacesCommandHandler.pieCriteria(colorlistNodes
								.get(cl).toString(), cl);
					} else {
						WorkspacesCommandHandler.stripeCriteria(colorlistNodes
								.get(cl).toString(), cl);
					}
				}

			} else if (CLEAR_COMBINED_CRITERIA.equals(label)) {
				// clear
				WorkspacesCommandHandler.clearCombinedCriteria();
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
		int[] dsetNodes = CyDataset.getAllDatasetNodes();

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

	public static int[] convertIntegers(List<Integer> integers) {
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++) {
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
