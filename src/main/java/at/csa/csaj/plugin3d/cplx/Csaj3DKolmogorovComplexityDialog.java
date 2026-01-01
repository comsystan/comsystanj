/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DKolmogorovComplexityDialog.java
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

package at.csa.csaj.plugin3d.cplx;

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
import at.csa.csaj.commons.CsajDialog_3DPlugin;

/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj3DKolmogorovComplexityDialog extends CsajDialog_3DPlugin {

	private static final long serialVersionUID = -843590691567020006L;

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
  	private JComboBox<String> comboBoxCompression;
	private String   choiceRadioButt_Compression;
	
	private JSpinner spinnerNumIterations;
	private int      spinnerInteger_NumIterations;
	
	private JCheckBox checkBoxSkipZeroes;
	private boolean   booleanSkipZeroes;

	
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
	public Csaj3DKolmogorovComplexityDialog(Context context, Dataset datasetIn) {
			
		super(context, datasetIn);
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important but already injected in super class
		this.datasetIn = datasetIn;
			
		//Title of plugin
		//Overwrite
		setTitle("3D KC and LD");

		//Add specific GUI elements according to Command @Parameter GUI elements
	    //*****************************************************************************************		
	    JLabel labelCompression = new JLabel("Compression");
	    labelCompression.setToolTipText("Type of image compression for estimating KC");
	    labelCompression.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"ZIP (lossless)", "ZLIB (lossless)", "GZIP (lossless)", "TIFF-LZW (lossless)"};
		comboBoxCompression = new JComboBox<String>(options);
		comboBoxCompression.setToolTipText("Type of image compression for estimating KC");
	    comboBoxCompression.setEditable(false);
	    comboBoxCompression.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_Compression = (String)comboBoxCompression.getSelectedItem();
				logService.info(this.getClass().getName() + " Compression method set to " + choiceRadioButt_Compression);
				if (comboBoxCompression.getSelectedItem().equals("TIFF-LZW (lossless)") ||
					comboBoxCompression.getSelectedItem().equals("PNG (lossless)")      ||	
					comboBoxCompression.getSelectedItem().equals("J2K (lossless)")      ||
					comboBoxCompression.getSelectedItem().equals("JPG (lossy)")) {
					checkBoxSkipZeroes.setSelected(false);
					booleanSkipZeroes = false;	
					logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
				}
				if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
			}
		});
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelCompression, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxCompression, gbc);
	    //initialize command variable
	    choiceRadioButt_Compression = (String)comboBoxCompression.getSelectedItem();
	    
	    //*****************************************************************************************
	    JLabel labelIteration = new JLabel("Iterations for LD");
	    labelIteration.setToolTipText("Number of compressions to compute averages");
	    labelIteration.setHorizontalAlignment(JLabel.RIGHT);
	
	    SpinnerNumberModel spinnerModelIteration = new SpinnerNumberModel(10, 1, 999999999, 1); // initial, min, max, step
        spinnerNumIterations = new JSpinner(spinnerModelIteration);
        spinnerNumIterations.setToolTipText("Number of compressions to compute averages");
        spinnerNumIterations.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();
                logService.info(this.getClass().getName() + " Number of iterations set to " + spinnerInteger_NumIterations);
                if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelIteration, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumIterations, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();
	   
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
		    	if (comboBoxCompression.getSelectedItem().equals("TIFF-LZW (lossless)") ||
					comboBoxCompression.getSelectedItem().equals("PNG (lossless)")      ||	
					comboBoxCompression.getSelectedItem().equals("J2K (lossless)")      ||
					comboBoxCompression.getSelectedItem().equals("JPG (lossy)")) {
		    		checkBoxSkipZeroes.setSelected(false);
					booleanSkipZeroes = false;
				}	
		    	logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
		    	if (booleanProcessImmediately) btnProcessSingleVolume.doClick();
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
		 
		Future<CommandModule> future = commandService.run(Csaj3DKolmogorovComplexityCmd.class, false,
														"datasetIn",                    datasetIn,  //is not automatically harvested in headless mode
			
														"choiceRadioButt_Compression",  choiceRadioButt_Compression,
														"spinnerInteger_NumIterations", spinnerInteger_NumIterations,
														"booleanSkipZeroes",            booleanSkipZeroes,
														
														"booleanOverwriteDisplays",     booleanOverwriteDisplays,
														"booleanProcessImmediately",	booleanProcessImmediately
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
		tableOutName = Csaj3DKolmogorovComplexityCmd.TABLE_OUT_NAME;
		tableOut     = (DefaultGenericTable)commandModule.getOutput("tableOut");	
		uiService.show(tableOutName, tableOut);
	}
}
