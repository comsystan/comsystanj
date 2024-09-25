/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DVolumeGeneratorDialog.java
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

package at.csa.csaj.plugin3d.misc;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.imagej.Dataset;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import at.csa.csaj.commons.CsajDialog_PluginFrame;


/*
 * This is a custom dialog for a CSAJ plugin
 */
public class Csaj3DVolumeGeneratorDialog extends CsajDialog_PluginFrame {

	private static final long serialVersionUID = -3947904355989303769L;

	@Parameter
	private LogService logService;
	
	@Parameter
	private CommandService commandService;	
	
	@Parameter
	private UIService uiService;
	
	@Parameter
	private ThreadService threadService;
	
	
  	private Dataset datasetOut;

	//Specific dialog items
  
  	private JLabel   labelWidth;
  	private JSpinner spinnerWidth;
	private int      spinnerInteger_Width;
	
	private JLabel   labelHeight;
	private JSpinner spinnerHeight;
	private int      spinnerInteger_Height;
	    
	private JLabel   labelDepth;
	private JSpinner spinnerDepth;
	private int      spinnerInteger_Depth;
	    
	private JPanel       panelColorModelType;
	private ButtonGroup  buttonGroupColorModelType;
    private JRadioButton radioButtonGrey8Bit;
    private JRadioButton radioButtonColorRGB;
	private String       choiceRadioButt_ColorModelType;
	    
	private JComboBox<String> comboBoxVolumeType;
	private String            choiceRadioButt_VolumeType;
	
	private JLabel   labelR;
	private JSpinner spinnerR;
	private int      spinnerInteger_R;
	
	private JLabel   labelG;
	private JSpinner spinnerG;
	private int      spinnerInteger_G;
	
	private JLabel   labelB;
	private JSpinner spinnerB;
	private int spinnerInteger_B;
	    
	private JLabel   labelFracDim;
	private JSpinner spinnerFracDim;
	private float    spinnerFloat_FracDim;
	
	private JLabel   labelOrderMandelbulb;
	private JSpinner spinnerOrderMandelbulb;
	private int      spinnerInteger_OrderMandelbulb;
	
	private JLabel   labelNumIterations;
	private JSpinner spinnerNumIterations;
	private int      spinnerInteger_NumIterations;

	public JButton btnGenerate;
		
	/**
	 * Create the dialog.
	 */
	public Csaj3DVolumeGeneratorDialog(Context context) {
			
		super();
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		context.inject(this); //Important but already injected in super class
			
		//Title of plugin
		//Overwrite
		setTitle("3D Image volume generator");

		//NORTH item
		//*****************************************************************************************
		JPanel panelInput = new JPanel();
		panelInput.setLayout(new GridBagLayout());
		panelInput.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
	    gbc.anchor = GridBagConstraints.CENTER;
		getContentPane().add(panelInput, BorderLayout.NORTH);
		
		JLabel labelInput = new JLabel("To be deleted");
		labelInput.setToolTipText("To be deleted");
		labelInput.setHorizontalAlignment(JLabel.RIGHT);
		labelInput.setToolTipText("To be deleted");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0; //IMPORTANT //This now sets the weight for the whole column 0 in the panelInput, although gpc is reset
		gbc.anchor = GridBagConstraints.WEST; //left
		//panelInput.add(labelInput, gbc); //NOT ADDED
		gbc.weightx = 0.0; //reset to default
		
		//Rest button--------------------------------------------------------
		JButton btnReset = new JButton("Reset");
		btnReset.setToolTipText("reset to default values");	
		btnReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				spinnerWidth.setValue(512);
				spinnerInteger_Width = (int)spinnerWidth.getValue();
				spinnerHeight.setValue(512);
				spinnerInteger_Height = (int)spinnerHeight.getValue();
				spinnerDepth.setValue(512);
				spinnerInteger_Depth = (int)spinnerDepth.getValue();
				radioButtonGrey8Bit.setSelected(true);
				choiceRadioButt_ColorModelType = radioButtonGrey8Bit.getText();
				comboBoxVolumeType.setSelectedItem("Random");
				choiceRadioButt_VolumeType = (String)comboBoxVolumeType.getSelectedItem();
				spinnerR.setValue(255);
				spinnerInteger_R = (int)spinnerR.getValue();
				spinnerG.setValue(255);
				spinnerInteger_G = (int)spinnerG.getValue();
				spinnerB.setValue(255);
				spinnerInteger_B = (int)spinnerB.getValue();
				spinnerFracDim.setValue(3.5);
				spinnerFloat_FracDim = (float)((SpinnerNumberModel)spinnerFracDim.getModel()).getNumber().doubleValue();
				spinnerOrderMandelbulb.setValue(8);
				spinnerInteger_OrderMandelbulb = (int)spinnerOrderMandelbulb.getValue();
				spinnerNumIterations.setValue(3);
				spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();			
			}
		});
		//gbc.insets = standardInsets;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST; //right
		panelInput.add(btnReset, gbc);
				
		//CENTER default items		
	    //*****************************************************************************************
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		contentPanel.setLayout(new GridBagLayout());
		//contentPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
	
		JScrollPane scrollPane = new JScrollPane(contentPanel);
	    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
			
	    //*****************************************************************************************
	    labelWidth = new JLabel("Width [pixel]");
	    labelWidth.setToolTipText("Width of output image stack in pixel");
	    labelWidth.setHorizontalAlignment(JLabel.RIGHT);
	    labelWidth.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelWidth = new SpinnerNumberModel(512, 1, 999999999, 1); // initial, min, max, step
        spinnerWidth = new JSpinner(spinnerModelWidth);
        spinnerWidth.setToolTipText("Width of output image stack in pixel");
        spinnerWidth.setEnabled(true);
        spinnerWidth.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Width = (int)spinnerWidth.getValue();
                logService.info(this.getClass().getName() + " Width set to " + spinnerInteger_Width);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });   
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelWidth, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerWidth, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Width = (int)spinnerWidth.getValue();
	    
	    //*****************************************************************************************
	    labelHeight = new JLabel("Height [pixel]");
	    labelHeight.setToolTipText("Height of output image stack");
	    labelHeight.setHorizontalAlignment(JLabel.RIGHT);
	    labelHeight.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelHeight = new SpinnerNumberModel(512, 1, 999999999, 1); // initial, min, max, step
        spinnerHeight = new JSpinner(spinnerModelHeight);
        spinnerHeight.setToolTipText("Height of output image stack");
        spinnerHeight.setEnabled(true);
        spinnerHeight.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Height = (int)spinnerHeight.getValue();
                logService.info(this.getClass().getName() + " Height set to " + spinnerInteger_Height);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        }); 
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHeight, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 2;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerHeight, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Height = (int)spinnerHeight.getValue();
	    
	    //*****************************************************************************************
	    labelDepth = new JLabel("Depth [voxel]");
	    labelDepth.setToolTipText("Depth of output image stack");
	    labelDepth.setHorizontalAlignment(JLabel.RIGHT);
	    labelDepth.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelDepth = new SpinnerNumberModel(512, 1, 999999999, 1); // initial, min, max, step
        spinnerDepth = new JSpinner(spinnerModelDepth);
        spinnerDepth.setToolTipText("Depth of output image stack");
        spinnerDepth.setEnabled(true);
        spinnerDepth.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_Depth = (int)spinnerDepth.getValue();
                logService.info(this.getClass().getName() + " Depth set to " + spinnerInteger_Depth);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        }); 
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelDepth, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerDepth, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_Depth = (int)spinnerDepth.getValue();
	    
	    //*****************************************************************************************
	    JLabel labelColorModelType = new JLabel("Color model");
	    labelColorModelType.setToolTipText("Color model of output image");
	    labelColorModelType.setHorizontalAlignment(JLabel.RIGHT);
		
		buttonGroupColorModelType = new ButtonGroup();
		radioButtonGrey8Bit = new JRadioButton("Grey-8bit");
		radioButtonColorRGB = new JRadioButton("Color-RGB");
		radioButtonGrey8Bit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonGrey8Bit.isSelected())  choiceRadioButt_ColorModelType = radioButtonGrey8Bit.getText();
				logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
				
				labelG.setEnabled(false);
				spinnerG.setEnabled(false);
				labelB.setEnabled(false);
				spinnerB.setEnabled(false);
				
				//if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		radioButtonColorRGB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (radioButtonGrey8Bit.isSelected()) {
					choiceRadioButt_ColorModelType = radioButtonGrey8Bit.getText();
					logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
					
					labelG.setEnabled(false);
					spinnerG.setEnabled(false);
					labelB.setEnabled(false);
					spinnerB.setEnabled(false);
				}			
				if (radioButtonColorRGB.isSelected()) {
					choiceRadioButt_ColorModelType = radioButtonColorRGB.getText();
					logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
					
					labelG.setEnabled(true);
					spinnerG.setEnabled(true);
					labelB.setEnabled(true);
					spinnerB.setEnabled(true);
				}		
				//if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
		buttonGroupColorModelType.add(radioButtonGrey8Bit);
		buttonGroupColorModelType.add(radioButtonColorRGB);
		radioButtonGrey8Bit.setSelected(true);
		
		panelColorModelType = new JPanel();
		panelColorModelType.setToolTipText("Color model of output image");
		panelColorModelType.setLayout(new BoxLayout(panelColorModelType, BoxLayout.Y_AXIS)); 
		
	    panelColorModelType.add(radioButtonGrey8Bit);
	    panelColorModelType.add(radioButtonColorRGB); 
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelColorModelType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 4;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(panelColorModelType, gbc);
	    //initialize command variable
		if (radioButtonGrey8Bit.isSelected())  choiceRadioButt_ColorModelType = radioButtonGrey8Bit.getText();
		if (radioButtonColorRGB.isSelected())  choiceRadioButt_ColorModelType = radioButtonColorRGB.getText();
	    
		//*****************************************************************************************
	    JLabel labelVolumeType = new JLabel("Volume type");
	    labelVolumeType.setToolTipText("Type of output image stack\bFFT..Fast Fourier transform\bMPD..Midpoint displacement");
	    labelVolumeType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Random", "Gaussian", "Constant", 
							"Fractal volume - FFT", "Fractal volume - MPD",
							"Fractal IFS - Menger", "Fractal IFS - Sierpinski1", "Fractal IFS - Sierpinski2", "Fractal IFS - Sierpinski3",
							"Fractal IFS - Mandelbulb", "Fractal IFS - Mandelbrot island"};
		comboBoxVolumeType = new JComboBox<String>(options);
		comboBoxVolumeType.setToolTipText("Type of output image stack\\bFFT..Fast Fourier transform\\bMPD..Midpoint displacement");
	    comboBoxVolumeType.setEditable(false);
	    comboBoxVolumeType.setSelectedItem("Random");
	    comboBoxVolumeType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_VolumeType = (String)comboBoxVolumeType.getSelectedItem();
				logService.info(this.getClass().getName() + " Volume type set to " + choiceRadioButt_VolumeType);
				
				//Reset all spinners and options
				labelFracDim.setEnabled(false);
				spinnerFracDim.setEnabled(false);
				labelOrderMandelbulb.setEnabled(false);
				spinnerOrderMandelbulb.setEnabled(false);
				labelNumIterations.setEnabled(false);
				spinnerNumIterations.setEnabled(false);
							
				if (   choiceRadioButt_VolumeType.equals("Fractal volume - FFT")
				    || choiceRadioButt_VolumeType.equals("Fractal volume - MPD") 
				    //|| choiceRadioButt_VolumeType.equals("Fractal surface - Sum of sine")//
				    ) {		
					labelFracDim.setEnabled(true);
					spinnerFracDim.setEnabled(true);
				}
				if (   choiceRadioButt_VolumeType.equals("Fractal IFS - Mandelbulb")
					) {		
					labelOrderMandelbulb.setEnabled(true);
					spinnerOrderMandelbulb.setEnabled(true);
				}
				if (   choiceRadioButt_VolumeType.equals("Fractal IFS - Menger")
					|| choiceRadioButt_VolumeType.equals("Fractal IFS - Sierpinski1") 
					|| choiceRadioButt_VolumeType.equals("Fractal IFS - Sierpinski2") 
					|| choiceRadioButt_VolumeType.equals("Fractal IFS - Sierpinski3") 
					|| choiceRadioButt_VolumeType.equals("Fractal IFS - Mandelbulb") 
					|| choiceRadioButt_VolumeType.equals("Fractal IFS - Mandelbrot island") 
					) {		
					labelNumIterations.setEnabled(true);
					spinnerNumIterations.setEnabled(true);
				}	
				//if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelVolumeType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxVolumeType, gbc);
	    //initialize command variable
	    choiceRadioButt_VolumeType = (String)comboBoxVolumeType.getSelectedItem();
		
	    //*****************************************************************************************
	    labelR = new JLabel("Grey or R");
	    labelR.setToolTipText("Grey value of Grey image or of the RGB R channel");
	    labelR.setHorizontalAlignment(JLabel.RIGHT);
	    labelR.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelR = new SpinnerNumberModel(255, 0, 255, 1); // initial, min, max, step
        spinnerR = new JSpinner(spinnerModelR);
        spinnerR.setToolTipText("Grey value of Grey image or of the RGB R channel");
        spinnerR.setEnabled(true);
        spinnerR.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_R = (int)spinnerR.getValue();
                logService.info(this.getClass().getName() + " Grey/R value set to " + spinnerInteger_R);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelR, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 6;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerR, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_R = (int)spinnerR.getValue();
	    
	    //*****************************************************************************************
	    labelG = new JLabel("G");
	    labelG.setToolTipText("Grey value of the RGB channel G");
	    labelG.setHorizontalAlignment(JLabel.RIGHT);
	    labelG.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelG = new SpinnerNumberModel(255, 0, 255, 1); // initial, min, max, step
        spinnerG = new JSpinner(spinnerModelG);
        spinnerG.setToolTipText("Grey value of the RGB channel G");
        spinnerG.setEnabled(false);
        spinnerG.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_G = (int)spinnerG.getValue();
                logService.info(this.getClass().getName() + " G value set to " + spinnerInteger_G);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 7;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelG, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 7;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerG, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_G = (int)spinnerG.getValue();
	    
	    //*****************************************************************************************
	    labelB = new JLabel("B");
	    labelB.setToolTipText("Grey value of the RGB channel B");
	    labelB.setHorizontalAlignment(JLabel.RIGHT);
	    labelB.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelB = new SpinnerNumberModel(255, 0, 255, 1); // initial, min, max, step
        spinnerB = new JSpinner(spinnerModelB);
        spinnerB.setToolTipText("Grey value of the RGB channel B");
        spinnerB.setEnabled(false);
        spinnerB.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_B = (int)spinnerB.getValue();
                logService.info(this.getClass().getName() + " B value set to " + spinnerInteger_B);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 8;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelB, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 8;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerB, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_B = (int)spinnerB.getValue();
	    
	    //*****************************************************************************************
	    labelFracDim = new JLabel("Fractal dimension");
	    labelFracDim.setToolTipText("Fractal dimension of fractal volume in the range [3,4]");
	    labelFracDim.setHorizontalAlignment(JLabel.RIGHT);
	    labelFracDim.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelFracDim = new SpinnerNumberModel(3.5, 3.0, 4.0, 0.1); // initial, min, max, step
        spinnerFracDim = new JSpinner(spinnerModelFracDim);
        spinnerFracDim.setToolTipText("Fractal dimension of fractal volume in the range [3,4]");
        spinnerFracDim.setEnabled(false);
        spinnerFracDim.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_FracDim = (float)((SpinnerNumberModel)spinnerFracDim.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Fractal dimension set to " + spinnerFloat_FracDim);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 9;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelFracDim, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 9;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerFracDim, gbc);	    
	    //initialize command variable
	    spinnerFloat_FracDim = (float)((SpinnerNumberModel)spinnerFracDim.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelOrderMandelbulb = new JLabel("Order");
	    labelOrderMandelbulb.setToolTipText("Order of spherical transformation for Mandelbulb, default = 8");
	    labelOrderMandelbulb.setHorizontalAlignment(JLabel.RIGHT);
	    labelOrderMandelbulb.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelOrderMandelbulb = new SpinnerNumberModel(8, 1, 999999999, 1); // initial, min, max, step
        spinnerOrderMandelbulb = new JSpinner(spinnerModelOrderMandelbulb);
        spinnerOrderMandelbulb.setToolTipText("Order of spherical transformation for Mandelbulb, default = 8");
        spinnerOrderMandelbulb.setEnabled(false);
        spinnerOrderMandelbulb.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_OrderMandelbulb = (int)spinnerOrderMandelbulb.getValue();
                logService.info(this.getClass().getName() + " Order set to " + spinnerInteger_OrderMandelbulb);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelOrderMandelbulb, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerOrderMandelbulb, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_OrderMandelbulb = (int)spinnerOrderMandelbulb.getValue();
	    
	    //*****************************************************************************************
	    labelNumIterations = new JLabel("Number of iterations");
	    labelNumIterations.setToolTipText("Number of iterations for IFS algorithms");
	    labelNumIterations.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumIterations.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelNumIterations = new SpinnerNumberModel(3, 1, 999999999, 1); // initial, min, max, step
        spinnerNumIterations = new JSpinner(spinnerModelNumIterations);
        spinnerNumIterations.setToolTipText("Number of iterations for IFS algorithms");
        spinnerNumIterations.setEnabled(false);
        spinnerNumIterations.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();
                logService.info(this.getClass().getName() + " Number of iterations set to " + spinnerInteger_NumIterations);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });     
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 11;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumIterations, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 11;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumIterations, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();
	    
	    //SOUTH Process buttons panel 
	    //*****************************************************************************************
	    JPanel buttonPanelGenerate = new JPanel();
		buttonPanelGenerate.setLayout(new GridBagLayout());
		buttonPanelGenerate.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
		getContentPane().add(buttonPanelGenerate, BorderLayout.SOUTH);
	    
		//Process button--------------------------------------------------------
		btnGenerate = new JButton("Generate volume");
		btnGenerate.setToolTipText("Generate volume");
		btnGenerate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				threadService.run(() -> processCommand());
			}
		});
		//gbc.insets = standardInsets;
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    buttonPanelGenerate.add(btnGenerate, gbc);	    
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
		//Following run initiates a "ProcessAllImages" 
		Future<CommandModule> future = commandService.run(Csaj3DVolumeGeneratorCommand.class, false,
														"spinnerInteger_Width",                spinnerInteger_Width,    
														"spinnerInteger_Height",               spinnerInteger_Height, 
														"spinnerInteger_Depth",                spinnerInteger_Depth,     
														"choiceRadioButt_ColorModelType",      choiceRadioButt_ColorModelType,
														"choiceRadioButt_VolumeType",          choiceRadioButt_VolumeType,    
														"spinnerInteger_R",                    spinnerInteger_R,
														"spinnerInteger_G",                    spinnerInteger_G,
														"spinnerInteger_B",                    spinnerInteger_B,
														"spinnerFloat_FracDim",                spinnerFloat_FracDim,
														"spinnerInteger_OrderMandelbulb",      spinnerInteger_OrderMandelbulb,
														"spinnerInteger_NumIterations",        spinnerInteger_NumIterations
																								
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
