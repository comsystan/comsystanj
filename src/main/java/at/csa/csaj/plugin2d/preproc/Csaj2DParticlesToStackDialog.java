/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DParticlesToStackDialog.java
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

package at.csa.csaj.plugin2d.preproc;

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
public class Csaj2DParticlesToStackDialog extends CsajDialog_2DPlugin {

	private static final long serialVersionUID = 6324666429474486634L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
  	private Dataset datasetIn;
	private Dataset datasetOut;
   
	//Specific dialog items
	private JPanel       panelConnectivityType;
	private ButtonGroup  buttonGroupConnectivityType;
    private JRadioButton radioButton4;
    private JRadioButton radioButton8;
	private String       choiceRadioButt_ConnectivityType;
	
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
	public Csaj2DParticlesToStackDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Particles to stack");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelConnectivityType = new JLabel("Connectivity");
	    labelConnectivityType.setToolTipText("4 or 8 neighbourhood connectivity");
	    labelConnectivityType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupConnectivityType = new ButtonGroup();
		radioButton4                = new JRadioButton("4");
		radioButton8                = new JRadioButton("8");
		radioButton4.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButton4.isSelected())  choiceRadioButt_ConnectivityType = radioButton4.getText();
				logService.info(this.getClass().getName() + " Connectivity type set to " + choiceRadioButt_ConnectivityType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButton8.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButton8.isSelected())  choiceRadioButt_ConnectivityType = radioButton8.getText();
				logService.info(this.getClass().getName() + " Connectivity type set to " + choiceRadioButt_ConnectivityType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	
		buttonGroupConnectivityType.add(radioButton4);
		buttonGroupConnectivityType.add(radioButton8);
		radioButton4.setSelected(true);
		
		panelConnectivityType = new JPanel();
		panelConnectivityType.setToolTipText("4 or 8 neighbourhood connectivity");
		panelConnectivityType.setLayout(new BoxLayout(panelConnectivityType, BoxLayout.Y_AXIS)); 
		
	    panelConnectivityType.add(radioButton4);
	    panelConnectivityType.add(radioButton8); 
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelConnectivityType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelConnectivityType, gbc);
	    //initialize command variable
		if (radioButton4.isSelected())  choiceRadioButt_ConnectivityType = radioButton4.getText();
		if (radioButton8.isSelected())     choiceRadioButt_ConnectivityType = radioButton8.getText();
		
		//*****************************************************************************************
		//Change/Override items defined in the super class(es)
		btnProcessSingleImage.setText("Preview in grey values");
		btnProcessAllImages.setText("Process single image#");
		
		
		//*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj2DParticlesToStackCmd.class, false,
														"datasetIn",                         datasetIn,  //is not automatically harvested in headless mode
														"processAll",					     processAll, //true for all
							
														"choiceRadioButt_ConnectivityType", choiceRadioButt_ConnectivityType,
					
														"booleanOverwriteDisplays",          booleanOverwriteDisplays,
														"booleanProcessImmediately",	     booleanProcessImmediately,
														"spinnerInteger_NumImageSlice",	     spinnerInteger_NumImageSlice
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
