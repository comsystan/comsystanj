/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DHurstPSDDialog.java
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

package at.csa.csaj.plugin1d.frac;


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
import javax.swing.SpinnerNumberModel;

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
public class Csaj1DHurstPSDDialog extends CsajDialog_1DPluginWithRegression {

	private static final long serialVersionUID = -3549913599788102745L;

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
	private JPanel       panelPSDType;
	private ButtonGroup  buttonGroupPSDType;
    private JRadioButton radioButtonPSD;
    private JRadioButton radioButtonLowPSDwe;
	private String       choiceRadioButt_PSDType;
	
	private JPanel       panelSWVType;
	private ButtonGroup  buttonGroupSWVType;
    private JRadioButton radioButtonSWV;
    private JRadioButton radioButtonBdSWV;
	private String       choiceRadioButt_SWVType;
	
	private JLabel			  labelSurrBoxHurstType;
	private JComboBox<String> comboBoxSurrBoxHurstType;
	private String            choiceRadioButt_SurrBoxHurstType;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DHurstPSDDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Hurst coefficient (PSD)");

		//Add specific GUI elements according to Command @Parameter GUI elements  
		//*****************************************************************************************		
	    JLabel labelPSDType = new JLabel("PSD type");
	    labelPSDType.setToolTipText("Power spectral density or low frequency PSD with parabolic windowing and end matching");
	    labelPSDType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupPSDType  = new ButtonGroup();
		radioButtonPSD      = new JRadioButton("PSD");
		radioButtonLowPSDwe = new JRadioButton("lowPSDwe");
		radioButtonPSD.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonPSD.isSelected()) {
					choiceRadioButt_PSDType = radioButtonPSD.getText();
					
					int poweSpecLength = (int)numRows/2;
					//set regression
					spinnerNumRegStart.setValue(1);
					spinnerNumRegEnd.setValue(poweSpecLength);
				
					spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();
					spinnerInteger_NumRegEnd   = (int)spinnerNumRegEnd.getValue();
				} 
								
				logService.info(this.getClass().getName() + " PSD type set to " + choiceRadioButt_PSDType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonLowPSDwe.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonLowPSDwe.isSelected()) {
					choiceRadioButt_PSDType = radioButtonLowPSDwe.getText();
					
					int poweSpecLength = (int)numRows/2;
					//restrict regression	
					spinnerNumRegStart.setValue(poweSpecLength/8);
					spinnerNumRegEnd.setValue(poweSpecLength/2);
					
					spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();
					spinnerInteger_NumRegEnd   = (int)spinnerNumRegEnd.getValue();
				}
							
				logService.info(this.getClass().getName() + " PSD type set to " + choiceRadioButt_PSDType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupPSDType.add(radioButtonPSD);
		buttonGroupPSDType.add(radioButtonLowPSDwe);
		radioButtonPSD.setSelected(true);
		
		panelPSDType = new JPanel();
		panelPSDType.setToolTipText("Power spectral density or low frequency PSD with parabolic windowing and end matching");
		panelPSDType.setLayout(new BoxLayout(panelPSDType, BoxLayout.Y_AXIS)); 
	    panelPSDType.add(radioButtonPSD);
	    panelPSDType.add(radioButtonLowPSDwe);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelPSDType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelPSDType, gbc);
	    //initialize command variable
		if (radioButtonPSD.isSelected())      choiceRadioButt_PSDType = radioButtonPSD.getText();
		if (radioButtonLowPSDwe.isSelected()) choiceRadioButt_PSDType = radioButtonLowPSDwe.getText();
		
		//*****************************************************************************************		
	    JLabel labelSWVType = new JLabel("SWV type");
	    labelSWVType.setToolTipText("Scaled window variance or bridge detrending SWV");
	    labelSWVType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupSWVType = new ButtonGroup();
		radioButtonSWV  = new JRadioButton("SWV");
		radioButtonBdSWV = new JRadioButton("bdSWV");
		radioButtonSWV.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonSWV.isSelected()) {
					choiceRadioButt_SWVType = radioButtonSWV.getText();
				} 
				logService.info(this.getClass().getName() + " SWV type set to " + choiceRadioButt_SWVType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonBdSWV.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonBdSWV.isSelected()) {
					choiceRadioButt_SWVType = radioButtonBdSWV.getText();
				}
				logService.info(this.getClass().getName() + " SWV type set to " + choiceRadioButt_SWVType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupSWVType.add(radioButtonSWV);
		buttonGroupSWVType.add(radioButtonBdSWV);
		radioButtonSWV.setSelected(true);
		
		panelSWVType = new JPanel();
		panelSWVType.setToolTipText("Scaled window variance or bridge detrending SWV");
		panelSWVType.setLayout(new BoxLayout(panelSWVType, BoxLayout.Y_AXIS)); 
	    panelSWVType.add(radioButtonSWV);
	    panelSWVType.add(radioButtonBdSWV);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSWVType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelSWVType, gbc);
	    //initialize command variable
		if (radioButtonSWV.isSelected())   choiceRadioButt_SWVType = radioButtonSWV.getText();
		if (radioButtonBdSWV.isSelected()) choiceRadioButt_SWVType = radioButtonBdSWV.getText();
		
		//*****************************************************************************************
	    labelSurrBoxHurstType = new JLabel("Hurst type");
	    labelSurrBoxHurstType.setToolTipText("Hurst for Surrogates, Subsequent boxes or Gliding box");
	    labelSurrBoxHurstType.setEnabled(false);
	    labelSurrBoxHurstType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsSurrBoxHurstType[] = {"PSD_Beta"};   //may be later  "DISP", "SWV"}, //"PSD", "DISP", "SWV"};
		comboBoxSurrBoxHurstType = new JComboBox<String>(optionsSurrBoxHurstType);
		comboBoxSurrBoxHurstType.setToolTipText("Hurst for Surrogates, Subsequent boxes or Gliding box");
	    comboBoxSurrBoxHurstType.setEnabled(false);
	    comboBoxSurrBoxHurstType.setEditable(false);
	    comboBoxSurrBoxHurstType.setSelectedItem("PSD_Beta");
	    comboBoxSurrBoxHurstType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_SurrBoxHurstType = (String)comboBoxSurrBoxHurstType.getSelectedItem();
				logService.info(this.getClass().getName() + " Hurst type set to " + choiceRadioButt_SurrBoxHurstType);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSurrBoxHurstType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxSurrBoxHurstType, gbc);
	    //initialize command variable
	    choiceRadioButt_SurrBoxHurstType = (String)comboBoxSurrBoxHurstType.getSelectedItem();
		
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)
		int numMaxRegEnd = (int) Math.floor((double)tableIn.getRowCount() / 1.0);
	       
		labelNumEps.setEnabled(false);
		labelNumEps.setVisible(false);
		
		spinnerNumEps.setEnabled(false);
		spinnerNumEps.setVisible(false);
		
		labelNumRegStart.setText("Regression min");
		spinnerModelNumRegStart = new SpinnerNumberModel(1, 1, numMaxRegEnd - 1, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double	
		spinnerNumRegStart.setModel(spinnerModelNumRegStart);

	    labelNumRegEnd.setText("Regression max");
		spinnerModelNumRegEnd = new SpinnerNumberModel(8, 3, numMaxRegEnd, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegEnd.setModel(spinnerModelNumRegEnd);
	 
		spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();	
		spinnerInteger_NumRegEnd   = (int)spinnerNumRegEnd.getValue();	
		
	    //add a second listener
	    comboBoxSequenceRange.addActionListener(new ActionListener() {
 			@Override
 			public void actionPerformed(final ActionEvent arg0) {
 				choiceRadioButt_SequenceRange = (String)comboBoxSequenceRange.getSelectedItem();
 				
 				labelSurrBoxHurstType.setEnabled(false);
				comboBoxSurrBoxHurstType.setEnabled(false);		
 				if (   choiceRadioButt_SequenceRange.equals("Entire sequence")
 				    ) {	
 					// Do nothing
 				}
 				if (   choiceRadioButt_SequenceRange.equals("Subsequent boxes")
 					|| choiceRadioButt_SequenceRange.equals("Gliding box") 				
 					) {		
 					labelSurrBoxHurstType.setEnabled(true);
					comboBoxSurrBoxHurstType.setEnabled(true);
 				}
 			}
 		});
	    
	    //add a second listener
	    comboBoxSurrogateType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_SurrogateType = (String)comboBoxSurrogateType.getSelectedItem();
				
				//Reset all spinners and options
				labelSurrBoxHurstType.setEnabled(false);
				comboBoxSurrBoxHurstType.setEnabled(false);							
				if (   choiceRadioButt_SurrogateType.equals("No surrogates")
				    ) {		
					//Surrogate event is also called after a Sequence range event, so we need to check this here again.
					if (   choiceRadioButt_SequenceRange.equals("Entire sequence")
	 				  ) {	
						//Do nothing					
	 				}
	
					if (   choiceRadioButt_SequenceRange.equals("Subsequent boxes")
	 					|| choiceRadioButt_SequenceRange.equals("Gliding box") 				
	 					) {		
	 					labelSurrBoxHurstType.setEnabled(true);
						comboBoxSurrBoxHurstType.setEnabled(true);
	 				}
				}
				if (   choiceRadioButt_SurrogateType.equals("Shuffle")
					|| choiceRadioButt_SurrogateType.equals("Gaussian") 
					|| choiceRadioButt_SurrogateType.equals("Random phase") 
					|| choiceRadioButt_SurrogateType.equals("AAFT") 			
					) {
					labelSurrBoxHurstType.setEnabled(true);
					comboBoxSurrBoxHurstType.setEnabled(true);		
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
		 
		Future<CommandModule> future = commandService.run(Csaj1DHurstPSDCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
														
														"choiceRadioButt_PSDType",       choiceRadioButt_PSDType,
														"choiceRadioButt_SWVType",       choiceRadioButt_SWVType,	
														"choiceRadioButt_SurrBoxHurstType", choiceRadioButt_SurrBoxHurstType,
														
														//"spinnerInteger_NumEps", 	     spinnerInteger_NumEps,
														"spinnerInteger_NumRegStart",    spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",      spinnerInteger_NumRegEnd,
													
														
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
		tableOutName = Csaj1DHurstPSDCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
