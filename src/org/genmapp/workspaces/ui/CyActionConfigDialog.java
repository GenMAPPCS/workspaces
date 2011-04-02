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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.genmapp.workspaces.objects.CyAction;
import org.genmapp.workspaces.tree.ActionPanel;

/**
 * 
 * @author gjj
 */
public class CyActionConfigDialog extends javax.swing.JDialog {

	private static final long serialVersionUID = 5106703609005644456L;
	
	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton okBtn;
	private javax.swing.JButton saveBtn;
	private javax.swing.JButton cancelBtn;
	private javax.swing.JButton leftButton;
	private javax.swing.JButton rightButton;
	private javax.swing.JList selectedActionsList;
	private OrderedActionsListModel selectedActionsData = new OrderedActionsListModel();
	private javax.swing.JList availableActionsList;
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
		javax.swing.JPanel OKPanel = new javax.swing.JPanel();
		okBtn = new javax.swing.JButton();
		saveBtn = new javax.swing.JButton();
		cancelBtn = new javax.swing.JButton();
		javax.swing.JPanel selectActionsPanel = new javax.swing.JPanel();
		javax.swing.JScrollPane availableActionsScrollPane = new javax.swing.JScrollPane();
		javax.swing.JScrollPane selectedActionsScrollPane = new javax.swing.JScrollPane();
		javax.swing.JPanel lrButtonPanel = new javax.swing.JPanel();
		rightButton = new javax.swing.JButton();
		leftButton = new javax.swing.JButton();
		JPanel workflowPanel = new JPanel();
		description = new JLabel(" ");
		requirements = new JLabel(" ");

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Configure Action List");
		getContentPane().setLayout(new java.awt.GridBagLayout());

		okBtn.setText("   OK   ");
		okBtn.setEnabled(false);
		okBtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				OKBtnActionPerformed(evt);
			}
		});
		OKPanel.add(okBtn);

		saveBtn.setText("Save");
		saveBtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				saveBtnActionPerformed(evt);
			}
		});
		OKPanel.add(saveBtn);

		cancelBtn.setText("Cancel");
		cancelBtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
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

		selectActionsPanel.setBorder(javax.swing.BorderFactory
				.createTitledBorder("Select actions"));
		selectActionsPanel.setMinimumSize(new java.awt.Dimension(700, 350));
		selectActionsPanel.setPreferredSize(new java.awt.Dimension(700, 350));
		selectActionsPanel.setLayout(new java.awt.GridBagLayout());

		availableActionsScrollPane.setPreferredSize(new java.awt.Dimension(300,
				150));

		availableActionsList.setBorder(javax.swing.BorderFactory
				.createTitledBorder("Availabe actions"));

		availableActionsList.setCellRenderer(new ListCellRenderer() {
			private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel renderer = (JLabel) defaultRenderer
						.getListCellRendererComponent(list, value, index,
								isSelected, cellHasFocus);
				renderer.setText(((CyAction) value).toString());
				return renderer;
			}
		});

		availableActionsList
				.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						int index = availableActionsList.getMinSelectionIndex();
						if (index > -1) {
							selectedActionsList.getSelectionModel()
									.clearSelection();
							rightButton.setEnabled(true);
							CyAction action = availableActionsData
									.getElementAt(index);
							description.setText(DESC + action.getDescription());
							requirements.setText(REQS
									+ action.getRequirements());
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
		workflowBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkboxActionPerformed(evt);
			}
		});
		workflowPanel.add(workflowBox);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 3);
		selectActionsPanel.add(workflowPanel, gridBagConstraints);

		lrButtonPanel.setLayout(new java.awt.GridLayout(0, 1, 0, 2));

		rightButton.setIcon(new javax.swing.ImageIcon(getClass().getResource(
				"../images/right.png")));
		rightButton.setEnabled(false);
		rightButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				int[] indices = availableActionsList.getSelectedIndices();
				if (indices == null || indices.length == 0) {
					return;
				}

				for (int i = indices.length - 1; i >= 0; i--) {
					CyAction selected = availableActionsData
							.getElementAt(indices[i]);
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
		lrButtonPanel.add(rightButton);

		leftButton.setIcon(new javax.swing.ImageIcon(getClass().getResource(
				"../images/left.png")));
		leftButton.setEnabled(false);
		leftButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				int[] indices = selectedActionsList.getSelectedIndices();
				if (indices == null || indices.length == 0) {
					return;
				}

				for (int i = indices.length - 1; i >= 0; i--) {
					CyAction removed = selectedActionsData
							.removeElement(indices[i]);
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
		lrButtonPanel.add(leftButton);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
		selectActionsPanel.add(lrButtonPanel, gridBagConstraints);

		selectedActionsScrollPane.setPreferredSize(new java.awt.Dimension(300,
				150));

		selectedActionsList
				.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		selectedActionsList.setBorder(javax.swing.BorderFactory
				.createTitledBorder("Selected actions"));

		selectedActionsList.setCellRenderer(new ListCellRenderer() {
			private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel renderer = (JLabel) defaultRenderer
						.getListCellRendererComponent(list, value, index,
								isSelected, cellHasFocus);
				renderer.setText(((CyAction) value).toString());
				return renderer;
			}
		});
		selectedActionsList
				.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						int index = selectedActionsList.getMinSelectionIndex();
						if (index > -1) {
							availableActionsList.getSelectionModel()
									.clearSelection();
							leftButton.setEnabled(true);
							CyAction action = availableActionsData
									.getElementAt(index);
							description.setText(DESC + action.getDescription());
							requirements.setText(REQS
									+ action.getRequirements());
						} else {
							leftButton.setEnabled(false);
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
	private void checkboxActionPerformed(java.awt.event.ActionEvent evt) {
		// toggle values
		ActionPanel.workflowState = !ActionPanel.workflowState;
		workflowBox.setSelected(ActionPanel.workflowState);
	}

	private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {
		setVisible(false);
		dispose();
	}

	private void saveBtnActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO
	}

	private void OKBtnActionPerformed(java.awt.event.ActionEvent evt) {

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
