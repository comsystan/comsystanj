/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Dialog_Csaj2DPlugin.java
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

package at.csa.csaj.commons;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

import net.imagej.Dataset;

/*
 * This is the super class for Csaj 2D dialogs
 */
public class CsajDialog_2DPluginWithRegression extends CsajDialog_2DPlugin {

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private ThreadService threadService;
	
	@Parameter
	private UIService uiService;
	
	public JLabel             labelNumEps;
	public SpinnerNumberModel spinnerModelNumEps;
	public JSpinner           spinnerNumEps;
	public int                spinnerInteger_NumEps;
	
	public JSpinner spinnerNumRegStart;
	public int      spinnerInteger_NumRegStart;
	
	public JSpinner spinnerNumRegEnd;
	public int      spinnerInteger_NumRegEnd;
	
	public JCheckBox checkBoxShowDoubleLogPlot;
	public boolean   booleanShowDoubleLogPlot;
	
	
	/**
	 * Create the dialog.
	 */
	public CsajDialog_2DPluginWithRegression(Context context, Dataset datasetIn) {
		
		super(context, datasetIn);
		
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important //Context already injected
	
		//Get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkDatasetIn(logService, datasetIn);
		width  =       			(long)datasetInInfo.get("width");
		height =       			(long)datasetInInfo.get("height");
		numDimensions =         (int)datasetInInfo.get("numDimensions");
		compositeChannelCount = (int)datasetInInfo.get("compositeChannelCount");
		numSlices =             (long)datasetInInfo.get("numSlices");
		imageType =   			(String)datasetInInfo.get("imageType");
		datasetName = 			(String)datasetInInfo.get("datasetName");
		sliceLabels = 			(String[])datasetInInfo.get("sliceLabels");
			
		//CENTER regression items		
	    //*****************************************************************************************
		//Specific items are declared in the sub class
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy = 100;
		gbc.weightx = 1.0; //IMPORTANT //This now sets the weight for the whole column 1 in the contentPanel, although gpc is reset
		contentPanel.add(separator, gbc);
		gbc.weightx = 0.0; //reset to default
		gbc.gridwidth = 1; //reset to default
	    //*****************************************************************************************
	    labelNumEps = new JLabel("Scales/Boxes/Eps #"); //Override text
	    labelNumEps.setToolTipText("Number of distinct Scales/Boxes/Eps");
	    labelNumEps.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumEps.setPreferredSize(DIMENSION_ITEM_STANDARD);
	   
	    //Override
	    //Model may be overwritten with new parameters (e.g.maximum) in sub class
	    spinnerModelNumEps= new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step   
	   
	    spinnerNumEps = new JSpinner(spinnerModelNumEps);
        spinnerNumEps.setToolTipText("Number of distinct Scales/Boxes/Eps");
        spinnerNumEps.setPreferredSize(DIMENSION_ITEM_STANDARD);
        spinnerNumEps.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {  		
        		int valueNumEps      = (int)spinnerNumEps.getValue();
        		int valueNumRegStart = (int)spinnerNumRegStart.getValue();
        		int valueNumRegEnd   = (int)spinnerNumRegEnd.getValue();   		
        
        		if (valueNumEps < 3) valueNumEps = 3;	
    			spinnerNumEps.setValue(valueNumEps);
    			spinnerInteger_NumEps = (int)spinnerNumEps.getValue();
    			logService.info(this.getClass().getName() + " Number of Scales/Boxes/Eps set to " + spinnerInteger_NumEps);
        	
        		if (valueNumRegEnd > valueNumEps) {
        			valueNumRegEnd = valueNumEps;
        			spinnerNumRegEnd.setValue(valueNumRegEnd);
        			spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();
        			logService.info(this.getClass().getName() + " Regression end set to " + spinnerInteger_NumRegEnd);
        		}
        		if (valueNumRegStart >= valueNumRegEnd - 2) {
        			valueNumRegStart = valueNumRegEnd - 2;
        			spinnerNumRegStart.setValue(valueNumRegStart);
        			spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();
        			logService.info(this.getClass().getName() + " Regression start set to " + spinnerInteger_NumRegStart);
        		}	
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 110;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumEps, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 110;   
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumEps, gbc);	
	  
	    //initialize command variable
	    spinnerInteger_NumEps = (int)spinnerNumEps.getValue();
	    
	    //*****************************************************************************************
	    JLabel labelNumRegStart = new JLabel("Regresssion start");
	    labelNumRegStart.setToolTipText("Minimum number of linear regression");
	    labelNumRegStart.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumRegStart.setPreferredSize(DIMENSION_ITEM_STANDARD);
	   
	    SpinnerNumberModel spinnerModelNumRegStart= new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
	    spinnerNumRegStart = new JSpinner(spinnerModelNumRegStart);
        spinnerNumRegStart.setToolTipText("Minimum number of linear regression");
        spinnerNumRegStart.setPreferredSize(DIMENSION_ITEM_STANDARD);
        spinnerNumRegStart.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
        		int valueNumRegStart = (int)spinnerNumRegStart.getValue();
        		int valueNumRegEnd   = (int)spinnerNumRegEnd.getValue();   		
        		
        		if (valueNumRegStart >= valueNumRegEnd - 2) valueNumRegStart  = valueNumRegEnd - 2;	
        		if (valueNumRegStart < 1) valueNumRegStart = 1;
        		
        		spinnerNumRegStart.setValue(valueNumRegStart);
    			spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();
    			logService.info(this.getClass().getName() + " Regression start set to " + spinnerInteger_NumRegStart);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 120;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumRegStart, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 120;   
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumRegStart, gbc);	
	  
	    //initialize command variable
	    spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();
	    
	    //*****************************************************************************************
	    JLabel labelNumRegEnd = new JLabel("Regresssion end");
	    labelNumRegEnd.setToolTipText("Maximum number of linear regression");
	    labelNumRegEnd.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumRegEnd.setPreferredSize(DIMENSION_ITEM_STANDARD);
	   
	    SpinnerNumberModel spinnerModelNumRegEnd= new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
	    spinnerNumRegEnd = new JSpinner(spinnerModelNumRegEnd);
        spinnerNumRegEnd.setToolTipText("Maximum number of linear regression");
        spinnerNumRegEnd.setPreferredSize(DIMENSION_ITEM_STANDARD);
        spinnerNumRegEnd.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
        		int valueNumEps      = (int)spinnerNumEps.getValue();
        		int valueNumRegStart = (int)spinnerNumRegStart.getValue();
        		int valueNumRegEnd   = (int)spinnerNumRegEnd.getValue();   		
        	
        		if (valueNumRegEnd <= valueNumRegStart + 2) valueNumRegEnd  = valueNumRegStart + 2;
        		if (valueNumRegEnd > valueNumEps) valueNumRegEnd = valueNumEps;
  
        		spinnerNumRegEnd.setValue(valueNumRegEnd);
    			spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();
    			logService.info(this.getClass().getName() + " Regression end set to " + spinnerInteger_NumRegEnd);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 130;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumRegEnd, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 130;   
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumRegEnd, gbc);	
	  
	    //initialize command variable
	    spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();
	    
	    //*****************************************************************************************
	    JLabel labelShowDoubleLogPlot = new JLabel("Show double log plot");
	    labelShowDoubleLogPlot.setToolTipText("Show double log linear regression plot");
	    labelShowDoubleLogPlot.setHorizontalAlignment(JLabel.RIGHT);
	    labelShowDoubleLogPlot.setPreferredSize(DIMENSION_ITEM_STANDARD);
	    
		checkBoxShowDoubleLogPlot = new JCheckBox();
		checkBoxShowDoubleLogPlot.setToolTipText("Show double log linear regression plot");
		checkBoxShowDoubleLogPlot.setSelected(true);
		checkBoxShowDoubleLogPlot.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanShowDoubleLogPlot = checkBoxShowDoubleLogPlot.isSelected();
		    	logService.info(this.getClass().getName() + " Show double log plot set to " + booleanShowDoubleLogPlot);
		    	if (booleanProcessImmediately) btnProcessSingleImage.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 140;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelShowDoubleLogPlot, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 140;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxShowDoubleLogPlot, gbc);	
	    //initialize command variable
	    booleanShowDoubleLogPlot = checkBoxShowDoubleLogPlot.isSelected();
	 
	    //*****************************************************************************************
	}
	
	/**
	 * Process by calling a command
	 * Will be defined in the specific Csaj GUI
	 */
	public void processCommand() {

	}
}
