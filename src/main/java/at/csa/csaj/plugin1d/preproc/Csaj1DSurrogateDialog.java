/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DSurrogateDialog.java
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

import javax.swing.JComboBox;
import javax.swing.JLabel;

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
public class Csaj1DSurrogateDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = -8017995373569206664L;

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
	private JLabel			  labelSurrType;
	private JComboBox<String> comboBoxSurrType;
	
	private JLabel			  labelWindowingType;
	private JComboBox<String> comboBoxWindowingType;


	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DSurrogateDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Surrogate");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    labelSurrType = new JLabel("Surrogate type");
	    labelSurrType.setToolTipText("Surrogate type");
	    labelSurrType.setEnabled(true);
	    labelSurrType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsSurrogateType[] = {"Shuffle", "Gaussian", "Random phase", "AAFT"};
		comboBoxSurrType = new JComboBox<String>(optionsSurrogateType);
		comboBoxSurrType.setToolTipText("Surrogate type");
	    comboBoxSurrType.setEnabled(true);
	    comboBoxSurrType.setEditable(false);
	    comboBoxSurrType.setSelectedItem("Shuffle");
	    comboBoxSurrType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (   comboBoxSurrType.getSelectedItem().equals("Random phase")
					|| comboBoxSurrType.getSelectedItem().equals("AAFT")
					) {		
					labelWindowingType.setEnabled(true);
					comboBoxWindowingType.setEnabled(true);
				} else {
					labelWindowingType.setEnabled(false);
					comboBoxWindowingType.setEnabled(false);
				}
				
				logService.info(this.getClass().getName() + " Surrogate type set to " + (String)comboBoxSurrType.getSelectedItem());
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSurrType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxSurrType, gbc);
	    //initialize command variable
	    
		//*****************************************************************************************
	    labelWindowingType = new JLabel("Windowing");
	    labelWindowingType.setToolTipText("FFT windowing type for Random phase or AAFT");
	    labelWindowingType.setEnabled(false);
	    labelWindowingType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsWindowingType[] = {"Rectangular", "Cosine", "Lanczos", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"};
		comboBoxWindowingType = new JComboBox<String>(optionsWindowingType);
		comboBoxWindowingType.setToolTipText("FFT windowing type for Random phase or AAFT");
	    comboBoxWindowingType.setEnabled(false);
	    comboBoxWindowingType.setEditable(false);
	    comboBoxWindowingType.setSelectedItem("Hanning");
	    comboBoxWindowingType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {	
				logService.info(this.getClass().getName() + " WindowingType set to " + (String)comboBoxWindowingType.getSelectedItem());
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelWindowingType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxWindowingType, gbc);
	    //initialize command variable
				    
	    //*****************************************************************************************
  		//Change/Override items defined in the super class(es)
	    //Restricted options
	    labelSequenceRange.setEnabled(false);
	    labelSequenceRange.setVisible(false);
		comboBoxSequenceRange.setEnabled(false);
		comboBoxSequenceRange.setVisible(false);
			
		labelSurrogateType.setEnabled(false);
		labelSurrogateType.setVisible(false);
		comboBoxSurrogateType.setEnabled(false);
		comboBoxSurrogateType.setVisible(false);

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
		Future<CommandModule> future = commandService.run(Csaj1DSurrogateCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_SurrogateType", comboBoxSurrType.getSelectedItem(),
														"choiceRadioButt_WindowingType", comboBoxWindowingType.getSelectedItem(),

//														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
//														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
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
		tableOutName = Csaj1DSurrogateCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
