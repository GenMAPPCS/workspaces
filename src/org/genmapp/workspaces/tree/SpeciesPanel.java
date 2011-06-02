package org.genmapp.workspaces.tree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.SpringLayout;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

import org.genmapp.workspaces.utils.Downloader;

import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;

public class SpeciesPanel extends JPanel
		implements
			ActionListener,
			PropertyChangeListener,
			MouseListener {

	private static final long serialVersionUID = 3824505202304808596L;

	private static final String bridgedbSpecieslist = "http://svn.bigcat.unimaas.nl/bridgedb/trunk/org.bridgedb.bio/resources/org/bridgedb/bio/organisms.txt";
	public static final String bridgedbDerbyDir = "http://bridgedb.org/data/gene_database/";
	public static String genmappcsdir = "/GenMAPP-CS-Data/";
	public static String genmappcsdatabasedir;
	private String speciesState = null;
	private String connState = null;
	private String derbyState = null;
	private String latestLocalState = null;
	String downloadFile = null;
	private static List<String> speciesList = new ArrayList<String>();
	private static String initSpecies = null;
	static JComboBox speciesBox;
	private JButton configButton;
	private JButton downloadButton;
	// private boolean autoRegister = false;
	static JLabel dbConnection;
	private static JLabel db2Connection;
	private Color green = new Color(20, 150, 20);
	private Color red = new Color(200, 50, 50);
	private Color orange = new Color(200, 150, 50);
	private Color blue = new Color(50, 50, 180);
	private Color grey = new Color(230, 230, 230);

	/**
	 * Hash of latin name keys mapped to latest databases available for
	 * download.
	 */
	public static Map<String, String> latestDatabases = new HashMap<String, String>();

	/**
	 * Hash of latin name keys mapped to arrays of common[0] and two-letter[1]
	 * values.
	 */
	public static Map<String, String[]> supportedSpecies = new HashMap<String, String[]>();

	public SpeciesPanel() {
		super();

		/*
		 * Species Selection Panel
		 */
		// Make sure default species is in list right away
		// and selected by default
		initSpecies = CytoscapeInit.getProperties().getProperty(
				"defaultSpeciesName");
		this.speciesState = initSpecies;
		speciesBox = new JComboBox();
		speciesBox.addItem(initSpecies);
		//speciesBox.setEnabled(false);
		speciesBox.addActionListener(this);
		speciesBox.setToolTipText("Select a species-specific database");

		configButton = new JButton(new ImageIcon(getClass().getResource(
				"../images/configure.png")));
		downloadButton = new JButton(new ImageIcon(getClass().getResource(
				"../images/download-green.png")));
		configButton.addMouseListener(this);
		downloadButton.addMouseListener(this);
		configButton.setEnabled(false);
		downloadButton.setEnabled(false);
		configButton.setToolTipText("Manually configure database resources");

		this.add(speciesBox);
		this.add(downloadButton);
		this.add(configButton);

		// Create main genmappcsdata dir
		genmappcsdir = System.getProperty("user.home") + genmappcsdir;
		genmappcsdatabasedir = genmappcsdir + "databases/";

		File gcsdir = new File(genmappcsdir);
		if (!gcsdir.exists()) {
			gcsdir.mkdir();
		}

		/*
		 * Start thread to fill in available species.
		 */
		final SwingWorker<Boolean, Void> worker1 = new SwingWorker<Boolean, Void>() {

			public Boolean doInBackground() {
				initializeSupportedSpecies();
				return true;
			}
		};
		worker1.execute();

		/*
		 * Start thread to compile list of downloadable databases.
		 */
		SwingWorker<Boolean, Void> worker2 = new SwingWorker<Boolean, Void>() {

			public Boolean doInBackground() {
				try {
					worker1.get();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} catch (ExecutionException e1) {
					e1.printStackTrace();
				}
				// AFTER worker1 is done
				try {
					initializeLatestDatabases();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			}
		};
		worker2.execute();

		// add connection text
		dbConnection = new JLabel("initializing...") {
			public JToolTip createToolTip() {
				return new JMultiLineToolTip();
			}
		};
		dbConnection.setForeground(blue);
		dbConnection.setFont(new Font("Arial", Font.ITALIC, 12));
		dbConnection.setForeground(blue);
		this.add(dbConnection);

		// add label for secondary db connections
		db2Connection = new JLabel() {
			public JToolTip createToolTip() {
				return new JMultiLineToolTip();
			}
		};

		db2Connection.setFont(new Font("Arial", Font.ITALIC, 12));
		this.add(db2Connection);

		// Layout
		SpringLayout layout = new SpringLayout();
		layout.putConstraint(SpringLayout.WEST, speciesBox, 5,
				SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.NORTH, speciesBox, 2,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, downloadButton, 1,
				SpringLayout.EAST, speciesBox);
		layout.putConstraint(SpringLayout.NORTH, downloadButton, 0,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, configButton, 1,
				SpringLayout.EAST, downloadButton);
		layout.putConstraint(SpringLayout.NORTH, configButton, 0,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, dbConnection, 0,
				SpringLayout.HORIZONTAL_CENTER, this);
		layout.putConstraint(SpringLayout.NORTH, dbConnection, 5,
				SpringLayout.SOUTH, speciesBox);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, db2Connection, 0,
				SpringLayout.HORIZONTAL_CENTER, this);
		layout.putConstraint(SpringLayout.NORTH, db2Connection, 2,
				SpringLayout.SOUTH, dbConnection);

		this.setLayout(layout);

		// this.setBackground(grey);
		this.setMinimumSize(new Dimension(320, 95));
		this.setPreferredSize(new Dimension(320, 95));
		this.setMaximumSize(new Dimension(400, 95));

		Border etchedBdr = BorderFactory.createEtchedBorder();
		Border titledBdr = BorderFactory.createTitledBorder(etchedBdr,
				"Database");
		this.setBorder(titledBdr);

		// Make this a prop change listener for Cytoscape global events.
		Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(this);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param e
	 *            DOCUMENT ME!
	 */
	public void propertyChange(PropertyChangeEvent e) {
		// TODO: add appropriate items here
		String prop = e.getPropertyName();
		if (prop.equals(Cytoscape.CYTOSCAPE_INITIALIZED)) {
			WaitForResoucesThread a = new WaitForResoucesThread(this);
			a.start();

			ConnectResourcesThread b = new ConnectResourcesThread(a, this);
			b.start();

			// now you can click on this
			configButton.setEnabled(true);

		} else if (prop.equals(Cytoscape.NETWORK_CREATED)) {
			// Apply selected criteria set(s)
		}
	}

	/**
	 * Collects list of species from centralized BridgeDb file and populates
	 * species selector. Should only run once per session.
	 * 
	 */
	private static void initializeSupportedSpecies() {
		List<String> lines = readUrl(bridgedbSpecieslist);

		/*
		 * If readUrl fails (for whatever reason), then just read local files to
		 * provide offline performance.
		 */
		if (null == lines || lines.size() < 1) {
			String derbyfile = null;
			int latest = 0;
			File dir = new File(genmappcsdatabasedir);
			if (!dir.exists()) {
				dir.mkdir();
			}

			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name.endsWith(".bridge") || name.endsWith(".pgdb"));
				}
			};

			String[] children = dir.list(filter);
			if (children == null) {
				JOptionPane
						.showMessageDialog(
								Cytoscape.getDesktop(),
								"Can not find databases online or in local directory.\n Please verify internet connection or download databases at your own convenience from\n\t http://bridgedb.org/data/gene_database/ \n and place in your user directory under GenMAPP-CS-Data/databases/",
								"Show stopper", JOptionPane.WARNING_MESSAGE);
			} else {
				JOptionPane
						.showMessageDialog(
								Cytoscape.getDesktop(),
								"Can not find databases online, but we did find some in a local directory.\n We will attempt to use these without reference to centralized resources. \n You may experience unexpected behaviors...",
								"Technical difficulty",
								JOptionPane.WARNING_MESSAGE);

				// TODO: need better solution here
				// e.g., create and reuse local file
				for (int i = 0; i < children.length; i++) {
					// Get filename of file or directory
					String filename = children[i];
					String twoletter = filename.substring(0, 2);
					// add unique list of twoletter codes
					if (!supportedSpecies.containsKey(twoletter)) {
						speciesList.add(twoletter);
						supportedSpecies.put(twoletter, new String[]{twoletter,
								twoletter});
					}

				}
			}

		} else {
			for (String line : lines) {
				String[] s = line.split("\t");
				// format: genus \t species \t common \t two-letter
				supportedSpecies.put(s[0] + " " + s[1],
						new String[]{s[2], s[3]});
				speciesList.add(s[0] + " " + s[1]);
			}
		}
		Collections.sort(speciesList);
		for (String s : speciesList) {
			speciesBox.addItem(s);
		}
		//speciesBox.setSelectedItem(initSpecies);
		//speciesBox.setEnabled(true);
	}

	/**
	 * Check the BridgeDb download site for the lateset Derby database files.
	 * Stores these in a hash for reference later. Should only run once per
	 * session.
	 * 
	 * @param species
	 */
	private void initializeLatestDatabases() throws MalformedURLException,
			IOException {

		String derbyfile = null;
		URL url = new URL(bridgedbDerbyDir);
		System.out.println("connecting to " + url.toString());

		HttpURLConnection c = (HttpURLConnection) url.openConnection();
		InputStream in = c.getInputStream();
		if (in != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					Pattern p = Pattern
							.compile(".*([A-Z][a-z]_Derby_\\d+\\.zip).*");
					Matcher m = p.matcher(line);
					while (m.find()) {
						derbyfile = m.group(1);
						latestDatabases.put(derbyfile.substring(0, 2),
								derbyfile);
						// simply keep the last keyed match since
						// they are already ordered by date
					}
				}
			} finally {
				in.close();
			}
		} else {
			System.out.println("No databases found at " + bridgedbDerbyDir);
		}
	}

	/**
	 * @param thread
	 */
	protected void timedResourceCheck(Thread thread) {
		// System.out.print("TIMING : ");
		int resourcesCount = 0;
		int attempts = 0;
		while (resourcesCount == 0) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			resourcesCount = latestDatabases.size();
			// System.out.print(attempts + "..." + resourcesCount + "\n");
			if (attempts++ >= 10) {
				dbConnection.setText("No databases found!  ");
				dbConnection
						.setToolTipText("You've basically got three options:\n 1. Try the download button\n 2. Configure your own resources\n 3. Select another species");
				dbConnection.setForeground(red);
				resourcesCount = -1;

				// try to kill worker
				thread.interrupt();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Sets status and tooltip for the download button based on availability of
	 * derby databases remotely and currently attached derby databases. Should
	 * run every time resources or species are changed.
	 * 
	 * @param species
	 */
	private void setDownloadButton(String species) {
		String remote = null;
		String current = this.derbyState;

		// two-letter code
		String key = supportedSpecies.get(species)[1];
		if (null != latestDatabases && !latestDatabases.isEmpty())
			remote = latestDatabases.get(key);

		this.downloadFile = remote;

		if (null == current) {
			if (null == remote) {
				// no database; no database!
				downloadButton.setToolTipText("Sorry, no database to be found");
				downloadButton.setEnabled(false);
			} else {
				// no local database; database to download
				downloadButton.setToolTipText("Download database for "
						+ supportedSpecies.get(species)[0]);
				downloadButton.setEnabled(true);
			}
		} else {
			// local database
			if (null == remote) {
				// nothing to do
				// using latest database
				downloadButton.setToolTipText("No update available");
				downloadButton.setEnabled(false);
			} else {
				/*
				 * Databases local and remote. So, compare versions.
				 */
				
				Pattern p = Pattern.compile("(\\d{6})");
				Matcher m = p.matcher(current);
				String currentdate = m.group(1); //current. current.substring(0, current.indexOf("."));
				int currentdateint = Integer.decode(currentdate);
				m = p.matcher(remote);
				String remotedate = m.group(1); // remote.substring(0, remote.indexOf("."));
				int remotedateint = Integer.decode(remotedate);
				m = p.matcher(this.latestLocalState);
				String latestlocaldate = m.group(1);
				int latestlocaldateint = Integer.decode(latestlocaldate);
				if (currentdateint < remotedateint) {
					// updated database available
					dbConnection.setForeground(red);
					if (currentdateint < latestlocaldateint) {
						// already got a newer one locally
						downloadButton
								.setToolTipText("Switch to latest database for "
										+ supportedSpecies.get(species)[0]);

					} else {
						// download it
						downloadButton
								.setToolTipText("Download updated database for "
										+ supportedSpecies.get(species)[0]);
					}
					downloadButton.setEnabled(true);
				} else {
					// using latest database
					downloadButton
							.setToolTipText("Already using latest database");
					downloadButton.setEnabled(false);
				}
			}
		}
		// and re-enable config button
		configButton.setEnabled(true);
	}

	/**
	 * @param strUrl
	 * @return
	 */
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
		// TODO: refactor executor
		try {
			if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
				System.err.println("Failed to connect to " + strUrl);
				executor.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * Queries CyThesaurus for currently selected resources. Prepares display
	 * list based on database type. Should run every time resources are changed.
	 * 
	 * @return number of selected resources
	 */
	public Integer updateResourceDisplay() {
		int count = 0;
		// TODO: move to CommandHandler
		Map<String, Object> args = new HashMap<String, Object>();
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping",
					"list selected resources", args);
			Set<String> mappers = (Set<String>) result.getResult();
			count = mappers.size();
			if (count < 1) {
				if (this.latestLocalState != null) {
					/*
					 * No database selected, but there is one there. This
					 * awkward state can occur when user manually configures 0
					 * databases
					 */
					dbConnection.setText("No database selected  ");
					dbConnection
							.setToolTipText("Make species selection or manually configure resources");
					dbConnection.setForeground(blue);
				} else {
					this.connState = null;
					this.derbyState = null;
				}
			}
			String db2ReList = "";
			for (String re : mappers) {
				if (re.startsWith("idmapper-bridgerest")) {
					String url = re.substring(re.indexOf("http"));
					dbConnection.setText(url);
					dbConnection.setToolTipText(re);
					dbConnection.setForeground(red);
					this.connState = re;
					this.derbyState = null;
				} else if (re.startsWith("idmapper-pgdb")) {
					String filename = re.substring(re.indexOf(".") - 17);
					dbConnection.setText(filename);
					dbConnection.setToolTipText(re);
					dbConnection.setForeground(green);
					this.connState = re;
					this.derbyState = filename;
					identifyLatestLocal(this.speciesState);
				} else {
					db2Connection.setText("plus custom connections...");
					db2Connection.setForeground(green);
					db2ReList = db2ReList + re + "\n";
				}
			}
			if (db2ReList.length() > 1) {
				// remove final \n character
				db2ReList = db2ReList.substring(0, db2ReList.length() - 2);
				db2Connection.setToolTipText(db2ReList);
			} else {
				db2Connection.setText("");
			}
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// update status of download button
		setDownloadButton(this.speciesState);

		return count;
	}

	/**
	 * Deselects current resource selection (derby or web service), then
	 * searches locally for latest species-specific database. If not found, then
	 * finds appropriate BridgeRest service. Registers and selects found derby
	 * or web service resources. Updates display of selected resources. Should
	 * be called when species or resources change.
	 * 
	 * @param species
	 */
	public void connectToResources() {
		/*
		 * Deselect old derby or web service resources
		 */
		if (null != this.connState) {
			// TODO: Move to CommandHandler
			Map<String, Object> args = new HashMap<String, Object>();
			args.put("connstring", this.connState);
			CyCommandResult result;
			try {
				result = CyCommandManager.execute("idmapping",
						"deselect resource", args);
			} catch (CyCommandException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (RuntimeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		/*
		 * Find latest local derby file for new species
		 */
		identifyLatestLocal(this.speciesState);

		/*
		 * Register new derby or web service resources
		 */
		String classpath;
		String connstring;
		String displayname;
		if (null == this.latestLocalState) {
			// then try web service
			dbConnection.setText("Connecting to BridgeDb web service...");
			dbConnection.setForeground(blue);
			classpath = "org.bridgedb.webservice.bridgerest.BridgeRest";
			connstring = "idmapper-bridgerest:http://webservice.bridgedb.org/"
					+ this.speciesState;
			displayname = "http://webservice.bridgedb.org/" + this.speciesState;
		} else {
			dbConnection.setText("Connecting to " + this.latestLocalState
					+ "...");
			dbConnection.setForeground(blue);
			classpath = "org.bridgedb.rdb.IDMapperRdb";
			connstring = "idmapper-pgdb:" + genmappcsdatabasedir
					+ this.latestLocalState;
			displayname = this.latestLocalState;
		}
		// TODO: Move to CommandHandler
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("classpath", classpath);
		args.put("connstring", connstring);
		args.put("displayname", displayname);
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping",
					"register resource", args);
			List<String> results = result.getMessages();
			if (results.size() > 0) {
				for (String re : results) {
					if (re.contains("Success")) {
						// save the connState for this panel
						this.connState = connstring;
					} else {
						// don't bother trying to deselect in the future
						this.connState = null;
					}
				}
			} else {
				dbConnection.setText("Failed to connect!  ");
				dbConnection.setToolTipText(displayname);
				dbConnection.setForeground(red);
				this.connState = null;
			}
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// and, finally, refresh list of selected resources
		updateResourceDisplay();
	}

	/**
	 * Find latest local derby file for new species
	 * 
	 * @return filename
	 */
	private void identifyLatestLocal(String species) {
		String derbyfile = null;
		final String prefix = supportedSpecies.get(species)[1];
		int latest = 0;
		File dir = new File(genmappcsdatabasedir);
		if (!dir.exists()) {
			dir.mkdir();
		}

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith(prefix) && (name.endsWith(".bridge") || name
						.endsWith(".pgdb")));
			}
		};

		String[] children = dir.list(filter);
		if (children == null) {
			// Either dir does not exist or is not a directory
		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of file or directory
				String filename = children[i];
				String temp = filename.substring(9, filename.indexOf("."));
				if (temp.matches("^\\d+$")) {
					int date = new Integer(temp);
					if (date > latest) {
						derbyfile = filename;
					}
				}
			}
		}
		this.latestLocalState = derbyfile;
	}

	/**
	 * Opens CyThesaurus resource configuration dialog. Updates display of
	 * selected resources after dialog is closed.
	 */
	private void configureManually() {
		boolean b = false;
		// TODO: Move to CommandHandler
		Map<String, Object> noargs = new HashMap<String, Object>();
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping",
					"resource config dialog", noargs);
			b = (Boolean) result.getResult();
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// update list of selected resources
		if (b) {
			updateResourceDisplay();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		/*
		 * Temporarily disable buttons and update message while performing
		 * action.
		 */
		downloadButton.setEnabled(false);
		configButton.setEnabled(false);
		dbConnection.setText("Connecting...  ");
		dbConnection.setToolTipText("one moment please");
		dbConnection.setForeground(blue);

		JComboBox b = (JComboBox) e.getSource();
		this.speciesState = (String) b.getSelectedItem();
		CytoscapeInit.getProperties().setProperty("defaultSpeciesName",
				this.speciesState);

		/*
		 * Start thread to register new resources per species selection.
		 */
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

			public Boolean doInBackground() {
				connectToResources();
				return true;
			}
		};
		worker.execute();
	}

	public void mouseClicked(MouseEvent e) {

		if (e.getSource().equals(configButton) && configButton.isEnabled()) {
			configureManually();
		} else if (e.getSource().equals(downloadButton)
				&& downloadButton.isEnabled()) {

			/*
			 * If there is a more recent database locally, then simply trigger
			 * speciesBox action performed to make the switch
			 */
			if (null != this.latestLocalState) {
				if (!this.latestLocalState.equals(this.derbyState)) {
					speciesBox.setSelectedItem(this.speciesState);

					// and skip download
					return;
				}
			}

			// In the meantime display message and progress
			speciesBox.setEnabled(false);
			downloadButton.setEnabled(false);
			configButton.setEnabled(false);
			dbConnection.setText(downloadFile + ": 0%");
			dbConnection.setToolTipText("one moment please...");
			dbConnection.setForeground(blue);
			downloadButton.setToolTipText("Downloading...");

			DownloadThread a = new DownloadThread(this);
			a.start();
			ConnectResourcesThread b = new ConnectResourcesThread(a, this);
			b.start();

		}
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}

/**
 * 
 * @author Anurag Sharma
 */
class DownloadThread extends Thread {

	private SpeciesPanel sp;

	public DownloadThread(SpeciesPanel sp) {
		this.sp = sp;
	}

	public void run() {
		try {
			System.out.println("starting download");
			Downloader d = new Downloader();
			d.download(sp.bridgedbDerbyDir + sp.downloadFile);
			int progress = d.getProgress();
			while (progress < 99) {
				sp.dbConnection
						.setText(sp.downloadFile + ": " + progress + "%");
				Thread.sleep(500);
				progress = d.getProgress();

			}
			sp.dbConnection.setText(sp.downloadFile + ": 100%");
			sp.dbConnection.setText("Uncompressing " + sp.downloadFile + "...");

			// must wait for completion of uncompression before giving up thread
			d.waitFor();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

/**
 * Start thread to check on web resources, such as latestDatabases, before
 * making connections. This coupling of threads is critical to get the
 * initialization timing right.
 */
class WaitForResoucesThread extends Thread {

	private SpeciesPanel sp;

	public WaitForResoucesThread(SpeciesPanel sp) {
		this.sp = sp;
	}

	public void run() {
		sp.timedResourceCheck(this);
	}
}

/**
 * After prior thread, connect to available resources.
 */
class ConnectResourcesThread extends Thread {

	private Thread priorThread;
	private SpeciesPanel sp;

	public ConnectResourcesThread(Thread a, SpeciesPanel sp) {
		this.priorThread = a;
		this.sp = sp;
	}

	public void run() {
		try {
			priorThread.join(); // wait for prior thread to finish
		} catch (InterruptedException ex) {
		}
		sp.connectToResources();
		sp.speciesBox.setEnabled(true);
	}
}

/**
 * Nifty override to support multi-line tool tips. Copied from
 * http://www.codeguru.com/java/articles/122.shtml
 * 
 * @author Zafir Anjum
 */
class JMultiLineToolTip extends JToolTip {
	private static final String uiClassID = "ToolTipUI";

	String tipText;
	JComponent component;

	public JMultiLineToolTip() {
		updateUI();
	}

	public void updateUI() {
		setUI(MultiLineToolTipUI.createUI(this));
	}

	public void setColumns(int columns) {
		this.columns = columns;
		this.fixedwidth = 0;
	}

	public int getColumns() {
		return columns;
	}

	public void setFixedWidth(int width) {
		this.fixedwidth = width;
		this.columns = 0;
	}

	public int getFixedWidth() {
		return fixedwidth;
	}

	protected int columns = 0;
	protected int fixedwidth = 0;
}

class MultiLineToolTipUI extends BasicToolTipUI {
	static MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();
	Font smallFont;
	static JToolTip tip;
	protected CellRendererPane rendererPane;

	private static JTextArea textArea;

	public static ComponentUI createUI(JComponent c) {
		return sharedInstance;
	}

	public MultiLineToolTipUI() {
		super();
	}

	public void installUI(JComponent c) {
		super.installUI(c);
		tip = (JToolTip) c;
		rendererPane = new CellRendererPane();
		c.add(rendererPane);
	}

	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);

		c.remove(rendererPane);
		rendererPane = null;
	}

	public void paint(Graphics g, JComponent c) {
		Dimension size = c.getSize();
		textArea.setBackground(c.getBackground());
		rendererPane.paintComponent(g, textArea, c, 1, 1, size.width - 1,
				size.height - 1, true);
	}

	public Dimension getPreferredSize(JComponent c) {
		String tipText = ((JToolTip) c).getTipText();
		if (tipText == null)
			return new Dimension(0, 0);
		textArea = new JTextArea(tipText);
		rendererPane.removeAll();
		rendererPane.add(textArea);
		textArea.setWrapStyleWord(true);
		int width = ((JMultiLineToolTip) c).getFixedWidth();
		int columns = ((JMultiLineToolTip) c).getColumns();

		if (columns > 0) {
			textArea.setColumns(columns);
			textArea.setSize(0, 0);
			textArea.setLineWrap(true);
			textArea.setSize(textArea.getPreferredSize());
		} else if (width > 0) {
			textArea.setLineWrap(true);
			Dimension d = textArea.getPreferredSize();
			d.width = width;
			d.height++;
			textArea.setSize(d);
		} else
			textArea.setLineWrap(false);

		Dimension dim = textArea.getPreferredSize();

		dim.height += 1;
		dim.width += 1;
		return dim;
	}

	public Dimension getMinimumSize(JComponent c) {
		return getPreferredSize(c);
	}

	public Dimension getMaximumSize(JComponent c) {
		return getPreferredSize(c);
	}
}
