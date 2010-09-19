package org.genmapp.workspaces.tree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cytoscape.CytoscapeInit;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;

public class SpeciesPanel extends JPanel implements ActionListener {

	private static final String bridgedbOrglist = "http://svn.bigcat.unimaas.nl/bridgedb/trunk/org.bridgedb.bio/resources/org/bridgedb/bio/organisms.txt";

	private static JComboBox speciesBox;

	/**
	 * Hash of latin name keys mapped to arrays of common[0] and two-letter[1]
	 * values.
	 */
	public static Map<String, String[]> supportedOrganisms = new HashMap<String, String[]>();

	public SpeciesPanel() {
		super();

		/*
		 * Species Selection Panel
		 */
		speciesBox = new JComboBox();

		// Make sure default species is in list right away
		// and selected by default
		String defaultSpecies = CytoscapeInit.getProperties().getProperty(
				"defaultSpeciesName");
		speciesBox.addItem(defaultSpecies);
		speciesBox.addActionListener(this);

		checkSupportedOrganisms();
		// TODO: store local copy for offline operation

		this.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		this.setBackground(new Color(204, 204, 204));
		this.setPreferredSize(new Dimension(180, 60));
		this.add(speciesBox);
		// add connection text
		checkResources();
		// add config... and download... links

	}

	private static void checkSupportedOrganisms() {
		List<String> lines = readUrl(bridgedbOrglist);

		for (String line : lines) {
			String[] s = line.split("\t");
			// format: genus \t species \t common \t two-letter
			supportedOrganisms.put(s[0] + " " + s[1], new String[]{s[2], s[3]});
			speciesBox.addItem(s[0] + " " + s[1]);
		}
	}

	private static List<String> readUrl(final String strUrl) {
		final List<String> ret = new ArrayList<String>();

		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new Runnable() {
			public void run() {
				try {
					URL url = new URL(strUrl);
					URLConnection yc = url.openConnection();
					BufferedReader in = new BufferedReader(
							new InputStreamReader(yc.getInputStream()));

					String inputLine;
					while ((inputLine = in.readLine()) != null)
						ret.add(inputLine);
					in.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				System.err.println("Failed to connect to " + strUrl);
				executor.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	public void checkResources() {
		Map<String, Object> args = new HashMap<String, Object>();
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping",
					"list selected resources", args);
			Set<String> mappers =  (Set<String>) result.getResult();
			for (String re : mappers){
				if (re.startsWith("idmapper-bridgerest"))
					this.add(new JLabel("Web service: BridgeRest"));
				else if (re.startsWith("idmapper-pgdb"))
					this.add(new JLabel("Local file: BridgeDerby "));
				else 
					this.add(new JLabel(re.substring(0, re.indexOf(":"))));
				
				System.out.println(re);
			}
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void actionPerformed(ActionEvent e) {
		JComboBox b = (JComboBox) e.getSource();
		String species = (String) b.getSelectedItem();
		System.out.println("SET SPECIES: " + species);
		CytoscapeInit.getProperties()
				.setProperty("defaultSpeciesName", species);

		// attempt to register resources per species selection
		// update text with name of resource connected (in green; use red text
		// while disconnected);
		checkResources();
	}
}
