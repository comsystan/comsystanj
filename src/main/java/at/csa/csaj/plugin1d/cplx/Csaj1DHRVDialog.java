/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DHRVDialog.java
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

package at.csa.csaj.plugin1d.cplx;

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
public class Csaj1DHRVDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 8584298203644147869L;

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
	private JPanel       panelTimeBase;
	private ButtonGroup  buttonGroupTimeBase;
    private JRadioButton radioButtonMsec;
    private JRadioButton radioButtonSec;
	private String       choiceRadioButt_TimeBase;
	
	private JComboBox<String> comboBoxWindowingType;
	private String            choiceRadioButt_WindowingType;
	
	private JLabel			  labelMeasurementType;
	private JComboBox<String> comboBoxMeasurementType;
	private String            choiceRadioButt_MeasurementType;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DHRVDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D HRV");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelTimeBase = new JLabel("Time base");
	    labelTimeBase.setToolTipText("Selection of time base in ms or sec");
	    labelTimeBase.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupTimeBase = new ButtonGroup();
		radioButtonMsec     = new JRadioButton("ms");
		radioButtonSec      = new JRadioButton("sec");
		radioButtonMsec.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonMsec.isSelected()) {
					choiceRadioButt_TimeBase = radioButtonMsec.getText();
				} 
				logService.info(this.getClass().getName() + " Time base set to " + choiceRadioButt_TimeBase);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonSec.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonSec.isSelected()) {
					choiceRadioButt_TimeBase = radioButtonSec.getText();
				}
				logService.info(this.getClass().getName() + " Time base set to " + choiceRadioButt_TimeBase);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupTimeBase.add(radioButtonMsec);
		buttonGroupTimeBase.add(radioButtonSec);
		radioButtonMsec.setSelected(true);
		
		panelTimeBase = new JPanel();
		panelTimeBase.setToolTipText("Selection of time base in ms or sec");
		panelTimeBase.setLayout(new BoxLayout(panelTimeBase, BoxLayout.Y_AXIS)); 
	    panelTimeBase.add(radioButtonMsec);
	    panelTimeBase.add(radioButtonSec);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelTimeBase, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelTimeBase, gbc);
	    //initialize command variable
		if (radioButtonMsec.isSelected())  choiceRadioButt_TimeBase = radioButtonMsec.getText();
		if (radioButtonSec.isSelected()) choiceRadioButt_TimeBase = radioButtonSec.getText();
			
	    //*****************************************************************************************
	    JLabel labelWindowingType = new JLabel("Windowing for PSD");
	    labelWindowingType.setToolTipText("Windowing type with increasing filter strength");
	    labelWindowingType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsWindowingType[] = {"Rectangular", "Cosine", "Lanczos", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"}; //In the order of increasing filter strength
		comboBoxWindowingType = new JComboBox<String>(optionsWindowingType);
		comboBoxWindowingType.setToolTipText("Windowing type with increasing filter strength");
	    comboBoxWindowingType.setEditable(false);
	    comboBoxWindowingType.setSelectedItem("Hanning");
	    comboBoxWindowingType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_WindowingType = (String)comboBoxWindowingType.getSelectedItem();
				logService.info(this.getClass().getName() + " Windowing type set to " + choiceRadioButt_WindowingType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelWindowingType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxWindowingType, gbc);
	    //initialize command variable
	    choiceRadioButt_WindowingType = (String)comboBoxWindowingType.getSelectedItem();
		
		//*****************************************************************************************
	    labelMeasurementType = new JLabel("Measurement type");
	    labelMeasurementType.setToolTipText("Measurement for Surrogates, Subsequent boxes or Gliding box");
	    labelMeasurementType.setEnabled(false);
	    labelMeasurementType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsMeasurementType[] = {"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", "NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"};
		comboBoxMeasurementType = new JComboBox<String>(optionsMeasurementType);
		comboBoxMeasurementType.setToolTipText("Measurement for Surrogates, Subsequent boxes or Gliding box");
	    comboBoxMeasurementType.setEnabled(false);
	    comboBoxMeasurementType.setEditable(false);
	    comboBoxMeasurementType.setSelectedItem("MeanHR [1/min]");
	    comboBoxMeasurementType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_MeasurementType = (String)comboBoxMeasurementType.getSelectedItem();
				logService.info(this.getClass().getName() + " Image type set to " + choiceRadioButt_MeasurementType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMeasurementType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxMeasurementType, gbc);
	    //initialize command variable
	    choiceRadioButt_MeasurementType = (String)comboBoxMeasurementType.getSelectedItem();
	    
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)
	    //add a second listener
	    comboBoxSequenceRange.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(final ActionEvent arg0) {
 				choiceRadioButt_SequenceRange = (String)comboBoxSequenceRange.getSelectedItem();
 				
 				labelMeasurementType.setEnabled(false);
				comboBoxMeasurementType.setEnabled(false);				
 				if (   choiceRadioButt_SequenceRange.equals("Entire sequence")
 				    ) {		
 					//Do nothing, already set
 				}
 				if (   choiceRadioButt_SequenceRange.equals("Subsequent boxes")
 					|| choiceRadioButt_SequenceRange.equals("Gliding box") 				
 					) {		
 					labelMeasurementType.setEnabled(true);
					comboBoxMeasurementType.setEnabled(true);
 				}
 			}
 		});
	    
	    //add a second listener
	    comboBoxSurrogateType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_SurrogateType = (String)comboBoxSurrogateType.getSelectedItem();
				
				//Reset all spinners and options
				labelMeasurementType.setEnabled(false);
				comboBoxMeasurementType.setEnabled(false);							
				if (   choiceRadioButt_SurrogateType.equals("No surrogates")
				    ) {		
					//Surrogate event is also called after a Sequence range event, so we need to check this here again.
					if (   choiceRadioButt_SequenceRange.equals("Subsequent boxes")
		 					|| choiceRadioButt_SequenceRange.equals("Gliding box") 				
		 					) {		
		 					labelMeasurementType.setEnabled(true);
							comboBoxMeasurementType.setEnabled(true);
		 				}
				}
				if (   choiceRadioButt_SurrogateType.equals("Shuffle")
					|| choiceRadioButt_SurrogateType.equals("Gaussian") 
					|| choiceRadioButt_SurrogateType.equals("Random phase") 
					|| choiceRadioButt_SurrogateType.equals("AAFT") 			
					) {
					labelMeasurementType.setEnabled(true);
					comboBoxMeasurementType.setEnabled(true);		
				}
			}
		});
	    
	    
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj1DHRVCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_TimeBase", 	   choiceRadioButt_TimeBase,
														"choiceRadioButt_WindowingType",   choiceRadioButt_WindowingType,
														"choiceRadioButt_MeasurementType", choiceRadioButt_MeasurementType,
														
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
		tableOutName = Csaj1DHRVCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
