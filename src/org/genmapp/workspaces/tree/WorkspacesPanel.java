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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;
import javax.swing.SwingConstants;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.utils.BackpagePanel;
import org.genmapp.workspaces.utils.NetworkMapping;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.logger.CyLogger;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;

/**
 * GUI component for managing network list in current session.
 */
public class WorkspacesPanel extends JPanel implements PropertyChangeListener {

	private static final long serialVersionUID = 3500704003585438431L;

	private static final int PANEL_PREFFERED_WIDTH = 320;

	private SpeciesPanel speciesPanel;
	private ActionPanel actionPanel;
	private static NetworkPanel networkPanel;
	private static DatasetPanel datasetPanel;
	private static CriteriasetPanel criteriaPanel;
	private static ResultsPanel resultsPanel;
	private static BackpagePanel backpagePanel;

	private JPanel navigatorPanel;
	// BirdsEyeView bev;
	// private BiModalJSplitPane split;
	private final CytoscapeDesktop cytoscapeDesktop;
	private CyLogger logger;
	private static JProgressBar progress;

	/**
	 * Constructor for the Workspaces Panel.
	 * 
	 */
	public WorkspacesPanel(CyLogger cyLogger) {
		super();
		this.cytoscapeDesktop = Cytoscape.getDesktop();
		this.logger = cyLogger;

		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(PANEL_PREFFERED_WIDTH, 700));

		speciesPanel = new SpeciesPanel(logger);
		actionPanel = new ActionPanel(logger);
		networkPanel = new NetworkPanel(logger);
		datasetPanel = new DatasetPanel(logger);
		criteriaPanel = new CriteriasetPanel(logger);
		resultsPanel = new ResultsPanel(logger);
		progress = new JProgressBar(0,100);

		// set default viz
		networkPanel.setVisible(true);
		datasetPanel.setVisible(false);
		criteriaPanel.setVisible(false);
		resultsPanel.setVisible(false);
		progress.setVisible(false);

		navigatorPanel = new JPanel();
		navigatorPanel.setMinimumSize(new Dimension(40, 40));
		navigatorPanel.setMaximumSize(new Dimension(120, 120));
		navigatorPanel.setPreferredSize(new Dimension(120, 120));

		// setNavigator(getBev());

		JPanel main = new JPanel();
		main.setLayout(new GridLayout(4, 1, 0, 0));
		main.add(networkPanel);
		main.add(datasetPanel);
		main.add(criteriaPanel);
		main.add(resultsPanel);

		JPanel wsPanel = new JPanel();
		wsPanel.setLayout(new BoxLayout(wsPanel, BoxLayout.Y_AXIS));
		wsPanel.add(speciesPanel);
		wsPanel.add(actionPanel);
		wsPanel.add(main);
		wsPanel.add(progress);

		// wsPanel.add(analysisTreePanel);
		// wsPanel.add(reportTreePanel);

		// split = new BiModalJSplitPane(cytoscapeDesktop,
		// JSplitPane.VERTICAL_SPLIT, BiModalJSplitPane.MODE_SHOW_SPLIT,
		// wsPanel, navigatorPanel);
		// split.setResizeWeight(1);
		// split.setDividerLocation(DEF_DEVIDER_LOCATION);
		// add(split);

		add(wsPanel);
		logger.debug("workspace panel constructed");

		// Make this a prop change listener for Cytoscape global events.
		Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(this);
		Cytoscape.getSwingPropertyChangeSupport().addPropertyChangeListener(
				this);
	}

	/**
	 * @return the networkTreePanel
	 */
	public static NetworkPanel getNetworkTreePanel() {
		return networkPanel;
	}

	/**
	 * @return the datasetTreePanel
	 */
	public static DatasetPanel getDatasetTreePanel() {
		return datasetPanel;
	}

	/**
	 * @return the criteriaTreePanel
	 */
	public static CriteriasetPanel getCriteriaTreePanel() {
		return criteriaPanel;
	}

	/**
	 * @return the resultsTreePanel
	 */
	public static ResultsPanel getResultsTreePanel() {
		return resultsPanel;
	}

	/**
	 * @return the backpagePanel
	 */
	public static BackpagePanel getBackpagePanel() {
		if (backpagePanel == null) {
			CytoPanel cytoPanel3 = Cytoscape.getDesktop().getCytoPanel(
					SwingConstants.EAST);

			backpagePanel = new BackpagePanel();
			cytoPanel3.add("Backpage", backpagePanel);
			cytoPanel3.setState(CytoPanelState.DOCK);
		}
		return backpagePanel;
	}
	
	public static JProgressBar getProgressBar() {
		return progress;
	}

	protected void clearAllDatasets() {
		logger.debug("clearing all datasets...");
		for (CyDataset dset : CyDataset.datasetNameMap.values()) {
			dset.deleteCyDatasetInfo(false);
		}
		logger.debug("clearing dataset name map");
		CyDataset.datasetNameMap.clear();

	}

	protected void clearAllCriteriasets() {
		logger.debug("clearing all criteriasets...");
		for (String csetname : CyCriteriaset.criteriaNameMap.keySet()) {
			CytoscapeInit.getProperties().remove(
					WorkspacesCommandHandler.PROPERTY_SET_PREFIX + csetname);
			WorkspacesPanel.getCriteriaTreePanel().removeItem(csetname);
		}
		logger.debug("clearing criteriaset prop from cytoscape preferences");
		CytoscapeInit.getProperties().remove(
				WorkspacesCommandHandler.PROPERTY_SETS);

		logger.debug("clearing criteriaset maps");
		CyCriteriaset.criteriaNetworkNodesMap.clear();
		CyCriteriaset.criteriaRowsMap.clear();
		CyCriteriaset.networkCriteriasetMap.clear();
		CyCriteriaset.criteriaNameMap.clear();

	}

	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (prop.equals(Cytoscape.CYTOSCAPE_INITIALIZED)) {
			logger.debug("cytoscape initialized");
		} else if (prop.equals(Cytoscape.NETWORK_LOADED)) {
			logger.debug("network loaded...");
			// handle new sessions separately below
			if (evt.getNewValue() != null) {
				CyNetwork newNetwork = (CyNetwork) ((Object[]) evt
						.getNewValue())[0];
				logger.info("CyNetwork " + newNetwork.getTitle() + " loaded.");

				progress.setVisible(true);
				progress.setValue(0);
				progress.setStringPainted(true);

				NetworkMapping.performNetworkAnnotation(newNetwork, false,
						logger, progress);
				NetworkMapping.performNetworkMapping(newNetwork, false, logger,
						progress);
				progress.setVisible(false);
				
				if (Cytoscape.viewExists(newNetwork.getIdentifier())) {
					for (CyCriteriaset cset : CyCriteriaset.criteriaNameMap
							.values()) {
						logger.debug("applying criteriaset: " + cset.getName());
						WorkspacesCommandHandler.criteriaMapperApplySet(cset,
								newNetwork);
						// track last applied cset per network
						CyCriteriaset.setNetworkCriteriaset(newNetwork, cset);

					}
				}
			}

		} else if (prop.equals(Cytoscape.SESSION_LOADED)) {
			logger.debug("session loaded...");
//			ProgressMonitor progress = new ProgressMonitor(Cytoscape
//					.getDesktop(), "Processing session networks", "", 0, 100);
//			progress.setProgress(1);

			progress.setVisible(true);
			progress.setValue(0);
			progress.setStringPainted(true);
			
			for (CyNetwork network : Cytoscape.getNetworkSet()) {

				/*
				 * for backward compatibility, so folks can start using
				 * GenMAPP-CS on previously generated session files
				 */
				NetworkMapping.performNetworkAnnotation(network, false, logger,
						progress);

				/*
				 * and restore network-criteria map from prop file
				 */
				CyCriteriaset cset = CyCriteriaset
						.getNetworkCriteriaset(network);
				if (null != cset
						&& Cytoscape.viewExists(network.getIdentifier())) {
					logger.debug("applying " + cset.getName() + " to "
							+ network.getTitle());
					WorkspacesCommandHandler.criteriaMapperApplySet(cset,
							network);
				}

				/*
				 * force each network to refresh is visual styles (including
				 * criteriaset mappings) by simulating panel selection
				 */
				logger.debug("refreshing " + network.getTitle());
				networkPanel.setSelectedNetwork(network.getIdentifier());

			}
			progress.setVisible(false);

			// Repaint panel to force display of loaded Network and Datasets
			this.repaint();

		} else if (prop.equals(Cytoscape.NETWORK_DESTROYED)) {
			logger.debug("network destroyed");
			// listen for last network destroyed and check session state in
			// order to determine if new session is being loaded... awkward!
			if ((Cytoscape.getNetworkSet().size() <= 1)
					&& (Cytoscape.getSessionstate() == Cytoscape.SESSION_OPENED)) {
				logger
						.debug("last network destroyed... new session being loaded!");
				clearAllDatasets();
				clearAllCriteriasets();
			}
		}

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