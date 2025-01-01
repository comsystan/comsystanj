/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimTugOfWarDialog.java
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

package at.csa.csaj.plugin2d.frac;

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

import net.imagej.Dataset;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.table.DefaultGenericTable;
import org.scijava.ui.UIService;

import at.csa.csaj.commons.CsajDialog_2DPluginWithRegression;
/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj2DFracDimTugOfWarDialog extends CsajDialog_2DPluginWithRegression {

	private static final long serialVersionUID = -5653778942913488303L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private Dataset datasetIn;
  	private String tableOutName;
	private DefaultGenericTable tableOut;
   
	//Specific dialog items
	public JSpinner spinnerNumAcurracy;
	public int      spinnerInteger_NumAcurracy;
	
	public JSpinner spinnerNumConfidence;
	public int      spinnerInteger_NumConfidence;
	
	
	/**Some default @Parameters are already defined in the super class
	 * public JCheckBox checkBoxOverwriteDisplays;
	 * public boolean   booleanOverwriteDisplays;
	 * 
	 * public JCheckBox checkBoxProcessImmediately;
	 * public boolean	booleanProcessImmediately;
	 * 
	 * public JSpinner spinnerNumImageSlice;
	 * public int      spinnerInteger_NumImageSlice;
	 * 
	 * public JButton btnProcessSingleImage;
	 * public JButton btnProcessAllImages;
	 */
	
		
	/**
	 * Create the dialog.
	 */
	public Csaj2DFracDimTugOfWarDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Tug of war dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelNumAcurracy = new JLabel("Accuracy");
	    labelNumAcurracy.setToolTipText("Accuracy (default=90)");
	    labelNumAcurracy.setHorizontalAlignment(JLabel.RIGHT);
	   
	    SpinnerNumberModel spinnerModelNumAcurracy= new SpinnerNumberModel(90, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
	    spinnerNumAcurracy = new JSpinner(spinnerModelNumAcurracy);
        spinnerNumAcurracy.setToolTipText("Accuracy (default=90)"); //s1=30 Wang paper
        spinnerNumAcurracy.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
    			spinnerInteger_NumAcurracy = (int)spinnerNumAcurracy.getValue();
    			logService.info(this.getClass().getName() + " Accuracy set to " + spinnerInteger_NumAcurracy);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
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
	    labelNumConfidence.setToolTipText("Confidence (default=15)");
	    labelNumConfidence.setHorizontalAlignment(JLabel.RIGHT);
	   
	    SpinnerNumberModel spinnerModelNumConfidence= new SpinnerNumberModel(15, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
	    spinnerNumConfidence = new JSpinner(spinnerModelNumConfidence);
        spinnerNumConfidence.setToolTipText("Confidence (default=15)"); //s2=5 Wang paper
        spinnerNumConfidence.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
    			spinnerInteger_NumConfidence = (int)spinnerNumConfidence.getValue();
    			logService.info(this.getClass().getName() + " Confidence set to " + spinnerInteger_NumConfidence);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
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
		labelNumEps.setText("Number of boxes");
		int numBoxes = Csaj2DFracDimTugOfWarCmd.getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
		spinnerModelNumEps= new SpinnerNumberModel(1, 1, numBoxes, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
		spinnerNumEps.setValue(numBoxes);
		spinnerNumRegEnd.setValue(numBoxes);
		spinnerInteger_NumEps    = (int)spinnerNumEps.getValue();
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
		Future<CommandModule> future = commandService.run(Csaj2DFracDimTugOfWarCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
													
														"spinnerInteger_NumAcurracy",     spinnerInteger_NumAcurracy,
														"spinnerInteger_NumConfidence",   spinnerInteger_NumConfidence,
					
														"spinnerInteger_NumBoxes",        spinnerInteger_NumEps, //WARNING: Exceptionally a different name
														"spinnerInteger_NumRegStart",     spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",       spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",       booleanShowDoubleLogPlot,
	
														"booleanOverwriteDisplays",       booleanOverwriteDisplays,
														"booleanProcessImmediately",	  booleanProcessImmediately,
														"spinnerInteger_NumImageSlice",	  spinnerInteger_NumImageSlice
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
		tableOutName = Csaj2DFracDimTugOfWarCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
