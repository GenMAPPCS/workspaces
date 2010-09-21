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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.CellRendererPane;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.SpringLayout;
import javax.swing.SwingWorker;
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

	private static final long serialVersionUID = 1L;
	private static final String bridgedbOrglist = "http://svn.bigcat.unimaas.nl/bridgedb/trunk/org.bridgedb.bio/resources/org/bridgedb/bio/organisms.txt";
	public static final String bridgedbDerbyDir = "http://bridgedb.org/data/gene_database/";
	public static String genmappcsdir = "/GenMAPP-CS-Data/";
	public static String genmappcsdatadir;
	private String speciesState = null;
	private String connState = null;
	private String derbyState = null;
	private String downloadFile = null;
	private static JComboBox speciesBox;
	private JButton configButton;
	private JButton downloadButton;
	// private boolean autoRegister = false;
	private static JLabel dbConnection;
	private static JLabel db2Connection;
	private Color green = new Color(20, 150, 20);
	private Color red = new Color(200, 50, 50);
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
		this.speciesState = defaultSpecies;
		speciesBox.addItem(defaultSpecies);
		speciesBox.addActionListener(this);

		configButton = new JButton(new ImageIcon(getClass().getResource(
				"../images/configure.png")));
		downloadButton = new JButton(new ImageIcon(getClass().getResource(
				"../images/download-green.png")));
		configButton.addMouseListener(this);
		downloadButton.addMouseListener(this);
		configButton.setEnabled(false);
		downloadButton.setEnabled(false);
		configButton.setToolTipText("manually configure database resources");

		this.add(speciesBox);
		this.add(downloadButton);
		this.add(configButton);

		/*
		 * Start thread to fill in available species.
		 */
		SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

			public String doInBackground() {
				String msg = "done!";
				checkSupportedOrganisms();
				// TODO: store local copy for offline operation
				return msg;
			}
		};
		worker.execute();

		/*
		 * Start thread to compile list of downloadable databases.
		 */
		SwingWorker<String, Void> worker2 = new SwingWorker<String, Void>() {

			public String doInBackground() {
				String msg = "done!";
				try {
					checkLatestDatabases();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// TODO: store local copy for offline operation
				return msg;
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
		layout.putConstraint(SpringLayout.NORTH, speciesBox, 3,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, downloadButton, 1,
				SpringLayout.EAST, speciesBox);
		layout.putConstraint(SpringLayout.NORTH, downloadButton, 1,
				SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, configButton, 1,
				SpringLayout.EAST, downloadButton);
		layout.putConstraint(SpringLayout.NORTH, configButton, 1,
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
		this.setMinimumSize(new Dimension(310, 70));
		this.setPreferredSize(new Dimension(310, 70));
		this.setMaximumSize(new Dimension(400, 70));

		// add config... and download... links

		genmappcsdir = System.getProperty("user.home") + genmappcsdir;
		genmappcsdatadir = genmappcsdir + "databases/";

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
			/*
			 * Start thread to "listen" for registration of default resources by
			 * CyThesaurus. Usually takes just over a second so we sleep for bit
			 * before trying.
			 */
			SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

				public String doInBackground() {
					String msg = "done!";
					int resourcesCount = 0;
					int attempts = 0;
					while (resourcesCount == 0) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						resourcesCount = listResources();
						if (attempts++ == 5) {
							dbConnection.setText("no connections!");
							dbConnection
									.setToolTipText("You've basically got three options:\n 1. Try the download button\n 2. Configure your own resources\n 3. Select another species");
							dbConnection.setForeground(red);
							resourcesCount = -1;
						}
					}
					return msg;
				}
			};
			worker.execute();

			// now you can click on this
			configButton.setEnabled(true);

		} else if (prop.equals(Cytoscape.NETWORK_CREATED)) {
			// Apply selected criteria set(s)
		}
	}

	/**
	 * 
	 */
	private static void checkSupportedOrganisms() {
		List<String> lines = readUrl(bridgedbOrglist);

		for (String line : lines) {
			String[] s = line.split("\t");
			// format: genus \t species \t common \t two-letter
			supportedOrganisms.put(s[0] + " " + s[1], new String[]{s[2], s[3]});
			speciesBox.addItem(s[0] + " " + s[1]);
		}

	}

	/**
	 * Check the BridgeDb download site for the lateset Derby database files.
	 * Store these in a hash for reference later.
	 * 
	 * @param species
	 */
	private void checkLatestDatabases() throws MalformedURLException,
			IOException {
		// TODO: should make local file for offline performance
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
							.compile(".*([A-Z][a-z]_Derby_\\d+\\.bridge).*");
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
	 * @param species
	 */
	private void checkDownloadStatus(String species) {
		String remote = null;
		String local = this.derbyState;

		// two-letter code
		String key = supportedOrganisms.get(species)[1];
		remote = latestDatabases.get(key);
		this.downloadFile = remote;

		if (null == local) {
			if (null == remote) {
				// no database; no database!
				downloadButton.setToolTipText("sorry, no database to be found");
				downloadButton.setEnabled(false);
			} else {
				// no local database; database to download
				downloadButton.setToolTipText("download database for "
						+ supportedOrganisms.get(species)[0]);
				downloadButton.setEnabled(true);
			}
		} else {
			// local database
			if (null == remote) {
				// nothing to do
				// using latest database
				downloadButton.setToolTipText("no update available");
				downloadButton.setEnabled(false);
			} else {
				/*
				 * Databases local and remote. So, compare versions.
				 */
				if (!local.equals(remote)) {
					// updated database available
					downloadButton
							.setToolTipText("download updated database for "
									+ supportedOrganisms.get(species)[0]);
					downloadButton.setEnabled(true);
				} else {
					// using latest database
					downloadButton
							.setToolTipText("already using latest database");
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

	public Integer listResources() {
		int count = 0;
		Map<String, Object> args = new HashMap<String, Object>();
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping",
					"list selected resources", args);
			Set<String> mappers = (Set<String>) result.getResult();
			count = mappers.size();
			String db2ReList = "";
			for (String re : mappers) {
				if (re.startsWith("idmapper-bridgerest")) {
					String url = re.substring(re.indexOf("http"));
					dbConnection.setText("Web service: " + url);
					dbConnection.setToolTipText(re);
					dbConnection.setForeground(green);
					this.connState = re;
					this.derbyState = null;
				} else if (re.startsWith("idmapper-pgdb")) {
					String filename = re.substring(re.indexOf(".") - 17);
					dbConnection.setText("file: " + filename);
					dbConnection.setToolTipText(re);
					dbConnection.setForeground(green);
					this.connState = re;
					this.derbyState = filename;
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
		checkDownloadStatus(this.speciesState);

		return count;
	}

	/**
	 * @param species
	 */
	public void collectResources(String species) {

		/*
		 * Deselect old derby or web service resources
		 */
		if (null != this.connState) {
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
		String derbyfile = null;
		final String prefix = supportedOrganisms.get(species)[1];
		int latest = 0;
		File dir = new File(genmappcsdatadir);

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

		/*
		 * Register new derby or web service resources
		 */
		String classpath;
		String connstring;
		String displayname;
		if (null == derbyfile) {
			// then try web service
			dbConnection.setText("connecting to BridgeDb web service...");
			classpath = "org.bridgedb.webservice.bridgerest.BridgeRest";
			connstring = "idmapper-bridgerest:http://webservice.bridgedb.org/"
					+ species;
			displayname = "http://webservice.bridgedb.org/" + species;
		} else {
			dbConnection.setText("connecting to " + derbyfile + "...");
			classpath = "org.bridgedb.rdb.IDMapperRdb";
			connstring = "idmapper-pgdb:" + genmappcsdatadir + derbyfile;
			displayname = "file: " + derbyfile;
		}
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
						// dbConnection.setText(displayname);
						// dbConnection.setToolTipText(connstring);
						// dbConnection.setForeground(green);

						// save the connState for this panel
						this.connState = connstring;
					} else {
						// don't bother trying to deselect in the future
						this.connState = null;
					}
				}
			} else {
				dbConnection.setText("failed to connect to " + displayname
						+ "!");
				dbConnection.setToolTipText(connstring);
				dbConnection.setForeground(red);
			}
		} catch (CyCommandException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// update the species state for the panel
		this.speciesState = species;

		// and, finally, refresh list of selected resources
		listResources();
	}

	/**
	 * 
	 */
	private void configureResources() {
		boolean b = false;
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
		if (b)
			listResources();
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
		dbConnection.setText("connecting...");
		dbConnection.setToolTipText("one moment please");
		dbConnection.setForeground(blue);

		JComboBox b = (JComboBox) e.getSource();
		// String oldSpecies = this.speciesState;
		final String newSpecies = (String) b.getSelectedItem();
		CytoscapeInit.getProperties().setProperty("defaultSpeciesName",
				newSpecies);

		/*
		 * Fire prop change to trigger automatic resource management by
		 * CyThesaurus (i.e., default BridgeRest resources)
		 */
		// if (autoRegister)
		// Cytoscape.firePropertyChange(Cytoscape.PREFERENCE_MODIFIED,
		// oldSpecies, newSpecies);
		/*
		 * Start thread to register new resources per species selection.
		 */
		SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

			public String doInBackground() {
				String msg = "done!";
				collectResources(newSpecies);
				return msg;
			}
		};
		worker.execute();

		// for debugging...
		// updateResources();
	}

	public void mouseClicked(MouseEvent e) {

		if (e.getSource().equals(configButton) && configButton.isEnabled()) {
			configureResources();
		} else if (e.getSource().equals(downloadButton)
				&& downloadButton.isEnabled()) {
			dbConnection.setText("downloading " + this.downloadFile + "...");
			dbConnection.setToolTipText("one moment please...");
			dbConnection.setForeground(blue);
			downloadButton.setToolTipText("downloading...");
			downloadButton.setEnabled(false);
			configButton.setEnabled(false);
			
			try {
				Downloader d = new Downloader();
				d.download(bridgedbDerbyDir + this.downloadFile);
				d.waitFor();

			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// recollect resources
			collectResources(this.speciesState);
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
