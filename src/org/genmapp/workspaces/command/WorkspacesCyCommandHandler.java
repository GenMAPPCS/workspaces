package org.genmapp.workspaces.command;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.objects.CyCriteria;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.tree.CriteriaTreeTableModel;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.command.AbstractCommandHandler;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.data.CyAttributes;
import cytoscape.layout.Tunable;

public class WorkspacesCyCommandHandler extends AbstractCommandHandler {
	public final static String NAMESPACE = "workspaces";

	public final static String ADD_DATASET = "add dataset";
	public final static String ARG_DATASET_URL = "url";
	public final static String ARG_DATASET_NAME = "displayname";
	public final static String ARG_DATASET_ROWS = "rows";

	public final static String UPDATE_CRITERIA = "update criteria";
	public final static String ARG_CRITERIASET_NAME = "setname";

	public final static String ADD_GOELITE_JOB = "add job";
	public final static String ARG_GOELITE_JOB = "jobid";
	public final static String ARG_GOELITE_TABLABEL = "tablabel";

	public WorkspacesCyCommandHandler() {
		super(CyCommandManager.reserveNamespace(NAMESPACE));

		addDescription(ADD_DATASET, "Add dataset to workspaces panel");
		addArgument(ADD_DATASET, ARG_DATASET_URL);
		addArgument(ADD_DATASET, ARG_DATASET_NAME);
		addArgument(ADD_DATASET, ARG_DATASET_ROWS);

		addDescription(UPDATE_CRITERIA,
				"Tell Workspaces to update the criteria set panel");
		addArgument(UPDATE_CRITERIA);

	}

	public CyCommandResult execute(String command, Collection<Tunable> args)
			throws CyCommandException {
		return execute(command, createKVMap(args));
	}

	public CyCommandResult execute(String command, Map<String, Object> args)
			throws CyCommandException {
		CyCommandResult result = new CyCommandResult();

		for (String t : args.keySet()) {
			result.addMessage("Arg: " + t + " = " + args.get(t));
		}
		if (ADD_DATASET.equals(command)) {
			URL url = null;
			String displayName;
			int rows = 0;

			Object u = getArg(command, ARG_DATASET_URL, args);
			if (u instanceof URL) {
				url = (URL) u;
			} else if (u instanceof String) {
				try {
					url = new URL((String) u);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				throw new CyCommandException(
						"url object type is not recognized, try URL or String");

			Object n = getArg(command, ARG_DATASET_NAME, args);
			if (n instanceof String) {
				displayName = (String) n;
			} else
				throw new CyCommandException(
						"name object type is not recognized, try String");

			Object r = getArg(command, ARG_DATASET_ROWS, args);
			if (r instanceof Integer) {
				rows = (Integer) r;
			} else if (r instanceof String) {
				if (((String) r).matches("\\d+")) {
					rows = new Integer((String) r);
				}
			} else {
				rows = 0;
			}

			// do it
			if (CyDataset.datasetUrlMap.containsKey(displayName)) {
				result.addMessage("Dataset already listed in Workspaces.");
			} else {
				CyDataset d = new CyDataset(displayName, url, rows);
				GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().addItem(
						displayName, "droot");
				result.addMessage("Dataset added to Workspaces.");
			}

		} else if (UPDATE_CRITERIA.equals(command)) {
			String setname;
			Object s = getArg(command, ARG_CRITERIASET_NAME, args);
			if (s instanceof String) {
				setname = (String) s;
			} else
				throw new CyCommandException(ARG_CRITERIASET_NAME
						+ ": unknown type (try String!)");

			// gather info on criteria from network attributes
			int criteriaCount = 0;
			List<String> clist = Cytoscape.getNetworkAttributes()
					.getListAttribute(
							Cytoscape.getCurrentNetwork().getIdentifier(),
							setname);
			criteriaCount = clist.size() - 1; // subtract 1 for mapTo entry

			String[] split;
			String nodeAttr = "";
			for (String c : clist) {
				split = c.split(":");
				if (split.length < 3)
					continue; // skip mapTo entry
				nodeAttr = nodeAttr+ setname+"_"+ split[1] + ":";
			}
			nodeAttr = nodeAttr.substring(0, nodeAttr.length() - 1); // prune final ":"

			// add item and create cycriteria
			CyCriteria cyCriteria;
			if (CyCriteria.criteriaNameMap.containsKey(setname)) {
				cyCriteria = CyCriteria.criteriaNameMap.get(setname);
				result.addMessage("Criteria already listed in Workspaces.");
			} else {
				cyCriteria = new CyCriteria(setname, criteriaCount);
				GenMAPPWorkspaces.wsPanel.getCriteriaTreePanel().addItem(
						setname, "croot");
				result.addMessage("Dataset added to Workspaces.");
			}
			// collect node counts per network for given criteria set
			CyAttributes ca = Cytoscape.getNodeAttributes();
			Map<String, Integer> networkNodes = new HashMap<String, Integer>();
			for (CyNetwork net : Cytoscape.getNetworkSet()) {
				int nodeCount = 0;
				for (CyNode n : (List<CyNode>) net.nodesList()) {
					if (ca.hasAttribute(n.getIdentifier(), nodeAttr)) {
						if (criteriaCount == 1) {

							boolean b = ca.getBooleanAttribute(n.getIdentifier(),
									nodeAttr);
							if (b) {
								nodeCount++;
							}

						} else if (criteriaCount > 1) {
							Integer i = ca.getIntegerAttribute(n
									.getIdentifier(), nodeAttr);
							if (i >= 0) {
								nodeCount++;
							}
						}
					} 
				}
				networkNodes.put(net.getIdentifier(), nodeCount);
			}
			// set criteria map'o'map
			cyCriteria.criteriaNetworkNodesMap.put(setname, networkNodes);

		} else {

			result.addError("Command not supported: " + command);
		}
		return (result);
	}

}
