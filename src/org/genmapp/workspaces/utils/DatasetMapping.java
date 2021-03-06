package org.genmapp.workspaces.utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;

import org.genmapp.workspaces.command.WorkspacesCommandHandler;
import org.genmapp.workspaces.objects.CyDataset;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.data.CyAttributes;
import cytoscape.groups.CyGroup;
import cytoscape.groups.CyGroupManager;
import cytoscape.logger.CyLogger;
import cytoscape.view.CyNetworkView;

public abstract class DatasetMapping {

	private static Map<String, Object> invalid = new HashMap<String, Object>();
	private static CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();

	public static final String NET_ATTR_DATASETS = "org.genmapp.datasets_1.0";
	public static final String NET_ATTR_DATASET_PREFIX = "org.genmapp.dataset.";
	public static final String ID = "GeneID";
	public static final String CODE = "SystemCode";

	private static CyLogger logger;

	private static JProgressBar progress;

	/**
	 * Gather secondary key mappings for every datanode. Then match key and
	 * secondary keys to existing network node ids, secondary keys and special
	 * id/code attributes.
	 * 
	 * @param d
	 *            CyDataset
	 * @param nl
	 *            list of CyNetworks to map to. Use null to map to all unmapped
	 *            networks.
	 * @param force
	 *            force re-annotation of dataset nodes?
	 */
	public static void performDatasetMapping(CyDataset d, List<CyNetwork> nl,
			boolean force, CyLogger cyLogger, JProgressBar progressBar) {
		progress = progressBar;
		logger = cyLogger;
		String dnKeyType = d.getKeyType();
		String secKeyType = getSecKeyType();
		logger.info("Dataset primary key (" + dnKeyType
				+ ") and secondary key (" + secKeyType + ")");

		/*
		 * Don't do the following, because it will skip the manual annotation of
		 * Ensembl ID columns, e.g., in the case of Yeast ORFs
		 */
		// if (dnKeyType.equals(secKeyType))
		// return;
		List<Integer> datanodeIndices = d.getNodes();
		List<String> nodeIds = new ArrayList<String>();
		for (Integer dni : datanodeIndices) {
			nodeIds.add(Cytoscape.getRootGraph().getNode(dni).getIdentifier());
		}
		int tail = d.getName().length() > 20 ? 20 : d.getName().length();
		progress
				.setString("Mapping: " + d.getName().substring(0, tail) + "...");
		Map<String, Set<String>> secondaryKeyMap = collectTableMappings(
				nodeIds, dnKeyType, secKeyType, force);
		progress.setValue(10);

		String datasetName = d.getName();
		List<CyNetwork> networkList = nl;
		if (null == networkList) {
			networkList = new ArrayList<CyNetwork>(Cytoscape.getNetworkSet());
		}

		// Now, screen out previously mapped networks, unless forced
		networkList = screenNetworkList(networkList, datasetName, force);

		logger.debug("mapping to " + networkList.size() + " networks");

		List<String> attrs = d.getAttrs();
		List<CyNetwork> mappedToNetworks = new ArrayList<CyNetwork>();

		/*
		 * Set metanode attr overrides and appearance settings per dataset
		 * before creating any groups
		 */
		if (!networkList.isEmpty())
			metanodeSettings(d);

		/*
		 * For every node in dataset...
		 */
		for (Integer dni : datanodeIndices) {
			int i = 0;
			i++;
			if (i % 100 == 0) { // every 100 ds nodes
				int prog = 10 + 80 * i / datanodeIndices.size(); // 10->90
				progress.setValue(prog);
				progress.setString("using \"" + secKeyType + "\"");
			}

			CyNode dn = (CyNode) Cytoscape.getRootGraph().getNode(dni);
			String dnKey = dn.getIdentifier();
			Set<String> secKeys = secondaryKeyMap.get(dnKey);

			if (force && secKeys != null) {
				List<String> secKeyList = new ArrayList<String>(secKeys);

				/*
				 * First, annotate every datanode with it's secondary key
				 * mappings and dataset source
				 */
				nodeAttrs
						.setListAttribute(dnKey, "__" + secKeyType, secKeyList);
				List<String> datasetlist;
				datasetlist = nodeAttrs.getListAttribute(dnKey,
						NET_ATTR_DATASETS);
				if (null == datasetlist)
					datasetlist = new ArrayList<String>();
				if (!datasetlist.contains(datasetName)) {
					datasetlist.add(datasetName);
					nodeAttrs.setListAttribute(dnKey, NET_ATTR_DATASETS,
							datasetlist);
				}
			}
			/*
			 * Perform mapping on a per network basis to track associations with
			 * datasets
			 */
			for (CyNetwork network : networkList) {
				/*
				 * Must collapse any expanded metanodes prior to collecting node
				 * list, otherwise it will include children nodes (e.g., prior
				 * dataset nodes) and produce an invalid group of groups.
				 */
				for (CyGroup gn : CyGroupManager.getGroupList(network)) {
					if (CyGroupManager.getGroupList(
							CyGroupManager.getGroupViewer("metaNode"))
							.contains(gn)) {
						if (gn.getState() == 1)
							gn.setState(2);
					}
				}

				for (CyNode cn : (List<CyNode>) network.nodesList()) {

					String nodeKey = cn.getIdentifier();
					/*
					 * First, check if datanode == existing network node
					 */
					if (dn.getIdentifier().equals(cn.getIdentifier())) {
						// mapping has already been performed, naturally,
						// so just tag network
						mappedToNetworks.add(network);
						logger.debug(network.getTitle() + ": "
								+ dn.getIdentifier() + "=>"
								+ cn.getIdentifier() + " mapping naturally");
						continue;
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
							mapData(d, dn, dnKeyType, attrs, cn, network);
							// mapAttributes(dn, dnKeyType, attrs, cn);
							mappedToNetworks.add(network);
							logger.debug(network.getTitle() + ": "
									+ dn.getIdentifier() + "=>"
									+ cn.getIdentifier()
									+ " mapping by ID/CODE: " + pkt);
							continue;
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
								mapData(d, dn, dnKeyType, attrs, cn, network);
								// mapAttributes(dn, dnKeyType, attrs, cn);
								mappedToNetworks.add(network);
								logger.debug(network.getTitle() + ": "
										+ dn.getIdentifier() + "=>"
										+ cn.getIdentifier()
										+ " mapping by dataset secondary key: "
										+ secKeyType);
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
										mapData(d, dn, dnKeyType, attrs, cn,
												network);
										// mapAttributes(dn, dnKeyType, attrs,
										// cn);
										mappedToNetworks.add(network);
										logger
												.debug(network.getTitle()
														+ ": "
														+ dn.getIdentifier()
														+ "=>"
														+ cn.getIdentifier()
														+ " mapping by network-dataset secondary keys: "
														+ secKeyType);
										break; // skip remaining secondary keys
									}
								}
							}
						}
					}
				}

			} // end for each network

		} // end for each node in dataset
		progress.setValue(90);
		progress.setString("Tagging networks");

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
		progress.setValue(100);
	}

	/**
	 * Check to see if the cn has already been mapped-to for this dataset. If
	 * so, then we need grouping strategy. If not, then a simple attribute copy
	 * will do.
	 * 
	 * Note: if someone loads multiple datasets with overlapping attribute
	 * names, these will be overwritten and not handled by groups. Groups are
	 * only formed when two or more dn from a single dataset map to a given cn.
	 */
	@SuppressWarnings("unchecked")
	private static boolean mapData(CyDataset d, CyNode dn, String dnType,
			List<String> attrs, CyNode cn, CyNetwork network) {
		String dnid = dn.getIdentifier();
		String cnid = cn.getIdentifier();

		// Node attr will tell us if this cn has already been mapped-to from
		// this dataset
		List<String> attr = (List<String>) Cytoscape.getNodeAttributes()
				.getListAttribute(cn.getIdentifier(),
						DatasetMapping.NET_ATTR_DATASETS);
		// System.out.print(cn.getIdentifier() + ":" + dn.getIdentifier());
		if (null == attr || attr.size() == 0) {
			if (cn.isaGroup()) {
				/*
				 * This is a pre-existing group node! We can not merge our
				 * metanode dataset mapping strategy with other group uses. So
				 * this case is skipped (and reported).
				 */
				// TODO: report it!
				logger
						.warn("Can not map data to pre-existing group node! Failed to map data from "
								+ dn.getIdentifier()
								+ " on to "
								+ cn.getIdentifier() + ".");
				return false;
			}
			// virgin node, so just map attributes
			return mapAttributes(d, dn, dnType, attrs, cn);
		} else {
			/*
			 * We have mapped to this node before. We need to check from which
			 * dataset the prior mapping was performed and whether we've already
			 * started using metanodes.
			 */

			// If cn is a gn, then we know to continue with grouping strategy
			if (cn.isaGroup()) {
				logger.debug("add " + dn.getIdentifier() + " to group "
						+ cn.getIdentifier());
				return relateNodes(dn, cn, network, d);
			}

			/*
			 * Otherwise, this is the first time switching to group strategy, so
			 * we need to retrieve the prior mapped dn from the prior dataset
			 * and process both the prior and the new dn as a group.
			 * 
			 * Note: if prior === new, then we just skip it (and report it!).
			 * This is a case where user is loading multiple values per dnid,
			 * which is not supported.
			 */
			if (attr.size() > 1) {
				// There should only be one prior dataset recorded
				// or else it would already be a metanode!
				logger
						.warn("More than one prior dataset for a non-metanode?! Failed to comprehend mapping of "
								+ dn.getIdentifier()
								+ " to "
								+ cn.getIdentifier()
								+ ". Skipped it, moving on.");
				return false;
			}

			// get prior dataset name
			String priordname = attr.get(0);
			attr = (List<String>) Cytoscape
					.getNodeAttributes()
					.getListAttribute(cnid,
							DatasetMapping.NET_ATTR_DATASET_PREFIX + priordname);

			if (null == attr) {
				// ah, we've been fooled! This is not a grouping situation.
				logger
						.warn("No node recorded for prior dataset mapping?! Failed to comprehend mapping of "
								+ dn.getIdentifier()
								+ " on to "
								+ cn.getIdentifier()
								+ ". Skipped it, moving on.");
				return false;
			} else if (attr.size() > 1) {
				// There should only be one prior node recorded
				// or else it would already be a metanode!
				logger
						.warn("More than one prior node for a non-metanode?! Failed to comprehend mapping of "
								+ dn.getIdentifier()
								+ " to "
								+ cn.getIdentifier()
								+ ". Skipped it, moving on.");
				return false;
			}

			// OK. All is well. Proceed!
			String priordnid = attr.get(0);
			if (priordnid.equals(dnid)) {
				logger
						.warn("We've seen this data identifier before... Sorry, we can only map one row of data from "
								+ dn.getIdentifier()
								+ " to "
								+ cn.getIdentifier() + ".");
				return false;
			}
			// remap prior data node
			CyNode priordn = Cytoscape.getCyNode(priordnid, false);
			if (d.getNodes().contains(priordn.getRootGraphIndex())) {
				logger
						.debug("Remapping " + priordn.getIdentifier()
								+ " into new metanode version of "
								+ cn.getIdentifier());
				relateNodes(priordn, cn, network, null);
			}
			// map this data node
			logger.debug("Mapping " + dn.getIdentifier() + " into metanode "
					+ cn.getIdentifier());
			return relateNodes(dn, cn, network, d);
		}

	}
	/**
	 * Declare network node to be a group node and add dataset nodes as
	 * children. Attribute mapping (up to parent) will be handled globally by
	 * metanode settings in manageMetanodes().
	 * 
	 * @param dn
	 *            dataset CyNode
	 * @param cn
	 *            network CyNode
	 * @param network
	 *            CyNetwork
	 * @return
	 */
	private static boolean relateNodes(CyNode dn, CyNode cn, CyNetwork network,
			CyDataset d) {
		CyGroup gn;
		/*
		 * We have to create dn views on relevant network centered on or around
		 * cn location in order to inform final group node position when
		 * collapsed
		 */
		network.addNode(dn);
		CyNetworkView cnv = Cytoscape.getNetworkView(network.getIdentifier());
		Point2D o = cnv.getNodeView(cn).getOffset();
		cnv.getNodeView(dn).setOffset(o.getX(), o.getY());

		// check if group node already exists
		if (CyGroupManager.isaGroup(cn)) {
			gn = CyGroupManager.getCyGroup(cn);
			if (gn.getViewer().equals("metaNode")) {
				gn.addNode(dn);

				CyDataset.arrangeChildren(gn, cnv);

			} else {
				/*
				 * This is a non-metanode group node, meaning it's not ours! We
				 * can not merge our metanode dataset mapping strategy with
				 * pre-existing group nodes. So, this mapping gets skipped (and
				 * reported).
				 */
				// TODO: report it!
				logger
						.warn("Can not map data to pre-existing group node! Failed to map data from "
								+ dn.getIdentifier()
								+ " on to "
								+ cn.getIdentifier() + ".");
				// undo dataset node addition to network
				network.removeNode(dn.getRootGraphIndex(), false);
				return false;
			}
		} else {
			// else, then create group node and add dn as "nodelist"
			List<CyNode> nodelist = new ArrayList<CyNode>();
			nodelist.add(dn);
			logger.debug("creating new metanode: " + cn.getIdentifier()
					+ "; adding " + dn.getIdentifier());

			gn = CyGroupManager.createGroup(cn, nodelist, "metaNode", network);

		}
		/*
		 * Without this, CyNodeViews on new group nodes go to 'null' and the
		 * next attempt to add a child crashes with NPE.
		 */
		gn.setState(2);

		// and tag the node
		if (d != null)
			annotateNode(d, dn, cn);

		return true;
	}
	/**
	 * Sets metanode settings and and all metanode states. Go through each attr
	 * being loaded from dataset and make a custom aggregation override. This is
	 * preferred over default aggregation in order to preserve the attrs of the
	 * original network node when it is transformed into a group, e.g., the
	 * width and height attrs for GPML nodes.
	 * 
	 * @param d
	 *            CyDataset being loaded
	 */
	private static void metanodeSettings(CyDataset d) {

		// Aggregation Overrides
		WorkspacesCommandHandler.setMetanodeAggregation("true");
		List<String> attrs = d.getAttrs();
		for (String attr : attrs) {
			logger.debug("set aggregation override for: " + attr);
			Byte aType = Cytoscape.getNodeAttributes().getType(attr);
			switch (aType) {
				case CyAttributes.TYPE_STRING :
					WorkspacesCommandHandler
							.setAggregationOverride(attr, "csv");
					break;
				case CyAttributes.TYPE_INTEGER :
					WorkspacesCommandHandler.setAggregationOverride(attr,
							"median");
					break;
				case CyAttributes.TYPE_FLOATING :
					WorkspacesCommandHandler.setAggregationOverride(attr,
							"median");
					break;
				case CyAttributes.TYPE_BOOLEAN :
					WorkspacesCommandHandler.setAggregationOverride(attr, "or");
					break;
				case CyAttributes.TYPE_SIMPLE_LIST :
					WorkspacesCommandHandler.setAggregationOverride(attr,
							"concatenate");
					break;
			}
		}

		// And set all defaults to none to prevent overwriting cn attrs
		WorkspacesCommandHandler.setMetanodeAggregation("true",
				WorkspacesCommandHandler.ARG_ATTR_STRING, "none");
		WorkspacesCommandHandler.setMetanodeAggregation("true",
				WorkspacesCommandHandler.ARG_ATTR_INTEGER, "none");
		WorkspacesCommandHandler.setMetanodeAggregation("true",
				WorkspacesCommandHandler.ARG_ATTR_DOUBLE, "none");
		WorkspacesCommandHandler.setMetanodeAggregation("true",
				WorkspacesCommandHandler.ARG_ATTR_BOOLEAN, "none");
		WorkspacesCommandHandler.setMetanodeAggregation("true",
				WorkspacesCommandHandler.ARG_ATTR_LIST, "none");

		// Appearance
		WorkspacesCommandHandler.setDefaultMetanodeAppearance(false, 100.0,
				"none", null);

	}
	/**
	 * Collapses all metanodes in a given network by calling CyCommand. This is
	 * basically functioning as the second half of MetaNode.recollapse().
	 * 
	 * @param network
	 */
	private static void collapseAllMetanodes(CyNetwork network) {
		String netId = network.getIdentifier();
		WorkspacesCommandHandler.allMetanodesOperation(netId,
				WorkspacesCommandHandler.COLLAPSE_ALL);
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
	private static boolean mapAttributes(CyDataset d, CyNode dn, String dnType,
			List<String> attrs, CyNode cn) {
		String dnid = dn.getIdentifier();
		String cnid = cn.getIdentifier();
		Object listsample = null;

		// gather attribute info for copy action
		for (String attr : attrs) {
			Object entry = nodeAttrs.getAttribute(dnid, attr);
			Byte type = nodeAttrs.getType(attr);
			if (type == CyAttributes.TYPE_SIMPLE_LIST) {
				List l = nodeAttrs.getListAttribute(dnid, attr);
				listsample = l.get(0);
			}
			mapAttribute(cnid, attr, entry, type, listsample);
		}

		// and tag the node
		annotateNode(d, dn, cn);

		return true;
	}

	/**
	 * @param d
	 * @param dn
	 * @param cn
	 * @return
	 */
	private static boolean annotateNode(CyDataset d, CyNode dn, CyNode cn) {
		String dnid = dn.getIdentifier();
		String cnid = cn.getIdentifier();

		// add dataset to cynode attribute
		List<String> attr = (List<String>) Cytoscape.getNodeAttributes()
				.getListAttribute(cnid, DatasetMapping.NET_ATTR_DATASETS);

		if (null == attr) {
			attr = new ArrayList<String>();
		}
		if (!attr.contains(d.getName())) {
			attr.add(d.getName());
			Cytoscape.getNodeAttributes().setListAttribute(cnid,
					DatasetMapping.NET_ATTR_DATASETS, attr);
		}

		// add dnid to cynode attribute
		attr = (List<String>) Cytoscape.getNodeAttributes().getListAttribute(
				cnid, DatasetMapping.NET_ATTR_DATASET_PREFIX + d.getName());

		if (null == attr) {
			attr = new ArrayList<String>();
		}
		if (!attr.contains(dnid)) {
			attr.add(dnid);
			Cytoscape.getNodeAttributes().setListAttribute(cnid,
					DatasetMapping.NET_ATTR_DATASET_PREFIX + d.getName(), attr);
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
		if (null == entry) {
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
	 * Screens out networks that have already been mapped for this dataset
	 * (unless forced), as well as metanode-generated nested networks, networks
	 * without views, and networks directly generated from dataset.
	 * 
	 * @param networkList
	 * @param title
	 * @param force
	 * @return
	 */
	private static List<CyNetwork> screenNetworkList(
			List<CyNetwork> networkList, String datasetname, boolean force) {
		// collect list of virgin networks
		List<CyNetwork> netList = new ArrayList<CyNetwork>();
		for (CyNetwork network : networkList) {
			String netid = network.getIdentifier();
			if (Cytoscape.viewExists(netid)) {
				// check network attributes for dataset tag
				if (Cytoscape.getNetworkAttributes().hasAttribute(netid,
						NET_ATTR_DATASETS)) {
					List<String> sourcelist = (List<String>) Cytoscape
							.getNetworkAttributes().getListAttribute(netid,
									NET_ATTR_DATASETS);
					if (!sourcelist.contains(datasetname) || force) {
						if (!network.getTitle().equals(datasetname))
							netList.add(network);
					}
				} else if (Cytoscape.getNetworkAttributes().hasAttribute(netid,
						"parent_nodes")) {
					/*
					 * This only excludes metanode-generated nested networks
					 * that are currently with view. Collapsed metanodes with
					 * prior nested views have an empty "parent_nodes" attr and
					 * will still be mapped!
					 * 
					 * Thus, addition reason for the initial exclusion based on
					 * viewExists()
					 */
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
		Set<String> idTypes = WorkspacesCommandHandler.getTargetIdTypes();
		for (String t : idTypes) {
			if (t.contains("Ensembl"))
				// System.out.println("Hits: "+t);
				type = t;
		}
		return type;
	}

	/**
	 * 
	 */
	private static Map<String, Set<String>> collectTableMappings(
			List<String> nodeIds, String pkt, String skt, boolean isNew) {
		Map<String, Set<String>> secondaryKeyMap = new HashMap<String, Set<String>>();

		if (isNew) {
			logger.debug("collecting secondary keys from CyThesaurus");
			CyCommandResult result = mapIdentifiers(nodeIds, pkt, skt);
			if (null != result) {
				Map<String, Set<String>> keyMappings = (Map<String, Set<String>>) result
						.getResult();
				for (String primaryKey : keyMappings.keySet()) {
					secondaryKeyMap
							.put(primaryKey, keyMappings.get(primaryKey));
				}
			}
		} else {
			logger.debug("collecting secondary keys from node attribute: __"
					+ skt);
			for (String id : nodeIds) {
				List<String> sklist = nodeAttrs
						.getListAttribute(id, "__" + skt);
				Set<String> secKeys = new HashSet<String>(sklist);
				secondaryKeyMap.put(id, secKeys);
			}

		}
		return secondaryKeyMap;
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

}
