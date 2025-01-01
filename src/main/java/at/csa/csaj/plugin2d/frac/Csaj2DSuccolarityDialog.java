/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DSuccolarityDialog.java
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
import javax.swing.JComboBox;
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
public class Csaj2DSuccolarityDialog extends CsajDialog_2DPluginWithRegression {

	private static final long serialVersionUID = -3933833258124941836L;

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

	private JComboBox<String> comboBoxFloodingType;
	private String            choiceRadioButt_FloodingType;
	
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
	public Csaj2DSuccolarityDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Succolarities");

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
				if (radioButtonRasterBox.isSelected()) {
					choiceRadioButt_ScanningType = radioButtonRasterBox.getText();
				}	 
				logService.info(this.getClass().getName() + " Scanning type set to " + choiceRadioButt_ScanningType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonSlidingBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonSlidingBox.isSelected()) {
					choiceRadioButt_ScanningType = radioButtonSlidingBox.getText();
				}
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
	    panelScanningType.add(radioButtonSlidingBox);
	    		
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
	    JLabel labelFloodingType = new JLabel("Flooding");
	    labelFloodingType.setToolTipText("Type of flooding, e.g. Top to down or Left to right.... or mean of all 4 directions");
	    labelFloodingType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"T2D", "D2T", "L2R", "R2L", "Mean & Anisotropy"};
		comboBoxFloodingType = new JComboBox<String>(options);
		comboBoxFloodingType.setToolTipText("Type of flooding, e.g. Top to down or Left to right.... or mean of all 4 directions");
	    comboBoxFloodingType.setEditable(false);
	    comboBoxFloodingType.setSelectedItem("T2D");
	    comboBoxFloodingType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_FloodingType = (String)comboBoxFloodingType.getSelectedItem();
				logService.info(this.getClass().getName() + " Flooding type set to " + choiceRadioButt_FloodingType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFloodingType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxFloodingType, gbc);
	    //initialize command variable
	    choiceRadioButt_FloodingType = (String)comboBoxFloodingType.getSelectedItem();
		     
		//*****************************************************************************************
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Number of boxes");
		int numEpsMax = Csaj2DSuccolarityCmd.getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
		spinnerModelNumEps= new SpinnerNumberModel(1, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
		spinnerNumEps.setValue(numEpsMax);
		//spinnerNumRegEnd.setValue(numEpsMax);
		spinnerInteger_NumEps    = (int)spinnerNumEps.getValue();
		//spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();	
		
		//Succolarity does not need Reg start and end
		//Remove items
		contentPanel.remove(labelNumRegStart);
	    contentPanel.remove(spinnerNumRegStart);
	    contentPanel.remove(labelNumRegEnd);
	    contentPanel.remove(spinnerNumRegEnd);
		
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
		Future<CommandModule> future = commandService.run(Csaj2DSuccolarityCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
												
														"choiceRadioButt_ScanningType",   choiceRadioButt_ScanningType,
														"choiceRadioButt_FloodingType",   choiceRadioButt_FloodingType,
												
					
														"spinnerInteger_NumBoxes",        spinnerInteger_NumEps, //WARNING: Exceptionally a different name
														//"spinnerInteger_NumRegStart",     spinnerInteger_NumRegStart,
														//"spinnerInteger_NumRegEnd",       spinnerInteger_NumRegEnd,
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
		tableOutName = Csaj2DSuccolarityCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
