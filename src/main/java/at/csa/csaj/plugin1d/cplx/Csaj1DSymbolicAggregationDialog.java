/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DSymbolicAggregationDialog.java
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

package at.csa.csaj.plugin1d.cplx;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.ui.UIService;
import at.csa.csaj.commons.CsajDialog_1DPlugin;
import net.imagej.Dataset;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj1DSymbolicAggregationDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 1615176757250845200L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private DefaultTableDisplay defaultTableDisplay;
  	private String datasetOutName;
	private Dataset datasetOut;
   
	//Specific dialog items
	private JSpinner spinnerAggLength;
	private int      spinnerInteger_AggLength;
	
	private JSpinner spinnerAlphabetSize;
	private int      spinnerInteger_AlphabetSize;
	
	private JSpinner spinnerWordLength;
	private int      spinnerInteger_WordLength;
	
	private JSpinner spinnerSubWordLength;
	private int      spinnerInteger_SubWordLength;
	
	private JSpinner spinnerMag;
	private int      spinnerInteger_Mag;
	
	private JPanel       panelColorModelType;
	private ButtonGroup  buttonGroupColorModelType;
    private JRadioButton radioButtonGrey;
    private JRadioButton radioButtonRGB;
	private String       choiceRadioButt_ColorModelType;
	

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DSymbolicAggregationDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Symbolic Aggregation");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    JLabel labelAggLength = new JLabel("Aggregation length");
	    labelAggLength.setToolTipText("Aggregation length");
	    labelAggLength.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelAggLength = new SpinnerNumberModel(2, 2, 999999999, 1); // initial, min, max, step
        spinnerAggLength = new JSpinner(spinnerModelAggLength);
        spinnerAggLength.setToolTipText("Aggregation length");
        spinnerAggLength.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_AggLength = (int)spinnerAggLength.getValue();
                logService.info(this.getClass().getName() + " Aggregation length set to " + spinnerInteger_AggLength);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelAggLength, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerAggLength, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_AggLength = (int)spinnerAggLength.getValue();
	    
		//*****************************************************************************************
	    JLabel labelAlphabetSize = new JLabel("Alphabet size");
	    labelAlphabetSize.setToolTipText("Alphabet size");
	    labelAlphabetSize.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelAlphabetSize= new SpinnerNumberModel(4, 4, 4, 1); // initial, min, max, step
        spinnerAlphabetSize = new JSpinner(spinnerModelAlphabetSize);
        spinnerAlphabetSize.setToolTipText("Alphabet size");
        spinnerAlphabetSize.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_AlphabetSize = (int)spinnerAlphabetSize.getValue();
                logService.info(this.getClass().getName() + " Alphabet size set to " + spinnerInteger_AlphabetSize);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelAlphabetSize, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerAlphabetSize, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_AlphabetSize = (int)spinnerAlphabetSize.getValue();
	    
		//*****************************************************************************************
	    JLabel labelWordLength = new JLabel("Word length");
	    labelWordLength.setToolTipText("Word length");
	    labelWordLength.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelWordLength = new SpinnerNumberModel(4, 1, 999999999, 1); // initial, min, max, step
        spinnerWordLength = new JSpinner(spinnerModelWordLength);
        spinnerWordLength.setToolTipText("Word length");
        spinnerWordLength.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_WordLength = (int)spinnerWordLength.getValue();
                logService.info(this.getClass().getName() + " Word length set to " + spinnerInteger_WordLength);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelWordLength, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerWordLength, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_WordLength = (int)spinnerWordLength.getValue();
	    
		//*****************************************************************************************
	    JLabel labelSubWordLength = new JLabel("Subword length");
	    labelSubWordLength.setToolTipText("Subword length");
	    labelSubWordLength.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelSubWordLength = new SpinnerNumberModel(2, 1, 999999999, 1); // initial, min, max, step
        spinnerSubWordLength = new JSpinner(spinnerModelSubWordLength);
        spinnerSubWordLength.setToolTipText("Subword length");
        spinnerSubWordLength.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_SubWordLength = (int)spinnerSubWordLength.getValue();
                logService.info(this.getClass().getName() + " Subword length set to " + spinnerInteger_SubWordLength);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSubWordLength, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSubWordLength, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_SubWordLength = (int)spinnerSubWordLength.getValue();
	    
	    //*****************************************************************************************		
	    JLabel labelImageSize = new JLabel("100% image size:");
	    labelImageSize.setToolTipText("100% size of output image");
	    labelImageSize.setHorizontalAlignment(JLabel.RIGHT);
		
	    JLabel label400x400 = new JLabel("400x400");
	    label400x400.setToolTipText("100% size of output image");
	    label400x400.setHorizontalAlignment(JLabel.LEFT);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelImageSize, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(label400x400, gbc);
	    
		//*****************************************************************************************
	    JLabel labelMag = new JLabel("Magnification [%]");
	    labelMag.setToolTipText("Magnification of output image, 100% -> 400x400");
	    labelMag.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelMag = new SpinnerNumberModel(100, 1, 999999999, 1); // initial, min, max, step
        spinnerMag = new JSpinner(spinnerModelMag);
        spinnerMag.setToolTipText("Magnification of output image, 100% -> 400x400");
        spinnerMag.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Mag = (int)spinnerMag.getValue();
                logService.info(this.getClass().getName() + " Magnification set to " + spinnerInteger_Mag);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMag, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMag, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Mag = (int)spinnerMag.getValue();
	  
	    //*****************************************************************************************		
	    JLabel labelColorModelType = new JLabel("Color model");
	    labelColorModelType.setToolTipText("Color model of output image");
	    labelColorModelType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupColorModelType = new ButtonGroup();
		radioButtonGrey       = new JRadioButton("Grey-8bit");
		radioButtonRGB            = new JRadioButton("Color-RGB");
		radioButtonGrey.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonGrey.isSelected()) {
					choiceRadioButt_ColorModelType = radioButtonGrey.getText();
				} 
				logService.info(this.getClass().getName() + " Color model set to " + choiceRadioButt_ColorModelType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonRGB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonRGB.isSelected()) {
					choiceRadioButt_ColorModelType = radioButtonRGB.getText();
				}
				logService.info(this.getClass().getName() + " Color model set to " + choiceRadioButt_ColorModelType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupColorModelType.add(radioButtonGrey);
		buttonGroupColorModelType.add(radioButtonRGB);
		radioButtonGrey.setSelected(true);
		
		panelColorModelType = new JPanel();
		panelColorModelType.setToolTipText("Color model of output image");
		panelColorModelType.setLayout(new BoxLayout(panelColorModelType, BoxLayout.Y_AXIS)); 
	    panelColorModelType.add(radioButtonGrey);
	    panelColorModelType.add(radioButtonRGB);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelColorModelType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelColorModelType, gbc);
	    //initialize command variable
		if (radioButtonGrey.isSelected()) choiceRadioButt_ColorModelType = radioButtonGrey.getText();
		if (radioButtonRGB.isSelected())  choiceRadioButt_ColorModelType = radioButtonRGB.getText();
				    			
		//*****************************************************************************************
		//Change/Override items defined in the super class(es)
	    //Restricted options
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>)comboBoxSequenceRange.getModel();
		model.removeElement("Subsequent boxes");
		model.removeElement("Gliding box");	
		comboBoxSequenceRange.setSelectedItem("Entire sequence");
		choiceRadioButt_SequenceRange = (String)comboBoxSequenceRange.getSelectedItem();
	    
		labelNumSurrogates.setEnabled(false);
		labelNumSurrogates.setVisible(false);
	    spinnerNumSurrogates.setEnabled(false);
	    spinnerNumSurrogates.setVisible(false);

	    labelBoxLength.setEnabled(false);
	    labelBoxLength.setVisible(false);
        spinnerBoxLength.setEnabled(false);
        spinnerBoxLength.setVisible(false);  
		
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		//Following run initiates a "ProcessAllImages" 
		Future<CommandModule> future = commandService.run(Csaj1DSymbolicAggregationCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"spinnerInteger_AggLength",       spinnerInteger_AggLength,
														"spinnerInteger_AlphabetSize",    spinnerInteger_AlphabetSize,
														"spinnerInteger_WordLength",      spinnerInteger_WordLength,
														"spinnerInteger_SubWordLength",   spinnerInteger_SubWordLength,
														"spinnerInteger_Mag",             spinnerInteger_Mag,
														"choiceRadioButt_ColorModelType", choiceRadioButt_ColorModelType,
																	
														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
														//"spinnerInteger_NumSurrogates",  spinnerInteger_NumSurrogates,
														//"spinnerInteger_BoxLength",      spinnerInteger_BoxLength,
														"booleanSkipZeroes",             booleanSkipZeroes,
														
														"booleanOverwriteDisplays",      booleanOverwriteDisplays,
														"booleanProcessImmediately",	 booleanProcessImmediately,
														"spinnerInteger_NumColumn",      spinnerInteger_NumColumn
														);
		CommandModule commandModule = null;
		try {
			commandModule = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//datasetOutName =(String)commandModule.getInfo().getLabel(); //Unfortunately, it is not possible to get this label inside the Command plugin class
		datasetOutName = Csaj1DSymbolicAggregationCmd.DATASET_OUT_NAME;
		datasetOut     = (Dataset)commandModule.getOutput("datasetOut");	
		uiService.show(datasetOutName, datasetOut);
	}
}
