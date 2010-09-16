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
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;
import cytoscape.logger.CyLogger;
import cytoscape.util.swing.JTreeTable;
import cytoscape.view.CytoscapeDesktop;
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

	private final JTreeTable TreeTable;
	private final GenericTreeNode root;

	private JPopupMenu Popup;
	private PopupActionListener CriteriaPopupActionListener;

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

		TreeTable = new JTreeTable(criteriaTreeTableModel);
		TreeTable
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		initialize();

		/*
		 * Remove CTR-A for enabling select all function in the main window.
		 */
		for (KeyStroke listener : TreeTable.getRegisteredKeyStrokes()) {
			if (listener.toString().equals("ctrl pressed A")) {
				final InputMap map = TreeTable.getInputMap();
				map.remove(listener);
				TreeTable.setInputMap(WHEN_FOCUSED, map);
				TreeTable.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map);
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

		this.setLayout(new BoxLayout(this,
				BoxLayout.Y_AXIS));

		TreeTable.getTree().addTreeSelectionListener(this);
		TreeTable.getTree().setRootVisible(false);
		ToolTipManager.sharedInstance().registerComponent(TreeTable);
		TreeTable.getTree().setCellRenderer(new TreeCellRenderer());

		resetTable();

//		navigatorPanel = new JPanel();
//		navigatorPanel.setMinimumSize(new Dimension(180, 180));
//		navigatorPanel.setMaximumSize(new Dimension(180, 180));
//		navigatorPanel.setPreferredSize(new Dimension(180, 180));

		JScrollPane scroll = new JScrollPane(TreeTable);
		this.add(scroll);

//		JPanel wsPanel = new JPanel();
//		wsPanel.setLayout(new GridLayout(2, 1, 10, 10));
//		wsPanel.add(criteriaTreePanel);
//
//		split = new BiModalJSplitPane(cytoscapeDesktop,
//				JSplitPane.VERTICAL_SPLIT, BiModalJSplitPane.MODE_SHOW_SPLIT,
//				wsPanel, navigatorPanel);
//		split.setResizeWeight(1);
//		split.setDividerLocation(DEF_DEVIDER_LOCATION);
//		add(split);

		// this mouse listener listens for the right-click event and will show
		// the pop-up
		// window when that occurrs
		TreeTable.addMouseListener(new PopupListener());

		// create and populate the popup window
		Popup = new JPopupMenu();
		destroyCriteriaItem = new JMenuItem(CriteriaPopupActionListener.DESTROY_DATASET);
		editCriteriaTitle = new JMenuItem(CriteriaPopupActionListener.EDIT_DATASET_TITLE);
		applyCriteria = new JMenuItem(CriteriaPopupActionListener.RELOAD_DATA);
		// action listener which performs the tasks associated with the popup
		CriteriaPopupActionListener = new PopupActionListener();
		destroyCriteriaItem.addActionListener(CriteriaPopupActionListener);
		editCriteriaTitle.addActionListener(CriteriaPopupActionListener);
		applyCriteria.addActionListener(CriteriaPopupActionListener);
		Popup.add(destroyCriteriaItem);
		Popup.add(editCriteriaTitle);
		Popup.add(applyCriteria);
	}

	public void resetTable() {
		TreeTable.getColumn(ColumnTypes.CRITERIA_SET.getDisplayName())
				.setPreferredWidth(180);
		TreeTable.getColumn(ColumnTypes.CRITERIA.getDisplayName())
		.setPreferredWidth(55);
		TreeTable.getColumn(ColumnTypes.CRITERIA_SET_VIEW.getDisplayName())
		.setPreferredWidth(25);
		TreeTable.setRowHeight(DEF_ROW_HEIGHT);
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
		TreeTable.getTree().updateUI();
		TreeTable.doLayout();
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
	 * DOCUMENT ME!
	 * 
	 * @param id
	 *            DOCUMENT ME!
	 * @param parent_id
	 *            DOCUMENT ME!
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
			TreeTable.getTree().collapsePath(
					new TreePath(new TreeNode[]{root}));

			TreeTable.getTree().updateUI();
			TreePath path = new TreePath(dmtn.getPath());
			TreeTable.getTree().expandPath(path);
			TreeTable.getTree().scrollPathToVisible(path);
			TreeTable.doLayout();

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
			TreeTable.getTree().getSelectionModel().setSelectionPath(
					new TreePath(node.getPath()));
			TreeTable.getTree().scrollPathToVisible(
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

		/*
		 * Support concurrent selections across panels
		 */
		JTree mtree = TreeTable.getTree();

		// sets the "current" dataset based on last node in the tree selected
		GenericTreeNode node = (GenericTreeNode) mtree.getLastSelectedPathComponent();
		if (node == null || node.getUserObject() == null)
			return;

		// creates a list of all selected datasets
		final List<String> datasetList = new LinkedList<String>();
		try {
			for (int i = mtree.getMinSelectionRow(); i <= mtree
					.getMaxSelectionRow(); i++) {
				GenericTreeNode n = (GenericTreeNode) mtree.getPathForRow(i)
						.getLastPathComponent();
				if (n != null && n.getUserObject() != null
						&& mtree.isRowSelected(i))
					datasetList.add(n.getID());
			}
		} catch (Exception ex) {
			CyLogger.getLogger().warn(
					"Exception handling dataset panel change: "
							+ ex.getMessage());
			ex.printStackTrace();
		}

		if (datasetList.size() > 0) {
			CyDataset.setSelectedDataset(datasetList);
		}
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param e
	 *            DOCUMENT ME!
	 */
	public void propertyChange(PropertyChangeEvent e) {
		//TODO: add appropriate items here
		if (Cytoscape.CYTOSCAPE_INITIALIZED.equals(e.getPropertyName())) {
			// ?
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
				final int[] selected = TreeTable.getSelectedRows();
				
//				if (e.isShiftDown()){ // TODO:fake for DATASETS
				//if (dselected.length > nselected.length) {
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

						Popup.show(e.getComponent(), e.getX(), e.getY());
//					}
				}
			}
		}
	}

	public void stateChanged(ChangeEvent e) {
		// TODO ?
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
class CriteriaPopupActionListener implements ActionListener {

	public static final String DESTROY_CRITERIA = "Destroy Criteria";
	public static final String EDIT_CRITERIA_TITLE = "Edit Criteria Title";
	public static final String APPLY_CRITERIA = "Apply Criteria";

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

		if (APPLY_CRITERIA.equals(label)) {
			//TODO
			//List<String> selectedCriteria = Criteria.getSelectedCriteria();
			//for (String criteria : selectedCriteria) {
				//TODO
			//}
		} else if  (EDIT_CRITERIA_TITLE.equals(label)) {
			//TODO
		} else if (DESTROY_CRITERIA.equals(label)) {
			//TODO
		}
		else {
			CyLogger.getLogger().warn("Unexpected panel popup option");
		}
	}

}
