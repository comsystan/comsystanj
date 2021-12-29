/*-
 * #%L
 * Project: ImageJ2 plugin to detect QRS peaks and RR intervals.
 * File: DetectQRSPeaks.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2021 Comsystan Software
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
package at.csa.csaj.sig.detect.qrspeaks;

import net.imagej.ImageJ;

import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;

import org.scijava.command.Previewable;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.sig.detect.qrspeaks.util.QRSPeaksDetector;
import at.csa.csaj.sig.detect.qrspeaks.util.QRSPeaksDetectorFileOpenDialog;

import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.UIManager;
/**
 * This is an ImageJ {@link Command} plugin to detect QRS complexes of a ecg file (Holter chan.raw file).
 * <p>
 * The code here is using
 * </p>
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "QRS peaks detection (from file)",
	//iconPath = "/images/comsystan-??.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Signal"),
	@Menu(label = "QRS peaks detection (from file)", weight = 12)})
public class DetectQRSPeaks  extends ContextCommand implements Previewable { //modal GUI with cancel
	
	private static final String PLUGIN_LABEL = "<html><b>Detects QRS complexes and RR intervals</b></html>";
	private static final String SPACE_LABEL = "";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	//@Parameter(label = "F", type = ItemIO.OUTPUT)
	//private Img<FloatType> resultImg;

	//@Parameter
	//private DatasetService datasetService;

	//@Parameter
	//private DatasetIOService datasetIOService;
	
	//@Parameter
	//private DisplayService displayService;

    //@Parameter
    //private UIService uiService;

    //@Parameter
    //private OpService opService;
	
	private ExecutorService exec;
    
    //Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
	private final String labelPlugin = PLUGIN_LABEL;

    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
  	private final String labelSpace = SPACE_LABEL;
    
    
    @Parameter(label = "Starting offset:",
    		   style = NumberWidget.SPINNER_STYLE,
    		   min = "0",
    		   max = "32768",
    		   initializer = "initialOffset",
    		   stepSize = "1",
    		   callback = "callbackOffset")
    private int spinnerInteger_Offset;
    
    @Parameter(label = "Sample Rate [Hz]:",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       initializer = "initialSampleRate",
 		       stepSize = "1",
 		       callback = "callbackSampleRate")
    private int spinnerInteger_SampleRate;
    
    @Parameter(label = "Method",
    		   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		   choices = { "QRSDetect1", "QRSDetect2", "BeatDetectAndClassify" },
    		   initializer = "initialMethod",
               callback = "callbackMethod")
    private String choiceRadioButt_Method;
	
    @Parameter(label = "Output option",
    		   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		   choices = {"RRIntervals", "QRSPeaksCoordinates" },
    		   initializer = "initialOutputOption",
               callback = "callbackOutputOption")
    private String choiceRadioButt_OutputOption;
    
    @Parameter(label = "Save file", persist = true,
		       callback = "callbackSaveFile")
	private boolean booleanSaveFile;
    
    //---------------------------------------------------------------------
    
    //The following initializer functions set initial values	
    protected void initialOffset() {
    	spinnerInteger_Offset = 0;
    }
    protected void initialSampleRate() {
    	spinnerInteger_SampleRate = 180;  // 180 Syncope  125 Helena
    }
    protected void initialMethod() {
    	choiceRadioButt_Method = "QRSDetect2";
    }
    protected void initialOutputOption() {
    	choiceRadioButt_OutputOption = "RRIntervals";
    }
    // ------------------------------------------------------------------------------
 	
 	/** Executed whenever the {@link #spinnerInteger_Dim} parameter changes. */
 	protected void callbackOffset() {
 		logService.info(this.getClass().getName() + " Offset changed to " + spinnerInteger_Offset);
 	}
 	protected void callbackSampleRate() {
 		logService.info(this.getClass().getName() + " Sample rate changed to " + spinnerInteger_SampleRate + " [Hz].");
 	}
    
	/** Executed whenever the {@link #choiceRadioButt_Method} parameter changes. */
	protected void callbackMethod() {
		logService.info(this.getClass().getName() + " Method changed to " + choiceRadioButt_Method + ".");
	}
	
	/** Executed whenever the {@link #choiceRadioButt_Method} parameter changes. */
	protected void callbackOutputOption() {
		logService.info(this.getClass().getName() + " Method changed to " + choiceRadioButt_OutputOption + ".");
	}
	
	/** Executed whenever the {@link #booleanSaveFiles} parameter changes. */
	protected void callbackSaveFiles() {
		logService.info(this.getClass().getName() + " Save file option set to " + booleanSaveFile);
	}
    
    // You can control how previews work by overriding the "preview" method.
 	// The code written in this method will be automatically executed every
 	// time a widget value changes.
 	public void preview() {
 		logService.info(this.getClass().getName() + " Preview initiated");
 		//statusService.showStatus(message);
 	}
 	
    // This is often necessary, for example, if your  "preview" method manipulates data;
	// the "cancel" method will then need to revert any changes done by the previews back to the original state.
	public void cancel() {
		logService.info(this.getClass().getName() + " Widget canceled");
	}
	
    //-------------------------------------------------------------------------------------------
	/**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    

    @Override
    public void run() {
    	
    	long startTime = System.currentTimeMillis();
         // create the ImageJ application context with all available services
    	//final ImageJ ij = new ImageJ();
    	//ij.ui().showUI();



		//Collect parameters
    	QRSPeaksDetectorFileOpenDialog dialog = new QRSPeaksDetectorFileOpenDialog();
		dialog.run();
		File[] files = dialog.getFiles();
	
		int        offSet   = spinnerInteger_Offset;
		int    sampleRate   = spinnerInteger_SampleRate;
		String oseaMethod   = choiceRadioButt_Method;
		String outputOption = this.choiceRadioButt_OutputOption;
		boolean saveFile    = this.booleanSaveFile;
	
		exec = Executors.newSingleThreadExecutor();
		
		//QRSPeaksDetector task = new QRSPeaksDetector(logService, statusService, files, offSet, sampleRate, oseaMethod, outputOption, saveFile); 
		//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Detecting QRS peaks and RR intervals, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Detecting QRS peaks and RR intervals, please wait... Open console window for further info.",
																					logService, false, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown );
		//dlgProgress.updatePercent("");
		//dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
		//task.addPropertyChangeListener(new CompletionWaiter(dlgProgress));	
//		task.run();
//	
//		
//		if (task.isDone()){
//			
//			dlgProgress.setVisible(false);
//			dlgProgress.dispose();
//			
//			long duration = System.currentTimeMillis() - startTime;
//			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
//			SimpleDateFormat sdf = new SimpleDateFormat();
//			sdf.applyPattern("HHH:mm:ss:SSS");
//			logService.info(this.getClass().getName() + " Elapsed total time: "+ sdf.format(duration));
//		}
		
     	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	logService.info(this.getClass().getName() + " Sarted detection of QRS peaks");
            	
        			QRSPeaksDetector task = new QRSPeaksDetector(dlgProgress, logService, statusService, 
        					                files, offSet, sampleRate, oseaMethod, outputOption, saveFile);
        			task.run();
        	    	        	
            		if (task.isDone()){
            			dlgProgress.addMessage("Processing finished! Collecting data for table...");
            			dlgProgress.setVisible(false);
            			dlgProgress.dispose();
            			
            			long duration = System.currentTimeMillis() - startTime;
            			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            			SimpleDateFormat sdf = new SimpleDateFormat();
            			sdf.applyPattern("HHH:mm:ss:SSS");
            			logService.info(this.getClass().getName() + " Elapsed total time: "+ sdf.format(duration));
            			Toolkit.getDefaultToolkit().beep();
            		}
            		
                } finally {
                	exec.shutdown();
                }		
            }
        });	
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
         ij.command().run(DetectQRSPeaks.class, true);
    	
    }

}
