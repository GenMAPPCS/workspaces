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
import javax.swing.SwingConstants;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.tree.WorkspacesPanel;

import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;


public class GenMAPPWorkspaces extends CytoscapePlugin {
	
	public static WorkspacesPanel wsPanel;
	
	public GenMAPPWorkspaces() {

		//create workspaces panel
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(
				SwingConstants.WEST);
		
		wsPanel = new WorkspacesPanel();

		cytoPanel.add("GenMAPP-CS", new ImageIcon(getClass().getResource("images/genmappcs.png")), wsPanel, "Workspaces Panel", 0);	
		cytoPanel.setSelectedIndex(0);	
		//cytoPanel.remove(1);
		
		//cycommands
		new WorkspacesCommandHandler();
	}

}