/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimPerimeterAreaDialog.java
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
import javax.swing.SpinnerNumberModel;

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
public class Csaj2DFracDimPerimeterAreaDialog extends CsajDialog_2DPluginWithRegression {

	private static final long serialVersionUID = 3168223874297613195L;

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
	private JPanel       panelScanningType;
	private ButtonGroup  buttonGroupScanningType;
    private JRadioButton radioButtonRasterBox;
    private JRadioButton radioButtonSlidingBox;
	private String       choiceRadioButt_ScanningType;
	
	private JPanel       panelColorModelType;
	private ButtonGroup  buttonGroupColorModelType;
    private JRadioButton radioButtonBinary;
    private JRadioButton radioButtonGrey;
	private String       choiceRadioButt_ColorModelType;
	
	
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
	public Csaj2DFracDimPerimeterAreaDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Perimeter area dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelScanningType = new JLabel("Scanning");
	    labelScanningType.setToolTipText("Type of box scanning");
	    labelScanningType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupScanningType = new ButtonGroup();
		radioButtonRasterBox    = new JRadioButton("Raster box");
		radioButtonSlidingBox   = new JRadioButton("Sliding box");
		radioButtonRasterBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonRasterBox.isSelected())  choiceRadioButt_ScanningType = radioButtonRasterBox.getText();
				logService.info(this.getClass().getName() + " Scanning type set to " + choiceRadioButt_ScanningType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonSlidingBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonSlidingBox.isSelected())  choiceRadioButt_ScanningType = radioButtonSlidingBox.getText();
				logService.info(this.getClass().getName() + " Scanning type set to " + choiceRadioButt_ScanningType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		buttonGroupScanningType.add(radioButtonRasterBox);
		buttonGroupScanningType.add(radioButtonSlidingBox);
		radioButtonRasterBox.setSelected(true);
		
		panelScanningType = new JPanel();
		panelScanningType.setToolTipText("Type of box scanning");
		panelScanningType.setLayout(new BoxLayout(panelScanningType, BoxLayout.Y_AXIS)); 
	    panelScanningType.add(radioButtonRasterBox);
	    //panelScanningType.add(radioButtonSlidingBox); //"Sliding box" not yet implemented
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelScanningType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelScanningType, gbc);
	    //initialize command variable
		if (radioButtonRasterBox.isSelected())  choiceRadioButt_ScanningType = radioButtonRasterBox.getText();
		if (radioButtonSlidingBox.isSelected()) choiceRadioButt_ScanningType = radioButtonSlidingBox.getText();
		
	    //*****************************************************************************************
	    JLabel labelColorModelType = new JLabel("Color model");
	    labelColorModelType.setToolTipText("Type of color model - binary or greyscale");
	    labelColorModelType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupColorModelType = new ButtonGroup();
		radioButtonBinary = new JRadioButton("Binary");
		radioButtonGrey   = new JRadioButton("Grey");
		radioButtonBinary.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonBinary.isSelected())  choiceRadioButt_ColorModelType = radioButtonBinary.getText();
				logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonGrey.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonGrey.isSelected())  choiceRadioButt_ColorModelType = radioButtonGrey.getText();
				logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		buttonGroupColorModelType.add(radioButtonBinary);
		buttonGroupColorModelType.add(radioButtonGrey);
		radioButtonBinary.setSelected(true);
		
		panelColorModelType = new JPanel();
		panelColorModelType.setToolTipText("Type of color model - binary or greyscale");
		panelColorModelType.setLayout(new BoxLayout(panelColorModelType, BoxLayout.Y_AXIS)); 
		
	    panelColorModelType.add(radioButtonBinary);
	    //panelColorModelType.add(radioButtonGrey); //"Grey" not yet implemented 
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelColorModelType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelColorModelType, gbc);
	    //initialize command variable
		if (radioButtonBinary.isSelected())  choiceRadioButt_ColorModelType = radioButtonBinary.getText();
		if (radioButtonGrey.isSelected())    choiceRadioButt_ColorModelType = radioButtonGrey.getText();
		
		//*****************************************************************************************
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Number of boxes");
		int numBoxes = Csaj2DFracDimPerimeterAreaCmd.getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
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
		 
		Future<CommandModule> future = commandService.run(Csaj2DFracDimPerimeterAreaCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
														"choiceRadioButt_ScanningType",   choiceRadioButt_ScanningType,
														"choiceRadioButt_ColorModelType", choiceRadioButt_ColorModelType,
					
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
		tableOutName = Csaj2DFracDimPerimeterAreaCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
