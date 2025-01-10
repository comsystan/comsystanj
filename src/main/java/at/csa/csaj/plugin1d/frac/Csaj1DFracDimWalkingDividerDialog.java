/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DFracDimWalkingDividerDialog.java
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
public class Csaj1DFracDimWalkingDividerDialog extends CsajDialog_1DPluginWithRegression {

	private static final long serialVersionUID = -6314071389994619095L;

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
	private JPanel       panelScalingType;
	private ButtonGroup  buttonGroupScalingType;
    private JRadioButton radioButtonRadialDist;
    private JRadioButton radioButtonCoordinates;
	private String       choiceRadioButt_ScalingType;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DFracDimWalkingDividerDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Walking divider dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements  
		//*****************************************************************************************		
	    JLabel labelScalingType = new JLabel("Scaling type");
	    labelScalingType.setToolTipText("Radial distance scaling or coordinates (x,y) scaling");
	    labelScalingType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupScalingType = new ButtonGroup();
		radioButtonRadialDist  = new JRadioButton("Radial distance");
		radioButtonCoordinates = new JRadioButton("Coordinates");
		radioButtonRadialDist.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonRadialDist.isSelected()) {
					choiceRadioButt_ScalingType = radioButtonRadialDist.getText();
				} 
				logService.info(this.getClass().getName() + " Scaling type set to " + choiceRadioButt_ScalingType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonCoordinates.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonCoordinates.isSelected()) {
					choiceRadioButt_ScalingType = radioButtonCoordinates.getText();
				}
				logService.info(this.getClass().getName() + " Scaling type set to " + choiceRadioButt_ScalingType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupScalingType.add(radioButtonRadialDist);
		buttonGroupScalingType.add(radioButtonCoordinates);
		radioButtonRadialDist.setSelected(true);
		
		panelScalingType = new JPanel();
		panelScalingType.setToolTipText("Radial distance scaling or coordinates (x,y) scaling");
		panelScalingType.setLayout(new BoxLayout(panelScalingType, BoxLayout.Y_AXIS)); 
	    panelScalingType.add(radioButtonRadialDist);
	    panelScalingType.add(radioButtonCoordinates);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelScalingType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelScalingType, gbc);
	    //initialize command variable
		if (radioButtonRadialDist.isSelected())  choiceRadioButt_ScalingType = radioButtonRadialDist.getText();
		if (radioButtonCoordinates.isSelected()) choiceRadioButt_ScalingType = radioButtonCoordinates.getText();
		
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)	
		labelNumEps.setText("Number of rulers");
		
		labelNumEps.setToolTipText("Number of rulers following 2^i");
		spinnerNumEps.setToolTipText("Number of rulers following 2^i");
		
		int numEpsMax = Csaj1DFracDimWalkingDividerCmd.getMaxRulersNumber(tableIn);
		spinnerModelNumEps= new SpinnerNumberModel(numEpsMax, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
	
		spinnerModelNumRegStart = new SpinnerNumberModel(1, 1, numEpsMax - 1, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegStart.setModel(spinnerModelNumRegStart);
	
		spinnerModelNumRegEnd = new SpinnerNumberModel(numEpsMax, 3, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegEnd.setModel(spinnerModelNumRegEnd);
	
		spinnerInteger_NumEps      = (int)spinnerNumEps.getValue();
		spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();	
		spinnerInteger_NumRegEnd   = (int)spinnerNumRegEnd.getValue();	
			
		//Skip zeroes not allowed
		labelSkipZeroes.setEnabled(false);
		checkBoxSkipZeroes.setEnabled(false);
		
		//Column number spinner must also be changed
		labelNumColumn.setText("Column X number");
		int nColumns = tableIn.getColumnCount();
	    spinnerModelNumColumn = new SpinnerNumberModel(1, 1, nColumns/2, 2); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
        spinnerNumColumn.setModel(spinnerModelNumColumn);
 	
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj1DFracDimWalkingDividerCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
														
														"choiceRadioButt_ScalingType",   choiceRadioButt_ScalingType,
														
														"spinnerInteger_NumRulers",      spinnerInteger_NumEps,
														"spinnerInteger_NumRegStart",    spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",      spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",      booleanShowDoubleLogPlot,
														
														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
														"spinnerInteger_NumSurrogates",  spinnerInteger_NumSurrogates,
														"spinnerInteger_BoxLength",      spinnerInteger_BoxLength,
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
		tableOutName = Csaj1DFracDimWalkingDividerCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
