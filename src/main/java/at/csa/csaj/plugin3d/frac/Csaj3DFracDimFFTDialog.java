/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DFracDimFFTDialog.java
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

package at.csa.csaj.plugin3d.frac;

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
import javax.swing.SpinnerNumberModel;

import net.imagej.Dataset;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.table.DefaultGenericTable;
import org.scijava.ui.UIService;

import at.csa.csaj.commons.CsajDialog_3DPluginWithRegression;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj3DFracDimFFTDialog extends CsajDialog_3DPluginWithRegression {

	private static final long serialVersionUID = -6131746356893565876L;

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
 	private JComboBox<String> comboBoxWindowingType;
	private String   choiceRadioButt_WindowingType;
	
	private JPanel       panelPowerSpecType;
	private ButtonGroup  buttonGroupPowerSpecType;
    private JRadioButton radioButtonCircularAverage;
    private JRadioButton radioButtonMeanOfLineScans;
    private JRadioButton radioButtonIntegralOfLineScans;
	private String       choiceRadioButt_PowerSpecType;
	
	
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
	public Csaj3DFracDimFFTDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("3D FFT dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelWindowingType = new JLabel("Windowing");
	    labelWindowingType.setToolTipText("Windowing type with increasing filter strength");
	    labelWindowingType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Rectangular", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"}; //In the order of increasing filter strength
		comboBoxWindowingType = new JComboBox<String>(options);
		comboBoxWindowingType.setToolTipText("Windowing type with increasing filter strength");
	    comboBoxWindowingType.setEditable(false);
	    comboBoxWindowingType.setSelectedItem("Hanning");
	    comboBoxWindowingType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_WindowingType = (String)comboBoxWindowingType.getSelectedItem();
				logService.info(this.getClass().getName() + " Windowing type set to " + choiceRadioButt_WindowingType);
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelWindowingType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxWindowingType, gbc);
	    //initialize command variable
	    choiceRadioButt_WindowingType = (String)comboBoxWindowingType.getSelectedItem();
		
	    //*****************************************************************************************
	    JLabel labelPowerSpecType = new JLabel("Power Spectrum");
	    labelPowerSpecType.setToolTipText("Type of power spectrum computation");
	    labelPowerSpecType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupPowerSpecType       = new ButtonGroup();
		radioButtonCircularAverage     = new JRadioButton("Spherical average");
		radioButtonMeanOfLineScans     = new JRadioButton("Mean of line scans");
		radioButtonIntegralOfLineScans = new JRadioButton("Integral of line scans");
		radioButtonCircularAverage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonCircularAverage.isSelected())  choiceRadioButt_PowerSpecType = radioButtonCircularAverage.getText();
				logService.info(this.getClass().getName() + " Power spectrum type set to " + choiceRadioButt_PowerSpecType);
				
				int numEpsMax = Csaj3DFracDimFFTCmd.getMaxK((int)width, (int)height, (int)depth, choiceRadioButt_PowerSpecType);
				spinnerModelNumEps= new SpinnerNumberModel(1, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
				spinnerNumEps.setModel(spinnerModelNumEps);
				spinnerNumEps.setValue(numEpsMax);
				spinnerNumRegEnd.setValue(numEpsMax);
				spinnerInteger_NumEps    = (int)spinnerNumEps.getValue();
				spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();	
				
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
		radioButtonMeanOfLineScans.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonMeanOfLineScans.isSelected())  choiceRadioButt_PowerSpecType = radioButtonMeanOfLineScans.getText();
				logService.info(this.getClass().getName() + " Power spectrum type set to " + choiceRadioButt_PowerSpecType);
				
				int numEpsMax = Csaj3DFracDimFFTCmd.getMaxK((int)width, (int)height, (int)depth, choiceRadioButt_PowerSpecType);
				spinnerModelNumEps= new SpinnerNumberModel(1, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
				spinnerNumEps.setModel(spinnerModelNumEps);
				spinnerNumEps.setValue(numEpsMax);
				spinnerNumRegEnd.setValue(numEpsMax);
				spinnerInteger_NumEps    = (int)spinnerNumEps.getValue();
				spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();	
				
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
		radioButtonIntegralOfLineScans.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonIntegralOfLineScans.isSelected())  choiceRadioButt_PowerSpecType = radioButtonIntegralOfLineScans.getText();
				logService.info(this.getClass().getName() + " Power spectrum type set to " + choiceRadioButt_PowerSpecType);
				
				int numEpsMax = Csaj3DFracDimFFTCmd.getMaxK((int)width, (int)height, (int)depth, choiceRadioButt_PowerSpecType);
				spinnerModelNumEps= new SpinnerNumberModel(1, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
				spinnerNumEps.setModel(spinnerModelNumEps);
				spinnerNumEps.setValue(numEpsMax);
				spinnerNumRegEnd.setValue(numEpsMax);
				spinnerInteger_NumEps    = (int)spinnerNumEps.getValue();
				spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();	
				
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
		buttonGroupPowerSpecType.add(radioButtonCircularAverage);
		buttonGroupPowerSpecType.add(radioButtonMeanOfLineScans);
		buttonGroupPowerSpecType.add(radioButtonIntegralOfLineScans);
		radioButtonCircularAverage.setSelected(true);
		
		panelPowerSpecType = new JPanel();
		panelPowerSpecType.setToolTipText("Type of power spectrum computation");
		panelPowerSpecType.setLayout(new BoxLayout(panelPowerSpecType, BoxLayout.Y_AXIS)); 
		
	    panelPowerSpecType.add(radioButtonCircularAverage);
	    panelPowerSpecType.add(radioButtonMeanOfLineScans); 
	    panelPowerSpecType.add(radioButtonIntegralOfLineScans); 
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelPowerSpecType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelPowerSpecType, gbc);
	    //initialize command variable
		if (radioButtonCircularAverage.isSelected())     choiceRadioButt_PowerSpecType = radioButtonCircularAverage.getText();
		if (radioButtonMeanOfLineScans.isSelected())     choiceRadioButt_PowerSpecType = radioButtonMeanOfLineScans.getText();
		if (radioButtonIntegralOfLineScans.isSelected()) choiceRadioButt_PowerSpecType = radioButtonIntegralOfLineScans.getText();
		
		//*****************************************************************************************
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Maximal k");
		int numEpsMax = Csaj3DFracDimFFTCmd.getMaxK((int)width, (int)height, (int)depth, choiceRadioButt_PowerSpecType);
		spinnerModelNumEps= new SpinnerNumberModel(1, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
		spinnerNumEps.setValue(numEpsMax);
		spinnerNumRegEnd.setValue(numEpsMax);
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
		 
		Future<CommandModule> future = commandService.run(Csaj3DFracDimFFTCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
	
														"choiceRadioButt_WindowingType",  choiceRadioButt_WindowingType,
														"choiceRadioButt_PowerSpecType",  choiceRadioButt_PowerSpecType,
					
														"spinnerInteger_MaxK",            spinnerInteger_NumEps, //WARNING: Exceptionally a different name
														"spinnerInteger_NumRegStart",     spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",       spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",       booleanShowDoubleLogPlot,
	
														"booleanOverwriteDisplays",       booleanOverwriteDisplays,
														"booleanProcessImmediately",	  booleanProcessImmediately
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
		tableOutName = Csaj3DFracDimFFTCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
