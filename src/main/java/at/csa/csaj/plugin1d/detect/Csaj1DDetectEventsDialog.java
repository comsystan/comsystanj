/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DDetectEventsDialog.java
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

package at.csa.csaj.plugin1d.detect;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
public class Csaj1DDetectEventsDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = -5527732752597905243L;

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
	private JComboBox<String> comboBoxEventType;
	private String            choiceRadioButt_EventType;
	
	private JLabel       labelThresholdType;
	private JPanel       panelThresholdType;
	private ButtonGroup  buttonGroupThresholdType;
    private JRadioButton radioButtonThres;
    private JRadioButton radioButtonMAC;
	private String       choiceRadioButt_ThresholdType;
	
	private JLabel   labelThreshold;
	private JSpinner spinnerThreshold;
	private float    spinnerFloat_Threshold;
	
	public JButton buttonEstimateTau;
	
	private JLabel   labelTau;
	private JSpinner spinnerTau;
	private int      spinnerInteger_Tau;
	
	private JLabel   labelOffset;
	private JSpinner spinnerOffset;
	private float    spinnerFloat_Offset;
	
	private JLabel       labelSlopeType;
	private JPanel       panelSlopeType;
	private ButtonGroup  buttonGroupSlopeType;
    private JRadioButton radioButtonPositive;
    private JRadioButton radioButtonNegative;
	private String       choiceRadioButt_SlopeType;
	
	private JLabel   labelChenM;
	private JSpinner spinnerChenM;
	private int      spinnerInteger_ChenM;
	
	private JLabel   labelSumInterval;
	private JSpinner spinnerSumInterval;
	private int      spinnerInteger_SumInterval;
	
	private JLabel   labelPeakFrame;
	private JSpinner spinnerPeakFrame;
	private int      spinnerInteger_PeakFrame;
	
	private JLabel            labelOseaMethod;
	private JComboBox<String> comboBoxOseaMethod;
	private String            choiceRadioButt_OseaMethod;
	
	private JLabel   labelSampleRate;
	private JSpinner spinnerSampleRate;
	private int      spinnerInteger_SampleRate;
	
	private JLabel			  labelOutputType;
	private JComboBox<String> comboBoxOutputType;
	private String            choiceRadioButt_OutputType;
	
	private JLabel    labelFirstColIsDomain;
	private JCheckBox checkBoxFirstColIsDomain;
 	private boolean   booleanFirstColIsDomain;
 	
 	private JLabel    labelSubtractMean;
	private JCheckBox checkBoxSubtractMean;
 	private boolean   booleanSubtractMean;
 	
	private JLabel   labelSequenceScalingFactor;
	private JSpinner spinnerSequenceScalingFactor;
	private float      spinnerFloat_SequenceScalingFactor;
 	
 	private JLabel    labelDisplayOnOriginalSequence;
	private JCheckBox checkBoxDisplayOnOriginalSequence;
 	private boolean   booleanDisplayOnOriginalSequence;
 	
 	private JLabel    labelDisplayAsSequence;
	private JCheckBox checkBoxDisplayAsSequence;
 	private boolean   booleanDisplayAsSequence;
	

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DDetectEventsDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Event detection");

		//Add specific GUI elements according to Command @Parameter GUI elements		
	    //*****************************************************************************************
	    JLabel labelEventType = new JLabel("Event type");
	    labelEventType.setToolTipText("Event type to be detected");
	    labelEventType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsEventType[] = {"Peaks", "Valleys", "Slope", "QRS peaks (Chen&Chen)", "QRS peaks (Osea)"};
		comboBoxEventType = new JComboBox<String>(optionsEventType);
		comboBoxEventType.setToolTipText("Event type to be detected");
	    comboBoxEventType.setEditable(false);
	    comboBoxEventType.setSelectedItem("Peaks");
	    comboBoxEventType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_EventType = (String)comboBoxEventType.getSelectedItem();
				logService.info(this.getClass().getName() + " Event type set to " + choiceRadioButt_EventType);
						
				//Disable all options		
				labelThresholdType.setEnabled(false);
				radioButtonThres.setEnabled(false);
				radioButtonMAC.setEnabled(false);
				
				labelThreshold.setEnabled(false);
				spinnerThreshold.setEnabled(false);
				
				buttonEstimateTau.setEnabled(false);
				
				labelTau.setEnabled(false);
				spinnerTau.setEnabled(false);
		
				labelOffset.setEnabled(false);
				spinnerOffset.setEnabled(false);

				labelSlopeType.setEnabled(false);
			    radioButtonPositive.setEnabled(false);
			    radioButtonNegative.setEnabled(false);

				labelChenM.setEnabled(false);
				spinnerChenM.setEnabled(false);
	
				labelSumInterval.setEnabled(false);
				spinnerSumInterval.setEnabled(false);

				labelPeakFrame.setEnabled(false);
				spinnerPeakFrame.setEnabled(false);
				
				labelOseaMethod.setEnabled(false);
				comboBoxOseaMethod.setEnabled(false);
		
				labelSampleRate.setEnabled(false);
				spinnerSampleRate.setEnabled(false);
		
				if (   choiceRadioButt_EventType.equals("Peaks")
					|| choiceRadioButt_EventType.equals("Valleys")
					) {		
					labelThresholdType.setEnabled(true);
					radioButtonThres.setEnabled(true);
					radioButtonThres.setSelected(true);
					labelThreshold.setEnabled(true);
					spinnerThreshold.setEnabled(true);
						
					comboBoxOutputType.removeAllItems();
					comboBoxOutputType.addItem("Event domain values");
					comboBoxOutputType.addItem("Event values");
					comboBoxOutputType.addItem("Intervals");
					comboBoxOutputType.addItem("Heights");
					comboBoxOutputType.addItem("Energies");
					comboBoxOutputType.addItem("delta Heights");				
				}
				
				if (   choiceRadioButt_EventType.equals("Slope")
					) {		
					labelThresholdType.setEnabled(true);
					radioButtonThres.setEnabled(true);
					radioButtonThres.setSelected(true);
					radioButtonMAC.setEnabled(true);
					
					labelThreshold.setEnabled(true);
					spinnerThreshold.setEnabled(true);
					
					labelSlopeType.setEnabled(true);
				    radioButtonPositive.setEnabled(true);
				    radioButtonNegative.setEnabled(true);
			
					comboBoxOutputType.removeAllItems();
					comboBoxOutputType.addItem("Event domain values");
					comboBoxOutputType.addItem("Event values");
					comboBoxOutputType.addItem("Intervals");		
				}
				
				if (   choiceRadioButt_EventType.equals("QRS peaks (Chen&Chen)")
					) {		
					labelChenM.setEnabled(true);
					spinnerChenM.setEnabled(true);
		
					labelSumInterval.setEnabled(true);
					spinnerSumInterval.setEnabled(true);

					labelPeakFrame.setEnabled(true);
					spinnerPeakFrame.setEnabled(true);	
					
					comboBoxOutputType.removeAllItems();
					comboBoxOutputType.addItem("Event domain values");
					comboBoxOutputType.addItem("Event values");
					comboBoxOutputType.addItem("Intervals");		
				}
				
				if (   choiceRadioButt_EventType.equals("QRS peaks (Osea)")
					) {
					
					labelOseaMethod.setEnabled(true);
					comboBoxOseaMethod.setEnabled(true);
			
					labelSampleRate.setEnabled(true);
					spinnerSampleRate.setEnabled(true);
				
					comboBoxOutputType.removeAllItems();
					comboBoxOutputType.addItem("Event domain values");
					comboBoxOutputType.addItem("Event values");
					comboBoxOutputType.addItem("Intervals");	
				}
						
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelEventType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxEventType, gbc);
	    //initialize command variable
	    choiceRadioButt_EventType = (String)comboBoxEventType.getSelectedItem();
		
	    //*****************************************************************************************		
	    labelThresholdType = new JLabel("Threshold type");
	    labelThresholdType.setToolTipText("Threshold type");
	    labelThresholdType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupThresholdType = new ButtonGroup();
		radioButtonThres         = new JRadioButton("Threshold");
		radioButtonMAC           = new JRadioButton("MAC");
		radioButtonThres.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonThres.isSelected()) {
					choiceRadioButt_ThresholdType = radioButtonThres.getText();
					
					labelThreshold.setEnabled(true);
					spinnerThreshold.setEnabled(true);
					
					buttonEstimateTau.setEnabled(false);
					
					labelTau.setEnabled(false);
					spinnerTau.setEnabled(false);
			
					labelOffset.setEnabled(false);
					spinnerOffset.setEnabled(false);

					//panelSlopeType.setEnabled(false);
				    //radioButtonPositive.setEnabled(false);
				    ///radioButtonNegative.setEnabled(false);
				} 
				logService.info(this.getClass().getName() + " Threshold type set to " + choiceRadioButt_ThresholdType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonMAC.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonMAC.isSelected()) {
					choiceRadioButt_ThresholdType = radioButtonMAC.getText();
					
					labelThreshold.setEnabled(false);
					spinnerThreshold.setEnabled(false);
					
					buttonEstimateTau.setEnabled(true);
					
					labelTau.setEnabled(true);
					spinnerTau.setEnabled(true);
			
					labelOffset.setEnabled(true);
					spinnerOffset.setEnabled(true);

					//panelSlopeType.setEnabled(false);
				    //radioButtonPositive.setEnabled(false);
				    //radioButtonNegative.setEnabled(false);
				}
				logService.info(this.getClass().getName() + " Threshold type set to " + choiceRadioButt_ThresholdType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupThresholdType.add(radioButtonThres);
		buttonGroupThresholdType.add(radioButtonMAC);
		radioButtonThres.setSelected(true);
		radioButtonMAC.setEnabled(false);
		
		panelThresholdType = new JPanel();
		panelThresholdType.setToolTipText("Threshold type");
		panelThresholdType.setLayout(new BoxLayout(panelThresholdType, BoxLayout.Y_AXIS)); 
	    panelThresholdType.add(radioButtonThres);
	    panelThresholdType.add(radioButtonMAC);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelThresholdType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelThresholdType, gbc);
	    //initialize command variable
		if (radioButtonThres.isSelected()) choiceRadioButt_ThresholdType = radioButtonThres.getText();
		if (radioButtonMAC.isSelected())   choiceRadioButt_ThresholdType = radioButtonMAC.getText();
	    
		//*****************************************************************************************
	    labelThreshold = new JLabel("Threshold value");
	    labelThreshold.setToolTipText("Threshold value - decimal point number, negative or positive.");
	    labelThreshold.setEnabled(true);
	    labelThreshold.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelThreshold = new SpinnerNumberModel(1.0, -999999999.0, 999999999.0, 1.0); // initial, min, max, step
        spinnerThreshold = new JSpinner(spinnerModelThreshold);
        spinnerThreshold.setToolTipText("Threshold value - decimal point number, negative or positive.");
        spinnerThreshold.setEnabled(true);
        spinnerThreshold.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {  
            	spinnerFloat_Threshold = (float)((SpinnerNumberModel)spinnerThreshold.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Threshold value set to " + spinnerFloat_Threshold);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelThreshold, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerThreshold, gbc);	    
	    
	    //initialize command variable
	    spinnerFloat_Threshold = (float)((SpinnerNumberModel)spinnerThreshold.getModel()).getNumber().doubleValue();
		
		//*****************************************************************************************
		buttonEstimateTau = new JButton("Estimate Tau");
		buttonEstimateTau.setToolTipText("Estiamte Tau");
		buttonEstimateTau.setEnabled(false);
		buttonEstimateTau.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				//TO DO
			}
		});
		//gbc.insets = standardInsets;
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(buttonEstimateTau, gbc);	
		
		//*****************************************************************************************
	    labelTau = new JLabel("MAC - Tau");
	    labelTau.setToolTipText("Time lag/delay for MAC");
	    labelTau.setEnabled(false);
	    labelTau.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelTau = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerTau = new JSpinner(spinnerModelTau);
        spinnerTau.setToolTipText("Time lag/delay for MAC");
        spinnerTau.setEnabled(false);
        spinnerTau.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Tau = (int)spinnerTau.getValue();
                logService.info(this.getClass().getName() + " Tau set to " + spinnerInteger_Tau);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelTau, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerTau, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Tau = (int)spinnerTau.getValue();
	    
		//*****************************************************************************************
	    labelOffset = new JLabel("MAC - Offset");
	    labelOffset.setToolTipText("Offset for MAC");
	    labelOffset.setEnabled(false);
	    labelOffset.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelOffset = new SpinnerNumberModel(0.0, 0.0, 999999999.0, 1.0); // initial, min, max, step
        spinnerOffset = new JSpinner(spinnerModelOffset);
        spinnerOffset.setToolTipText("Offset for MAC");
        spinnerOffset.setEnabled(false);
        spinnerOffset.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_Offset = (float)((SpinnerNumberModel)spinnerOffset.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Offset set to " + spinnerFloat_Offset);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOffset, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerOffset, gbc);	    
	    
	    //initialize command variable
	    spinnerFloat_Offset = (float)((SpinnerNumberModel)spinnerOffset.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************		
	    labelSlopeType = new JLabel("Slope type");
	    labelSlopeType.setToolTipText("Slope type");
	    labelSlopeType.setEnabled(false);
	    labelSlopeType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupSlopeType = new ButtonGroup();
		radioButtonPositive  = new JRadioButton("Positive");
		radioButtonNegative  = new JRadioButton("Negative");
		radioButtonPositive.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonPositive.isSelected()) {
					choiceRadioButt_SlopeType = radioButtonPositive.getText();
				} 
				logService.info(this.getClass().getName() + " Slope type set to " + choiceRadioButt_SlopeType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonNegative.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonNegative.isSelected()) {
					choiceRadioButt_SlopeType = radioButtonNegative.getText();
				}
				logService.info(this.getClass().getName() + " Slope type set to " + choiceRadioButt_SlopeType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupSlopeType.add(radioButtonPositive);
		buttonGroupSlopeType.add(radioButtonNegative);
		radioButtonPositive.setSelected(true);
		radioButtonPositive.setEnabled(false);
		radioButtonNegative.setEnabled(false);
		
		panelSlopeType = new JPanel();
		panelSlopeType.setToolTipText("Slope type");
		panelSlopeType.setLayout(new BoxLayout(panelSlopeType, BoxLayout.Y_AXIS)); 
	    panelSlopeType.add(radioButtonPositive);
	    panelSlopeType.add(radioButtonNegative);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSlopeType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelSlopeType, gbc);
	    //initialize command variable
		if (radioButtonPositive.isSelected()) choiceRadioButt_SlopeType = radioButtonPositive.getText();
		if (radioButtonNegative.isSelected()) choiceRadioButt_SlopeType = radioButtonNegative.getText();
	    
		//*****************************************************************************************
	    labelChenM = new JLabel("Chen&Chen - M");
	    labelChenM.setToolTipText("Chen&Chen highpass filter parameter, usually set to 3,5,7,9...");
	    labelChenM.setEnabled(false);
	    labelChenM.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelChenM = new SpinnerNumberModel(5, 1, 999999999, 2); // initial, min, max, step
        spinnerChenM = new JSpinner(spinnerModelChenM);
        spinnerChenM.setToolTipText("Chen&Chen highpass filter parameter, usually set to 3,5,7,9...");
        spinnerChenM.setEnabled(false);
        spinnerChenM.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_ChenM = (int)spinnerChenM.getValue();
                logService.info(this.getClass().getName() + " M set to " + spinnerInteger_ChenM);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 7;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelChenM, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 7;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerChenM, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_ChenM = (int)spinnerChenM.getValue();
	    
		//*****************************************************************************************
	    labelSumInterval = new JLabel("Chen&Chen - Sum interval");
	    labelSumInterval.setToolTipText("Chen&Chen lowpass filter parameter, usually set to 10,20,30,40,50...");
	    labelSumInterval.setEnabled(false);
	    labelSumInterval.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelSumInterval = new SpinnerNumberModel(30, 10, 999999999, 10); // initial, min, max, step
        spinnerSumInterval = new JSpinner(spinnerModelSumInterval);
        spinnerSumInterval.setToolTipText("Chen&Chen lowpass filter parameter, usually set to 10,20,30,40,50...");
        spinnerSumInterval.setEnabled(false);
        spinnerSumInterval.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_SumInterval = (int)spinnerSumInterval.getValue();
                logService.info(this.getClass().getName() + " Sum interval set to " + spinnerInteger_SumInterval);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 8;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSumInterval, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 8;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSumInterval, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_SumInterval = (int)spinnerSumInterval.getValue();
	    
		//*****************************************************************************************
	    labelPeakFrame = new JLabel("Chen&Chen - Peak frame");
	    labelPeakFrame.setToolTipText("Chen&Chen frame for peak parameter, usually set to 100, 150, 200, 250, 300,...");
	    labelPeakFrame.setEnabled(false);
	    labelPeakFrame.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelPeakFrame = new SpinnerNumberModel(250, 50, 999999999, 50); // initial, min, max, step
        spinnerPeakFrame = new JSpinner(spinnerModelPeakFrame);
        spinnerPeakFrame.setToolTipText("Chen&Chen frame for peak parameter, usually set to 100, 150, 200, 250, 300,...");
        spinnerPeakFrame.setEnabled(false);
        spinnerPeakFrame.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_PeakFrame = (int)spinnerPeakFrame.getValue();
                logService.info(this.getClass().getName() + " Peak frame set to " + spinnerInteger_PeakFrame);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 9;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelPeakFrame, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 9;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerPeakFrame, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_PeakFrame = (int)spinnerPeakFrame.getValue();
	    
	    //*****************************************************************************************
	    labelOseaMethod = new JLabel("Osea - method");
	    labelOseaMethod.setToolTipText("Osea method");
	    labelOseaMethod.setEnabled(false);
	    labelOseaMethod.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsOseaMethod[] = {"QRSDetect", "QRSDetect2", "BeatDetectAndClassify"};
		comboBoxOseaMethod = new JComboBox<String>(optionsOseaMethod);
		comboBoxOseaMethod.setToolTipText("Osea method");
		comboBoxOseaMethod.setEnabled(false);
	    comboBoxOseaMethod.setEditable(false);
	    comboBoxOseaMethod.setSelectedItem("QRSDetect");
	    comboBoxOseaMethod.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_OseaMethod = (String)comboBoxOseaMethod.getSelectedItem();
				logService.info(this.getClass().getName() + " Osea method set to " + choiceRadioButt_OseaMethod);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOseaMethod, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxOseaMethod, gbc);
	    //initialize command variable
	    choiceRadioButt_OseaMethod = (String)comboBoxOseaMethod.getSelectedItem();
	    
		//*****************************************************************************************
	    labelSampleRate = new JLabel("Osea - sample rate");
	    labelSampleRate.setToolTipText("Sample rate of signal in Hz");
	    labelSampleRate.setEnabled(false);
	    labelSampleRate.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelSampleRate = new SpinnerNumberModel(100, 1, 999999999, 1); // initial, min, max, step
        spinnerSampleRate = new JSpinner(spinnerModelSampleRate);
        spinnerSampleRate.setToolTipText("Sample rate of signal in Hz");
        spinnerSampleRate.setEnabled(false);
        spinnerSampleRate.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_SampleRate = (int)spinnerSampleRate.getValue();
                logService.info(this.getClass().getName() + " Sample rate set to " + spinnerInteger_SampleRate);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 11;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSampleRate, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 11;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSampleRate, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_SampleRate = (int)spinnerSampleRate.getValue();
	    
		//*****************************************************************************************
	    labelOutputType = new JLabel("Output type");
	    labelOutputType.setToolTipText("Output type");
	    labelOutputType.setEnabled(true);
	    labelOutputType.setHorizontalAlignment(JLabel.RIGHT);
		
		String[] optionsOutputType = new String[]{"Event domain values", "Event values", "Intervals", "Heights", "Energies", "delta Heights"};
		comboBoxOutputType = new JComboBox<String>(optionsOutputType);
		comboBoxOutputType.setToolTipText("Output type");
	    comboBoxOutputType.setEnabled(true);
	    comboBoxOutputType.setEditable(false);
	    comboBoxOutputType.setSelectedItem("Event domain values");
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
	    gbc.gridy = 12;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOutputType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 12;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxOutputType, gbc);
	    //initialize command variable
	    choiceRadioButt_OutputType = (String)comboBoxOutputType.getSelectedItem();
	    
	    //*****************************************************************************************
 		labelFirstColIsDomain = new JLabel("First data column is the domain");
 		labelFirstColIsDomain.setToolTipText("First data column is the domain");
 		labelFirstColIsDomain.setHorizontalAlignment(JLabel.RIGHT);
 		labelFirstColIsDomain.setEnabled(true);
 		
 		checkBoxFirstColIsDomain = new JCheckBox();
 		checkBoxFirstColIsDomain.setToolTipText("First data column is the domain");
 		checkBoxFirstColIsDomain.setEnabled(true);
 		checkBoxFirstColIsDomain.setSelected(false);
 		checkBoxFirstColIsDomain.addItemListener(new ItemListener() {
 			@Override
 		    public void itemStateChanged(ItemEvent e) {
 		    	booleanFirstColIsDomain = checkBoxFirstColIsDomain.isSelected();	    
 				logService.info(this.getClass().getName() + " First data column is the domain option set to " + booleanFirstColIsDomain);
 				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
 		    }
 		});
 		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
 	    gbc.gridy = 13;
 	    gbc.anchor = GridBagConstraints.EAST; //right
 	    contentPanel.add(labelFirstColIsDomain, gbc);
 	    gbc.gridx = 1;
 	    gbc.gridy = 13;
 	    gbc.anchor = GridBagConstraints.WEST; //left
 	    contentPanel.add(checkBoxFirstColIsDomain, gbc);	
 	 
 	    //initialize command variable
 	    booleanFirstColIsDomain = checkBoxFirstColIsDomain.isSelected();
 	    
 	   //*****************************************************************************************
 		labelSubtractMean = new JLabel("Subtract mean");
 		labelSubtractMean.setToolTipText("Subtract mean from signal before event detection");
 		labelSubtractMean.setHorizontalAlignment(JLabel.RIGHT);
 		labelSubtractMean.setEnabled(true);
 		
 		checkBoxSubtractMean = new JCheckBox();
 		checkBoxSubtractMean.setToolTipText("Subtract mean from signal before event detetection");
 		checkBoxSubtractMean.setEnabled(true);
 		checkBoxSubtractMean.setSelected(false);
 		checkBoxSubtractMean.addItemListener(new ItemListener() {
 			@Override
 		    public void itemStateChanged(ItemEvent e) {
 		    	booleanSubtractMean = checkBoxSubtractMean.isSelected();	    
 				logService.info(this.getClass().getName() + " Subtract mean option set to " + booleanSubtractMean);
 				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
 		    }
 		});
 		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
 	    gbc.gridy = 14;
 	    gbc.anchor = GridBagConstraints.EAST; //right
 	    contentPanel.add(labelSubtractMean, gbc);
 	    gbc.gridx = 1;
 	    gbc.gridy = 14;
 	    gbc.anchor = GridBagConstraints.WEST; //left
 	    contentPanel.add(checkBoxSubtractMean, gbc);	
 	 
 	    //initialize command variable
 	    booleanSubtractMean = checkBoxSubtractMean.isSelected();
 	    
 		//*****************************************************************************************
	    labelSequenceScalingFactor = new JLabel("Sequence scaling factor");
	    labelSequenceScalingFactor.setToolTipText("This factor is multiplied to the sequence values. Usefull for small sequence levels (e.g. Osea uses int values!)");
	    labelSequenceScalingFactor.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelSequenceScalingFactor = new SpinnerNumberModel(1.0, -999999999.0, 999999999.0, 1.0); // initial, min, max, step
        spinnerSequenceScalingFactor = new JSpinner(spinnerModelSequenceScalingFactor);
        spinnerSequenceScalingFactor.setToolTipText("This factor is multiplied to the sequence values. Usefull for small sequence levels (e.g. Osea uses int values!)");
        spinnerSequenceScalingFactor.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
        		spinnerFloat_SequenceScalingFactor = (float)((SpinnerNumberModel)spinnerSequenceScalingFactor.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Embedding dimension set to " + spinnerFloat_SequenceScalingFactor);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 15;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSequenceScalingFactor, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 15;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSequenceScalingFactor, gbc);	    
	    
	    //initialize command variable
	    spinnerFloat_SequenceScalingFactor = (float)((SpinnerNumberModel)spinnerSequenceScalingFactor.getModel()).getNumber().doubleValue();
	 
 	   //*****************************************************************************************
 		labelDisplayOnOriginalSequence = new JLabel("Display detected events on original sequence");
 		labelDisplayOnOriginalSequence.setToolTipText("Display detected events on original sequence");
 		labelDisplayOnOriginalSequence.setHorizontalAlignment(JLabel.RIGHT);
 		labelDisplayOnOriginalSequence.setEnabled(true);
 		
 		checkBoxDisplayOnOriginalSequence = new JCheckBox();
 		checkBoxDisplayOnOriginalSequence.setToolTipText("Display detected events on original sequence");
 		checkBoxDisplayOnOriginalSequence.setEnabled(true);
 		checkBoxDisplayOnOriginalSequence.setSelected(false);
 		checkBoxDisplayOnOriginalSequence.addItemListener(new ItemListener() {
 			@Override
 		    public void itemStateChanged(ItemEvent e) {
 		    	booleanDisplayOnOriginalSequence = checkBoxDisplayOnOriginalSequence.isSelected();	    
 				logService.info(this.getClass().getName() + " Display detected events on original sequence option set to " + booleanDisplayOnOriginalSequence);
 				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
 		    }
 		});
 		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
 	    gbc.gridy = 16;
 	    gbc.anchor = GridBagConstraints.EAST; //right
 	    contentPanel.add(labelDisplayOnOriginalSequence, gbc);
 	    gbc.gridx = 1;
 	    gbc.gridy = 16;
 	    gbc.anchor = GridBagConstraints.WEST; //left
 	    contentPanel.add(checkBoxDisplayOnOriginalSequence, gbc);	
 	 
 	    //initialize command variable
 	    booleanDisplayOnOriginalSequence = checkBoxDisplayOnOriginalSequence.isSelected();
 	    
 	   //*****************************************************************************************
 		labelDisplayAsSequence = new JLabel("Display detected events as a sequence");
 		labelDisplayAsSequence.setToolTipText("Display detected events as a sequence");
 		labelDisplayAsSequence.setHorizontalAlignment(JLabel.RIGHT);
 		labelDisplayAsSequence.setEnabled(true);
 		
 		checkBoxDisplayAsSequence = new JCheckBox();
 		checkBoxDisplayAsSequence.setToolTipText("Display detected events as a sequence");
 		checkBoxDisplayAsSequence.setEnabled(true);
 		checkBoxDisplayAsSequence.setSelected(false);
 		checkBoxDisplayAsSequence.addItemListener(new ItemListener() {
 			@Override
 		    public void itemStateChanged(ItemEvent e) {
 		    	booleanDisplayAsSequence = checkBoxDisplayAsSequence.isSelected();	    
 				logService.info(this.getClass().getName() + " Display detected events as a sequence option set to " + booleanDisplayAsSequence);
 				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
 		    }
 		});
 		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
 	    gbc.gridy = 17;
 	    gbc.anchor = GridBagConstraints.EAST; //right
 	    contentPanel.add(labelDisplayAsSequence, gbc);
 	    gbc.gridx = 1;
 	    gbc.gridy = 17;
 	    gbc.anchor = GridBagConstraints.WEST; //left
 	    contentPanel.add(checkBoxDisplayAsSequence, gbc);	
 	 
 	    //initialize command variable
 	    booleanDisplayAsSequence = checkBoxDisplayAsSequence.isSelected();
 	    
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)
 	   //Restricted options
		String optionsSequenceRange[] = {"Entire sequence"}; 
		comboBoxSequenceRange = new JComboBox<String>(optionsSequenceRange);
	    
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
		Future<CommandModule> future = commandService.run(Csaj1DDetectEventsCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_EventType",          choiceRadioButt_EventType,
														"choiceRadioButt_ThresholdType",      choiceRadioButt_ThresholdType,
														"spinnerFloat_Threshold",             spinnerFloat_Threshold,
														"spinnerInteger_Tau",			      spinnerInteger_Tau,
														"spinnerFloat_Offset",                spinnerFloat_Offset,
														"choiceRadioButt_SlopeType",          choiceRadioButt_SlopeType,
														"spinnerInteger_ChenM",               spinnerInteger_ChenM,
														"spinnerInteger_SumInterval",         spinnerInteger_SumInterval,
														"spinnerInteger_PeakFrame", 	      spinnerInteger_PeakFrame,
														"choiceRadioButt_OseaMethod",	      choiceRadioButt_OseaMethod,
														"spinnerInteger_SampleRate", 	      spinnerInteger_SampleRate,
														"choiceRadioButt_OutputType",         choiceRadioButt_OutputType,
														"booleanFirstColIsDomain",	          booleanFirstColIsDomain,
														"booleanSubtractMean",	              booleanSubtractMean,
														"spinnerFloat_SequenceScalingFactor", spinnerFloat_SequenceScalingFactor,
														"booleanDisplayOnOriginalSequence",	  booleanDisplayOnOriginalSequence,
														"booleanDisplayAsSequence",	          booleanDisplayAsSequence,
						
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
		tableOutName = Csaj1DDetectEventsCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
