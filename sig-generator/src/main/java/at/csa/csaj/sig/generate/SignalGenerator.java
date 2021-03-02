/*-
 * #%L
 * Project: ImageJ plugin to generate signals.
 * File: SignalGenerator.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 Comsystan Software
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
package at.csa.csaj.sig.generate;


import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.math3.util.Precision;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.plot.PlotDisplayFrame;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.TimeZone;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
/**
 * This is an ImageJ {@link Command} plugin to generate single or multiple signals.
 * <p>
 * 
 * </p>
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>ComsystanJ>Signal>Signal Generator")
public class SignalGenerator<T extends RealType<T>> implements Command {
	
	private static final String PLUGIN_LABEL = "Generates single or multiple signals";
	private static final String SPACE_LABEL = "";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	@Parameter (label = "Signal(s)", type = ItemIO.OUTPUT) //so that it can be displayed
	private DefaultGenericTable defaultGenericTable;
	
	@Parameter
	private DatasetService datasetService;
	
	@Parameter
    private UIService uiService;
    
    @Parameter
    private PrefService prefService;
    
    @Parameter
    private IOService ioService;
    
    
    //Widget elements------------------------------------------------------
  //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
	private final String labelPlugin = PLUGIN_LABEL;

    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
  	//private final String labelSpace = SPACE_LABEL;
    
    @Parameter(label = "Method",
 		       style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
 		       choices = {"Constant", "Sine", "Square", "Triangle", "SawTooth", "Gaussian", "Uniform", "fGn", "fBm"},
               callback = "changedMethod")
    private String choiceRadioButt_Method;
    
    @Parameter(label = "Number of signals",
    		   style = NumberWidget.SPINNER_STYLE,
    		   min = "1",
    		   max = "999999999999",
    		   initializer = "initialNumSignals",
    		   stepSize = "1",
    		   callback = "changedNumSignals")
    private int spinnerInteger_NumSignals;
    
    @Parameter(label = "Number of data values",
    		   style = NumberWidget.SPINNER_STYLE,
    		   min = "1",
    		   max = "9999999999999999999999999999999",
    		   initializer = "initialNumDataPoints",
    		   stepSize = "1",
    		   callback = "changedNumDataPoints")
    private int spinnerInteger_NumDataPoints;
    
    @Parameter(label = "Constant signal level:",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "9999999999999999999999999999999",
	  		   initializer = "initialConstant",
	  		   stepSize = "1",
	  		   callback = "changedConstant")
    private int spinnerInteger_Constant;
    
    @Parameter(label = "Number of periods",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       initializer = "initialNumPeriods",
 		       stepSize = "1",
 		       callback = "changedNumPeriods")
    private int spinnerInteger_NumPeriods;
    
    @Parameter(label = "Hurst [0,1]:",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "1",
		       initializer = "initialHurst",
		       stepSize = "0.1",
		       callback = "changedHurst")
    private float spinnerFloat_Hurst;
   
    
    //---------------------------------------------------------------------
    
    //The following initialzer functions set initial values
    protected void initialMethod() {
    	choiceRadioButt_Method = "Constant";
    }
    
    protected void initialNumSignals() {
    	spinnerInteger_NumSignals = 1;
    }
    
    protected void initialNumDataPoints() {
    	spinnerInteger_NumDataPoints = 1024;
    }
    
    protected void initialConstant() {
    	spinnerInteger_Constant = 1;
    }
    
    protected void initialNumPeriods() {
    	spinnerInteger_NumPeriods = 10;
    }
    
    protected void initialHurst() {
    	spinnerFloat_Hurst = 0.5f;
    }
    
   
    
	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	/** Executed whenever the {@link #spinInteger_Width} parameter changes. */
	
	/** Executed whenever the {@link #choiceRadioButt_Method} parameter changes. */
	protected void changedMethod() {
		logService.info(this.getClass().getName() + " Method changed to " + choiceRadioButt_Method);
	}
	
	protected void changedNumSignals() {
		logService.info(this.getClass().getName() + " Number of signals changed to " + spinnerInteger_NumSignals);
	}
	
	protected void changedNumDataPoints() {
		logService.info(this.getClass().getName() + " Number of data points changed to " + spinnerInteger_NumDataPoints);
	}
	
	/** Executed whenever the {@link #spinInteger_Constant} parameter changes. */
	protected void changedConstant() {
		logService.info(this.getClass().getName() + " Constant changed to " + spinnerInteger_Constant);
	}
	
	/** Executed whenever the {@link #spinInteger_NumPeriods} parameter changes. */
	protected void changedNumPeriods() {
		logService.info(this.getClass().getName() + " Number of periods changed to " + spinnerInteger_NumPeriods);
	}
	
	/** Executed whenever the {@link #spinFloat_Hurst} parameter changes. */
	protected void changedHurst() {
		logService.info(this.getClass().getName() + " Hurst coefficient changed to " + spinnerFloat_Hurst);
	}
	//--------------------------------------------------------------------------------------------------------
    // You can control how previews work by overriding the "preview" method.
 	// The code written in this method will be automatically executed every time a widget value changes.
 	public void preview() {
 		logService.info(this.getClass().getName() + " Preview initiated");
 		//statusService.showStatus(message);
 	}
 	
    // This is often necessary, for example, if your  "preview" method manipulates data;
 	// the "cancel" method will then need to revert any changes done by the previews back to the original state.
 	public void cancel() {
 		logService.info(this.getClass().getName() + " Widget canceled");
 	}
 	
 	/**
 	 * 
 	 * @param dlgProgress
 	 */
 	private void computeConstantSignals(WaitingDialogWithProgressBar dlgProgress) {
 	        
 	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
 	  	
 	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns
 
 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 	  	
			long startTime = System.currentTimeMillis();
			defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));	 
 	  		
			//computation
			for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, (double)spinnerInteger_Constant);
	 	  	}
 	  		long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}
 	}
 	
 	/**
 	 * 
 	 * @param dlgProgress
 	 */
    private void computeSineSignals(WaitingDialogWithProgressBar dlgProgress) {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	double amplitude = 1.0;
    	//numDataPoints = (numPeriods * sampleRate / frequency);
    	//double factor = 2.0 * Math.PI * frequency / sampleRate;
    	//x = ((double) i / sampleRate);
    	
    	double sampleRate = 1.0 / (double) (spinnerInteger_NumDataPoints - 1);  //Assumption: The whole signal lasts 1 sec
    	double frequency = ((double)spinnerInteger_NumPeriods * sampleRate)/((double)(spinnerInteger_NumDataPoints - 1)); //-1 damit sichs genau wieder auf 0 ausgeht
    	
		double factor = 2.0 * Math.PI * frequency / sampleRate;
	  	
 	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));
	 	  
 	  		//computation
 	  		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows	
	 	  		//Zero is only approximated by a very small number,therefore we need to round 
	 	  		defaultGenericTable.set(c, r, Precision.round(amplitude * Math.sin(r * factor), 12));
	 	  	}
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	} 
	}
    
    /**
     * 
     * @param dlgProgress
     */
    private void computeSquareSignals(WaitingDialogWithProgressBar dlgProgress) {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	double amplitude = 1.0;
    	//numDataPoints = (numPeriods * sampleRate / frequency);
    	//double factor = 2.0 * Math.PI * frequency / sampleRate;
    	//x = ((double) i / sampleRate);
    	
    	double sampleRate = 1.0 / (double) (spinnerInteger_NumDataPoints - 1);  //Assumption: The whole signal lasts 1 sec
    	double frequency = ((double)spinnerInteger_NumPeriods * sampleRate)/((double)(spinnerInteger_NumDataPoints - 1)); //-1 damit sichs genau wieder auf 0 ausgeht
    	
		double factor = 2.0 * Math.PI * frequency / sampleRate;
	  	
 	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));
	 	  
 	  		//computation
 	  		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows	
	 	  		//Zero is only approximated by a very small number,therefore we need to round 
	 	  		defaultGenericTable.set(c, r, amplitude * Math.signum(Precision.round(Math.sin(r * factor), 12)));
	 	  	}
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}
	}
   
    /**
     * 
     * @param dlgProgress
     */
    private void computeTriangleSignals(WaitingDialogWithProgressBar dlgProgress) {
   	
	   	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
	   	double amplitude = 1.0;
	   	//numDataPoints = (numPeriods * sampleRate / frequency);
	   	//double factor = 2.0 * Math.PI * frequency / sampleRate;
	   	//x = ((double) i / sampleRate);
	   	
	   	double sampleRate = 1.0 / (double) (spinnerInteger_NumDataPoints - 1);  //Assumption: The whole signal lasts 1 sec
	   	double frequency = ((double)spinnerInteger_NumPeriods * sampleRate)/((double)(spinnerInteger_NumDataPoints - 1)); //-1 damit sichs genau wieder auf 0 ausgeht
	   	
		double factor = 2.0 * Math.PI * frequency / sampleRate;
	  	
	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 
			long startTime = System.currentTimeMillis();
	  		defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));
	  		
	  		//computation
	 	  	for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows		
	 	  		//Zero is only approximated by a very small number,therefore we need to round 
	 	  		defaultGenericTable.set(c, r, amplitude * Math.asin(Precision.round(Math.sin(r * factor), 12))/Math.asin(1));
	 	  	}
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	  	} 
	}
   
   /**
    * 
    * @param dlgProgress
    */
   private void computeSawToothSignals(WaitingDialogWithProgressBar dlgProgress) {
   	
   	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
   	double amplitude = 1.0;
   	//numDataPoints = (numPeriods * sampleRate / frequency);
   	//double factor = 2.0 * Math.PI * frequency / sampleRate;
   	//x = ((double) i / sampleRate);
   	
   	double sampleRate = 1.0 / (double) (spinnerInteger_NumDataPoints - 1);  //Assumption: The whole signal lasts 1 sec
   	double frequency = ((double)spinnerInteger_NumPeriods * sampleRate)/((double)(spinnerInteger_NumDataPoints - 1)); //-1 damit sichs genau wieder auf 0 ausgeht
   	
		//double factor = 2.0 * Math.PI * frequency / sampleRate;
	  	
	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 
			long startTime = System.currentTimeMillis();
	  		defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));
	  		
	  	    //computation
	 	  	for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows	 	  		
	 	  		double t = (double)r * frequency  /sampleRate;
	 	  		defaultGenericTable.set(c, r, amplitude * ((t - Math.floor(t)) * 2.0 - 1.0));
	 	  	}
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	  	}
	}
    
    /**
     * 
     * @param dlgProgress
     */
    private void computeGaussianSignals(WaitingDialogWithProgressBar dlgProgress) {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	double SD = 1.0;
    	double mean = 0.0;
    
 	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));
 	  		
 	  		//computation
	 	  	for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, generator.nextGaussian() * SD + mean);
	 	  	}
	 	  	
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}
    }
    
    /**
     * 
     * @param dlgProgress
     */
    private void computeUniformSignals(WaitingDialogWithProgressBar dlgProgress) {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	double amplitude = 1.0;
    
 	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));
 	  		
 	  		//computation
	 	  	for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, generator.nextDouble() * amplitude);
	 	  	}
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * Generates fGn signals using Davis and Harte autocorrelation method DHM 
     * 
     * @param dlgProgress
     */
    
    private void computefGnSignals(WaitingDialogWithProgressBar dlgProgress) {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	
    	int M = spinnerInteger_NumDataPoints * 2;
		double[]   signalAC;
		double[]   signalS;
		double[][] signalV;  //complex  [0][*] Real   [1][*] Imag
		double[]   signal = null;

    	
 	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns
 	  
 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));
 	  			
 			// generate auto covariance signal
 			signalAC = generateAC(M, spinnerFloat_Hurst);	
 			signalS  = calcDFTofAC_Equ6a(signalAC); // Imaginary signal strictly not needed, it is always 0
 		
 			boolean allDataPointsAreNotNegative = checkIfDataPointsAreNotNegative(signalS);
 			if (allDataPointsAreNotNegative) {
 				signalV = calcRSA_Equ6be(signalS);
 					
 				signal  = calcDFTofRSA_Equ6f(signalV);

 				// take another signal to show or so
 				// signal = signalAC;
 			} else {
 				// output nothing
 			}

 			// take another signal
 			// signal = signalS;
 			// signal = new double[];
 			// for (int i = 0; i < signalV.length; i++){
 			// signal[i] = signalV[0][i];
 			// }

// 			// set x-axis data points
// 			for (int i = 0; i < signal.length; i++) {
// 				range.add((double) i + 1.0d);
// 			}		
// 			if (fGnTofBm == 1) {
// 				signal = this.convert_fGnTofBm(signal);
// 			}
	 	  	for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, signal[r]);
	 	  	}
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * Generates fBm signals using spectral synthesis method SSM
     * 
     * @param dlgProgress
     */
    private void computefBmSignals(WaitingDialogWithProgressBar dlgProgress) { 
    	
	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
	    	
	  	int M = (2 * spinnerInteger_NumDataPoints) * 4; // 2* because of DFT halves the number, *2 or *4 is not so memory intensive than *8 as suggested by Caccia et al 1997
		double beta = 2.0d * spinnerFloat_Hurst + 1.0d;
		double[] signalReal;
		double[] signalImag;
		double[] signalPhase;
		double[] signalPower;
		double[] signal;
		Random generator = new Random();
		
 	  	for (int c = 0; c < spinnerInteger_NumSignals; c++) {    //columns
 	  	  	
 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSignals) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSignals, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSignals);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSignals);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Signal_" + (String.format("%03d",(c +1))));
 	  		
 	  		signalPhase = new double[M];
 			signalPower = new double[M];
 			signalReal  = new double[M/2];
 	  		
 	  		// generate random phase variables [0, 2pi]
			generator = new Random();
			for (int i = 0; i < M; i++) {
				signalPhase[i] = generator.nextDouble() * 2.0d * Math.PI;// -1.0d*Math.PI);
			}
		
			// set 1/f beta power spectrum
			for (int i = 0; i < M; i++) {
				signalPower[i] = 1.0d / Math.pow(i + 1, beta);
			}
	
			double sumReal = 0;
			double sumImag = 0;
			// calculate invers FFT
			int N = signalPower.length;
			for (int k = 0; k < N / 2; k++) { // output points
				sumReal = 0;
				sumImag = 0;
				for (int n = 0; n < N; n++) { // input points
					double cos = Math.cos(2 * Math.PI * n * k / N);
					double sin = Math.sin(2 * Math.PI * n * k / N);
					double real = Math.sqrt(signalPower[n]) * Math.cos(signalPhase[n]);
					double imag = Math.sqrt(signalPower[n]) * Math.sin(signalPhase[n]);
					sumReal += real * cos + imag * sin;
					// sumImag += -real * sin + imag * cos;
				}
				signalReal[k] = sumReal;
				// signalImag[k] = sumImag;
			}
			signal = signalReal;
			
			// eliminate data points which are too much
			if (signal.length > spinnerInteger_NumDataPoints) {
				for (int i = 0; i < (signal.length - spinnerInteger_NumDataPoints) / 2; i++) {
					signal[i] = Double.NaN;
				}
				for (int i = (signal.length - spinnerInteger_NumDataPoints) / 2 + spinnerInteger_NumDataPoints; i < signal.length; i++) {
					signal[i] = Double.NaN;
				}
				signal = removeNaN(signal);
			}
				
//			// set x-axis data points
//			for (int i = 0; i < signal.size(); i++) {
//				range[i] = ((double) i + 1.0d);
//			}			
//			if (fBmTofGn == 1) {
//				signal = this.convert_fBmTofGn(signal);
//			}
 	  		  		
	 	  	for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, signal[r]);
	 	  	}
	 		long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
	/**
	 * this method generates an auto covariance function with defined length and Hurst coefficient
	 * 
	 * @param M
	 * @param hurst
	 * @return auto covariance function
	 */
	private double[] generateAC(int M, double hurst) {
		double[] signalAC = new double[M+1];
		for (int i =0; i<signalAC.length; i++) signalAC[i] = Double.NaN;
		double sigma = 1;
		double sigma2 = sigma * sigma;
		for (int t = 0; t <= M / 2; t++) {
			signalAC[t] = sigma2 / 2.0d * (Math.pow(Math.abs(t + 1), 2 * hurst) - 2 * Math.pow(Math.abs(t), 2 * hurst) + Math.pow(Math.abs(t - 1), 2 * hurst));	
		}
		for (int t = M / 2 - 1; t >= 0; t--) {
			signalAC[(M) - t] = signalAC[t];
		}
		return signalAC;
	}
    
	/**
	 * This method calculates the DFT of a complex number
	 * 
	 * @param inReal
	 * @param inImag
	 * @param outReal
	 * @param outImag
	 */
	private void calcDFT(double[] inReal, double[] inImag, double[] outReal, double[] outImag) {
		int N = inReal.length;
		for (int k = 0; k < N; k++) { // output points
			double sumReal = 0;
			double sumImag = 0;
			for (int n = 0; n < N; n++) { // input points
				double cos = Math.cos(2 * Math.PI * n * k / N);
				double sin = Math.sin(2 * Math.PI * n * k / N);
				sumReal +=  inReal[n] * cos + inImag[n] * sin;
				sumImag += -inReal[n] * sin + inImag[n] * cos;
			}
			outReal[k] = sumReal;
			outImag[k] = sumImag;
		}
	}

	/**
	 * This method calculates the DFT of a real number
	 * 
	 * @param inReal
	 * @param outReal
	 * @param outImag
	 */
	private void calcDFT(double[] inReal, double[] outReal, double[] outImag) {
		int N = inReal.length;
		for (int k = 0; k < N; k++) { // output points
			double sumReal = 0;
			double sumImag = 0;
			for (int n = 0; n < N; n++) { // input points
				double cos = Math.cos(2 * Math.PI * n * k / N);
				double sin = Math.sin(2 * Math.PI * n * k / N);
				sumReal +=  inReal[n] * cos;
				sumImag += -inReal[n] * sin;
			}
			outReal[k] = sumReal;
			outImag[k] = sumImag;
		}
	}

	/**
	 * This method calculates the DFT of a real numbered auto correlation
	 * function according to DHM Method Caccia et al 1997 Equ 6a
	 * 
	 * @param inReal
	 * @return signalS
	 */
	private double[] calcDFTofAC_Equ6a(double[] inReal) {
		int M = inReal.length;
		double[] signalS = new double[M/2+1];
		for (int j = 0; j <= M / 2; j++) { // Sj
			double sumReal1 = 0;
			double sumImag1 = 0;
			double sumReal2 = 0;
			double sumImag2 = 0;
			for (int t = 0; t <= M/2; t++) { // Sum1
				double cos = Math.cos(2 * Math.PI * j * t / M);
				// double sin = Math.sin(2*Math.PI * j * t / M);
				sumReal1 += inReal[t] * cos;
				// sumImag1 += -inReal.get(t) * sin;
			}
			for (int t = M/2 + 1; t <= M-1; t++) { // Sum2
				double cos = Math.cos(2 * Math.PI * j * t / M);
				// double sin = Math.sin(2*Math.PI * j * t / M);
				sumReal2 += inReal[M - t] * cos;
				// sumImag2 += -inReal.get(M-t) * sin;
			}
			//Zero is sometimes approximated by a very small but negative number,therefore we need to round 
			signalS[j] = Precision.round(sumReal1 + sumReal2, 4);
			// signalS.add(sumReal1 + sumReal2 + (M/2 - j));???????????????
			// signalS.add(sumImag1 + sumImag2);
		}
		return signalS;
	}

	private boolean checkIfDataPointsAreNotNegative(double[] signal) {
		boolean allDataPointsAreNotNegative = true;
		for (int i = 0; i < signal.length; i++) {
			if (signal[i] < 0) {
				logService.info(this.getClass().getName() + "  Signal has negative values, which is not allowed!");
				allDataPointsAreNotNegative = false;
			}
		}
		return allDataPointsAreNotNegative;
	}

	private double[][] calcRSA_Equ6be(double[] signalS) {//Otput is complex  [0][*] Real,  [1][*] Imaginary 
	
		int M = (signalS.length - 1) * 2;
		
		double[][]signalV = new double[2][M]; // Complex
		double[]  signalW = new double[M]; // Gaussian random variables
		

		// generate random variables
		Random generator = new Random();
		// RandomData generator = new RandomDataImpl();
		double stdDev = 1.0d;
		double mean   = 0.0d;
		for (int i = 0; i < M; i++) {
			signalW[i] = generator.nextGaussian() * stdDev + mean;
			//signalW[i] = generator.nextGaussian(mean, stdDev);
		}

		// calculate randomized spectral amplitudes

		// set first value Equ6b;
		signalV[0][0] = Math.sqrt(signalS[0]) * signalW[0];  //Real
		signalV[1][0] = 0.0d; //Imag
		// set next values Equ6c
		for (int k = 1; k < M / 2; k++) {
			signalV[0][k] = Math.sqrt(1.0d / 2.0d * signalS[k]) * signalW[2 * k - 1]; //Real
			signalV[1][k] = Math.sqrt(1.0d / 2.0d * signalS[k]) * signalW[2 * k];     //Imag
		}
		// set middle value Equ6d
		signalV[0][M/2] = Math.sqrt(signalS[M / 2]) * signalW[M - 1];
		signalV[0][M/2] = 0.0d;		
		// set next values Equ6e
		for (int k = M / 2 + 1; k <= (M - 1); k++) {
			//Set complex conjugate
			signalV[0][k] =  signalV[0][M-k];
			signalV[1][k] = -signalV[1][M-k];
		}
		return signalV;
	}

	/**
	 * This method calculates the simulated time series
	 * 
	 * @param complex signalV [0][*] Real, [1][*]  Imaginary
	 * @return the simulated signal
	 */
	private double[] calcDFTofRSA_Equ6f(double[][] signalV) { //complex signalV [0][*] Real, [1][*] Imaginary
		int M = signalV[0].length;
		double[] signalY = new double[M/2];
		double sumReal = 0.0;
		double sumImag = 0.0;
		for (int k = 0; k < M / 2; k++) { // half of output points
			sumReal = 0.0;
			sumImag = 0.0;
			// double sumImag = 0;
			for (int n = 0; n < M; n++) { // input points
				double cos = Math.cos(2 * Math.PI * n * k / M);
				double sin = Math.sin(2 * Math.PI * n * k / M);
				sumReal +=  signalV[0][n] * cos + signalV[1][n] * sin;
				sumImag += -signalV[0][n] * sin + signalV[1][n] * cos; //Imag is always 0
			}
			signalY[k] = (1.0d / Math.sqrt(M) * sumReal);
		}		
		return signalY;
	}

	/**
	 * This method calculates the power spectrum of a signal
	 * 
	 * @param signal
	 * @return the power spectrum of the signal
	 */
	private double[] calcPowerSpectrum(double[]signal) {
		int N = signal.length;
		double[] signalPower = new double[N];
		double[] signalPhase = new double[N];	
		
		for (int k = 0; k < N; k++) { // output points
			double sumReal = 0;
			double sumImag = 0;
			for (int n = 0; n < N; n++) { // input points
				double cos = Math.cos(2 * Math.PI * n * k / N);
				double sin = Math.sin(2 * Math.PI * n * k / N);
				sumReal += signal[n]  * cos;
				sumImag += -signal[n] * sin;
			}
			signalPower[k] = Math.sqrt(sumReal*sumReal + sumImag * sumImag);
			signalPhase[k] = Math.atan(sumImag/sumReal);
		}
		return signalPower;
	}
    
 // This method removes NaN  from field signal1D
 	private double[] removeNaN(double[] signal) {
 		int lengthOld = signal.length;
 		int lengthNew = 0;
 		
 		for (int i = 0; i < lengthOld; i++) {
 			if (!Double.isNaN(signal[i])) {
 				lengthNew += 1;
 			}
 		}
 		double[] signal1D = new double[lengthNew];
 		int ii = -1;
 		for (int i = 0; i < lengthOld; i++) {
 			if (!Double.isNaN(signal[i])) {
 				ii +=  1;
 				signal1D[ii] = signal[i];
 			}
 		}
 		return signal1D;
 	}
  
	/**
	 * This runs a signal(s) generator routine
	 * 
     * @param args whatever, it's ignored
     * @throws Exception
     */
    @Override
    public void run() {
    	
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
    		
    	long startTimeAll = System.currentTimeMillis();
   
    	//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Signal generation, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Signal generation, please wait... Open console window for further info.",
		                                                                             logService, false, null); //isCanceable = false, because no following method listens to exec.shutdown 

		//dlgProgress.updatePercent("0%");
		//dlgProgress.setBarIndeterminate(true);
		//dlgProgress.updateBar(0);
		dlgProgress.setVisible(true);
	
    	logService.info(this.getClass().getName() + " Processing all available images");
    	//"Constant", "Sine", "Square", "Triangle", "SawTooth", "Gaussian", "Uniform"
    	//generate this.defaultGenericTable = new DefaultGenericTable();
		if (choiceRadioButt_Method.equals("Constant")) computeConstantSignals(dlgProgress);
		if (choiceRadioButt_Method.equals("Sine"))     computeSineSignals(dlgProgress);
		if (choiceRadioButt_Method.equals("Square"))   computeSquareSignals(dlgProgress);
		if (choiceRadioButt_Method.equals("Triangle")) computeTriangleSignals(dlgProgress);
		if (choiceRadioButt_Method.equals("SawTooth")) computeSawToothSignals(dlgProgress);
		if (choiceRadioButt_Method.equals("Gaussian")) computeGaussianSignals(dlgProgress);
		if (choiceRadioButt_Method.equals("Uniform"))  computeUniformSignals(dlgProgress);
		if (choiceRadioButt_Method.equals("fGn"))      computefGnSignals(dlgProgress);
		if (choiceRadioButt_Method.equals("fBm"))      computefBmSignals(dlgProgress);
	
		
		int percent = (100);
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(100, 100, "Collecting data for table...");
		logService.info(this.getClass().getName() + " Collecting data for table...");	
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
	
		//show table----------------------------------------------------------
		uiService.show("Signal(s)", defaultGenericTable);
		//--------------------------------------------------------------------
		
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
 
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
        int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the signals?\nNot recommended for a large number of signals", "Display option", JOptionPane.YES_NO_OPTION); 
		if (selectedOption == JOptionPane.YES_OPTION) {
			int[] cols = new int[spinnerInteger_NumSignals];
			boolean isLineVisible = true;
			String signalTitle = choiceRadioButt_Method + " signal(s)";
			String xLabel = "#";
			String yLabel = "Value";
			String[] seriesLabels = new String[spinnerInteger_NumSignals];		
			for (int c = 0; c < spinnerInteger_NumSignals; c++) {
				cols[c] = c;
				seriesLabels[c] = defaultGenericTable.getColumnHeader(c);				
			}
			PlotDisplayFrame pdf = new PlotDisplayFrame(defaultGenericTable, cols, isLineVisible, "Signal(s)", signalTitle, xLabel, yLabel, seriesLabels);
			pdf.setVisible(true);
		}
		
			
		//This might free some memory and would be nice for a large table
		defaultGenericTable = null;
		Runtime.getRuntime().gc();
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed total time: "+ sdf.format(duration));
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
    }

	public static void main(final String... args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
//        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
//
//        // ask the user for a file to open
//        final File file = ij.ui().chooseFile(null, "open");
//
//        if (file != null) {
//            // load the dataset
//            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());
//
//            // show the image
//            ij.ui().show(dataset);
//
//            // invoke the plugin
//            ij.command().run(FracCreate3D.class, true);
//        }
//       
         //invoke the plugin
         ij.command().run(SignalGenerator.class, true);
    	
    }

}
