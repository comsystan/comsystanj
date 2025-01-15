/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DFilterDialog.java
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

package at.csa.csaj.plugin1d.preproc;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
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
public class Csaj1DFilterDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = -5317902611709031806L;

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
	private JLabel			  labelFilterType;
	private JComboBox<String> comboBoxFilterType;
	private String            choiceRadioButt_FilterType;
	
	private JLabel	 labelRange;
	private JSpinner spinnerRange;
	private int      spinnerInteger_Range;
	
	private JLabel   labelFFTRadius;
	private JSpinner spinnerFFTRadius;
	private int      spinnerInteger_FFTRadius;
	
	private JLabel			  labelWindowingType;
	private JComboBox<String> comboBoxWindowingType;
	private String            choiceRadioButt_WindowingType;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DFilterDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Filter");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    labelFilterType = new JLabel("Filter type");
	    labelFilterType.setToolTipText("Type of filter");
	    labelFilterType.setEnabled(true);
	    labelFilterType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsFilterType[] = {"Moving average", "Moving median", "Low pass - FFT", "High pass - FFT"};
		comboBoxFilterType = new JComboBox<String>(optionsFilterType);
		comboBoxFilterType.setToolTipText("Type of filter");
	    comboBoxFilterType.setEnabled(true);
	    comboBoxFilterType.setEditable(false);
	    comboBoxFilterType.setSelectedItem("Moving average");
	    comboBoxFilterType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_FilterType = (String)comboBoxFilterType.getSelectedItem();
						
				labelRange.setEnabled(false);
				spinnerRange.setEnabled(false);
				labelFFTRadius.setEnabled(false);
				spinnerFFTRadius.setEnabled(false);
				labelWindowingType.setEnabled(false);
				comboBoxWindowingType.setEnabled(false);
				
				if (   choiceRadioButt_FilterType.equals("Moving average")
					|| choiceRadioButt_FilterType.equals("Moving median")
					) {		
					labelRange.setEnabled(true);
					spinnerRange.setEnabled(true);
				} 
				
				if (   choiceRadioButt_FilterType.equals("Low pass - FFT")
					|| choiceRadioButt_FilterType.equals("High pass - FFT")
					) {		
					labelFFTRadius.setEnabled(true);
					spinnerFFTRadius.setEnabled(true);
					labelWindowingType.setEnabled(true);
					comboBoxWindowingType.setEnabled(true);
				}  
				
				logService.info(this.getClass().getName() + " FilterType set to " + choiceRadioButt_FilterType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFilterType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxFilterType, gbc);
	    //initialize command variable
	    choiceRadioButt_FilterType = (String)comboBoxFilterType.getSelectedItem();
			
		//*****************************************************************************************
	    labelRange = new JLabel("Range");
	    labelRange.setToolTipText("Computation range from (i-range/2) to (i+range/2)");
	    labelRange.setEnabled(true);
	    labelRange.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelRange = new SpinnerNumberModel(3, 3, 999999999, 2); // initial, min, max, step
        spinnerRange = new JSpinner(spinnerModelRange);
        spinnerRange.setToolTipText("Computation range from (i-range/2) to (i+range/2)");
        spinnerRange.setEnabled(true);
        spinnerRange.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Range = (int)spinnerRange.getValue();
            
            	if (spinnerInteger_Range % 2 == 0 ) spinnerRange.setValue(spinnerInteger_Range + 1);  //even numbers are not allowed     	
            	spinnerInteger_Range = (int)spinnerRange.getValue();
            	
                logService.info(this.getClass().getName() + " Range set to " + spinnerInteger_Range);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelRange, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerRange, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Range = (int)spinnerRange.getValue();
	    
		//*****************************************************************************************
	    labelFFTRadius = new JLabel("FFT radius");
	    labelFFTRadius.setToolTipText("Cutoff frequency - distance from frequency = 0");
	    labelFFTRadius.setEnabled(false);
	    labelFFTRadius.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelFFTRadius = new SpinnerNumberModel(10, 0, 999999999, 1); // initial, min, max, step
        spinnerFFTRadius = new JSpinner(spinnerModelFFTRadius);
        spinnerFFTRadius.setToolTipText("Cutoff frequency - distance from frequency = 0");
        spinnerFFTRadius.setEnabled(false);
        spinnerFFTRadius.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_FFTRadius = (int)spinnerFFTRadius.getValue();
            	
                logService.info(this.getClass().getName() + " FFT radius set to " + spinnerInteger_FFTRadius);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFFTRadius, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerFFTRadius, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_FFTRadius = (int)spinnerFFTRadius.getValue();
	    
	  //*****************************************************************************************
	    labelWindowingType = new JLabel("Windowing");
	    labelWindowingType.setToolTipText("FFT windowing type for FFT filtering");
	    labelWindowingType.setEnabled(false);
	    labelWindowingType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsWindowingType[] = {"Rectangular", "Cosine", "Lanczos", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"};
		comboBoxWindowingType = new JComboBox<String>(optionsWindowingType);
		comboBoxWindowingType.setToolTipText("FFT windowing type for FFT filtering");
	    comboBoxWindowingType.setEnabled(false);
	    comboBoxWindowingType.setEditable(false);
	    comboBoxWindowingType.setSelectedItem("Hanning");
	    comboBoxWindowingType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {	
				   choiceRadioButt_WindowingType = (String) comboBoxWindowingType.getSelectedItem();
				
				logService.info(this.getClass().getName() + " WindowingType set to " + (String)comboBoxWindowingType.getSelectedItem());
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelWindowingType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxWindowingType, gbc);
	    //initialize command variable
	    choiceRadioButt_WindowingType = (String) comboBoxWindowingType.getSelectedItem();
   
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
		 
		Future<CommandModule> future = commandService.run(Csaj1DFilterCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_FilterType",    choiceRadioButt_FilterType,
														"spinnerInteger_Range",          spinnerInteger_Range,
														"spinnerInteger_FFTRadius",      spinnerInteger_FFTRadius,
														"choiceRadioButt_WindowingType", choiceRadioButt_WindowingType,
														
														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
//														"spinnerInteger_NumSurrogates",  spinnerInteger_NumSurrogates,
//														"spinnerInteger_BoxLength",      spinnerInteger_BoxLength,
//														"booleanSkipZeroes",             booleanSkipZeroes,
														
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
		tableOutName = Csaj1DFilterCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
