/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajDialog_3DPlugin.java
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

package at.csa.csaj.commons;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

import net.imagej.Dataset;

/*
 * This is the super class for Csaj 3D dialogs
 */
public class CsajDialog_3DPlugin extends CsajDialog_PluginFrame {

	private static final long serialVersionUID = -4712612138325562994L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private ThreadService threadService;
	
	@Parameter
	private UIService uiService;
	
	//Input image variables
	public long width;
	public long height;
	public long depth;
	public int numDimensions;
	public int compositeChannelCount;
	public long numSlices;
	public String imageType;
	public String datasetName;
	public String[] sliceLabels;
	 
	
	public JPanel contentPanel;
	
	public JPanel  panelInput;
	public JLabel  labelInput;
	public JButton btnShowInput;
  	
  	public JPanel buttonPanelProcess;
  	
	public JCheckBox checkBoxOverwriteDisplays;
	public boolean   booleanOverwriteDisplays;
	
	public JCheckBox  checkBoxProcessImmediately;
	public boolean	  booleanProcessImmediately;

	public JButton btnProcessSingleVolume;
	
	/**
	 * Create the dialog.
	 */
	public CsajDialog_3DPlugin(Context context, Dataset datasetIn) {
		
		super();
		
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		context.inject(this); //Important
	
		//Define supported image types for over all plugins
		//String[] supportedImageTypes = {"Grey"};
		//String[] supportedImageTypes = {"RGB"};
		String[] supportedImageTypes = {"Grey", "RGB"};
				
		//Get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkVolumeDatasetIn(logService, datasetIn, supportedImageTypes);
		width  =       			(long)datasetInInfo.get("width");
		height =       			(long)datasetInInfo.get("height");
		depth  =       			(long)datasetInInfo.get("depth");
		numDimensions =         (int)datasetInInfo.get("numDimensions");
		compositeChannelCount = (int)datasetInInfo.get("compositeChannelCount");
		numSlices =             (long)datasetInInfo.get("numSlices");
		imageType =   			(String)datasetInInfo.get("imageType");
		datasetName = 			(String)datasetInInfo.get("datasetName");
		sliceLabels = 			(String[])datasetInInfo.get("sliceLabels");
			
		//NORTH item
		//*****************************************************************************************
		panelInput = new JPanel();
		panelInput.setLayout(new GridBagLayout());
		panelInput.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
	    gbc.anchor = GridBagConstraints.CENTER;
		getContentPane().add(panelInput, BorderLayout.NORTH);
		
		labelInput = new JLabel(datasetName);
		labelInput.setToolTipText("Name of input volume");
		labelInput.setHorizontalAlignment(JLabel.RIGHT);
		labelInput.setToolTipText(datasetName);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0; //IMPORTANT //This now sets the weight for the whole column 0 in the panelInput, although gpc is reset
		gbc.anchor = GridBagConstraints.WEST; //left
		panelInput.add(labelInput, gbc);
		gbc.weightx = 0.0; //reset to default
		
		//Show input button--------------------------------------------------------
		btnShowInput = new JButton("Show input volume");
		btnShowInput.setToolTipText("Show input image volume in an extra window");	
		btnShowInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				uiService.show(datasetIn.getName(), datasetIn);	
				
				labelInput.setText(datasetIn.getName());
				labelInput.revalidate();
				labelInput.repaint();
			}
		});
		//gbc.insets = standardInsets;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST; //right
		panelInput.add(btnShowInput, gbc);
			
		//CENTER default items		
	    //*****************************************************************************************
		//Specific items are declared in the sub class
		
		contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		contentPanel.setLayout(new GridBagLayout());
		//contentPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
	
		JScrollPane scrollPane = new JScrollPane(contentPanel);
	    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setToolTipText("Process options");
		separator.setName("Process options");
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy = 200;
		gbc.weightx = 1.0; //IMPORTANT //This now sets the weight for the whole column 1 in the contentPanel, although gpc is reset
		contentPanel.add(separator, gbc);
		gbc.weightx = 0.0; //reset to default
		gbc.gridwidth = 1; //reset to default
		
		JLabel labelOverWriteDisplays = new JLabel("Overwrite result display(s)");
		labelOverWriteDisplays.setToolTipText("Overwrite already existing result images, plots or tables");
		labelOverWriteDisplays.setHorizontalAlignment(JLabel.RIGHT);
		
		checkBoxOverwriteDisplays = new JCheckBox();
		checkBoxOverwriteDisplays.setToolTipText("Overwrite already existing result images, plots or tables");
		checkBoxOverwriteDisplays.setSelected(true);
		checkBoxOverwriteDisplays.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanOverwriteDisplays = checkBoxOverwriteDisplays.isSelected();	    
				logService.info(this.getClass().getName() + " Overwrite display(s) set to " + booleanOverwriteDisplays);
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 220;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOverWriteDisplays, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 220;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxOverwriteDisplays, gbc);	
	 
	    //initialize command variable
	    booleanOverwriteDisplays = checkBoxOverwriteDisplays.isSelected();	 
		
	    //*****************************************************************************************
	    JLabel labelProcessImmediate = new JLabel("Immediate processing");
	    labelProcessImmediate.setToolTipText("Immediate processing of active image whenever a parameter is changed");
	    labelProcessImmediate.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxProcessImmediately = new JCheckBox();
		checkBoxProcessImmediately.setToolTipText("Immediate processing of active image whenever a parameter is changed");
		checkBoxProcessImmediately.setSelected(false);
		checkBoxProcessImmediately.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanProcessImmediately = checkBoxProcessImmediately.isSelected();	    
				logService.info(this.getClass().getName() + " Immediate processing set to " + booleanProcessImmediately);	
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 230;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelProcessImmediate, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 230;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxProcessImmediately, gbc);	
	
	    //initialize command variable
	    booleanProcessImmediately = checkBoxProcessImmediately.isSelected();	 
	    
	    //SOUTH Process buttons panel 
	    //*****************************************************************************************
	    buttonPanelProcess = new JPanel();
		buttonPanelProcess.setLayout(new GridBagLayout());
		buttonPanelProcess.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
		getContentPane().add(buttonPanelProcess, BorderLayout.SOUTH);
	    
		//Process single button--------------------------------------------------------
		btnProcessSingleVolume = new JButton("Process volume");
		btnProcessSingleVolume.setToolTipText("Process volume");
		btnProcessSingleVolume.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				threadService.run(() -> processCommand());
			}
		});
		//gbc.insets = standardInsets;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    buttonPanelProcess.add(btnProcessSingleVolume, gbc);	
	 
	    //*****************************************************************************************
	}
	
	/**
	 * Process by calling a command
	 * Will be defined in the specific Csaj GUI
	 */
	public void processCommand() {

	}
}
