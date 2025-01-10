/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DGeneralisedDFADialog.java
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;
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
import at.csa.csaj.commons.CsajDialog_1DPluginWithRegression;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj1DGeneralisedDFADialog extends CsajDialog_1DPluginWithRegression {

	private static final long serialVersionUID = 5888044633135973042L;

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
	private JSpinner spinnerMinQ;
	private int      spinnerInteger_MinQ;

	private JSpinner spinnerMaxQ;
	private int      spinnerInteger_MaxQ;
	
	private JLabel    labelShowHPlot;
	private JCheckBox checkBoxShowHPlot;
	private boolean   booleanShowHPlot;
    
	private JLabel    labelShowFSpectrum;
	private JCheckBox checkBoxShowFSpectrum;
 	private boolean   booleanShowFSpectrum;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DGeneralisedDFADialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Generalised DFA");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelMinQ = new JLabel("Min q");
	    labelMinQ.setToolTipText("Minimum q of generalised DFA");
	    labelMinQ.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMinQ = new SpinnerNumberModel(-5, -1000, 1000, 1); // initial, min, max, step
        spinnerMinQ = new JSpinner(spinnerModelMinQ);
        spinnerMinQ.setToolTipText("Minimum q of generalised DFA");
        spinnerMinQ.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_MinQ = (int)spinnerMinQ.getValue();
            	spinnerInteger_MaxQ = (int)spinnerMaxQ.getValue();      
            	if (spinnerInteger_MinQ >= spinnerInteger_MaxQ) {
            		spinnerMinQ.setValue((int)spinnerMaxQ.getValue() - 1);
            		spinnerInteger_MinQ = (int)spinnerMinQ.getValue();
            	}       	
                logService.info(this.getClass().getName() + " MinQ set to " + spinnerInteger_MinQ);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMinQ, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMinQ, gbc);	    
	    //initialize command variable
	    spinnerInteger_MinQ = (int)spinnerMinQ.getValue();  
	    
	    //*****************************************************************************************
	    JLabel labelMaxQ = new JLabel("Max q");
	    labelMaxQ.setToolTipText("Maximum q of generalised DFA");
	    labelMaxQ.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMaxQ = new SpinnerNumberModel(5, -1000, 1000, 1); // initial, min, max, step
        spinnerMaxQ = new JSpinner(spinnerModelMaxQ);
        spinnerMaxQ.setToolTipText("Maximum q of generalised DFA");
        spinnerMaxQ.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_MaxQ = (int)spinnerMaxQ.getValue();
            	spinnerInteger_MinQ = (int)spinnerMinQ.getValue();
            	if (spinnerInteger_MaxQ <= spinnerInteger_MinQ) {
            		spinnerMaxQ.setValue((int)spinnerMinQ.getValue() + 1);
            		spinnerInteger_MaxQ = (int)spinnerMaxQ.getValue();
            	}
                logService.info(this.getClass().getName() + " MaxQ set to " + spinnerInteger_MaxQ);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMaxQ, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMaxQ, gbc);	    
	    //initialize command variable
	    spinnerInteger_MaxQ = (int)spinnerMaxQ.getValue();    
	    
	    //*****************************************************************************************
		labelShowHPlot = new JLabel("Show h[q] plot");
		labelShowHPlot.setToolTipText("Show plot of generalised h[q]");
		labelShowHPlot.setHorizontalAlignment(JLabel.RIGHT);
		labelShowHPlot.setEnabled(true);
		
		checkBoxShowHPlot = new JCheckBox();
		checkBoxShowHPlot.setToolTipText("Show plot of generalised h[q]");
		checkBoxShowHPlot.setEnabled(true);
		checkBoxShowHPlot.setSelected(true);
		checkBoxShowHPlot.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanShowHPlot = checkBoxShowHPlot.isSelected();	    
				logService.info(this.getClass().getName() + " Show h[q] plot option set to " + booleanShowHPlot);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelShowHPlot, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxShowHPlot, gbc);	
	 
	    //initialize command variable
	    booleanShowHPlot = checkBoxShowHPlot.isSelected();	 
	    
	    //*****************************************************************************************
		labelShowFSpectrum = new JLabel("Show F spectrum");
		labelShowFSpectrum.setToolTipText("Show F spectrum");
		labelShowFSpectrum.setHorizontalAlignment(JLabel.RIGHT);
		labelShowFSpectrum.setEnabled(true);
		
		checkBoxShowFSpectrum = new JCheckBox();
		checkBoxShowFSpectrum.setToolTipText("Show F spectrum");
		checkBoxShowFSpectrum.setEnabled(true);
		checkBoxShowFSpectrum.setSelected(true);
		checkBoxShowFSpectrum.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanShowFSpectrum = checkBoxShowFSpectrum.isSelected();	    
				logService.info(this.getClass().getName() + " Show F spectrum option set to " + booleanShowFSpectrum);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelShowFSpectrum, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxShowFSpectrum, gbc);	
	 
	    //initialize command variable
	    booleanShowFSpectrum = checkBoxShowFSpectrum.isSelected();
		
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Window size max");
		//int numEpsMax = Csaj1DDFACmd.getMaxBoxNumber(numRows);
		spinnerModelNumEps = new SpinnerNumberModel(8, 4, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
	
		spinnerModelNumRegStart = new SpinnerNumberModel(4, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegStart.setModel(spinnerModelNumRegStart);
	
		spinnerModelNumRegEnd = new SpinnerNumberModel(8, 2, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegEnd.setModel(spinnerModelNumRegEnd);
		
		spinnerInteger_NumEps      = (int)spinnerNumEps.getValue();
		spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();	
		spinnerInteger_NumRegEnd   = (int)spinnerNumRegEnd.getValue();	
		
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj1DGeneralisedDFACmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"spinnerInteger_MinQ",           spinnerInteger_MinQ,
														"spinnerInteger_MaxQ",           spinnerInteger_MaxQ,
														"booleanShowHPlot",              booleanShowHPlot,
														"booleanShowFSpectrum",          booleanShowFSpectrum,
														
														"spinnerInteger_WinSizeMax",     spinnerInteger_NumEps,
														"spinnerInteger_NumRegStart",    spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",      spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",      booleanShowDoubleLogPlot,
														
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
		tableOutName = Csaj1DGeneralisedDFACmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
