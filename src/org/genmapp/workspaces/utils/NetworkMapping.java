package org.genmapp.workspaces.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.data.CyAttributes;

public abstract class NetworkMapping {

	private static Map<String, List<String>> typeKeysMap = new HashMap<String, List<String>>();
	private static Map<String, List<String>> keyNodesMap = new HashMap<String, List<String>>();

	private static CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();
	private static Map<String, Object> invalid = new HashMap<String, Object>();

	public static final String ID = "GeneID";
	public static final String CODE = "SystemCode";
	public static final String MIXED = "MIXED";

	/**
	 * @param parts
	 * 
	 * The purpose of this function is to
	 * - determine the SystemCode for the primary ids of this network 
	 * - map these ids to a secondary key
	 * 
	 * In the process, one or more additional node attributes ( "__" + secondaryKeyType ) are created
	 * to hold the secondaryKeys.
	 * 
	 * The idea is to first try and find a secondary key type that works for most of the nodes,
	 * and then, if need be, map individual nodes on a special case-by-case basis; some of the
	 * networks could have multiple key types for their primary id.   Such networks will have
	 * their SystemCode set to "MIXED".
	 */
	public static void performNetworkMappings(CyNetwork network) {
		String netid = network.getIdentifier();
		String secKeyType = DatasetMapping.getSecKeyType();

		// check network-level system code
		String networkCode = Cytoscape.getNetworkAttributes()
				.getStringAttribute(netid, CODE);
		if (networkCode != null) {
			// skip this network; it's already been id mapped
			return;
		}
		// determine if there *is* a network-level system code
		// collect first 10 nodes
		List<String> keyList = new ArrayList<String>();
		int j = 10;
		if (network.nodesList().size() < 10)
			j = network.nodesList().size();
		for (int i = 0; i < j; i++) {
			keyList.add(((CyNode) network.nodesList().get(i)).getIdentifier());
		}
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("sourceid", keyList);
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping",
					"guess id type", args);
			if (null != result) {
				// we only trust unique hits
				if (((Set<String>) result.getResult()).size() == 1) {
					String type = null;
					for (String t : (Set<String>) result.getResult()) {
						type = t;
					}
					Cytoscape.getNetworkAttributes().setAttribute(netid, CODE,
							type);
					// skip special cases, which will be mapped
					// naturally
					CyCommandResult result2 = mapIdentifiersByAttr(network,
							type, secKeyType);
				}
			}
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// check node-level system codes
		for (CyNode cn : (List<CyNode>) network.nodesList()) {
			List<String> sk = (List<String>) Cytoscape.getNodeAttributes()
					.getListAttribute(cn.getIdentifier(), "__" + secKeyType);
			if (sk != null)
				if (sk.size() > 0) {
					continue; // next node
				}
			String pk = Cytoscape.getNodeAttributes().getStringAttribute(
					cn.getIdentifier(), ID);
			String pkt = Cytoscape.getNodeAttributes().getStringAttribute(
					cn.getIdentifier(), CODE);
			if (pk != null && pkt != null) {
				List<String> keys = typeKeysMap.get(pkt);
				if (null == keys) {
					keys = new ArrayList<String>();
				}
				keys.add(pk);
				typeKeysMap.put(pkt, keys);
				List<String> nodes = keyNodesMap.get(pk);
				if (null == nodes) {
					nodes = new ArrayList<String>();
				}
				nodes.add(cn.getIdentifier());
				keyNodesMap.put(pk, nodes);

				Cytoscape.getNetworkAttributes().setAttribute(netid, CODE,
						MIXED);

			} else {
				// screw it! They need to try harder!
			}
		}

		// Finally, take collection of nodes to be mapped and map them
		for (String type : typeKeysMap.keySet()) {

			// first check if mapping is supported
			boolean greenLight = checkMappingSupported(type, secKeyType);

			if (greenLight) {
				CyCommandResult result = mapIdentifiers(typeKeysMap.get(type),
						type, secKeyType);

				if (null != result) {
					Map<String, Set<String>> keyMappings = (Map<String, Set<String>>) result
							.getResult();
					for (String pkey : keyMappings.keySet()) {
						List<String> slist = new ArrayList<String>();
						for (String skey : keyMappings.get(pkey)) {
							slist.add(skey);
						}

						for (String node : keyNodesMap.get(pkey)) {
							try {
								nodeAttrs.setListAttribute(node, "__"
										+ secKeyType, slist);
							} catch (Exception e) {
								invalid.put(pkey, slist);
							}
						}

					}
				}
			}
		}
	}

	/**
	 * Returns true iff mapping between the source and target key types is supported
	 *   by the idmapping infrastructure.
	 *   
	 * @param st
	 * @param tt
	 * @return
	 */
	private static Boolean checkMappingSupported(String st, String tt) {

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("sourcetype", st);
		args.put("targettype", tt);
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping",
					"check mapping supported", args);
			if (null != result) {
				Boolean b = (Boolean) result.getResult();
				return b;
			} else {
				return false;
			}
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// if all else fails
		return false;
	}

	/*
	 *  Performs a mapping of primary keys to their secondary key type and stores
	 *  them in a result array.  No side effects.
	 */

	private static CyCommandResult mapIdentifiers(List<String> l, String pkt,
			String skt) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("sourceid", l);
		args.put("sourcetype", pkt);
		args.put("targettype", skt);
		CyCommandResult result = null;
		try {
			result = CyCommandManager.execute("idmapping", "general mapping",
					args);
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String msg : result.getMessages()) {
			// System.out.println(msg);
		}
		return result;
	}

	/*
	 * Creates a new node attribute column called ( "__" + skt ) and fills it with the
	 *   secondary key that has been mapped from the primary key / primary key type.
	 */
	private static CyCommandResult mapIdentifiersByAttr(CyNetwork net,
			String pkt, String skt) {

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("networklist", net);
		args.put("sourceattr", "ID");
		args.put("sourcetype", pkt);
		args.put("targetattr", "__" + skt);
		args.put("targettype", skt);
		CyCommandResult result = null;
		try {
			result = CyCommandManager.execute("idmapping",
					"attribute based mapping", args);
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String msg : result.getMessages()) {
			//System.out.println(msg);
		}
		return result;
	}

}
