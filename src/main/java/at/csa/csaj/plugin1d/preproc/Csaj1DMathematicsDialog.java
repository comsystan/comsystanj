/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DMathematicsDialog.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2026 Comsystan Software
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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
public class Csaj1DMathematicsDialog extends CsajDialog_1DPlugin {

	private static final long serialVersionUID = 1945678532278042800L;

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
	private JLabel			  labelOperator;
	private JComboBox<String> comboBoxOperator;
	private String            choiceRadioButt_Operator;
	
	private JLabel			  labelDomain;
	private JComboBox<String> comboBoxDomain;
	private String            choiceRadioButt_Domain;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DMathematicsDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Mathematics");

		//Add specific GUI elements according to Command @Parameter GUI elements   
		//*****************************************************************************************
	    labelOperator = new JLabel("Operator");
	    labelOperator.setToolTipText("Mathematical function");
	    labelOperator.setEnabled(true);
	    labelOperator.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsOperator[] = {"Diff+", "Diff-", "Diff+-", "Integral", "Exp", "Ln", "Log", "Sin", "Cos", "Tan"};
		comboBoxOperator = new JComboBox<String>(optionsOperator);
		comboBoxOperator.setToolTipText("Mathematical function");
	    comboBoxOperator.setEnabled(true);
	    comboBoxOperator.setEditable(false);
	    comboBoxOperator.setSelectedItem("Diff+-");
	    comboBoxOperator.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_Operator = (String)comboBoxOperator.getSelectedItem();
				
				if (  choiceRadioButt_Operator.equals("Diff+")
					|| choiceRadioButt_Operator.equals("Diff-")
					|| choiceRadioButt_Operator.equals("Diff+-")
					|| choiceRadioButt_Operator.equals("Integral")
					) {		
					labelDomain.setEnabled(true);
					comboBoxDomain.setEnabled(true);
				} else {
					labelDomain.setEnabled(false);
					comboBoxDomain.setEnabled(false);
				}
				
				logService.info(this.getClass().getName() + " Operator set to " + choiceRadioButt_Operator);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOperator, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxOperator, gbc);
	    //initialize command variable
	    choiceRadioButt_Operator = (String)comboBoxOperator.getSelectedItem();
	    
		//*****************************************************************************************
	    labelDomain = new JLabel("Domain");
	    labelDomain.setToolTipText("Domain for difference or inetgral operators");
	    labelDomain.setEnabled(true);
	    labelDomain.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsDomain[] = {"Unity", "Column #1"};
		comboBoxDomain = new JComboBox<String>(optionsDomain);
		comboBoxDomain.setToolTipText("Domain for difference or inetgral operators");
	    comboBoxDomain.setEnabled(true);
	    comboBoxDomain.setEditable(false);
	    comboBoxDomain.setSelectedItem("Unity");
	    comboBoxDomain.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_Domain = (String)comboBoxDomain.getSelectedItem();
				
				if (choiceRadioButt_Domain.equals("Column #1")) {
					if (tableIn.getColumnCount() == 1) {
						logService.info(this.getClass().getName() + " Column #1 cannot be a data column as well as the domain column");
						choiceRadioButt_Domain = "Unity";
					} else if (spinnerInteger_NumColumn <= 1) {
						spinnerNumColumn.setValue(2);
						spinnerInteger_NumColumn = 2;
					}
				}
				
				if (tableIn.getColumnCount() == 1) {
					logService.info(this.getClass().getName() + " Column #1 cannot be a data column as well as the domain column");
					comboBoxDomain.setSelectedItem("Unity");
					choiceRadioButt_Domain = "Unity";
				}
				if (!tableIn.get(0).get(0).getClass().getSimpleName().equals("Double")) { //e.g. String column
					logService.info(this.getClass().getName() + " Column #1 is not a Double value column");
					comboBoxDomain.setSelectedItem("Unity");
					choiceRadioButt_Domain = "Unity";
				}
		
				logService.info(this.getClass().getName() + " Domain set to " + choiceRadioButt_Domain);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelDomain, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxDomain, gbc);
	    //initialize command variable
	    choiceRadioButt_Domain = (String)comboBoxDomain.getSelectedItem();    
	          	
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
    	
    	//add additional listener
        spinnerNumColumn.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
				choiceRadioButt_Domain = (String)comboBoxDomain.getSelectedItem();
			 	spinnerInteger_NumColumn = (int)spinnerNumColumn.getValue();
				
				if (choiceRadioButt_Domain.equals("Column #1")) {
					if (tableIn.getColumnCount() == 1) {
						logService.info(this.getClass().getName() + " Column #1 cannot be a data column as well as the domain column");
						choiceRadioButt_Domain = "Unity";
					} else if (spinnerInteger_NumColumn <= 1) {
						spinnerNumColumn.setValue(2);
						spinnerInteger_NumColumn = (int)spinnerNumColumn.getValue();
					}
				}
			}
    	});
	    	
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    
	    //*****************************************************************************************
		//Do additional things
	}
			
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj1DMathematicsCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_Operator",     choiceRadioButt_Operator,
														"choiceRadioButt_Domain",        choiceRadioButt_Domain,

														"choiceRadioButt_SequenceRange", choiceRadioButt_SequenceRange,
														"choiceRadioButt_SurrogateType", choiceRadioButt_SurrogateType,
														//"spinnerInteger_NumSurrogates",  spinnerInteger_NumSurrogates,
														//"spinnerInteger_BoxLength",      spinnerInteger_BoxLength,
														//"booleanSkipZeroes",             booleanSkipZeroes,
														
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
		tableOutName = Csaj1DMathematicsCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
