/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DAllomScaleDialog.java
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.SpinnerNumberModel;

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
public class Csaj1DAllomScaleDialog extends CsajDialog_1DPluginWithRegression {

	private static final long serialVersionUID = 4844876619343675457L;

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

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DAllomScaleDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Allometric Scaling");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Number of boxes");
		//int numEpsMax = Csaj1DAllomScale.getMaxBoxNumber((int)datasetIn.dimension(0), (int)datasetIn.dimension(1));
		int numEpsMax = 999999999; 
		spinnerModelNumEps = new SpinnerNumberModel(numEpsMax, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
		spinnerNumEps.setValue(numEpsMax);
		
		//spinnerIntefer_NumEps not needed for Allometric Scaling
		//NOTE: spinnerNumEps must still be set to a maximum value, so that spinnerNumRegEnd is not limited
		//Hide items
		labelNumEps.setEnabled(false);
		labelNumEps.setVisible(false);
		spinnerNumEps.setEnabled(false);
		spinnerNumEps.setVisible(false);
		
		spinnerNumRegEnd.setValue(3);
		//spinnerInteger_NumEps    = (int)spinnerNumEps.getValue();
		spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();	
		
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
		Future<CommandModule> future = commandService.run(Csaj1DAllomScaleCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
															
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
		tableOutName = Csaj1DAllomScaleCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
