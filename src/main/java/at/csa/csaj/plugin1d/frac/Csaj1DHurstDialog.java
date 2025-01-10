/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DHurstDialog.java
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import at.csa.csaj.commons.CsajUtil_GenerateInterval;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj1DHurstDialog extends CsajDialog_1DPluginWithRegression {

	private static final long serialVersionUID = 4273848943682988217L;

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
	private JLabel    labelHurstHK;
	private JCheckBox checkBoxHurstHK;
	private boolean   booleanHurstHK;
	
	private JLabel    labelHurstRS;
	private JCheckBox checkBoxHurstRS;
	private boolean   booleanHurstRS;
	
	private JLabel    labelHurstSP;
	private JCheckBox checkBoxHurstSP;
	private boolean   booleanHurstSP;
	
	private JLabel   labelHKN;
	private JSpinner spinnerHKN;
	private int      spinnerInteger_HKN;
	
	private JLabel   labelSPMaxLag;
	private JSpinner spinnerSPMaxLag;
	private int      spinnerInteger_SPMaxLag;
	
	private JLabel			  labelSurrBoxHurstType;
	private JComboBox<String> comboBoxSurrBoxHurstType;
	private String            choiceRadioButt_SurrBoxHurstType;

	//Some default @Parameters are already defined in the super class

	/**
	 * Create the dialog.
	 */
	public Csaj1DHurstDialog(Context context, DefaultTableDisplay defaultTableDisplay) {
			
		super(context, defaultTableDisplay);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.defaultTableDisplay = defaultTableDisplay;
			
		//Title of plugin
		//Overwrite
		setTitle("1D Hurst coefficient (HK,RS,SP)");

		//Add specific GUI elements according to Command @Parameter GUI elements  
		//*****************************************************************************************
	    labelHurstHK = new JLabel("fGn HK");
	    labelHurstHK.setToolTipText("Hurst-Kolmogorov method for Gaussian noise Hurst coefficient");
	    labelHurstHK.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxHurstHK = new JCheckBox();
		checkBoxHurstHK.setToolTipText("Hurst-Kolmogorov method for Gaussian noise Hurst coefficient");
		checkBoxHurstHK.setSelected(false);
		checkBoxHurstHK.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanHurstHK = checkBoxHurstHK.isSelected();	
		    	
		    	if (booleanHurstHK) {
		    		labelHKN.setEnabled(true);
		    		spinnerHKN.setEnabled(true);
		    	} else {
		    		labelHKN.setEnabled(false);
		    		spinnerHKN.setEnabled(false);
		    	}
		    	
				logService.info(this.getClass().getName() + " fGn HK set to " + booleanHurstHK);	
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHurstHK, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxHurstHK, gbc);	
	
	    //initialize command variable
	    booleanHurstHK = checkBoxHurstHK.isSelected();	
	      
	    //*****************************************************************************************
	    labelHurstSP = new JLabel("fBm SP");
	    labelHurstSP.setToolTipText("Scaling method for Brownian motion Hurst coefficient");
	    labelHurstSP.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxHurstSP = new JCheckBox();
		checkBoxHurstSP.setToolTipText("Scaling method for Brownian motion Hurst coefficient");
		checkBoxHurstSP.setSelected(false);
		checkBoxHurstSP.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanHurstSP = checkBoxHurstSP.isSelected();	 
		    	
		    	if (booleanHurstSP) {
		    		labelSPMaxLag.setEnabled(true);
		    		spinnerSPMaxLag.setEnabled(true);
		    	} else {
		    		labelSPMaxLag.setEnabled(false);
		    		spinnerSPMaxLag.setEnabled(false);
		    	}
		    	
				logService.info(this.getClass().getName() + " fBm SP set to " + booleanHurstSP);	
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHurstSP, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxHurstSP, gbc);	
	
	    //initialize command variable
	    booleanHurstSP = checkBoxHurstSP.isSelected();	
	    
	    //*****************************************************************************************
	    labelHurstRS = new JLabel("fBm RS");
	    labelHurstRS.setToolTipText("Rescaled range method for Brownian motion Hurst coefficient");
	    labelHurstRS.setHorizontalAlignment(JLabel.RIGHT);
	  
		checkBoxHurstRS = new JCheckBox();
		checkBoxHurstRS.setToolTipText("Rescaled range method for Brownian motion Hurst coefficient");
		checkBoxHurstRS.setSelected(false);
		checkBoxHurstRS.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	booleanHurstRS = checkBoxHurstRS.isSelected();
		    	
		    	if (booleanHurstRS) {
		    		labelNumEps.setEnabled(true);
		    		spinnerNumEps.setEnabled(true);
		    		labelNumRegStart.setEnabled(true);
		    		spinnerNumRegStart.setEnabled(true);
		    		labelNumRegEnd.setEnabled(true);
		    		spinnerNumRegEnd.setEnabled(true);
		    		labelShowDoubleLogPlot.setEnabled(true);
		    		checkBoxShowDoubleLogPlot.setEnabled(true);
		    		
		    	} else {
		    		labelNumEps.setEnabled(false);
		    		spinnerNumEps.setEnabled(false);
		    		labelNumRegStart.setEnabled(false);
		    		spinnerNumRegStart.setEnabled(false);
		    		labelNumRegEnd.setEnabled(false);
		    		spinnerNumRegEnd.setEnabled(false);
		    		labelShowDoubleLogPlot.setEnabled(false);
		    		checkBoxShowDoubleLogPlot.setEnabled(false);
		    	}
		    	
				logService.info(this.getClass().getName() + "  fBm RS set to " + booleanHurstRS);	
				if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
		    }
		});
		gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHurstRS, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(checkBoxHurstRS, gbc);	
	
	    //initialize command variable
	    booleanHurstRS = checkBoxHurstRS.isSelected();	
	    
	    //*****************************************************************************************
	    labelHKN = new JLabel("(HK) n");
	    labelHKN.setToolTipText("Number of samples from the posterior distribution - default=500");
	    labelHKN.setEnabled(false);
	    labelHKN.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelHKN = new SpinnerNumberModel(500, 1, 999999999, 1); // initial, min, max, step
        spinnerHKN = new JSpinner(spinnerModelHKN);
        spinnerHKN.setToolTipText("Number of samples from the posterior distribution - default=500");
        spinnerHKN.setEnabled(false);
        spinnerHKN.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_HKN = (int)spinnerHKN.getValue();     	
            	if (spinnerInteger_HKN < 100) {
        			JOptionPane.showMessageDialog(null, "n should be higher for high quality estimates of H!/n>=100 is recommended", "Reliability warning", JOptionPane.WARNING_MESSAGE);
        		}   	
                logService.info(this.getClass().getName() + " (HK) n set to " + spinnerInteger_HKN);
                if (booleanProcessImmediately) btnProcessSingleColumn .doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHKN, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerHKN, gbc);	    
	    //initialize command variable
	    spinnerInteger_HKN = (int)spinnerHKN.getValue();
	        
	    //*****************************************************************************************
	    labelSPMaxLag = new JLabel("(SP) Max lag");
	    labelSPMaxLag.setToolTipText("Maximum lag between data values");
		labelSPMaxLag.setEnabled(false);
	    labelSPMaxLag.setHorizontalAlignment(JLabel.RIGHT);
	    
	    SpinnerNumberModel spinnerModelSPMaxLag = new SpinnerNumberModel(10, 3, 999999999, 1); // initial, min, max, step
        spinnerSPMaxLag = new JSpinner(spinnerModelSPMaxLag);
        spinnerSPMaxLag.setToolTipText("Maximum lag between data values");
		spinnerSPMaxLag.setEnabled(false);
        spinnerSPMaxLag.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_SPMaxLag = (int)spinnerSPMaxLag.getValue();
                logService.info(this.getClass().getName() + " (SP) Max lag set to " + spinnerInteger_SPMaxLag);
                if (booleanProcessImmediately) btnProcessSingleColumn .doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSPMaxLag, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSPMaxLag, gbc);	    
	    //initialize command variable
	    spinnerInteger_SPMaxLag = (int)spinnerSPMaxLag.getValue();
	    
		//*****************************************************************************************
	    labelSurrBoxHurstType = new JLabel("Hurst type");
	    labelSurrBoxHurstType.setToolTipText("Hurst for Surrogates, Subsequent boxes or Gliding box");
	    labelSurrBoxHurstType.setEnabled(false);
	    labelSurrBoxHurstType.setHorizontalAlignment(JLabel.RIGHT);
		
		String optionsSurrBoxHurstType[] = {"H (HK)", "H (RS)", "H (SP)"};
		comboBoxSurrBoxHurstType = new JComboBox<String>(optionsSurrBoxHurstType);
		comboBoxSurrBoxHurstType.setToolTipText("Hurst for Surrogates, Subsequent boxes or Gliding box");
	    comboBoxSurrBoxHurstType.setEnabled(false);
	    comboBoxSurrBoxHurstType.setEditable(false);
	    comboBoxSurrBoxHurstType.setSelectedItem("H (HK)");
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
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSurrBoxHurstType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxSurrBoxHurstType, gbc);
	    //initialize command variable
	    choiceRadioButt_SurrBoxHurstType = (String)comboBoxSurrBoxHurstType.getSelectedItem();
		
	    //*****************************************************************************************
		//Change/Override items defined in the super class(es)	
	    labelNumEps.setText("(RS) Number of windows");
		labelNumEps.setToolTipText("Number of unique window sizes - default=10");
		spinnerNumEps.setToolTipText("Number of unique window sizes - default=10");
		
		int numMaxRegEnd = (int) Math.floor((double)tableIn.getRowCount() / 1.0);
		spinnerModelNumEps= new SpinnerNumberModel(10, 1, 999999999, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumEps.setModel(spinnerModelNumEps);
	
		//Remove standard listener(s)
		ChangeListener[] listeners = spinnerNumEps.getListeners(ChangeListener.class);
		spinnerNumEps.removeChangeListener(listeners[1]); //The first(default) one should not be removed
	
		//Add specific listener
	    spinnerNumEps.addChangeListener(new ChangeListener() {
	    	@Override
            public void stateChanged(ChangeEvent e) {  		
	    		updateNumEps(); //Ausgelagert, damit andere Listeners auch den Code ausführen können.
            }
        });
	    
	    labelNumRegStart.setText("(RS) Win min");
	    labelNumRegStart.setToolTipText("Minimum size of window");
		spinnerModelNumRegStart = new SpinnerNumberModel(2, 1, numMaxRegEnd - 1, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double	
		spinnerNumRegStart.setModel(spinnerModelNumRegStart);
		spinnerNumRegStart.setToolTipText("Minimum size of window");
	
		//Remove standard listener(s)
		listeners = spinnerNumRegStart.getListeners(ChangeListener.class);
		spinnerNumRegStart.removeChangeListener(listeners[1]); //The first(default) one should not be removed
		
		//Add specific listener
	    spinnerNumRegStart.addChangeListener(new ChangeListener() {
	    	@Override
            public void stateChanged(ChangeEvent e) {
	  
	    		int valueNumEps      = (int)spinnerNumEps.getValue();
        		int valueNumRegStart = (int)spinnerNumRegStart.getValue();
        		int valueNumRegEnd   = (int)spinnerNumRegEnd.getValue();  
	    		if (valueNumRegStart >= valueNumRegEnd - 2) valueNumRegStart  = valueNumRegEnd - 2;	
        		if (valueNumRegStart < 1) valueNumRegStart = 1;
        		
        		spinnerNumRegStart.setValue(valueNumRegStart);
    			spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();
    			logService.info(this.getClass().getName() + " Regression start set to " + spinnerInteger_NumRegStart);
	 		
	    		updateNumEps();	
                //if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
	
	    labelNumRegEnd.setText("(RS) Win max");
	    labelNumRegEnd.setToolTipText("Maximum size of window");
		spinnerModelNumRegEnd = new SpinnerNumberModel(numMaxRegEnd, 3, numMaxRegEnd, 1); // initial, min, max, step NOTE: (int) cast because JSpinner interprets long as double   
		spinnerNumRegEnd.setModel(spinnerModelNumRegEnd);
		spinnerNumRegEnd.setToolTipText("Maximum size of window");
		
		//Remove standard listener(s)
		listeners = spinnerNumRegEnd.getListeners(ChangeListener.class);
		spinnerNumRegEnd.removeChangeListener(listeners[1]); //The first(default) one should not be removed
		
		//Add specific listener
	    spinnerNumRegEnd.addChangeListener(new ChangeListener() {
	    	@Override
            public void stateChanged(ChangeEvent e) {
	    		
	    		int valueNumEps      = (int)spinnerNumEps.getValue();
        		int valueNumRegStart = (int)spinnerNumRegStart.getValue();
        		int valueNumRegEnd   = (int)spinnerNumRegEnd.getValue();   		
        	
        		if (valueNumRegEnd <= valueNumRegStart + 2) valueNumRegEnd  = valueNumRegStart + 2;
  
        		spinnerNumRegEnd.setValue(valueNumRegEnd);
    			spinnerInteger_NumRegEnd = (int)spinnerNumRegEnd.getValue();
    			logService.info(this.getClass().getName() + " Regression end set to " + spinnerInteger_NumRegEnd);
	    		
	    		updateNumEps();
                //if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
            }
        });
	    
		spinnerInteger_NumEps      = (int)spinnerNumEps.getValue();
		spinnerInteger_NumRegStart = (int)spinnerNumRegStart.getValue();	
		spinnerInteger_NumRegEnd   = (int)spinnerNumRegEnd.getValue();	
		
		labelNumEps.setEnabled(false);
		spinnerNumEps.setEnabled(false);
		labelNumRegStart.setEnabled(false);
		spinnerNumRegStart.setEnabled(false);
		labelNumRegEnd.setEnabled(false);
		spinnerNumRegEnd.setEnabled(false);
		labelShowDoubleLogPlot.setEnabled(false);
		checkBoxShowDoubleLogPlot.setEnabled(false);
		
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
	
	private void updateNumEps() {
		int valueNumEps      = (int)spinnerNumEps.getValue();
		int valueNumRegStart = (int)spinnerNumRegStart.getValue();
		int valueNumRegEnd   = (int)spinnerNumRegEnd.getValue();  
			
		int[] epsInterval = CsajUtil_GenerateInterval.getIntLogDistributedInterval(valueNumRegStart, valueNumRegEnd, valueNumEps);
		
		if (epsInterval.length < valueNumEps) {
			logService.info(this.getClass().getName() + " Note: Eps interval is limited to a sequence of unique values");
			valueNumEps = epsInterval.length;
			spinnerNumEps.setValue(valueNumEps);
			spinnerInteger_NumEps = (int)spinnerNumEps.getValue();
		}

		logService.info(this.getClass().getName() + " Regression # set to: " + spinnerInteger_NumEps);
		logService.info(this.getClass().getName() + " Eps #        set to: " + epsInterval.length);
		logService.info(this.getClass().getName() + " Eps interval set to: " + Arrays.toString(epsInterval));
        if (booleanProcessImmediately) btnProcessSingleColumn.doClick();
	}
		
	/**
	 * process by calling a command
	 */
	public void processCommand() {
		 
		Future<CommandModule> future = commandService.run(Csaj1DHurstCmd.class, false,
														"defaultTableDisplay",           defaultTableDisplay,  //is not automatically harvested in headless mode
														"processAll",                    processAll,
														
														"booleanHurstHK",				 booleanHurstHK,	
														"booleanHurstRS",				 booleanHurstRS,	
														"booleanHurstSP",				 booleanHurstSP,	
														"spinnerInteger_HKN",			 spinnerInteger_HKN,
														"spinnerInteger_SPMaxLag",       spinnerInteger_SPMaxLag,
														"choiceRadioButt_SurrBoxHurstType", choiceRadioButt_SurrBoxHurstType,
														
														"spinnerInteger_NumEps", 	     spinnerInteger_NumEps,
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
		tableOutName = Csaj1DHurstCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
