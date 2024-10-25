/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DFracDimTugOfWarDialog.java
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

package at.csa.csaj.plugin1d.frac;

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
import at.csa.csaj.commons.CsajDialog_1DPluginWithRegression;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj1DFracDimTugOfWarDialog extends CsajDialog_1DPluginWithRegression {

	private static final long serialVersionUID = 5740955530697062094L;

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
	private JSpinner spinnerNumAcurracy;
	private int      spinnerInteger_NumAcurracy;

	private JSpinner spinnerNumConfidence;
	private int      spinnerInteger_NumConfidence;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DFracDimTugOfWarDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Tug of war dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements  
		  //*****************************************************************************************
	    JLabel labelNumAcurracy = new JLabel("Accuracy");
	    labelNumAcurracy.setToolTipText("Statistical accuracy (default=90)");
	    labelNumAcurracy.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelNumAcurracy = new SpinnerNumberModel(90, 1, 999999999, 1); // initial, min, max, step
        spinnerNumAcurracy = new JSpinner(spinnerModelNumAcurracy);
        spinnerNumAcurracy.setToolTipText("Statistical accuracy (default=90)");
        spinnerNumAcurracy.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumAcurracy = (int)spinnerNumAcurracy.getValue();
            	
                logService.info(this.getClass().getName() + " Accuracy set to " + spinnerInteger_NumAcurracy);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumAcurracy, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumAcurracy, gbc);	    
	    //initialize command variable
	    spinnerInteger_NumAcurracy = (int)spinnerNumAcurracy.getValue();  
	    
	    //*****************************************************************************************
	    JLabel labelNumConfidence = new JLabel("Confidence");
	    labelNumConfidence.setToolTipText("Statistical confidence (default=15)");
	    labelNumConfidence.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelNumConfidence = new SpinnerNumberModel(15, 1, 999999999, 1); // initial, min, max, step
        spinnerNumConfidence = new JSpinner(spinnerModelNumConfidence);
        spinnerNumConfidence.setToolTipText("Statistical confidence (default=15)");
        spinnerNumConfidence.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumConfidence = (int)spinnerNumConfidence.getValue();
            
                logService.info(this.getClass().getName() + " Confidence set to " + spinnerInteger_NumConfidence);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumConfidence, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumConfidence, gbc);	    
	    //initialize command variable
	    spinnerInteger_NumConfidence = (int)spinnerNumConfidence.getValue();
		
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)	
		//Eps max not limited
		labelNumEps.setText("Number of boxes");
		
		labelNumEps.setToolTipText("Maximum number of boxes");
		spinnerNumEps.setToolTipText("Maximum number of boxes");
		
		spinnerModelNumEps = new SpinnerNumberModel(8, 3, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
	
		spinnerModelNumRegStart = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegStart.setModel(spinnerModelNumRegStart);
	
		spinnerModelNumRegEnd = new SpinnerNumberModel(8, 3, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegEnd.setModel(spinnerModelNumRegEnd);
	
		spinnerInteger_NumEps      = (int)spinnerNumEps.getValue();
		spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();	
		spinnerInteger_NumRegEnd   = (int)spinnerNumRegEnd.getValue();	
		
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
		Future<CommandModule> future = commandService.run(Csaj1DFracDimTugOfWarCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
														
														"spinnerInteger_NumAcurracy",    spinnerInteger_NumAcurracy,
														"spinnerInteger_NumConfidence",  spinnerInteger_NumConfidence,
														
														"spinnerInteger_NumBoxes",       spinnerInteger_NumEps,
														"spinnerInteger_NumRegStart",    spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",      spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",      booleanShowDoubleLogPlot,
														
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
		tableOutName = Csaj1DFracDimTugOfWarCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
