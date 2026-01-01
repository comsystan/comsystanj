/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DGeneralisedEntropiesDialog.java
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

package at.csa.csaj.plugin2d.ent;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
import at.csa.csaj.commons.CsajDialog_2DPlugin;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj2DGeneralisedEntropiesDialog extends CsajDialog_2DPlugin {

	private static final long serialVersionUID = -8523048595497569720L;

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
  	private JComboBox<String> comboBoxProbabilityType;
	private String   choiceRadioButt_ProbabilityType;
	
	private JLabel   labelLag; 
	private JSpinner spinnerLag;
	private int      spinnerInteger_Lag;
	
	private JLabel    labelSkipZeroes;
	private JCheckBox checkBoxSkipZeroes;
	private boolean   booleanSkipZeroes;

	private JSpinner spinnerMinQ;
	private int spinnerInteger_MinQ;

	private JSpinner spinnerMaxQ;
	private int spinnerInteger_MaxQ;

	private JSpinner spinnerMinEta;
	private float spinnerFloat_MinEta;
	
	private JSpinner spinnerMaxEta;
	private float spinnerFloat_MaxEta;

	private JSpinner spinnerMinKappa;
	private float spinnerFloat_MinKappa;
	
	private JSpinner spinnerMaxKappa;
	private float spinnerFloat_MaxKappa;
	
	private JSpinner spinnerMinB;
	private float spinnerFloat_MinB;
	
	private JSpinner spinnerMaxB;
	private float spinnerFloat_MaxB;

	private JSpinner spinnerMinBeta;
	private float spinnerFloat_MinBeta;
	
	private JSpinner spinnerMaxBeta;
	private float spinnerFloat_MaxBeta;
	
	private JSpinner spinnerMinGamma;
	private float spinnerFloat_MinGamma;
	
	private JSpinner spinnerMaxGamma;
	private float spinnerFloat_MaxGamma;
	
	private JCheckBox checkBoxShowRenyiPlot;
	private boolean booleanShowRenyiPlot;
	
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
	public Csaj2DGeneralisedEntropiesDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("2D Generalised entropies");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelProbabilityType = new JLabel("Probability type");
	    labelProbabilityType.setToolTipText("Selection of probability type");
	    labelProbabilityType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Grey values", "Pairwise differences"};// "Sum of differences", "SD"}, 
		comboBoxProbabilityType = new JComboBox<String>(options);
		comboBoxProbabilityType.setToolTipText("Selection of probability type");
	    comboBoxProbabilityType.setEditable(false);
	    comboBoxProbabilityType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_ProbabilityType = (String)comboBoxProbabilityType.getSelectedItem();
				logService.info(this.getClass().getName() + " Probability type set to " + choiceRadioButt_ProbabilityType);
				//Lag must always be 1 for Sequence values
				if (choiceRadioButt_ProbabilityType.equals("Grey values")) {
					labelLag.setEnabled(false);
					spinnerLag.setEnabled(false);	
					spinnerLag.setValue(1);
				}
				else {
					labelLag.setEnabled(true);
					spinnerLag.setEnabled(true);	
				}
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelProbabilityType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxProbabilityType, gbc);
	    //initialize command variable
	    choiceRadioButt_ProbabilityType = (String)comboBoxProbabilityType.getSelectedItem();
	    
	    //*****************************************************************************************
	    labelLag = new JLabel("Lag");
	    labelLag.setToolTipText("Delta (difference) between two data points");
	    labelLag.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelLag = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerLag = new JSpinner(spinnerModelLag);
        spinnerLag.setToolTipText("Delta (difference) between two data points");
        spinnerLag.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Lag = (int)spinnerLag.getValue();
            	
            	if ((spinnerInteger_Lag > 1) && (String)comboBoxProbabilityType.getSelectedItem() == "Grey values") {
        			spinnerLag.setValue(1);
                	logService.info(this.getClass().getName() + " Lag > 1 not possible for Grey values");
                }
                
            	spinnerInteger_Lag = (int)spinnerLag.getValue();
                logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_Lag);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelLag, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerLag, gbc);	    
	    //initialize command variable
	    spinnerInteger_Lag = (int)spinnerLag.getValue();
	    
	    //*****************************************************************************************
	    labelSkipZeroes = new JLabel("Skip zero values");
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
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSkipZeroes, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxSkipZeroes, gbc);	
	    //initialize command variable
	    booleanSkipZeroes = checkBoxSkipZeroes.isSelected();
	    
	    //*****************************************************************************************
	    JLabel labelMinQ = new JLabel("Min q");
	    labelMinQ.setToolTipText("Minimum q for Renyi, Tsallis, SNorm and SEscort entropies");
	    labelMinQ.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMinQ = new SpinnerNumberModel(-5, -1000, 1000, 1); // initial, min, max, step
        spinnerMinQ = new JSpinner(spinnerModelMinQ);
        spinnerMinQ.setToolTipText("Minimum q for Renyi, Tsallis, SNorm and SEscort entropies");
        spinnerMinQ.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_MinQ = (int)spinnerMinQ.getValue();
            	spinnerInteger_MaxQ = (int)spinnerMaxQ.getValue();      
            	if (spinnerInteger_MinQ >= spinnerInteger_MaxQ) {
            		spinnerMinQ.setValue((int)spinnerMaxQ.getValue() - 1);
            		spinnerInteger_MinQ = (int)spinnerMinQ.getValue();
            	}	
                logService.info(this.getClass().getName() + " MinQ set to " + spinnerInteger_MinQ);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMinQ, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMinQ, gbc);	    
	    //initialize command variable
	    spinnerInteger_MinQ = (int)spinnerMinQ.getValue();    
	    //*****************************************************************************************
	    JLabel labelMaxQ = new JLabel("Max q");
	    labelMaxQ.setToolTipText("Maximum q for Renyi, Tsallis, SNorm and SEscort entropies");
	    labelMaxQ.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMaxQ = new SpinnerNumberModel(5, -1000, 1000, 1); // initial, min, max, step
        spinnerMaxQ = new JSpinner(spinnerModelMaxQ);
        spinnerMaxQ.setToolTipText("Maximum q for Renyi, Tsallis, SNorm and SEscort entropies");
        spinnerMaxQ.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_MaxQ = (int)spinnerMaxQ.getValue();
            	spinnerInteger_MinQ = (int)spinnerMinQ.getValue();
            	if (spinnerInteger_MaxQ <= spinnerInteger_MinQ) {
            		spinnerMaxQ.setValue((int)spinnerMinQ.getValue() + 1);
            		spinnerInteger_MaxQ = (int)spinnerMaxQ.getValue();
            	}
                logService.info(this.getClass().getName() + " MaxQ set to " + spinnerInteger_MaxQ);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMaxQ, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMaxQ, gbc);	    
	    //initialize command variable
	    spinnerInteger_MaxQ = (int)spinnerMaxQ.getValue();    
	    //*****************************************************************************************
	    JLabel labelMinEta = new JLabel("Min eta");
	    labelMinEta.setToolTipText("Minimum eta for SEta entropies");
	    labelMinEta.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMinEta = new SpinnerNumberModel(0.1, 0.0, 1.0, 0.1); // initial, min, max, step
        spinnerMinEta = new JSpinner(spinnerModelMinEta);
        spinnerMinEta.setToolTipText("Minimum eta for SEta entropies");
        spinnerMinEta.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MinEta = (float)((SpinnerNumberModel)spinnerMinEta.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Min eta set to " + spinnerFloat_MinEta);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMinEta, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMinEta, gbc);	    
	    //initialize command variable
	    spinnerFloat_MinEta = (float)((SpinnerNumberModel)spinnerMinEta.getModel()).getNumber().doubleValue();
	    //*****************************************************************************************
	    JLabel labelMaxEta = new JLabel("Max eta");
	    labelMaxEta.setToolTipText("Maximum eta for SEta entropies");
	    labelMaxEta.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMaxEta = new SpinnerNumberModel(1.0, 0.0, 1.0, 0.1); // initial, min, max, step
        spinnerMaxEta = new JSpinner(spinnerModelMaxEta);
        spinnerMaxEta.setToolTipText("Maximum eta for SEta entropies");
        spinnerMaxEta.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MaxEta = (float)((SpinnerNumberModel)spinnerMaxEta.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Max eta set to " + spinnerFloat_MaxEta);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMaxEta, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMaxEta, gbc);	    
	    //initialize command variable
	    spinnerFloat_MaxEta = (float)((SpinnerNumberModel)spinnerMaxEta.getModel()).getNumber().doubleValue();
	    //*****************************************************************************************
	    JLabel labelMinKappa = new JLabel("Min kappa");
	    labelMinKappa.setToolTipText("Minimum kappa for SKappa entropies");
	    labelMinKappa.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMinKappa = new SpinnerNumberModel(0.1, 0.0, 1.0, 0.1); // initial, min, max, step
        spinnerMinKappa = new JSpinner(spinnerModelMinKappa);
        spinnerMinKappa.setToolTipText("Minimum kappa for SKappa entropies");
        spinnerMinKappa.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MinKappa = (float)((SpinnerNumberModel)spinnerMinKappa.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Min kappa set to " + spinnerFloat_MinKappa);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 7;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMinKappa, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 7;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMinKappa, gbc);	    
	    //initialize command variable
	    spinnerFloat_MinKappa = (float)((SpinnerNumberModel)spinnerMinKappa.getModel()).getNumber().doubleValue();  
	    //*****************************************************************************************
	    JLabel labelMaxKappa = new JLabel("Max kappa");
	    labelMaxKappa.setToolTipText("Maximum kappa for SKappa entropies");
	    labelMaxKappa.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMaxKappa = new SpinnerNumberModel(0.9, 0.0, 1.0, 0.1); // initial, min, max, step
        spinnerMaxKappa = new JSpinner(spinnerModelMaxKappa);
        spinnerMaxKappa.setToolTipText("Maximum kappa for SKappa entropies");
        spinnerMaxKappa.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MaxKappa = (float)((SpinnerNumberModel)spinnerMaxKappa.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Max kappa set to " + spinnerFloat_MaxKappa);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 8;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMaxKappa, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 8;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMaxKappa, gbc);	    
	    //initialize command variable
	    spinnerFloat_MaxKappa = (float)((SpinnerNumberModel)spinnerMaxKappa.getModel()).getNumber().doubleValue();
	    //*****************************************************************************************
	    JLabel labelMinB = new JLabel("Min B");
	    labelMinB.setToolTipText("Minimum B for SB entropies");
	    labelMinB.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMinB = new SpinnerNumberModel(1.0, 0.0, 100000.0, 1.0); // initial, min, max, step
        spinnerMinB = new JSpinner(spinnerModelMinB);
        spinnerMinB.setToolTipText("Minimum B for SB entropies");
        spinnerMinB.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MinB = (float)((SpinnerNumberModel)spinnerMinB.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Min B set to " + spinnerFloat_MinB);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 9;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMinB, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 9;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMinB, gbc);	    
	    //initialize command variable
	    spinnerFloat_MinB = (float)((SpinnerNumberModel)spinnerMinB.getModel()).getNumber().doubleValue();   
	    //*****************************************************************************************
	    JLabel labelMaxB = new JLabel("Max B");
	    labelMaxB.setToolTipText("Maximum B for SB entropies");
	    labelMaxB.setHorizontalAlignment(JLabel.RIGHT);

	    SpinnerNumberModel spinnerModelMaxB = new SpinnerNumberModel(10.0, 1.0, 100000.0, 1.0); // initial, min, max, step
        spinnerMaxB = new JSpinner(spinnerModelMaxB);
        spinnerMaxB.setToolTipText("Maximum B for SB entropies");
        spinnerMaxB.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MaxB = (float)((SpinnerNumberModel)spinnerMaxB.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Max B set to " + spinnerFloat_MaxB);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMaxB, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMaxB, gbc);	    
	    //initialize command variable
	    spinnerFloat_MaxB = (float)((SpinnerNumberModel)spinnerMaxB.getModel()).getNumber().doubleValue(); 
	    //*****************************************************************************************
	    JLabel labelMinBeta = new JLabel("Min beta");
	    labelMinBeta.setToolTipText("Minimum beta for SBeta entropies");
	    labelMinBeta.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMinBeta = new SpinnerNumberModel(0.5, -100000.0, 100000.0, 0.1); // initial, min, max, step
        spinnerMinBeta = new JSpinner(spinnerModelMinBeta);
        spinnerMinBeta.setToolTipText("Minimum beta for SBeta entropies");
        spinnerMinBeta.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MinBeta = (float)((SpinnerNumberModel)spinnerMinBeta.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Min beta set to " + spinnerFloat_MinBeta);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 11;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMinBeta, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 11;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMinBeta, gbc);	    
	    //initialize command variable
	    spinnerFloat_MinBeta = (float)((SpinnerNumberModel)spinnerMinBeta.getModel()).getNumber().doubleValue();  
	    //*****************************************************************************************
	    JLabel labelMaxBeta = new JLabel("Max beta");
	    labelMaxBeta.setToolTipText("Maximum beta for SBeta entropies");
	    labelMaxBeta.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMaxBeta = new SpinnerNumberModel(1.5, -100000.0, 100000.0, 0.1); // initial, min, max, step
        spinnerMaxBeta = new JSpinner(spinnerModelMaxBeta);
        spinnerMaxBeta.setToolTipText("Maximum beta for SBeta entropies");
        spinnerMaxBeta.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MaxBeta = (float)((SpinnerNumberModel)spinnerMaxBeta.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Max beta set to " + spinnerFloat_MaxBeta);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 12;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMaxBeta, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 12;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMaxBeta, gbc);	    
	    //initialize command variable
	    spinnerFloat_MaxBeta = (float)((SpinnerNumberModel)spinnerMaxBeta.getModel()).getNumber().doubleValue();  
	    //*****************************************************************************************
	    JLabel labelMinGamma = new JLabel("Min gamma");
	    labelMinGamma.setToolTipText("Minimum gamma for SGamma entropies");
	    labelMinGamma.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMinGamma = new SpinnerNumberModel(0.1, 0.0, 100000.0, 1.0); // initial, min, max, step
        spinnerMinGamma = new JSpinner(spinnerModelMinGamma);
        spinnerMinGamma.setToolTipText("Minimum gamma for SGamma entropies");
        spinnerMinGamma.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MinGamma = (float)((SpinnerNumberModel)spinnerMinGamma.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Min gamma set to " + spinnerFloat_MinGamma);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 13;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMinGamma, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 13;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMinGamma, gbc);	    
	    //initialize command variable
	    spinnerFloat_MinGamma = (float)((SpinnerNumberModel)spinnerMinGamma.getModel()).getNumber().doubleValue();  
	    //*****************************************************************************************
	    JLabel labelMaxGamma = new JLabel("Max gamma");
	    labelMaxGamma.setToolTipText("Maximum gamma for SGamma entropies");
	    labelMaxGamma.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelMaxGamma = new SpinnerNumberModel(1.0, 0.0, 100000.0, 1.0); // initial, min, max, step
        spinnerMaxGamma = new JSpinner(spinnerModelMaxGamma);
        spinnerMaxGamma.setToolTipText("Maximum gamma for SGamma entropies");
        spinnerMaxGamma.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_MaxGamma = (float)((SpinnerNumberModel)spinnerMaxGamma.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Max gamma set to " + spinnerFloat_MaxGamma);
                if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 14;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelMaxGamma, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 14;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerMaxGamma, gbc);	    
	    //initialize command variable
	    spinnerFloat_MaxGamma = (float)((SpinnerNumberModel)spinnerMaxGamma.getModel()).getNumber().doubleValue();   
	    //*****************************************************************************************
	    JLabel labelShowRenyiPlot = new JLabel("Show Renyi plot");
	    labelShowRenyiPlot.setToolTipText("Show Renyi plot");
	    labelShowRenyiPlot.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxShowRenyiPlot = new JCheckBox();
		checkBoxShowRenyiPlot.setToolTipText("Show Renyi plot");
		checkBoxShowRenyiPlot.setSelected(false);
		checkBoxShowRenyiPlot.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanShowRenyiPlot = checkBoxShowRenyiPlot.isSelected();	    
				logService.info(this.getClass().getName() + " Show Renyi plot set to " + booleanShowRenyiPlot);	
				if (booleanProcessImmediately) btnProcessSingleImage.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 15;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelShowRenyiPlot, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 15;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxShowRenyiPlot, gbc);	
	
	    //initialize command variable
	    booleanShowRenyiPlot = checkBoxShowRenyiPlot.isSelected();	 
	    
	    //*****************************************************************************************
		//Change items defined in the super class(es)
		
	    //*****************************************************************************************
	    pack(); //IMPORTANT //Otherwise some unexpected padding may occur
	    //*****************************************************************************************
		//Do additional things
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj2DGeneralisedEntropiesCmd.class, false,
														"datasetIn",                        datasetIn,  //is not automatically harvested in headless mode
														"processAll",					    processAll, //true for all
														
														"choiceRadioButt_ProbabilityType",	choiceRadioButt_ProbabilityType,
														"spinnerInteger_Lag",				spinnerInteger_Lag,
														"booleanSkipZeroes",                booleanSkipZeroes,	
														"spinnerInteger_MinQ",				spinnerInteger_MinQ,
														"spinnerInteger_MaxQ",				spinnerInteger_MaxQ,
														"spinnerFloat_MinEta",				spinnerFloat_MinEta,
														"spinnerFloat_MaxEta",				spinnerFloat_MaxEta,
														"spinnerFloat_MinKappa",			spinnerFloat_MinKappa,
														"spinnerFloat_MaxKappa",			spinnerFloat_MaxKappa,
														"spinnerFloat_MinB",				spinnerFloat_MinB,
														"spinnerFloat_MaxB",				spinnerFloat_MaxB,
														"spinnerFloat_MinBeta",				spinnerFloat_MinBeta,
														"spinnerFloat_MaxBeta",				spinnerFloat_MaxBeta,
														"spinnerFloat_MinGamma",			spinnerFloat_MinGamma,
														"spinnerFloat_MaxGamma",			spinnerFloat_MaxGamma,
														"booleanShowRenyiPlot",				booleanShowRenyiPlot,
															
														"booleanOverwriteDisplays",			booleanOverwriteDisplays,
														"booleanProcessImmediately",		booleanProcessImmediately,
														"spinnerInteger_NumImageSlice",		spinnerInteger_NumImageSlice
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
		tableOutName = Csaj2DGeneralisedEntropiesCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
