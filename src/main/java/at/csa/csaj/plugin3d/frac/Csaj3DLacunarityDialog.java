/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DLacunarityDialog.java
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

package at.csa.csaj.plugin3d.frac;

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
import at.csa.csaj.commons.CsajDialog_3DPluginWithRegression;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj3DLacunarityDialog extends CsajDialog_3DPluginWithRegression {

	private static final long serialVersionUID = -5968807796236851779L;

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
	private JPanel       panelScanningType;
	private ButtonGroup  buttonGroupScanningType;
    private JRadioButton radioButtonRasterBox;
    private JRadioButton radioButtonSlidingBox;
    private JRadioButton radioButtonTugOfWar;
	private String       choiceRadioButt_ScanningType;
	
	private JPanel       panelColorModelType;
	private ButtonGroup  buttonGroupColorModelType;
    private JRadioButton radioButtonBinary;
    private JRadioButton radioButtonGrey;
	private String       choiceRadioButt_ColorModelType;
	
	private JLabel   labelPixelPercentage;
	private JSpinner spinnerPixelPercentage;
	private int      spinnerInteger_PixelPercentage;
	
	private JLabel  labelNumAcurracy;
	public JSpinner spinnerNumAcurracy;
	public int      spinnerInteger_NumAcurracy;
	
	private JLabel  labelNumConfidence;
	public JSpinner spinnerNumConfidence;
	public int      spinnerInteger_NumConfidence;
	
	
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
	public Csaj3DLacunarityDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("3D Lacunarities");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelScanningType = new JLabel("Scanning");
	    labelScanningType.setToolTipText("Type of 3D box scanning");
	    labelScanningType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupScanningType = new ButtonGroup();
		radioButtonRasterBox    = new JRadioButton("Raster box");
		radioButtonSlidingBox   = new JRadioButton("Sliding box");
		radioButtonSlidingBox.setToolTipText("NOTE: Sliding box is very slow");
		radioButtonTugOfWar     = new JRadioButton("Tug of war");
		radioButtonRasterBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonRasterBox.isSelected()) {
					choiceRadioButt_ScanningType = radioButtonRasterBox.getText();
					labelPixelPercentage.setEnabled(false);
					spinnerPixelPercentage.setEnabled(false);
					labelNumAcurracy.setEnabled(false);
					spinnerNumAcurracy.setEnabled(false);
					labelNumConfidence.setEnabled(false);
					spinnerNumConfidence.setEnabled(false);
				}	 
				logService.info(this.getClass().getName() + " Scanning type set to " + choiceRadioButt_ScanningType);
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
		radioButtonSlidingBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonSlidingBox.isSelected()) {
					choiceRadioButt_ScanningType = radioButtonSlidingBox.getText();
					labelPixelPercentage.setEnabled(true);
					spinnerPixelPercentage.setEnabled(true);
					labelNumAcurracy.setEnabled(false);
					spinnerNumAcurracy.setEnabled(false);
					labelNumConfidence.setEnabled(false);
					spinnerNumConfidence.setEnabled(false);
				}
				logService.info(this.getClass().getName() + " Scanning type set to " + choiceRadioButt_ScanningType);
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
		radioButtonTugOfWar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonTugOfWar.isSelected()) {
					choiceRadioButt_ScanningType = radioButtonTugOfWar.getText();
					labelPixelPercentage.setEnabled(false);
					spinnerPixelPercentage.setEnabled(false);
					labelNumAcurracy.setEnabled(true);
					spinnerNumAcurracy.setEnabled(true);
					labelNumConfidence.setEnabled(true);
					spinnerNumConfidence.setEnabled(true);
				}
				logService.info(this.getClass().getName() + " Scanning type set to " + choiceRadioButt_ScanningType);
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
		buttonGroupScanningType.add(radioButtonRasterBox);
		buttonGroupScanningType.add(radioButtonSlidingBox);
		buttonGroupScanningType.add(radioButtonTugOfWar);
		radioButtonRasterBox.setSelected(true);
		
		panelScanningType = new JPanel();
		panelScanningType.setToolTipText("Type of 3D box scanning");
		panelScanningType.setLayout(new BoxLayout(panelScanningType, BoxLayout.Y_AXIS)); 
	    panelScanningType.add(radioButtonRasterBox);
	    panelScanningType.add(radioButtonSlidingBox);
	    //panelScanningType.add(radioButtonTugOfWar);
	    		
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelScanningType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelScanningType, gbc);
	    //initialize command variable
		if (radioButtonRasterBox.isSelected())  choiceRadioButt_ScanningType = radioButtonRasterBox.getText();
		if (radioButtonSlidingBox.isSelected()) choiceRadioButt_ScanningType = radioButtonSlidingBox.getText();
		if (radioButtonTugOfWar.isSelected())   choiceRadioButt_ScanningType = radioButtonTugOfWar.getText();
		
	    //*****************************************************************************************
	    JLabel labelColorModelType = new JLabel("Color model");
	    labelColorModelType.setToolTipText("Type of color model - binary or greyscale");
	    labelColorModelType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupColorModelType = new ButtonGroup();
		radioButtonBinary         = new JRadioButton("Binary");
		radioButtonGrey           = new JRadioButton("Grey");
		radioButtonBinary.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonBinary.isSelected())  choiceRadioButt_ColorModelType = radioButtonBinary.getText();
				logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
		radioButtonGrey.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonGrey.isSelected())  choiceRadioButt_ColorModelType = radioButtonGrey.getText();
				logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
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
	    labelPixelPercentage.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelPixelPercentage = new SpinnerNumberModel(1, 1, 100, 1); // initial, min, max, step
        spinnerPixelPercentage = new JSpinner(spinnerModelPixelPercentage);
        spinnerPixelPercentage.setToolTipText("% of object pixels to be taken - Sliding disc option to lower computation times");
        spinnerPixelPercentage.setEnabled(false);
        spinnerPixelPercentage.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_PixelPercentage = (int)spinnerPixelPercentage.getValue();
                logService.info(this.getClass().getName() + " Pixel percentage set to " + spinnerInteger_PixelPercentage);
                if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
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
	    labelNumAcurracy = new JLabel("Accuracy");
	    labelNumAcurracy.setToolTipText("Accuracy (default=90)");
	    labelNumAcurracy.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumAcurracy.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelNumAcurracy= new SpinnerNumberModel(90, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
	    spinnerNumAcurracy = new JSpinner(spinnerModelNumAcurracy);
        spinnerNumAcurracy.setToolTipText("Accuracy (default=90)"); //s1=30 Wang paper
        spinnerNumAcurracy.setEnabled(false);
        spinnerNumAcurracy.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
    			spinnerInteger_NumAcurracy = (int)spinnerNumAcurracy.getValue();
    			logService.info(this.getClass().getName() + " Accuracy set to " + spinnerInteger_NumAcurracy);
                if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumAcurracy, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;   
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumAcurracy, gbc);	
	  
	    //initialize command variable
	    spinnerInteger_NumAcurracy = (int)spinnerNumAcurracy.getValue();
	    
	    //*****************************************************************************************
	    labelNumConfidence = new JLabel("Confidence");
	    labelNumConfidence.setToolTipText("Confidence (default=15)");
	    labelNumConfidence.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumConfidence.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelNumConfidence= new SpinnerNumberModel(15, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double 
	    spinnerNumConfidence = new JSpinner(spinnerModelNumConfidence);
        spinnerNumConfidence.setToolTipText("Confidence (default=15)"); //s2=5 Wang paper
        spinnerNumConfidence.setEnabled(false);
        spinnerNumConfidence.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
    			spinnerInteger_NumConfidence = (int)spinnerNumConfidence.getValue();
    			logService.info(this.getClass().getName() + " Confidence set to " + spinnerInteger_NumConfidence);
                if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumConfidence, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;   
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumConfidence, gbc);	
	  
	    //initialize command variable
	    spinnerInteger_NumConfidence = (int)spinnerNumConfidence.getValue();
	       
		//*****************************************************************************************
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Number of boxes");
		int numEpsMax = Csaj3DLacunarityCmd.getMaxBoxNumber(width, height, depth);
		spinnerModelNumEps= new SpinnerNumberModel(1, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
		spinnerNumEps.setValue(numEpsMax);
		//spinnerNumRegEnd.setValue(numEpsMax);
		spinnerInteger_NumEps    = (int)spinnerNumEps.getValue();
		//spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();	
		
		//Lacunarity does not need Reg start and end
		//Remove items
		contentPanel.remove(labelNumRegStart);
	    contentPanel.remove(spinnerNumRegStart);
	    contentPanel.remove(labelNumRegEnd);
	    contentPanel.remove(spinnerNumRegEnd);
		
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
		Future<CommandModule> future = commandService.run(Csaj3DLacunarityCmd.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
													
														"choiceRadioButt_ScanningType",   choiceRadioButt_ScanningType,
														"choiceRadioButt_ColorModelType", choiceRadioButt_ColorModelType,
														"spinnerInteger_PixelPercentage", spinnerInteger_PixelPercentage,
														//"spinnerInteger_NumAcurracy",	  spinnerInteger_NumAcurracy,
														//"spinnerInteger_NumConfidence",   spinnerInteger_NumConfidence,
					
														"spinnerInteger_NumBoxes",        spinnerInteger_NumEps, //WARNING: Exceptionally a different name
														//"spinnerInteger_NumRegStart",     spinnerInteger_NumRegStart,
														//"spinnerInteger_NumRegEnd",       spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",       booleanShowDoubleLogPlot,
	
														"booleanOverwriteDisplays",       booleanOverwriteDisplays,
														"booleanProcessImmediately",	  booleanProcessImmediately
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
		tableOutName = Csaj3DLacunarityCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}