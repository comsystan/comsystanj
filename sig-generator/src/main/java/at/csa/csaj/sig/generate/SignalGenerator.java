/*-
 * #%L
 * Project: ImageJ2 plugin to generate signals.
 * File: SignalGenerator.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2022 Comsystan Software
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
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.plot.SignalPlotFrame;
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
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "Signal generator",
	//iconPath = "/images/comsystan-??.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Signal (1D)"),
	@Menu(label = "Signal generator ", weight = 10)}) //Space at the end of the label is necessary to avoid duplicate with image2d plugin 
public class SignalGenerator<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel
	
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
    
    WaitingDialogWithProgressBar dlgProgress;
    
    private double[] signalCantor;
    
    
    //Widget elements------------------------------------------------------
  //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
	private final String labelPlugin = PLUGIN_LABEL;

    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
  	//private final String labelSpace = SPACE_LABEL;
    
    @Parameter(label = "Method",
    		   description = "Type of signal, fGn..fractional Gaussian noise, fBm..fractional Brownian noise, W-M..Weierstraß-Mandelbrot signal",
 		       style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
 		       choices = {"Constant", "Sine", "Square", "Triangle", "SawTooth", "Gaussian", "Uniform", "Logistic", "Henon", "Cubic", "Spence", "fGn", "fBm", "W-M", "Cantor"},
 		       initializer = "initialMethod",
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
    
    @Parameter(label = "(Constant) Signal level",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "9999999999999999999999999999999",
	  		   initializer = "initialConstant",
	  		   stepSize = "1",
	  		   callback = "changedConstant")
    private int spinnerInteger_Constant;
    
    @Parameter(label = "(Sine) periods",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       initializer = "initialNumPeriods",
 		       stepSize = "1",
 		       callback = "changedNumPeriods")
    private int spinnerInteger_NumPeriods;
    
    @Parameter(label = "(Logistic/Henon/Cubic) a",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "9999999999999999999999",
		       initializer = "initialParamA",
		       stepSize = "0.1",
		       callback = "changedParamA")
    private float spinnerFloat_ParamA;
    
    @Parameter(label = "(Henon) b",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "9999999999999999999999",
		       initializer = "initialParamB",
		       stepSize = "0.1",
		       callback = "changedParamB")
    private float spinnerFloat_ParamB;
    
    @Parameter(label = "(fGn/fBm) Hurst [0,1]",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "1",
		       initializer = "initialHurst",
		       stepSize = "0.1",
		       callback = "changedHurst")
    private float spinnerFloat_Hurst;
   
    @Parameter(label = "(W-M) Fractal dimension [1,2]",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "-2", //Values <1 would produce non-fractal <-> Euclidean shapes 
		       max = "2",
		       initializer = "initialFractalDimWM",
		       stepSize = "0.1",
		       callback = "changedFractalDimWM")
    private float spinnerFloat_FractalDimWM;
    
    @Parameter(label = "(Cantor) Fractal dimension [0,1]",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "1",
		       initializer = "initialFractalDimCantor",
		       stepSize = "0.1",
		       callback = "changedFractalDimCantor")
 private float spinnerFloat_FractalDimCantor;
    //---------------------------------------------------------------------
    
    //The following initializer functions set initial values	
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
    
    protected void initialParamA() {
    	spinnerFloat_ParamA = 4f;
    }
    
    protected void initialParamB() {
    	spinnerFloat_ParamB = 0.3f;
    }
    
    protected void initialHurst() {
    	spinnerFloat_Hurst = 0.5f;
    }
    
    protected void initialFractalDimWM() {
    	spinnerFloat_FractalDimWM = 1.5f;
    }
    
    protected void initialFractalDimCantor() {
    	spinnerFloat_FractalDimCantor = (float)(Math.log(2f)/Math.log(3f)); //0.6309..... 1/3
    }
    
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #spinnerInteger_Width} parameter changes. */
	
	/** Executed whenever the {@link #choiceRadioButt_Method} parameter changes. */
	protected void changedMethod() {
		logService.info(this.getClass().getName() + " Method changed to " + choiceRadioButt_Method);
		if       (choiceRadioButt_Method.equals("Logistic")) {
			spinnerFloat_ParamA = 4f;
			changedParamA();
		}
		else if  (choiceRadioButt_Method.equals("Henon")) {
			spinnerFloat_ParamA = 1.4f;
			spinnerFloat_ParamB = 0.3f;
			changedParamA();
			changedParamB();
		}
		else if  (choiceRadioButt_Method.equals("Cubic")) {
			spinnerFloat_ParamA = 3f;
			changedParamA();
		}
	}
	
	protected void changedNumSignals() {
		logService.info(this.getClass().getName() + " Number of signals changed to " + spinnerInteger_NumSignals);
	}
	
	protected void changedNumDataPoints() {
		logService.info(this.getClass().getName() + " Number of data points changed to " + spinnerInteger_NumDataPoints);
	}
	
	/** Executed whenever the {@link #spinnerInteger_Constant} parameter changes. */
	protected void changedConstant() {
		logService.info(this.getClass().getName() + " Constant changed to " + spinnerInteger_Constant);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumPeriods} parameter changes. */
	protected void changedNumPeriods() {
		logService.info(this.getClass().getName() + " Number of periods changed to " + spinnerInteger_NumPeriods);
	}
	
	/** Executed whenever the {@link #spinFloat_ParamA} parameter changes. */
	protected void changedParamA() {
		spinnerFloat_ParamA = Precision.round(spinnerFloat_ParamA, 2);
		logService.info(this.getClass().getName() + " Parameter a changed to " + spinnerFloat_ParamA);
	}
	
	/** Executed whenever the {@link #spinFloat_ParamB} parameter changes. */
	protected void changedParamB() {
		spinnerFloat_ParamB = Precision.round(spinnerFloat_ParamB, 2);
		logService.info(this.getClass().getName() + " Parameter b changed to " + spinnerFloat_ParamB);
	}
	
	/** Executed whenever the {@link #spinFloat_Hurst} parameter changes. */
	protected void changedHurst() {
		spinnerFloat_Hurst = Precision.round(spinnerFloat_Hurst, 2);
		logService.info(this.getClass().getName() + " Hurst coefficient changed to " + spinnerFloat_Hurst);
	}
	
	/** Executed whenever the {@link #spinFloat_FractalDimWM} parameter changes. */
	protected void changedFractalDimWM() {
		spinnerFloat_FractalDimWM = Precision.round(spinnerFloat_FractalDimWM, 7);
		logService.info(this.getClass().getName() + " W-M Fractal dimension changed to " + spinnerFloat_FractalDimWM);
	}
	
	/** Executed whenever the {@link #spinFloat_FractalDimCantor} parameter changes. */
	protected void changedFractalDimCantor() {
		spinnerFloat_FractalDimCantor= Precision.round(spinnerFloat_FractalDimCantor, 7);
		logService.info(this.getClass().getName() + " Cantor Fractal dimension changed to " + spinnerFloat_FractalDimCantor);
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
 	 */
 	private void computeConstantSignals() {
 	        
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
 	 */
    private void computeSineSignals() {
    	
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
     */
    private void computeSquareSignals() {
    	
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
     */
    private void computeTriangleSignals() {
   	
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
    */
   private void computeSawToothSignals() {
   	
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
     */
    private void computeGaussianSignals() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
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
     */
    private void computeUniformSignals() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
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
     * <b>DCM discrete chaotic map</b><br>
     * According to Silva & Murta Jr Evaluation of physiologic complexity in time series
     * using generalized sample entropy and surrogate data analysis. Chaos 22, 2012
     * 
     * This method computes a logistic signal
     * 
     */
    private void computeLogisticSignals() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    	
    	double valueX;
    
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
 	  		//set first data point randomly
 			//range[c][0] = (double)0.0d + 1.0d;
 	  		valueX = (generator.nextDouble() + 0.0000001d);
 	  		
 		   //skip first 50 points to be attracted to the attractor
 	  		for (int r = 0; r < 50; r++) { //r
 	  			valueX = (spinnerFloat_ParamA * valueX * (1.0d - valueX));
 	  		}
 	  		
 	  		defaultGenericTable.set(c, 0, valueX);
	 	  	for (int r = 1; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, (spinnerFloat_ParamA * (double)defaultGenericTable.get(c, r - 1) * (1.0d - (double)defaultGenericTable.get(c, r - 1))));
	 	  		//range[c][r] = ((double)r + 1.0d);
	 	  	}
	 	  	
	 	  	if (amplitude != 1.0) {
		 		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { 
		 	  		defaultGenericTable.set(c, r, amplitude*(double)defaultGenericTable.get(c, r));
		 	  	}
	 	  	}
	 	  	
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * <b>DCM discrete chaotic map</b><br>
     * According to Silva & Murta Jr Evaluation of physiologic complexity in time series
     * using generalized sample entropy and surrogate data analysis. Chaos 22, 2012
     * 
     * This method computes a Henon signal
     * 
     */
    private void computeHenonSignals() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    	//double[] signalX; //X coordinate of  Henon map
    	
    	double valueXOld; 
    	double valueXNew; 
    	double valueY;
    
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
 	  		//set first data point randomly
 			//range[c][0] = (double)0.0d + 1.0d;
 	  		//signalY = new double[spinnerInteger_NumDataPoints];
 	  		//signalY[0] = 0; //a random signalX starting point can be outside the basin of attraction, so the maps can drift away to -infinity // = amplitude * (generator.nextDouble());
 	  		valueXOld = (generator.nextDouble() + 0.0000001d);
 	  		valueY = 0; //a random Y starting point can be outside the basin of attraction, so the maps can drift away to -infinity // = amplitude * (generator.nextDouble());
 	  		
 	  	   //skip first 50 points to be attracted to the attractor
 	  		for (int r = 0; r < 50; r++) { //r
 	  			valueXNew = 1.0 - spinnerFloat_ParamA*(valueXOld*valueXOld) + valueY;
 	  			valueY = spinnerFloat_ParamB*valueXOld;
 	  			valueXOld = valueXNew;
 	  		}
 	  	
 	  		defaultGenericTable.set(c, 0, valueXOld);
	 	  	for (int r = 1; r < spinnerInteger_NumDataPoints; r++) { //rows skip first 50 points to be attracted t othe attractor
	 	  		defaultGenericTable.set(c, r, (valueY + 1.0d - (spinnerFloat_ParamA*(double)defaultGenericTable.get(c, r - 1)*(double)defaultGenericTable.get(c, r - 1))));
	 	  		valueY = (spinnerFloat_ParamB * (double)defaultGenericTable.get(c, r - 1));
	 	  		//signalX[r] = amplitude*(spinnerFloat_ParamB * (double)defaultGenericTable.get(c, r - 1));
	 	  		//range[c][r] = ((double)r + 1.0d);
	 	  	}
	 	  	
	 	  	if (amplitude != 1.0) {
		 		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { 
		 	  		defaultGenericTable.set(c, r, amplitude*(double)defaultGenericTable.get(c, r));
		 	  	}
	 	  	}
	 	  	
	 	  	//signalX = null;
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * <b>DCM discrete chaotic map</b><br>
     * According to Silva & Murta Jr Evaluation of physiologic complexity in time series
     * using generalized sample entropy and surrogate data analysis. Chaos 22, 2012
     * 
     * This method computes a cubic signal
     * 
     */
    private void computeCubicSignals() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    	double valueX;
    
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
 	  		//set first data point randomly
 			//range[c][0] = (double)0.0d + 1.0d;
 	  		
	  		valueX = (generator.nextDouble() + 0.0000001d);
 	  		
 		   //skip first 50 points to be attracted to the attractor
 	  		for (int r = 0; r < 50; r++) { //r
 	  			valueX = (spinnerFloat_ParamA * valueX * (1.0d - valueX*valueX));
 	  		}
 	  		
 	  		defaultGenericTable.set(c, 0, valueX);
	 	  	for (int r = 1; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, (spinnerFloat_ParamA * (double)defaultGenericTable.get(c, r - 1) * (1.0d - (double)defaultGenericTable.get(c, r - 1)*(double)defaultGenericTable.get(c, r - 1))));
	 	  		//range[c][r] = ((double)r + 1.0d);
	 	  	}
	 	  	
	 		if (amplitude != 1.0) {
		 		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { 
		 	  		defaultGenericTable.set(c, r, amplitude*(double)defaultGenericTable.get(c, r));
		 	  	}
	 	  	}
	 	  	
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * <b>DCM discrete chaotic map</b><br>
     * According to Silva & Murta Jr Evaluation of physiologic complexity in time series
     * using generalized sample entropy and surrogate data analysis. Chaos 22, 2012
     * 
     * This method computes a Spence signal
     * 
     */
    private void computeSpenceSignals() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    	double valueX;
    
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
 	  		//set first data point randomly
 			//range[c][0] = (double)0.0d + 1.0d;
 	  		
	  		valueX = (generator.nextDouble() + 0.0000001d);
 	  		
 		   //skip first 50 points to be attracted to the attractor
 	  		for (int r = 0; r < 50; r++) { //r
 	  			valueX = Math.abs(Math.log(valueX));
 	  		}
 	  		
 	  		defaultGenericTable.set(c, 0, valueX);
	 	  	for (int r = 1; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, (Math.abs(Math.log((double)defaultGenericTable.get(c, r - 1)))));
	 	  		//range[c][r] = ((double)r + 1.0d);
	 	  	}
	 	  	
	 		if (amplitude != 1.0) {
		 		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { 
		 	  		defaultGenericTable.set(c, r, amplitude*(double)defaultGenericTable.get(c, r));
		 	  	}
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
     */
    
    private void computefGnSignals() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
    	double amplitude = 1.0;
    	
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
	 	  		defaultGenericTable.set(c, r, amplitude * signal[r]);
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
     */
    private void computefBmSignals() { 
    	
	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
	    double amplitude = 1.0;	
	  	
	  	int M = (2 * spinnerInteger_NumDataPoints) * 4; // 2* because of DFT halves the number, *2 or *4 is not so memory intensive than *8 as suggested by Caccia et al 1997
		double beta = 2.0d * spinnerFloat_Hurst + 1.0d;
		double[] signalReal;
		double[] signalImag;
		double[] signalPhase;
		double[] signalPower;
		double[] signal;
		Random generator = new Random();
		generator.setSeed(System.currentTimeMillis());
		
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
			generator.setSeed(System.currentTimeMillis());
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
	 	  		defaultGenericTable.set(c, r, amplitude * signal[r]);
	 	  	}
	 		
	 	 	
	 		long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * Generates Weierstrass-Mandelbrot signals
     */
    private void computeWMSignals() { 
    	
	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
	    double amplitude = 1.0;	
	
	    float fracDim = spinnerFloat_FractalDimWM;
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	int M = 50;
    	double gamma = 1.5;
    	double phi;
    	double offsetX;
    	double value;
    	double x;
    	
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
 	  		//********HA: arbitrary starting point, otherwise all signals would be identical******
 	  		offsetX = generator.nextDouble()*(Integer.MAX_VALUE - spinnerInteger_NumDataPoints); 
 	  		//offsetX = 0;
 	  		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows 	  		
 	  			//but also found in Falconer: 0 = t0<t1<....tm=1
 	  			//-> datapoint number/number of datapoints!!!!
	 	  		x = (double)r/(double)spinnerInteger_NumDataPoints + offsetX; //this fulfills also x>0 Majumdar&Tien paper
	 	  	
	 	  		value = 0.0;
	 	  		for (int n = 0; n < M; n++) {
	 	  			
	 	  			//Berry&Levis 1980, 10.1098/rspa.1980.0044
	 	  			//Falconer, Fractal Geometry, 2013 ed3 p178 Falconer is without -1^n
	 	  			//1>D<2, 0 = t0<t1<....tm=1
	 	  			//gamma = 1.5 Majumdar&Tien 1990, Wear, DOI 10.1016/0043-1648(90)90154-3
	 	  			//value = value + Math.pow(-1, n)*Math.sin(Math.pow(gamma,  n)*x)/Math.pow(gamma, (2.0-fracDim)*n); //Berry
	 	  			value = value +                   Math.sin(Math.pow(gamma,  n)*x)/Math.pow(gamma, (2.0-fracDim)*n); //Falconer
	 	  						
	 	  			//Berry&Levis 1980, 10.1098/rspa.1980.0044
	 	  			//1>D<2, 0<=t<=1, never negative
	 	  			//also Zhang et al 2014, Fractals, DOI 10.1142/S0218348X15500061
	 	  			//value = value + (1 - Math.cos(x*Math.pow(gamma, n)))/(Math.pow(gamma, (2.0-fracDim)*n));	
	 	  			
	 	  			//Majumdar&Tien 1990, Wear, DOI 10.1016/0043-1648(90)90154-3
	 	  			//1>D<2   Gamma = 1.5  x>0  
	 	  			//2*PI seems to be too much
	 	  			//value = value + Math.cos(2.0*Math.PI*Math.pow(gamma, n)*x)/ Math.pow(gamma, n*(2.0-fracDim)); 
	 	  			
	 	  			//Wang etal, 2013, IEEE Access, DOI10.1109/ACCESS.2019.2926515
	 	  			//phi = generator.nextDouble()/100;  //??? not specified in the paper  //*2*Math.PI would be far too large -> always too irregular
	 	  			//value = value + Math.pow(gamma, (fracDim - 2.0)*n)*(Math.cos(phi) - Math.cos(x*Math.pow(gamma, n) + phi));
	 	  			
	 	  		}
	 	  		defaultGenericTable.set(c, r, value);
	 	  	}
	 	  	
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * Generates BINARY Cantor set signals
     * FD to gamma relation according to 
     * Cherny, A. Y., E. M. Anitas, A. I. Kuklin, M. Balasoiu, und V. A. Osipov. „Scattering from Generalized Cantor Fractals“. Journal of Applied Crystallography 43, Nr. 4 (1. August 2010): 790–97. https://doi.org/10.1107/S0021889810014184.
     *
     */
    private void computeCantorSignals() { 
    	
	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSignals, spinnerInteger_NumDataPoints);
	    double amplitude = 1.0;	
    	
    	//double[] signal;
	    
	    float fracDim = spinnerFloat_FractalDimCantor;
	    
	    //DEBUG: Overwrite fracDim with standard value 
	    //fracDim = (float)(Math.log(2f)/Math.log(3f)); //0.6309..... 1/3
	    
	    float gamma = -(2f*(float)Math.exp(-Math.log(2f)/fracDim) - 1f); //gamma = 1/3 for standard FD  0.6309..
    	
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
 	  		signalCantor = new double[spinnerInteger_NumDataPoints];
 	  		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) signalCantor[r] = amplitude;	
 	  		deleteSegment(gamma, spinnerInteger_NumDataPoints, 0);	
 	  		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) defaultGenericTable.set(c, r, signalCantor[r]);
	 	  		  	
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
   
    /**
     * This method eliminates recursively a segment in the middle of length
     * The segment length is gamma*length
     * 
     * @param float gamma;
     * @param in length;
     * @param int start;
     */   
    private void deleteSegment(float gamma, int length, int start) {
        //segment = 1/3 for FD=0.6309....
    	int segment = (int)Math.round(gamma*length); //segment length in the middle
    	if (segment == length) return; //segment too large
    	if (segment == 0) return; //segment too small
    	
    	int indxStart = (length - segment)/2;
    	int indxEnd   = (length - segment)/2 + segment;
      
        for (int i = start + indxStart; i < start + indxEnd; i++) signalCantor[i] = 0;
             
        deleteSegment(gamma, (length - segment)/2, start);
        deleteSegment(gamma, (length - segment)/2, start + indxEnd);
    }
    
	/**
	 * This method generates an auto covariance function with defined length and Hurst coefficient
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
		generator.setSeed(System.currentTimeMillis());
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
     */
    @Override
    public void run() {
    	
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
    		
    	long startTimeAll = System.currentTimeMillis();
   
    	//dlgProgress = new WaitingDialogWithProgressBar("<html>Signal generation, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Signal generation, please wait... Open console window for further info.",
		                                                                             logService, false, null); //isCanceable = false, because no following method listens to exec.shutdown 

		//dlgProgress.updatePercent("0%");
		//dlgProgress.setBarIndeterminate(true);
		//dlgProgress.updateBar(0);
		dlgProgress.setVisible(true);
	
    	logService.info(this.getClass().getName() + " Processing all available images");
    	//"Constant", "Sine", "Square", "Triangle", "SawTooth", "Gaussian", "Uniform", "Logistic", "Henon", "Cubic", "Spence", "fGn", "fBm"
    	//generate this.defaultGenericTable = new DefaultGenericTable();
		if      (choiceRadioButt_Method.equals("Constant")) computeConstantSignals();
		else if (choiceRadioButt_Method.equals("Sine"))     computeSineSignals();
		else if (choiceRadioButt_Method.equals("Square"))   computeSquareSignals();
		else if (choiceRadioButt_Method.equals("Triangle")) computeTriangleSignals();
		else if (choiceRadioButt_Method.equals("SawTooth")) computeSawToothSignals();
		else if (choiceRadioButt_Method.equals("Gaussian")) computeGaussianSignals();
		else if (choiceRadioButt_Method.equals("Uniform"))  computeUniformSignals();
		else if (choiceRadioButt_Method.equals("Logistic")) computeLogisticSignals();
		else if (choiceRadioButt_Method.equals("Henon"))    computeHenonSignals();
		else if (choiceRadioButt_Method.equals("Cubic"))    computeCubicSignals();
		else if (choiceRadioButt_Method.equals("Spence"))   computeSpenceSignals();
		else if (choiceRadioButt_Method.equals("fGn"))      computefGnSignals();
		else if (choiceRadioButt_Method.equals("fBm"))      computefBmSignals();
		else if (choiceRadioButt_Method.equals("W-M"))      computeWMSignals();
		else if (choiceRadioButt_Method.equals("Cantor"))   computeCantorSignals();
	
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
			SignalPlotFrame pdf = new SignalPlotFrame(defaultGenericTable, cols, isLineVisible, "Signal(s)", signalTitle, xLabel, yLabel, seriesLabels);
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
