/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DFracDimRSEDialog.java
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
public class Csaj1DFracDimRSEDialog extends CsajDialog_1DPluginWithRegression {

	private static final long serialVersionUID = -9085833091279070194L;

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
	private JSpinner spinnerNumM;
	private int      spinnerInteger_NumM;

	private JSpinner spinnerFlatteningOrder;
	private int      spinnerInteger_FlatteningOrder;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DFracDimRSEDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D RSE dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements  
		  //*****************************************************************************************
	    JLabel labelNumM = new JLabel("M");
	    labelNumM.setToolTipText("Number of randomly chosen sub-sequences for each length (M=50 recommended)");
	    labelNumM.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelNumM = new SpinnerNumberModel(50, 1, 999999999, 1); // initial, min, max, step
        spinnerNumM = new JSpinner(spinnerModelNumM);
        spinnerNumM.setToolTipText("Number of randomly chosen sub-sequences for each length (M=50 recommended)");
        spinnerNumM.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumM = (int)spinnerNumM.getValue();
            	
                logService.info(this.getClass().getName() + " M set to " + spinnerInteger_NumM);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumM, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumM, gbc);	    
	    //initialize command variable
	    spinnerInteger_NumM = (int)spinnerNumM.getValue();  
	    
	    //*****************************************************************************************
	    JLabel labelFlatteningOrder = new JLabel("Polynomial order");
	    labelFlatteningOrder.setToolTipText("Order of polynomial flattening (1.. recommended, 0.. without flattening)");
	    labelFlatteningOrder.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelFlatteningOrder = new SpinnerNumberModel(1, 0, 999999999, 1); // initial, min, max, step
        spinnerFlatteningOrder = new JSpinner(spinnerModelFlatteningOrder);
        spinnerFlatteningOrder.setToolTipText("Order of polynomial flattening (1.. recommended, 0.. without flattening)");
        spinnerFlatteningOrder.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_FlatteningOrder = (int)spinnerFlatteningOrder.getValue();
            
                logService.info(this.getClass().getName() + " Polynomial order set to " + spinnerInteger_FlatteningOrder);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFlatteningOrder, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerFlatteningOrder, gbc);	    
	    //initialize command variable
	    spinnerInteger_FlatteningOrder = (int)spinnerFlatteningOrder.getValue();
		
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)	
		//Eps max not limited
		labelNumEps.setText("Maximum length");
		
		labelNumEps.setToolTipText("Maximum length of sub-sequences");
		spinnerNumEps.setToolTipText("Maximum length of sub-sequences");
		
		spinnerModelNumEps = new SpinnerNumberModel(8, 3, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
	
		spinnerModelNumRegStart = new SpinnerNumberModel(3, 2, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
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
		 
		Future<CommandModule> future = commandService.run(Csaj1DFracDimRSECmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
														
														"spinnerInteger_NumM",           spinnerInteger_NumM,
														"spinnerInteger_FlatteningOrder",spinnerInteger_FlatteningOrder,
														
														"spinnerInteger_LMax",           spinnerInteger_NumEps,
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
		tableOutName = Csaj1DFracDimRSECmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
