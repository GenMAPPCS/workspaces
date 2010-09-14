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
package org.genmapp.workspaces;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.genmapp.workspaces.tree.WorkspacesPanel;
import org.genmapp.workspaces.tree.WorkspacesTab;

import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;

public class GenMAPPWorkspaces extends CytoscapePlugin {
	public GenMAPPWorkspaces() {

		//create workspaces panel
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(
				SwingConstants.WEST);
		
		JPanel wsPanel = new WorkspacesPanel();
		
		cytoPanel.remove(0);
		cytoPanel.add("GenMAPP-CS", new ImageIcon(getClass().getResource("images/genmappcs.png")), wsPanel, "Workspaces Panel");	
		int count = cytoPanel.getCytoPanelComponentCount();
		int wspCount = cytoPanel.indexOfComponent(wsPanel);
		System.out.println(count+":"+wspCount);
		//cytoPanel.setSelectedIndex(wspCount);	
	}

}