/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DGeneratorDialog.java
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

package at.csa.csaj.plugin1d.misc;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.table.DefaultGenericTable;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import at.csa.csaj.commons.CsajDialog_PluginFrame;


/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj1DGeneratorDialog extends CsajDialog_PluginFrame {

	private static final long serialVersionUID = -3947904355989303769L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
	@Parameter
	private ThreadService threadService;
	
	
  	private DefaultGenericTable defaultGenericTable;

	//Specific dialog items
	private JComboBox<String> comboBoxMethod;
	private String            choiceRadioButt_Method;
	
	private JLabel   labelNumSequences;
	private JSpinner spinnerNumSequences;
	private int      spinnerInteger_NumSequences;
	
	private JLabel   labelNumDataPoints;
	private JSpinner spinnerNumDataPoints;
	private int      spinnerInteger_NumDataPoints;
	
	private JLabel   labelConstant;
	private JSpinner spinnerConstant;
	private int      spinnerInteger_Constant;
	    
	private JLabel   labelNumPeriods;
	private JSpinner spinnerNumPeriods;
	private int      spinnerInteger_NumPeriods;
	    
	private JLabel   labelParamA;
	private JSpinner spinnerParamA;
	private float    spinnerFloat_ParamA;
	
	private JLabel   labelParamB;
	private JSpinner spinnerParamB;
	private float    spinnerFloat_ParamB;
	
	private JLabel   labelHurst;
	private JSpinner spinnerHurst;
	private float    spinnerFloat_Hurst;
	
	private JLabel   labelFractalDimWM;
	private JSpinner spinnerFractalDimWM;
	private float    spinnerFloat_FractalDimWM;
	
	private JLabel   labelFractalDimCantor;
	private JSpinner spinnerFractalDimCantor;
	private float    spinnerFloat_FractalDimCantor;
	
	private JCheckBox  checkBoxDoNotShowSignals;
	private boolean    booleanDoNotShowSignals;
	
	public JButton btnGenerate;
		
	/**
	 * Create the dialog.
	 */
	public Csaj1DGeneratorDialog(Context context) {
			
		super();
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		context.inject(this); //Important but already injected in super class
			
		//Title of plugin
		//Overwrite
		setTitle("1D Sequence generator");

		//NORTH item
		//*****************************************************************************************
		JPanel panelInput = new JPanel();
		panelInput.setLayout(new GridBagLayout());
		panelInput.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
	    gbc.anchor = GridBagConstraints.CENTER;
		getContentPane().add(panelInput, BorderLayout.NORTH);
		
		JLabel labelInput = new JLabel("To be deleted");
		labelInput.setToolTipText("To be deleted");
		labelInput.setHorizontalAlignment(JLabel.RIGHT);
		labelInput.setToolTipText("To be deleted");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0; //IMPORTANT //This now sets the weight for the whole column 0 in the panelInput, although gpc is reset
		gbc.anchor = GridBagConstraints.WEST; //left
		//panelInput.add(labelInput, gbc); //NOT ADDED
		gbc.weightx = 0.0; //reset to default
		
		//Rest button--------------------------------------------------------
		JButton btnReset = new JButton("Reset");
		btnReset.setToolTipText("reset to default values");	
		btnReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				
				comboBoxMethod.setSelectedItem("Sine");
				choiceRadioButt_Method = (String)comboBoxMethod.getSelectedItem();
				spinnerNumSequences.setValue(1);
				spinnerInteger_NumSequences = (int)spinnerNumSequences.getValue();
				spinnerNumDataPoints.setValue(1024);
				spinnerInteger_NumDataPoints = (int)spinnerNumDataPoints.getValue();
				spinnerConstant.setValue(100);
				spinnerInteger_Constant = (int)spinnerConstant.getValue();
				spinnerNumPeriods.setValue(5);
				spinnerInteger_NumPeriods = (int)spinnerNumPeriods.getValue();
				spinnerParamA.setValue(4f);
				spinnerFloat_ParamA = (float)((SpinnerNumberModel)spinnerParamA.getModel()).getNumber().doubleValue();
				spinnerParamB.setValue(0.3);
				spinnerFloat_ParamB = (float)((SpinnerNumberModel)spinnerParamB.getModel()).getNumber().doubleValue();
				spinnerHurst.setValue(0.5);
				spinnerFloat_Hurst = (float)((SpinnerNumberModel)spinnerHurst.getModel()).getNumber().doubleValue();
				spinnerFractalDimWM.setValue(1.5);
				spinnerFloat_FractalDimWM = (float)((SpinnerNumberModel)spinnerFractalDimWM.getModel()).getNumber().doubleValue();
				float init = (float)(Math.log(2f)/Math.log(3f)); //0.6309..... 1/3
				spinnerFractalDimCantor.setValue(init);
				spinnerFloat_FractalDimCantor = (float)((SpinnerNumberModel)spinnerFractalDimCantor.getModel()).getNumber().doubleValue();
				checkBoxDoNotShowSignals.setSelected(false);
				booleanDoNotShowSignals = checkBoxDoNotShowSignals.isSelected();
			}
		});
		//gbc.insets = standardInsets;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST; //right
		panelInput.add(btnReset, gbc);
				
		//CENTER default items		
	    //*****************************************************************************************
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		contentPanel.setLayout(new GridBagLayout());
		//contentPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
	
		JScrollPane scrollPane = new JScrollPane(contentPanel);
	    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
	
		//*****************************************************************************************
	    JLabel labelMethod = new JLabel("Sequence type");
	    labelMethod.setToolTipText("Type of sequence, fGn..fractional Gaussian noise, fBm..fractional Brownian noise, W-M..Weierstraß-Mandelbrot sequence");
	    labelMethod.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Constant", "Sine", "Square", "Triangle", "SawTooth", "Gaussian", "Uniform", "Logistic", "Lorenz", "Henon", "Cubic", "Spence", "fGn", "fBm", "W-M", "Cantor"};
		comboBoxMethod = new JComboBox<String>(options);
		comboBoxMethod.setToolTipText("Type of sequence, fGn..fractional Gaussian noise, fBm..fractional Brownian noise, W-M..Weierstraß-Mandelbrot sequence");
	    comboBoxMethod.setEditable(false);
	    comboBoxMethod.setSelectedItem("Sine");
	    comboBoxMethod.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_Method = (String)comboBoxMethod.getSelectedItem();
				logService.info(this.getClass().getName() + " Image type set to " + choiceRadioButt_Method);
				
				//Reset all spinners and options
				labelConstant.setEnabled(false);
				spinnerConstant.setEnabled(false);
				labelNumPeriods.setEnabled(false);
				spinnerNumPeriods.setEnabled(false);
				labelParamA.setEnabled(false);
				spinnerParamA.setEnabled(false);
				labelParamB.setEnabled(false);
				spinnerParamB.setEnabled(false);
				labelHurst.setEnabled(false);
				spinnerHurst.setEnabled(false);
				labelFractalDimWM.setEnabled(false);
				spinnerFractalDimWM.setEnabled(false);
				labelFractalDimCantor.setEnabled(false);
				spinnerFractalDimCantor.setEnabled(false);
							
				if (   choiceRadioButt_Method.equals("Constant")
				    ) {		
					labelConstant.setEnabled(true);
					spinnerConstant.setEnabled(true);
				}
				
				if (   choiceRadioButt_Method.equals("Sine")
					|| choiceRadioButt_Method.equals("Square")
					|| choiceRadioButt_Method.equals("Triangle")
					|| choiceRadioButt_Method.equals("SawTooth")
				    ) {		
					labelNumPeriods.setEnabled(true);
					spinnerNumPeriods.setEnabled(true);
				}
				if (   choiceRadioButt_Method.equals("Logistic")
					|| choiceRadioButt_Method.equals("Henon")
					|| choiceRadioButt_Method.equals("Cubic")
					) {		
					labelParamA.setEnabled(true);
					spinnerParamA.setEnabled(true);
				}
				if (   choiceRadioButt_Method.equals("Henon")
					) {		
					labelParamB.setEnabled(true);
					spinnerParamB.setEnabled(true);
				}
				if (   choiceRadioButt_Method.equals("Henon")
					) {		
					spinnerFloat_ParamA = 1.4f;
					spinnerFloat_ParamB = 0.3f;
					spinnerParamA.setValue(1.4f);
					spinnerParamB.setValue(0.3f);
				}
				if (   choiceRadioButt_Method.equals("Logistic")
					) {	
					spinnerFloat_ParamA = 4f;
					spinnerParamA.setValue(4f);
				}
				if (   choiceRadioButt_Method.equals("Cubic")
					) {	
					spinnerFloat_ParamA = 3f;
					spinnerParamA.setValue(3f);
				}
				if (   choiceRadioButt_Method.equals("fGn")
					|| choiceRadioButt_Method.equals("fBm")
					) {		
					labelHurst.setEnabled(true);
					spinnerHurst.setEnabled(true);
				}
				if (   choiceRadioButt_Method.equals("W-M")
					) {		
					labelFractalDimWM.setEnabled(true);
					spinnerFractalDimWM.setEnabled(true);
				}
				if (   choiceRadioButt_Method.equals("Cantor")
					) {		
					labelFractalDimCantor.setEnabled(true);
					spinnerFractalDimCantor.setEnabled(true);
				}
				//if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMethod, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxMethod, gbc);
	    //initialize command variable
	    choiceRadioButt_Method = (String)comboBoxMethod.getSelectedItem();
		
	    //*****************************************************************************************
	    labelNumSequences = new JLabel("Number of sequences");
	    labelNumSequences.setToolTipText("Number of sequences");
	    labelNumSequences.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumSequences.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelNumSequences = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerNumSequences = new JSpinner(spinnerModelNumSequences);
        spinnerNumSequences.setToolTipText("Number of sequences");
        spinnerNumSequences.setEnabled(true);
        spinnerNumSequences.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumSequences = (int)spinnerNumSequences.getValue();
                logService.info(this.getClass().getName() + " Number of sequences set to " + spinnerInteger_NumSequences);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumSequences, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumSequences, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumSequences = (int)spinnerNumSequences.getValue();
	    
	    //*****************************************************************************************
	    labelNumDataPoints = new JLabel("Number of data values");
	    labelNumDataPoints.setToolTipText("Number of data point values");
	    labelNumDataPoints.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumDataPoints.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelNumDataPoints = new SpinnerNumberModel(1024, 1, 999999999, 1); // initial, min, max, step
        spinnerNumDataPoints = new JSpinner(spinnerModelNumDataPoints);
        spinnerNumDataPoints.setToolTipText("Number of data point values");
        spinnerNumDataPoints.setEnabled(true);
        spinnerNumDataPoints.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumDataPoints = (int)spinnerNumDataPoints.getValue();
                logService.info(this.getClass().getName() + " Number of data point values set to " + spinnerInteger_NumDataPoints);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumDataPoints, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumDataPoints, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumDataPoints = (int)spinnerNumDataPoints.getValue();
	    
	    //*****************************************************************************************
	    labelConstant = new JLabel("Sequence level");
	    labelConstant.setToolTipText("Sequence level");
	    labelConstant.setHorizontalAlignment(JLabel.RIGHT);
	    labelConstant.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelConstant = new SpinnerNumberModel(100, 0, 255, 1); // initial, min, max, step
        spinnerConstant = new JSpinner(spinnerModelConstant);
        spinnerConstant.setToolTipText("Sequence level");
        spinnerConstant.setEnabled(false);
        spinnerConstant.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Constant = (int)spinnerConstant.getValue();
                logService.info(this.getClass().getName() + " Constant value set to " + spinnerInteger_Constant);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelConstant, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerConstant, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Constant = (int)spinnerConstant.getValue();
	    
	    //*****************************************************************************************
	    labelNumPeriods = new JLabel("Periods");
	    labelNumPeriods.setToolTipText("Number of periods");
	    labelNumPeriods.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumPeriods.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelNumPeriods = new SpinnerNumberModel(5, 1, 999999999, 1); // initial, min, max, step
        spinnerNumPeriods = new JSpinner(spinnerModelNumPeriods);
        spinnerNumPeriods.setToolTipText("Number of periods");
        spinnerNumPeriods.setEnabled(true);
        spinnerNumPeriods.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumPeriods = (int)spinnerNumPeriods.getValue();
                logService.info(this.getClass().getName() + " Number of periods set to " + spinnerInteger_NumPeriods);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumPeriods, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumPeriods, gbc);	    
	    //initialize command variable
	    spinnerInteger_NumPeriods = (int)spinnerNumPeriods.getValue();
	    
	    //*****************************************************************************************
	    labelParamA = new JLabel("a");
	    labelParamA.setToolTipText("Parameter a for Logistic/Henon/Cubic..... signals");
	    labelParamA.setHorizontalAlignment(JLabel.RIGHT);
		labelParamA.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelParamA = new SpinnerNumberModel(4.0, 0.0, 999999999999999.0, 0.1); // initial, min, max, step
        spinnerParamA = new JSpinner(spinnerModelParamA);
        spinnerParamA.setToolTipText("Parameter a for Logistic/Henon/Cubic..... signals");
		spinnerParamA.setEnabled(false);
        spinnerParamA.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_ParamA = (float)((SpinnerNumberModel)spinnerParamA.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Paramter a set to " + spinnerFloat_ParamA);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelParamA, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerParamA, gbc);	    
	    //initialize command variable
	    spinnerFloat_ParamA = (float)((SpinnerNumberModel)spinnerParamA.getModel()).getNumber().doubleValue();
	      
	    //*****************************************************************************************
	    labelParamB = new JLabel("b");
	    labelParamB.setToolTipText("Parameter b for Henon,... signals");
	    labelParamB.setHorizontalAlignment(JLabel.RIGHT);
		labelParamB.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelParamB = new SpinnerNumberModel(0.3, 0.0, 999999999999999.0, 0.1); // initial, min, max, step
        spinnerParamB = new JSpinner(spinnerModelParamB);
        spinnerParamB.setToolTipText("Parameter b for Henon,... signals");
		spinnerParamB.setEnabled(false);
        spinnerParamB.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_ParamB = (float)((SpinnerNumberModel)spinnerParamB.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Parameter b set to " + spinnerFloat_ParamB);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 7;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelParamB, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 7;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerParamB, gbc);	    
	    //initialize command variable
	    spinnerFloat_ParamB = (float)((SpinnerNumberModel)spinnerParamB.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelHurst = new JLabel("Hurst [0,1]");
	    labelHurst.setToolTipText("Hurst coefficient in the range [0,1]");
	    labelHurst.setHorizontalAlignment(JLabel.RIGHT);
		labelHurst.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelHurst = new SpinnerNumberModel(0.5, 0.0, 1.0, 0.1); // initial, min, max, step
        spinnerHurst = new JSpinner(spinnerModelHurst);
        spinnerHurst.setToolTipText("Hurst coefficient in the range [0,1]");
		spinnerHurst.setEnabled(false);
        spinnerHurst.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_Hurst = (float)((SpinnerNumberModel)spinnerHurst.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Hurst coefficient set to " + spinnerFloat_Hurst);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 8;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHurst, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 8;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerHurst, gbc);	    
	    //initialize command variable
	    spinnerFloat_Hurst = (float)((SpinnerNumberModel)spinnerHurst.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelFractalDimWM = new JLabel("W-M Fractal dimension [1,2]");
	    labelFractalDimWM.setToolTipText("Fractal dimension in the range [1,2]");
	    labelFractalDimWM.setHorizontalAlignment(JLabel.RIGHT);
		labelFractalDimWM.setEnabled(false);
	    
		//NOTE Values <1 would produce non-fractal <-> Euclidean shapes 
	    SpinnerNumberModel spinnerModelFractalDimWM = new SpinnerNumberModel(1.5, -2.0, 2.0, 0.1); // initial, min, max, step
        spinnerFractalDimWM = new JSpinner(spinnerModelFractalDimWM);
        spinnerFractalDimWM.setToolTipText("Fractal dimension in the range [1,2]");
		spinnerFractalDimWM.setEnabled(false);
        spinnerFractalDimWM.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_FractalDimWM = (float)((SpinnerNumberModel)spinnerFractalDimWM.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " WM Fractal dimension set to " + spinnerFloat_FractalDimWM);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 9;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFractalDimWM, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 9;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerFractalDimWM, gbc);	    
	    //initialize command variable
	    spinnerFloat_FractalDimWM = (float)((SpinnerNumberModel)spinnerFractalDimWM.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelFractalDimCantor = new JLabel("Cantor Fractal dimension [0,1]");
	    labelFractalDimCantor.setToolTipText("Fractal dimension in the range [0,1]");
	    labelFractalDimCantor.setHorizontalAlignment(JLabel.RIGHT);
		labelFractalDimCantor.setEnabled(false);
	    
		float init = (float)(Math.log(2f)/Math.log(3f)); //0.6309..... 1/3
	    SpinnerNumberModel spinnerModelFractalDimCantor = new SpinnerNumberModel(init, 0.0, 1.0, 0.1); // initial, min, max, step
        spinnerFractalDimCantor = new JSpinner(spinnerModelFractalDimCantor);
        spinnerFractalDimCantor.setToolTipText("Fractal dimension in the range [0,1]");
		spinnerFractalDimCantor.setEnabled(false);
        spinnerFractalDimCantor.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_FractalDimCantor = (float)((SpinnerNumberModel)spinnerFractalDimCantor.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Cantor Fractal dimension set to " + spinnerFloat_FractalDimCantor);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFractalDimCantor, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerFractalDimCantor, gbc);	    
	    //initialize command variable
	    spinnerFloat_FractalDimCantor = (float)((SpinnerNumberModel)spinnerFractalDimCantor.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setToolTipText("Display options");
		separator.setName("Display options");
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy = 11;
		gbc.weightx = 1.0; //IMPORTANT //This now sets the weight for the whole column 1 in the contentPanel, although gpc is reset
		contentPanel.add(separator, gbc);
		gbc.weightx = 0.0; //reset to default
		gbc.gridwidth = 1; //reset to default
	    
	    //*****************************************************************************************
	    JLabel labelDoNotShowSignals = new JLabel("Do not show signal(s)");
	    labelDoNotShowSignals.setToolTipText("Recommended for large series to reduce memory demand");
	    labelDoNotShowSignals.setHorizontalAlignment(JLabel.RIGHT);
	    
		checkBoxDoNotShowSignals = new JCheckBox();
		checkBoxDoNotShowSignals.setToolTipText("Recommended for large series to reduce memory demand");
		checkBoxDoNotShowSignals.setSelected(false);
		checkBoxDoNotShowSignals.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanDoNotShowSignals = checkBoxDoNotShowSignals.isSelected();
		    	logService.info(this.getClass().getName() + " Do not show signals option set to " + booleanDoNotShowSignals);
		    	//if (booleanProcessImmediately) btnProcessSingleImage.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 12;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelDoNotShowSignals, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 12;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxDoNotShowSignals, gbc);	
	    //initialize command variable
	    booleanDoNotShowSignals = checkBoxDoNotShowSignals.isSelected();
	    
	    //SOUTH Process buttons panel 
	    //*****************************************************************************************
	    JPanel buttonPanelGenerate = new JPanel();
		buttonPanelGenerate.setLayout(new GridBagLayout());
		buttonPanelGenerate.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
		getContentPane().add(buttonPanelGenerate, BorderLayout.SOUTH);
	    
		//Process button--------------------------------------------------------
		btnGenerate = new JButton("Generate sequence(s)");
		btnGenerate.setToolTipText("Generate sequence(s)");
		btnGenerate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				threadService.run(() -> processCommand());
			}
		});
		//gbc.insets = standardInsets;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    buttonPanelGenerate.add(btnGenerate, gbc);	    
	    //*****************************************************************************************
		//Change items defined in the super class(es)
		
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
		Future<CommandModule> future = commandService.run(Csaj1DGeneratorCmd.class, false,
														"choiceRadioButt_Method",       choiceRadioButt_Method,
														"spinnerInteger_NumSequences",  spinnerInteger_NumSequences,
														"spinnerInteger_NumDataPoints", spinnerInteger_NumDataPoints,
														"spinnerInteger_Constant",      spinnerInteger_Constant,
														"spinnerInteger_NumPeriods",    spinnerInteger_NumPeriods,
														"spinnerFloat_ParamA",          spinnerFloat_ParamA,
														"spinnerFloat_ParamB",          spinnerFloat_ParamB,
														"spinnerFloat_Hurst",           spinnerFloat_Hurst,
														"spinnerFloat_FractalDimWM",    spinnerFloat_FractalDimWM,
														"spinnerFloat_FractalDimCantor",spinnerFloat_FractalDimCantor,
														"booleanDoNotShowSignals",      booleanDoNotShowSignals
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
		
		String tableOutName = choiceRadioButt_Method + " sequence(s)";
		defaultGenericTable = (DefaultGenericTable)commandModule.getOutput("defaultGenericTable");	
		uiService.show(tableOutName, defaultGenericTable);
	}
}
