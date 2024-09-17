/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimMinkowskiDialog.java
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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpinnerNumberModel;

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
public class Csaj2DFracDimMinkowskiDialog extends CsajDialog_2DPluginWithRegression {

	private static final long serialVersionUID = -1971118263202932507L;

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


 	private JComboBox<String> comboBoxShapeType;
	private String   choiceRadioButt_ShapeType;
	
	private JPanel       panelMorphologicalOperator;
	private ButtonGroup  buttonGroupMorphologicalOperator;
    private JRadioButton radioButtonBinaryDilation;
    private JRadioButton radioButtonBlanketDilation;
    private JRadioButton radioButtonVariationDilation;
	private String       choiceRadioButt_MorphologicalOperator;
	
	private JCheckBox  checkBoxShowLastMorphImg;
	private boolean    booleanShowLastMorphImg;
	
	
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
	public Csaj2DFracDimMinkowskiDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Minkowski dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelShapeType = new JLabel("Kernel shape");
	    labelShapeType.setToolTipText("Shape of morphological structuring element");
	    labelShapeType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Square", "Disk", "Diamond", "Horizontal", "Vertical"};
		comboBoxShapeType = new JComboBox<String>(options);
		comboBoxShapeType.setToolTipText("Shape of morphological structuring element");
	    comboBoxShapeType.setEditable(false);
	    comboBoxShapeType.setSelectedItem("Square");
	    comboBoxShapeType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_ShapeType = (String)comboBoxShapeType.getSelectedItem();
				logService.info(this.getClass().getName() + " Kernel shape set to " + choiceRadioButt_ShapeType);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelShapeType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxShapeType, gbc);
	    //initialize command variable
	    choiceRadioButt_ShapeType = (String)comboBoxShapeType.getSelectedItem();
		
	    //*****************************************************************************************
	    JLabel labelMorphologicalOperator = new JLabel("Morphological operator");
	    labelMorphologicalOperator.setToolTipText("Type of image and according morphological operation");
	    labelMorphologicalOperator.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupMorphologicalOperator = new ButtonGroup();
		radioButtonBinaryDilation        = new JRadioButton("Binary dilation");
		radioButtonBlanketDilation       = new JRadioButton("Blanket dilation/erosion");
		radioButtonVariationDilation     = new JRadioButton("Variation dilation/erosion");
		radioButtonBinaryDilation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonBinaryDilation.isSelected())  choiceRadioButt_MorphologicalOperator = radioButtonBinaryDilation.getText();
				logService.info(this.getClass().getName() + " Morphological operator set to " + choiceRadioButt_MorphologicalOperator);		
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonBlanketDilation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonBlanketDilation.isSelected())  choiceRadioButt_MorphologicalOperator = radioButtonBlanketDilation.getText();
				logService.info(this.getClass().getName() + " Morphological operator set to " + choiceRadioButt_MorphologicalOperator);
			}
		});
		radioButtonVariationDilation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonVariationDilation.isSelected())  choiceRadioButt_MorphologicalOperator = radioButtonVariationDilation.getText();
				logService.info(this.getClass().getName() + " Morphological operator set to " + choiceRadioButt_MorphologicalOperator);			
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		buttonGroupMorphologicalOperator.add(radioButtonBinaryDilation);
		buttonGroupMorphologicalOperator.add(radioButtonBlanketDilation);
		buttonGroupMorphologicalOperator.add(radioButtonVariationDilation);
		radioButtonBinaryDilation.setSelected(true);
		
		panelMorphologicalOperator = new JPanel();
		panelMorphologicalOperator.setToolTipText("Type of image and according morphological operation");
		panelMorphologicalOperator.setLayout(new BoxLayout(panelMorphologicalOperator, BoxLayout.Y_AXIS)); 
		
	    panelMorphologicalOperator.add(radioButtonBinaryDilation);
	    panelMorphologicalOperator.add(radioButtonBlanketDilation); 
	    panelMorphologicalOperator.add(radioButtonVariationDilation); 
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMorphologicalOperator, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelMorphologicalOperator, gbc);
	    //initialize command variable
		if (radioButtonBinaryDilation.isSelected())    choiceRadioButt_MorphologicalOperator = radioButtonBinaryDilation.getText();
		if (radioButtonBlanketDilation.isSelected())   choiceRadioButt_MorphologicalOperator = radioButtonBlanketDilation.getText();
		if (radioButtonVariationDilation.isSelected()) choiceRadioButt_MorphologicalOperator = radioButtonVariationDilation.getText();
		
	    //*****************************************************************************************
	    JLabel labelShowLastMorphImg = new JLabel("Show last dilated/eroded image");
	    labelShowLastMorphImg.setToolTipText("Show last dilated/eroded image");
	    labelShowLastMorphImg.setHorizontalAlignment(JLabel.RIGHT);
	    
		checkBoxShowLastMorphImg = new JCheckBox();
		checkBoxShowLastMorphImg.setToolTipText("Show last dilated/eroded image");
		checkBoxShowLastMorphImg.setSelected(false);
		checkBoxShowLastMorphImg.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanShowLastMorphImg = checkBoxShowLastMorphImg.isSelected();
		    	logService.info(this.getClass().getName() + " Show last dilated/eroded image option set to " + booleanShowLastMorphImg);
		    	if (booleanProcessImmediately) btnProcessSingleImage.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 210;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelShowLastMorphImg, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 210;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxShowLastMorphImg, gbc);	
	    //initialize command variable
	    booleanShowLastMorphImg = checkBoxShowLastMorphImg.isSelected();
		
		//*****************************************************************************************
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Number of dilations");
		//int numEpsMax = Csaj2DFracDimMinkowskiCommand.getMaxBoxNumber((int)datasetIn.dimension(0), (int)datasetIn.dimension(1));
		int numEpsMax = 999999999; 
		spinnerModelNumEps= new SpinnerNumberModel(20, 1, numEpsMax, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
		spinnerNumEps.setValue(20);
		spinnerNumRegEnd.setValue(20);
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
		//Following run initiates a "ProcessAllImages" 
		Future<CommandModule> future = commandService.run(Csaj2DFracDimMinkowskiCommand.class, false,
														"datasetIn",                      datasetIn,  //is not automatically harvested in headless mode
														"processAll",					  processAll, //true for all
													
														"choiceRadioButt_ShapeType",      choiceRadioButt_ShapeType,
														"choiceRadioButt_MorphologicalOperator",  choiceRadioButt_MorphologicalOperator,
					
														"spinnerInteger_NumDilations",    spinnerInteger_NumEps, //WARNING: Exceptionally a different name
														"spinnerInteger_NumRegStart",     spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",       spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",       booleanShowDoubleLogPlot,
	
														"booleanShowLastMorphImg",        booleanShowLastMorphImg,
														
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
		tableOutName = Csaj2DFracDimMinkowskiCommand.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
