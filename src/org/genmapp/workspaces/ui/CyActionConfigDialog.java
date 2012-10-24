/*
 Copyright (c) 2006, 2007, The Cytoscape Consortium (www.cytoscape.org)

 The Cytoscape Consortium is:
 - Institute for Systems Biology
 - University of California San Diego
 - Memorial Sloan-Kettering Cancer Center
 - Institut Pasteur
 - Agilent Technologies

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.genmapp.workspaces.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.genmapp.workspaces.objects.CyAction;
import org.genmapp.workspaces.tree.ActionPanel;

/**
 * 
 * @author gjj
 */
public class CyActionConfigDialog extends JDialog {

	private static final long serialVersionUID = 5106703609005644456L;

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private JButton okBtn;
	private JButton loadBtn;
	private JButton saveBtn;
	private JButton cancelBtn;
	private JButton leftButton;
	private JButton rightButton;
	private JButton upButton;
	private JButton downButton;
	private JList selectedActionsList;
	private OrderedActionsListModel selectedActionsData = new OrderedActionsListModel();
	private JList availableActionsList;
	private SortedActionsListModel availableActionsData = new SortedActionsListModel();
	private JCheckBox workflowBox = new JCheckBox();
	private JLabel description;
	private final String DESC = "Description: ";
	private final String REQS = "Prerequisites: ";
	private JLabel requirements;

	/** Creates new CyActionConfigDialog */
	public CyActionConfigDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
		postInit();
	}

	public void postInit() {
		updateOKButtonEnable();

	}

	/**
	 * Initialize data models, lists and gui
	 */
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		// build models
		for (CyAction action : ActionPanel.availableActionsList) {
			availableActionsData.add(action);
		}
		for (CyAction action : ActionPanel.currentActionsList) {
			selectedActionsData.add(action);
		}

		// build lists
		availableActionsList = new JList(availableActionsData);
		selectedActionsList = new JList(selectedActionsData);

		// gui
		JPanel OKPanel = new JPanel();
		okBtn = new JButton();
		loadBtn = new JButton();
		saveBtn = new JButton();
		cancelBtn = new JButton();
		JPanel selectActionsPanel = new JPanel();
		JScrollPane availableActionsScrollPane = new JScrollPane();
		JScrollPane selectedActionsScrollPane = new JScrollPane();
		JPanel leftRightPanel = new JPanel();
		rightButton = new JButton();
		rightButton.setToolTipText("Add action to workflow");
		leftButton = new JButton();
		leftButton.setToolTipText("Remove action from workflow");
		JPanel upDownPanel = new JPanel();
		upButton = new JButton();
		upButton.setToolTipText("Move action up");
		downButton = new JButton();
		downButton.setToolTipText("Move action down");
		JPanel workflowPanel = new JPanel();
		description = new JLabel(" ");
		requirements = new JLabel(" ");

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Configure Action List");
		getContentPane().setLayout(new java.awt.GridBagLayout());

		okBtn.setText("   OK   ");
		okBtn.setEnabled(false);
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				OKBtnActionPerformed(evt);
			}
		});
		OKPanel.add(okBtn);

		loadBtn.setText("Load");
		loadBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				loadBtnActionPerformed(evt);
			}
		});
		OKPanel.add(loadBtn);

		saveBtn.setText("Save");
		saveBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				saveBtnActionPerformed(evt);
			}
		});
		OKPanel.add(saveBtn);

		cancelBtn.setText("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				cancelBtnActionPerformed(evt);
			}
		});
		OKPanel.add(cancelBtn);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		getContentPane().add(OKPanel, gridBagConstraints);

		selectActionsPanel.setBorder(BorderFactory.createTitledBorder("Select actions"));
		selectActionsPanel.setMinimumSize(new java.awt.Dimension(700, 350));
		selectActionsPanel.setPreferredSize(new java.awt.Dimension(700, 350));
		selectActionsPanel.setLayout(new java.awt.GridBagLayout());

		availableActionsScrollPane.setPreferredSize(new java.awt.Dimension(300, 150));

		availableActionsList.setBorder(BorderFactory.createTitledBorder("Availabe actions"));

		availableActionsList.setCellRenderer(new ListCellRenderer() {
			private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				renderer.setText(((CyAction) value).toString());
				return renderer;
			}
		});

		availableActionsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent evt) {
				int index = availableActionsList.getMinSelectionIndex();
				if (index > -1) {
					selectedActionsList.getSelectionModel().clearSelection();
					rightButton.setEnabled(true);
					CyAction action = availableActionsData.getElementAt(index);
					description.setText(DESC + action.getDescription());
					requirements.setText(REQS + action.getRequirements());
				} else {
					rightButton.setEnabled(false);
				}

			}
		});
		availableActionsScrollPane.setViewportView(availableActionsList);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
		selectActionsPanel.add(availableActionsScrollPane, gridBagConstraints);

		workflowPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		workflowBox.setText("Automatically step through actions");
		workflowBox.setSelected(ActionPanel.workflowState);
		workflowBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				checkboxActionPerformed(evt);
			}
		});
		workflowPanel.add(workflowBox);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 3);
		selectActionsPanel.add(workflowPanel, gridBagConstraints);

		leftRightPanel.setLayout(new java.awt.GridLayout(0, 1, 0, 2));

		rightButton.setIcon(new ImageIcon(getClass().getResource("../images/right.png")));
		rightButton.setEnabled(false);
		rightButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int[] indices = availableActionsList.getSelectedIndices();
				if (indices == null)
					return;
				if (indices.length == 0)
					return;

				for (int i = indices.length - 1; i >= 0; i--) {
					CyAction selected = availableActionsData.getElementAt(indices[i]);
					selectedActionsData.add(selected);
				}

				if (availableActionsData.getSize() == 0) {
					availableActionsList.clearSelection();
					rightButton.setEnabled(false);
				} else {
					int minindex = availableActionsList.getMinSelectionIndex();
					if (minindex >= availableActionsData.getSize()) {
						minindex = 0;
					}
					availableActionsList.setSelectedIndex(minindex);
				}

				selectedActionsList.repaint();
				availableActionsList.repaint();

				updateOKButtonEnable();
			}
		});
		leftRightPanel.add(rightButton);

		leftButton.setIcon(new ImageIcon(getClass().getResource("../images/left.png")));
		leftButton.setEnabled(false);
		leftButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int[] indices = selectedActionsList.getSelectedIndices();
				if (indices == null)
					return;
				if (indices.length == 0)
					return;

				for (int i = indices.length - 1; i >= 0; i--) {
					CyAction removed = selectedActionsData.removeElement(indices[i]);
					availableActionsData.add(removed);
				}

				if (selectedActionsData.getSize() == 0) {
					selectedActionsList.clearSelection();
					leftButton.setEnabled(false);
				} else {
					int minindex = selectedActionsList.getMinSelectionIndex();
					if (minindex >= selectedActionsData.getSize()) {
						minindex = 0;
					}
					selectedActionsList.setSelectedIndex(minindex);
				}

				selectedActionsList.repaint();
				availableActionsList.repaint();
				updateOKButtonEnable();

			}
		});
		leftRightPanel.add(leftButton);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
		selectActionsPanel.add(leftRightPanel, gridBagConstraints);

		// Up-Down Panel
		upDownPanel.setLayout(new java.awt.GridLayout(0, 1, 0, 2));

		upButton.setIcon(new ImageIcon(getClass().getResource("../images/up.png")));
		upButton.setEnabled(false);
		upButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int index = selectedActionsList.getSelectedIndex();
				if (index != 0) {
					swap(index, index - 1);
					selectedActionsList.setSelectedIndex(index - 1);
					selectedActionsList.ensureIndexIsVisible(index - 1);
				}
				selectedActionsList.repaint();
			}
		});
		upDownPanel.add(upButton);

		downButton.setIcon(new ImageIcon(getClass().getResource("../images/down.png")));
		downButton.setEnabled(false);
		downButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int index = selectedActionsList.getSelectedIndex();
				if (index != selectedActionsData.getSize() - 1) {
					swap(index, index + 1);
					selectedActionsList.setSelectedIndex(index + 1);
					selectedActionsList.ensureIndexIsVisible(index + 1);
				}
				selectedActionsList.repaint();
			}
		});
		upDownPanel.add(downButton);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
		selectActionsPanel.add(upDownPanel, gridBagConstraints);

		selectedActionsScrollPane.setPreferredSize(new java.awt.Dimension(300, 150));

		selectedActionsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		selectedActionsList.setBorder(BorderFactory.createTitledBorder("Selected actions"));

		selectedActionsList.setCellRenderer(new ListCellRenderer() {
			private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				renderer.setText(((CyAction) value).toString());
				return renderer;
			}
		});
		selectedActionsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent evt) {
				int index = selectedActionsList.getMinSelectionIndex();
				if (index > -1) {
					availableActionsList.getSelectionModel().clearSelection();
					leftButton.setEnabled(true);
					CyAction action = selectedActionsData.getElementAt(index);
					description.setText(DESC + action.getDescription());
					requirements.setText(REQS + action.getRequirements());
				} else {
					leftButton.setEnabled(false);
				}
				int[] indices = selectedActionsList.getSelectedIndices();
				if (indices != null) {
					if (indices.length == 1) {
						if (indices[0] != 0) {
							upButton.setEnabled(true);
						} else {
							upButton.setEnabled(false);
						}
						if (indices[0] != selectedActionsData.getSize() - 1) {
							downButton.setEnabled(true);
						} else {
							downButton.setEnabled(false);
						}
					} else {
						upButton.setEnabled(false);
						downButton.setEnabled(false);
					}
				}
			}
		});
		selectedActionsScrollPane.setViewportView(selectedActionsList);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
		selectActionsPanel.add(selectedActionsScrollPane, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 0, 0);
		selectActionsPanel.add(description, gridBagConstraints);
		gridBagConstraints.gridy = 3;
		selectActionsPanel.add(requirements, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		getContentPane().add(selectActionsPanel, gridBagConstraints);

		pack();
	}

	// Swap two elements in the list.
	private void swap(int a, int b) {
		CyAction aAction = selectedActionsData.getElementAt(a);
		CyAction bAction = selectedActionsData.getElementAt(b);
		selectedActionsData.setElementAt(a, bAction);
		selectedActionsData.setElementAt(b, aAction);
	}

	private void checkboxActionPerformed(ActionEvent evt) {
		// toggle values
		ActionPanel.workflowState = !ActionPanel.workflowState;
		workflowBox.setSelected(ActionPanel.workflowState);
	}

	private void cancelBtnActionPerformed(ActionEvent evt) {
		setVisible(false);
		dispose();
	}

	private void loadBtnActionPerformed(ActionEvent evt) {
		// TODO
	}

	private void saveBtnActionPerformed(ActionEvent evt) {
		// TODO
	}

	private void OKBtnActionPerformed(ActionEvent evt) {

		CyAction actions[] = selectedActionsData.getActions();
		ActionPanel.loadActions(actions);

		setVisible(false);
		dispose();
	}

	private void updateOKButtonEnable() {
		if (selectedActionsData.getSize() == 0) {
			okBtn.setEnabled(false);
			okBtn.setToolTipText("None of the actions were selected!");
		}

		okBtn.setEnabled(true);
		okBtn.setToolTipText(null);

		okBtn.repaint();
	}

	private class SortedActionsListModel extends AbstractListModel {
		TreeMap<String, CyAction> model;

		public SortedActionsListModel() {
			model = new TreeMap<String, CyAction>();
		}

		// @Override
		public int getSize() {
			return model.size();
		}

		// @Override
		public CyAction getElementAt(int index) {
			return (CyAction) model.values().toArray()[index];
		}

		public void add(CyAction action) {
			String title = action.toString();
			model.put(title, action);
			fireContentsChanged(this, 0, getSize());
		}

		public Collection<CyAction> getActions() {
			return model.values();
		}
	}

	private class OrderedActionsListModel extends AbstractListModel {
		List<CyAction> model;

		public OrderedActionsListModel() {
			model = new ArrayList<CyAction>();
		}

		public int getSize() {
			return model.size();
		}

		public CyAction getElementAt(int index) {
			return (CyAction) model.get(index);
		}

		public void setElementAt(int index, CyAction action) {
			model.set(index, action);
			fireContentsChanged(this, 0, getSize());
		}

		public void add(CyAction action) {
			model.add(action);
			fireContentsChanged(this, 0, getSize());
		}

		public CyAction removeElement(int index) {
			CyAction removed = model.remove(index);
			if (removed != null) {
				fireContentsChanged(this, 0, getSize());
			}
			return removed;
		}

		public CyAction[] getActions() {
			CyAction actions[] = new CyAction[getSize()];
			for (int i = 0; i < getSize(); i++) {
				actions[i] = model.get(i);
			}

			return actions;
		}
	}

}
