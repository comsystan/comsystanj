/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimHiguchi1DDialog.java
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
public class Csaj2DFracDimHiguchi1DDialog extends CsajDialog_2DPluginWithRegression {

	private static final long serialVersionUID = 8025805789017474802L;

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
 	private JComboBox<String> comboBoxMethod;
	private String            choiceRadioButt_Method;
	
	private JLabel    labelShowSomeRadialLinePlots;
	private JCheckBox checkBoxShowSomeRadialLinePlots;
	private boolean   booleanShowSomeRadialLinePlots;
	
	private JLabel    labelGetAllRadialDhValues;
	private JCheckBox checkBoxGetAllRadialDhValues;
	private boolean   booleanGetAllRadialDhValues;
	
	private JCheckBox checkBoxSkipZeroes;
	private boolean   booleanSkipZeroes;

	private JCheckBox checkBoxOnlyHighQualityRegressions;
	private boolean   booleanOnlyHighQualityRegressions;
	
	
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
	public Csaj2DFracDimHiguchi1DDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Higuchi1D dimension");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************
	    JLabel labelMethod = new JLabel("1D Extraction method");
	    labelMethod.setToolTipText("Type of 1D grey value profile extraction");
	    labelMethod.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Single centered row/column",
							"Single meander row/column",
							"Mean of all rows/columns",
							"Mean of      4 radial lines [0-180°]",
							"Mean of 180 radial lines [0-180°]"
		};
		comboBoxMethod = new JComboBox<String>(options);
		comboBoxMethod.setToolTipText("Type of 1D grey value profile extraction");
	    comboBoxMethod.setEditable(false);
	    comboBoxMethod.setSelectedItem("Single meander row/column");
	    comboBoxMethod.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_Method = (String)comboBoxMethod.getSelectedItem();
				logService.info(this.getClass().getName() + " Method set to " + choiceRadioButt_Method);
				if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-180°]") || choiceRadioButt_Method.equals("Mean of      4 radial lines [0-180°]")) {		
					labelShowSomeRadialLinePlots.setEnabled(true);
					checkBoxShowSomeRadialLinePlots.setEnabled(true);
					//booleanShowSomeRadialLinePlots = true;
					labelGetAllRadialDhValues.setEnabled(true);
					checkBoxGetAllRadialDhValues.setEnabled(true);
					//checkBoxGetAllRadialDhValues.setSelected(rue;)
					//booleanGetAllRadialDhValues = true;		
					//logService.info(this.getClass().getName() + "  set to " + ?);
				} else {
					labelShowSomeRadialLinePlots.setEnabled(false);
					checkBoxShowSomeRadialLinePlots.setEnabled(false);
					checkBoxShowSomeRadialLinePlots.setSelected(false);
					booleanShowSomeRadialLinePlots = false;
					logService.info(this.getClass().getName() + "  Show some radila line plots option set to " + booleanShowSomeRadialLinePlots);
					labelGetAllRadialDhValues.setEnabled(false);
					checkBoxGetAllRadialDhValues.setEnabled(false);
					checkBoxGetAllRadialDhValues.setSelected(false);
					booleanGetAllRadialDhValues = false;		
					logService.info(this.getClass().getName() + "  Get all Dh values option set to " + booleanGetAllRadialDhValues);
				}
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMethod, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxMethod, gbc);
	  
	    //initialize command variable
	    choiceRadioButt_Method = (String)comboBoxMethod.getSelectedItem();

	    //*****************************************************************************************
	    JLabel labelSkipZeroes = new JLabel("Skip zero values");
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
		    	if (booleanProcessImmediately) btnProcessSingleImage.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSkipZeroes, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxSkipZeroes, gbc);	
	    //initialize command variable
	    booleanSkipZeroes = checkBoxSkipZeroes.isSelected();
	    
	    //*****************************************************************************************
	  	labelShowSomeRadialLinePlots = new JLabel("Show some radial line plots");
  		labelShowSomeRadialLinePlots.setToolTipText("Show some radial line plots");
  		labelShowSomeRadialLinePlots.setHorizontalAlignment(JLabel.RIGHT);
  		labelShowSomeRadialLinePlots.setEnabled(false);
  		
  		checkBoxShowSomeRadialLinePlots = new JCheckBox();
  		checkBoxShowSomeRadialLinePlots.setToolTipText("Show some radial line plots");
  		checkBoxShowSomeRadialLinePlots.setEnabled(false);
  		checkBoxShowSomeRadialLinePlots.setSelected(false);
  		checkBoxShowSomeRadialLinePlots.addItemListener(new ItemListener() {
  			@Override
  		    public void itemStateChanged(ItemEvent e) {
  		    	booleanShowSomeRadialLinePlots = checkBoxShowSomeRadialLinePlots.isSelected();	    
  				logService.info(this.getClass().getName() + " Show some radial line plots option set to " + booleanShowSomeRadialLinePlots);
  				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
  		    }
  		});
  		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
  	    gbc.gridy = 2;
  	    gbc.anchor = GridBagConstraints.EAST; //right
  	    contentPanel.add(labelShowSomeRadialLinePlots, gbc);
  	    gbc.gridx = 1;
  	    gbc.gridy = 2;
  	    gbc.anchor = GridBagConstraints.WEST; //left
  	    contentPanel.add(checkBoxShowSomeRadialLinePlots, gbc);	
  	 
  	    //initialize command variable
  	    booleanShowSomeRadialLinePlots = checkBoxShowSomeRadialLinePlots.isSelected();	 
	   
		//*****************************************************************************************
		labelGetAllRadialDhValues = new JLabel("Get Dh values of all radial directions");
		labelGetAllRadialDhValues.setToolTipText("Get Dh values of all radial directions");
		labelGetAllRadialDhValues.setHorizontalAlignment(JLabel.RIGHT);
		labelGetAllRadialDhValues.setEnabled(false);
		
		checkBoxGetAllRadialDhValues = new JCheckBox();
		checkBoxGetAllRadialDhValues.setToolTipText("Get Dh values of all radial directions");
		checkBoxGetAllRadialDhValues.setEnabled(false);
		checkBoxGetAllRadialDhValues.setSelected(false);
		checkBoxGetAllRadialDhValues.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanGetAllRadialDhValues = checkBoxGetAllRadialDhValues.isSelected();	    
				logService.info(this.getClass().getName() + " Get Dh values option set to " + booleanGetAllRadialDhValues);
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelGetAllRadialDhValues, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxGetAllRadialDhValues, gbc);	
	 
	    //initialize command variable
	    booleanGetAllRadialDhValues = checkBoxGetAllRadialDhValues.isSelected();	 
	    
	    //*****************************************************************************************
	    JLabel labelOnlyHighQualityRegressions = new JLabel("Only high quality regressions");
	    labelOnlyHighQualityRegressions.setToolTipText("Takes for multiple grey value profiles only those with a coefficient of determination > 0.9");
	    labelOnlyHighQualityRegressions.setHorizontalAlignment(JLabel.RIGHT);
	    
		checkBoxOnlyHighQualityRegressions = new JCheckBox();
		checkBoxOnlyHighQualityRegressions.setToolTipText("Takes for multiple grey value profiles only those with a coefficient of determination > 0.9");
		checkBoxOnlyHighQualityRegressions.setSelected(true);
		checkBoxOnlyHighQualityRegressions.addItemListener(new ItemListener() {
			@Override
		    public void itemStateChanged(ItemEvent e) {
		    	booleanOnlyHighQualityRegressions = checkBoxOnlyHighQualityRegressions.isSelected();
		    	logService.info(this.getClass().getName() + " Only high quality regressions option set to " + booleanOnlyHighQualityRegressions);
		    	if (booleanProcessImmediately) btnProcessSingleImage.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 150;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOnlyHighQualityRegressions, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 150;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxOnlyHighQualityRegressions, gbc);	
	    //initialize command variable
	    booleanOnlyHighQualityRegressions = checkBoxOnlyHighQualityRegressions.isSelected();
	      
		//*****************************************************************************************    
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("k");
		int numEpsMax = Csaj2DFracDimHiguchi1DCmd.getMaxK((int)datasetIn.dimension(0), (int)datasetIn.dimension(1));
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
		//Following run initiates a "ProcessAllImages" 
		Future<CommandModule> future = commandService.run(Csaj2DFracDimHiguchi1DCmd.class, false,
														"datasetIn",                         datasetIn,  //is not automatically harvested in headless mode
														"processAll",					     processAll, //true for all
														
														"choiceRadioButt_Method",            choiceRadioButt_Method,
														"booleanSkipZeroes", 				 booleanSkipZeroes,
														"booleanShowSomeRadialLinePlots",    booleanShowSomeRadialLinePlots,
														"booleanGetAllRadialDhValues",       booleanGetAllRadialDhValues, 
							
														"spinnerInteger_KMax",               spinnerInteger_NumEps, //WARNING: Exceptionally a different name
														"spinnerInteger_NumRegStart",        spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",          spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",          booleanShowDoubleLogPlot,
														"booleanOnlyHighQualityRegressions", booleanOnlyHighQualityRegressions,
														
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
		//tableOutName =(String)commandModule.getInfo().getLabel(); //Unfortunately, it is not possible to get this label inside the Command plugin class
		tableOutName = Csaj2DFracDimHiguchi1DCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
