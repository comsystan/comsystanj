/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DAutoCropBordersDialog.java
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
public class Csaj2DAutoCropBordersDialog extends CsajDialog_2DPlugin {

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
	private JPanel       panelBackgroundType;
	private ButtonGroup  buttonGroupBackgroundType;
    private JRadioButton radioButtonBlack;
    private JRadioButton radioButtonWhite;
	private String       choiceRadioButt_BackgroundType;
	
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
	public Csaj2DAutoCropBordersDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Auto crop borders");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelBackgroundType = new JLabel("Background");
	    labelBackgroundType.setToolTipText("Type of background");
	    labelBackgroundType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupBackgroundType = new ButtonGroup();
		radioButtonBlack          = new JRadioButton("Black");
		radioButtonWhite          = new JRadioButton("White");
		radioButtonBlack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonBlack.isSelected())  choiceRadioButt_BackgroundType = radioButtonBlack.getText();
				logService.info(this.getClass().getName() + " Background type set to " + choiceRadioButt_BackgroundType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonWhite.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonWhite.isSelected())  choiceRadioButt_BackgroundType = radioButtonWhite.getText();
				logService.info(this.getClass().getName() + " Background type set to " + choiceRadioButt_BackgroundType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	
		buttonGroupBackgroundType.add(radioButtonBlack);
		buttonGroupBackgroundType.add(radioButtonWhite);
		radioButtonBlack.setSelected(true);
		
		panelBackgroundType = new JPanel();
		panelBackgroundType.setToolTipText("Type of background");
		panelBackgroundType.setLayout(new BoxLayout(panelBackgroundType, BoxLayout.Y_AXIS)); 
		
	    panelBackgroundType.add(radioButtonBlack);
	    panelBackgroundType.add(radioButtonWhite); 
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelBackgroundType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelBackgroundType, gbc);
	    //initialize command variable
		if (radioButtonBlack.isSelected())  choiceRadioButt_BackgroundType = radioButtonBlack.getText();
		if (radioButtonWhite.isSelected())     choiceRadioButt_BackgroundType = radioButtonWhite.getText();
		
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
		 
		Future<CommandModule> future = commandService.run(Csaj2DAutoCropBordersCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
							
														"choiceRadioButt_BackgroundType", choiceRadioButt_BackgroundType,
					
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
