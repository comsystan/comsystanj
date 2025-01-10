/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DCutOutDialog.java
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
public class Csaj1DCutOutDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = -6772815614884724074L;

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
	private JSpinner spinnerRangeStart;
	private int spinnerInteger_RangeStart;

	private JSpinner spinnerRangeEnd;
	private int spinnerInteger_RangeEnd;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DCutOutDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Cut out");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelRangeStart = new JLabel("Range start index");
	    labelRangeStart.setToolTipText("Start index for cutting out");
	    labelRangeStart.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelRangeStart = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerRangeStart = new JSpinner(spinnerModelRangeStart);
        spinnerRangeStart.setToolTipText("Start index for cutting out");
        spinnerRangeStart.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	
        		spinnerInteger_RangeStart = (int)spinnerRangeStart.getValue();
        		spinnerInteger_RangeEnd   = (int)spinnerRangeEnd.getValue();
        		
        		if (spinnerInteger_RangeStart > spinnerInteger_RangeEnd) spinnerRangeStart.setValue(spinnerInteger_RangeEnd);
        		
        		spinnerInteger_RangeStart = (int)spinnerRangeStart.getValue();
        		spinnerInteger_RangeEnd   = (int)spinnerRangeEnd.getValue();
            	
            	
                logService.info(this.getClass().getName() + " Rabge start index set to " + spinnerInteger_RangeStart);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelRangeStart, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerRangeStart, gbc);	    
	    //initialize command variable
	    spinnerInteger_RangeStart = (int)spinnerRangeStart.getValue();  
	    
	    //*****************************************************************************************
	    JLabel labelRangeEnd = new JLabel("Range end index");
	    labelRangeEnd.setToolTipText("End index for cutting out");
	    labelRangeEnd.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelRangeEnd = new SpinnerNumberModel(10, 1, 999999999, 1); // initial, min, max, step
        spinnerRangeEnd = new JSpinner(spinnerModelRangeEnd);
        spinnerRangeEnd.setToolTipText("End index for cutting out");
        spinnerRangeEnd.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
        		spinnerInteger_RangeStart = (int)spinnerRangeStart.getValue();
        		spinnerInteger_RangeEnd   = (int)spinnerRangeEnd.getValue();
        		
        		if (spinnerInteger_RangeEnd < spinnerInteger_RangeStart) spinnerRangeEnd.setValue(spinnerInteger_RangeStart);
        		
        		spinnerInteger_RangeStart = (int)spinnerRangeStart.getValue();
        		spinnerInteger_RangeEnd   = (int)spinnerRangeEnd.getValue();
            
                logService.info(this.getClass().getName() + " Range end index set to " + spinnerInteger_RangeEnd);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelRangeEnd, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerRangeEnd, gbc);	    
	    //initialize command variable
	    spinnerInteger_RangeEnd = (int)spinnerRangeEnd.getValue();
      
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
		 
		Future<CommandModule> future = commandService.run(Csaj1DCutOutCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
															
														"spinnerInteger_RangeStart",	 spinnerInteger_RangeStart,
														"spinnerInteger_RangeEnd",		 spinnerInteger_RangeEnd,
																										
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
		tableOutName = Csaj1DCutOutCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
