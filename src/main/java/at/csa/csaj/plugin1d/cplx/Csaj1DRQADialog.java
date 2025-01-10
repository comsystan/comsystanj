/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DRQADialog.java
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

package at.csa.csaj.plugin1d.cplx;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;
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
public class Csaj1DRQADialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = -5340024902066872219L;

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
	private JLabel   labelEmbDim;
	private JSpinner spinnerEmbDim;
	private int      spinnerInteger_EmbDim;
	
	private JLabel   labelTau;
	private JSpinner spinnerTau;
	private int      spinnerInteger_Tau;
	
	private JLabel   labelEps;
	private JSpinner spinnerEps;
	private float    spinnerFloat_Eps;
	
	private JComboBox<String> comboBoxNorm;
	private String   choiceRadioButt_Norm;
	
	private JLabel    labelShowReccurenceMatrix;
	private JCheckBox checkBoxShowReccurenceMatrix;
 	private boolean   booleanShowReccurenceMatrix;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DRQADialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Recurrence quantification analysis");

		//Add specific GUI elements according to Command @Parameter GUI elements	
		//*****************************************************************************************
	    labelEmbDim = new JLabel("Embedding dimension");
	    labelEmbDim.setToolTipText("Embedding dimension of phase space");
	    labelEmbDim.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelEmbDim = new SpinnerNumberModel(2, 1, 999999999, 1); // initial, min, max, step
        spinnerEmbDim = new JSpinner(spinnerModelEmbDim);
        spinnerEmbDim.setToolTipText("Embedding dimension of phase space");
        spinnerEmbDim.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_EmbDim = (int)spinnerEmbDim.getValue();
                logService.info(this.getClass().getName() + " Embedding dimension set to " + spinnerInteger_EmbDim);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelEmbDim, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerEmbDim, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_EmbDim = (int)spinnerEmbDim.getValue();
	    
		//*****************************************************************************************
	    labelTau = new JLabel("Tau");
	    labelTau.setToolTipText("Time lag/delay of phase space reconstruction");
	    labelTau.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelTau = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerTau = new JSpinner(spinnerModelTau);
        spinnerTau.setToolTipText("Time lag/delay of phase space reconstruction");
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
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelTau, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerTau, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Tau = (int)spinnerTau.getValue();
	    
		//*****************************************************************************************
	    labelEps = new JLabel("Eps");
	    labelEps.setToolTipText("Epsilon of neighboring data points");
	    labelEps.setEnabled(true);
	    labelEps.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelEps = new SpinnerNumberModel(0.001, 0.0, 999999999.0, 0.01); // initial, min, max, step
        spinnerEps = new JSpinner(spinnerModelEps);
        spinnerEps.setToolTipText("Epsilon of neighboring data points");
        spinnerEps.setEnabled(true);
        spinnerEps.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {  
            	spinnerFloat_Eps = (float)((SpinnerNumberModel)spinnerEps.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Epsilon set to " + spinnerFloat_Eps);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelEps, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerEps, gbc);	    
	    
	    //initialize command variable
	    spinnerFloat_Eps = (float)((SpinnerNumberModel)spinnerEps.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    JLabel labelNorm = new JLabel("Norm");
	    labelNorm.setToolTipText("Type of norm (distance)");
	    labelNorm.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsNorm[] = {"Euclidean", "Maximum", "Cityblock", "Angular"};
		comboBoxNorm = new JComboBox<String>(optionsNorm);
		comboBoxNorm.setToolTipText("Type of norm (distance)");
	    comboBoxNorm.setEditable(false);
	    comboBoxNorm.setSelectedItem("Euclidean");
	    comboBoxNorm.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_Norm = (String)comboBoxNorm.getSelectedItem();
				logService.info(this.getClass().getName() + " Norm set to " + choiceRadioButt_Norm);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNorm, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxNorm, gbc);
	    //initialize command variable
	    choiceRadioButt_Norm = (String)comboBoxNorm.getSelectedItem();
	    
	    //*****************************************************************************************
 		labelShowReccurenceMatrix = new JLabel("Show reccurence matrix");
 		labelShowReccurenceMatrix.setToolTipText("Show reccurence matrix");
 		labelShowReccurenceMatrix.setHorizontalAlignment(JLabel.RIGHT);
 		labelShowReccurenceMatrix.setEnabled(true);
 		
 		checkBoxShowReccurenceMatrix = new JCheckBox();
 		checkBoxShowReccurenceMatrix.setToolTipText("Show reccurence matrix");
 		checkBoxShowReccurenceMatrix.setEnabled(true);
 		checkBoxShowReccurenceMatrix.setSelected(true);
 		checkBoxShowReccurenceMatrix.addItemListener(new ItemListener() {
 			@Override
 		    public void itemStateChanged(ItemEvent e) {
 		    	booleanShowReccurenceMatrix = checkBoxShowReccurenceMatrix.isSelected();	    
 				logService.info(this.getClass().getName() + " Show reccurence matrix option set to " + booleanShowReccurenceMatrix);
 				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
 		    }
 		});
 		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
 	    gbc.gridy = 4;
 	    gbc.anchor = GridBagConstraints.EAST; //right
 	    contentPanel.add(labelShowReccurenceMatrix, gbc);
 	    gbc.gridx = 1;
 	    gbc.gridy = 4;
 	    gbc.anchor = GridBagConstraints.WEST; //left
 	    contentPanel.add(checkBoxShowReccurenceMatrix, gbc);	
 	 
 	    //initialize command variable
 	    booleanShowReccurenceMatrix = checkBoxShowReccurenceMatrix.isSelected();
		
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
		 
		Future<CommandModule> future = commandService.run(Csaj1DRQACmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
				
														"spinnerInteger_EmbDim",         spinnerInteger_EmbDim,
														"spinnerInteger_Tau",            spinnerInteger_Tau,
														"spinnerFloat_Eps",              spinnerFloat_Eps,
														"choiceRadioButt_Norm",			 choiceRadioButt_Norm,
														"booleanShowReccurenceMatrix",	 booleanShowReccurenceMatrix,
														
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
		tableOutName = Csaj1DRQACmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
