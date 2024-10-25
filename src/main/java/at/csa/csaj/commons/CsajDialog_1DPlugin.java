/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajDialog_1DPlugin.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 Comsystan Software
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package at.csa.csaj.commons;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;


/*
 * This is the super class for Csaj 2D dialogs
 */
public class CsajDialog_1DPlugin extends CsajDialog_PluginFrame {

	private static final long serialVersionUID = -4381466266001233934L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private ThreadService threadService;
	
	@Parameter
	private UIService uiService;
	
	//Input table variables
	public static DefaultGenericTable tableIn;
	public static String   tableInName;
	public static int      numColumns;
	public static int      numRows;
	public static String[] columnLabels;
	 
	
	public JPanel contentPanel;
	
	public JPanel  panelInput;
	public JLabel  labelInput;
	public JButton btnShowInput;
  	
  	public JPanel buttonPanelProcess;
  	
	public boolean processAll;
	
	public JLabel            labelSequenceRange;
	public JComboBox<String> comboBoxSequenceRange;
	public String            choiceRadioButt_SequenceRange;
	
	public JLabel            labelSurrogateType;
	public JComboBox<String> comboBoxSurrogateType;
	public String            choiceRadioButt_SurrogateType;

	public JLabel             labelNumSurrogates;
	public SpinnerNumberModel spinnerModelNumSurrogates;
	public JSpinner           spinnerNumSurrogates;
	public int                spinnerInteger_NumSurrogates;
	
	public JLabel             labelBoxLength;
	public SpinnerNumberModel spinnerModelBoxLength;
	public JSpinner           spinnerBoxLength;
	public int                spinnerInteger_BoxLength;
	
	public JLabel    labelSkipZeroes;
	public JCheckBox checkBoxSkipZeroes;
	public boolean   booleanSkipZeroes;

	public JLabel    labelOverWriteDisplays;
	public JCheckBox checkBoxOverwriteDisplays;
	public boolean   booleanOverwriteDisplays;
	
	public JLabel     labelProcessImmediate;
	public JCheckBox  checkBoxProcessImmediately;
	public boolean	  booleanProcessImmediately;

	public JLabel             labelNumColumn;
	public SpinnerNumberModel spinnerModelNumColumn;
	public JSpinner           spinnerNumColumn;
	public int                spinnerInteger_NumColumn;
	
	public JButton btnProcessSingleColumn;
	public JButton btnProcessAllColumns;
	
	/**
	 * Create the dialog.
	 */
	public CsajDialog_1DPlugin(Context context, DefaultTableDisplay defaultTableDisplay) {
		
		super();
		
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		context.inject(this); //Important
			
		//Get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkTableIn(logService, defaultTableDisplay);
		tableIn =      (DefaultGenericTable)datasetInInfo.get("tableIn");
		tableInName =  (String)datasetInInfo.get("tableInName"); 
		numColumns  =  (int)datasetInInfo.get("numColumns");
		numRows =      (int)datasetInInfo.get("numRows");
		columnLabels = (String[])datasetInInfo.get("columnLabels");
					
		//NORTH item
		//*****************************************************************************************
		panelInput = new JPanel();
		panelInput.setLayout(new GridBagLayout());
		panelInput.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
	    gbc.anchor = GridBagConstraints.CENTER;
		getContentPane().add(panelInput, BorderLayout.NORTH);
		
		labelInput = new JLabel(tableInName);
		labelInput.setToolTipText("Name of input table");
		labelInput.setHorizontalAlignment(JLabel.RIGHT);
		labelInput.setToolTipText(tableInName);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0; //IMPORTANT //This now sets the weight for the whole column 0 in the panelInput, although gpc is reset
		gbc.anchor = GridBagConstraints.WEST; //left
		panelInput.add(labelInput, gbc);
		gbc.weightx = 0.0; //reset to default
		
		//Show input button--------------------------------------------------------
		btnShowInput = new JButton("Show input table");
		btnShowInput.setToolTipText("Show input table in an extra window");	
		btnShowInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				
				labelInput.setText(tableInName);
				labelInput.revalidate();
				labelInput.repaint();
				
				int numElements = numColumns*numRows;
				CsajPlot_SequenceFrame pdf = null;
				if (numColumns == 1) {
					boolean isLineVisible = true;
					String sequenceTitle = tableInName;
					String xLabel = "#";
					String yLabel = tableIn.getColumnHeader(0);
					String seriesLabel = null;
					
					if (numElements < 1000000) {
						pdf = new CsajPlot_SequenceFrame(tableIn, 0, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel);
						pdf.setVisible(true);
					} else {
						int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the sequences?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
						if (selectedOption == JOptionPane.YES_OPTION) {
							pdf = new CsajPlot_SequenceFrame(tableIn, 0, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel);
							pdf.setVisible(true);
						}
					}	
				}
				
				if (numColumns > 1) {
					int[] cols = new int[numColumns];
					boolean isLineVisible = true;
					String sequenceTitle = tableInName;
					String xLabel = "#";
					String yLabel = "Value";
					String[] seriesLabels = new String[numColumns];		
					for (int c = 0; c < numColumns; c++) {
						cols[c] = c;
						seriesLabels[c] = tableIn.getColumnHeader(c);				
					}
						
					if (numElements < 1000000) {
						pdf = new CsajPlot_SequenceFrame(tableIn, cols, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel, seriesLabels);
						pdf.setVisible(true);
					} else {
					int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the sequences?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
						if (selectedOption == JOptionPane.YES_OPTION) {
							pdf = new CsajPlot_SequenceFrame(tableIn, cols, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel, seriesLabels);
							pdf.setVisible(true);
						}	
					}
				}				
				//Show table after plot to set it as the active display
				//This is mandatory for launching a sequence processing plugin 
				uiService.show(tableInName, tableIn);			
			}
		});
		//gbc.insets = standardInsets;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST; //right
		panelInput.add(btnShowInput, gbc);
			
		//CENTER default items		
	    //*****************************************************************************************
		//Specific items are declared in the sub class
		
		contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		contentPanel.setLayout(new GridBagLayout());
		//contentPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
	
		JScrollPane scrollPane = new JScrollPane(contentPanel);
	    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setToolTipText("Process options");
		separator.setName("Process options");
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy = 200;
		gbc.weightx = 1.0; //IMPORTANT //This now sets the weight for the whole column 1 in the contentPanel, although gpc is reset
		contentPanel.add(separator, gbc);
		gbc.weightx = 0.0; //reset to default
		gbc.gridwidth = 1; //reset to default
		
		//*****************************************************************************************
	    labelSequenceRange = new JLabel("Sequence range");
	    labelSequenceRange.setToolTipText("Range of sequence data values to be processed");
	    labelSequenceRange.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsSequenceRange[] = {"Entire sequence", "Subsequent boxes", "Gliding box"};
		comboBoxSequenceRange = new JComboBox<String>(optionsSequenceRange);
		comboBoxSequenceRange.setToolTipText("Range of sequence data values to be processed");
	    comboBoxSequenceRange.setEditable(false);
	    comboBoxSequenceRange.setSelectedItem("Entire sequence");
	    comboBoxSequenceRange.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_SequenceRange = (String)comboBoxSequenceRange.getSelectedItem();
				logService.info(this.getClass().getName() + " Sequence range set to " + choiceRadioButt_SequenceRange);
				
				//Reset all spinners and options		
				labelSurrogateType.setEnabled(false);
				comboBoxSurrogateType.setEnabled(false);	
				labelNumSurrogates.setEnabled(false);
				spinnerNumSurrogates.setEnabled(false);
				labelBoxLength.setEnabled(false);
				spinnerBoxLength.setEnabled(false);
				
				if (   choiceRadioButt_SequenceRange.equals("Entire sequence")
				    ) {		
					labelSurrogateType.setEnabled(true);
					comboBoxSurrogateType.setEnabled(true);	
					labelNumSurrogates.setEnabled(true);
					spinnerNumSurrogates.setEnabled(true);
					
					comboBoxSurrogateType.setSelectedItem("No surrogates");
					choiceRadioButt_SurrogateType = (String)comboBoxSurrogateType.getSelectedItem();
					spinnerNumSurrogates.setValue(0);
					spinnerInteger_NumSurrogates = (int)spinnerNumSurrogates.getValue();
				}
				if (   choiceRadioButt_SequenceRange.equals("Subsequent boxes")
					|| choiceRadioButt_SequenceRange.equals("Gliding box") 
					
					) {		
					comboBoxSurrogateType.setSelectedItem("No surrogates");
					spinnerNumSurrogates.setValue(0);
					spinnerInteger_NumSurrogates = (int)spinnerNumSurrogates.getValue();
					
					labelBoxLength.setEnabled(true);
					spinnerBoxLength.setEnabled(true);
				}
		
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 210;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSequenceRange, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 210;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxSequenceRange, gbc);
	    //initialize command variable
	    choiceRadioButt_SequenceRange = (String)comboBoxSequenceRange.getSelectedItem();
	    
		//*****************************************************************************************
	    labelSurrogateType = new JLabel("Surrogate type");
	    labelSurrogateType.setToolTipText("Type of surrogate");
	    labelSurrogateType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsSurrrogateType[] = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"};
		comboBoxSurrogateType = new JComboBox<String>(optionsSurrrogateType);
		comboBoxSurrogateType.setToolTipText("Type of surrogate");
	    comboBoxSurrogateType.setEditable(false);
	    comboBoxSurrogateType.setSelectedItem("No surrogates");
	    comboBoxSurrogateType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_SurrogateType = (String)comboBoxSurrogateType.getSelectedItem();
				logService.info(this.getClass().getName() + " Surrogate type set to " + choiceRadioButt_SurrogateType);
				
				//Reset all spinners and options
				labelNumSurrogates.setEnabled(false);
				spinnerNumSurrogates.setEnabled(false);							
				if (   choiceRadioButt_SurrogateType.equals("No surrogates")
				    ) {		
					spinnerNumSurrogates.setValue(0);
					spinnerInteger_NumSurrogates = (int)spinnerNumSurrogates.getValue();
				}
				if (   choiceRadioButt_SurrogateType.equals("Shuffle")
					|| choiceRadioButt_SurrogateType.equals("Gaussian") 
					|| choiceRadioButt_SurrogateType.equals("Random phase") 
					|| choiceRadioButt_SurrogateType.equals("AAFT") 			
					) {
					spinnerNumSurrogates.setValue(10);
					spinnerInteger_NumSurrogates = (int)spinnerNumSurrogates.getValue();
					labelNumSurrogates.setEnabled(true);
					spinnerNumSurrogates.setEnabled(true);
				}
				
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 220;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSurrogateType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 220;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxSurrogateType, gbc);
	    //initialize command variable
	    choiceRadioButt_SurrogateType = (String)comboBoxSurrogateType.getSelectedItem();
	    
	    //*****************************************************************************************
	    labelNumSurrogates = new JLabel("Number of surrogates");
	    labelNumSurrogates.setToolTipText("Number of generated surrogates");
	    labelNumSurrogates.setEnabled(false);
	    labelNumSurrogates.setHorizontalAlignment(JLabel.RIGHT);
	  
	    spinnerModelNumSurrogates = new SpinnerNumberModel(0, 0, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
        spinnerNumSurrogates = new JSpinner(spinnerModelNumSurrogates);
        spinnerNumSurrogates.setToolTipText("Number of generated surrogates");
        spinnerNumSurrogates.setEnabled(false);
        spinnerNumSurrogates.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumSurrogates = (int)spinnerNumSurrogates.getValue();
                logService.info(this.getClass().getName() + " Number of generated surrogates set to " + spinnerInteger_NumSurrogates);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 230;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumSurrogates, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 230;   
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumSurrogates, gbc);	
	  
	    //initialize command variable
	    spinnerInteger_NumSurrogates = (int)spinnerNumSurrogates.getValue();
	    
	    //*****************************************************************************************
	    labelBoxLength = new JLabel("Box length");
	    labelBoxLength.setToolTipText("Length of subsequent or gliding boxes");
	    labelBoxLength.setEnabled(false);
	    labelBoxLength.setHorizontalAlignment(JLabel.RIGHT);
	  
	    spinnerModelBoxLength = new SpinnerNumberModel(100, 2, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
        spinnerBoxLength = new JSpinner(spinnerModelBoxLength);
        spinnerBoxLength.setToolTipText("Length of subsequent or gliding boxes");
        spinnerBoxLength.setEnabled(false);
        spinnerBoxLength.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_BoxLength = (int)spinnerBoxLength.getValue();
                logService.info(this.getClass().getName() + " Box length set to " + spinnerInteger_BoxLength);           
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 240;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelBoxLength, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 240;   
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerBoxLength, gbc);	
	  
	    //initialize command variable
	    spinnerInteger_BoxLength = (int)spinnerBoxLength.getValue();
	    
	    //*****************************************************************************************
	    labelSkipZeroes = new JLabel("Skip zero values");
	    labelSkipZeroes.setToolTipText("Delete zeroes or not");
	    labelSkipZeroes.setHorizontalAlignment(JLabel.RIGHT);
	    
		checkBoxSkipZeroes = new JCheckBox();
		checkBoxSkipZeroes.setToolTipText("Delete zeroes or not");
		checkBoxSkipZeroes.setSelected(false);
		checkBoxSkipZeroes.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanSkipZeroes = checkBoxSkipZeroes.isSelected();
		    	logService.info(this.getClass().getName() + " Skip zeroes option set to " + booleanSkipZeroes);
		    	if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 250;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSkipZeroes, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 250;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxSkipZeroes, gbc);	
	    //initialize command variable
	    booleanSkipZeroes = checkBoxSkipZeroes.isSelected();
		
		//*****************************************************************************************
		labelOverWriteDisplays = new JLabel("Overwrite result display(s)");
		labelOverWriteDisplays.setToolTipText("Overwrite already existing result images, plots or tables");
		labelOverWriteDisplays.setHorizontalAlignment(JLabel.RIGHT);
		
		checkBoxOverwriteDisplays = new JCheckBox();
		checkBoxOverwriteDisplays.setToolTipText("Overwrite already existing result images, plots or tables");
		checkBoxOverwriteDisplays.setSelected(true);
		checkBoxOverwriteDisplays.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanOverwriteDisplays = checkBoxOverwriteDisplays.isSelected();	    
				logService.info(this.getClass().getName() + " Overwrite display(s) set to " + booleanOverwriteDisplays);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 260;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOverWriteDisplays, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 260;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxOverwriteDisplays, gbc);	
	 
	    //initialize command variable
	    booleanOverwriteDisplays = checkBoxOverwriteDisplays.isSelected();	 
	    
	    //*****************************************************************************************
	    labelProcessImmediate = new JLabel("Immediate processing");
	    labelProcessImmediate.setToolTipText("Immediate processing of active table whenever a parameter is changed");
	    labelProcessImmediate.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxProcessImmediately = new JCheckBox();
		checkBoxProcessImmediately.setToolTipText("Immediate processing of active table whenever a parameter is changed");
		checkBoxProcessImmediately.setSelected(false);
		checkBoxProcessImmediately.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanProcessImmediately = checkBoxProcessImmediately.isSelected();	    
				logService.info(this.getClass().getName() + " Immediate processing set to " + booleanProcessImmediately);	
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 270;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelProcessImmediate, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 270;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxProcessImmediately, gbc);	
	
	    //initialize command variable
	    booleanProcessImmediately = checkBoxProcessImmediately.isSelected();	 
	    
	    //*****************************************************************************************
	    labelNumColumn = new JLabel("Column number");
	    labelNumColumn.setToolTipText("Table column number");
	    labelNumColumn.setHorizontalAlignment(JLabel.RIGHT);
	  
	    spinnerModelNumColumn = new SpinnerNumberModel(1, 1, numColumns, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
        spinnerNumColumn = new JSpinner(spinnerModelNumColumn);
        spinnerNumColumn.setToolTipText("Table column number");
        spinnerNumColumn.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumColumn = (int)spinnerNumColumn.getValue();
                logService.info(this.getClass().getName() + " Table column number set to " + spinnerInteger_NumColumn);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 280;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumColumn, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 280;   
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumColumn, gbc);	
	  
	    //initialize command variable
	    spinnerInteger_NumColumn = (int)spinnerNumColumn.getValue();
		
	    //SOUTH Process buttons panel 
	    //*****************************************************************************************
	    buttonPanelProcess = new JPanel();
		buttonPanelProcess.setLayout(new GridBagLayout());
		buttonPanelProcess.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
		getContentPane().add(buttonPanelProcess, BorderLayout.SOUTH);
	    
		//Process single button--------------------------------------------------------
		btnProcessSingleColumn = new JButton("Process single column");
		btnProcessSingleColumn.setToolTipText("Process single column");
		btnProcessSingleColumn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				processAll = false;
				threadService.run(() -> processCommand()); //IMPORTANT //Without thread a deadlock will occur at future.get() //Probably because it runs on the EDT	
			}
		});
		//gbc.insets = standardInsets;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    buttonPanelProcess.add(btnProcessSingleColumn, gbc);	
	    
		//Process all button----------------------------------------------------------
		btnProcessAllColumns = new JButton("Pocess all columns");
		btnProcessAllColumns.setToolTipText("Process all available columns");
		btnProcessAllColumns.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				processAll = true;
				threadService.run(() -> processCommand()); //IMPORTANT //Without thread a deadlock will occur at future.get() //Probably because it runs on the EDT		
			}
		});
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    buttonPanelProcess.add(btnProcessAllColumns, gbc);	
	 
	    //*****************************************************************************************
	}
	
	/**
	 * Process by calling a command
	 * Will be defined in the specific Csaj GUI
	 */
	public void processCommand() {

	}
}
