/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DTo1DScanCmd.java
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
package at.csa.csaj.plugin2d.preproc;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.UIManager;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.Position;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.display.DefaultDisplayService;
import org.scijava.display.Display;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajAlgorithm_HilbertScan;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;


/**
 * A {@link ContextCommand} plugin computing <a 3D to 1D scan </a>
 * of an image.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		label = "2D to 1D scan",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {})

public class Csaj2DTo1DScanCmd<T extends RealType<T>> extends ContextCommand implements Previewable {
	
	private static final String PLUGIN_LABEL            = "<html><b>2D to 1D scan</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String SCANTYPE_LABEL          = "<html><b>Scan type</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	 
	private static Dataset dataset;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
		
	public static final String TABLE_OUT_NAME = "Table - 2D to 1D scan";
	
    private CsajDialog_WaitingWithProgressBar dlgProgress;
    private ExecutorService exec;
    
	@Parameter
	private ImageJ ij;
	
	@Parameter
	private PrefService prefService;

	@Parameter
	private LogService logService;
	
	@Parameter
	private StatusService statusService;

	@Parameter
	private OpService opService;
	
	@Parameter
	private UIService uiService;
	
	@Parameter
	private ImageDisplayService imageDisplayService;
	
	//This parameter does not work in an InteractiveCommand plugin (duplicate displayService error during startup) pom-scijava 24.0.0
	//in Command Plugin no problem
	//@Parameter  
	//private DisplayService displayService;
	
	@Parameter  //This works in an InteractiveCommand plugin
    private DefaultDisplayService defaultDisplayService;
	
	@Parameter
	private DatasetService datasetService;
	
	@Parameter
	private IOService ioService;
	
  	@Parameter (type = ItemIO.INPUT)
  	private Dataset datasetIn;
		
  	@Parameter(label = TABLE_OUT_NAME, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;

   //Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
  	//private final String labelSpace = SPACE_LABEL;
    
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
  	private final String labelScanType = SCANTYPE_LABEL;

    @Parameter(label = "Scan type",
		    description = "Type of 1D scanning",
		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
  		    choices = {"Hilbert", "Row meander", "Column meander", "Random"},
  		    persist = true,  //restore previous value default = true
		    initializer = "initialScanType",
            callback = "callbackScanType")
    private String choiceRadioButt_ScanType;
    
    //-----------------------------------------------------------------------------------------------------
  	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
  	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;
  	
    
    //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
    
    @Parameter(label = "Overwrite result display(s)",
        	   description = "Overwrite already existing result images, plots or tables",
        	   persist = true,  //restore previous value default = true
    		   initializer = "initialOverwriteDisplays")
    private boolean booleanOverwriteDisplays;
      
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
    private final String labelProcessOptions = PROCESSOPTIONS_LABEL;
     
    @Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
               description = "Immediate processing of active image when a parameter is changed",
    		   callback = "callbackProcessImmediately")
    private boolean booleanProcessImmediately;
     
	@Parameter(label = "OK - process image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumImageSlice",
			   callback = "callbackNumImageSlice")
	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "OK - process all images",
			   description = "Set for final Command.run execution",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialProcessAll")
	private boolean processAll;
	
	@Parameter(label = "   Preview of single image #    ", callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label = "Preview of single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
     
    @Parameter(label = "Preview of all available images", callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;

    //---------------------------------------------------------------------
 
    //The following initializer functions set initial values	
    protected void initialPluginLaunch() {
    	checkItemIOIn();
	}
    
    protected void initialScanType() {
    	choiceRadioButt_ScanType = "Hilbert";
    } 
 
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
    
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
  
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #choiceRadioButt_ScanType} parameter changes. */
	protected void callbackScanType() {
		logService.info(this.getClass().getName() + " Scan type set to " + choiceRadioButt_ScanType);	
	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumImageSlice} parameter changes. */
	protected void callbackNumImageSlice() {
		if (spinnerInteger_NumImageSlice > numSlices){
			logService.info(this.getClass().getName() + " No more images available");
			spinnerInteger_NumImageSlice = (int)numSlices;
		}
		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessSingleImage} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessSingleImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    startWorkflowForSingleImage();
	    	   	uiService.show(TABLE_OUT_NAME, tableOut);
	        }
	    });
	   	exec.shutdown(); //No new tasks
	}
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed.*/
	protected void callbackProcessActiveImage() {
	
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessAllImages} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessAllImages() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	        	startWorkflowForAllImages();
	    	   	uiService.show(TABLE_OUT_NAME, tableOut);
	        }
	    });
	   	exec.shutdown(); //No new tasks
	}

	/**
	 * Executed automatically every time a widget value changes.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	@Override //Interface Previewable
	public void preview() { 
	 	logService.info(this.getClass().getName() + " Preview initiated");
	 	if (booleanProcessImmediately) {
			exec = Executors.newSingleThreadExecutor();
		   	exec.execute(new Runnable() {
		        public void run() {
		    	    startWorkflowForSingleImage();
		    	   	uiService.show(TABLE_OUT_NAME, tableOut);   //Show table because it did not go over the run() method
		        }
		    });
		   	exec.shutdown(); //No new tasks
	 	}	
	}

	/**
	 * This is necessary if the "preview" method manipulates data
	 * the "cancel" method will then need to revert any changes back to the original state.
	 */
	@Override //Interface Previewable
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
		logService.info(this.getClass().getName() + " Starting command run");
		
		checkItemIOIn();
		if (processAll) startWorkflowForAllImages();
		else            startWorkflowForSingleImage();
	
		logService.info(this.getClass().getName() + " Finished command run");
	}
	
	public void checkItemIOIn() {
		//Get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkDatasetIn(logService, datasetIn);
		if (datasetInInfo == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Missing input image or image type is not byte or float");
			cancel("ComsystanJ 2D plugin cannot be started - missing input image or wrong image type.");
		} else {
			width  =       			(long)datasetInInfo.get("width");
			height =       			(long)datasetInInfo.get("height");
			numDimensions =         (int)datasetInInfo.get("numDimensions");
			compositeChannelCount = (int)datasetInInfo.get("compositeChannelCount");
			numSlices =             (long)datasetInInfo.get("numSlices");
			imageType =   			(String)datasetInInfo.get("imageType");
			datasetName = 			(String)datasetInInfo.get("datasetName");
			sliceLabels = 			(String[])datasetInInfo.get("sliceLabels");
			
			//RGB not allowed
			if (!imageType.equals("Grey")) { 
				logService.error(this.getClass().getName() + " ERROR: Grey value image(s) expected!");
				cancel("ComsystanJ 2D plugin cannot be started - grey value image(s) expected!");
			}
		}
	}
	
	/**
	 * This method starts the workflow for a single image of the active display
	 */
	protected void startWorkflowForSingleImage() {
			
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing 2D to 1D scan, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
		deleteExistingDisplays();
		generateSingleColumnTableHeader();
		int sliceIndex = spinnerInteger_NumImageSlice - 1;
		logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));	
		dlgProgress.setVisible(true);		
		processSingleInputImage(sliceIndex);
	
		dlgProgress.addMessage("Processing finished! Collecting data for table...");		
	    dlgProgress.setVisible(false);
	    dlgProgress.dispose();	
		Toolkit.getDefaultToolkit().beep();     
	}

	/**
	 * This method starts the workflow for all images of the active display
	 */
	protected void startWorkflowForAllImages() {
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing 2D to 1D scans, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);
	
		logService.info(this.getClass().getName() + " Processing all available images");
	    deleteExistingDisplays();
	    generateMultiColumnTableHeader();
		processAllInputImages();
		
		dlgProgress.addMessage("Processing finished! Collecting data for table...");			
		dlgProgress.setVisible(false);
		dlgProgress.dispose();	
	    Toolkit.getDefaultToolkit().beep();      
	}

	/**
	 * This methods gets the index of the active image in a stack
	 * @return int index
	 */
	private int getActiveImageIndex() {
		int activeSliceIndex = 0;
		try {
			//This works in eclipse but not as jar in the plugin folder of fiji 
			//SCIFIO activated: throws a NullPointerException
			//SCIFIO deactivated: gives always back index = 0! 
			Position pos = imageDisplayService.getActivePosition();
			activeSliceIndex = (int) pos.getIndex();
			
			//This gives always back 0, SCIFIO setting does not matter
			//int activeSliceNumber = (int) imageDisplayService.getActiveImageDisplay().getActiveView().getPlanePosition().getIndex();
			//???
			//int activeSliceNumber = (int) defaultImageDisplayService.getActivePosition().getIndex(); 
			//int activeSliceNumber2 = (int) defaultImageDisplayService.getActiveImageDisplay().getActiveView().getPlanePosition().getIndex();
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to get active slice index. Index set to first image.");
			activeSliceIndex = 0;
		} 
		logService.info(this.getClass().getName() + " Active slice index = " + activeSliceIndex);
		//logService.info(this.getClass().getName() + " Active slice index alternative = " + activeSliceNumber2);
		return activeSliceIndex;
	}
	
	/** This method deletes already open displays*/
	private void deleteExistingDisplays() {
		
		boolean optDeleteExistingImgs   = false;
		boolean optDeleteExistingPlots  = false;
		boolean optDeleteExistingTables = false;
		if (booleanOverwriteDisplays) {
			optDeleteExistingImgs   = true;
			optDeleteExistingPlots  = true;
			optDeleteExistingTables = true;
		}
		
		if (optDeleteExistingImgs) {
//			//List<Display<?>> list = defaultDisplayService.getDisplays();
//			//for (int i = 0; i < list.size(); i++) {
//			//	display = list.get(i);
//			//	System.out.println("display name: " + display.getName());
//			//	if (display.getName().contains("Name")) display.close(); //does not close correctly in Fiji, it is only not available any more
//			//}			
//			//List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Is also not closed in Fiji 
//		
//			Frame frame;
//			Frame[] listFrames = JFrame.getFrames();
//			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
//				frame = listFrames[i];
//				//System.out.println("frame name: " + frame.getTitle());
//				if (frame.getTitle().contains("Name")) {
//					frame.setVisible(false); //Successfully closes also in Fiji
//					frame.dispose();
//				}
//			}
		}
		if (optDeleteExistingTables) {
			Display<?> display;
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().contains(TABLE_OUT_NAME)) display.close();
			}			
		}
	}
	
	
	/** This method takes the active image and computes results. 
	 *
	 **/
	private void processSingleInputImage(int s) {
		long startTime = System.currentTimeMillis();

		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//mg<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		RandomAccessibleInterval<T> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai = (RandomAccessibleInterval<T>)datasetIn.copy().getImgPlus();

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<T>)Views.hyperSlice(datasetIn.copy(), 2, s);
		}

		//Compute regression parameters
		CsajContainer_ProcessMethod containerPM = process(rai, s);	
		//0 Image size, 1 KC, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
	
		writeToSingleColumnTable(s, containerPM); //write always to the first row
		
		logService.info(this.getClass().getName() + " KC: " + containerPM.item1_Values[1]);
		
		//Set/Reset focus to DatasetIn display
		//may not work for all Fiji/ImageJ2 versions or operating systems
		Frame frame;
		Frame[] listFrames = JFrame.getFrames();
		for (int i = 0; i < listFrames.length; i++) {
			frame = listFrames[i];
			//System.out.println("frame name: " + frame.getTitle());
			if (frame.getTitle().contains(datasetIn.getName())) { //sometimes Fiji adds some characters to the frame title such as "(V)"
				frame.setVisible(true);
				frame.toFront();
				frame.requestFocus();
			}
		}
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}
	
	/** This method loops over all input images and computes results. 
	 *
	 **/
	private void processAllInputImages() {
		
		long startTimeAll = System.currentTimeMillis();
		
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//Img<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		CsajContainer_ProcessMethod containerPM;
		//loop over all slices of stack
		for (int s = 0; s < numSlices; s++){ //p...planes of an image stack
			//if (!exec.isShutdown()) {
				int percent = (int)Math.round((  ((float)s)/((float)numSlices)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numSlices, "Processing " + (s+1) + "/" + (int)numSlices);
	//			try {
	//				Thread.sleep(3000);
	//			} catch (InterruptedException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
				
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing image number " + (s+1) + "(" + numSlices + ")");
				//get slice and convert to float values
				//imgFloat = opService.convert().float32((Img<T>)dataset.gett);	
				
				RandomAccessibleInterval<T> rai = null;
				if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
					rai = (RandomAccessibleInterval<T>)datasetIn.copy().getImgPlus();
	
				} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
					rai = (RandomAccessibleInterval<T>)Views.hyperSlice(datasetIn.copy(), 2, s);
				}
				//Compute result values
				containerPM = process(rai, s);	
				
				writeToMultiColumnTable(s, containerPM);
				
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			//}
		} //s
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		//Set/Reset focus to DatasetIn display
		//may not work for all Fiji/ImageJ2 versions or operating systems
		Frame frame;
		Frame[] listFrames = JFrame.getFrames();
		for (int i = 0; i < listFrames.length; i++) {
			frame = listFrames[i];
			//System.out.println("frame name: " + frame.getTitle());
			if (frame.getTitle().contains(datasetIn.getName())) { //sometimes Fiji adds some characters to the frame title such as "(V)"
				frame.setVisible(true);
				frame.toFront();
				frame.requestFocus();
			}
		}
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all image(s): "+ sdf.format(duration));
	}
	
	/** Generates the table header {@code DefaultGenericTable} */
	private void generateSingleColumnTableHeader(){
		
	    tableOut = new DefaultGenericTable();
	    String columnHeader = "1Dscan"; //Will be adapted later
		GenericColumn column1DScan = new GenericColumn(columnHeader);
		tableOut.add(column1DScan);
		
	    int width  = (int) datasetIn.dimension(0);
	    int height = (int) datasetIn.dimension(1);
	    int numberOfSequencePoints = width * height;
	    for (int r = 0; r < numberOfSequencePoints; r++) {
	     	tableOut.appendRow();    	
	    }	
	}
	
	/** Generates the table header {@code DefaultGenericTable} */
	private void generateMultiColumnTableHeader(){
		
	    tableOut = new DefaultGenericTable();
	    
	    String columnHeader = "1Dscan"; //Will be adapted later
	    for (int s = 0; s < numSlices; s++) {
	    	GenericColumn column1DScan = new GenericColumn(columnHeader + "-" + s);
			tableOut.add(column1DScan);
	    }
	    
	    int width  = (int) datasetIn.dimension(0);
	    int height = (int) datasetIn.dimension(1);
	    int numberOfSequencePoints = width * height;
	    for (int r = 0; r < numberOfSequencePoints; r++) {
	     	tableOut.appendRow();    	
	    }
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int numRow to write in the result table
	 * @param int numSlice sclice number of images from datasetIn.
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToSingleColumnTable(int numSlice, CsajContainer_ProcessMethod containerPM) {

		String scanType = choiceRadioButt_ScanType;
		int numberOfValues = containerPM.item1_Values.length;
	    int s = numSlice;	
		    
	    String columnHeader = "1Dscan-" + scanType + "-sclice#" + (numSlice+1);
	    tableOut.setColumnHeader(0, columnHeader);;
	    
	    
	    //tableOut.setColumnHeader(numSlice, columnHeader);
		//if (sliceLabels != null) tableOut.set("Slice name", r, sliceLabels[s]);
	    
	    for (int row = 0; row < numberOfValues; row++) {
			//fill table with values
			tableOut.set(0, row, containerPM.item1_Values[row]);
	    }

	}
	

	/**
	 * collects current result and writes to table
	 * 
	 * @param int numRow to write in the result table
	 * @param int numSlice sclice number of images from datasetIn.
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToMultiColumnTable(int numSlice, CsajContainer_ProcessMethod containerPM) {

		String scanType = choiceRadioButt_ScanType;
		int numberOfValues = containerPM.item1_Values.length;
	    int s = numSlice;	
		    
	    String columnHeader = "1Dscan-" + scanType + "-sclice#" + (numSlice+1);
	    tableOut.setColumnHeader(numSlice, columnHeader);;
	    
	    
	    //tableOut.setColumnHeader(numSlice, columnHeader);
		//if (sliceLabels != null) tableOut.set("Slice name", r, sliceLabels[s]);
	    
	    for (int row = 0; row < numberOfValues; row++) {
			//fill table with values
			tableOut.set(numSlice, row, containerPM.item1_Values[row]);
	    }

	}
						
	/** 
	 * Processing 
	 * */
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<T> rai, int plane) { //plane plane (Image) number
		
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		dataset = datasetService.create(rai);
			
//		DefaultImgUtilityService dius = new DefaultImgUtilityService();
//		SCIFIOImgPlus<T> scifioImgPlus = dius.makeSCIFIOImgPlus((Img<T>) rai);
			
		String scanType = choiceRadioButt_ScanType;
		
		double[] resultValues = null;
		
		//*******************************************************************************************************************
		if(scanType.equals("Hilbert")){	
					
			CsajAlgorithm_HilbertScan hilbertScan = new CsajAlgorithm_HilbertScan(rai);
			resultValues = hilbertScan.getHilbertScan();

		}
		
		//*******************************************************************************************************************
		if(scanType.equals("Row meander")){
		
			RandomAccess<?> ra=  rai.randomAccess();
			// Single meander row---------------------------------------------------------------------------------
			resultValues = new double[(int)(width*height)];
			int idx = 0;
			
			for (int y = 0; y < height; y++) { // columns
				if (y % 2 == 0) { //even y
					for (int x = 0; x < width; x++) { // one row from left to right
						ra.setPosition(x, 0);
						ra.setPosition(y, 1);
						resultValues[idx] = ((UnsignedByteType) ra.get()).getRealFloat(); //always from left to right
						idx = idx + 1;
						//logService.info(this.getClass().getName() + " Meander coordinates x,y: "+ x + " " + y);
					}
				}
				else { //uneven y
					for (int x = (int)(width-1); x >= 0; x--) { // one row from right to left
						ra.setPosition(x, 0);
						ra.setPosition(y, 1);
						resultValues[idx] = ((UnsignedByteType) ra.get()).getRealFloat(); //always from left to right
						idx = idx + 1;
						//logService.info(this.getClass().getName() + " Meander coordinates x,y: "+ x + " " + y);
					}
				}
			}
		}
		
		if(scanType.equals("Column meander")){
			
			RandomAccess<?> ra=  rai.randomAccess();
			// Single meander column---------------------------------------------------------------------------------
			resultValues = new double[(int)(width*height)];
			
			for (int x = 0; x < width; x++) { // columns
				if (x % 2 == 0) { //even x
					for (int y = 0; y < height; y++) { // one column from top to bottom
						ra.setPosition(x, 0);
						ra.setPosition(y, 1);
						resultValues[y + x * (int)height] = ((UnsignedByteType) ra.get()).getRealFloat(); //always from left to right
						//logService.info(this.getClass().getName() + " Meander coordinates x,y: "+ x + " " + y);
					}
				}
				else { //uneven x
					for (int y = (int)(height-1); y >= 0; y--) { // one column from bottom to top
						ra.setPosition(x, 0);
						ra.setPosition(y, 1);
						resultValues[((int)(height-1) - y) + x * (int)height] = ((UnsignedByteType) ra.get()).getRealFloat(); //always from left to right
						//logService.info(this.getClass().getName() + " Meander coordinates x,y: "+ x + " " + y);
					}
				}
			}
		}
		
	if(scanType.equals("Random")){
		
		resultValues = new double[(int)(width*height)];
		Cursor<T> cursor = rai.localizingCursor();
		ArrayList<Integer> list = new ArrayList<Integer>();
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos);	
			list.add((int)((UnsignedByteType) cursor.get()).get()); //copy image data to the vector
		} //cursor
		
		//Shuffle
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		Collections.shuffle(list, random);
		//Collections.Shuffle() does a Fisher-Yates shuffle.
		//It's a more evenly distributed form of shuffling,
		//and does not reshuffle what might have previously been shuffled already.
		//The Fisher-Yates shuffle (also known as Donald Knuth Shuffle)
		//is an unbiased algorithm that shuffles items in the array in an equally likely probability.
		//It avoids the chance of 'moving' the same object twice.
		//The easy way (also the known as the naive implementation) is
		//to pick randomly any array index and shuffle it over,
		//meaning there's a high chance of picking the same index that has already been shuffled.
			
		for (int l = 0; l < list.size(); l++) {
			resultValues[l] = list.get(l);
		}
		
		cursor.reset();	
		list.clear();
		list = null;
	
		}
		
		return new CsajContainer_ProcessMethod(resultValues);
	}

	//This methods reduces dimensionality to 2D just for the display 	
	//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
	//pushes a 3D array to the display and
	//yields mouse moving errors because the third dimension is not available
	private <T extends Type<T>, F> void displayImage(String name, IterableInterval<FloatType> iv) {

		// Create an image.
		long[] dims = {iv.max(0)+1, iv.max(0)+1};
		AxisType[] axes = {Axes.X, Axes.Y};
		int bitsPerPixel = 32;
		boolean signed = true;
		boolean floating = true;
		boolean virtual = false;
		//dataset = ij.dataset().create(dims, name, axes, bitsPerPixel, signed, floating);
		Dataset datasetDisplay = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
		
		RandomAccess<RealType<?>> ra = datasetDisplay.randomAccess();
		
		Cursor<FloatType> cursor = iv.localizingCursor();
    	final long[] pos = new long[iv.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos[0], 0);
			ra.setPosition(pos[1], 1);
			ra.get().setReal(cursor.get().get());
		}  	
		
		uiService.show(name, datasetDisplay);
	}
	

	/** The main method enables standalone testing of the command. */
	public static void main(final String... args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		// display the user interface
		ij.ui().showUI();

		// open and display an image
		final File imageFile = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE);
		final Dataset image = ij.scifio().datasetIO().open(imageFile.getAbsolutePath());
		ij.ui().show(image);
		// execute the filter, waiting for the operation to finish.
		//ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}

