/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DNoiseDialog.java
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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
public class Csaj1DNoiseDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = -2392291181631163040L;

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
	private JLabel			  labelNoiseType;
	private JComboBox<String> comboBoxNoiseType;
	private String            choiceRadioButt_NoiseType;
	
	private JSpinner spinnerPercentage;
	private float    spinnerFloat_Percentage;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DNoiseDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Noise");

		//Add specific GUI elements according to Command @Parameter GUI elements   
		//*****************************************************************************************
	    labelNoiseType = new JLabel("Noise type");
	    labelNoiseType.setToolTipText("Noise type");
	    labelNoiseType.setEnabled(true);
	    labelNoiseType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsNoiseType[] = {"Shot", "Salt&Pepper", "Uniform", "Gaussian", "Rayleigh", "Exponential"};
		comboBoxNoiseType = new JComboBox<String>(optionsNoiseType);
		comboBoxNoiseType.setToolTipText("Noise type");
	    comboBoxNoiseType.setEnabled(true);
	    comboBoxNoiseType.setEditable(false);
	    comboBoxNoiseType.setSelectedItem("Gaussian");
	    comboBoxNoiseType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_NoiseType = (String)comboBoxNoiseType.getSelectedItem();
				logService.info(this.getClass().getName() + " Noise type set to " + choiceRadioButt_NoiseType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNoiseType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxNoiseType, gbc);
	    //initialize command variable
	    choiceRadioButt_NoiseType = (String)comboBoxNoiseType.getSelectedItem();
	       
	    //*****************************************************************************************
	    JLabel labelPercentage = new JLabel("Percentage(%) or scale");
	    labelPercentage.setToolTipText("Maximal percentage of affected data points or scaling parameter (e.g. sigma for Gaussian)");
	    labelPercentage.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelPercentage = new SpinnerNumberModel(10.0, 0.0, 999999999.0, 0.1); // initial, min, max, step
        spinnerPercentage = new JSpinner(spinnerModelPercentage);
        spinnerPercentage.setToolTipText("Maximal percentage of affected data points or scaling parameter (e.g. sigma for Gaussian)");
        spinnerPercentage.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_Percentage = (float)((SpinnerNumberModel)spinnerPercentage.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Percentage set to " + spinnerFloat_Percentage);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelPercentage, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerPercentage, gbc);	    
	    //initialize command variable
	    spinnerFloat_Percentage = (float)((SpinnerNumberModel)spinnerPercentage.getModel()).getNumber().doubleValue();
	
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
		 
		Future<CommandModule> future = commandService.run(Csaj1DNoiseCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_NoiseType",     choiceRadioButt_NoiseType,
														"spinnerFloat_Percentage",       spinnerFloat_Percentage,

														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
														//"spinnerInteger_NumSurrogates",  spinnerInteger_NumSurrogates,
														//"spinnerInteger_BoxLength",      spinnerInteger_BoxLength,
														//"booleanSkipZeroes",             booleanSkipZeroes,
														
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
		tableOutName = Csaj1DNoiseCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
