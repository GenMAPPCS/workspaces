package org.genmapp.workspaces.tree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
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

import javax.swing.BoxLayout;
import javax.swing.CellRendererPane;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.SwingWorker;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;

public class SpeciesPanel extends JPanel implements ActionListener {

	private static final String bridgedbOrglist = "http://svn.bigcat.unimaas.nl/bridgedb/trunk/org.bridgedb.bio/resources/org/bridgedb/bio/organisms.txt";
	private static final String bridgedbDerbylist = "http://bridgedb.org/data/gene_database/";
	private String genmappcsdatadir = "/GenMAPP-CS-Data/databases/";
	private String speciesState = null;
	private String connState = null;
	private static JComboBox speciesBox;
	private boolean autoRegister = false;
	private static JLabel dbConnection;
	private static JLabel db2Connection;
	private Color green = new Color(20, 150, 20);
	private Color red = new Color(200, 50, 50);
	private Color blue = new Color(50, 50, 180);
	private Color grey = new Color(230, 230, 230);

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
		speciesBox.setAlignmentY(TOP_ALIGNMENT);
		speciesBox.setAlignmentX(CENTER_ALIGNMENT);

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
		

		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBackground(grey);
		this.setMinimumSize(new Dimension(180, 60));
		this.add(speciesBox);

		// add connection text
		dbConnection = new JLabel("initializing...") {
			public JToolTip createToolTip() {
				return new JMultiLineToolTip();
			}
		};
		dbConnection.setForeground(blue);
		dbConnection.setFont(new Font("Arial", Font.ITALIC, 12));
		dbConnection.setForeground(blue);
		dbConnection.setSize(170, 30);
		dbConnection.setAlignmentX(CENTER_ALIGNMENT);
		dbConnection.setAlignmentY(TOP_ALIGNMENT);

		this.add(dbConnection);

		// add label for secondary db connections
		db2Connection = new JLabel() {
			public JToolTip createToolTip() {
				return new JMultiLineToolTip();
			}
		};

		db2Connection.setFont(new Font("Arial", Font.ITALIC, 12));
		db2Connection.setSize(170, 30);
		db2Connection.setAlignmentX(CENTER_ALIGNMENT);
		db2Connection.setAlignmentY(TOP_ALIGNMENT);
		this.add(db2Connection);

		// add config... and download... links

		genmappcsdatadir = System.getProperty("user.home") + genmappcsdatadir;

		// hold until Cytoscape is initialized
		addInitializationListener();
	}

	public void addInitializationListener() {
		PropertyChangeSupport pcs = Cytoscape.getPropertyChangeSupport();

		pcs.addPropertyChangeListener(Cytoscape.CYTOSCAPE_INITIALIZED,
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						// reach out
						listResources();
					}
				});
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
			if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
				System.err.println("Failed to connect to " + strUrl);
				executor.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	public void listResources() {
		Map<String, Object> args = new HashMap<String, Object>();
		try {
			CyCommandResult result = CyCommandManager.execute("idmapping",
					"list selected resources", args);
			Set<String> mappers = (Set<String>) result.getResult();
			String db2ReList = "";
			for (String re : mappers) {
				if (re.startsWith("idmapper-bridgerest")) {
					String url = re.substring(re.indexOf("http"));
					dbConnection.setText("Web service: " + url);
					dbConnection.setToolTipText(re);
					dbConnection.setForeground(green);
					this.connState = re;
				} else if (re.startsWith("idmapper-pgdb")) {
					String filename = re.substring(re.indexOf(".") - 17);
					dbConnection.setText("file: " + filename);
					dbConnection.setToolTipText(re);
					dbConnection.setForeground(green);
					this.connState = re;
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
			// try web service
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

		// refresh list of selected resources
		listResources();

		// finally, update the species state for the panel
		this.speciesState = species;
	}

	private void updateResources() {
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
		JComboBox b = (JComboBox) e.getSource();
		String oldSpecies = this.speciesState;
		final String newSpecies = (String) b.getSelectedItem();
		CytoscapeInit.getProperties().setProperty("defaultSpeciesName",
				newSpecies);

		dbConnection.setText("connecting...");
		dbConnection.setToolTipText("one moment please");
		dbConnection.setForeground(blue);

		/*
		 * Fire prop change to trigger automatic resource management by
		 * CyThesaurus (i.e., default BridgeRest resources)
		 */
		if (autoRegister)
			Cytoscape.firePropertyChange(Cytoscape.PREFERENCE_MODIFIED,
					oldSpecies, newSpecies);

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
