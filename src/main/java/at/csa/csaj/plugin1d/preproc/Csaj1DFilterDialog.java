/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DFilterDialog.java
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

package at.csa.csaj.plugin1d.preproc;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
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
public class Csaj1DFilterDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = -5317902611709031806L;

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
	private JPanel       panelFilterType;
	private ButtonGroup  buttonGroupFilterType;
    private JRadioButton radioButtonMovAver;
    private JRadioButton radioButtonMovMedi;
	private String       choiceRadioButt_FilterType;
	
	private JSpinner spinnerRange;
	private int      spinnerInteger_Range;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DFilterDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Filter");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelFilterType = new JLabel("Filter");
	    labelFilterType.setToolTipText("Filter type");
	    labelFilterType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupFilterType = new ButtonGroup();
		radioButtonMovAver    = new JRadioButton("Moving average");
		radioButtonMovMedi    = new JRadioButton("Moving median");
		radioButtonMovAver.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonMovAver.isSelected()) {
					choiceRadioButt_FilterType = radioButtonMovAver.getText();
				} 
				logService.info(this.getClass().getName() + " Filter type set to " + choiceRadioButt_FilterType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonMovMedi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonMovMedi.isSelected()) {
					choiceRadioButt_FilterType = radioButtonMovMedi.getText();
				}
				logService.info(this.getClass().getName() + " Filter type set to " + choiceRadioButt_FilterType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupFilterType.add(radioButtonMovAver);
		buttonGroupFilterType.add(radioButtonMovMedi);
		radioButtonMovAver.setSelected(true);
		
		panelFilterType = new JPanel();
		panelFilterType.setToolTipText("Filter type");
		panelFilterType.setLayout(new BoxLayout(panelFilterType, BoxLayout.Y_AXIS)); 
	    panelFilterType.add(radioButtonMovAver);
	    panelFilterType.add(radioButtonMovMedi);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFilterType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelFilterType, gbc);
	    //initialize command variable
		if (radioButtonMovAver.isSelected()) choiceRadioButt_FilterType = radioButtonMovAver.getText();
		if (radioButtonMovMedi.isSelected()) choiceRadioButt_FilterType = radioButtonMovMedi.getText();
			
		//*****************************************************************************************
	    JLabel labelRange = new JLabel("Range");
	    labelRange.setToolTipText("Computation range from (i-range/2) to (i+range/2)");
	    labelRange.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelRange = new SpinnerNumberModel(3, 3, 999999999, 2); // initial, min, max, step
        spinnerRange = new JSpinner(spinnerModelRange);
        spinnerRange.setToolTipText("Computation range from (i-range/2) to (i+range/2)");
        spinnerRange.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Range = (int)spinnerRange.getValue();
            
            	if (spinnerInteger_Range % 2 == 0 ) spinnerRange.setValue(spinnerInteger_Range + 1);  //even numbers are not allowed     	
            	spinnerInteger_Range = (int)spinnerRange.getValue();
            	
                logService.info(this.getClass().getName() + " Range set to " + spinnerInteger_Range);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelRange, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerRange, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Range = (int)spinnerRange.getValue();
	    
	    //*****************************************************************************************
  		//Change/Override items defined in the super class(es)
	    //Restricted options
  		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>)comboBoxSequenceRange.getModel();
  		model.removeElement("Subsequent boxes");
  		model.removeElement("Gliding box");	
  		comboBoxSequenceRange.setSelectedItem("Entire sequence");
  		choiceRadioButt_SequenceRange = (String)comboBoxSequenceRange.getSelectedItem();
  	    
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
		Future<CommandModule> future = commandService.run(Csaj1DFilterCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_FilterType",    choiceRadioButt_FilterType,
														"spinnerInteger_Range",          spinnerInteger_Range,
														
														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
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
		tableOutName = Csaj1DFilterCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
