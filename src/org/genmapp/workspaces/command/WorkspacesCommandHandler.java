package org.genmapp.workspaces.command;

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.genmapp.workspaces.objects.CyCriteriaset;
import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.command.AbstractCommandHandler;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandHandler;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
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

	private final static String GET_ALL_DATASET_NODES = "get all dataset nodes";

	// VERSIONING
	public static final String PROPERTY_SETS = "org.genmapp.criteriasets_1.0";
	public static final String PROPERTY_SET_PREFIX = "org.genmapp.criteriaset.";
	// public static final String NET_ATTR_APPLIED_SET =
	// "org.genmapp.criteriaset";
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

	public static final String METANODE_PLUGIN = "metanode";
	public static final String CREATE_METANODE = "create";
	public static final String ADD_NODE_TO_METANODE = "add node";
	public static final String SETDEFAULTAGG = "set default aggregation";
	public static final String SETAGGOVERRIDE = "set default overrides";
	public static final String SETDEFAULTAPP = "set default appearance";
	public static final String MODIFYAPP = "modify appearance";
	public static final String EXPAND_ALL = "expand all";
	public static final String COLLAPSE_ALL = "collapse all";
	public static final String ALL_METANODES = "apply to all";
	public static final String SELECTED_METANODES = "apply to selected";
	public static final String ARG_METANODE_NAME = "metanode";
	public static final String ARG_NODE = "node";
	public static final String ARG_NODE_LIST = "nodelist";
	public static final String ARG_ENABLED = "enabled";
	public static final String ARG_ATTR_BOOLEAN = "boolean";
	public static final String ARG_ATTR_DOUBLE = "double";
	public static final String ARG_ATTR_INTEGER = "integer";
	public static final String ARG_ATTR_LIST = "list";
	public static final String ARG_ATTR_STRING = "string";
	public static final String ARG_AGGREGATION = "aggregation";
	public static final String ARG_ATTRIBUTE = "attribute";
	public static final String ARG_OPACITY = "opacity";
	public static final String ARG_USENESTEDNETWORKS = "usenestednetworks";
	public static final String ARG_CHARTATTR = "chartattribute";
	public static final String ARG_NODECHART = "nodechart";
	public static final String ARG_NETWORKID = "networkview";

	private final static String ADD_GOELITE_JOB = "add job";
	private final static String ARG_GOELITE_JOB = "jobid";
	private final static String ARG_GOELITE_TABLABEL = "tablabel";

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

		addDescription(GET_ALL_DATASET_NODES,
				"Ask Workspaces for array of dataset node indicies");
		addArgument(GET_ALL_DATASET_NODES);

	}

	public CyCommandResult execute(String command, Collection<Tunable> args)
			throws CyCommandException {
		return execute(command, createKVMap(args));
	}
	public static void showMessage(String message) {
		
//		 JOptionPane.showMessageDialog( Cytoscape.getDesktop(), message, "",
//		 JOptionPane.ERROR_MESSAGE );
		 
	}

	public CyCommandResult execute(String command, Map<String, Object> args)
			throws CyCommandException {
		showMessage("WorkspacesCommandHandler:execute");
		CyCommandResult result = new CyCommandResult();

		for (String t : args.keySet()) {
			result.addMessage("Arg: " + t + " = " + args.get(t));
		}

		if (UPDATE_CRITERIASETS.equals(command)) {
			String setName;
			Object s = getArg(command, ARG_SETNAME, args);
			showMessage("args passed in: " + args);
			showMessage("args passed in count: " + args.size());

			if (s instanceof String) {
				setName = (String) s;
			} else {
				throw new CyCommandException(ARG_SETNAME
						+ ": unknown type (try String!)");
			}
			String msg = updateCriteriaset(setName);
			result.addMessage(msg);

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

		} else if (GET_ALL_DATASET_NODES.equals(command)) {
			result.addResult(CyDataset.getAllDatasetNodes());

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

	public static String updateCriteriaset(String setName) {

		/*
		 * Three possibilities: (1) saving new set (2) saving or loading
		 * existing set (3) deleting existing set
		 */
		showMessage("Update CriteriaSets: " + setName);

		String setParameters = CytoscapeInit.getProperties().getProperty(
				PROPERTY_SET_PREFIX + setName);
		boolean isExistingSet = CyCriteriaset.criteriaNameMap
				.containsKey(setName);
		if (!isExistingSet && setParameters != null) {
			// (1) saving new set or restoring session with saved sets
			showMessage("Update CriteriaSets: save new set " + setName);

			CyCriteriaset cset = new CyCriteriaset(setName, setParameters);
			CyCriteriaset.setNetworkCriteriaset(Cytoscape.getCurrentNetwork(),
					cset);
			return "Criteria " + setName + " added.";

		} else if (isExistingSet && setParameters != null) {
			showMessage("Update CriteriaSets: save/load existing set "
					+ setName);

			// (2) saving or loading an existing set
			CyCriteriaset cset = CyCriteriaset.criteriaNameMap.get(setName);
			cset.setCriteriaParams(setParameters);
			cset.collectNetworkCounts();
			CyCriteriaset.setNetworkCriteriaset(Cytoscape.getCurrentNetwork(),
					cset);
			return "Criteria " + setName + " updated.";

		} else if (isExistingSet && null == setParameters) {
			showMessage("Update CriteriaSets: delete existing set " + setName);

			// (3) deleting an existing set
			CyCriteriaset cset = CyCriteriaset.criteriaNameMap.get(setName);
			cset.deleteCriteriaset();
			return "Criteria " + setName + " removed.";
		} else {
			return null;
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
	public static void criteriaMapperApplySet(CyCriteriaset cset,
			CyNetwork network) {

		String mapto = new String();
		List<String> explist = new ArrayList<String>();
		List<String> colorlist = new ArrayList<String>();
		List<String> labellist = new ArrayList<String>();

		String[] setParams = cset.getCriteriaParams();
		if (setParams.length > 0) {
			mapto = setParams[0];
		}
		System.out.println(cset.getName() + ":" + network.getIdentifier() + ":"
				+ mapto);
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
		args.put(ARG_SETNAME, cset.getName());
		args.put(ARG_NETWORK, network.getIdentifier());
		args.put(ARG_MAP_TO, mapto);
		args.put(ARG_COLOR_LIST, colorlist);
		args.put(ARG_EXP_LIST, explist);
		args.put(ARG_LABEL_LIST, labellist);

		CyCommandResult result;
		try {
			result = CyCommandManager.execute(CRITERIA_MAPPER, APPLY_SET, args);
			System.out.println(result.getMessages());
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// update counts and color highlight
		cset.collectNetworkCounts();
		// track last applied cset per network
		CyCriteriaset.setNetworkCriteriaset(network, cset);
	}

	public static void createMetanode(String mnodeName, CyNetwork network,
			List<CyNode> nodelist) {

		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_METANODE_NAME, mnodeName);
		args.put(ARG_NETWORK, network);
		args.put(ARG_NODE_LIST, nodelist);
		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					CREATE_METANODE, args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * Use this constructor to enable or disable attribute aggregation for
	 * metanodes.
	 * 
	 * @param b
	 *            boolean control of 'enabled' setting
	 */
	public static void setMetanodeAggregation(String b) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_ENABLED, b);
		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					SETDEFAULTAGG, args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	/**
	 * Use this constructor to pass individual default settings.
	 * 
	 * @param b
	 *            boolean control of 'enabled' setting
	 * @param type
	 *            one of the ARG_ATTR_x types available to be set
	 * @param value
	 *            one of the available aggregation operations per type (see
	 *            MetaNodeCommandHandler argument strings)
	 */
	public static void setMetanodeAggregation(String b, String type,
			String value) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_ENABLED, b);
		args.put(type, value);
		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					SETDEFAULTAGG, args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * Use this constructor to set attribute-specific overrides.
	 * 
	 * @param attr
	 *            attribute name
	 * @param value
	 *            one of the available aggregation operations (see
	 *            MetaNodeCommandHandler argument strings)
	 */
	public static void setMetanodeAggregation(String attr, String value) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_ATTRIBUTE, attr);
		args.put(ARG_AGGREGATION, value);
		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					SETAGGOVERRIDE, args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * @param usenestednetworks
	 * @param opacity
	 * @param nodechart
	 * @param chartattr
	 */
	public static void setDefaultMetanodeAppearance(Boolean usenestednetworks,
			Double opacity, String nodechart, String chartattr) {
		Map<String, Object> args = new HashMap<String, Object>();
		if (null != usenestednetworks)
			args.put(ARG_USENESTEDNETWORKS, usenestednetworks.toString());
		if (null != opacity)
			args.put(ARG_OPACITY, opacity);
		if (null != nodechart)
			args.put(ARG_NODECHART, nodechart);
		if (null != chartattr)
			args.put(ARG_CHARTATTR, chartattr);
		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					SETDEFAULTAPP, args);
			// System.out.println("RE: " + re.getErrors().toString()
			// + re.getMessages().toString());
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * @param metanode
	 * @param usenestednetworks
	 * @param opacity
	 * @param nodechart
	 * @param chartattr
	 */
	public static void setMetanodeAppearance(String metanode,
			Boolean usenestednetworks, Double opacity, String nodechart,
			String chartattr) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_METANODE_NAME, metanode);
		args.put(ARG_USENESTEDNETWORKS, usenestednetworks.toString());
		args.put(ARG_OPACITY, opacity);
		args.put(ARG_NODECHART, nodechart);
		args.put(ARG_CHARTATTR, chartattr);
		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					MODIFYAPP, args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void setAggregationOverride(String attr, String aggtype) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_ATTRIBUTE, attr);
		args.put(ARG_AGGREGATION, aggtype);
		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					SETAGGOVERRIDE, args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * @param id
	 *            network id
	 * @param function
	 *            "expand all" or "collapse all"
	 */
	public static void allMetanodes(String id, String function) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put(ARG_NETWORKID, id);
		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					function, args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * @param scope
	 *            "apply to all" or "apply to selected"
	 */
	public static void applyMetanodeSettings(String scope) {
		Map<String, Object> args = new HashMap<String, Object>();
		if (scope.equals(ALL_METANODES) || scope.equals(SELECTED_METANODES)) {
			try {
				CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
						scope, args);
			} catch (CyCommandException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (RuntimeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public static List<String> listMetanodes(CyNetwork net) {
		Map<String, Object> args = new HashMap<String, Object>();
		if (null != net)
			args.put(ARG_NETWORK, net.getIdentifier());

		try {
			CyCommandResult re = CyCommandManager.execute(METANODE_PLUGIN,
					"list metanodes", args);
			
			return (List<String>) re.getResult();
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return null;
	}

	public static void clearCombinedCriteria() {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("file",
				"/Applications/Cytoscape_v2.8.1/nodecharts-demo-CLEAR-run.txt");
		try {
			CyCommandResult re = CyCommandManager.execute("commandtool", "run",
					args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void pieCriteria(List<String> nodelist, List<String> colorlist) {
		Map<String, Object> args = new HashMap<String, Object>();
		
		//construct even-distribution valuelist to match colorlist
		List<String> valuelist = new ArrayList<String>();
		for (int i = 0; i < colorlist.size(); i++){
			valuelist.add("1");
		}
		args.put("nodelist", nodelist);
		args.put("colorlist", colorlist);
		args.put("valuelist", valuelist);
		args.put("scale", "1.0");
		args.put("showlabels", "false");
		args.put("arcstart", "90");
		try {
			CyCommandResult re = CyCommandManager.execute("nodecharts", "pie",
					args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void stripeCriteria(List<String> nodelist, List<String> colorlist) {
		Map<String, Object> args = new HashMap<String, Object>();
		
		//construct even-distribution valuelist to match colorlist
		List<String> valuelist = new ArrayList<String>();
		for (int i = 0; i < colorlist.size(); i++){
			valuelist.add("1");
		}
		args.put("nodelist", nodelist);
		args.put("colorlist", colorlist);
		args.put("valuelist", valuelist);
		args.put("scale", "1.0");
		args.put("showlabels", "false");
		try {
			CyCommandResult re = CyCommandManager.execute("nodecharts", "stripe",
					args);
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void selectedCircleLayout() {
		CyCommandHandler handler = CyCommandManager.getCommand("layout",
				"circular");
		Map<String, Tunable> layoutTunables = handler.getTunables("circular");
		// Tunable t = layoutTunables.get("selected_only");
		// t.setValue(false);
		Collection<Tunable> ts = new ArrayList<Tunable>();
		// ts.add(t);
		try {
			CyCommandResult layoutResult = handler.execute("circular", ts);
		} catch (CyCommandException e) {
		}
	}
}
