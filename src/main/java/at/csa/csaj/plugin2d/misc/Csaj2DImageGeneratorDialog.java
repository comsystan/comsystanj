/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DImageGeneratorDialog.java
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

package at.csa.csaj.plugin2d.misc;

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
public class Csaj2DImageGeneratorDialog extends CsajDialog_PluginFrame {

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
	    
	private JLabel   labelNumImages;
	private JSpinner spinnerNumImages;
	private int      spinnerInteger_NumImages;
	    
	private JPanel       panelColorModelType;
	private ButtonGroup  buttonGroupColorModelType;
    private JRadioButton radioButtonGrey8Bit;
    private JRadioButton radioButtonColorRGB;
	private String       choiceRadioButt_ColorModelType;
	    
	private JComboBox<String> comboBoxImageType;
	private String            choiceRadioButt_ImageType;
	
	private JLabel   labelR;
	private JSpinner spinnerR;
	private int      spinnerInteger_R;
	
	private JLabel   labelG;
	private JSpinner spinnerG;
	private int      spinnerInteger_G;
	
	private JLabel   labelB;
	private JSpinner spinnerB;
	private int      spinnerInteger_B;
	    
	private JLabel   labelFracDim;
	private JSpinner spinnerFracDim;
	private float    spinnerFloat_FracDim;
	    
	private JLabel   labelSineSumOfSineFrequency;
	private JSpinner spinnerSineSumOfSineFrequency;
	private float    spinnerFloat_SineSumOfSineFrequency;
	
	private JLabel   labelSumOfSineAmplitude;
	private JSpinner spinnerSumOfSineAmplitude;
	private float    spinnerFloat_SumOfSineAmplitude;

	private JLabel   labelNumIterations;
	private JSpinner spinnerNumIterations;
	private int      spinnerInteger_NumIterations;
	  
	private JLabel   labelShapeSize;
	private JSpinner spinnerShapeSize;
	private int      spinnerInteger_ShapeSize;
	    
	private JLabel   labelShapeScaling;
	private JSpinner spinnerShapeScaling;
	private float    spinnerFloat_ShapeScaling;
	
	private JLabel   labelNumPolygons;
	private JSpinner spinnerNumPolygons;
	private int      spinnerInteger_NumPolygons;
	    
	private JLabel   labelHRMProbability1;
	private JSpinner spinnerHRMProbability1;
	private float    spinnerFloat_HRMProbability1;
	
	private JLabel   labelHRMProbability2;
	private JSpinner spinnerHRMProbability2;
	private float    spinnerFloat_HRMProbability2;
	
	private JLabel   labelHRMProbability3;
	private JSpinner spinnerHRMProbability3;
	private float    spinnerFloat_HRMProbability3;
	
	public JButton btnGenerate;
		
	/**
	 * Create the dialog.
	 */
	public Csaj2DImageGeneratorDialog(Context context) {
			
		super();
			
		//This dialog has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		context.inject(this); //Important but already injected in super class
			
		//Title of plugin
		//Overwrite
		setTitle("2D Image generator");

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
				spinnerNumImages.setValue(1);
				spinnerInteger_NumImages = (int)spinnerNumImages.getValue();
				radioButtonGrey8Bit.setSelected(true);
				choiceRadioButt_ColorModelType = radioButtonGrey8Bit.getText();
				comboBoxImageType.setSelectedItem("Random");
				choiceRadioButt_ImageType = (String)comboBoxImageType.getSelectedItem();
				spinnerR.setValue(255);
				spinnerInteger_R = (int)spinnerR.getValue();
				spinnerG.setValue(255);
				spinnerInteger_G = (int)spinnerG.getValue();
				spinnerB.setValue(255);
				spinnerInteger_B = (int)spinnerB.getValue();
				spinnerFracDim.setValue(2.5);
				spinnerFloat_FracDim = (float)((SpinnerNumberModel)spinnerFracDim.getModel()).getNumber().doubleValue();
				spinnerSineSumOfSineFrequency.setValue(2.0);
				spinnerFloat_SineSumOfSineFrequency = (float)((SpinnerNumberModel)spinnerSineSumOfSineFrequency.getModel()).getNumber().doubleValue();
				spinnerSumOfSineAmplitude.setValue(2.0);
				spinnerFloat_SumOfSineAmplitude = (float)((SpinnerNumberModel)spinnerSumOfSineAmplitude.getModel()).getNumber().doubleValue();
				spinnerNumIterations.setValue(3);
				spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();
				spinnerShapeSize.setValue(10);
				spinnerInteger_ShapeSize = (int)spinnerShapeSize.getValue();
				spinnerShapeScaling.setValue(0.1);
				spinnerFloat_ShapeScaling = (float)((SpinnerNumberModel)spinnerShapeScaling.getModel()).getNumber().doubleValue();
				spinnerNumPolygons.setValue(3);
				spinnerInteger_NumPolygons = (int)spinnerNumPolygons.getValue();
				spinnerHRMProbability1.setValue(0.5);
				spinnerFloat_HRMProbability1 = (float)((SpinnerNumberModel)spinnerHRMProbability1.getModel()).getNumber().doubleValue();
				spinnerHRMProbability2.setValue(0.5);
				spinnerFloat_HRMProbability2 = (float)((SpinnerNumberModel)spinnerHRMProbability2.getModel()).getNumber().doubleValue();
				spinnerHRMProbability3.setValue(0.5);
				spinnerFloat_HRMProbability3 = (float)((SpinnerNumberModel)spinnerHRMProbability3.getModel()).getNumber().doubleValue();
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
	    labelWidth.setToolTipText("Width of output image in pixel");
	    labelWidth.setHorizontalAlignment(JLabel.RIGHT);
	    labelWidth.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelWidth = new SpinnerNumberModel(512, 1, 999999999, 1); // initial, min, max, step
        spinnerWidth = new JSpinner(spinnerModelWidth);
        spinnerWidth.setToolTipText("Width of output image in pixel");
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
	    labelHeight.setToolTipText("Height of output image in pixel");
	    labelHeight.setHorizontalAlignment(JLabel.RIGHT);
	    labelHeight.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelHeight = new SpinnerNumberModel(512, 1, 999999999, 1); // initial, min, max, step
        spinnerHeight = new JSpinner(spinnerModelHeight);
        spinnerHeight.setToolTipText("Height of output image in pixel");
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
	    labelNumImages = new JLabel("Number of images");
	    labelNumImages.setToolTipText("Number of output images");
	    labelNumImages.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumImages.setEnabled(true);
	    
	    SpinnerNumberModel spinnerModelNumImages = new SpinnerNumberModel(1, 1, 999999999, 1); // initial, min, max, step
        spinnerNumImages = new JSpinner(spinnerModelNumImages);
        spinnerNumImages.setToolTipText("Number of output images");
        spinnerNumImages.setEnabled(true);
        spinnerNumImages.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumImages = (int)spinnerNumImages.getValue();
                logService.info(this.getClass().getName() + " Number of output images set to " + spinnerInteger_NumImages);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumImages, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 3;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumImages, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumImages = (int)spinnerNumImages.getValue();
	    
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
	    JLabel labelImageType = new JLabel("Image type");
	    labelImageType.setToolTipText("Type of output image\bFFT..Fast Fourier transform\bMPD..Midpoint displacement\bHRM..Hirarchical random maps\bIFS..Iterated function system");
	    labelImageType.setHorizontalAlignment(JLabel.RIGHT);
		
		String options[] = {"Random", "Gaussian", "Sine - radial", "Sine - horizontal", "Sine - vertical",  "Constant", 
				   "Fractal surface - FFT", "Fractal surface - MPD", "Fractal surface - Sum of sine", "Fractal - HRM",
				   "Fractal random shapes - Lines", "Fractal random shapes - Circles", "Fractal random shapes - Squares", "Fractal random shapes - Filled circles", "Fractal random shapes - Filled squares",
				   "Fractal IFS - Menger", "Fractal IFS - Sierpinski-1", "Fractal IFS - Sierpinski-2",
				   "Fractal IFS - Mandelbrot set", "Fractal IFS - Mandelbrot island-1", "Fractal IFS - Mandelbrot island-2",
				   "Fractal IFS - Mandelbrot island&lake-1", "Fractal IFS - Mandelbrot island&lake-2", 
				   "Fractal IFS - Koch snowflake",  "Fractal IFS - Fern", "Fractal IFS - Heighway dragon",
				   "Fractal - Hofstaedter butterfly"
				   };
		comboBoxImageType = new JComboBox<String>(options);
		comboBoxImageType.setToolTipText("Type of output image\bFFT..Fast Fourier transform\bMPD..Midpoint displacement\bHRM..Hirarchical random maps\bIFS..Iterated function system");
	    comboBoxImageType.setEditable(false);
	    comboBoxImageType.setSelectedItem("Random");
	    comboBoxImageType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				choiceRadioButt_ImageType = (String)comboBoxImageType.getSelectedItem();
				logService.info(this.getClass().getName() + " Image type set to " + choiceRadioButt_ImageType);
				
				//Reset all spinners and options
				labelFracDim.setEnabled(false);
				spinnerFracDim.setEnabled(false);
				labelSineSumOfSineFrequency.setEnabled(false);
				spinnerSineSumOfSineFrequency.setEnabled(false);
				labelSumOfSineAmplitude.setEnabled(false);
				spinnerSumOfSineAmplitude.setEnabled(false);
				labelNumIterations.setEnabled(false);
				spinnerNumIterations.setEnabled(false);
				labelShapeSize.setEnabled(false);
				spinnerShapeSize.setEnabled(false);
				labelShapeScaling.setEnabled(false);
				spinnerShapeScaling.setEnabled(false);
				labelNumPolygons.setEnabled(false);
				spinnerNumPolygons.setEnabled(false); 
				labelHRMProbability1.setEnabled(false);
				spinnerHRMProbability1.setEnabled(false);
				labelHRMProbability2.setEnabled(false);
				spinnerHRMProbability2.setEnabled(false);
				labelHRMProbability3.setEnabled(false);
				spinnerHRMProbability3.setEnabled(false);
							
				if (   choiceRadioButt_ImageType.equals("Fractal surface - FFT")
				    || choiceRadioButt_ImageType.equals("Fractal surface - MPD") 
				    //|| choiceRadioButt_ImageType.equals("Fractal surface - Sum of sine")//
				    ) {		
					labelFracDim.setEnabled(true);
					spinnerFracDim.setEnabled(true);
				}
				if (   choiceRadioButt_ImageType.equals("Sine - radial")
					|| choiceRadioButt_ImageType.equals("Sine - horizontal") 
					|| choiceRadioButt_ImageType.equals("Sine - vertical")
					|| choiceRadioButt_ImageType.equals("Fractal surface - Sum of sine")
					) {		
					labelSineSumOfSineFrequency.setEnabled(true);
					spinnerSineSumOfSineFrequency.setEnabled(true);
				}
				if (   choiceRadioButt_ImageType.equals("Fractal surface - Sum of sine") ) {		
					labelSumOfSineAmplitude.setEnabled(true);
					spinnerSumOfSineAmplitude.setEnabled(true);
				}
				if (   choiceRadioButt_ImageType.equals("Fractal surface - Sum of sine")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Lines") 
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Circles")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Squares")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Filled circles")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Filled squares")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Menger")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Sierpinski-1")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Sierpinski-2")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Mandelbrot set")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Mandelbrot island-1")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Mandelbrot island-2")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Mandelbrot island&lake-1")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Mandelbrot island&lake-2")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Koch snowflake")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Fern")
					|| choiceRadioButt_ImageType.equals("Fractal IFS - Heighway dragon")
					) {		
					labelNumIterations.setEnabled(true);
					spinnerNumIterations.setEnabled(true);
				}
				if (   choiceRadioButt_ImageType.equals("Fractal random shapes - Lines") 
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Circles")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Squares")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Filled circles")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Filled squares")
					) {		
					labelShapeSize.setEnabled(true);
					spinnerShapeSize.setEnabled(true);
				}
				if (   choiceRadioButt_ImageType.equals("Fractal random shapes - Lines") 
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Circles")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Squares")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Filled circles")
					|| choiceRadioButt_ImageType.equals("Fractal random shapes - Filled squares")
					) {		
					labelShapeScaling.setEnabled(true);
					spinnerShapeScaling.setEnabled(true );
				}
				if (   choiceRadioButt_ImageType.equals("Fractal IFS - Koch snowflake") ) {		
					labelNumPolygons.setEnabled(true);
					spinnerNumPolygons.setEnabled(true); 
				}
				
				if (   choiceRadioButt_ImageType.equals("Fractal - HRM") ) {		
					labelHRMProbability1.setEnabled(true);
					spinnerHRMProbability1.setEnabled(true);
					labelHRMProbability2.setEnabled(true);
					spinnerHRMProbability2.setEnabled(true);
					labelHRMProbability3.setEnabled(true);
					spinnerHRMProbability3.setEnabled(true);
				}
				
				if (   choiceRadioButt_ImageType.equals("Fractal - Hofstaedter butterfly") ) {		

				}
				
				//if (booleanProcessImmediately) btnProcessSingleImage.doClick();
			}
		});
	    
	    gbc.insets = INSETS_STANDARD;
	    gbc.gridx = 0;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelImageType, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 5;
	    gbc.anchor = GridBagConstraints.WEST; //left 
	    contentPanel.add(comboBoxImageType, gbc);
	    //initialize command variable
	    choiceRadioButt_ImageType = (String)comboBoxImageType.getSelectedItem();
		
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
	    labelFracDim.setToolTipText("Fractal dimension of fractal surface in the range [2,3]");
	    labelFracDim.setHorizontalAlignment(JLabel.RIGHT);
	    labelFracDim.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelFracDim = new SpinnerNumberModel(2.5, 2.0, 3.0, 0.1); // initial, min, max, step
        spinnerFracDim = new JSpinner(spinnerModelFracDim);
        spinnerFracDim.setToolTipText("Fractal dimension of fractal surface in the range [2,3]");
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
	    labelSineSumOfSineFrequency = new JLabel("Frequency");
	    labelSineSumOfSineFrequency.setToolTipText("Frequency for Sine or Sum of sine method");
	    labelSineSumOfSineFrequency.setHorizontalAlignment(JLabel.RIGHT);
		labelSineSumOfSineFrequency.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelSineSumOfSineFrequency = new SpinnerNumberModel(2.0, 0.0, 999999999999999.0, 1); // initial, min, max, step
        spinnerSineSumOfSineFrequency = new JSpinner(spinnerModelSineSumOfSineFrequency);
        spinnerSineSumOfSineFrequency.setToolTipText("Frequency for Sine or Sum of sine method");
		spinnerSineSumOfSineFrequency.setEnabled(false);
        spinnerSineSumOfSineFrequency.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_SineSumOfSineFrequency = (float)((SpinnerNumberModel)spinnerSineSumOfSineFrequency.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Frequency set to " + spinnerFloat_SineSumOfSineFrequency);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSineSumOfSineFrequency, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 10;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSineSumOfSineFrequency, gbc);	    
	    //initialize command variable
	    spinnerFloat_SineSumOfSineFrequency = (float)((SpinnerNumberModel)spinnerSineSumOfSineFrequency.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelSumOfSineAmplitude = new JLabel("Amplitude");
	    labelSumOfSineAmplitude.setToolTipText("Amplitude for Sum of sine method");
	    labelSumOfSineAmplitude.setHorizontalAlignment(JLabel.RIGHT);
		labelSumOfSineAmplitude.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelSumOfSineAmplitude = new SpinnerNumberModel(2.0, 0.0, 999999999999999.0, 1); // initial, min, max, step
        spinnerSumOfSineAmplitude = new JSpinner(spinnerModelSumOfSineAmplitude);
        spinnerSumOfSineAmplitude.setToolTipText("Amplitude for Sum of sine method");
		spinnerSumOfSineAmplitude.setEnabled(false);
        spinnerSumOfSineAmplitude.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_SumOfSineAmplitude = (float)((SpinnerNumberModel)spinnerSumOfSineAmplitude.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Min eta set to " + spinnerFloat_SumOfSineAmplitude);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 11;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelSumOfSineAmplitude, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 11;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerSumOfSineAmplitude, gbc);	    
	    //initialize command variable
	    spinnerFloat_SumOfSineAmplitude = (float)((SpinnerNumberModel)spinnerSumOfSineAmplitude.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelNumIterations = new JLabel("Number of iterations");
	    labelNumIterations.setToolTipText("Number of iterations or Sum of sine, Random shapes and IFS algorithms");
	    labelNumIterations.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumIterations.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelNumIterations = new SpinnerNumberModel(3, 1, 999999999, 1); // initial, min, max, step
        spinnerNumIterations = new JSpinner(spinnerModelNumIterations);
        spinnerNumIterations.setToolTipText("Number of iterations or Sum of sine, Random shapes and IFS algorithms");
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
	    gbc.gridy = 12;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumIterations, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 12;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumIterations, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumIterations = (int)spinnerNumIterations.getValue();
	    
	    //*****************************************************************************************
	    labelShapeSize = new JLabel("Shape size [pixel]");
	    labelShapeSize.setToolTipText("Maximal Size/radius/thickness of shapes in pixels");
	    labelShapeSize.setHorizontalAlignment(JLabel.RIGHT);
	    labelShapeSize.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelShapeSize = new SpinnerNumberModel(10, 1, 999999999, 1); // initial, min, max, step
        spinnerShapeSize = new JSpinner(spinnerModelShapeSize);
        spinnerShapeSize.setToolTipText("Maximal Size/radius/thickness of shapes in pixels");
        spinnerShapeSize.setEnabled(false);
        spinnerShapeSize.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_ShapeSize = (int)spinnerShapeSize.getValue();
                logService.info(this.getClass().getName() + " Shape size set to " + spinnerInteger_ShapeSize);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });       
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 13;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelShapeSize, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 13;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerShapeSize, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_ShapeSize = (int)spinnerShapeSize.getValue();
	    
	    //*****************************************************************************************
	    labelShapeScaling = new JLabel("Shape scaling");
	    labelShapeScaling.setToolTipText("Exponential scaling of Size/radius/thickness distribution [0, 1]"); //0..without scaling, same thickness   1..maximal scaling
	    labelShapeScaling.setHorizontalAlignment(JLabel.RIGHT);
		labelShapeScaling.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelShapeScaling = new SpinnerNumberModel(0.1, 0.0, 1.0, 0.1); // initial, min, max, step
        spinnerShapeScaling = new JSpinner(spinnerModelShapeScaling);
        spinnerShapeScaling.setToolTipText("Exponential scaling of Size/radius/thickness distribution [0, 1]"); //0..without scaling, same thickness   1..maximal scaling
		spinnerShapeScaling.setEnabled(false);
        spinnerShapeScaling.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_ShapeScaling = (float)((SpinnerNumberModel)spinnerShapeScaling.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Shape scaling set to " + spinnerFloat_ShapeScaling);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 14;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelShapeScaling, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 14;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerShapeScaling, gbc);	    
	    //initialize command variable
	    spinnerFloat_ShapeScaling = (float)((SpinnerNumberModel)spinnerShapeScaling.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelNumPolygons = new JLabel("Number of polygons");
	    labelNumPolygons.setToolTipText("Starting number of polygons");
	    labelNumPolygons.setHorizontalAlignment(JLabel.RIGHT);
	    labelNumPolygons.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelNumPolygons = new SpinnerNumberModel(3, 1, 999999999, 1); // initial, min, max, step
        spinnerNumPolygons = new JSpinner(spinnerModelNumPolygons);
        spinnerNumPolygons.setToolTipText("Starting number of polygons");
        spinnerNumPolygons.setEnabled(false);
        spinnerNumPolygons.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerInteger_NumPolygons = (int)spinnerNumPolygons.getValue();
                logService.info(this.getClass().getName() + " Number of polygons set to " + spinnerInteger_NumPolygons);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        }); 
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 15;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelNumPolygons, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 15;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerNumPolygons, gbc);	    
	    
	    //initialize command variable
	    spinnerInteger_NumPolygons = (int)spinnerNumPolygons.getValue();
	    
	    //*****************************************************************************************
	    labelHRMProbability1 = new JLabel("Probability 1");
	    labelHRMProbability1.setToolTipText("Probability of first level");
	    labelHRMProbability1.setHorizontalAlignment(JLabel.RIGHT);
		labelHRMProbability1.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelHRMProbability1 = new SpinnerNumberModel(0.5, 0.0, 1.0, 0.01); // initial, min, max, step
        spinnerHRMProbability1 = new JSpinner(spinnerModelHRMProbability1);
        spinnerHRMProbability1.setToolTipText("Probability of first level");
		spinnerHRMProbability1.setEnabled(false);
        spinnerHRMProbability1.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_HRMProbability1 = (float)((SpinnerNumberModel)spinnerHRMProbability1.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Probability 1 set to " + spinnerFloat_HRMProbability1);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 16;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHRMProbability1, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 16;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerHRMProbability1, gbc);	    
	    //initialize command variable
	    spinnerFloat_HRMProbability1 = (float)((SpinnerNumberModel)spinnerHRMProbability1.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelHRMProbability2 = new JLabel("Probability 2");
	    labelHRMProbability2.setToolTipText("Probability of second level");
	    labelHRMProbability2.setHorizontalAlignment(JLabel.RIGHT);
		labelHRMProbability2.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelHRMProbability2 = new SpinnerNumberModel(0.5, 0.0, 1.0, 0.01); // initial, min, max, step
        spinnerHRMProbability2 = new JSpinner(spinnerModelHRMProbability2);
        spinnerHRMProbability2.setToolTipText("Probability of second level");
		spinnerHRMProbability2.setEnabled(false);
        spinnerHRMProbability2.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_HRMProbability2 = (float)((SpinnerNumberModel)spinnerHRMProbability2.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Probability 2 set to " + spinnerFloat_HRMProbability2);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 17;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHRMProbability2, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 17;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerHRMProbability2, gbc);	    
	    //initialize command variable
	    spinnerFloat_HRMProbability2 = (float)((SpinnerNumberModel)spinnerHRMProbability2.getModel()).getNumber().doubleValue();
	    
	    //*****************************************************************************************
	    labelHRMProbability3 = new JLabel("Probability 3");
	    labelHRMProbability3.setToolTipText("Probability of third level");
	    labelHRMProbability3.setHorizontalAlignment(JLabel.RIGHT);
		labelHRMProbability3.setEnabled(false);
	    
	    SpinnerNumberModel spinnerModelHRMProbability3 = new SpinnerNumberModel(0.5, 0.0, 1.0, 0.01); // initial, min, max, step
        spinnerHRMProbability3 = new JSpinner(spinnerModelHRMProbability3);
        spinnerHRMProbability3.setToolTipText("Probability of third level");
		spinnerHRMProbability3.setEnabled(false);
        spinnerHRMProbability3.addChangeListener(new ChangeListener() {
        	@Override
            public void stateChanged(ChangeEvent e) {
            	spinnerFloat_HRMProbability3 = (float)((SpinnerNumberModel)spinnerHRMProbability3.getModel()).getNumber().doubleValue();
                logService.info(this.getClass().getName() + " Probability 3 set to " + spinnerFloat_HRMProbability3);
                //if (booleanProcessImmediately) btnProcessSingleImage.doClick();
            }
        });
        gbc.insets = INSETS_STANDARD;
        gbc.gridx = 0;
	    gbc.gridy = 18;
	    gbc.anchor = GridBagConstraints.EAST; //right
	    contentPanel.add(labelHRMProbability3, gbc);
	    gbc.gridx = 1;
	    gbc.gridy = 18;
	    gbc.anchor = GridBagConstraints.WEST; //left
	    contentPanel.add(spinnerHRMProbability3, gbc);	    
	    //initialize command variable
	    spinnerFloat_HRMProbability3 = (float)((SpinnerNumberModel)spinnerHRMProbability3.getModel()).getNumber().doubleValue();
	    
	    
	    //SOUTH Process buttons panel 
	    //*****************************************************************************************
	    JPanel buttonPanelGenerate = new JPanel();
		buttonPanelGenerate.setLayout(new GridBagLayout());
		buttonPanelGenerate.setBorder(new EmptyBorder(5, 5, 5, 5)); 
		gbc.insets = INSETS_STANDARD;
		getContentPane().add(buttonPanelGenerate, BorderLayout.SOUTH);
	    
		//Process button--------------------------------------------------------
		btnGenerate = new JButton("Generate image(s)");
		btnGenerate.setToolTipText("Generate image(s)");
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
		 
		Future<CommandModule> future = commandService.run(Csaj2DImageGeneratorCmd.class, false,
														"spinnerInteger_Width",                spinnerInteger_Width,    
														"spinnerInteger_Height",               spinnerInteger_Height, 
														"spinnerInteger_NumImages",            spinnerInteger_NumImages,     
														"choiceRadioButt_ColorModelType",      choiceRadioButt_ColorModelType,
														"choiceRadioButt_ImageType",            choiceRadioButt_ImageType,    
														"spinnerInteger_R",                    spinnerInteger_R,
														"spinnerInteger_G",                    spinnerInteger_G,
														"spinnerInteger_B",                    spinnerInteger_B,
														"spinnerFloat_FracDim",                spinnerFloat_FracDim,    
														"spinnerFloat_SineSumOfSineFrequency", spinnerFloat_SineSumOfSineFrequency, 
														"spinnerFloat_SumOfSineAmplitude",     spinnerFloat_SumOfSineAmplitude,
														"spinnerInteger_NumIterations",        spinnerInteger_NumIterations,
														"spinnerInteger_ShapeSize",            spinnerInteger_ShapeSize,
														"spinnerFloat_ShapeScaling",           spinnerFloat_ShapeScaling,
														"spinnerInteger_NumPolygons",          spinnerInteger_NumPolygons,
														"spinnerFloat_HRMProbability1",        spinnerFloat_HRMProbability1,
														"spinnerFloat_HRMProbability2",        spinnerFloat_HRMProbability2,
														"spinnerFloat_HRMProbability3",        spinnerFloat_HRMProbability3
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
