package org.genmapp.workspaces.command;

import java.awt.Color;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.genmapp.workspaces.GenMAPPWorkspaces;
import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
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
	public static final String ARG_SETNAME = "setname";

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

	// EXTERNAL
	public static final String CRITERIA_MAPPER = "criteriamapper";
	public static final String OPEN_CRITERIA_MAPPER = "open dialog";
	public static final String DELETE_SET = "delete set";
	public static final String APPLY_SET = "apply set";
	public static final String ARG_NETWORK = "network";
	public static final String ARG_MAP_TO = "mapto";
	public static final String ARG_LABEL_LIST = "labellist";
	public static final String ARG_EXP_LIST = "expressionlist";
	public static final String ARG_COLOR_LIST = "colorlist";

	public WorkspacesCommandHandler() {
		super(CyCommandManager.reserveNamespace(NAMESPACE));

		// addDescription(ADD_DATASET, "Add dataset to workspaces panel");
		// addArgument(ADD_DATASET, ARG_DATASET_URL);
		// addArgument(ADD_DATASET, ARG_DATASET_NAME);
		// addArgument(ADD_DATASET, ARG_DATASET_ROWS);

		addDescription(UPDATE_CRITERIASETS,
				"Tell Workspaces to update the criteria set panel");
		addArgument(UPDATE_CRITERIASETS, ARG_SETNAME);

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
	public static void showMessage( String message )
	{
		/* JOptionPane.showMessageDialog(  Cytoscape.getDesktop(), 
				message, 
				"", 
				JOptionPane.ERROR_MESSAGE ); */
	}

	public CyCommandResult execute(String command, Map<String, Object> args)
			throws CyCommandException {
		showMessage( "WorkspacesCommandHandler:execute");
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
			String setName;
			Object s = getArg(command, ARG_SETNAME, args);
			showMessage( "args passed in: " + args );
			showMessage( "args passed in count: " + args.size() );
			
			if (s instanceof String) {
				setName = (String) s;
			} else
			{
				throw new CyCommandException(ARG_SETNAME
						+ ": unknown type (try String!)");
			}
			/*
			 * Three possibilities: (1) saving new set (2) saving or loading
			 * existing set (3) deleting existing set
			 */
			showMessage( "Update CriteriaSets: " + setName );

			String setParameters = CytoscapeInit.getProperties().getProperty(
					NET_ATTR_SET_PREFIX + setName);
			boolean isExistingSet = CyCriteriaset.criteriaNameMap
					.containsKey(setName);
			if (!isExistingSet && setParameters != null) {
				// (1) saving new set
				showMessage( "Update CriteriaSets: save new set " + setName );

				CyCriteriaset cyCriteria = new CyCriteriaset(setName,
						setParameters);
				// System.out.println("SAVE "+setName+":"+setParameters);
				result.addMessage("Criteria " + setName + " added.");

			} else if (isExistingSet && setParameters != null) {
				showMessage( "Update CriteriaSets: save/load existing set " + setName );

				// (2) saving or loading existing set
				CyCriteriaset cyCriteria = CyCriteriaset.criteriaNameMap
						.get(setName);
				cyCriteria.setCriteriaParams(setParameters);
				cyCriteria.collectNetworkCounts();
				// System.out.println("UPDATE "+setName+":"+setParameters);
				result.addMessage("Criteria " + setName + " updated.");

			} else if (isExistingSet && null == setParameters) {
				showMessage( "Update CriteriaSets: delete existing set " + setName );

				// (3) deleting existing set
				CyCriteriaset cyCriteria = CyCriteriaset.criteriaNameMap
						.get(setName);
				cyCriteria.deleteCyCriteriaset();
				result.addMessage("Criteria " + setName + " removed.");
			}

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
				if (((String) dn).startsWith("[")
						&& ((String) dn).endsWith("]"))
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
				case '=':
					// Get the next token
					i = st.nextToken();
					if (i == StreamTokenizer.TT_WORD || i == '"') {
						tokenIndex--;
						String key = tokenList.get(tokenIndex);
						settings.put(key, st.sval);
						tokenList.remove(tokenIndex);
					}
					break;
				case '"':
				case StreamTokenizer.TT_WORD:
					tokenList.add(st.sval);
					tokenIndex++;
					break;
				default:
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

	/**
	 * @param setName
	 */
	public static void openCriteriaMapper(String setName) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_SETNAME, setName);

		try {
			CyCommandManager.execute(CRITERIA_MAPPER, OPEN_CRITERIA_MAPPER,
					args);
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param setName
	 */
	public static void deleteCriteriaset(String setName) {

		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_SETNAME, setName);
		try {
			CyCommandResult re = CyCommandManager.execute(CRITERIA_MAPPER,
					DELETE_SET, args);
			System.out.println(re.getErrors().get(0).toString());
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * @param cset
	 * @param network
	 */
	public static void applyCriteriasetToNetwork(CyCriteriaset cset,
			CyNetwork network) {

		String mapto = new String();
		List<String> explist = new ArrayList<String>();
		List<String> colorlist = new ArrayList<String>();
		List<String> labellist = new ArrayList<String>();

		String[] setParams = cset.getCriteriaParams();
		if (setParams.length > 0) {
			mapto = setParams[0];
		}
		System.out.println(cset.getDisplayName() + ":"
				+ network.getIdentifier() + ":" + mapto);
		for (int i = 1; i < setParams.length; i++) {
			String[] temp = setParams[i].split(":");
			if (temp.length != 3) {
				break;
			}
			System.out.println(temp[0] + ":" + temp[1] + ":" + temp[2]);
			explist.add(temp[0]);
			labellist.add(temp[1]);
			colorlist.add(temp[2]);
		}

		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_SETNAME, cset.getDisplayName());
		args.put(ARG_NETWORK, network);
		args.put(ARG_MAP_TO, mapto);
		args.put(ARG_COLOR_LIST, colorlist);
		args.put(ARG_EXP_LIST, explist);
		args.put(ARG_LABEL_LIST, labellist);

		CyCommandResult result;
		try {
			result = CyCommandManager.execute(CRITERIA_MAPPER, APPLY_SET, args);
			// System.out.println(result.getMessages());
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// then update counts
		// TODO: broken!!
		// cset.collectCounts();

	}
}
