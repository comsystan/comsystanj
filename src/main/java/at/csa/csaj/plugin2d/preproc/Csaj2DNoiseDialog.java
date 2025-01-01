/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DNoiseDialog.java
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

package at.csa.csaj.plugin2d.preproc;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
import org.scijava.ui.UIService;
import at.csa.csaj.commons.CsajDialog_2DPlugin;
/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj2DNoiseDialog extends CsajDialog_2DPlugin {

	private static final long serialVersionUID = -1421675677121830901L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private Dataset datasetIn;
	private Dataset datasetOut;
   
	//Specific dialog items
	private JComboBox<String> comboBoxNoiseType;
	private String            choiceRadioButt_NoiseType;
	
	private JLabel   labelPercentage;
	private JSpinner spinnerPercentage;
	private float    spinnerFloat_Percentage;
	
	private JLabel   labelSigma;
	private JSpinner spinnerSigma;
	private float    spinnerFloat_Sigma;
	
	private JLabel   labelScale;
	private JSpinner spinnerScale;
	private float    spinnerFloat_Scale;
	
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
	public Csaj2DNoiseDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Noise");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    JLabel labelNoiseType = new JLabel("Noise");
	    labelNoiseType.setToolTipText("Type of noise");
	    labelNoiseType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Shot", "Salt&Pepper", "Uniform", "Gaussian", "Rayleigh", "Exponential"};
		comboBoxNoiseType = new JComboBox<String>(options);
		comboBoxNoiseType.setToolTipText("Type of filter");
	    comboBoxNoiseType.setEditable(false);
	    comboBoxNoiseType.setSelectedItem("Gaussian");
	    comboBoxNoiseType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_NoiseType = (String)comboBoxNoiseType.getSelectedItem();
				logService.info(this.getClass().getName() + " Noise type set to " + choiceRadioButt_NoiseType);
				
				//Reset all spinners and options
				labelPercentage.setEnabled(false);
				spinnerPercentage.setEnabled(false);
				labelSigma.setEnabled(false);
				spinnerSigma.setEnabled(false);
				labelScale.setEnabled(false);
				spinnerScale.setEnabled(false);
								
				if (   choiceRadioButt_NoiseType.equals("Shot")
					|| choiceRadioButt_NoiseType.equals("Salt&Pepper")
					|| choiceRadioButt_NoiseType.equals("Uniform")			
				    ) {		
					labelPercentage.setEnabled(true);
					spinnerPercentage.setEnabled(true);
				}	
				if (   choiceRadioButt_NoiseType.equals("Gaussian")	
				    ) {		
					labelSigma.setEnabled(true);
					spinnerSigma.setEnabled(true);
				}	
				if (   choiceRadioButt_NoiseType.equals("Rayleigh")
					|| choiceRadioButt_NoiseType.equals("Exponential")			
				    ) {		
					labelScale.setEnabled(true);
					spinnerScale.setEnabled(true);
				}
				
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNoiseType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxNoiseType, gbc);
	    //initialize command variable
	    choiceRadioButt_NoiseType = (String)comboBoxNoiseType.getSelectedItem();
	    
	    //*****************************************************************************************
	    labelPercentage = new JLabel("Percentage");
	    labelPercentage.setToolTipText("Maximal percentage of affected data points");
	    labelPercentage.setHorizontalAlignment(JLabel.RIGHT);
	    labelPercentage.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelPercentage = new SpinnerNumberModel(10, 0.0, 100.0, 0.1); // initial, min, max, step
        spinnerPercentage = new JSpinner(spinnerModelPercentage);
        spinnerPercentage.setToolTipText("Maximal percentage of affected data points");
        spinnerPercentage.setEnabled(false);
        spinnerPercentage.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_Percentage = (float)((SpinnerNumberModel)spinnerPercentage.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Percentage set to " + spinnerFloat_Percentage);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelPercentage, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerPercentage, gbc);	    
	    //initialize command variable
	    spinnerFloat_Percentage = (float)((SpinnerNumberModel)spinnerPercentage.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelSigma = new JLabel("Sigma");
	    labelSigma.setToolTipText("Sigma of Gaussina blur");
	    labelSigma.setHorizontalAlignment(JLabel.RIGHT);
	    labelSigma.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelSigma = new SpinnerNumberModel(2.5, 0.5, 999999999999.0, 0.1); // initial, min, max, step
        spinnerSigma = new JSpinner(spinnerModelSigma);
        spinnerSigma.setToolTipText("Sigma of Gaussina blur");
        spinnerSigma.setEnabled(true);
        spinnerSigma.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_Sigma = (float)((SpinnerNumberModel)spinnerSigma.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Sigma set to " + spinnerFloat_Sigma);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSigma, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSigma, gbc);	    
	    //initialize command variable
	    spinnerFloat_Sigma = (float)((SpinnerNumberModel)spinnerSigma.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelScale = new JLabel("Scaling");
	    labelScale.setToolTipText("Scaling variable for Rayleigh and Exponential");
	    labelScale.setHorizontalAlignment(JLabel.RIGHT);
	    labelScale.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelScaling = new SpinnerNumberModel(50.0, 0.0, 999999999999.0, 0.1); // initial, min, max, step
        spinnerScale = new JSpinner(spinnerModelScaling);
        spinnerScale.setToolTipText("Scaling variable for Rayleigh and Exponential");
        spinnerScale.setEnabled(false);
        spinnerScale.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_Scale = (float)((SpinnerNumberModel)spinnerScale.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Scaling set to " + spinnerFloat_Scale);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelScale, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerScale, gbc);	    
	    //initialize command variable
	    spinnerFloat_Scale = (float)((SpinnerNumberModel)spinnerScale.getModel()).getNumber().doubleValue();
	    	
		//*****************************************************************************************
		//Change/Override items defined in the super class(es)
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
		
		//NOTE: Percentage, Sigma and Scaling go with parameter Percentage 
		if (   choiceRadioButt_NoiseType.equals("Shot")
			|| choiceRadioButt_NoiseType.equals("Salt&Pepper")
			|| choiceRadioButt_NoiseType.equals("Uniform")			
		    ) {		
			//DO NOTHING
		}
		
		if (   choiceRadioButt_NoiseType.equals("Gaussian")		
		    ) {		
			spinnerFloat_Percentage = spinnerFloat_Sigma;
		}	
		
		if (   choiceRadioButt_NoiseType.equals("Rayleigh")
			|| choiceRadioButt_NoiseType.equals("Exponential")			
		    ) {		
			spinnerFloat_Percentage = spinnerFloat_Scale;
		}	

		Future<CommandModule> future = commandService.run(Csaj2DNoiseCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
							
														"choiceRadioButt_NoiseType",      choiceRadioButt_NoiseType,
														"spinnerFloat_Percentage",        spinnerFloat_Percentage,    
														
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
		datasetOut = (Dataset)commandModule.getOutput("datasetOut");	
		uiService.show(datasetOut);
	}
}
