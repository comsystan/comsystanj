/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DSurrogateDialog.java
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

package at.csa.csaj.plugin3d.preproc;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import net.imagej.Dataset;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import at.csa.csaj.commons.CsajDialog_3DPlugin;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj3DSurrogateDialog extends CsajDialog_3DPlugin {

	private static final long serialVersionUID = 8182324403381019579L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private Dataset datasetIn;
	private Dataset datasetOut;
   
	//Specific dialog items
	private JComboBox<String> comboBoxSurrogateType;
	private String            choiceRadioButt_SurrogateType;
	
	private JLabel            labelWindowingType;
	private JComboBox<String> comboBoxWindowingType;
	private String            choiceRadioButt_WindowingType;
		
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
	public Csaj3DSurrogateDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("3D Surrogate");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    JLabel labelSurrogateType = new JLabel("Surrogate");
	    labelSurrogateType.setToolTipText("Type of surrogate");
	    labelSurrogateType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsSurr[] = {"Shuffle", "Gaussian", "Random phase", "AAFT"};
		comboBoxSurrogateType = new JComboBox<String>(optionsSurr);
		comboBoxSurrogateType.setToolTipText("Type of surrogate");
	    comboBoxSurrogateType.setEditable(false);
	    comboBoxSurrogateType.setSelectedItem("Shuffle");
	    comboBoxSurrogateType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_SurrogateType = (String)comboBoxSurrogateType.getSelectedItem();
				logService.info(this.getClass().getName() + " Surrogate type set to " + choiceRadioButt_SurrogateType);
				
				//Reset all spinners and option
				labelWindowingType.setEnabled(false);
				comboBoxWindowingType.setEnabled(false);
								
				if (   choiceRadioButt_SurrogateType.equals("Random phase")
					|| choiceRadioButt_SurrogateType.equals("AAFT")
				    ) {		
					labelWindowingType.setEnabled(true);
					comboBoxWindowingType.setEnabled(true);
				}
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSurrogateType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxSurrogateType, gbc);
	    //initialize command variable
	    choiceRadioButt_SurrogateType = (String)comboBoxSurrogateType.getSelectedItem();
	    
	    //*****************************************************************************************
	    labelWindowingType = new JLabel("Windowing");
	    labelWindowingType.setToolTipText("Windowing type with increasing filter strength");
	    labelWindowingType.setHorizontalAlignment(JLabel.RIGHT);
	    labelWindowingType.setEnabled(false);
	    
		String options[] = {"Rectangular", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"}; //In the order of increasing filter strength
		comboBoxWindowingType = new JComboBox<String>(options);
		comboBoxWindowingType.setToolTipText("Windowing type with increasing filter strength");
		comboBoxWindowingType.setEnabled(false);
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
		Future<CommandModule> future = commandService.run(Csaj3DSurrogateCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
							
														"choiceRadioButt_SurrogateType",  choiceRadioButt_SurrogateType,
														"choiceRadioButt_WindowingType",  choiceRadioButt_WindowingType,
													
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
		datasetOut = (Dataset)commandModule.getOutput("datasetOut");	
		uiService.show(datasetOut);
	}
}
