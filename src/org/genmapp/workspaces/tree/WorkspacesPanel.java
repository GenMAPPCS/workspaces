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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.utils.NetworkMapping;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.BiModalJSplitPane;
import ding.view.BirdsEyeView;
import ding.view.DGraphView;

/**
 * GUI component for managing network list in current session.
 */
public class WorkspacesPanel extends JPanel implements PropertyChangeListener {

	private static final long serialVersionUID = 3500704003585438431L;
	
	private static final int DEF_DEVIDER_LOCATION = 280;
	private static final int PANEL_PREFFERED_WIDTH = 250;

	private SpeciesPanel speciesPanel;
	private ActionPanel actionPanel;
	private static NetworkPanel networkPanel;
	private static DatasetPanel datasetPanel;
	private static CriteriasetPanel criteriaPanel;
	// private AnalysisPanel analysisTreePanel;
	// private ReportPanel reportTreePanel;

	private JPanel navigatorPanel;
//	BirdsEyeView bev;
//	private BiModalJSplitPane split;
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
		actionPanel = new ActionPanel();
		networkPanel = new NetworkPanel();
		datasetPanel = new DatasetPanel();
		criteriaPanel = new CriteriasetPanel();
		// analysisTreePanel = new AnalysisPanel();
		// reportTreePanel = new ReportPanel();

		// set default viz
		networkPanel.setVisible(false);
		datasetPanel.setVisible(false);
		criteriaPanel.setVisible(false);

		navigatorPanel = new JPanel();
		navigatorPanel.setMinimumSize(new Dimension(40, 40));
		navigatorPanel.setMaximumSize(new Dimension(120, 120));
		navigatorPanel.setPreferredSize(new Dimension(120, 120));
		
//		setNavigator(getBev());

		JPanel main = new JPanel();
		main.setLayout(new GridLayout(3, 1, 0, 0));
		main.add(networkPanel);
		main.add(datasetPanel);
		main.add(criteriaPanel);

		JPanel wsPanel = new JPanel();
		wsPanel.setLayout(new BoxLayout(wsPanel, BoxLayout.Y_AXIS));
		wsPanel.add(speciesPanel);
		wsPanel.add(actionPanel);
		wsPanel.add(main);
		// wsPanel.add(analysisTreePanel);
		// wsPanel.add(reportTreePanel);

//		split = new BiModalJSplitPane(cytoscapeDesktop,
//				JSplitPane.VERTICAL_SPLIT, BiModalJSplitPane.MODE_SHOW_SPLIT,
//				wsPanel, navigatorPanel);
//		split.setResizeWeight(1);
//		split.setDividerLocation(DEF_DEVIDER_LOCATION);
//		add(split);
		
		add(wsPanel);

		// Make this a prop change listener for Cytoscape global events.
		Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(this);
	}

//	/**
//	 * DOCUMENT ME!
//	 * 
//	 * @param comp
//	 *            DOCUMENT ME!
//	 */
//	public void setNavigator(final Component comp) {
//		split.setRightComponent(comp);
//		split.validate();
//	}
//	
//	/**
//	 * Creates a new BirdsEyeViewHandler object.
//	 * @param desktopPane The JDesktopPane of the NetworkViewManager. Can be null.
//	 */
//	private BirdsEyeView getBev() {
//		return bev = new BirdsEyeView((DGraphView) Cytoscape.getCurrentNetworkView()) {
//				public Dimension getMinimumSize() {
//					return new Dimension(180, 180);
//				}
//
//				public Dimension getMaximumSize() {
//					return new Dimension(180, 180);
//				}
//
//				public Dimension getPreferredSize() {
//					return new Dimension(180, 180);
//				}
//			};
//	}

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

	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (prop.equals(Cytoscape.CYTOSCAPE_INITIALIZED)) {
			// nothing

		} else if (prop.equals(Cytoscape.NETWORK_LOADED)) {
			 // handle new sessions separately below
			if (evt.getNewValue() != null) {
				CyNetwork newNetwork = (CyNetwork) ((Object[]) evt
						.getNewValue())[0];
				NetworkMapping.performNetworkMappings(newNetwork);
				for (CyCriteriaset cset : CyCriteriaset.criteriaNameMap
						.values()) {
					WorkspacesCommandHandler.applyCriteriasetToNetwork(cset,
							newNetwork);
				}
			}

		} else if (prop.equals(Cytoscape.SESSION_LOADED)){
			for (CyNetwork newNetwork : Cytoscape.getNetworkSet()){
				NetworkMapping.performNetworkMappings(newNetwork);
				for (CyCriteriaset cset : CyCriteriaset.criteriaNameMap
						.values()) {
					WorkspacesCommandHandler.applyCriteriasetToNetwork(cset,
							newNetwork);
				}

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