/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DStatCplxMeasDialog.java
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

package at.csa.csaj.plugin3d.cplx;

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
import net.imagej.Dataset;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.table.DefaultGenericTable;
import org.scijava.ui.UIService;
import at.csa.csaj.commons.CsajDialog_3DPlugin;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj3DStatCplxMeasDialog extends CsajDialog_3DPlugin {

	private static final long serialVersionUID = -4458604160603507243L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private Dataset datasetIn;
  	private String tableOutName;
	private DefaultGenericTable tableOut;
   
	//Specific dialog items
  	private JComboBox<String> comboBoxProbabilityType;
	private String   choiceRadioButt_ProbabilityType;
	
	private JLabel   labelLag;
	private JSpinner spinnerLag;
	private int      spinnerInteger_Lag;
	
	private JLabel    labelNormaliseH;
	private JCheckBox checkBoxNormaliseH;
	private boolean   booleanNormaliseH;
	
	private JLabel    labelNormaliseD;
	private JCheckBox checkBoxNormaliseD;
	private boolean   booleanNormaliseD;
	
	private JLabel    labelSkipZeroes;
	private JCheckBox checkBoxSkipZeroes;
	private boolean   booleanSkipZeroes;
	
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
	public Csaj3DStatCplxMeasDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("3D Statistical complexity measures");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelProbabilityType = new JLabel("Probability type");
	    labelProbabilityType.setToolTipText("Selection of probability type");
	    labelProbabilityType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Grey values", "Pairwise differences"};// "Sum of differences", "SD"}, 
		comboBoxProbabilityType = new JComboBox<String>(options);
		comboBoxProbabilityType.setToolTipText("Selection of probability type");
	    comboBoxProbabilityType.setEditable(false);
	    comboBoxProbabilityType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_ProbabilityType = (String)comboBoxProbabilityType.getSelectedItem();
				logService.info(this.getClass().getName() + " Probability type set to " + choiceRadioButt_ProbabilityType);
				//Lag must always be 1 for Sequence values
				if (choiceRadioButt_ProbabilityType.equals("Grey values")) {
					labelLag.setEnabled(false);
					spinnerLag.setEnabled(false);	
					spinnerLag.setValue(1);
				}
				else {
					labelLag.setEnabled(true);
					spinnerLag.setEnabled(true);	
				}
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelProbabilityType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxProbabilityType, gbc);
	    //initialize command variable
	    choiceRadioButt_ProbabilityType = (String)comboBoxProbabilityType.getSelectedItem();
	    
	    //*****************************************************************************************
	    labelLag = new JLabel("Lag");
	    labelLag.setToolTipText("Delta (difference) between two data points");
	    labelLag.setHorizontalAlignment(JLabel.RIGHT);
	    labelLag.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelLag = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerLag = new JSpinner(spinnerModelLag);
        spinnerLag.setToolTipText("Delta (difference) between two data points");
        spinnerLag.setEnabled(false);
        spinnerLag.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Lag = (int)spinnerLag.getValue();
            	
            	if ((spinnerInteger_Lag > 1) && (String)comboBoxProbabilityType.getSelectedItem() == "Grey values") {
        			spinnerLag.setValue(1);
                	logService.info(this.getClass().getName() + " Lag > 1 not possible for Grey values");
                }
                
            	spinnerInteger_Lag = (int)spinnerLag.getValue();
                logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_Lag);
                if (booleanProcessImmediately) btnProcessSingleVolume .doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelLag, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerLag, gbc);	    
	    //initialize command variable
	    spinnerInteger_Lag = (int)spinnerLag.getValue();
	    
	    //*****************************************************************************************
	    labelNormaliseH = new JLabel("Normalise H");
	    labelNormaliseH.setToolTipText("Normalisation of Shannon entropy H");
	    labelNormaliseH.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxNormaliseH = new JCheckBox();
		checkBoxNormaliseH.setToolTipText("Normalisation of Shannon entropy H");
		checkBoxNormaliseH.setSelected(true);
		checkBoxNormaliseH.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanNormaliseH = checkBoxNormaliseH.isSelected();	    
				logService.info(this.getClass().getName() + " Normalisation of H set to " + booleanNormaliseH);	
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNormaliseH, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxNormaliseH, gbc);	
	
	    //initialize command variable
	    booleanNormaliseH = checkBoxNormaliseH.isSelected();	
	    
	    //*****************************************************************************************
	    labelNormaliseD = new JLabel("Normalise D");
	    labelNormaliseD.setToolTipText("Normalisation of statistical distribution distance D");
	    labelNormaliseD.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxNormaliseD = new JCheckBox();
		checkBoxNormaliseD.setToolTipText("Normalisation of statistical distribution distance D");
		checkBoxNormaliseD.setSelected(true);
		checkBoxNormaliseD.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanNormaliseD = checkBoxNormaliseD.isSelected();	    
				logService.info(this.getClass().getName() + " Normalisation of D set to " + booleanNormaliseD);	
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNormaliseD, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxNormaliseD, gbc);	
	
	    //initialize command variable
	    booleanNormaliseD = checkBoxNormaliseD.isSelected();	
	    
	    //*****************************************************************************************
	    labelSkipZeroes = new JLabel("Skip zero values");
	    labelSkipZeroes.setToolTipText("Delete zeroes or not");
	    labelSkipZeroes.setHorizontalAlignment(JLabel.RIGHT);
	    
		checkBoxSkipZeroes = new JCheckBox();
		checkBoxSkipZeroes.setToolTipText("Delete zeroes or not");
		checkBoxSkipZeroes.setSelected(false);
		checkBoxSkipZeroes.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanSkipZeroes = checkBoxSkipZeroes.isSelected();
		    	logService.info(this.getClass().getName() + " Skip zeroes option set to " + booleanSkipZeroes);
		    	if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSkipZeroes, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxSkipZeroes, gbc);	
	    //initialize command variable
	    booleanSkipZeroes = checkBoxSkipZeroes.isSelected();
	   
	    //*****************************************************************************************
		//Change items defined in the super class(es)
		
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj3DStatCplxMeasCmd.class, false,
														"datasetIn",                        datasetIn,  //is not automatically harvested in headless mode
														
														"choiceRadioButt_ProbabilityType",	choiceRadioButt_ProbabilityType,
														"spinnerInteger_Lag",				spinnerInteger_Lag,
														"booleanNormaliseH",				booleanNormaliseH,
														"booleanNormaliseD",				booleanNormaliseD,
														"booleanSkipZeroes",                booleanSkipZeroes,
															
														"booleanOverwriteDisplays",			booleanOverwriteDisplays,
														"booleanProcessImmediately",		booleanProcessImmediately
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
		tableOutName = Csaj3DStatCplxMeasCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
