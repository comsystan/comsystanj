/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DGeneratorCommand.java
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
package at.csa.csaj.command;


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
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.Dialog_WaitingWithProgressBar;
import at.csa.csaj.commons.Plot_SequenceFrame;

import java.awt.Toolkit;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
/**
 * This is an ImageJ {@link ContextCommand} plugin to generate single or multiple sequences.
 * <p>
 * 
 * </p>
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "1D sequence generator",
	iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
	menu = {}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * The Maven build will execute CreateCommandFiles.java which creates Csaj***Command.java files
 *
 *
 */
public class Csaj1DGeneratorCommand<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL = "Generates single or multiple sequences";
	private static final String SPACE_LABEL = "";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	@Parameter (label = "Sequence(s)", type = ItemIO.OUTPUT) //so that it can be displayed
	private DefaultGenericTable defaultGenericTable;
	
	@Parameter
	private DatasetService datasetService;
	
	@Parameter
    private UIService uiService;
    
    @Parameter
    private PrefService prefService;
    
    @Parameter
    private IOService ioService;
    
    Dialog_WaitingWithProgressBar dlgProgress; 
    private ExecutorService exec;
    private double[] sequenceCantor;
    
    
    //Widget elements------------------------------------------------------
  //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
	private final String labelPlugin = PLUGIN_LABEL;

    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
  	//private final String labelSpace = SPACE_LABEL;
    
    @Parameter(label = "Method",
    		   description = "Type of sequence, fGn..fractional Gaussian noise, fBm..fractional Brownian noise, W-M..Weierstraß-Mandelbrot sequence",
 		       style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
 		       choices = {"Constant", "Sine", "Square", "Triangle", "SawTooth", "Gaussian", "Uniform", "Logistic", "Lorenz", "Henon", "Cubic", "Spence", "fGn", "fBm", "W-M", "Cantor"},
 		       initializer = "initialMethod",
               callback = "callbackMethod")
    private String choiceRadioButt_Method;
    
    @Parameter(label = "Number of sequences",
    		   style = NumberWidget.SPINNER_STYLE,
    		   min = "1",
    		   max = "999999999999",
    		   initializer = "initialNumSequences",
    		   stepSize = "1",
    		   callback = "callbackNumSequences")
    private int spinnerInteger_NumSequences;
    
    @Parameter(label = "Number of data values",
    		   style = NumberWidget.SPINNER_STYLE,
    		   min = "1",
    		   max = "9999999999999999999999999999999",
    		   initializer = "initialNumDataPoints",
    		   stepSize = "1",
    		   callback = "callbackNumDataPoints")
    private int spinnerInteger_NumDataPoints;
    
    @Parameter(label = "(Constant) Sequence level",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "9999999999999999999999999999999",
	  		   initializer = "initialConstant",
	  		   stepSize = "1",
	  		   callback = "callbackConstant")
    private int spinnerInteger_Constant;
    
    @Parameter(label = "(Sine) periods",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       initializer = "initialNumPeriods",
 		       stepSize = "1",
 		       callback = "callbackNumPeriods")
    private int spinnerInteger_NumPeriods;
    
    @Parameter(label = "(Logistic/Henon/Cubic) a",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "9999999999999999999999",
		       initializer = "initialParamA",
		       stepSize = "0.1",
		       callback = "callbackParamA")
    private float spinnerFloat_ParamA;
    
    @Parameter(label = "(Henon) b",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "9999999999999999999999",
		       initializer = "initialParamB",
		       stepSize = "0.1",
		       callback = "callbackParamB")
    private float spinnerFloat_ParamB;
    
    @Parameter(label = "(fGn/fBm) Hurst [0,1]",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "1",
		       initializer = "initialHurst",
		       stepSize = "0.1",
		       callback = "callbackHurst")
    private float spinnerFloat_Hurst;
   
    @Parameter(label = "(W-M) Fractal dimension [1,2]",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "-2", //Values <1 would produce non-fractal <-> Euclidean shapes 
		       max = "2",
		       initializer = "initialFractalDimWM",
		       stepSize = "0.1",
		       callback = "callbackFractalDimWM")
    private float spinnerFloat_FractalDimWM;
    
    @Parameter(label = "(Cantor) Fractal dimension [0,1]",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "1",
		       initializer = "initialFractalDimCantor",
		       stepSize = "0.1",
		       callback = "callbackFractalDimCantor")
    private float spinnerFloat_FractalDimCantor;
   
    @Parameter(label = "Do not show signal(s)", persist = false,
    		   description = "Recommended for large series to reduce memory demand",
    		   initializer = "initialDoNotShowSignals",
		       callback = "callbackDoNotShowSignals")
	private boolean booleanDoNotShowSignals;
    
	@Parameter(label = "Process", callback = "callbackProcess")
	private Button buttonProcess;
    //---------------------------------------------------------------------
    
    //The following initializer functions set initial values	
    protected void initialMethod() {
    	choiceRadioButt_Method = "Constant";
    }
    
    protected void initialNumSequences() {
    	spinnerInteger_NumSequences = 1;
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
    
    protected void initialDoNotShowSignals() {
    	booleanDoNotShowSignals = false;
    }
    
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #spinnerInteger_Width} parameter changes. */
	
	/** Executed whenever the {@link #choiceRadioButt_Method} parameter changes. */
	protected void callbackMethod() {
		logService.info(this.getClass().getName() + " Method changed to " + choiceRadioButt_Method);
		if       (choiceRadioButt_Method.equals("Logistic")) {
			spinnerFloat_ParamA = 4f;
			callbackParamA();
		}
		else if  (choiceRadioButt_Method.equals("Henon")) {
			spinnerFloat_ParamA = 1.4f;
			spinnerFloat_ParamB = 0.3f;
			callbackParamA();
			callbackParamB();
		}
		else if  (choiceRadioButt_Method.equals("Cubic")) {
			spinnerFloat_ParamA = 3f;
			callbackParamA();
		}
	}
	
	protected void callbackNumSequences() {
		logService.info(this.getClass().getName() + " Number of sequences changed to " + spinnerInteger_NumSequences);
	}
	
	protected void callbackNumDataPoints() {
		logService.info(this.getClass().getName() + " Number of data points changed to " + spinnerInteger_NumDataPoints);
	}
	
	/** Executed whenever the {@link #spinnerInteger_Constant} parameter changes. */
	protected void callbackConstant() {
		logService.info(this.getClass().getName() + " Constant changed to " + spinnerInteger_Constant);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumPeriods} parameter changes. */
	protected void callbackNumPeriods() {
		logService.info(this.getClass().getName() + " Number of periods changed to " + spinnerInteger_NumPeriods);
	}
	
	/** Executed whenever the {@link #spinFloat_ParamA} parameter changes. */
	protected void callbackParamA() {
		spinnerFloat_ParamA = Precision.round(spinnerFloat_ParamA, 2);
		logService.info(this.getClass().getName() + " Parameter a changed to " + spinnerFloat_ParamA);
	}
	
	/** Executed whenever the {@link #spinFloat_ParamB} parameter changes. */
	protected void callbackParamB() {
		spinnerFloat_ParamB = Precision.round(spinnerFloat_ParamB, 2);
		logService.info(this.getClass().getName() + " Parameter b changed to " + spinnerFloat_ParamB);
	}
	
	/** Executed whenever the {@link #spinFloat_Hurst} parameter changes. */
	protected void callbackHurst() {
		spinnerFloat_Hurst = Precision.round(spinnerFloat_Hurst, 2);
		logService.info(this.getClass().getName() + " Hurst coefficient changed to " + spinnerFloat_Hurst);
	}
	
	/** Executed whenever the {@link #spinFloat_FractalDimWM} parameter changes. */
	protected void callbackFractalDimWM() {
		spinnerFloat_FractalDimWM = Precision.round(spinnerFloat_FractalDimWM, 7);
		logService.info(this.getClass().getName() + " W-M Fractal dimension changed to " + spinnerFloat_FractalDimWM);
	}
	
	/** Executed whenever the {@link #spinFloat_FractalDimCantor} parameter changes. */
	protected void callbackFractalDimCantor() {
		spinnerFloat_FractalDimCantor= Precision.round(spinnerFloat_FractalDimCantor, 7);
		logService.info(this.getClass().getName() + " Cantor Fractal dimension changed to " + spinnerFloat_FractalDimCantor);
	}
	
	/** Executed whenever the {@link #booleanDoNotShowSignals()} parameter changes. */
	protected void callbackDoNotShowSignals() {
		logService.info(this.getClass().getName() + " Do not schow signal(s) changed to " + booleanDoNotShowSignals);
	}
	
	
	/**
	 * Executed whenever the {@link #buttonProcess} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcess() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    startWorkflow();
	    	   	uiService.show("Sequence(s)", defaultGenericTable);
	        }
	    });
	   	exec.shutdown(); //No new tasks
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
 		logService.info(this.getClass().getName() + " ComsystanJ plugin canceled");
 	}

 	/** 
	 * The run method executes the command via a SciJava thread
	 * by pressing the OK button in the UI or
	 * by CommandService.run(Command.class, false, parameters) in a script  
	 *  
	 * The @Parameter ItemIO.INPUT  is automatically harvested 
	 * The @Parameter ItemIO.OUTPUT is automatically shown 
	 * 
	 * A thread is not necessary in this method and should be avoided
	 * Nevertheless a thread may be used to get a reference for canceling
	 * But then the @Parameter ItemIO.OUTPUT would not be automatically shown and
	 * CommandService.run(Command.class, false, parameters) in a script  would not properly work
	 *
	 * An InteractiveCommand (Non blocking dialog) has no automatic OK button and would call this method twice during start up
	 */
	@Override //Interface CommandService
	public void run() {
		logService.info(this.getClass().getName() + " Run");
// 			if (ij != null) { //might be null in Fiji
// 				if (ij.ui().isHeadless()) {
// 				}
// 			}
		if (this.getClass().getName().contains("Command")) { //Processing only if class is a Csaj***Command.class
			startWorkflow();
		}
	}

	/**
	 * This method starts the workflow
	 */
	protected void startWorkflow() {
		   		
		long startTimeAll = System.currentTimeMillis();
	
		//dlgProgress = new Dialog_WaitingWithProgressBar("<html>Sequence generation, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new Dialog_WaitingWithProgressBar("Sequence generation, please wait... Open console window for further info.",
		                                                                             logService, false, null); //isCanceable = false, because no following method listens to exec.shutdown 
	
		//dlgProgress.updatePercent("0%");
		//dlgProgress.setBarIndeterminate(true);
		//dlgProgress.updateBar(0);
		dlgProgress.setVisible(true);
	
		//"Constant", "Sine", "Square", "Triangle", "SawTooth", "Gaussian", "Uniform", "Logistic", "Henon", "Cubic", "Spence", "fGn", "fBm"
		//generate this.defaultGenericTable = new DefaultGenericTable();
		if      (choiceRadioButt_Method.equals("Constant")) computeConstantSequences();
		else if (choiceRadioButt_Method.equals("Sine"))     computeSineSequences();
		else if (choiceRadioButt_Method.equals("Square"))   computeSquareSequences();
		else if (choiceRadioButt_Method.equals("Triangle")) computeTriangleSequences();
		else if (choiceRadioButt_Method.equals("SawTooth")) computeSawToothSequences();
		else if (choiceRadioButt_Method.equals("Gaussian")) computeGaussianSequences();
		else if (choiceRadioButt_Method.equals("Uniform"))  computeUniformSequences();
		else if (choiceRadioButt_Method.equals("Logistic")) computeLogisticSequences();
		else if (choiceRadioButt_Method.equals("Lorenz"))   computeLorenzSequences();
		else if (choiceRadioButt_Method.equals("Henon"))    computeHenonSequences();
		else if (choiceRadioButt_Method.equals("Cubic"))    computeCubicSequences();
		else if (choiceRadioButt_Method.equals("Spence"))   computeSpenceSequences();
		else if (choiceRadioButt_Method.equals("fGn"))      computefGnSequences();
		else if (choiceRadioButt_Method.equals("fBm"))      computefBmSequences();
		else if (choiceRadioButt_Method.equals("W-M"))      computeWMSequences();
		else if (choiceRadioButt_Method.equals("Cantor"))   computeCantorSequences();
	
		int percent = (100);
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(100, 100, "Collecting data for table...");
		logService.info(this.getClass().getName() + " Collecting data for table...");	
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
	
		//show table----------------------------------------------------------
		//uiService.show("Sequence(s)", defaultGenericTable);
		//--------------------------------------------------------------------
		
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		//*****************************************************************
	    //int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the sequences?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
	 
		if (!booleanDoNotShowSignals) {   		
			int[] cols;
			boolean isLineVisible = true;
			String sequenceTitle = choiceRadioButt_Method + " sequence(s)";
			String xLabel = "#";
			String yLabel = "Value";
			String[] seriesLabels;
			
			//EXCEPTION for Lorenz, always 3 sequences for X Y Z
			if ((choiceRadioButt_Method.equals("Lorenz"))  ) {
				cols = new int[3];
				seriesLabels = new String[3];
				for (int c = 0; c < 3; c++) {
					cols[c] = c;
					seriesLabels[c] = defaultGenericTable.getColumnHeader(c);				
				}
			}
			else { //for all others
				cols         = new int[spinnerInteger_NumSequences];
				seriesLabels = new String[spinnerInteger_NumSequences];
				for (int c = 0; c < spinnerInteger_NumSequences; c++) {
					cols[c] = c;
					seriesLabels[c] = defaultGenericTable.getColumnHeader(c);				
				}
			};
			
		
			Plot_SequenceFrame pdf = new Plot_SequenceFrame(defaultGenericTable, cols, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel, seriesLabels);
			pdf.setVisible(true);
		}
		
			
		//This might free some memory and would be nice for a large table
		//defaultGenericTable = null;
		Runtime.getRuntime().gc();
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed total time: "+ sdf.format(duration));
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
	}

	/**
 	 */
 	private void computeConstantSequences() {
 	        
 	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
 	  	
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns
 
 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 	  	
			long startTime = System.currentTimeMillis();
			defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));	 
 	  		
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
    private void computeSineSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	double amplitude = 1.0;
    	//numDataPoints = (numPeriods * sampleRate / frequency);
    	//double factor = 2.0 * Math.PI * frequency / sampleRate;
    	//x = ((double) i / sampleRate);
    	
    	double sampleRate = 1.0 / (double) (spinnerInteger_NumDataPoints - 1);  //Assumption: The whole sequence lasts 1 sec
    	double frequency = ((double)spinnerInteger_NumPeriods * sampleRate)/((double)(spinnerInteger_NumDataPoints - 1)); //-1 damit sichs genau wieder auf 0 ausgeht
    	
		double factor = 2.0 * Math.PI * frequency / sampleRate;
	  	
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
	 	  
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
    private void computeSquareSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	double amplitude = 1.0;
    	//numDataPoints = (numPeriods * sampleRate / frequency);
    	//double factor = 2.0 * Math.PI * frequency / sampleRate;
    	//x = ((double) i / sampleRate);
    	
    	double sampleRate = 1.0 / (double) (spinnerInteger_NumDataPoints - 1);  //Assumption: The whole sequence lasts 1 sec
    	double frequency = ((double)spinnerInteger_NumPeriods * sampleRate)/((double)(spinnerInteger_NumDataPoints - 1)); //-1 damit sichs genau wieder auf 0 ausgeht
    	
		double factor = 2.0 * Math.PI * frequency / sampleRate;
	  	
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
	 	  
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
    private void computeTriangleSequences() {
   	
	   	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
	   	double amplitude = 1.0;
	   	//numDataPoints = (numPeriods * sampleRate / frequency);
	   	//double factor = 2.0 * Math.PI * frequency / sampleRate;
	   	//x = ((double) i / sampleRate);
	   	
	   	double sampleRate = 1.0 / (double) (spinnerInteger_NumDataPoints - 1);  //Assumption: The whole sequence lasts 1 sec
	   	double frequency = ((double)spinnerInteger_NumPeriods * sampleRate)/((double)(spinnerInteger_NumDataPoints - 1)); //-1 damit sichs genau wieder auf 0 ausgeht
	   	
		double factor = 2.0 * Math.PI * frequency / sampleRate;
	  	
	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
	  		
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
   private void computeSawToothSequences() {
   	
   	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
   	double amplitude = 1.0;
   	//numDataPoints = (numPeriods * sampleRate / frequency);
   	//double factor = 2.0 * Math.PI * frequency / sampleRate;
   	//x = ((double) i / sampleRate);
   	
   	double sampleRate = 1.0 / (double) (spinnerInteger_NumDataPoints - 1);  //Assumption: The whole sequence lasts 1 sec
   	double frequency = ((double)spinnerInteger_NumPeriods * sampleRate)/((double)(spinnerInteger_NumDataPoints - 1)); //-1 damit sichs genau wieder auf 0 ausgeht
   	
		//double factor = 2.0 * Math.PI * frequency / sampleRate;
	  	
	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
	  		
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
    private void computeGaussianSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double SD = 1.0;
    	double mean = 0.0;
    
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
 	  		
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
    private void computeUniformSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
 	  		
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
     * using generalised sample entropy and surrogate data analysis. Chaos 22, 2012
     * 
     * This method computes a logistic sequence
     * 
     */
    private void computeLogisticSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    	
    	double valueX;
    
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
 	  		
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
     * This method computes a Lorenz attractor 
     */
    private void computeLorenzSequences() {
    	
    	//defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
      	//EXCEPTION
    	//Always 3 sequences for X Y Z
    	defaultGenericTable = new DefaultGenericTable(3, spinnerInteger_NumDataPoints);
    
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    
    	double amplitude = 1.0;
    	double sigma=10f;
    	double beta=8f/3f;
    	double rho=28f;
    	double dt=0.01f;
   
    	//double[] sequenceX;
    	//double[] sequenceY;
    	//double[] sequenceZ;
    	
    	double valueX; 
    	double valueY; 
    	double valueZ;
    	
    	double dX; //Incremental change
    	double dY;
    	double dZ;
    
    	//EXCEPTION
    	//always three columns at once for X Y Z 
 	  	//for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(0, "SequenceX");
 	  		defaultGenericTable.setColumnHeader(1, "SequenceY");
 	  		defaultGenericTable.setColumnHeader(2, "SequenceZ");
 	  		
 	  		//Starting point 
	  		valueX = 1;
	  		valueY = 1;
	  		valueZ = 1; 		 	  	
 	  		defaultGenericTable.set(0, 0, valueX);
 	  		defaultGenericTable.set(1, 0, valueY);
 	  		defaultGenericTable.set(2, 0, valueZ);
	 	 
 	  		for (int r = 1; r < spinnerInteger_NumDataPoints; r++) {
 	  			
 	  			
 	  			int percent = (int)Math.round((  ((float)r)/((float)spinnerInteger_NumDataPoints) * 100.f ));
 				dlgProgress.updatePercent(String.valueOf(percent+"%"));
 				dlgProgress.updateBar(percent);
 				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
 				statusService.showStatus((r), (int)spinnerInteger_NumDataPoints, "Processing " + (r+1) + "/" + (int)spinnerInteger_NumDataPoints);
 				logService.info(this.getClass().getName() + " Processing " + (r+1) + "/" + spinnerInteger_NumDataPoints);
 	  			
 	  			
	 	  	    dX=(sigma*(valueY-valueX))*dt;
	 	  	    dY=((valueX)*(rho-valueZ)-valueY)*dt;
	 	  	    dZ=(valueX*valueY-beta*valueZ)*dt;
	 	  		valueX += dX;
	 	  		valueY += dY;
	 	  		valueZ += dZ;
	 	  		defaultGenericTable.set(0, r, valueX);  
	 	  		defaultGenericTable.set(1, r, valueY);  
	 	  		defaultGenericTable.set(2, r, valueZ);  
	 	  	}
	 	  	
//	 	  	if (amplitude != 1.0) {
//		 		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { 
//		 	  		defaultGenericTable.set(c, r, amplitude*(double)defaultGenericTable.get(c, r));
//		 	  	}
//	 	  	}
	 	  	
	 	  	//sequenceX = null;
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	//}     
    }
    
    /**
     * <b>DCM discrete chaotic map</b><br>
     * According to Silva & Murta Jr Evaluation of physiologic complexity in time series
     * using generalised sample entropy and surrogate data analysis. Chaos 22, 2012
     * 
     * This method computes a Henon sequence
     * 
     */
    private void computeHenonSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    	//double[] sequenceX; //X coordinate of  Henon map
    	
    	double valueXOld; 
    	double valueXNew; 
    	double valueY;
    
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
 	  		
	  		//computation
 	  		//set first data point randomly
 			//range[c][0] = (double)0.0d + 1.0d;
 	  		//sequenceY = new double[spinnerInteger_NumDataPoints];
 	  		//sequenceY[0] = 0; //a random sequenceX starting point can be outside the basin of attraction, so the maps can drift away to -infinity // = amplitude * (generator.nextDouble());
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
	 	  		//sequenceX[r] = amplitude*(spinnerFloat_ParamB * (double)defaultGenericTable.get(c, r - 1));
	 	  		//range[c][r] = ((double)r + 1.0d);
	 	  	}
	 	  	
	 	  	if (amplitude != 1.0) {
		 		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { 
		 	  		defaultGenericTable.set(c, r, amplitude*(double)defaultGenericTable.get(c, r));
		 	  	}
	 	  	}
	 	  	
	 	  	//sequenceX = null;
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
     * using generalised sample entropy and surrogate data analysis. Chaos 22, 2012
     * 
     * This method computes a cubic sequence
     * 
     */
    private void computeCubicSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    	double valueX;
    
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
	  		
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
     * using generalised sample entropy and surrogate data analysis. Chaos 22, 2012
     * 
     * This method computes a Spence sequence
     * 
     */
    private void computeSpenceSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	Random generator = new Random();
    	generator.setSeed(System.currentTimeMillis());
    	double amplitude = 1.0;
    	double valueX;
    
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
	  		
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
     * Generates fGn sequences using Davies and Harte autocorrelation method DHM 
     * Davies, R.B., Harte, D.: Tests for Hurst effect. Biometrika 74(1), 95–101 (1987). https://doi.org/10.1093/biomet/74.1.95
     */
    
    private void computefGnSequences() {
    	
    	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
    	double amplitude = 1.0;
    	
    	int M = spinnerInteger_NumDataPoints * 2;
		double[]   sequenceAC;
		double[]   sequenceS;
		double[][] sequenceV;  //complex  [0][*] Real   [1][*] Imag
		double[]   sequence = null;

    	
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns
 	  
 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
 	  			
 			// generate auto covariance sequence
 			sequenceAC = generateAC(M, spinnerFloat_Hurst);	
 			sequenceS  = calcDFTofAC_Equ6a(sequenceAC); // Imaginary sequence strictly not needed, it is always 0
 		
 			boolean allDataPointsAreNotNegative = checkIfDataPointsAreNotNegative(sequenceS);
 			if (allDataPointsAreNotNegative) {
 				sequenceV = calcRSA_Equ6be(sequenceS);
 					
 				sequence  = calcDFTofRSA_Equ6f(sequenceV);

 				// take another sequence to show or so
 				// sequence = sequenceAC;
 			} else {
 				// output nothing
 			}

 			// take another sequence
 			// sequence = sequenceS;
 			// sequence = new double[];
 			// for (int i = 0; i < sequenceV.length; i++){
 			// sequence[i] = sequenceV[0][i];
 			// }

// 			// set x-axis data points
// 			for (int i = 0; i < sequence.length; i++) {
// 				range.add((double) i + 1.0d);
// 			}		
// 			if (fGnTofBm == 1) {
// 				sequence = this.convert_fGnTofBm(sequence);
// 			}
	 	  	for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, amplitude * sequence[r]);
	 	  	}
	 	  
	 	  	long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * Generates fBm sequences using spectral synthesis method SSM
     */
    private void computefBmSequences() { 
    	
	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
	    double amplitude = 1.0;	
	  	
	  	int M = (2 * spinnerInteger_NumDataPoints) * 4; // 2* because of DFT halves the number, *2 or *4 is not so memory intensive than *8 as suggested by Caccia et al 1997
		double beta = 2.0d * spinnerFloat_Hurst + 1.0d;
		double[] sequenceReal;
		double[] sequenceImag;
		double[] sequencePhase;
		double[] sequencePower;
		double[] sequence;
		Random generator = new Random();
		generator.setSeed(System.currentTimeMillis());
		
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns
 	  	  	
 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
 	  		
 	  		sequencePhase = new double[M];
 			sequencePower = new double[M];
 			sequenceReal  = new double[M/2];
 	  		
 	  		// generate random phase variables [0, 2pi]
			generator = new Random();
			generator.setSeed(System.currentTimeMillis());
			for (int i = 0; i < M; i++) {
				sequencePhase[i] = generator.nextDouble() * 2.0d * Math.PI;// -1.0d*Math.PI);
			}
		
			// set 1/f beta power spectrum
			for (int i = 0; i < M; i++) {
				sequencePower[i] = 1.0d / Math.pow(i + 1, beta);
			}
	
			double sumReal = 0;
			double sumImag = 0;
			// calculate invers FFT
			int N = sequencePower.length;
			for (int k = 0; k < N / 2; k++) { // output points
				sumReal = 0;
				sumImag = 0;
				for (int n = 0; n < N; n++) { // input points
					double cos = Math.cos(2 * Math.PI * n * k / N);
					double sin = Math.sin(2 * Math.PI * n * k / N);
					double real = Math.sqrt(sequencePower[n]) * Math.cos(sequencePhase[n]);
					double imag = Math.sqrt(sequencePower[n]) * Math.sin(sequencePhase[n]);
					sumReal += real * cos + imag * sin;
					// sumImag += -real * sin + imag * cos;
				}
				sequenceReal[k] = sumReal;
				// sequenceImag[k] = sumImag;
			}
			sequence = sequenceReal;
			
			// eliminate data points which are too much
			if (sequence.length > spinnerInteger_NumDataPoints) {
				for (int i = 0; i < (sequence.length - spinnerInteger_NumDataPoints) / 2; i++) {
					sequence[i] = Double.NaN;
				}
				for (int i = (sequence.length - spinnerInteger_NumDataPoints) / 2 + spinnerInteger_NumDataPoints; i < sequence.length; i++) {
					sequence[i] = Double.NaN;
				}
				sequence = removeNaN(sequence);
			}
				
//			// set x-axis data points
//			for (int i = 0; i < sequence.size(); i++) {
//				range[i] = ((double) i + 1.0d);
//			}			
//			if (fBmTofGn == 1) {
//				sequence = this.convert_fBmTofGn(sequence);
//			}
 	  		  		
	 	  	for (int r = 0; r < spinnerInteger_NumDataPoints; r++) { //rows
	 	  		defaultGenericTable.set(c, r, amplitude * sequence[r]);
	 	  	}
	 		
	 	 	
	 		long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
 	  	}     
    }
    
    /**
     * Generates Weierstrass-Mandelbrot sequences
     */
    private void computeWMSequences() { 
    	
	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
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
    	
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
 	  		
 	  		
 	  		//computation
 	  		//********HA: arbitrary starting point, otherwise all sequences would be identical******
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
     * Generates BINARY Cantor set sequences
     * FD to gamma relation according to 
     * Cherny, A. Y., E. M. Anitas, A. I. Kuklin, M. Balasoiu, und V. A. Osipov. „Scattering from Generalised Cantor Fractals“. Journal of Applied Crystallography 43, Nr. 4 (1. August 2010): 790–97. https://doi.org/10.1107/S0021889810014184.
     *
     */
    private void computeCantorSequences() { 
    	
	  	defaultGenericTable = new DefaultGenericTable(spinnerInteger_NumSequences, spinnerInteger_NumDataPoints);
	    double amplitude = 1.0;	
    	
    	//double[] sequence;
	    
	    float fracDim = spinnerFloat_FractalDimCantor;
	    
	    //DEBUG: Overwrite fracDim with standard value 
	    //fracDim = (float)(Math.log(2f)/Math.log(3f)); //0.6309..... 1/3
	    
	    float gamma = -(2f*(float)Math.exp(-Math.log(2f)/fracDim) - 1f); //gamma = 1/3 for standard FD  0.6309..
    	
 	  	for (int c = 0; c < spinnerInteger_NumSequences; c++) {    //columns

 	  		int percent = (int)Math.round((  ((float)c)/((float)spinnerInteger_NumSequences) * 100.f ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((c), (int)spinnerInteger_NumSequences, "Processing " + (c+1) + "/" + (int)spinnerInteger_NumSequences);
			logService.info(this.getClass().getName() + " Processing " + (c+1) + "/" + spinnerInteger_NumSequences);
 
			long startTime = System.currentTimeMillis();
 	  		defaultGenericTable.setColumnHeader(c, "Sequence_" + (String.format("%03d",(c +1))));
 	  		 		
 	  		//computation	
 	  		sequenceCantor = new double[spinnerInteger_NumDataPoints];
 	  		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) sequenceCantor[r] = amplitude;	
 	  		deleteSegment(gamma, spinnerInteger_NumDataPoints, 0);	
 	  		for (int r = 0; r < spinnerInteger_NumDataPoints; r++) defaultGenericTable.set(c, r, sequenceCantor[r]);
	 	  		  	
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
      
        for (int i = start + indxStart; i < start + indxEnd; i++) sequenceCantor[i] = 0;
             
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
		double[] sequenceAC = new double[M+1];
		for (int i =0; i<sequenceAC.length; i++) sequenceAC[i] = Double.NaN;
		double sigma = 1;
		double sigma2 = sigma * sigma;
		for (int t = 0; t <= M / 2; t++) {
			sequenceAC[t] = sigma2 / 2.0d * (Math.pow(Math.abs(t + 1), 2 * hurst) - 2 * Math.pow(Math.abs(t), 2 * hurst) + Math.pow(Math.abs(t - 1), 2 * hurst));	
		}
		for (int t = M / 2 - 1; t >= 0; t--) {
			sequenceAC[(M) - t] = sequenceAC[t];
		}
		return sequenceAC;
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
	 * @return sequenceS
	 */
	private double[] calcDFTofAC_Equ6a(double[] inReal) {
		int M = inReal.length;
		double[] sequenceS = new double[M/2+1];
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
			sequenceS[j] = Precision.round(sumReal1 + sumReal2, 4);
			// sequenceS.add(sumReal1 + sumReal2 + (M/2 - j));???????????????
			// sequenceS.add(sumImag1 + sumImag2);
		}
		return sequenceS;
	}

	private boolean checkIfDataPointsAreNotNegative(double[] sequence) {
		boolean allDataPointsAreNotNegative = true;
		for (int i = 0; i < sequence.length; i++) {
			if (sequence[i] < 0) {
				logService.info(this.getClass().getName() + "  Sequence has negative values, which is not allowed!");
				allDataPointsAreNotNegative = false;
			}
		}
		return allDataPointsAreNotNegative;
	}

	private double[][] calcRSA_Equ6be(double[] sequenceS) {//Otput is complex  [0][*] Real,  [1][*] Imaginary 
	
		int M = (sequenceS.length - 1) * 2;
		
		double[][]sequenceV = new double[2][M]; // Complex
		double[]  sequenceW = new double[M]; // Gaussian random variables
		

		// generate random variables
		Random generator = new Random();
		generator.setSeed(System.currentTimeMillis());
		// RandomData generator = new RandomDataImpl();
		double stdDev = 1.0d;
		double mean   = 0.0d;
		for (int i = 0; i < M; i++) {
			sequenceW[i] = generator.nextGaussian() * stdDev + mean;
			//sequenceW[i] = generator.nextGaussian(mean, stdDev);
		}

		// calculate randomized spectral amplitudes

		// set first value Equ6b;
		sequenceV[0][0] = Math.sqrt(sequenceS[0]) * sequenceW[0];  //Real
		sequenceV[1][0] = 0.0d; //Imag
		// set next values Equ6c
		for (int k = 1; k < M / 2; k++) {
			sequenceV[0][k] = Math.sqrt(1.0d / 2.0d * sequenceS[k]) * sequenceW[2 * k - 1]; //Real
			sequenceV[1][k] = Math.sqrt(1.0d / 2.0d * sequenceS[k]) * sequenceW[2 * k];     //Imag
		}
		// set middle value Equ6d
		sequenceV[0][M/2] = Math.sqrt(sequenceS[M / 2]) * sequenceW[M - 1];
		sequenceV[0][M/2] = 0.0d;		
		// set next values Equ6e
		for (int k = M / 2 + 1; k <= (M - 1); k++) {
			//Set complex conjugate
			sequenceV[0][k] =  sequenceV[0][M-k];
			sequenceV[1][k] = -sequenceV[1][M-k];
		}
		return sequenceV;
	}

	/**
	 * This method calculates the simulated time series
	 * 
	 * @param complex sequenceV [0][*] Real, [1][*]  Imaginary
	 * @return the simulated sequence
	 */
	private double[] calcDFTofRSA_Equ6f(double[][] sequenceV) { //complex sequenceV [0][*] Real, [1][*] Imaginary
		int M = sequenceV[0].length;
		double[] sequenceY = new double[M/2];
		double sumReal = 0.0;
		double sumImag = 0.0;
		for (int k = 0; k < M / 2; k++) { // half of output points
			sumReal = 0.0;
			sumImag = 0.0;
			// double sumImag = 0;
			for (int n = 0; n < M; n++) { // input points
				double cos = Math.cos(2 * Math.PI * n * k / M);
				double sin = Math.sin(2 * Math.PI * n * k / M);
				sumReal +=  sequenceV[0][n] * cos + sequenceV[1][n] * sin;
				sumImag += -sequenceV[0][n] * sin + sequenceV[1][n] * cos; //Imag is always 0
			}
			sequenceY[k] = (1.0d / Math.sqrt(M) * sumReal);
		}		
		return sequenceY;
	}

	/**
	 * This method calculates the power spectrum of a sequence
	 * 
	 * @param sequence
	 * @return the power spectrum of the sequence
	 */
	private double[] calcPowerSpectrum(double[]sequence) {
		int N = sequence.length;
		double[] sequencePower = new double[N];
		double[] sequencePhase = new double[N];	
		
		for (int k = 0; k < N; k++) { // output points
			double sumReal = 0;
			double sumImag = 0;
			for (int n = 0; n < N; n++) { // input points
				double cos = Math.cos(2 * Math.PI * n * k / N);
				double sin = Math.sin(2 * Math.PI * n * k / N);
				sumReal += sequence[n]  * cos;
				sumImag += -sequence[n] * sin;
			}
			sequencePower[k] = Math.sqrt(sumReal*sumReal + sumImag * sumImag);
			sequencePhase[k] = Math.atan(sumImag/sumReal);
		}
		return sequencePower;
	}
    
 // This method removes NaN  from field sequence1D
 	private double[] removeNaN(double[] sequence) {
 		int lengthOld = sequence.length;
 		int lengthNew = 0;
 		
 		for (int i = 0; i < lengthOld; i++) {
 			if (!Double.isNaN(sequence[i])) {
 				lengthNew += 1;
 			}
 		}
 		double[] sequence1D = new double[lengthNew];
 		int ii = -1;
 		for (int i = 0; i < lengthOld; i++) {
 			if (!Double.isNaN(sequence[i])) {
 				ii +=  1;
 				sequence1D[ii] = sequence[i];
 			}
 		}
 		return sequence1D;
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
//            ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
//        }
//       
         //invoke the plugin
         ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
    	
    }

}
