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
import javax.swing.SwingWorker;

import cytoscape.Cytoscape;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.BiModalJSplitPane;

/**
 * GUI component for managing network list in current session.
 */
public class WorkspacesPanel extends JPanel  implements PropertyChangeListener {

	private static final long serialVersionUID = -7102083850894612840L;

	private static final int DEF_DEVIDER_LOCATION = 280;
	private static final int PANEL_PREFFERED_WIDTH = 250;

	private SpeciesPanel speciesPanel;
	private NetworkPanel networkPanel;
	private DatasetPanel datasetPanel;
	private CriteriaPanel criteriaPanel;
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
		networkPanel = new NetworkPanel();
		datasetPanel = new DatasetPanel();
		criteriaPanel = new CriteriaPanel();
		// analysisTreePanel = new AnalysisPanel();
		// reportTreePanel = new ReportPanel();

		navigatorPanel = new JPanel();
		navigatorPanel.setMinimumSize(new Dimension(180, 180));
		navigatorPanel.setMaximumSize(new Dimension(180, 180));
		navigatorPanel.setPreferredSize(new Dimension(180, 180));

		JPanel main = new JPanel();
		main.setLayout(new GridLayout(3, 1, 0, 0));
		main.add(networkPanel);
		main.add(datasetPanel);
		main.add(criteriaPanel);
		
		JPanel wsPanel = new JPanel();
		wsPanel.setLayout(new BoxLayout(wsPanel, BoxLayout.Y_AXIS));
		wsPanel.add(speciesPanel);
		wsPanel.add(main);
		// wsPanel.add(analysisTreePanel);
		// wsPanel.add(reportTreePanel);

		split = new BiModalJSplitPane(cytoscapeDesktop,
				JSplitPane.VERTICAL_SPLIT, BiModalJSplitPane.MODE_SHOW_SPLIT,
				wsPanel, navigatorPanel);
		split.setResizeWeight(1);
		split.setDividerLocation(DEF_DEVIDER_LOCATION);
		add(split);
		
		// Make this a prop change listener for Cytoscape global events.
		Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(this);
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
		return networkPanel;
	}

	/**
	 * @return the datasetTreePanel
	 */
	public DatasetPanel getDatasetTreePanel() {
		return datasetPanel;
	}

	/**
	 * @return the criteriaTreePanel
	 */
	public CriteriaPanel getCriteriaTreePanel() {
		return criteriaPanel;
	}
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (prop.equals(Cytoscape.CYTOSCAPE_INITIALIZED)) {
			// nothing

		} else if (prop.equals(Cytoscape.NETWORK_LOADED)) {
			// reload all attached datasets
//			try {
//				Thread.sleep(10000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

				public String doInBackground() {
					String msg = "done!";
//					System.out.println("NEW :"+evt.getNewValue());
					datasetPanel.reloadDataset();
					
					return msg;
				}
			};
			worker.execute();

			
			
			// then apply select criteria set(s)
			
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