/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DKolmogorovComplexityDialog.java
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
public class Csaj1DKolmogorovComplexityDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 1615176757250845200L;

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
	private JPanel       panelCompressionType;
	private ButtonGroup  buttonGroupCompressionType;
    private JRadioButton radioButtonZLIB;
    private JRadioButton radioButtonGZIB;
	private String       choiceRadioButt_CompressionType;
	
	private JSpinner spinnerNumIterations;
	private int      spinnerInteger_NumIterations;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DKolmogorovComplexityDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Kolmogorov complexity and LD");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelCompressionType = new JLabel("Compression type");
	    labelCompressionType.setToolTipText("Compresson type to estimate KC");
	    labelCompressionType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupCompressionType = new ButtonGroup();
		radioButtonZLIB      = new JRadioButton("ZLIB");
		radioButtonGZIB      = new JRadioButton("GZIB");
		radioButtonZLIB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonZLIB.isSelected()) {
					choiceRadioButt_CompressionType = radioButtonZLIB.getText();
				} 
				logService.info(this.getClass().getName() + " Compression type set to " + choiceRadioButt_CompressionType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonGZIB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonGZIB.isSelected()) {
					choiceRadioButt_CompressionType = radioButtonGZIB.getText();
				}
				logService.info(this.getClass().getName() + " Compression type set to " + choiceRadioButt_CompressionType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupCompressionType.add(radioButtonZLIB);
		buttonGroupCompressionType.add(radioButtonGZIB);
		radioButtonZLIB.setSelected(true);
		
		panelCompressionType = new JPanel();
		panelCompressionType.setToolTipText("Compresson type to estimate KC");
		panelCompressionType.setLayout(new BoxLayout(panelCompressionType, BoxLayout.Y_AXIS)); 
	    panelCompressionType.add(radioButtonZLIB);
	    panelCompressionType.add(radioButtonGZIB);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelCompressionType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelCompressionType, gbc);
	    //initialize command variable
		if (radioButtonZLIB.isSelected())  choiceRadioButt_CompressionType = radioButtonZLIB.getText();
		if (radioButtonGZIB.isSelected()) choiceRadioButt_CompressionType = radioButtonGZIB.getText();
			
		//*****************************************************************************************
	    JLabel labelIteration = new JLabel("Iterations for LD");
	    labelIteration.setToolTipText("Number of compressions to compute averages");
	    labelIteration.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelIteration = new SpinnerNumberModel(10, 1, 999999999, 1); // initial, min, max, step
        spinnerNumIterations = new JSpinner(spinnerModelIteration);
        spinnerNumIterations.setToolTipText("Number of compressions to compute averages");
        spinnerNumIterations.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();
                logService.info(this.getClass().getName() + " Number of iterations set to " + spinnerInteger_NumIterations);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelIteration, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumIterations, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();
		    
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj1DKolmogorovComplexityCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_CompressionType", choiceRadioButt_CompressionType,
														"spinnerInteger_NumIterations",    spinnerInteger_NumIterations,
														
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
		tableOutName = Csaj1DKolmogorovComplexityCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
