/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DSegRGBRelDialog.java
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
public class Csaj2DSegRGBRelDialog extends CsajDialog_2DPlugin {

	private static final long serialVersionUID = 1949726182650003540L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private Dataset datasetIn;
	private Dataset datasetOut;
   
	//Specific dialog items
	private JComboBox<String> comboBoxRatioType;
	private String            choiceRadioButt_RatioType;
	
	private JLabel   labelRatio;
	private JSpinner spinnerRatio;
	private float    spinnerFloat_Ratio;
	
	
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
	public Csaj2DSegRGBRelDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D RGB relative segmentation");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    JLabel labelRatioType = new JLabel("RGB channel ratio type");
	    labelRatioType.setToolTipText("Type of relative RGB ratio");
	    labelRatioType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"R/(R+G+B)", "R/(G+B)", "R/G", "R/B", "G/(R+G+B)", "G/(R+B)", "G/R", "G/B", "B/(R+G+B)", "B/(R+G)", "B/R", "B/G"};
		comboBoxRatioType = new JComboBox<String>(options);
		comboBoxRatioType.setToolTipText("Type of relative RGB ratio");
	    comboBoxRatioType.setEditable(false);
	    comboBoxRatioType.setSelectedItem("R/(R+G+B)");
	    comboBoxRatioType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_RatioType = (String)comboBoxRatioType.getSelectedItem();
				logService.info(this.getClass().getName() + " RGB ratio type set to " + choiceRadioButt_RatioType);
				
//				//Reset all spinners and options
//				labelRatio.setEnabled(false);
//				spinnerRatio.setEnabled(false);
//								
//				if (   choiceRadioButt_RatioType.equals("R/(R+G+B)")
//					|| choiceRadioButt_RatioType.equals("G/(R+G+B)")
//					|| choiceRadioButt_RatioType.equals("B/(R+G+B)")			
//				    ) {		
//					labelRatio.setEnabled(true);
//					spinnerRatio.setEnabled(true);
//				}	
				
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelRatioType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxRatioType, gbc);
	    //initialize command variable
	    choiceRadioButt_RatioType = (String)comboBoxRatioType.getSelectedItem();
	    
	    //*****************************************************************************************
	    labelRatio = new JLabel("Ratio threshold");
	    labelRatio.setToolTipText("Relative ratio threshold");
	    labelRatio.setHorizontalAlignment(JLabel.RIGHT);
	    labelRatio.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelRatio = new SpinnerNumberModel(0.5, 0.0, 1.0, 0.01); // initial, min, max, step
        spinnerRatio = new JSpinner(spinnerModelRatio);
        spinnerRatio.setToolTipText("Relative ratio threshold");
        spinnerRatio.setEnabled(true);
        spinnerRatio.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_Ratio = (float)((SpinnerNumberModel)spinnerRatio.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Ratio set to " + spinnerFloat_Ratio);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelRatio, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerRatio, gbc);	    
	    //initialize command variable
	    spinnerFloat_Ratio = (float)((SpinnerNumberModel)spinnerRatio.getModel()).getNumber().doubleValue();
	    
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
		 
		Future<CommandModule> future = commandService.run(Csaj2DSegRGBRelCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
							
														"choiceRadioButt_RatioType",      choiceRadioButt_RatioType,
														"spinnerFloat_Ratio",        	  spinnerFloat_Ratio,    
														
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
