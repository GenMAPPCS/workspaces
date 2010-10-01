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

public abstract class DatasetMapping {

	private static Map<String, Object> invalid = new HashMap<String, Object>();
	private static CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();

	public static final String NET_ATTR_DATASETS = "org.genmapp.datasets_1.0";
	private static final String NET_ATTR_DATASET_PREFIX = "org.genmapp.dataset.";
	public static final String ID = "GeneID";
	public static final String CODE = "SystemCode";

	/**
	 * Annotate every datanode with it's secondary key mappings. Then match key
	 * and secondary keys to existing network node ids, secondary keys and
	 * special id/code attributes.
	 * 
	 * @param d
	 */
	public static void performDatasetMapping(CyDataset d) {
		String dnKeyType = d.getKeyType();
		String secKeyType = getSecKeyType();

		if (dnKeyType.equals(secKeyType))
			return;

		List<Integer> datanodeIndices = d.getNodes();
		List<String> nodeIds = new ArrayList<String>();
		for (Integer dni : datanodeIndices) {
			nodeIds.add(Cytoscape.getRootGraph().getNode(dni).getIdentifier());
		}
		Map<String, Set<String>> secondaryKeyMap = collectTableMappings(
				nodeIds, dnKeyType, secKeyType);

		String datasetName = d.getDisplayName();
		List<CyNetwork> networkList = collectVirginNetworks(datasetName);

		List<String> attrs = d.getAttrs();
		List<CyNetwork> mappedToNetworks = new ArrayList<CyNetwork>();

		/*
		 * For every node in dataset...
		 */
		for (Integer dni : datanodeIndices) {
			CyNode dn = (CyNode) Cytoscape.getRootGraph().getNode(dni);
			String dnKey = dn.getIdentifier();
			Set<String> secKeys = secondaryKeyMap.get(dnKey);
			if (secKeys != null) {
				List<String> secKeyList = new ArrayList<String>();
				for (String sk : secKeys) {
					secKeyList.add(sk);
				}

				/*
				 * First, annotation every datanode with it's secondary key
				 * mappings and dataset source
				 */
				nodeAttrs
						.setListAttribute(dnKey, "__" + secKeyType, secKeyList);
				List<String> datasetlist;
				datasetlist = nodeAttrs.getListAttribute(dnKey,
						NET_ATTR_DATASETS);
				if (null == datasetlist)
					datasetlist = new ArrayList<String>();
				datasetlist.add(datasetName);
				nodeAttrs.setListAttribute(dnKey, NET_ATTR_DATASETS,
						datasetlist);
			}
			/*
			 * Perform mapping on a per network basis to track associations with
			 * datasets
			 */
			for (CyNetwork network : networkList) {
				for (CyNode cn : (List<CyNode>) network.nodesList()) {
					String nodeKey = cn.getIdentifier();
					boolean didMap = false;
					/*
					 * First, check if datanode == existing network node
					 */
					if (dn == cn) {
						// mapping has already been performed, naturally,
						// so just tag network
						mappedToNetworks.add(network);
					}

					/*
					 * Next, check if datanode matches ID/CODE directly
					 */
					String pk = Cytoscape.getNodeAttributes()
							.getStringAttribute(nodeKey, ID);
					String pkt = Cytoscape.getNodeAttributes()
							.getStringAttribute(nodeKey, CODE);
					if (pk != null && pkt != null) {
						if (pkt.equals(dnKeyType) && pk.equals(dnKey)) {
							mapAttributes(dn, dnKeyType, attrs, cn);
							mappedToNetworks.add(network);

						}
					}

					/*
					 * Next, check matches with datanode secondary keys
					 */
					if (secKeys != null) {
						for (String secondaryKey : secKeys) {
							/*
							 * Check network node ids
							 */
							if (nodeKey.equals(secondaryKey)) {
								mapAttributes(dn, dnKeyType, attrs, cn);
								mappedToNetworks.add(network);
								break; // skip remaining secondary keys
							}

							/*
							 * And check network node secondary keys
							 */
							List<String> sk = (List<String>) Cytoscape
									.getNodeAttributes().getListAttribute(
											nodeKey, "__" + secKeyType);
							if (sk != null) {
								if (sk.size() > 0) {
									if (sk.contains(secondaryKey)) {
										mapAttributes(dn, dnKeyType, attrs, cn);
										mappedToNetworks.add(network);
										break; // skip remaining secondary keys
									}
								}
							}
						}
					}
				}
			} // end for each network

		} // end for each node in dataset

		// tag networks
		for (CyNetwork network : mappedToNetworks) {
			String netid = network.getIdentifier();
			List<String> sourcelist = new ArrayList<String>();
			if (Cytoscape.getNetworkAttributes().hasAttribute(netid,
					NET_ATTR_DATASETS)) {
				sourcelist = (List<String>) Cytoscape.getNetworkAttributes()
						.getListAttribute(netid, NET_ATTR_DATASETS);
				if (!sourcelist.contains(datasetName)) {
					sourcelist.add(datasetName);
					Cytoscape.getNetworkAttributes().setListAttribute(netid,
							NET_ATTR_DATASETS, sourcelist);
				}
			} else {
				sourcelist.add(datasetName);
				Cytoscape.getNetworkAttributes().setListAttribute(netid,
						NET_ATTR_DATASETS, sourcelist);
			}
			Cytoscape.getNetworkAttributes().setListAttribute(netid,
					NET_ATTR_DATASET_PREFIX + datasetName, attrs);
		}

	}

	/**
	 * Copy attributes from datanode to existing node. And add datanode key to
	 * list attribute of mapped-to node.
	 * 
	 * @param skipIndex
	 * @param nkey
	 * @param parts
	 * @return
	 */
	private static boolean mapAttributes(CyNode dn, String dnType,
			List<String> attrs, CyNode cn) {
		String dnKey = dn.getIdentifier();
		String nKey = cn.getIdentifier();
		Object listsample = null;

		// gather attribute info for copy action
		for (String attr : attrs) {
			Object entry = nodeAttrs.getAttribute(dnKey, attr);
			Byte type = nodeAttrs.getType(attr);
			if (type == CyAttributes.TYPE_SIMPLE_LIST) {
				List l = nodeAttrs.getListAttribute(dnKey, attr);
				listsample = l.get(0);
			}
			mapAttribute(nKey, attr, entry, type, listsample);
		}

		// add datanode key to list attribute
		List<String> plist = (List<String>) Cytoscape.getNodeAttributes()
				.getListAttribute(nKey, "__" + dnType);
		if (null == plist) {
			plist = new ArrayList<String>();
		}
		if (!plist.contains(dnKey)) {
			plist.add(dnKey);
		}
		try {
			nodeAttrs.setListAttribute(nKey, "__" + dnType, plist);
		} catch (Exception e) {
			invalid.put(nKey, plist);
		}

		return true;
	}
	/**
	 * Based on the attribute types, map the entry to CyAttributes.
	 * 
	 * @param key
	 * @param name
	 * @param entry
	 * @param type
	 * @param listsample
	 */
	private static void mapAttribute(final String key, final String name,
			final Object entry, final Byte type, final Object listsample) {

		switch (type) {
			case CyAttributes.TYPE_BOOLEAN :

				Boolean newBool;

				try {
					newBool = (Boolean) entry;
					nodeAttrs.setAttribute(key, name, newBool);
				} catch (Exception e) {
					invalid.put(key, entry);
				}

				break;

			case CyAttributes.TYPE_INTEGER :

				Integer newInt;

				try {
					newInt = (Integer) entry;
					nodeAttrs.setAttribute(key, name, newInt);
				} catch (Exception e) {
					invalid.put(key, entry);
				}

				break;

			case CyAttributes.TYPE_FLOATING :

				Double newDouble;

				try {
					newDouble = (Double) entry;
					nodeAttrs.setAttribute(key, name, newDouble);
				} catch (Exception e) {
					invalid.put(key, entry);
				}

				break;

			case CyAttributes.TYPE_STRING :
				String newString;
				try {
					newString = (String) entry;
					nodeAttrs.setAttribute(key, name, newString);
				} catch (Exception e) {
					invalid.put(key, entry);
				}

				break;

			case CyAttributes.TYPE_SIMPLE_LIST :
				List newList;
				if (listsample instanceof Boolean)
					newList = (List<Boolean>) entry;
				else if (listsample instanceof Integer)
					newList = (List<Integer>) entry;
				else if (listsample instanceof Double)
					newList = (List<Double>) entry;
				else
					newList = (List<String>) entry;

				try {
					nodeAttrs.setListAttribute(key, name, newList);
				} catch (Exception e) {
					invalid.put(key, entry);
				}

				break;

			default :
				invalid.put(key, entry);

		}
	}

	/**
	 * If an entry is a list, split the string and create new List Attribute.
	 * 
	 * @return listAttr new list attribute
	 */
	private List buildList(final String entry, final Byte dataType,
			final String listDel) {
		if (entry == null) {
			return null;
		}

		final String[] parts = (entry.replace("\"", "")).split(listDel);

		final List listAttr = new ArrayList();

		for (String listItem : parts) {
			switch (dataType) {
				case CyAttributes.TYPE_BOOLEAN :
					listAttr.add(Boolean.parseBoolean(listItem.trim()));

					break;

				case CyAttributes.TYPE_INTEGER :
					listAttr.add(Integer.parseInt(listItem.trim()));

					break;

				case CyAttributes.TYPE_FLOATING :
					listAttr.add(Double.parseDouble(listItem.trim()));

					break;

				case CyAttributes.TYPE_STRING :
					listAttr.add(listItem.trim());

					break;

				default :
					break;
			}
		}

		return listAttr;
	}

	/**
	 * @param title
	 * @return
	 */
	private static List<CyNetwork> collectVirginNetworks(String title) {
		// collect list of virgin networks
		List<CyNetwork> netList = new ArrayList<CyNetwork>();
		for (CyNetwork network : Cytoscape.getNetworkSet()) {
			String netid = network.getIdentifier();
			System.out.println(netid);
			if (Cytoscape.viewExists(netid)) {
				// check network attributes for dataset tag
				if (Cytoscape.getNetworkAttributes().hasAttribute(netid,
						NET_ATTR_DATASETS)) {
					List<String> sourcelist = (List<String>) Cytoscape
							.getNetworkAttributes().getListAttribute(netid,
									NET_ATTR_DATASETS);
					if (!sourcelist.contains(title)) {
						System.out.println("2 " + netid);
						netList.add(network);
					}
				} else {
					netList.add(network);
				}
			}
		}
		return netList;
	}

	/**
	 * 
	 */
	public static String getSecKeyType() {
		String type = null;
		Map<String, Object> noargs = new HashMap<String, Object>();
		CyCommandResult result = null;
		try {
			result = CyCommandManager.execute("idmapping",
					"get target id types", noargs);
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (null != result) {
			Set<String> idTypes = (Set<String>) result.getResult();
			for (String t : idTypes) {
				if (t.contains("Ensembl"))
					type = t;
			}
		}
		return type;
	}
	/**
	 * 
	 */
	private static Map<String, Set<String>> collectTableMappings(
			List<String> nodeIds, String pkt, String skt) {
		Map<String, Set<String>> secondaryKeyMap = new HashMap<String, Set<String>>();

		CyCommandResult result = mapIdentifiers(nodeIds, pkt, skt);

		if (null != result) {
			Map<String, Set<String>> keyMappings = (Map<String, Set<String>>) result
					.getResult();
			for (String primaryKey : keyMappings.keySet()) {
				secondaryKeyMap.put(primaryKey, keyMappings.get(primaryKey));

			}

		}
		return secondaryKeyMap;
	}

	/**
	 * @param st
	 * @param tt
	 * @return
	 */
	private Boolean checkMappingSupported(String st, String tt) {
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

	/**
	 * @param l
	 * @param pkt
	 * @param skt
	 * @return
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

	/**
	 * @param net
	 * @param pkt
	 * @param skt
	 * @return
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
			// System.out.println(msg);
		}
		return result;
	}

}
