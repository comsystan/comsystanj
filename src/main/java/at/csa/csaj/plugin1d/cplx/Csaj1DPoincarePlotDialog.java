/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DPoincarePlotDialog.java
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

package at.csa.csaj.plugin1d.cplx;

import java.awt.GridBagConstraints;
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
public class Csaj1DPoincarePlotDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 4144429509107284421L;

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
	private JLabel   labelNumLag;
	private JSpinner spinnerNumLag;
	private int      spinnerInteger_NumLag;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DPoincarePlotDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Poincare plot");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    labelNumLag = new JLabel("Lag");
	    labelNumLag.setToolTipText("Time lag/delay of phase space reconstruction");
	    labelNumLag.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelNumLag = new SpinnerNumberModel(1, 0, 999999999, 1); // initial, min, max, step
        spinnerNumLag = new JSpinner(spinnerModelNumLag);
        spinnerNumLag.setToolTipText("Time lag/delay of phase space reconstruction");
        spinnerNumLag.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumLag = (int)spinnerNumLag.getValue();
                logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_NumLag);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumLag, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumLag, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumLag = (int)spinnerNumLag.getValue();
	    
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
		Future<CommandModule> future = commandService.run(Csaj1DPoincarePlotCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"spinnerInteger_NumLag",         spinnerInteger_NumLag,
														
														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
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
		//No table
		//tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		//uiService.show(tableOutName, tableOut);
	}
}
