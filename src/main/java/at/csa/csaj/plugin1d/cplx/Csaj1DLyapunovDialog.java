/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DLyapunovDialog.java
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

package at.csa.csaj.plugin1d.cplx;

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
public class Csaj1DLyapunovDialog extends CsajDialog_1DPluginWithRegression {

	private static final long serialVersionUID = 7591512972492390804L;

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
	private JPanel       panelAlgorithm;
	private ButtonGroup  buttonGroupAlgorithm;
    private JRadioButton radioButtonRosenstein;
    private JRadioButton radioButtonKantz;
    private JRadioButton radioButtonDirect;
	private String       choiceRadioButt_Algorithm;
	
	private JLabel   labelEmbDim;
	private JSpinner spinnerEmbDim;
	private int      spinnerInteger_EmbDim;
	
	private JLabel   labelTau;
	private JSpinner spinnerTau;
	private int      spinnerInteger_Tau;
	
	private JLabel   labelSampFrequ;
	private JSpinner spinnerSampFrequ;
	private int      spinnerInteger_SampFrequ;
	
	private JLabel   labelNumPointPairs;
	private JSpinner spinnerNumPointPairs;
	private int      spinnerInteger_NumPointPairs;
	
	private JLabel   labelEps;
	private JSpinner spinnerEps;
	private float    spinnerFloat_Eps;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DLyapunovDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Lyapunov exponent");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************			
	    JLabel labelAlgorithm = new JLabel("Algorithm");
	    labelAlgorithm.setToolTipText("Type of algorithm");
	    labelAlgorithm.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupAlgorithm = new ButtonGroup();
		radioButtonRosenstein = new JRadioButton("Rosenstein");
		radioButtonKantz      = new JRadioButton("Kantz");
		radioButtonDirect     = new JRadioButton("Direct");
		radioButtonRosenstein.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonRosenstein.isSelected()) {
					choiceRadioButt_Algorithm = radioButtonRosenstein.getText();
					
					labelEmbDim.setEnabled(true);
					spinnerEmbDim.setEnabled(true);
						
					labelTau.setEnabled(true);
					spinnerTau.setEnabled(true);
			
					labelSampFrequ.setEnabled(true);
					spinnerSampFrequ.setEnabled(true);
		
					labelNumPointPairs.setEnabled(false);
					spinnerNumPointPairs.setEnabled(false);
					
					labelEps.setEnabled(false);
					spinnerEps.setEnabled(false);
				} 
				logService.info(this.getClass().getName() + " Algorithm type set to " + choiceRadioButt_Algorithm);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonKantz.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonKantz.isSelected()) {
					choiceRadioButt_Algorithm = radioButtonKantz.getText();
					
					labelEmbDim.setEnabled(true);
					spinnerEmbDim.setEnabled(true);
						
					labelTau.setEnabled(true);
					spinnerTau.setEnabled(true);
			
					labelSampFrequ.setEnabled(true);
					spinnerSampFrequ.setEnabled(true);
		
					labelNumPointPairs.setEnabled(true);
					spinnerNumPointPairs.setEnabled(true);
					
					labelEps.setEnabled(false);
					spinnerEps.setEnabled(false);
				}
				logService.info(this.getClass().getName() + " Algorithm type set to " + choiceRadioButt_Algorithm);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		radioButtonDirect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonDirect.isSelected()) {
					choiceRadioButt_Algorithm = radioButtonDirect.getText();
					
					labelEmbDim.setEnabled(false);
					spinnerEmbDim.setEnabled(false);
						
					labelTau.setEnabled(false);
					spinnerTau.setEnabled(false);
			
					labelSampFrequ.setEnabled(false);
					spinnerSampFrequ.setEnabled(false);
		
					labelNumPointPairs.setEnabled(false);
					spinnerNumPointPairs.setEnabled(false);
					
					labelEps.setEnabled(true);
					spinnerEps.setEnabled(true);
				}
				logService.info(this.getClass().getName() + " Algorithm type set to " + choiceRadioButt_Algorithm);
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
			}
		});
		buttonGroupAlgorithm.add(radioButtonRosenstein);
		buttonGroupAlgorithm.add(radioButtonKantz);
		buttonGroupAlgorithm.add(radioButtonDirect);
		radioButtonRosenstein.setSelected(true);
		
		panelAlgorithm = new JPanel();
		panelAlgorithm.setToolTipText("Type of algorithm");
		panelAlgorithm.setLayout(new BoxLayout(panelAlgorithm, BoxLayout.Y_AXIS)); 
	    panelAlgorithm.add(radioButtonRosenstein);
	    panelAlgorithm.add(radioButtonKantz);
	    panelAlgorithm.add(radioButtonDirect);
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelAlgorithm, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelAlgorithm, gbc);
	    //initialize command variable
		if (radioButtonRosenstein.isSelected()) choiceRadioButt_Algorithm = radioButtonRosenstein.getText();
		if (radioButtonKantz.isSelected())      choiceRadioButt_Algorithm = radioButtonKantz.getText();
		if (radioButtonDirect.isSelected())     choiceRadioButt_Algorithm = radioButtonDirect.getText();
		
		//*****************************************************************************************
	    labelEmbDim = new JLabel("Embedding dimension");
	    labelEmbDim.setToolTipText("Embedding dimension of phase space");
	    labelEmbDim.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelEmbDim = new SpinnerNumberModel(2, 1, 999999999, 1); // initial, min, max, step
        spinnerEmbDim = new JSpinner(spinnerModelEmbDim);
        spinnerEmbDim.setToolTipText("Embedding dimension of phase space");
        spinnerEmbDim.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_EmbDim = (int)spinnerEmbDim.getValue();
                logService.info(this.getClass().getName() + " Embedding dimension set to " + spinnerInteger_EmbDim);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelEmbDim, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerEmbDim, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_EmbDim = (int)spinnerEmbDim.getValue();
	    
		//*****************************************************************************************
	    labelTau = new JLabel("Tau");
	    labelTau.setToolTipText("Time lag/delay of phase space reconstruction");
	    labelTau.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelTau = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerTau = new JSpinner(spinnerModelTau);
        spinnerTau.setToolTipText("Time lag/delay of phase space reconstruction");
        spinnerTau.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Tau = (int)spinnerTau.getValue();
                logService.info(this.getClass().getName() + " Tau set to " + spinnerInteger_Tau);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelTau, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerTau, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Tau = (int)spinnerTau.getValue();
	    
		//*****************************************************************************************
	    labelSampFrequ = new JLabel("Sampl. frequ.(Hz)");
	    labelSampFrequ.setToolTipText("Sampling frequency. If not known set it to 1");
	    labelSampFrequ.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelSampFrequ = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerSampFrequ = new JSpinner(spinnerModelSampFrequ);
        spinnerSampFrequ.setToolTipText("Sampling frequency. If not known set it to 1");
        spinnerSampFrequ.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_SampFrequ = (int)spinnerSampFrequ.getValue();
                logService.info(this.getClass().getName() + " Sampling frequency set to " + spinnerInteger_SampFrequ);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSampFrequ,  gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSampFrequ, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_SampFrequ = (int)spinnerSampFrequ.getValue();
	    
		//*****************************************************************************************
	    labelNumPointPairs = new JLabel("Point pairs #");
	    labelNumPointPairs.setToolTipText("Number of point pairs for which the divergences are computed");
	    labelNumPointPairs.setEnabled(false);
	    labelNumPointPairs.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelNumPointPairs = new SpinnerNumberModel(10, 0, 999999999, 1); // initial, min, max, step
        spinnerNumPointPairs = new JSpinner(spinnerModelNumPointPairs);
        spinnerNumPointPairs.setToolTipText("Number of point pairs for which the divergences are computed");
        spinnerNumPointPairs.setEnabled(false);
        spinnerNumPointPairs.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumPointPairs = (int)spinnerNumPointPairs.getValue();
                logService.info(this.getClass().getName() + " Number of point pairs set to " + spinnerInteger_NumPointPairs);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumPointPairs, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumPointPairs, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumPointPairs = (int)spinnerNumPointPairs.getValue();
	    
		//*****************************************************************************************
	    labelEps = new JLabel("Eps");
	    labelEps.setToolTipText("Epsilon of neighboring data points");
	    labelEps.setEnabled(false);
	    labelEps.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelEps = new SpinnerNumberModel(0.001, 0.0, 999999999.0, 0.01); // initial, min, max, step
        spinnerEps = new JSpinner(spinnerModelEps);
        spinnerEps.setToolTipText("Epsilon of neighboring data points");
        spinnerEps.setEnabled(false);
        spinnerEps.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {  
            	spinnerFloat_Eps = (float)((SpinnerNumberModel)spinnerEps.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Epsilon set to " + spinnerFloat_Eps);
                if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelEps, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerEps, gbc);	    
	    
	    //initialize command variable
	    spinnerFloat_Eps = (float)((SpinnerNumberModel)spinnerEps.getModel()).getNumber().doubleValue();
		
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)
		labelNumEps.setText("Maximum delay k");
		//int numEpsMax = Csaj1DDFACmd.getMaxBoxNumber(numRows);
		spinnerModelNumEps = new SpinnerNumberModel(20, 3, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
	
		spinnerModelNumRegEnd = new SpinnerNumberModel(8, 2, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegEnd.setModel(spinnerModelNumRegEnd);
		
		spinnerInteger_NumEps      = (int)spinnerNumEps.getValue();
		spinnerInteger_NumRegEnd   = (int)spinnerNumRegEnd.getValue();	
		
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
		Future<CommandModule> future = commandService.run(Csaj1DLyapunovCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
			
														"choiceRadioButt_Algorithm",     choiceRadioButt_Algorithm,
														"spinnerInteger_EmbDim",         spinnerInteger_EmbDim,
														"spinnerInteger_Tau",            spinnerInteger_Tau,
														"spinnerInteger_SampFrequ",      spinnerInteger_SampFrequ,
														"spinnerInteger_NumPointPairs",  spinnerInteger_NumPointPairs,
														"spinnerFloat_Eps",              spinnerFloat_Eps,
														
														"spinnerInteger_KMax",           spinnerInteger_NumEps,
														"spinnerInteger_NumRegStart",    spinnerInteger_NumRegStart,
														"spinnerInteger_NumRegEnd",      spinnerInteger_NumRegEnd,
														"booleanShowDoubleLogPlot",      booleanShowDoubleLogPlot,
														
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
		tableOutName = Csaj1DLyapunovCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
