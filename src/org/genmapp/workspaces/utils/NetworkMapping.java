package org.genmapp.workspaces.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.genmapp.workspaces.objects.CyDataset;

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
	 * @param network
	 * @param force
	 *            force re-annotation of network
	 */
	public static void performNetworkAnnotation(CyNetwork network, Boolean force) {
		String netid = network.getIdentifier();
		String secKeyType = DatasetMapping.getSecKeyType();
		// check network-level system code
		String networkCode = Cytoscape.getNetworkAttributes()
				.getStringAttribute(netid, CODE);
		if (networkCode != null && !force) {
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
	 * @param network
	 * @param force
	 *            force re-annotation of dataset nodes
	 */
	public static void performNetworkMapping(CyNetwork network, Boolean force) {
		List<CyNetwork> networklist = new ArrayList<CyNetwork>();
		networklist.add(network);
		for (CyDataset d : CyDataset.datasetNameMap.values()) {
			DatasetMapping.performDatasetMapping(d, networklist, force);
		}
		CyDataset.setCurrentHighlight();
	}

	/**
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
			// System.out.println(msg);
		}
		return result;
	}

}
