/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DKolmogorovComplexityCmd.java
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
package at.csa.csaj.plugin2d.cplx;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.display.DefaultDisplayService;
import org.scijava.display.Display;
import org.scijava.io.IOService;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
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
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;
import io.scif.SCIFIO;
import io.scif.codec.CompressionType;
import io.scif.config.SCIFIOConfig;


/**
 * A {@link ContextCommand} plugin computing
 * <the Kolmogorov complexity and Logical depth </a>
 * of an image.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		label = "KC and LD",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {})

public class Csaj2DKolmogorovComplexityCmd<T extends RealType<T>> extends ContextCommand implements Previewable {
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes Kolmogorov complexity KC and Logical depth LD</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String COMPRESSION_LABEL       = "<html><b>Compression type</b></html>";
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
	private static File kolmogorovComplexityDir;
	private static double durationReference  = Double.NaN;
	private static double megabytesReference = Double.NaN;
		
	public static final String TABLE_OUT_NAME = "Table - KC and LD";
	
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
  	private final String labelRegression = COMPRESSION_LABEL;

    @Parameter(label = "Compression",
		    description = "Type of image compression",
		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
  		    choices = {"ZIP (lossless)", "ZLIB (lossless)", "GZIP (lossless)", "TIFF-LZW (lossless)", "PNG (lossless)", "J2K (lossless)", "JPG (lossy)"},  //"PNG (lossless)" "ZIP (lossless)" 
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
    
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
  
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #choiceRadioButt_Interpolation} parameter changes. */
	protected void callbackCompression() {
		logService.info(this.getClass().getName() + " Compression method set to " + choiceRadioButt_Compression);
		if (choiceRadioButt_Compression.equals("TIFF-LZW (lossless)") ||
			choiceRadioButt_Compression.equals("PNG (lossless)")      ||	
			choiceRadioButt_Compression.equals("J2K (lossless)")      ||
			choiceRadioButt_Compression.equals("JPG (lossy)")) {
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
		if (choiceRadioButt_Compression.equals("TIFF-LZW (lossless)") ||
			choiceRadioButt_Compression.equals("PNG (lossless)")      ||	
			choiceRadioButt_Compression.equals("J2K (lossless)")      ||
			choiceRadioButt_Compression.equals("JPG (lossy)")) {
			booleanSkipZeroes = false;		
			logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
		}
		logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
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
			
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Kolmogorov complexity, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
		deleteExistingDisplays();
		generateTableHeader();
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
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Kolmogorov complexities, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);
	
		logService.info(this.getClass().getName() + " Processing all available images");
	    deleteExistingDisplays();
	    generateTableHeader();
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
	
		writeToTable(0, s, containerPM); //write always to the first row
		
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
				
				writeToTable(s, s, containerPM);
				
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
	private void generateTableHeader(){
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		GenericColumn columnSliceName      = new GenericColumn("Slice name");
		IntColumn columnNumbIterations     = new IntColumn("Iterations [#]");
		GenericColumn columnComprType      = new GenericColumn("Compression");
		BoolColumn columnSkipZeroes        = new BoolColumn("Skip zeroes");
		GenericColumn columnImgSz          = new GenericColumn("Image size [MB]");
		GenericColumn columnKC             = new GenericColumn("KC [MB]");
		GenericColumn columnImgSzMinusKC   = new GenericColumn("Image size - KC [MB]");
		GenericColumn columnKCDivImgSz     = new GenericColumn("KC/Imagesize");
		GenericColumn columnLD             = new GenericColumn("LD [ns]");
		
	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
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
	 * 
	 * @param int numRow to write in the result table
	 * @param int numSlice sclice number of images from datasetIn.
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(int numRow, int numSlice, CsajContainer_ProcessMethod containerPM) {

		String compressionType = choiceRadioButt_Compression;
		int numIterations      = spinnerInteger_NumIterations;
	
		int row = numRow;
	    int s = numSlice;	
		
		//fill table with values
		tableOut.appendRow();
		tableOut.set("File name",  row, datasetName);	
		if (sliceLabels != null) tableOut.set("Slice name", row, sliceLabels[s]);
		tableOut.set("Compression",           row, choiceRadioButt_Compression);
		tableOut.set("Skip zeroes",           row, booleanSkipZeroes);
		tableOut.set("Image size [MB]",       row, containerPM.item1_Values[0]);
		tableOut.set("KC [MB]",               row, containerPM.item1_Values[1]);
		tableOut.set("Image size - KC [MB]",  row, containerPM.item1_Values[2]);
		tableOut.set("KC/Imagesize",          row, containerPM.item1_Values[3]);	
		tableOut.set("Iterations [#]",        row, numIterations);	
		tableOut.set("LD [ns]",               row, containerPM.item1_Values[4]);	
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
			
		String compressionType = choiceRadioButt_Compression;
		boolean skipZeroes = booleanSkipZeroes;
		
		int numIterations     = spinnerInteger_NumIterations;
		double[] resultValues = new double[5];
		for (int n = 0; n < resultValues.length; n++) resultValues[n] = Double.NaN;
		
		DescriptiveStatistics stats;
		
		//*******************************************************************************************************************
		if(compressionType.equals("ZIP (lossless)")){	
			
			//convert rai to 1D byte[]
			List<Byte> list = new ArrayList<Byte>();  //Image as a byte list
			byte sample = 0; 
			// Loop through all pixels of this image
			Cursor<?> cursor = Views.iterable(rai).localizingCursor();
			while (cursor.hasNext()) { //Image
				cursor.fwd();
				sample = (byte) ((UnsignedByteType) cursor.get()).get();
				if ((skipZeroes) && (sample == 0)) {
					//do not add zeroes;
				} else {
					list.add(sample); 
				}
			}
			byte[] sequence = ArrayUtils.toPrimitive((list.toArray(new Byte[list.size()])));
			list = null;
			
			double sequenceSize = sequence.length; //Bytes
		    double originalSize = sequenceSize/1024/1024;   //[MB]
		    
			byte[] compressedSequence = null;
			compressedSequence = calcCompressedBytes_ZIP(sequence);
			double kc = (double)compressedSequence.length/1024/1024; //[MB]	
				
			byte[] decompressedSequence;
			stats = new DescriptiveStatistics();
			dlgProgress.setBarIndeterminate(false);
			
			for (int it = 0; it < numIterations; it++){
				long startTime = System.nanoTime();
				decompressedSequence = calcDecompressedBytes_ZIP(compressedSequence);
				long time = System.nanoTime();
				stats.addValue((double)(time - startTime));
			}
			//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
			double ld = stats.getPercentile(50); //Median	; //[ns]	
			//double is = megabytesReference;
			double is = originalSize;
			//double is = dataset.getBytesOfInfo()/1024.0/1024.0;
			resultValues[0] = is;
			resultValues[1] = kc;
			resultValues[2] = is-kc;
			resultValues[3] = kc/is;
			resultValues[4] = ld; 
		}
		
		//*******************************************************************************************************************
		if(compressionType.equals("ZLIB (lossless)")){
		
			//convert rai to 1D byte[]
			List<Byte> list = new ArrayList<Byte>();  //Image as a byte list
			byte sample = 0; 
			// Loop through all pixels of this image
			Cursor<?> cursor = Views.iterable(rai).localizingCursor();
			while (cursor.hasNext()) { //Image
				cursor.fwd();
				sample = (byte) ((UnsignedByteType) cursor.get()).get();
				if ((skipZeroes) && (sample == 0)) {
					//do not add zeroes;
				} else {
					list.add(sample); 
				}
			}
			byte[] sequence = ArrayUtils.toPrimitive((list.toArray(new Byte[list.size()])));
			list = null;
			
			double sequenceSize = sequence.length; //Bytes
		    double originalSize = sequenceSize/1024/1024;   //[MB]
		    
			byte[] compressedSequence = null;
			compressedSequence = calcCompressedBytes_ZLIB(sequence);
			double kc =  (double)compressedSequence.length/1024/1024; //[MB]	
				
			byte[] decompressedSequence;
			stats = new DescriptiveStatistics();		
		
			for (int it = 0; it < numIterations; it++){
				statusService.showStatus((it+1), numIterations, "Processing " + (it+1) + "/" + numIterations);
				long startTime = System.nanoTime();
				decompressedSequence = calcDecompressedBytes_ZLIB(compressedSequence);
				long time = System.nanoTime();
				stats.addValue((double)(time - startTime));
			}
			//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
			double ld = stats.getPercentile(50); //Median	; //[ns]	
			//double is = megabytesReference;
			double is = originalSize;
			//double is = dataset.getBytesOfInfo()/1024.0/1024.0;
			resultValues[0] = is;
			resultValues[1] = kc;
			resultValues[2] = is-kc;
			resultValues[3] = kc/is;
			resultValues[4] = ld; 
		}
		
		//*******************************************************************************************************************
		if(compressionType.equals("GZIP (lossless)")){	
			
			//convert rai to 1D byte[]
			List<Byte> list = new ArrayList<Byte>();  //Image as a byte list
			byte sample = 0; 
			// Loop through all pixels of this image
			Cursor<?> cursor = Views.iterable(rai).localizingCursor();
			while (cursor.hasNext()) { //Image
				cursor.fwd();
				sample = (byte) ((UnsignedByteType) cursor.get()).get();
				if ((skipZeroes) && (sample == 0)) {
					//do not add zeroes;
				} else {
					list.add(sample); 
				}
			}
			byte[] sequence = ArrayUtils.toPrimitive((list.toArray(new Byte[list.size()])));
			list = null;
			
			double sequenceSize = sequence.length; //Bytes
		    double originalSize = sequenceSize/1024/1024;   //[MB]
		    
			byte[] compressedSequence = null;
			compressedSequence = calcCompressedBytes_GZIP(sequence);
			double kc = (double)compressedSequence.length/1024/1024; //[MB]	
				
			byte[] decompressedSequence;
			stats = new DescriptiveStatistics();
			
			for (int it = 0; it < numIterations; it++){
				long startTime = System.nanoTime();
				decompressedSequence = calcDecompressedBytes_GZIP(compressedSequence);
				long time = System.nanoTime();
				stats.addValue((double)(time - startTime));
			}
			//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
			double ld = stats.getPercentile(50); //Median	; //[ns]	
			//double is = megabytesReference;
			double is = originalSize;
			//double is = dataset.getBytesOfInfo()/1024.0/1024.0;
			resultValues[0] = is;
			resultValues[1] = kc;
			resultValues[2] = is-kc;
			resultValues[3] = kc/is;
			resultValues[4] = ld; 
		}

		File targetFile;
		//*******************************************************************************************************************	
		//Slow because tiff files are saved to disk
		if(compressionType.equals("TIFF-LZW (lossless)")){
	
			createTempDirectory();
			computeTiffReferenceValues();	
			targetFile = saveTiffLZWfile();
			resultValues = computeKCValues(targetFile);
			deleteTempDirectory();
		}	
		
		//*******************************************************************************************************************
		if(compressionType.equals("PNG (lossless)")){

			createTempDirectory();
			computeTiffReferenceValues();	
			targetFile = savePNGfile();
			resultValues = computeKCValues(targetFile);
			deleteTempDirectory();
		}
		
		//*******************************************************************************************************************
		if(compressionType.equals("J2K (lossless)")){
		
			createTempDirectory();
			computeTiffReferenceValues();	
			targetFile = saveJ2Kfile();
			resultValues = computeKCValues(targetFile);
			deleteTempDirectory();
		}
		//*******************************************************************************************************************
		if(compressionType.equals("JPG (lossy)")){
	
			createTempDirectory();
			computeTiffReferenceValues();	
			targetFile = saveJPGfile();
			resultValues = computeKCValues(targetFile);
			deleteTempDirectory();
		}
		
		return new CsajContainer_ProcessMethod(resultValues);
	}
	//*******************************************************************************************************************
	/**
	 * This method generates a temporary directory for saving temporary files
	 * @param kolmogorovComplexityDir
	 * @return
	 */
	private void createTempDirectory() {
		File userTempDir = new File(System.getProperty("user.home"));
		String tempFolder = userTempDir.toString() + File.separator + ".kolmogorovcomplexity";
		
		//check if new directory is already available
		FileFilter fileFilter = new FileFilter() {
			public boolean accept(File file) {
			return file.isDirectory();
		}};
		File[] files = userTempDir.listFiles(fileFilter);
		boolean found = false;
		kolmogorovComplexityDir = null;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				if (files[i].toString().contains(".kolmogorovcomplexity")) {
					found = true;
					kolmogorovComplexityDir = files[i]; 
					logService.info(this.getClass().getName() + " Directory " + kolmogorovComplexityDir.toString() + " already exists");
				}
			}
		}
		//create new director if it is not available
		if (!found){
			kolmogorovComplexityDir = new File(tempFolder);
			boolean success = kolmogorovComplexityDir.mkdir();
			if (success) {
				logService.info(this.getClass().getName() + " Created directory: " + kolmogorovComplexityDir.toString());
			} else {
				logService.info(this.getClass().getName() + " Unable to create directory:  " + kolmogorovComplexityDir.toString());
				return;
			}

		}
	
		//Delete files already in the folder
		files = new File(tempFolder).listFiles();

		//delete Reference file
		boolean success1 = false;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				success1 = files[i].delete();
				if (success1) logService.info(this.getClass().getName() + " Successfully deleted existing temp image " + files[i].getName());
				else {
					logService.info(this.getClass().getName() + " Could not delete existing temp image " + files[i].getName());
					return;
				}
			}		
		}
	}
	//*******************************************************************************************************************
	/**
	 * This method computes reference values of uncompressed TIFF files
	 * @param referenceFile
	 * @return
	 */
	private void computeTiffReferenceValues() {
	
		int numIterations      = spinnerInteger_NumIterations;
		//calculate Reference file if chosen
		//double durationReference  = Double.NaN;
		//double megabytesReference = Double.NaN;
		
		//Reference Image for correction of loading and system bias;
		String referencePath = kolmogorovComplexityDir.toString() + File.separator + "Temp.tif";
		File referenceFile = new File(referencePath);

		//save reference file******************************************************************
//		JAI.create("filestore", pi, reference, "TIFF");	
//		PlanarImage piReference = null;
		
//		// save image using SciJava IoService  -- does work too
//		try {
//			ioService.save(dataset, referencePath);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		SCIFIO       scifio = new SCIFIO();
		SCIFIOConfig config = new SCIFIOConfig();
		config.writerSetCompression(CompressionType.UNCOMPRESSED);
		FileLocation loc = new FileLocation(referenceFile.toURI());
		//final Context ctx = new Context();
		//new ImgSaver(ctx).saveImg(loc, dataset, config);
		try {
			scifio.datasetIO().save(dataset, loc, config);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		//scifio.getContext().dispose(); //NullPointerExeption when run as Plugin in Fiji 
		if (referenceFile.exists()){
			logService.info(this.getClass().getName() + " Successfully saved temp reference image " + referenceFile.getName());

		} else {
			logService.info(this.getClass().getName() + " Something went wrong,  image " + referenceFile.getName() + " coud not be loaded!");

		}
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for (int i = 0; i < numIterations; i++){
			
			statusService.showStatus((i+1), numIterations, "Processing " + (i+1) + "/" + numIterations);
			
			long startSavingToDiskTime = System.nanoTime();
			//piReference = JAI.create("fileload", reference);
			//piReference.getHeight(); //Essential for proper time stamps. Without this, duration times are almost identical for every image  and very small
			try {
				//BufferedImage bi = ImageIO.read(referenceFile);  //faster than JAI
				dataset = (Dataset) ioService.open(referencePath);
				//uiService.show(referenceFile.getName(), dataset);
				if (dataset == null) {
					logService.info(this.getClass().getName() + " Something went wrong,  image " + referenceFile.getName() + " coud not be loaded!");
					return;
				}
				//System.out.println("  ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long time = System.nanoTime();
			stats.addValue((double)(time - startSavingToDiskTime));
		}
		//durationReference = (double)(System.nanaoTime() - startTime)/iterations;
		durationReference = stats.getPercentile(50); //Median
		
		//get size of reference file
		double bytes = referenceFile.length();
		double kilobytes = (bytes / 1024);
		megabytesReference = (kilobytes / 1024);
		
		//delete Reference file
		boolean success2 = false;
		if (referenceFile.exists()){
			success2 = referenceFile.delete();
			if (success2)  logService.info(this.getClass().getName() + " Successfully deleted temp reference image " + referenceFile.getName());
			if (!success2) logService.info(this.getClass().getName() + " Could not delete temp reference image "     + referenceFile.getName());
		}
		
		//0 megabytesReference, 1 durationReference
	}
	//*******************************************************************************************************************
	/**
	 * This method saves a compressed tiff file and computes KC and LD
	 * @param targetFile
	 * @return
	 */
	private File saveTiffLZWfile() {
			
		this.deleteTempFile("Temp_LZW.tif");
		
		String targetPath = kolmogorovComplexityDir.toString() + File.separator + "Temp_LZW.tif";
		File targetFile = new File(targetPath);
						
//		// save image using SciJava IoService
//		try {
//			ioService.save(img, target);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		SCIFIO scifio = new SCIFIO();
		SCIFIOConfig config = new SCIFIOConfig();
		
		config.writerSetCompression(CompressionType.LZW);
	 
		FileLocation loc = new FileLocation(targetFile.toURI());
		//final Context ctx = new Context();
		//new ImgSaver(ctx).saveImg(loc, dataset, config);
		try {
			scifio.datasetIO().save(dataset, loc, config);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		//scifio.getContext().dispose(); //NullPointerExeption when run as Plugin in Fiji
	
		return targetFile;
	
	}

	//***************************************************************************************************
	/**
	 * This method saves a compressed png file
	 * @return
	 */
	private File savePNGfile() {
		
		this.deleteTempFile("Temp.png");
		String targetPath = kolmogorovComplexityDir.toString() + File.separator + "Temp.png";
		File targetFile = new File(targetPath);
				
		BufferedImage bi = new BufferedImage((int)dataset.dimension(0), (int)dataset.dimension(1), 10); //TYPE_BYTE_GRAY = 10;  TYPE_INT_RGB = 1;
		WritableRaster raster = bi.getRaster();
		
		Cursor<RealType<?>> cursor = dataset.localizingCursor();
	 	final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			raster.setPixel((int)pos[0], (int)pos[1], new int[]{Math.round(cursor.get().getRealFloat())});
		}  	

		// save image using ImageIO
		try {
			ImageIO.write(bi, "PNG", targetFile); //level of compression lower ~ zip tif compared to JAI
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
				
//		//save image using SciJava IoService
//		try {
//			ioService.save(dataset, targetPath);  //does not work with png
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
					
//		scifio = new SCIFIO();
//		config = new SCIFIOConfig();
//		//config.writerSetCompression(compressionType.??????????.toString());
//		loc = new FileLocation(targetFile.toURI());
//		//final Context ctx = new Context();
//		//new ImgSaver(ctx).saveImg(loc, dataset, config);
//		try {
//			scifio.datasetIO().save(dataset, loc, config);
//		} catch (IOException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
//		scifio.getContext().dispose();
		
		
//		final APNGFormat apngFormat = new APNGFormat();   
//	    apngFormat.setContext(scifio.context()); 
//	      
//        // Get writer and set metadata
//        io.scif.Writer writer = null;
//        config = new SCIFIOConfig();
//        config.writerSetCompression(CompressionType.UNCOMPRESSED.toString());
//
//		try {
//			writer = apngFormat.createWriter();
//		} catch (FormatException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//        try {
//        	io.scif.formats.APNGFormat.Metadata meta = new io.scif.formats.APNGFormat.Metadata();
//        	meta.createImageMetadata(1);	
//        	ImageMetadata imageMetadata = meta.get(0);
//    	    imageMetadata.setBitsPerPixel(FormatTools.getBitsPerPixel(FormatTools.UINT8));
//    	    imageMetadata.setFalseColor(true);
//    	    imageMetadata.setPixelType(FormatTools.UINT8);
//    	    imageMetadata.setPlanarAxisCount(2);
//    	    imageMetadata.setLittleEndian(false);
//    	    imageMetadata.setIndexed(false);
//    	    imageMetadata.setInterleavedAxisCount(0);
//    	    imageMetadata.setThumbnail(false);
//    	    imageMetadata.setOrderCertain(true);
//    	    imageMetadata.addAxis(Axes.X, dataset.dimension(0));
//    	    imageMetadata.addAxis(Axes.Y, dataset.dimension(1));
//    	   
//			writer.setMetadata(meta);
//		} catch (FormatException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
//        try {
//			writer.setDest(loc);
//		} catch (FormatException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}       
//        //Check metadata.
//        // Write the image
//        ImgSaver save = new ImgSaver(scifio.getContext());
//        save.saveImg(writer, dataset, config);	
//		
//        DefaultImgUtilityService dius = new DefaultImgUtilityService();
//		SCIFIOImgPlus<T> scifioImgPlus = dius.makeSCIFIOImgPlus((Img<T>) rai);  	
////        save.saveImg(writer, scifioImgPlus, config);	
		
		
		
		
		//scifio.getContext().dispose(); //NullPointerExeption when run as Plugin in Fiji
		
		return targetFile;
	}
	
	//***************************************************************************************************
	/**
	 * This method saves a compressed j2k file
	 * @return
	 */
	private File saveJ2Kfile() {
		
		this.deleteTempFile("Temp.j2k");
		String targetPath = kolmogorovComplexityDir.toString() + File.separator + "Temp.j2k";
		File targetFile = new File(targetPath);
		
		SCIFIO scifio = new SCIFIO();
		SCIFIOConfig config = new SCIFIOConfig();
		config.writerSetCompression(CompressionType.J2K);
		FileLocation loc = new FileLocation(targetFile.toURI());
		//final Context ctx = new Context();
		//new ImgSaver(ctx).saveImg(loc, dataset, config);
		try {
			scifio.datasetIO().save(dataset, loc, config);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		//scifio.getContext().dispose(); //NullPointerExeption when run as Plugin in Fiji
		return targetFile;
	}
	
	//***************************************************************************************************
	/**
	 * This method saves a compressed jpg file
	 * @return
	 */
	private File saveJPGfile() {
		
		this.deleteTempFile("Temp.jpg");
		String targetPath = kolmogorovComplexityDir.toString() + File.separator + "Temp.jpg";
		File targetFile = new File(targetPath);
						
		SCIFIO scifio = new SCIFIO();
		SCIFIOConfig config = new SCIFIOConfig();
		config.writerSetCompression(CompressionType.JPEG);
		FileLocation loc = new FileLocation(targetFile.toURI());
		//final Context ctx = new Context();
		//new ImgSaver(ctx).saveImg(loc, dataset, config);
		try {
			scifio.datasetIO().save(dataset, loc, config);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		//scifio.getContext().dispose(); //NullPointerExeption when run as Plugin in Fiji
		return targetFile;
	}
	
	//***************************************************************************************************************************
	/*
	 * This method computes the KC and LG values
	 * @param targetFile
	 * @return
	 */
	private double[] computeKCValues(File targetFile) {
		
		int numIterations = spinnerInteger_NumIterations;
		//set default complexities
		double is         = Double.NaN;  //image size of uncopressed image in MB	
		double kc         = Double.NaN;  //Kolmogorov complexity (size of compressed image in MB)
		double ld         = Double.NaN;  //Logical depth
		double systemBias = Double.NaN;  //duration of system without compression
		double ldCorr     = Double.NaN;  //corrected logical depth = ld -systemBias
		
		//calculate Reference file if chosen
		//double durationReference  = Double.NaN;
		//double megabytesReference = Double.NaN;
		
		double[] resultValues  = new double[5];
		//Check if file exists
		if (targetFile.exists()){
			logService.info(this.getClass().getName() + " Successfully saved temp target image " + targetFile.getName());

		} else {
			logService.info(this.getClass().getName() + " Something went wrong,  image " + targetFile.getName() + " coud not be loaded!");
			return null;
		}
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for (int i = 0; i < numIterations; i++) { //
           
			long startTimeOfIterations = System.nanoTime();
			//piTarget = JAI.create("fileload", target);
			//piTarget.getHeight(); //Essential for proper time stamps. Without this, duration times are almost identical for every image and very small
			try {
				//BufferedImage bi = ImageIO.read(targetFile);  //faster than JAI  //BufferedImage not for 3D
				dataset = (Dataset) ioService.open(targetFile.getAbsolutePath());
				//uiService.show(referenceFile.getName(), dataset);
				if (dataset == null) {
					logService.info(this.getClass().getName() + " Something went wrong,  image " + targetFile.getName() + " coud not be loaded!");
					return null;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long time = System.nanoTime();
			stats.addValue((double)(time - startTimeOfIterations));
		}
		//durationTarget = (double)(System.nanoTime() - startTime) / (double)iterations;
	    double durationTarget = stats.getPercentile(50); //Median		

		double bytes = targetFile.length();
		double kilobytes = (bytes / 1024);
		double megabytesTarget = (kilobytes / 1024);
//		double gigabytes = (megabytes / 1024);
//		double terabytes = (gigabytes / 1024);
//		double petabytes = (terabytes / 1024);
		
		boolean success3 = false;
		if (targetFile.exists()){
			success3 = targetFile.delete();
			if (success3)  logService.info(this.getClass().getName() + " Successfully deleted temp target image " + targetFile.getName());
			if (!success3) logService.info(this.getClass().getName() + " Could not delete temp target image " + targetFile.getName());
		}
		
		ld     = durationTarget;
		systemBias = durationReference; //* megabytesTarget / megabytesReference; //Duration of tiff image with same size as compressed image
		ldCorr = ld - systemBias;
		
//		ld         = ld         * 1000.0;
//		systemBias = systemBias * 1000.0;
//		ldCorr     = ldCorr     * 1000.0;
				
		logService.info(this.getClass().getName() + " Iterations: " +numIterations+ "   Duration Image: "     + durationTarget +     "   ld: " + ld + "   Duration Reference: " + durationReference +  "   systemBias: " + systemBias + "   ldCorr: " + ldCorr );
		logService.info(this.getClass().getName() + " megabytesTarget: " +megabytesTarget + "     megabytesReference: " + megabytesReference);
		kc = megabytesTarget;
		is = megabytesReference;
		//is = originalSize;
		//is = dataset.getBytesOfInfo();
		
	    resultValues[0] = is;
	    resultValues[1] = kc;
	    resultValues[2] = is-kc;
	    resultValues[3] = kc/is;
	    resultValues[4] = ldCorr; //target and reference files are tiff
	    //or
	    //resultValues[4] = ld;  //no reference tif only target tiff
	 	    
		return resultValues;
		//0 "Image size [MB]"  1 "KC [MB]" 2 "Image size - KC [MB]"  3 "KC/Imagesize [MB]" 4 "LD [ns]"	
		
		//Output
		//uiService.show("Table - ", table);
		/////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
		//table;
	}
	
	//*******************************************************************************************************************
	/**
	 * This method looks for the file (image) and deletes it
	 * @param string
	 */
	private void deleteTempFile(String fileName) {
		File[] files = kolmogorovComplexityDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				if (files[i].toString().contains(fileName)) {
					logService.info(this.getClass().getName() + " " + fileName + " already exists");
					boolean success = files[i].delete();
					if (success)  logService.info(this.getClass().getName() + " Successfully deleted " + fileName);
					if (!success) logService.info(this.getClass().getName() + " Could not delete " + fileName);
				}
			}
		}	
	}
	//*******************************************************************************************************************

	/**
	 * This method deletes the temp directory
	 */
	private void deleteTempDirectory() {
		boolean success = kolmogorovComplexityDir.delete();
		if (success)  logService.info(this.getClass().getName() + " Successfully deleted temp director " + kolmogorovComplexityDir.getName());
		else {
			logService.info(this.getClass().getName() + " Could not delete temp directory " + kolmogorovComplexityDir.getName());
		}
		
	}
	//*******************************************************************************************************************

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
	
	/**
	 * This method calculates and returns compressed bytes
	 * @param  byte[] sequence
	 * @return byte[] compressed image
	 */
	private byte[] calcCompressedBytes_ZIP(byte[] sequence) {
		
	
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    	try{
	            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
	            ZipEntry ze = new ZipEntry("ZipEntry");
	            zipOutputStream.putNextEntry(ze);
	            //zipOutputStream.setMethod(0); ??
	            zipOutputStream.setLevel(9); //0...9    9 highest compression
	            zipOutputStream.write(sequence);
	            zipOutputStream.close();
	        } catch(IOException e){
	            throw new RuntimeException(e);
	        }
	    byte[] output = outputStream.toByteArray(); 
	    try {
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //System.out.println(" Original: " + data.length  ); 
	    //System.out.println(" ZIP Compressed: " + output.length ); 
	    return output;
	}
	
	/**
	 * This method calculates and returns compressed bytes
	 * @param  byte[] sequence
	 * @return byte[] compressed sequence
	 */
	private byte[] calcCompressedBytes_ZLIB(byte[] sequence) {

		Deflater deflater = new Deflater(); 
		deflater.setLevel(Deflater.BEST_COMPRESSION);
		deflater.setInput(sequence); 
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(sequence.length);  
		
		deflater.finish(); 
		byte[] buffer = new byte[1048];  
		while (!deflater.finished()) { 
			int count = deflater.deflate(buffer); 
			//System.out.println("PlotOpComplLogDepth  Count: " +count);
		    outputStream.write(buffer, 0, count);  
		} 
		try {
			outputStream.close();
		} catch (IOException e) {
			 // TODO Auto-generated catch block
			e.printStackTrace();
		} 
		byte[] output = outputStream.toByteArray(); 
		deflater.end();
		//System.out.println(" Original: " + data.length  ); 
		//System.out.println(" ZLIB Compressed: " + output.length ); 
		return output;
	}
	
	/**
	 * This method calculates and returns compressed bytes
	 * @param  byte[] sequence
	 * @return byte[] compressed sequence
	 */
	private byte[] calcCompressedBytes_GZIP(byte[] sequence) {
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    	try{
	            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
	            gzipOutputStream.write(sequence);
	            gzipOutputStream.close();
	        } catch(IOException e){
	            throw new RuntimeException(e);
	        }
	    byte[] output = outputStream.toByteArray(); 
	    try {
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //System.out.println(" Original: " + data.length  ); 
	    //System.out.println(" GZIP Compressed: " + output.length ); 
	    return output;
	}
	
	/**
	 * This method decompresses byte array
	 * @param  byte[] array  compressed
	 * @return byte[] array  decompressed
	 */
	private byte[] calcDecompressedBytes_ZIP(byte[] array) {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
		InputStream in = null;
		
		in = new ZipInputStream(inputStream);
	
	    byte[] bbuf = new byte[256];
	    while (true) {
	        int r = 0;
			try {
				r = in.read(bbuf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        if (r < 0) {
	          break;
	        }
	        buffer.write(bbuf, 0, r);
	    }
		byte[] output = buffer.toByteArray();  		   
		try {
			buffer.close();
			inputStream.close();
			in.close();		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //System.out.println(" ZIP Input: " + array.length  ); 
	    //System.out.println(" Decompressed: " + output.length ); 
	    return output;
	}
	
	/**
	 * This method decompresses byte array
	 * @param  byte[] array  compressed
	 * @return byte[] array  decompressed
	 */
	private byte[] calcDecompressedBytes_ZLIB(byte[] array) {
		Inflater inflater = new Inflater();   
		inflater.setInput(array);  
		   
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
		    int count = 0;
			try {
				count = inflater.inflate(buffer);
			} catch (DataFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		    outputStream.write(buffer, 0, count);  
		}  
		try {
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		byte[] output = outputStream.toByteArray();  		   
		inflater.end();	
	    //System.out.println(" ZLIB Input: " + array.length  ); 
	    //System.out.println(" Decompressed: " + output.length ); 
	    return output;
	}
	/**
	 * This method decompresses byte array
	 * @param  byte[] array  compressed
	 * @return byte[] array  decompressed
	 */
	private byte[] calcDecompressedBytes_GZIP(byte[] array) {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
		InputStream in = null;
		try {
			in = new GZIPInputStream(inputStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    byte[] bbuf = new byte[256];
	    while (true) {
	        int r = 0;
			try {
				r = in.read(bbuf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        if (r < 0) {
	          break;
	        }
	        buffer.write(bbuf, 0, r);
	    }
		byte[] output = buffer.toByteArray();  		   
		try {
			buffer.close();
			inputStream.close();
			in.close();		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //System.out.println(" GZIP Input: " + array.length  ); 
	    //System.out.println(" Decompressed: " + output.length ); 
	    return output;
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

