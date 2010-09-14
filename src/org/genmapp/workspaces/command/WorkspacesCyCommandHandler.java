package org.genmapp.workspaces.command;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.tree.WorkspacesPanel;

import cytoscape.command.AbstractCommandHandler;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.layout.Tunable;

public class WorkspacesCyCommandHandler extends AbstractCommandHandler {
	public final static String NAMESPACE = "workspaces";

	public final static String ADD_DATASET = "add dataset";
	public final static String ARG_DATASET_URL = "url";
	public final static String ARG_DATASET_NAME = "displayname";
	public final static String ARG_DATASET_ROWS = "rows";

	public WorkspacesCyCommandHandler() {
		super(CyCommandManager.reserveNamespace(NAMESPACE));

		addDescription(ADD_DATASET, "Add dataset to workspaces panel");
		addArgument(ADD_DATASET, ARG_DATASET_URL);
		addArgument(ADD_DATASET, ARG_DATASET_NAME);
		addArgument(ADD_DATASET, ARG_DATASET_ROWS);

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
				GenMAPPWorkspaces.wsPanel.addDataset(displayName, "droot");
				result.addMessage("Dataset added to Workspaces.");
			}
			

		} else {

			result.addError("Command not supported: " + command);
		}
		return (result);
	}

}
