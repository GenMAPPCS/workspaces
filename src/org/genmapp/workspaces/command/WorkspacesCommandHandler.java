package org.genmapp.workspaces.command;

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

public class WorkspacesCommandHandler extends AbstractCommandHandler {
	private final static String NAMESPACE = "workspaces";

	// public final static String ADD_DATASET = "add dataset";
	// public final static String ARG_DATASET_URL = "url";
	// public final static String ARG_DATASET_NAME = "displayname";
	// public final static String ARG_DATASET_ROWS = "rows";

	private final static String UPDATE_CRITERIASETS = "update criteriasets";
	private final static String ARG_CRITERIASET_NAME = "name";

	private final static String UPDATE_DATASETS = "update datasets";
	private final static String ARG_DATASET_NAME = "name";
	private final static String ARG_DATASET_TYPE = "type";
	private final static String ARG_DATASET_NODES = "nodes";
	private final static String ARG_DATASET_ATTRS = "attrs";

	private final static String ADD_GOELITE_JOB = "add job";
	private final static String ARG_GOELITE_JOB = "jobid";
	private final static String ARG_GOELITE_TABLABEL = "tablabel";

	// VERSIONING
	private final static String NET_ATTR_SETS = "org.genmapp.criteriasets_1.0";
	private final static String NET_ATTR_SET_PREFIX = "org.genmapp.criteriaset.";
	private final static String NET_ATTR_DATASETS = "org.genmapp.datasets_1.0";
	private final static String NET_ATTR_DATASET_PREFIX = "org.genmapp.dataset.";

	public WorkspacesCommandHandler() {
		super(CyCommandManager.reserveNamespace(NAMESPACE));

		// addDescription(ADD_DATASET, "Add dataset to workspaces panel");
		// addArgument(ADD_DATASET, ARG_DATASET_URL);
		// addArgument(ADD_DATASET, ARG_DATASET_NAME);
		// addArgument(ADD_DATASET, ARG_DATASET_ROWS);

		addDescription(UPDATE_CRITERIASETS,
				"Tell Workspaces to update the criteria set panel");
		addArgument(UPDATE_CRITERIASETS, ARG_CRITERIASET_NAME);

		addDescription(UPDATE_DATASETS,
				"Tell Workspaces to update the dataset panel");
		addArgument(UPDATE_DATASETS, ARG_DATASET_NAME);
		addArgument(UPDATE_DATASETS, ARG_DATASET_TYPE);
		addArgument(UPDATE_DATASETS, ARG_DATASET_NODES);
		addArgument(UPDATE_DATASETS, ARG_DATASET_ATTRS);


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
		// if (ADD_DATASET.equals(command)) {
		// URL url = null;
		// String displayName;
		// int rows = 0;
		//
		// Object u = getArg(command, ARG_DATASET_URL, args);
		// if (u instanceof URL) {
		// url = (URL) u;
		// } else if (u instanceof String) {
		// try {
		// url = new URL((String) u);
		// } catch (MalformedURLException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// } else
		// throw new CyCommandException(
		// "url object type is not recognized, try URL or String");
		//
		// Object n = getArg(command, ARG_DATASET_NAME, args);
		// if (n instanceof String) {
		// displayName = (String) n;
		// } else
		// throw new CyCommandException(
		// "name object type is not recognized, try String");
		//
		// Object r = getArg(command, ARG_DATASET_ROWS, args);
		// if (r instanceof Integer) {
		// rows = (Integer) r;
		// } else if (r instanceof String) {
		// if (((String) r).matches("\\d+")) {
		// rows = new Integer((String) r);
		// }
		// } else {
		// rows = 0;
		// }
		//
		// // do it
		// if (CyDataset.datasetUrlMap.containsKey(displayName)) {
		// result.addMessage("Dataset already listed in Workspaces.");
		// } else {
		// CyDataset d = new CyDataset(displayName, url, rows);
		// GenMAPPWorkspaces.wsPanel.getDatasetTreePanel().addItem(
		// displayName, "droot");
		// result.addMessage("Dataset added to Workspaces.");
		// }
		//
		// } else
		if (UPDATE_CRITERIASETS.equals(command)) {
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
							NET_ATTR_SET_PREFIX + setname);
			criteriaCount = clist.size() - 1; // subtract 1 for mapTo entry

			String[] split;
			String nodeAttr = "";
			for (String c : clist) {
				split = c.split(":");
				if (split.length < 3)
					continue; // skip mapTo entry
				nodeAttr = nodeAttr + setname + "_" + split[1] + ":";
			}
			nodeAttr = nodeAttr.substring(0, nodeAttr.length() - 1); // prune
			// final
			// ":"

			// add item and create cycriteria
			CyCriteria cyCriteria;
			if (CyCriteria.criteriaNameMap.containsKey(setname)) {
				cyCriteria = CyCriteria.criteriaNameMap.get(setname);
				result.addMessage("Criteria " + setname
						+ " already listed in Workspaces.");
			} else {
				cyCriteria = new CyCriteria(setname, criteriaCount);
				GenMAPPWorkspaces.wsPanel.getCriteriaTreePanel().addItem(
						setname, "croot");
				result.addMessage("Criteria " + setname
						+ " added to Workspaces.");
			}
			// collect node counts per network for given criteria set
			CyAttributes ca = Cytoscape.getNodeAttributes();
			Map<String, Integer> networkNodes = new HashMap<String, Integer>();
			for (CyNetwork net : Cytoscape.getNetworkSet()) {
				int nodeCount = 0;
				for (CyNode n : (List<CyNode>) net.nodesList()) {
					if (ca.hasAttribute(n.getIdentifier(), nodeAttr)) {
						if (criteriaCount == 1) {

							boolean b = ca.getBooleanAttribute(n
									.getIdentifier(), nodeAttr);
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

		} else if (UPDATE_DATASETS.equals(command)) {
			String name;
			String type;
			List<Integer> nodes;
			List<String> attrs;
			Object n = getArg(command, ARG_DATASET_NAME, args);
			if (n instanceof String) {
				name = (String) n;
			} else
				throw new CyCommandException(ARG_DATASET_NAME
						+ ": unknown type (try String!)");

			Object t = getArg(command, ARG_DATASET_TYPE, args);
			if (t instanceof String) {
				type = (String) t;
			} else
				throw new CyCommandException(ARG_DATASET_TYPE
						+ ": unknown type (try String!)");

			Object dn = getArg(command, ARG_DATASET_NODES, args);
			if (dn instanceof List) {
				if (((List) dn).get(0) instanceof Integer) {
					nodes = (List<Integer>) dn;
				} else
					throw new CyCommandException(ARG_DATASET_NODES
							+ ": unknown type (try List<Integer>!)");
			} else if (dn instanceof String) {
				nodes = new ArrayList<Integer>();
				// escape the escape characters
				dn = ((String) dn).replaceAll("\t", "\\\\t");
				// remove brackets, if they are there
				if (((String) dn).startsWith("[") && ((String) dn).endsWith("]"))
					dn = ((String) dn).substring(1, ((String) dn).length() - 1);
				// parse at comma delimiters
				String[] list = ((String) dn).split(",");
				for (String item : list) {
					// remove all whitespace before trying to generate int
					item = item.replaceAll("\\s+", "");
					nodes.add(new Integer(item));
				}
			} else
				throw new CyCommandException(ARG_DATASET_NODES
						+ ": unknown type (try List<Integer>!)");

			Object a = getArg(command, ARG_DATASET_ATTRS, args);
			if (a instanceof List) {
				if (((List) a).get(0) instanceof String) {
					attrs = (List<String>) a;
				} else
					throw new CyCommandException(ARG_DATASET_ATTRS
							+ ": unknown type (try List<String>!)");
			} else if (a instanceof String) {
				attrs = new ArrayList<String>();
				// escape the escape characters
				a = ((String) a).replaceAll("\t", "\\\\t");
				// remove brackets, if they are there
				if (((String) a).startsWith("[") && ((String) a).endsWith("]"))
					a = ((String) a).substring(1, ((String) a).length() - 1);
				// parse at comma delimiters
				String[] list = ((String) a).split(",");
				for (String item : list) {
					// remove all leading and trailing whitespace 
					item = item.replaceAll("^\\s+|\\s+$", "");
					attrs.add(item);
				}
			} else
				throw new CyCommandException(ARG_DATASET_ATTRS
						+ ": unknown type (try List<String>!)");

			if (CyDataset.datasetNameMap.containsKey(name)) {
				result.addMessage("Dataset " + name
						+ " already listed in Workspaces.");
			} else {
				CyDataset dataset = new CyDataset(name, type, nodes, attrs);
				result.addMessage("Dataset " + name + " added to Workspaces.");
			}

		} else {

			result.addError("Command not supported: " + command);
		}
		return (result);
	}

	/**
	 * From CyCommandTool to handle command strings
	 * 
	 * @param input
	 */
	public static void handleCommand(String input) {
		String ns = null;
		for (String n : CyCommandManager.getNamespaceList()) {
			if (input.toLowerCase().startsWith(n.toLowerCase())
					&& (ns == null || n.length() > ns.length()))
				ns = n;
		}
		if (ns != null) {
			try {
				handleCommand(input, ns);
			} catch (CyCommandException e) {
				e.printStackTrace();
			}
		} else {
			throw new RuntimeException("Unknown command: " + input);
		}

	}

	private static CyCommandResult handleCommand(String inputLine, String ns)
			throws CyCommandException {
		String sub = null;

		// Parse the input, breaking up the tokens into appropriate
		// commands, subcommands, and maps
		Map<String, Object> settings = new HashMap();
		String comm = parseInput(inputLine.substring(ns.length()).trim(),
				settings);

		for (String command : CyCommandManager.getCommandList(ns)) {
			if (command.toLowerCase().equals(comm.toLowerCase())) {
				sub = command;
				break;
			}
		}

		if (sub == null && (comm != null && comm.length() > 0))
			throw new CyCommandException("Unknown argument: " + comm);

		return CyCommandManager.execute(ns, sub, settings);
	}
	private static String parseInput(String input, Map<String, Object> settings) {

		// Tokenize
		StringReader reader = new StringReader(input);
		StreamTokenizer st = new StreamTokenizer(reader);

		// We don't really want to parse numbers as numbers...
		st.ordinaryChar('/');
		st.ordinaryChar('-');
		st.ordinaryChar('.');
		st.ordinaryChars('0', '9');

		st.wordChars('/', '/');
		st.wordChars('-', '-');
		st.wordChars('.', '.');
		st.wordChars('0', '9');

		List<String> tokenList = new ArrayList();
		int tokenIndex = 0;
		int i;
		try {
			while ((i = st.nextToken()) != StreamTokenizer.TT_EOF) {
				switch (i) {
					case '=' :
						// Get the next token
						i = st.nextToken();
						if (i == StreamTokenizer.TT_WORD || i == '"') {
							tokenIndex--;
							String key = tokenList.get(tokenIndex);
							settings.put(key, st.sval);
							tokenList.remove(tokenIndex);
						}
						break;
					case '"' :
					case StreamTokenizer.TT_WORD :
						tokenList.add(st.sval);
						tokenIndex++;
						break;
					default :
						break;
				}
			}
		} catch (Exception e) {
			return "";
		}

		// Concatenate the commands together
		String command = "";
		for (String word : tokenList)
			command += word + " ";

		// Now, the last token of the args goes with the first setting
		return command.trim();
	}
}
