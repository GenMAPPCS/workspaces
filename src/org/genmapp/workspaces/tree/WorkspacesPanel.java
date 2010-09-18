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
public class WorkspacesPanel extends JPanel {

	private static final long serialVersionUID = -7102083850894612840L;

	private static final int DEF_DEVIDER_LOCATION = 280;
	private static final int PANEL_PREFFERED_WIDTH = 250;

	private NetworkPanel networkTreePanel;
	private DatasetPanel datasetTreePanel;
	private CriteriaPanel criteriaTreePanel;
	// private AnalysisPanel analysisTreePanel;
	// private ReportPanel reportTreePanel;

	private JPanel navigatorPanel;

	private BiModalJSplitPane split;

	private final CytoscapeDesktop cytoscapeDesktop;

	/**
	 * Constructor for the Network Panel.
	 * 
	 * @param desktop
	 */
	public WorkspacesPanel() {
		super();
		this.cytoscapeDesktop = Cytoscape.getDesktop();

		initialize();

	}

	/**
	 * Initialize GUI components
	 */
	private void initialize() {
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(PANEL_PREFFERED_WIDTH, 700));

		networkTreePanel = new NetworkPanel();
		datasetTreePanel = new DatasetPanel();
		criteriaTreePanel = new CriteriaPanel();
		// analysisTreePanel = new AnalysisPanel();
		// reportTreePanel = new ReportPanel();

		navigatorPanel = new JPanel();
		navigatorPanel.setMinimumSize(new Dimension(180, 180));
		navigatorPanel.setMaximumSize(new Dimension(180, 180));
		navigatorPanel.setPreferredSize(new Dimension(180, 180));

		JPanel wsPanel = new JPanel();
		wsPanel.setLayout(new GridLayout(3, 1, 0, 0));
		wsPanel.add(networkTreePanel);
		wsPanel.add(datasetTreePanel);
		wsPanel.add(criteriaTreePanel);
		// wsPanel.add(analysisTreePanel);
		// wsPanel.add(reportTreePanel);

		split = new BiModalJSplitPane(cytoscapeDesktop,
				JSplitPane.VERTICAL_SPLIT, BiModalJSplitPane.MODE_SHOW_SPLIT,
				wsPanel, navigatorPanel);
		split.setResizeWeight(1);
		split.setDividerLocation(DEF_DEVIDER_LOCATION);
		add(split);
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
	 * @return the networkTreePanel
	 */
	public NetworkPanel getNetworkTreePanel() {
		return networkTreePanel;
	}

	/**
	 * @return the datasetTreePanel
	 */
	public DatasetPanel getDatasetTreePanel() {
		return datasetTreePanel;
	}

	/**
	 * @return the criteriaTreePanel
	 */
	public CriteriaPanel getCriteriaTreePanel() {
		return criteriaTreePanel;
	}

	// /**
	// * @return the analysisTreePanel
	// */
	// public AnalysisPanel getAnalysisTreePanel() {
	// return analysisTreePanel;
	// }
	//
	// /**
	// * @return the reportTreePanel
	// */
	// public ReportPanel getReportTreePanel() {
	// return reportTreePanel;
	// }

}