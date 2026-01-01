/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFilterDialog.java
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
public class Csaj2DFilterDialog extends CsajDialog_2DPlugin {

	private static final long serialVersionUID = -6460172269131670060L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private Dataset datasetIn;
	private Dataset datasetOut;
   
	//Specific dialog items
	private JComboBox<String> comboBoxFilterType;
	private String            choiceRadioButt_FilterType;
	
	private JLabel   labelSigma;
	private JSpinner spinnerSigma;
	private float    spinnerFloat_Sigma;
	
  	private JLabel   labelKernelSize;
  	private JSpinner spinnerKernelSize;
	private int      spinnerInteger_KernelSize;
	
	private JLabel   labelFFTRadius;
	private JSpinner spinnerFFTRadius;
	private int      spinnerInteger_FFTRadius;
	
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
	public Csaj2DFilterDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Filter");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    JLabel labelFilterType = new JLabel("Filter");
	    labelFilterType.setToolTipText("Type of filter");
	    labelFilterType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Gaussian blur", "Mean", "Median", "Low pass - FFT", "High pass - FFT"};
		comboBoxFilterType = new JComboBox<String>(options);
		comboBoxFilterType.setToolTipText("Type of filter");
	    comboBoxFilterType.setEditable(false);
	    comboBoxFilterType.setSelectedItem("Gaussian blur");
	    comboBoxFilterType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_FilterType = (String)comboBoxFilterType.getSelectedItem();
				logService.info(this.getClass().getName() + " Filter type set to " + choiceRadioButt_FilterType);
				
				//Reset all spinners and options
				labelSigma.setEnabled(false);
				spinnerSigma.setEnabled(false);
				labelKernelSize.setEnabled(false);
				spinnerKernelSize.setEnabled(false);
				labelFFTRadius.setEnabled(false);
				spinnerFFTRadius.setEnabled(false);
								
				if (   choiceRadioButt_FilterType.equals("Gaussian blur")
				    ) {		
					labelSigma.setEnabled(true);
					spinnerSigma.setEnabled(true);
				}
				if (   choiceRadioButt_FilterType.equals("Mean")
					|| choiceRadioButt_FilterType.equals("Median") 
					
					) {		
					labelKernelSize.setEnabled(true);
					spinnerKernelSize.setEnabled(true);
				}
				if (   choiceRadioButt_FilterType.equals("Low pass - FFT")
					|| choiceRadioButt_FilterType.equals("High pass - FFT") 
					
					) {		
					labelFFTRadius.setEnabled(true);
					spinnerFFTRadius.setEnabled(true);
				}	
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFilterType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxFilterType, gbc);
	    //initialize command variable
	    choiceRadioButt_FilterType = (String)comboBoxFilterType.getSelectedItem();
	    
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
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSigma, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSigma, gbc);	    
	    //initialize command variable
	    spinnerFloat_Sigma = (float)((SpinnerNumberModel)spinnerSigma.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelKernelSize = new JLabel("Kernel size [pixel]");
	    labelKernelSize.setToolTipText("Kernel size in pixel");
	    labelKernelSize.setHorizontalAlignment(JLabel.RIGHT);
	    labelKernelSize.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelKernelSize = new SpinnerNumberModel(3, 1, 999999999, 1); // initial, min, max, step
        spinnerKernelSize = new JSpinner(spinnerModelKernelSize);
        spinnerKernelSize.setToolTipText("Kernel size in pixel");
        spinnerKernelSize.setEnabled(false);
        spinnerKernelSize.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_KernelSize = (int)spinnerKernelSize.getValue();
                logService.info(this.getClass().getName() + " Kernel size set to " + spinnerInteger_KernelSize);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelKernelSize, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerKernelSize, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_KernelSize = (int)spinnerKernelSize.getValue();
	    
	    //*****************************************************************************************
	    labelFFTRadius = new JLabel("FFT Radius");
	    labelFFTRadius.setToolTipText("Cutoff frequency - distance from frequency = 0");
	    labelFFTRadius.setHorizontalAlignment(JLabel.RIGHT);
	    labelFFTRadius.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelFFTRadius = new SpinnerNumberModel(3, 0, 999999999, 1); // initial, min, max, step
        spinnerFFTRadius = new JSpinner(spinnerModelFFTRadius);
        spinnerFFTRadius.setToolTipText("Cutoff frequency - distance from frequency = 0");
        spinnerFFTRadius.setEnabled(false);
        spinnerFFTRadius.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_FFTRadius = (int)spinnerFFTRadius.getValue();
                logService.info(this.getClass().getName() + " FFT radius set to " + spinnerInteger_FFTRadius);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFFTRadius, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerFFTRadius, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_FFTRadius = (int)spinnerFFTRadius.getValue();
	    	
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
		 
		Future<CommandModule> future = commandService.run(Csaj2DFilterCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
							
														"choiceRadioButt_FilterType",     choiceRadioButt_FilterType,
														"spinnerFloat_Sigma",             spinnerFloat_Sigma,    
														"spinnerInteger_KernelSize",      spinnerInteger_KernelSize,    
														"spinnerInteger_FFTRadius",       spinnerInteger_FFTRadius, 
					
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
