/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DFFTDialog.java
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

package at.csa.csaj.plugin1d.lin;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
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
public class Csaj1DFFTDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 8988713327894975685L;

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
	private JLabel			  labelWindowingType;
	private JComboBox<String> comboBoxWindowingType;
	private String            choiceRadioButt_WindowingType;
	
	private JLabel			  labelOutputType;
	private JComboBox<String> comboBoxOutputType;
	private String            choiceRadioButt_OutputType;
	
	private JLabel			  labelNormalizationType;
	private JComboBox<String> comboBoxNormalizationType;
	private String            choiceRadioButt_NormalizationType;
	
	private JLabel			  labelScalingType;
	private JComboBox<String> comboBoxScalingType;
	private String            choiceRadioButt_ScalingType;
	
	private JLabel			  labelTimeDomainType;
	private JComboBox<String> comboBoxTimeDomainType;
	private String            choiceRadioButt_TimeDomainType;

	private JLabel   labelSampleRate;
	private JSpinner spinnerSampleRate;
	private int      spinnerInteger_SampleRate;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DFFTDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D FFT");

		//Add specific GUI elements according to Command @Parameter GUI elements   
		//*****************************************************************************************
	    labelWindowingType = new JLabel("Windowing");
	    labelWindowingType.setToolTipText("Windowing type");
	    labelWindowingType.setEnabled(true);
	    labelWindowingType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsWindowingType[] = {"Rectangular", "Cosine", "Lanczos", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"};
		comboBoxWindowingType = new JComboBox<String>(optionsWindowingType);
		comboBoxWindowingType.setToolTipText("Windowing type");
	    comboBoxWindowingType.setEnabled(true);
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
	    labelOutputType = new JLabel("Output");
	    labelOutputType.setToolTipText("Power spectrum or Magnitude spectrum");
	    labelOutputType.setEnabled(true);
	    labelOutputType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsOutputType[] = {"Power", "Magnitude"};
		comboBoxOutputType = new JComboBox<String>(optionsOutputType);
		comboBoxOutputType.setToolTipText("Power spectrum or Magnitude spectrum");
	    comboBoxOutputType.setEnabled(true);
	    comboBoxOutputType.setEditable(false);
	    comboBoxOutputType.setSelectedItem("Power");
	    comboBoxOutputType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_OutputType = (String)comboBoxOutputType.getSelectedItem();
				logService.info(this.getClass().getName() + " Output type set to " + choiceRadioButt_OutputType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOutputType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxOutputType, gbc);
	    //initialize command variable
	    choiceRadioButt_OutputType = (String)comboBoxOutputType.getSelectedItem();
	    
		//*****************************************************************************************
	    labelNormalizationType = new JLabel("Normalization");
	    labelNormalizationType.setToolTipText("Normalization type");
	    labelNormalizationType.setEnabled(true);
	    labelNormalizationType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsNormalizationType[] = {"Standard", "Unitary"};
		comboBoxNormalizationType = new JComboBox<String>(optionsNormalizationType);
		comboBoxNormalizationType.setToolTipText("Normalization type");
	    comboBoxNormalizationType.setEnabled(true);
	    comboBoxNormalizationType.setEditable(false);
	    comboBoxNormalizationType.setSelectedItem("Standard");
	    comboBoxNormalizationType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_NormalizationType = (String)comboBoxNormalizationType.getSelectedItem();
				logService.info(this.getClass().getName() + " Normalization type set to " + choiceRadioButt_NormalizationType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNormalizationType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxNormalizationType, gbc);
	    //initialize command variable
	    choiceRadioButt_NormalizationType = (String)comboBoxNormalizationType.getSelectedItem();
	    
		//*****************************************************************************************
	    labelScalingType = new JLabel("Scaling");
	    labelScalingType.setToolTipText("Scaling of output values");
	    labelScalingType.setEnabled(true);
	    labelScalingType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsScalingType[] = {"Log", "Ln", "Linear"};
		comboBoxScalingType = new JComboBox<String>(optionsScalingType);
		comboBoxScalingType.setToolTipText("Scaling of output values");
	    comboBoxScalingType.setEnabled(true);
	    comboBoxScalingType.setEditable(false);
	    comboBoxScalingType.setSelectedItem("Log");
	    comboBoxScalingType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_ScalingType = (String)comboBoxScalingType.getSelectedItem();
				logService.info(this.getClass().getName() + " Scaling type set to " + choiceRadioButt_ScalingType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelScalingType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxScalingType, gbc);
	    //initialize command variable
	    choiceRadioButt_ScalingType = (String)comboBoxScalingType.getSelectedItem();
	    
		//*****************************************************************************************
	    labelTimeDomainType = new JLabel("Time domain");
	    labelTimeDomainType.setToolTipText("Time domain type");
	    labelTimeDomainType.setEnabled(true);
	    labelTimeDomainType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsTimeDomainType[] = {"Unitary", "Hz"};
		comboBoxTimeDomainType = new JComboBox<String>(optionsTimeDomainType);
		comboBoxTimeDomainType.setToolTipText("Time domain type");
	    comboBoxTimeDomainType.setEnabled(true);
	    comboBoxTimeDomainType.setEditable(false);
	    comboBoxTimeDomainType.setSelectedItem("Unitary");
	    comboBoxTimeDomainType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_TimeDomainType = (String)comboBoxTimeDomainType.getSelectedItem();
				logService.info(this.getClass().getName() + " Time domain type set to " + choiceRadioButt_TimeDomainType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelTimeDomainType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxTimeDomainType, gbc);
	    //initialize command variable
	    choiceRadioButt_TimeDomainType = (String)comboBoxTimeDomainType.getSelectedItem();
		     
	    //*****************************************************************************************
	    labelSampleRate = new JLabel("Sample rate (Hz)");
	    labelSampleRate.setToolTipText("Sample rate");
	    labelSampleRate.setEnabled(true);
	    labelSampleRate.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelSampleRate = new SpinnerNumberModel(1000, 1, 999999999, 1); // initial, min, max, step
        spinnerSampleRate = new JSpinner(spinnerModelSampleRate);
        spinnerSampleRate.setToolTipText("Sample rate");
        spinnerSampleRate.setEnabled(true);
        spinnerSampleRate.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_SampleRate = (int)spinnerSampleRate.getValue();     	 	
                logService.info(this.getClass().getName() + " Sample rate set to " + spinnerInteger_SampleRate);
                if (booleanProcessImmediately) btnProcessSingleColumn .doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSampleRate, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSampleRate, gbc);	    
	    //initialize command variable
	    spinnerInteger_SampleRate = (int)spinnerSampleRate.getValue();
	          	
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)	
	    //Restricted options
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>)comboBoxSequenceRange.getModel();
		model.removeElement("Subsequent boxes");
		model.removeElement("Gliding box");	
		comboBoxSequenceRange.setSelectedItem("Entire sequence");
		choiceRadioButt_SequenceRange = (String)comboBoxSequenceRange.getSelectedItem();
	    
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
		//Following run initiates a "ProcessAllImages" 
		Future<CommandModule> future = commandService.run(Csaj1DFFTCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_WindowingType",     choiceRadioButt_WindowingType,
														"choiceRadioButt_OutputType",        choiceRadioButt_OutputType,
														"choiceRadioButt_NormalizationType", choiceRadioButt_NormalizationType,
														"choiceRadioButt_ScalingType",       choiceRadioButt_ScalingType,
														"choiceRadioButt_TimeDomainType",    choiceRadioButt_TimeDomainType,
														"spinnerInteger_SampleRate",		 spinnerInteger_SampleRate,

														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
														//"spinnerInteger_NumSurrogates",  spinnerInteger_NumSurrogates,
														//"spinnerInteger_BoxLength",      spinnerInteger_BoxLength,
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
		tableOutName = Csaj1DFFTCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
