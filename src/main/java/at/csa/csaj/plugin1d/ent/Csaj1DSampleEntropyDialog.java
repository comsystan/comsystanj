/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DSampleEntropyDialog.java
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

package at.csa.csaj.plugin1d.ent;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
public class Csaj1DSampleEntropyDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 6243623536761241706L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
	private JPanel       panelEntropyType;
	private ButtonGroup  buttonGroupEntropyType;
    private JRadioButton radioButtonSampEnt;
    private JRadioButton radioButtonAppEnt;
	private String       choiceRadioButt_EntropyType;
	
  	private DefaultTableDisplay defaultTableDisplay;
  	private String tableOutName;
	private DefaultGenericTable tableOut;
   
	//Specific dialog items
	private JSpinner spinnerParamM;
	private int      spinnerInteger_ParamM;

	private JSpinner spinnerParamR;
	private float    spinnerFloat_ParamR;
	
	private JSpinner spinnerParamD;
	private int      spinnerInteger_ParamD;


	
	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DSampleEntropyDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Sample entropy");

		//Add specific GUI elements according to Command @Parameter GUI elements  
		  //*****************************************************************************************		
	    JLabel labelEntropyType = new JLabel("Entropy type");
	    labelEntropyType.setToolTipText("Sample entropy or Approximate entropy");
	    labelEntropyType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupEntropyType = new ButtonGroup();
		radioButtonSampEnt      = new JRadioButton("Sample entropy");
		radioButtonAppEnt      = new JRadioButton("Approximate entropy");
		radioButtonSampEnt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonSampEnt.isSelected()) {
					choiceRadioButt_EntropyType = radioButtonSampEnt.getText();
				} 
				logService.info(this.getClass().getName() + " Entropy type set to " + choiceRadioButt_EntropyType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonAppEnt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonAppEnt.isSelected()) {
					choiceRadioButt_EntropyType = radioButtonAppEnt.getText();
				}
				logService.info(this.getClass().getName() + " Entropy type set to " + choiceRadioButt_EntropyType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupEntropyType.add(radioButtonSampEnt);
		buttonGroupEntropyType.add(radioButtonAppEnt);
		radioButtonSampEnt.setSelected(true);
		
		panelEntropyType = new JPanel();
		panelEntropyType.setToolTipText("Sample entropy or Approximate entropy");
		panelEntropyType.setLayout(new BoxLayout(panelEntropyType, BoxLayout.Y_AXIS)); 
	    panelEntropyType.add(radioButtonSampEnt);
	    panelEntropyType.add(radioButtonAppEnt);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelEntropyType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelEntropyType, gbc);
	    //initialize command variable
		if (radioButtonSampEnt.isSelected()) choiceRadioButt_EntropyType = radioButtonSampEnt.getText();
		if (radioButtonAppEnt.isSelected())  choiceRadioButt_EntropyType = radioButtonAppEnt.getText();
		
	    //*****************************************************************************************
	    JLabel labelParamM = new JLabel("m");
	    labelParamM.setToolTipText("Parameter m");
	    labelParamM.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelParamM = new SpinnerNumberModel(2, 1, 100, 1); // initial, min, max, step
        spinnerParamM = new JSpinner(spinnerModelParamM);
        spinnerParamM.setToolTipText("Parameter m");
        spinnerParamM.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_ParamM = (int)spinnerParamM.getValue();
            
                logService.info(this.getClass().getName() + " Param m set to " + spinnerInteger_ParamM);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelParamM, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerParamM, gbc);	    
	    //initialize command variable
	    spinnerInteger_ParamM = (int)spinnerParamM.getValue();  
	    
	    //*****************************************************************************************
	    JLabel labelParamR = new JLabel("r");
	    labelParamR.setToolTipText("Param r");
	    labelParamR.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelParamR = new SpinnerNumberModel(0.15, 0.05, 1.0, 0.05); // initial, min, max, step
        spinnerParamR = new JSpinner(spinnerModelParamR);
        spinnerParamR.setToolTipText("Param r");
        spinnerParamR.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_ParamR = (float)((SpinnerNumberModel)spinnerParamR.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Param r set to " + spinnerFloat_ParamR);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelParamR, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerParamR, gbc);	    
	    //initialize command variable
	    spinnerFloat_ParamR = (float)((SpinnerNumberModel)spinnerParamR.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    JLabel labelParamD = new JLabel("d");
	    labelParamD.setToolTipText("Parameter d");
	    labelParamD.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelParamD = new SpinnerNumberModel(1, 1, 100, 1); // initial, min, max, step
        spinnerParamD = new JSpinner(spinnerModelParamD);
        spinnerParamD.setToolTipText("Parameter d");
        spinnerParamD.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_ParamD = (int)spinnerParamD.getValue();
            
                logService.info(this.getClass().getName() + " Param d set to " + spinnerInteger_ParamD);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelParamD, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerParamD, gbc);	    
	    //initialize command variable
	    spinnerInteger_ParamD = (int)spinnerParamD.getValue(); 
	        
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
		Future<CommandModule> future = commandService.run(Csaj1DSampleEntropyCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
															
														"choiceRadioButt_EntropyType",   choiceRadioButt_EntropyType,
														"spinnerInteger_ParamM",		 spinnerInteger_ParamM,
														"spinnerFloat_ParamR",			 spinnerFloat_ParamR,
														"spinnerInteger_ParamD",	     spinnerInteger_ParamD,
																				
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
		tableOutName = Csaj1DSampleEntropyCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
