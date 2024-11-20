/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DKolmogorovComplexityCmd.java
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


package at.csa.csaj.plugin3d.cplx;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import javax.swing.UIManager;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.Position;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
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
import org.scijava.table.BoolColumn;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import at.csa.csaj.plugin3d.cplx.util.Kolmogorov3DMethods;
import at.csa.csaj.plugin3d.cplx.util.Kolmogorov3D_Grey;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link ContextCommand} plugin computing <the 3D Kolmogorov complexity</a>
 * of an image volume.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		label = "3D Kolmogorov complexity",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {})

public class Csaj3DKolmogorovComplexityCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "Computes 3D Kolmogorov complexity and Logical depth";
	private static final String SPACE_LABEL             = "";
	private static final String COMPRESSION_LABEL       = "<html><b>Compression type</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	private static Dataset dataset;
	private static String datasetName;
	private static long width = 0;
	private static long height = 0;
	private static long depth = 0;
	private static long numDimensions = 0;
	private static double numBytes = 0;
	private static long numSlices = 0;
	private static int numVolumes = 0;
	private static long compositeChannelCount = 0;
	private static String imageType = "";
	
	public static final String TABLE_OUT_NAME = "Table - 3D KC and LD";
	
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
	
	//@Parameter
	//private DefaultThreadService defaultThreadService;

	// This parameter does not work in an InteractiveCommand plugin
	// -->> (duplicate displayService error during startup) pom-scijava 24.0.0
	// no problem in a Command Plugin
	//@Parameter
	//private DisplayService displayService;

	@Parameter // This works in an InteractiveCommand plugin
	private DefaultDisplayService defaultDisplayService;

	@Parameter
	private DatasetService datasetService;
	
	@Parameter
	private IOService ioService;

	@Parameter(label = TABLE_OUT_NAME, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;


	// Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelSpace = SPACE_LABEL;

	// Input dataset which is updated in callback functions
	@Parameter(type = ItemIO.INPUT)
	private Dataset datasetIn;

	//-----------------------------------------------------------------------------------------------------
   @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
  	private final String labelRegression = COMPRESSION_LABEL;

    @Parameter(label = "Compression",
		    description = "Type of image compression",
		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
  		    choices = {"ZIP (lossless)", "ZLIB (lossless)", "GZIP (lossless)", "TIFF-LZW (lossless)"}, 
  		    persist = true,  //restore previous value default = true
		    initializer = "initialCompression",
            callback = "callbackCompression")
    private String choiceRadioButt_Compression;
    
    @Parameter(label = "Iterations for LD",
    		   description = "Number of compressions to compute averages",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "99999999999999",
	           stepSize = "1",
	           persist = true,  //restore previous value default = true
	           initializer = "initialNumIterations",
	           callback    = "callbackNumIterations")
    private int spinnerInteger_NumIterations;
    
//    @Parameter(label = "Correct system bias",
//    		 //persist = false,  //restore previous value default = true
//  		       initializer = "initialCorrectSystemBias")
//	private boolean booleanCorrectSystemBias;

    //-----------------------------------------------------------------------------------------------------
   	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
   	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;
   	
   	@Parameter(label = "Skip zero values", persist = true,
   		       callback = "callbackSkipZeroes")
   	private boolean booleanSkipZeroes;
    //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
	@Parameter(label = "Overwrite result display(s)",
	    	   description = "Overwrite already existing result images, plots or tables",
	    	   persist = true,  //restore previous value default = true
			   initializer = "initialOverwriteDisplays")
	private boolean booleanOverwriteDisplays;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelProcessOptions = PROCESSOPTIONS_LABEL;

	@Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
	    	description = "Immediate processing of active image when a parameter is changed",
			callback = "callbackProcessImmediately")
	private boolean booleanProcessImmediately;
	
//	@Parameter(label = "OK - process image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumImageSlice",
//			   callback = "callbackNumImageSlice")
//	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "    OK - process single volume     ", callback = "callbackProcessSingleVolume")
	private Button buttonProcessSingleVolume;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//  @Parameter(label = "Preview of single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

//	@Parameter(label = "Preview of all available images", callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;
	
	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}

    protected void initialCompression() {
    	choiceRadioButt_Compression = "ZIP (lossless)";
    } 
     
    protected void initialNumIterations() {
      	spinnerInteger_NumIterations = 10;
    }
//    protected void initialCorrectSystemBias() {
//    	booleanCorrectSystemBias = true;
//    }

	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	
	/** Executed whenever the {@link #choiceRadioButt_Interpolation} parameter changes. */
	protected void callbackCompression() {
		logService.info(this.getClass().getName() + " Compression method set to " + choiceRadioButt_Compression);
		if (choiceRadioButt_Compression.equals("TIFF-LZW (lossless)")) {
				booleanSkipZeroes = false;	
				logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
			}
	}
	/** Executed whenever the {@link #spinnerInteger_NumIterations} parameter changes. */
	protected void callbackNumIterations() {
		logService.info(this.getClass().getName() + " Number of iterations set to " + spinnerInteger_NumIterations);
	}
	/** Executed whenever the {@link #booleanSkipZeroes} parameter changes. */
	protected void callbackSkipZeroes() {
		if (choiceRadioButt_Compression.equals("TIFF-LZW (lossless)")) {
			booleanSkipZeroes = false;		
			logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
		}
		logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
//	/** Executed whenever the {@link #spinnerInteger_NumImageSlice} parameter changes. */
//	protected void callbackNumImageSlice() {
//		if (spinnerInteger_NumImageSlice > numSlices){
//			logService.info(this.getClass().getName() + " No more images available");
//			spinnerInteger_NumImageSlice = (int)numSlices;
//		}
//		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
//	}
	
	/**
	 * Executed whenever the {@link #buttonProcessSingleImage} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessSingleVolume() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    startWorkflowForSingleVolume();
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
	        	startWorkflowForSingleVolume();
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
		    	    startWorkflowForSingleVolume();
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
		startWorkflowForSingleVolume();
	
		logService.info(this.getClass().getName() + " Finished command run");
	}

	public void checkItemIOIn() {

		//Define supported image types for this plugin
		String[] supportedImageTypes = {"Grey"};
		//String[] supportedImageTypes = {"RGB"};
		//String[] supportedImageTypes = {"Grey", "RGB"};
		
		//Check input and get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkVolumeDatasetIn(logService, datasetIn, supportedImageTypes);
		if (datasetInInfo == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Inital check failed");
			cancel("ComsystanJ 3D plugin cannot be started - Initial check failed.");
		} else {
			width  =       			(long)datasetInInfo.get("width");
			height =       			(long)datasetInInfo.get("height");
			depth  =       			(long)datasetInInfo.get("depth");
			numDimensions =         (int)datasetInInfo.get("numDimensions");
			compositeChannelCount = (int)datasetInInfo.get("compositeChannelCount");
			numSlices =             (long)datasetInInfo.get("numSlices");
			imageType =   			(String)datasetInInfo.get("imageType");
			datasetName = 			(String)datasetInInfo.get("datasetName");
			//sliceLabels = 		(String[])datasetInInfo.get("sliceLabels");
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleVolume() {
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing 3D KC, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	deleteExistingDisplays();
    	generateTableHeader();
        logService.info(this.getClass().getName() + " Processing volume...");
		processSingleInputVolume();
		
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
			
			//Check if Grey or RGB
			if (imageType.equals("Grey")) {
				//do nothing, it is OK
			};
			if (imageType.equals("RGB")) {
				//At first Index runs through RGB channels and then through stack index
				//0... R of first RGB image, 1.. G of first RGB image, 2..B of first RGB image, 3... R of second RGB image, 4...G, 5...B,.......
				activeSliceIndex = (int) Math.floor((float)activeSliceIndex/3.f);
			}
			
			
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
	
	/**
	 * This method deletes already open displays
	 * 
	 */
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
		if (optDeleteExistingPlots) {
//			//This dose not work with DisplayService because the JFrame is not "registered" as an ImageJ display	
//			if (doubleLogPlotList != null) {
//				for (int l = 0; l < doubleLogPlotList.size(); l++) {
//					doubleLogPlotList.get(l).setVisible(false);
//					doubleLogPlotList.get(l).dispose();
//					// doubleLogPlotList.remove(l); /
//				}
//				doubleLogPlotList.clear();
//			}
//			//ImageJ PlotWindows aren't recognized by DeafultDisplayService!!?
//			List<Display<?>> list = defaultDisplayService.getDisplays();
//			for (int i = 0; i < list.size(); i++) {
//				Display<?> display = list.get(i);
//				System.out.println("display name: " + display.getName());
//				if (display.getName().contains("Grey value profile"))
//					display.close();
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


	/** This method takes the active image volume and computes results. 
	 *
	 **/
	private void processSingleInputVolume() {
		
		long startTime = System.currentTimeMillis();
	
		//get rai
		RandomAccessibleInterval<T> rai = null;	
		rai =  (RandomAccessibleInterval<T>) datasetIn.getImgPlus(); //dim==3

		// Compute regression parameters
		CsajContainer_ProcessMethod containerPM = process(rai); //rai is 3D
		//0 Volume size, 1 KC, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		writeToTable(containerPM); //write always to the first row

		
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

	

	/** Generates the table header {@code DefaultGenericTable} */
	private void generateTableHeader() {
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		IntColumn columnNumbIterations     = new IntColumn("Iterations [#]");
		GenericColumn columnComprType      = new GenericColumn("Compression");	
		BoolColumn columnSkipZeroes        = new BoolColumn("Skip zeroes");
		GenericColumn columnImgSz          = new GenericColumn("Volume size [MB]");
		GenericColumn columnKC             = new GenericColumn("3D KC [MB]");
		GenericColumn columnImgSzMinusKC   = new GenericColumn("Volume size - (3D KC) [MB]");
		GenericColumn columnKCDivImgSz     = new GenericColumn("(3D KC)/Volumesize");
		GenericColumn columnLD             = new GenericColumn("3D LD [ns]");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnComprType);
		tableOut.add(columnSkipZeroes);
		tableOut.add(columnImgSz);
		tableOut.add(columnKC);
		tableOut.add(columnImgSzMinusKC);
		tableOut.add(columnKCDivImgSz);
		tableOut.add(columnNumbIterations);
		tableOut.add(columnLD);
	}

	/**
	 * collects current result and writes to table
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(CsajContainer_ProcessMethod containerPM) { 

		String compressionType = choiceRadioButt_Compression;
		int numIterations   = spinnerInteger_NumIterations;
		
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",				   tableOut.getRowCount()-1, datasetName);	
		tableOut.set("Compression",                tableOut.getRowCount()-1, choiceRadioButt_Compression);
		tableOut.set("Skip zeroes",                tableOut.getRowCount()-1, booleanSkipZeroes);
		tableOut.set("Volume size [MB]",           tableOut.getRowCount()-1, containerPM.item1_Values[0]);
		tableOut.set("3D KC [MB]",                 tableOut.getRowCount()-1, containerPM.item1_Values[1]);
		tableOut.set("Volume size - (3D KC) [MB]", tableOut.getRowCount()-1, containerPM.item1_Values[2]);
		tableOut.set("(3D KC)/Volumesize",         tableOut.getRowCount()-1, containerPM.item1_Values[3]);	
		tableOut.set("Iterations [#]",             tableOut.getRowCount()-1, numIterations);	
		tableOut.set("3D LD [ns]",                 tableOut.getRowCount()-1, containerPM.item1_Values[4]);	
	}

	/**
	*
	* Processing
	*/
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<T> rai) { //3Dvolume
	
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		//dataset = datasetService.create(rai);
		
		String compressionType = choiceRadioButt_Compression;
		int numIterations      = spinnerInteger_NumIterations;
		boolean skipZeroes     = booleanSkipZeroes;
		
		double[] resultValues  = new double[5];

		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		long depth  = rai.dimension(2);
		
		Kolmogorov3DMethods kc3D = null;
		
	
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
			//*****************************************************************************************************************************************
				kc3D = new Kolmogorov3D_Grey(rai, compressionType, skipZeroes, numIterations, logService, uiService, ioService, datasetService, dlgProgress, statusService);	
		
			//******************************************************************************************************************************************		
		} else if (imageType.equals("RGB")) { // RGB image  //additional check, is already checked during validation of active dataset
		
			//no method implemented

		}
		
		resultValues = kc3D.calcResults();
		logService.info(this.getClass().getName() + " 3D KC: " + resultValues[1]);
		
		return new CsajContainer_ProcessMethod(resultValues);
		// Output
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
		// ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
