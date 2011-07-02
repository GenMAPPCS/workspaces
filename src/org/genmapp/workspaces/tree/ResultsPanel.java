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
import javax.swing.SwingConstants;
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
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.objects.CyResult;
import org.genmapp.workspaces.tree.CriteriasetPanel.PopupActionListener;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.SelectEvent;
import cytoscape.logger.CyLogger;
import cytoscape.util.swing.JTreeTable;
import cytoscape.view.cytopanels.BiModalJSplitPane;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;

/**
 * GUI component for managing network list in current session.
 */
public class ResultsPanel extends JPanel
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

	private JMenuItem destroyResultItem;

	private BiModalJSplitPane split;

	private final ResultsTreeTableModel resultsTreeTableModel;

	private CyLogger logger;

	/**
	 * Constructor for the Results Panel.
	 * 
	 * @param desktop
	 */
	public ResultsPanel(CyLogger cyLogger) {
		super();
		this.logger = cyLogger;

		root = new GenericTreeNode("Results Root", "rroot");
		resultsTreeTableModel = new ResultsTreeTableModel(root);

		treeTable = new JTreeTable(resultsTreeTableModel);
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
		TreeCellRenderer treeCellRenderer = new TreeCellRenderer();
		// ImageIcon leafIcon = new
		// ImageIcon(GenMAPPWorkspaces.class.getResource(
		// "images/criteriaset.png"));
		// if (leafIcon != null) {
		// treeCellRenderer.setLeafIcon(leafIcon);
		// }
		treeTable.getTree().setCellRenderer(treeCellRenderer);

		resetTable();

		JScrollPane scroll = new JScrollPane(treeTable);
		this.add(scroll);

		// create and populate the popup window
		popup = new JPopupMenu();
		destroyResultItem = new JMenuItem(PopupActionListener.DESTROY_RESULT);
		destroyResultItem.addActionListener(popupActionListener);
		popup.add(destroyResultItem);

	}

	public void resetTable() {
		treeTable.getColumn(GenericColumnTypes.RESULTS.getDisplayName())
				.setPreferredWidth(280);
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
		final GenericTreeNode node = getResultsTreeNode(id);
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
		if (resultsTreeTableModel.getChildCount(root) < 1) {
			this.setVisible(false);
		}

	}

	/**
	 * 
	 * 
	 * @param rname
	 *            results name
	 * @param parent_id
	 *            parent node
	 * 
	 */
	public void addItem(String rname, String parent_id) {
		// activate panel
		this.setVisible(true);

		// first see if it exists
		if (getResultsTreeNode(rname) == null) {
			GenericTreeNode dmtn = new GenericTreeNode(rname, rname);

			if (parent_id != null && getResultsTreeNode(parent_id) != null) {
				getResultsTreeNode(parent_id).add(dmtn);
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
	 * 
	 * @param rname
	 *            results name
	 */
	public void setSelectedResults(String rname) {
		// logger.info("CriteriasetPanel: focus criteriaset node");
		DefaultMutableTreeNode node = getResultsTreeNode(rname);

		if (node != null) {
			// fires valueChanged if the criteriaset isn't already selected
			treeTable.getTree().getSelectionModel().setSelectionPath(
					new TreePath(node.getPath()));
			treeTable.getTree().scrollPathToVisible(
					new TreePath(node.getPath()));
		}
	}

	/**
	 * @return results name
	 */
	public static String getSelectedResults() {
		GenericTreeNode node = (GenericTreeNode) treeTable.getTree()
				.getLastSelectedPathComponent();
		if (null == node)
			return null;
		else
			return node.getID();
	}

	/**
	 * 
	 * @param rname
	 *            results name
	 * 
	 * @return tree node
	 */
	public GenericTreeNode getResultsTreeNode(String rname) {
		Enumeration tree_node_enum = root.breadthFirstEnumeration();

		while (tree_node_enum.hasMoreElements()) {
			GenericTreeNode node = (GenericTreeNode) tree_node_enum
					.nextElement();

			if (((String) node.getID()).equals(rname)) {
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
		GenericTreeNode node = (GenericTreeNode) treeTable.getTree()
				.getLastSelectedPathComponent();
		if (node == null || node.getUserObject() == null)
			return;

		CyResult cr = CyResult.resultNameMap.get(node.getID());

		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(
				SwingConstants.EAST);
		if (cytoPanel.getState().equals(CytoPanelState.HIDE))
			cytoPanel.setState(CytoPanelState.DOCK);
		int index = cytoPanel.indexOfComponent(cr.getComponentLabel());
		cytoPanel.setSelectedIndex(index);

		// TODO: if sub, then go deeper

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

				if (nselected != null)
					if (nselected.length == 1) {
						destroyResultItem.setEnabled(true);

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

		public static final String DESTROY_RESULT = "Destroy Result";

		public void actionPerformed(ActionEvent ae) {
			final String label = ((JMenuItem) ae.getSource()).getText();

			GenericTreeNode node = (GenericTreeNode) treeTable.getTree()
					.getLastSelectedPathComponent();

			if (node == null || node.getUserObject() == null)
				return;

			if (DESTROY_RESULT.equals(label)) {

				CyResult.resultNameMap.get(node.getID()).deleteResult();
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

			String nodeid = ((GenericTreeNode) value).getID();

			if (!nodeid.equals("rroot")) {
				CyResult cr = CyResult.resultNameMap.get(nodeid);
				setToolTipText(cr.getComponentLabel());

				boolean green = true;
				green = cr.isGreen();

				if (green) {
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
