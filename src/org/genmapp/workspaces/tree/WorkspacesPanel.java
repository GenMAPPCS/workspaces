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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.BiModalJSplitPane;

/**
 * GUI component for managing network list in current session.
 */
public class WorkspacesPanel extends JPanel  {

	private static final long serialVersionUID = -7102083850894612840L;

	private static final int DEF_DEVIDER_LOCATION = 280;
	private static final int PANEL_PREFFERED_WIDTH = 250;

	private SpeciesPanel speciesPanel;
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

		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(PANEL_PREFFERED_WIDTH, 700));

		speciesPanel = new SpeciesPanel();
		networkTreePanel = new NetworkPanel();
		datasetTreePanel = new DatasetPanel();
		criteriaTreePanel = new CriteriaPanel();
		// analysisTreePanel = new AnalysisPanel();
		// reportTreePanel = new ReportPanel();

		navigatorPanel = new JPanel();
		navigatorPanel.setMinimumSize(new Dimension(120, 120));
		navigatorPanel.setMaximumSize(new Dimension(180, 180));
		navigatorPanel.setPreferredSize(new Dimension(180, 180));

		JPanel main = new JPanel();
		main.setLayout(new GridLayout(3, 1, 0, 0));
		main.add(networkTreePanel);
		main.add(datasetTreePanel);
		main.add(criteriaTreePanel);
		main.setAlignmentX(CENTER_ALIGNMENT);
		
		speciesPanel.setAlignmentX(CENTER_ALIGNMENT);
		speciesPanel.setAlignmentY(TOP_ALIGNMENT);
		JPanel spacer = new JPanel();
		spacer.setMinimumSize(new Dimension(180,7));
		spacer.setBackground(new Color(230, 230, 230));
		
		JPanel wsPanel = new JPanel();
		wsPanel.setLayout(new BoxLayout(wsPanel, BoxLayout.Y_AXIS));
		wsPanel.add(speciesPanel);
		wsPanel.add(spacer);
		wsPanel.add(main);

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