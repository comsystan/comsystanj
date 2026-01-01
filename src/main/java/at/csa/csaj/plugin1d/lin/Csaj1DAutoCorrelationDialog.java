/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DAutoCorrelationDialog.java
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

package at.csa.csaj.plugin1d.lin;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.DefaultComboBoxModel;
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
public class Csaj1DAutoCorrelationDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 3800563415064372266L;

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
	private JLabel			  labelAutoCorrelationMethod;
	private JComboBox<String> comboBoxAutoCorrelationMethod;
	private String            choiceRadioButt_AutoCorrelationMethod;
	
	private JLabel    labelLimitToMaxLag;
	private JCheckBox checkBoxLimitToMaxLag;
	private boolean   booleanLimitToMaxLag;
	
	private JLabel   labelNumMaxLag;
	private JSpinner spinnerNumMaxLag;
	private int      spinnerInteger_NumMaxLag;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DAutoCorrelationDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Autocorrelation");

		//Add specific GUI elements according to Command @Parameter GUI elements   
		//*****************************************************************************************
	    labelAutoCorrelationMethod = new JLabel("Autocorrelation method");
	    labelAutoCorrelationMethod.setToolTipText("Autocorrelation method");
	    labelAutoCorrelationMethod.setEnabled(true);
	    labelAutoCorrelationMethod.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsAutoCorrelationMethod[] = {"Large N", "Small N", "FFT"};
		comboBoxAutoCorrelationMethod = new JComboBox<String>(optionsAutoCorrelationMethod);
		comboBoxAutoCorrelationMethod.setToolTipText("Autocorrelation method");
	    comboBoxAutoCorrelationMethod.setEnabled(true);
	    comboBoxAutoCorrelationMethod.setEditable(false);
	    comboBoxAutoCorrelationMethod.setSelectedItem("Large N");
	    comboBoxAutoCorrelationMethod.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_AutoCorrelationMethod = (String)comboBoxAutoCorrelationMethod.getSelectedItem();
				logService.info(this.getClass().getName() + " Autocorrelation method set to " + choiceRadioButt_AutoCorrelationMethod);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelAutoCorrelationMethod, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxAutoCorrelationMethod, gbc);
	    //initialize command variable
	    choiceRadioButt_AutoCorrelationMethod = (String)comboBoxAutoCorrelationMethod.getSelectedItem();
		
	    //*****************************************************************************************
	    labelLimitToMaxLag = new JLabel("Limit to Max lag");
	    labelLimitToMaxLag.setToolTipText("Maximum lag for the autocorrelation output");
	    labelLimitToMaxLag.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxLimitToMaxLag = new JCheckBox();
		checkBoxLimitToMaxLag.setToolTipText("Maximum lag for the autocorrelation output");
		checkBoxLimitToMaxLag.setSelected(false);
		checkBoxLimitToMaxLag.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanLimitToMaxLag = checkBoxLimitToMaxLag.isSelected();	 
		    	
		    	if (booleanLimitToMaxLag) {
		    		labelNumMaxLag.setEnabled(true);
		    		spinnerNumMaxLag.setEnabled(true);
		    	} else {
		    		labelNumMaxLag.setEnabled(false);
		    		spinnerNumMaxLag.setEnabled(false);
		    	}
		    	
				logService.info(this.getClass().getName() + " Limit to Max Lag option set to " + booleanLimitToMaxLag);	
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelLimitToMaxLag, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxLimitToMaxLag, gbc);	
	
	    //initialize command variable
	    booleanLimitToMaxLag = checkBoxLimitToMaxLag.isSelected();	
	        
	    //*****************************************************************************************
	    labelNumMaxLag = new JLabel("Max lag");
	    labelNumMaxLag.setToolTipText("Maximum (time) lag");
	    labelNumMaxLag.setEnabled(false);
	    labelNumMaxLag.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelNumMaxLag = new SpinnerNumberModel(10, 1, 999999999, 1); // initial, min, max, step
        spinnerNumMaxLag = new JSpinner(spinnerModelNumMaxLag);
        spinnerNumMaxLag.setToolTipText("Maximum (time) lag");
        spinnerNumMaxLag.setEnabled(false);
        spinnerNumMaxLag.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
        		int numMaxLag = (int)spinnerNumMaxLag.getValue();
        		int numRows = tableIn.getRowCount();
        		if (numMaxLag >= numRows) {
        			spinnerNumMaxLag.setValue(numRows - 1);
        		};	
            	spinnerInteger_NumMaxLag = (int)spinnerNumMaxLag.getValue();     	 	
                logService.info(this.getClass().getName() + " Max lag set to " + spinnerInteger_NumMaxLag);
                if (booleanProcessImmediately) btnProcessSingleColumn .doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumMaxLag, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumMaxLag, gbc);	    
	    //initialize command variable
	    spinnerInteger_NumMaxLag = (int)spinnerNumMaxLag.getValue();
	          	
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
		 
		Future<CommandModule> future = commandService.run(Csaj1DAutoCorrelationCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_AutoCorrelationMethod", choiceRadioButt_AutoCorrelationMethod,
														"booleanLimitToMaxLag",			         booleanLimitToMaxLag,	
														"spinnerInteger_NumMaxLag",			     spinnerInteger_NumMaxLag,

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
		//tableOutName =(String)commandModule.getInfo().getLabel(); //Unfortunately, it is not possible to get this label inside the Command plugin class
		tableOutName = Csaj1DAutoCorrelationCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
