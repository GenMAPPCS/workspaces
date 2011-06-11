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
package org.genmapp.workspaces.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.tree.SpeciesPanel;

import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.command.CyCommandResult;
import cytoscape.groups.CyGroup;
import cytoscape.groups.CyGroupManager;

public class BackpagePanel extends JPanel implements HyperlinkListener {

	private JEditorPane htmlPane;
	private JToolTip toolTip;
	private Desktop desktop;

	private CyNode node;
	private String nodeid = null;
	private String sourceid = null;
	private String sourcetype = null;
	private Set<String> targettypes = new HashSet<String>();
	private Set<String> nonMappingTypes = new HashSet<String>() {
		{
			add("Description");
			add("Type");
			add("Chromosome");
			add("Symbol");
			add("Synonyms");
		}
	};
	private String html;
	private String divx = "</div>";
	private String div1 = "<div align='center' style='font-family:Arial Black; font-size:14px; color:black'>";
	private String div2 = "<div align='center' style='font-family:Arial; font-size:9px; color:black'>";
	private String table = "<table border='1' cellpadding='5' cellspacing='0' margin-left:auto; margin-right:auto;";
	private String tablex = "</table>";
	private String ul = "<ul style='font-family:Arial; font-size:9px; margin:0 0 0 20; list-style-type:none; list-style-position:outside;'>";
	private String ulx = "</ul>";
	private String li = "<li>";
	private String lix = "</li>";
	private String br = "<br />";

	/**
	 * 
	 */
	public BackpagePanel() {
		super();

		// Take care of panel stuff
		htmlPane = new JEditorPane();
		htmlPane.setContentType("text/html");
		htmlPane.setText(html);
		htmlPane.createToolTip();
		htmlPane.setEditable(false);
		htmlPane.addHyperlinkListener(this);

		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JScrollPane scrollPane = new JScrollPane(htmlPane);
		this.add(scrollPane);

	}

	/**
	 * 
	 */
	public void clearBackpage() {
		htmlPane.setText("");
	}

	/**
	 * Called upon node selection, e.g., from NetworkPanel.onSelectEvent.
	 * 
	 * @param nodeid
	 */
	public void updateBackpage(CyNode cn) {
		htmlPane.setText(html);

		node = cn;
		nodeid = cn.getIdentifier();

		sourcetype = getSecKeyType();
		if (null == sourcetype) {
			// TODO: then work with nodeid and
			// get from net attr or guess id
			sourceid = nodeid;
			// sourcetype = ??
		} else {
			List<String> sklist = (List<String>) Cytoscape.getNodeAttributes()
					.getListAttribute(nodeid, "__" + sourcetype);
			// just take first for now
			// TODO: be smarter
			sourceid = sklist.get(0);
		}

		if (null == sourceid) {
			html = div2 + "No information..." + divx;

		} else {

			html = div1 + nodeid + divx + br;
			html = html.concat(div2 + table);
			html = html.concat("<tr><td>" + sourcetype + "</td><td>" + sourceid
					+ "</td></tr>");

			// add rest of table via swing worker1
			getTable();

			// add list of links via swing worker2
			getLinks();

			// add dataset mapping and criteriaset info via swing worker3
			getData();
		}

		// update pane
		htmlPane.setText(html);
		htmlPane.setCaretPosition(0); // scroll to top

	}

	/**
	 *
	 */
	private void getTable() {
		// TODO: pick specific target types...
		// figure out how to get these fro cythesaurus if not supported by
		// general mapping!?
		html = html.concat(tablex + divx + br);
	}

	/**
	 * 
	 */
	private void getLinks() {
		html = html.concat(ul);

		for (String targettype : targettypes) {
			if (nonMappingTypes.contains(targettype))
				continue;
			CyCommandResult result = WorkspacesCommandHandler
					.performGeneralMapping("true", sourceid, sourcetype,
							targettype);
			if (null != result) {
				Map<String, Set<String>> keyMappings = (Map<String, Set<String>>) result
						.getResult();
				String[] datasourceinfo = SpeciesPanel.supportedDatasources
						.get(targettype);
				for (String key : keyMappings.keySet()) {
					html = html.concat(li + targettype + ": ");
					Set<String> valueSet = keyMappings.get(key);
					Iterator<String> it = valueSet.iterator();
					while (it.hasNext()) {
						String value = it.next();
						String querylink = null;
						if (datasourceinfo != null)
							querylink = datasourceinfo[2];
						if (querylink != null) {
							querylink = querylink.replaceAll("\\$id", value);
							html = html.concat("<a href='" + querylink + "'>"
									+ value + "</a>");
						} else {
							html = html.concat(value);
						}
						if (it.hasNext()) {
							html = html.concat(", ");
						}
					}
					html = html.concat(lix);
				}

			}
		}

		html = html.concat(ulx);
	}
	/**
	 * 
	 */
	private void getData() {
		// TODO: add links/thumbnail images that open structure viz?

		// TODO: table of criteria per node/chidren
		if (CyCriteriaset.criteriaNameMap.size() > 0 || node.isaGroup()) {
			html = html.concat("<br />" + div2 + table);
			html = html.concat("<th>ID</th>");

			for (CyCriteriaset cset : CyCriteriaset.criteriaNameMap.values()) {
				String csetname = cset.getName();
				html = html.concat("<th colspan='1'>" + csetname + "</th>");
			}

			List<String> nodeids = new ArrayList<String>();
			nodeids.add(nodeid);

			if (node.isaGroup()) {
				for (CyNode cn : CyGroupManager.getCyGroup(node).getNodes()) {
					nodeids.add(cn.getIdentifier());
				}
			}

			for (String id : nodeids) {
				if (id.equals(nodeid))
					html = html
							.concat("<tr style='outline:black solid 1px'><td><b>"
									+ id + "</b></td>");
				else
					html = html.concat("<tr><td><i>" + id + "</i></td>");
				for (CyCriteriaset cset : CyCriteriaset.criteriaNameMap
						.values()) {
					String attr = Cytoscape.getNodeAttributes()
							.getStringAttribute(id, cset.getNodeAttribute());
					// defaults for "false" and "null"
					String label = "";
					String criteria = "No data";
					String color = "#FFFFFF";
					if (null == attr) {
						// null defaults
					} else if (attr.equals("")) {
						// null defaults
					} else if (attr.equals("false")) {
						criteria = "No criteria met";
						color = "#C0C0C0";
					} else if (attr.equals("true")) {
						color = cset.getCriteriaParams()[1].split("::")[2];
						label = cset.getCriteriaParams()[1].split("::")[1]
								+ ": ";
						criteria = cset.getCriteriaParams()[1].split("::")[0];
					} else if (attr.startsWith("#")) {
						color = attr;
						for (int i = cset.getCriteriaParams().length - 1; i >= 1; i--) {
							if (color.equals(cset.getCriteriaParams()[i]
									.split("::")[2])) {
								label = cset.getCriteriaParams()[i].split("::")[1]
										+ ": ";
								criteria = cset.getCriteriaParams()[i]
										.split("::")[0];
							}

						}
					}
					html = html
							.concat("<td style='background-color:"
									+ color
									+ ";'><a href='\" + label + criteria + \"'>&nbsp;</a></td>");

				}
			}
			html = html.concat("</tr>" + tablex + divx + br);
		}
	}

	/**
	 * 
	 */
	private String getSecKeyType() {
		String type = null;
		targettypes = WorkspacesCommandHandler.getTargetIdTypes();
		for (String t : targettypes) {
			if (t.contains("Ensembl"))
				type = t;
		}
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.event.HyperlinkListener#hyperlinkUpdate(javax.swing.event
	 * .HyperlinkEvent)
	 */
	public void hyperlinkUpdate(HyperlinkEvent event) {
		if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

			if (Desktop.isDesktopSupported()) {
				desktop = Desktop.getDesktop();
				// Now enable buttons for actions that are supported.
				if (desktop.isSupported(Desktop.Action.BROWSE)
						&& event.getURL().toString().startsWith("http")) {
					try {
						desktop.browse(event.getURL().toURI());
					} catch (IOException ioe) {
						ioe.printStackTrace();
					} catch (URISyntaxException use) {
						use.printStackTrace();
					}
				}
			}
		} else if (event.getEventType() == HyperlinkEvent.EventType.ENTERED) {
			htmlPane.setToolTipText(event.getURL().toString());
		} else if (event.getEventType() == HyperlinkEvent.EventType.EXITED) {
			htmlPane.setToolTipText(null);
		}
	}

}