/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complexity analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DFisherInformationDialog.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2026 Comsystan Software
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;
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
public class Csaj1DFisherInformationDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 7181494182489627193L;

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
 	private JComboBox<String> comboBoxProbabilityType;
	private String            choiceRadioButt_ProbabilityType;
	
	private JLabel   labelLag;
	private JSpinner spinnerLag;
	private int      spinnerInteger_Lag;
		
	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DFisherInformationDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Fisher information  measure");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelProbabilityType = new JLabel("Probability type");
	    labelProbabilityType.setToolTipText("Selection of probability type");
	    labelProbabilityType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Sequence values", "Pairwise differences", "Sum of differences", "SD"}; 
		comboBoxProbabilityType = new JComboBox<String>(options);
		comboBoxProbabilityType.setToolTipText("Selection of probability type");
	    comboBoxProbabilityType.setEditable(false);
	    comboBoxProbabilityType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_ProbabilityType = (String)comboBoxProbabilityType.getSelectedItem();
				logService.info(this.getClass().getName() + " Probability type set to " + choiceRadioButt_ProbabilityType);
				//Lag must always be 1 for Sequence values
				if (choiceRadioButt_ProbabilityType.equals("Sequence values")) {
					labelLag.setEnabled(false);
					spinnerLag.setEnabled(false);	
					spinnerLag.setValue(1);
				}
				else {
					labelLag.setEnabled(true);
					spinnerLag.setEnabled(true);	
				}
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelProbabilityType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxProbabilityType, gbc);
	    //initialize command variable
	    choiceRadioButt_ProbabilityType = (String)comboBoxProbabilityType.getSelectedItem();
	    
	    //*****************************************************************************************
	    labelLag = new JLabel("Lag");
	    labelLag.setToolTipText("Delta (difference) between two data points");
	    labelLag.setHorizontalAlignment(JLabel.RIGHT);
	    labelLag.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelLag = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerLag = new JSpinner(spinnerModelLag);
        spinnerLag.setToolTipText("Delta (difference) between two data points");
        spinnerLag.setEnabled(false);
        spinnerLag.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Lag = (int)spinnerLag.getValue();
 
                if ((spinnerInteger_Lag > 1) && (String)comboBoxProbabilityType.getSelectedItem() == "Sequence values") {
        			spinnerLag.setValue(1);
                	logService.info(this.getClass().getName() + " Lag > 1 not possible for Sequence values");
                }
                
            	spinnerInteger_Lag = (int)spinnerLag.getValue();
                logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_Lag);
                if (booleanProcessImmediately) btnProcessSingleColumn .doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelLag, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerLag, gbc);	    
	    //initialize command variable
	    spinnerInteger_Lag = (int)spinnerLag.getValue();
	    
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)
	    //add a second listener
	    comboBoxSequenceRange.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(final ActionEvent arg0) {
 				choiceRadioButt_SequenceRange = (String)comboBoxSequenceRange.getSelectedItem();
 			}
 		});
	    
	    //add a second listener
	    comboBoxSurrogateType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_SurrogateType = (String)comboBoxSurrogateType.getSelectedItem();
				
				//Reset all spinners and options
			}
		});
	    
			
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj1DFisherInformationCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
															
														"choiceRadioButt_ProbabilityType",	choiceRadioButt_ProbabilityType,
														"spinnerInteger_Lag",				spinnerInteger_Lag,
																										
														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
														"spinnerInteger_NumSurrogates",  spinnerInteger_NumSurrogates,
														"spinnerInteger_BoxLength",      spinnerInteger_BoxLength,
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
		//tableOutName =(String)commandModule.getInfo().getLabel(); //Unfortunately, it is not possible to get this label inside the Command plugin class
		tableOutName = Csaj1DFisherInformationCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
