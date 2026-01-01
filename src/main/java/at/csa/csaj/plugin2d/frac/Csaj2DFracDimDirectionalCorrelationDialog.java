/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimDirectionalCorrelationDialog.java
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

package at.csa.csaj.plugin2d.frac;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
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
import org.scijava.table.DefaultGenericTable;
import org.scijava.ui.UIService;
import at.csa.csaj.commons.CsajDialog_2DPluginWithRegression;
/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj2DFracDimDirectionalCorrelationDialog extends CsajDialog_2DPluginWithRegression {

	private static final long serialVersionUID = -3475871861446942774L;

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
	private JPanel       panelDirection;
	private ButtonGroup  buttonGroupDirection;
    private JRadioButton radioButtonCross;
    private JRadioButton radioButtonMeanOf4;
    private JRadioButton radioButtonMeanOf180;
	private String       choiceRadioButt_Direction;
	
	private JPanel       panelColorModelType;
	private ButtonGroup  buttonGroupColorModelType;
    private JRadioButton radioButtonBinary;
    private JRadioButton radioButtonGrey;
	private String       choiceRadioButt_ColorModelType;
	
	private JLabel   labelPixelPercentage;
	private JSpinner spinnerPixelPercentage;
	private int      spinnerInteger_PixelPercentage;
	
	private JLabel    labelGetAllRadialDsValues;
	private JCheckBox checkBoxGetAllRadialDsValues;
	private boolean   booleanGetAllRadialDsValues;
	
	
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
	public Csaj2DFracDimDirectionalCorrelationDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Direction correlation dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelDirection = new JLabel("Direction");
	    labelDirection.setToolTipText("Direction of counting pair-wise correlations");
	    labelDirection.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupDirection = new ButtonGroup();
		radioButtonCross      = new JRadioButton("Horizontal and vertical direction");
		radioButtonMeanOf4    = new JRadioButton("Mean of     4 radial directions [0-180°]");
		radioButtonMeanOf180  = new JRadioButton("Mean of 180 radial directions [0-180°]");
		radioButtonCross.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonCross.isSelected()) {
					choiceRadioButt_Direction = radioButtonCross.getText();
					labelGetAllRadialDsValues.setEnabled(false);
					checkBoxGetAllRadialDsValues.setEnabled(false);
					checkBoxGetAllRadialDsValues.setSelected(false);
				}		 
				logService.info(this.getClass().getName() + " Direction set to " + choiceRadioButt_Direction);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonMeanOf4.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonMeanOf4.isSelected()) {
					choiceRadioButt_Direction = radioButtonMeanOf4.getText();
					labelGetAllRadialDsValues.setEnabled(true);
					checkBoxGetAllRadialDsValues.setEnabled(true);
				}
				logService.info(this.getClass().getName() + " Direction set to " + choiceRadioButt_Direction);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonMeanOf180.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonMeanOf180.isSelected()) {
					choiceRadioButt_Direction = radioButtonMeanOf180.getText();
					labelGetAllRadialDsValues.setEnabled(true);
					checkBoxGetAllRadialDsValues.setEnabled(true);
				}
				logService.info(this.getClass().getName() + " Direction set to " + choiceRadioButt_Direction);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		buttonGroupDirection.add(radioButtonCross);
		buttonGroupDirection.add(radioButtonMeanOf4);
		buttonGroupDirection.add(radioButtonMeanOf180);
		radioButtonCross.setSelected(true);
		
		panelDirection = new JPanel();
		panelDirection.setToolTipText("Direction of counting pair-wise correlations");
		panelDirection.setLayout(new BoxLayout(panelDirection, BoxLayout.Y_AXIS)); 
	    panelDirection.add(radioButtonCross);
	    panelDirection.add(radioButtonMeanOf4);
	    panelDirection.add(radioButtonMeanOf180);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelDirection, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelDirection, gbc);
	    //initialize command variable
		if (radioButtonCross.isSelected())     choiceRadioButt_Direction = radioButtonCross.getText();
		if (radioButtonMeanOf4.isSelected())   choiceRadioButt_Direction = radioButtonMeanOf4.getText();
		if (radioButtonMeanOf180.isSelected()) choiceRadioButt_Direction = radioButtonMeanOf180.getText();
		
	    //*****************************************************************************************
	    JLabel labelColorModelType = new JLabel("Color model");
	    labelColorModelType.setToolTipText("Type of color model - binary or greyscale");
	    labelColorModelType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupColorModelType = new ButtonGroup();
		radioButtonBinary  = new JRadioButton("Binary");
		radioButtonGrey    = new JRadioButton("Grey");
		radioButtonBinary.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonBinary.isSelected())  choiceRadioButt_ColorModelType = radioButtonBinary.getText();
				logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonGrey.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonGrey.isSelected())  choiceRadioButt_ColorModelType = radioButtonGrey.getText();
				logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		buttonGroupColorModelType.add(radioButtonBinary);
		buttonGroupColorModelType.add(radioButtonGrey);
		radioButtonBinary.setSelected(true);
		
		panelColorModelType = new JPanel();
		panelColorModelType.setToolTipText("Type of color model - binary or greyscale");
		panelColorModelType.setLayout(new BoxLayout(panelColorModelType, BoxLayout.Y_AXIS)); 
		
	    panelColorModelType.add(radioButtonBinary);
	    panelColorModelType.add(radioButtonGrey); 
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelColorModelType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelColorModelType, gbc);
	    //initialize command variable
		if (radioButtonBinary.isSelected())  choiceRadioButt_ColorModelType = radioButtonBinary.getText();
		if (radioButtonGrey.isSelected())    choiceRadioButt_ColorModelType = radioButtonGrey.getText();
	
	    //*****************************************************************************************
	    labelPixelPercentage = new JLabel("Pixel %");
	    labelPixelPercentage.setToolTipText("% of object pixels to be taken - Sliding disc option to lower computation times");
	    labelPixelPercentage.setHorizontalAlignment(JLabel.RIGHT);
	    labelPixelPercentage.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelPixelPercentage = new SpinnerNumberModel(10, 1, 100, 1); // initial, min, max, step
        spinnerPixelPercentage = new JSpinner(spinnerModelPixelPercentage);
        spinnerPixelPercentage.setToolTipText("% of object pixels to be taken - Sliding disc option to lower computation times");
        spinnerPixelPercentage.setEnabled(true);
        spinnerPixelPercentage.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_PixelPercentage = (int)spinnerPixelPercentage.getValue();
                logService.info(this.getClass().getName() + " Pixel percentage set to " + spinnerInteger_PixelPercentage);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelPixelPercentage, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerPixelPercentage, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_PixelPercentage = (int)spinnerPixelPercentage.getValue();
		
		//*****************************************************************************************
		labelGetAllRadialDsValues = new JLabel("Get Dc values of all radial directions");
		labelGetAllRadialDsValues.setToolTipText("Get Dc values of all radial directions");
		labelGetAllRadialDsValues.setHorizontalAlignment(JLabel.RIGHT);
		labelGetAllRadialDsValues.setEnabled(false);
		
		checkBoxGetAllRadialDsValues = new JCheckBox();
		checkBoxGetAllRadialDsValues.setToolTipText("Get Dc values of all radial directions");
		checkBoxGetAllRadialDsValues.setEnabled(false);
		checkBoxGetAllRadialDsValues.setSelected(false);
		checkBoxGetAllRadialDsValues.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanGetAllRadialDsValues = checkBoxGetAllRadialDsValues.isSelected();	    
				logService.info(this.getClass().getName() + " Get Dc values set to " + booleanGetAllRadialDsValues);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 150;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelGetAllRadialDsValues, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 150;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxGetAllRadialDsValues, gbc);	
	 
	    //initialize command variable
	    booleanGetAllRadialDsValues = checkBoxGetAllRadialDsValues.isSelected();	 
	    
		//*****************************************************************************************    
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Number of distances");
		int numEpsMax = Csaj2DFracDimDirectionalCorrelationCmd.getMaxEpsNumber(datasetIn.dimension(0), datasetIn.dimension(1));
		spinnerModelNumEps= new SpinnerNumberModel(1, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
		spinnerNumEps.setValue(numEpsMax);
		spinnerNumRegEnd.setValue(numEpsMax);
		spinnerInteger_NumEps    = (int)spinnerNumEps.getValue();
		spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();	
		//*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj2DFracDimDirectionalCorrelationCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
														"choiceRadioButt_Direction",      choiceRadioButt_Direction,
														"choiceRadioButt_ColorModelType", choiceRadioButt_ColorModelType,
														"spinnerInteger_PixelPercentage", spinnerInteger_PixelPercentage,
					
														"spinnerInteger_NumBoxes",        spinnerInteger_NumEps, //WARNING: Exceptionally a different name
														"spinnerInteger_NumRegStart",     spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",       spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",       booleanShowDoubleLogPlot,
														"booleanGetAllRadialDsValues",    booleanGetAllRadialDsValues, 
														
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
		//tableOutName =(String)commandModule.getInfo().getLabel(); //Unfortunately, it is not possible to get this label inside the Command plugin class
		tableOutName = Csaj2DFracDimDirectionalCorrelationCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
