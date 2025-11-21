/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DOutliersDialog.java
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

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
public class Csaj2DOutliersDialog extends CsajDialog_2DPlugin {

	private static final long serialVersionUID = 7797396069960009585L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private Dataset datasetIn;
	private Dataset datasetOut;
   
	//Specific dialog items
	private JPanel       panelExtractionType;
	private ButtonGroup  buttonGroupExtractionType;
    private JRadioButton radioButtonEliminate;
    private JRadioButton radioButtonExtract;
	private String       choiceRadioButt_ExtractionType;
	
	private JLabel			  labelDetectionType;
	private JComboBox<String> comboBoxDetectionType;
	private String            choiceRadioButt_DetectionType;
	
	private JLabel   labelZscore;
	private JSpinner spinnerZscore;
	private int      spinnerInteger_Zscore;
	
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
	public Csaj2DOutliersDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Outliers");

		//Add specific GUI elements according to Command @Parameter GUI elements
		//*****************************************************************************************
	    JLabel labelExtractionType = new JLabel("Extraction");
	    labelExtractionType.setToolTipText("Extraction type");
	    labelExtractionType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupExtractionType = new ButtonGroup();
		radioButtonEliminate  = new JRadioButton("Eliminate outliers");
		radioButtonExtract    = new JRadioButton("Extract outliers");
		radioButtonEliminate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonEliminate.isSelected()) {
					choiceRadioButt_ExtractionType = radioButtonEliminate.getText();
				} 
				logService.info(this.getClass().getName() + " Extraction type set to " + choiceRadioButt_ExtractionType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonExtract.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonExtract.isSelected()) {
					choiceRadioButt_ExtractionType = radioButtonExtract.getText();
				}
				logService.info(this.getClass().getName() + " Extraction type set to " + choiceRadioButt_ExtractionType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		buttonGroupExtractionType.add(radioButtonEliminate);
		buttonGroupExtractionType.add(radioButtonExtract);
		radioButtonEliminate.setSelected(true);
		
		panelExtractionType = new JPanel();
		panelExtractionType.setToolTipText("Extraction type");
		panelExtractionType.setLayout(new BoxLayout(panelExtractionType, BoxLayout.Y_AXIS)); 
	    panelExtractionType.add(radioButtonEliminate);
	    panelExtractionType.add(radioButtonExtract);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelExtractionType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelExtractionType, gbc);
	    //initialize command variable
		if (radioButtonEliminate.isSelected()) choiceRadioButt_ExtractionType = radioButtonEliminate.getText();
		if (radioButtonExtract.isSelected())   choiceRadioButt_ExtractionType = radioButtonExtract.getText();
			
		//*****************************************************************************************
	    labelDetectionType = new JLabel("Detection type");
	    labelDetectionType.setToolTipText("Type of outliers detection");
	    labelDetectionType.setEnabled(true);
	    labelDetectionType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsDetectionType[] = {"Z-score", "IQR"};
		comboBoxDetectionType = new JComboBox<String>(optionsDetectionType);
		comboBoxDetectionType.setToolTipText("Type of outliers detection");
	    comboBoxDetectionType.setEnabled(true);
	    comboBoxDetectionType.setEditable(false);
	    comboBoxDetectionType.setSelectedItem("Z-score");
	    comboBoxDetectionType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_DetectionType = (String)comboBoxDetectionType.getSelectedItem();
						
				labelZscore.setEnabled(false);
				spinnerZscore.setEnabled(false);
			
				if (   choiceRadioButt_DetectionType.equals("Z-score")
					) {		
					labelZscore.setEnabled(true);
					spinnerZscore.setEnabled(true);
				}
				if (   choiceRadioButt_DetectionType.equals("IQR")
						) {		
					//nothing
				} 
							
				logService.info(this.getClass().getName() + " Detection type set to " + choiceRadioButt_DetectionType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelDetectionType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxDetectionType, gbc);
	    //initialize command variable
	    choiceRadioButt_DetectionType = (String)comboBoxDetectionType.getSelectedItem();
		//*****************************************************************************************
	
	    labelZscore = new JLabel("Z-score");
	    labelZscore.setToolTipText("Z-score value");
	    labelZscore.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelZscore = new SpinnerNumberModel(3, 1, 999999999, 1); // initial, min, max, step
        spinnerZscore = new JSpinner(spinnerModelZscore);
        spinnerZscore.setToolTipText("Z-score value");
        spinnerZscore.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Zscore = (int)spinnerZscore.getValue();
    	
                logService.info(this.getClass().getName() + " Factor set to " + spinnerInteger_Zscore);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelZscore, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerZscore, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Zscore = (int)spinnerZscore.getValue();
	    	
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
		 
		Future<CommandModule> future = commandService.run(Csaj2DOutliersCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
							
														"choiceRadioButt_ExtractionType", choiceRadioButt_ExtractionType,
														"choiceRadioButt_DetectionType",  choiceRadioButt_DetectionType,				
														"spinnerInteger_Zscore",          spinnerInteger_Zscore,
					
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
