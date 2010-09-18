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
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.InputMap;
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

import org.genmapp.workspaces.objects.CyCriteria;
import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;
import cytoscape.logger.CyLogger;
import cytoscape.util.swing.JTreeTable;
import cytoscape.view.cytopanels.BiModalJSplitPane;

/**
 * GUI component for managing network list in current session.
 */
public class CriteriaPanel extends JPanel
		implements
			PropertyChangeListener,
			TreeSelectionListener,
			SelectEventListener,
			ChangeListener {

	private static final long serialVersionUID = -7102083850894612840L;

	private static final int PANEL_PREFFERED_WIDTH = 250;

	private static final int DEF_ROW_HEIGHT = 20;

	// Make this panel as a source of events.
	private final SwingPropertyChangeSupport pcs;

	private final JTreeTable treeTable;
	private final GenericTreeNode root;

	private JPopupMenu popup;
	private PopupActionListener popupActionListener;

	private JMenuItem destroyCriteriaItem;
	private JMenuItem editCriteriaTitle;
	private JMenuItem applyCriteria;

	private BiModalJSplitPane split;

	private final CriteriaTreeTableModel criteriaTreeTableModel;

	/**
	 * Constructor for the Network Panel.
	 * 
	 * @param desktop
	 */
	public CriteriaPanel() {
		super();

		root = new GenericTreeNode("Criteria Root", "droot");
		criteriaTreeTableModel = new CriteriaTreeTableModel(root);

		treeTable = new JTreeTable(criteriaTreeTableModel);
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
		editCriteriaTitle = new JMenuItem(
				PopupActionListener.EDIT_CRITERIA_TITLE);
		applyCriteria = new JMenuItem(PopupActionListener.APPLY_CRITERIA);
		// action listener which performs the tasks associated with the popup
		popupActionListener = new PopupActionListener();
		destroyCriteriaItem.addActionListener(popupActionListener);
		editCriteriaTitle.addActionListener(popupActionListener);
		applyCriteria.addActionListener(popupActionListener);
		popup.add(destroyCriteriaItem);
		popup.add(editCriteriaTitle);
		popup.add(applyCriteria);
	}

	public void resetTable() {
		treeTable.getColumn(GenericColumnTypes.CRITERIA_SET.getDisplayName())
				.setPreferredWidth(180);
		treeTable.getColumn(GenericColumnTypes.CRITERIA.getDisplayName())
				.setPreferredWidth(40);
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
	}

	/**
	 * 
	 * 
	 * @param id
	 * @param parent_id
	 * 
	 */
	public void addItem(String id, String parent_id) {
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

			// this is necessary because valueChanged is not fired above
			focusNode(id);
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param id
	 *            DOCUMENT ME!
	 */
	public void focusNode(String id) {
		// logger.info("NetworkPanel: focus network node");
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
		// TODO: Every time user select a network name, this method will be
		// called 3 times!

		// do nothing. no selection model for datasets

		//
		// /*
		// * Support concurrent selections across panels
		// */
		// JTree mtree = TreeTable.getTree();
		//
		// // sets the "current" dataset based on last node in the tree selected
		// GenericTreeNode node = (GenericTreeNode)
		// mtree.getLastSelectedPathComponent();
		// if (node == null || node.getUserObject() == null)
		// return;
		//
		// // creates a list of all selected datasets
		// final List<String> datasetList = new LinkedList<String>();
		// try {
		// for (int i = mtree.getMinSelectionRow(); i <= mtree
		// .getMaxSelectionRow(); i++) {
		// GenericTreeNode n = (GenericTreeNode) mtree.getPathForRow(i)
		// .getLastPathComponent();
		// if (n != null && n.getUserObject() != null
		// && mtree.isRowSelected(i))
		// datasetList.add(n.getID());
		// }
		// } catch (Exception ex) {
		// CyLogger.getLogger().warn(
		// "Exception handling dataset panel change: "
		// + ex.getMessage());
		// ex.printStackTrace();
		// }
		//
		// if (datasetList.size() > 0) {
		// CyDataset.setSelectedDataset(datasetList);
		// }
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
				final int[] selected = treeTable.getSelectedRows();

				// if (e.isShiftDown()){ // TODO:fake for DATASETS
				// if (dselected.length > nselected.length) {
				if (selected != null && selected.length != 0) {
					final int selectedItemCount = selected.length;

					// Edit title command will be enabled only when ONE
					// network
					// is selected.
					if (selectedItemCount == 1) {
						editCriteriaTitle.setEnabled(true);
					} else
						editCriteriaTitle.setEnabled(false);

					// At least one selected network has a view.
					destroyCriteriaItem.setEnabled(true);
					applyCriteria.setEnabled(true);

					popup.show(e.getComponent(), e.getX(), e.getY());
					// }
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

		public static final String DESTROY_CRITERIA = "Destroy Criteria";
		public static final String EDIT_CRITERIA_TITLE = "Edit Criteria Title";
		public static final String APPLY_CRITERIA = "Apply Criteria";

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

			if (APPLY_CRITERIA.equals(label)) {
				// TODO
				// List<String> selectedCriteria =
				// Criteria.getSelectedCriteria();
				// for (String criteria : selectedCriteria) {
				// TODO
				// }
			} else if (EDIT_CRITERIA_TITLE.equals(label)) {
				// TODO
			} else if (DESTROY_CRITERIA.equals(label)) {
				// TODO
			} else {
				CyLogger.getLogger().warn("Unexpected panel popup option");
			}
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

			if (isCriteria(value)) {
				setBackgroundNonSelectionColor(java.awt.Color.green.brighter());
				setBackgroundSelectionColor(java.awt.Color.green.darker());
			} else {
				setBackgroundNonSelectionColor(java.awt.Color.red.brighter());
				setBackgroundSelectionColor(java.awt.Color.red.darker());
			}

			return this;
		}

		private boolean isCriteria(Object value) {
			GenericTreeNode node = (GenericTreeNode) value;
			setToolTipText(node.getID());

			return CyCriteria.criteriaNameMap.containsKey(node.getID());
		}
	}
}
