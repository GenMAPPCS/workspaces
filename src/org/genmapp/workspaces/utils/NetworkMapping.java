package org.genmapp.workspaces.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;

import org.genmapp.workspaces.objects.CyDataset;
import org.genmapp.workspaces.tree.DatasetPanel;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.data.CyAttributes;
import cytoscape.logger.CyLogger;

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
	public static void performNetworkAnnotation(CyNetwork network, Boolean force, CyLogger logger, JProgressBar progress) {

		String netid = network.getIdentifier();

		int tail = netid.length() > 20 ? 20 : netid.length();
		progress.setString("Annotating: " + netid.substring(0, tail) + "...");
		progress.setValue(10);

		String secKeyType = DatasetMapping.getSecKeyType();
		logger.debug("mapping to " + secKeyType);
		// check network-level system code
		String networkCode = Cytoscape.getNetworkAttributes().getStringAttribute(netid, CODE);
		if (networkCode != null && !force) {
			// skip this network; it's already been id mapped
			logger.debug("system code known: " + networkCode);
			progress.setValue(100);
			return;
		}
		// determine if there *is* a network-level system code
		// check first 10 nodes
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
			CyCommandResult result = CyCommandManager.execute("idmapping", "guess id type", args);
			if (null != result) {
				// we only trust unique hits
				if (((Set<String>) result.getResult()).size() < 1) {
					logger.debug("no system code found");
					// TODO: pop up user selector
				} else if (((Set<String>) result.getResult()).size() == 1) {
					String type = null;
					for (String t : (Set<String>) result.getResult()) {
						type = t;
					}
					logger.debug("system code found: " + type);
					progress.setString("using \"" + type + "\"");
					Cytoscape.getNetworkAttributes().setAttribute(netid, CODE, type);

					CyCommandResult result2 = mapIdentifiersByAttr(network, type, secKeyType);
					logger.info(result2.getMessages().toString());
					progress.setValue(20);
				} else {
					logger.debug("found more than one system code:");
					// TODO: pop up user selector
					if (logger.isDebugging()) {
						for (String t : (Set<String>) result.getResult()) {
							logger.debug("    ==>" + t);
						}
					}
				}
			} else {
				logger.debug("null result from guessing system code");
			}
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			logger.error("Trying to guess system code: CyCommandException", e);
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			logger.error("Trying to guess system code: RuntimeException", e);
			e.printStackTrace();
		}

		// check node-level system codes
		List<CyNode> cnList = (List<CyNode>) network.nodesList();
		
		String idsThatDidNotMap = null;
		int numIdsThatDidNotMap = 0;
		
		for (CyNode cn : cnList) {
			int i = 0;
			i++;
			if (i % 100 == 0) { // every 100 nodes
				int prog = 20 + 70 * i / cnList.size(); // 20->90
				progress.setValue(prog);
				progress.setString("using \"" + secKeyType + "\"");
			}
			
			List<String> sk = (List<String>) Cytoscape.getNodeAttributes().getListAttribute(cn.getIdentifier(), "__" + secKeyType);
			if (sk != null)
				if (sk.size() > 0) {
					logger.debug("has secondary key");
					continue; // next node
				}
			String pk = Cytoscape.getNodeAttributes().getStringAttribute(cn.getIdentifier(), ID);
			String pkt = Cytoscape.getNodeAttributes().getStringAttribute(cn.getIdentifier(), CODE);
			if (pk != null && pkt != null) {
				logger.debug("has explicit ID and SYSTEM CODE");
				progress.setString("using \"" + pkt + "\"");

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

				Cytoscape.getNetworkAttributes().setAttribute(netid, CODE, MIXED);

			} else {
				idsThatDidNotMap = ((idsThatDidNotMap == null) ? "" : (idsThatDidNotMap + " , ")) + cn.toString();
				numIdsThatDidNotMap++;
			}
		}
		if (idsThatDidNotMap != null) {
			logger.warn("Warning: we failed to identify the following " + numIdsThatDidNotMap + " nodes that did not map. Here are all their names in one long string, delimited by ' , ': \"" + idsThatDidNotMap + "\". No ID mapping was performed on these IDs!");
		}
		
		progress.setValue(90);
		progress.setString("using \"" + CODE + "\"");

		// Finally, take collection of nodes to be mapped and map them
		for (final String type : typeKeysMap.keySet()) {
			if (checkMappingSupported(type, secKeyType)) {
				logger.debug("mapping from " + type + " to " + secKeyType + " is supported...");
				final CyCommandResult result = mapIdentifiers(typeKeysMap.get(type), type, secKeyType);
				if (null != result) {
					logger.info(result.getMessages().toString());
					Map<String, Set<String>> keyMappings = (Map<String, Set<String>>) result.getResult();
					for (String pkey : keyMappings.keySet()) {
						List<String> slist = new ArrayList<String>(keyMappings.get(pkey));
						for (String node : keyNodesMap.get(pkey)) {
							try {
								nodeAttrs.setListAttribute(node, "__" + secKeyType, slist);
								logger.debug("annotated " + node + " with ID mapping result");
							} catch (Exception e) {
								logger.warn("failed annotate " + node + " with ID mapping result");
								invalid.put(pkey, slist);
							}
						}

					}
				} else {
					logger.warn("No result for ID mapping from " + type + " to " + secKeyType);
				}
			}
		}
		progress.setValue(100);
	}

	/**
	 * @param network
	 * @param force
	 *            force re-annotation of dataset nodes
	 */
	public static void performNetworkMapping(CyNetwork network, Boolean force, CyLogger logger, JProgressBar progress) {
		List<CyNetwork> networklist = new ArrayList<CyNetwork>();
		networklist.add(network);
		for (CyDataset d : CyDataset.datasetNameMap.values()) {
			progress.setValue(0);
			DatasetMapping.performDatasetMapping(d, networklist, force, logger, progress);
		}
		DatasetPanel.getTreeTable().getTree().updateUI();
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
			CyCommandResult result = CyCommandManager.execute("idmapping", "check mapping supported", args);
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

	private static CyCommandResult mapIdentifiers(List<String> l, String pkt, String skt) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("sourceid", l);
		args.put("sourcetype", pkt);
		args.put("targettype", skt);
		CyCommandResult result = null;
		try {
			result = CyCommandManager.execute("idmapping", "general mapping", args);
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	private static CyCommandResult mapIdentifiersByAttr(CyNetwork net, String pkt, String skt) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("networklist", net);
		args.put("sourceattr", "ID");
		args.put("sourcetype", pkt);
		args.put("targetattr", "__" + skt);
		args.put("targettype", skt);
		CyCommandResult result = null;
		try {
			result = CyCommandManager.execute("idmapping", "attribute based mapping", args);
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

}
