/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DResamplingDialog.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2025 Comsystan Software
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

package at.csa.csaj.plugin1d.preproc;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj1DResamplingDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = -5957538930247284973L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private DefaultTableDisplay defaultTableDisplay;
  	private String tableOutName;
	private DefaultGenericTable tableOut;
   
	//Specific dialog items
	private JPanel       panelResamplingType;
	private ButtonGroup  buttonGroupResamplingType;
    private JRadioButton radioButtonDown;
    private JRadioButton radioButtonUp;
	private String       choiceRadioButt_ResamplingType;
	
	private JSpinner spinnerNumFactor;
	private int      spinnerInteger_NumFactor;
	
	private JPanel       panelInterpolationType;
	private ButtonGroup  buttonGroupInterpolationType;
    private JRadioButton radioButtonNone;
    private JRadioButton radioButtonLinear;
	private String       choiceRadioButt_InterpolationType;


	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DResamplingDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Resampling");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelResamplingType = new JLabel("Resampling");
	    labelResamplingType.setToolTipText("Resampling type");
	    labelResamplingType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupResamplingType = new ButtonGroup();
		radioButtonDown  = new JRadioButton("Down-sampling");
		radioButtonUp    = new JRadioButton("Up-sampling");
		radioButtonDown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonDown.isSelected()) {
					choiceRadioButt_ResamplingType = radioButtonDown.getText();
				} 
				logService.info(this.getClass().getName() + " Resampling type set to " + choiceRadioButt_ResamplingType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonUp.isSelected()) {
					choiceRadioButt_ResamplingType = radioButtonUp.getText();
				}
				logService.info(this.getClass().getName() + " Resampling type set to " + choiceRadioButt_ResamplingType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupResamplingType.add(radioButtonDown);
		buttonGroupResamplingType.add(radioButtonUp);
		radioButtonDown.setSelected(true);
		
		panelResamplingType = new JPanel();
		panelResamplingType.setToolTipText("Resampling type");
		panelResamplingType.setLayout(new BoxLayout(panelResamplingType, BoxLayout.Y_AXIS)); 
	    panelResamplingType.add(radioButtonDown);
	    panelResamplingType.add(radioButtonUp);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelResamplingType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelResamplingType, gbc);
	    //initialize command variable
		if (radioButtonDown.isSelected()) choiceRadioButt_ResamplingType = radioButtonDown.getText();
		if (radioButtonUp.isSelected())   choiceRadioButt_ResamplingType = radioButtonUp.getText();
			
		//*****************************************************************************************
	    JLabel labelNumFactor = new JLabel("Factor");
	    labelNumFactor.setToolTipText("Resampling factor");
	    labelNumFactor.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelNumFactor = new SpinnerNumberModel(2, 1, 999999999, 1); // initial, min, max, step
        spinnerNumFactor = new JSpinner(spinnerModelNumFactor);
        spinnerNumFactor.setToolTipText("Resampling factor");
        spinnerNumFactor.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumFactor = (int)spinnerNumFactor.getValue();
    	
                logService.info(this.getClass().getName() + " Factor set to " + spinnerInteger_NumFactor);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumFactor, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumFactor, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumFactor = (int)spinnerNumFactor.getValue();
	    
	    //*****************************************************************************************		
	    JLabel labelInterpolationType = new JLabel("Interpolation");
	    labelInterpolationType.setToolTipText("Interpolation type");
	    labelInterpolationType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupInterpolationType = new ButtonGroup();
		radioButtonNone    = new JRadioButton("None");
		radioButtonLinear  = new JRadioButton("Linear");
		radioButtonNone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonNone.isSelected()) {
					choiceRadioButt_InterpolationType = radioButtonNone.getText();
				} 
				logService.info(this.getClass().getName() + " Interpolation type set to " + choiceRadioButt_InterpolationType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonLinear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonLinear.isSelected()) {
					choiceRadioButt_InterpolationType = radioButtonLinear.getText();
				}
				logService.info(this.getClass().getName() + " Interpolation type set to " + choiceRadioButt_InterpolationType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupInterpolationType.add(radioButtonNone);
		buttonGroupInterpolationType.add(radioButtonLinear);
		radioButtonLinear.setSelected(true);
		
		panelInterpolationType = new JPanel();
		panelInterpolationType.setToolTipText("Interpolation type");
		panelInterpolationType.setLayout(new BoxLayout(panelInterpolationType, BoxLayout.Y_AXIS)); 
	    panelInterpolationType.add(radioButtonNone);
	    panelInterpolationType.add(radioButtonLinear);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelInterpolationType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelInterpolationType, gbc);
	    //initialize command variable
		if (radioButtonNone.isSelected())   choiceRadioButt_InterpolationType = radioButtonNone.getText();
		if (radioButtonLinear.isSelected()) choiceRadioButt_InterpolationType = radioButtonLinear.getText();
			    
	    //*****************************************************************************************
  		//Change/Override items defined in the super class(es)
	    //Restricted options
	    labelSequenceRange.setEnabled(false);
	    labelSequenceRange.setVisible(false);
		comboBoxSequenceRange.setEnabled(false);
		comboBoxSequenceRange.setVisible(false);
			
		labelSurrogateType.setEnabled(false);
		labelSurrogateType.setVisible(false);
		comboBoxSurrogateType.setEnabled(false);
		comboBoxSurrogateType.setVisible(false);

		labelNumSurrogates.setEnabled(false);
		labelNumSurrogates.setVisible(false);
		spinnerNumSurrogates.setEnabled(false);
		spinnerNumSurrogates.setVisible(false);
	
		labelBoxLength.setEnabled(false);
		labelBoxLength.setVisible(false);
		spinnerBoxLength.setEnabled(false);
		spinnerBoxLength.setVisible(false);

		labelSkipZeroes.setEnabled(false);
		labelSkipZeroes.setVisible(false);
		checkBoxSkipZeroes.setEnabled(false);
		checkBoxSkipZeroes.setVisible(false);
   	
		    
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
		Future<CommandModule> future = commandService.run(Csaj1DResamplingCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_ResamplingType",    choiceRadioButt_ResamplingType,
														"spinnerInteger_NumFactor",          spinnerInteger_NumFactor,
														"choiceRadioButt_InterpolationType", choiceRadioButt_InterpolationType, 

//														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
//														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
//														"spinnerInteger_NumSurrogates",  spinnerInteger_NumSurrogates,
//														"spinnerInteger_BoxLength",      spinnerInteger_BoxLength,
//														"booleanSkipZeroes",             booleanSkipZeroes,
														
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
		//tableOutName =(String)commandModule.getInfo().getLabel(); //Unfortunately, it is not possible to get this label inside the Command plugin class
		tableOutName = Csaj1DResamplingCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
