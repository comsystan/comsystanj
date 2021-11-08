/*-
 * #%L
 * Project: ImageJ plugin for computing lacunarity.
 * File: Img2DLacunarity.java
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
package at.csa.csaj.img2d.frac.lacunarity;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
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
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.display.DefaultDisplayService;
import org.scijava.display.Display;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link Command} plugin computing
 * <the lacunarity </a>
 * of an image.
 *  
 * <li> Allain C, Cloitre M, Characterizing the lacunarity of random and deterministic fractal sets. Phys. Rev. A, 44, 1991, 3552-3558
 * <li> Plotnik RE, Gardner RH, Hargrove WW, Prestegaard K, Perlmutter M, Lacunarity analysis: A general technique for the analysis of spatial patterns, Phys Rev E, 53, 1996, 5461-5468.
 * <li> Sengupta K, Vinoy KJ, A new measure of lacunarity for generalized fractals and its impact in the electromagnetic behavior of the Koch dipole antennas, Fractals, 14, 2006, 271-282.   
 
 *  
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "Lacunariy",
	initializer = "initialPluginLaunch",
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Image (2D)"),
	@Menu(label = "Lacunarity", weight = 20)})
//public class Img2DLacunarity<T extends RealType<T>> extends InteractiveCommand { //non blocking GUI
public class Img2DLacunarity<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes Lacunarity</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static Img<FloatType> imgFloat;
	private static Img<UnsignedByteType> imgBin;
	private static RandomAccess<UnsignedByteType> raBin;
	private static Img<FloatType> imgDil;
	private static RandomAccess<FloatType> raDil;
	private static RandomAccessibleInterval<?> raiDil;
	private static RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<UnsignedByteType> ra;
	
	private static Cursor<?> cursor = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices  = 0;
	private static int  numBoxes = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[][] resultValuesTable; //first column is the image index, second column are the corresponding regression values
	private static final String tableOutName = "Table - Lacunarities";
	
	private WaitingDialogWithProgressBar dlgProgress;
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
	
	//Input dataset which is updated in callback functions
	@Parameter (type = ItemIO.INPUT)
	private Dataset datasetIn;

	@Parameter(label = tableOutName, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;

	
   //Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
  	//private final String labelSpace = SPACE_LABEL;
    
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
  	private final String labelRegression = REGRESSION_LABEL;

    @Parameter(label = "Number of boxes",
    		   description = "Number of distinct box sizes",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialNumBoxes",
	           callback    = "callbackNumBoxes")
    private int spinnerInteger_NumBoxes;
    
//    @Parameter(label = "Regression Min",
//    		   description = "Minimum x value of linear regression",
// 		       style = NumberWidget.SPINNER_STYLE,
// 		       min = "1",
// 		       max = "32768",
// 		       stepSize = "1",
// 		       //persist  = false,   //restore previous value default = true
// 		       initializer = "initialRegMin",
// 		       callback = "callbackRegMin")
//    private int spinnerInteger_RegMin = 1;
// 
//    @Parameter(label = "Regression Max",
//    		   description = "Maximum x value of linear regression",
//    		   style = NumberWidget.SPINNER_STYLE,
//		       min = "3",
//		       max = "32768",
//		       stepSize = "1",
//		       persist  = false,   //restore previous value default = true
//		       initializer = "initialRegMax",
//		       callback = "callbackRegMax")
//     private int spinnerInteger_RegMax = 3;
    
  //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelMethodOptions = METHODOPTIONS_LABEL;
    
    @Parameter(label = "Method",
   		    description = "Type of analysis",
   		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
     		choices = {"Raster box", "Sliding box", "Tug of war"}, //"Fast Sliding box" is not reliable
     		//persist  = false,  //restore previous value default = true
   		    initializer = "initialMethodType",
            callback = "callbackMethodType")
    private String choiceRadioButt_MethodType;
    
    @Parameter(label = "Color model",
 		    description = "Type of image and computation",
 		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
   		    choices = {"Binary", "Grey"},
   		    //persist  = false,  //restore previous value default = true
 		    initializer = "initialColorModelType",
             callback = "callbackColorModelType")
    private String choiceRadioButt_ColorModelType;
    
    @Parameter(label = "(Sliding box) Pixel %",
   		   description = "% of image pixels to be taken - to lower computation times",
 	       	   style = NumberWidget.SPINNER_STYLE,
 	           min = "1",
 	           max = "100",
 	           stepSize = "1",
 	           //persist  = false,  //restore previous value default = true
 	           initializer = "initialPixelPercentage",
 	           callback    = "callbackPixelPercentage")
     private int spinnerInteger_PixelPercentage;
    
    @Parameter(label = "(Tug of war) Accuracy",
		       description = "Accuracy",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "99999999999999",
	           stepSize = "1",
	           //persist  = false,  //restore previous value default = true
	           initializer = "initialNumAccuracy",
	           callback    = "callbackNumAccuracy")
    private int spinnerInteger_NumAcurracy;
  
    @Parameter(label = "(Tug of war) Confidence",
		       description = "Confidence",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "99999999999999",
	           stepSize = "1",
	           //persist  = false,  //restore previous value default = true
	           initializer = "initialNumConfidence",
	           callback    = "callbackNumConfidence")
    private int spinnerInteger_NumConfidence;
     
 	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
      
     @Parameter(label = "Show double log plot",
    		    //persist  = false,  //restore previous value default = true
  		        initializer = "initialShowDoubleLogPlots")
	 private boolean booleanShowDoubleLogPlot;
       
     @Parameter(label = "Overwrite result display(s)",
    	    	description = "Overwrite already existing result images, plots or tables",
    	    	//persist  = false,  //restore previous value default = true
    			initializer = "initialOverwriteDisplays")
     private boolean booleanOverwriteDisplays;
     
 	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
     private final String labelProcessOptions = PROCESSOPTIONS_LABEL;
     
     @Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
    	    	description = "Immediate processing of active image when a parameter is changed",
    			callback = "callbackProcessImmediately")
     private boolean booleanProcessImmediately;
     
 	@Parameter(label = "Image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumImageSlice",
			   callback = "callbackNumImageSlice")
	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label   = "   Process single image #    ",
		    	callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label   = "Process single active image ",
//		    callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
	    
//  @Parameter(label   = "Process all available images",
// 		    callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;

    //---------------------------------------------------------------------
    //The following initializer functions set initial values	
	protected void initialPluginLaunch() {
		//datasetIn = imageDisplayService.getActiveDataset();
		checkItemIOIn();
	}
    protected void initialNumBoxes() {
      	numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
      	spinnerInteger_NumBoxes = numBoxes;
    }
//    protected void initialRegMin() {
//    	spinnerInteger_RegMin = 1;
//    }
//    protected void initialRegMax() {
//    	numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
//    	spinnerInteger_RegMax =  numBoxes;
//    }
    
    protected void initialMethodType() {
    	choiceRadioButt_MethodType = "Raster box";
    }
    
    protected void initialColorModelType() {
    	choiceRadioButt_ColorModelType = "Binary";
    }
    
    protected void initialPixelPercentage() {
      	spinnerInteger_PixelPercentage = 10;
    }
    
    protected void initialNumAccuracy() {
    	spinnerInteger_NumAcurracy = 90; //s1=30 Wang paper
    }
    
    protected void initialNumConfidence() {
    	spinnerInteger_NumConfidence = 15; //s2=5 Wang paper
    }

    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
  
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #spinInteger_NumBoxes} parameter changes. */
	protected void callbackNumBoxes() {	
		if  (spinnerInteger_NumBoxes < 3) {
			spinnerInteger_NumBoxes = 3;
		}
		int numMaxBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));	
		if (spinnerInteger_NumBoxes > numMaxBoxes) {
			spinnerInteger_NumBoxes = numMaxBoxes;
		};
//		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
//			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
//		}
//		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
//			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
//		}	
		numBoxes = spinnerInteger_NumBoxes;
		logService.info(this.getClass().getName() + " Number of boxes set to " + spinnerInteger_NumBoxes);
	}
//    /** Executed whenever the {@link #spinInteger_RegMin} parameter changes. */
//	protected void callbackRegMin() {
//		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
//			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
//		}
//		if(spinnerInteger_RegMin < 1) {
//			spinnerInteger_RegMin = 1;
//		}
//		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
//	}
//	/** Executed whenever the {@link #spinInteger_RegMax} parameter changes. */
//	protected void callbackRegMax() {
//		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
//			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
//		}		
//		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
//			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
//		}	
//		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_RegMax);
//	}

	/** Executed whenever the {@link #choiceRadioButt_MethodType} parameter changes. */
	protected void callbackMethodType() {
		if (choiceRadioButt_MethodType.equals("Tug of war")) {
			if (choiceRadioButt_ColorModelType.equals("Grey")) {
				logService.info(this.getClass().getName() + " NOTE! Only binary Tug of war algorithm possible!");
				choiceRadioButt_ColorModelType = "Binary";
			}
		}
		logService.info(this.getClass().getName() + " Method set to " + choiceRadioButt_MethodType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
	protected void callbackColorModelType() {
		if (choiceRadioButt_MethodType.equals("Tug of war")) {
			if (choiceRadioButt_ColorModelType.equals("Grey")) {
				logService.info(this.getClass().getName() + " NOTE! Only binary Tug of war algorithm possible!");
				choiceRadioButt_ColorModelType = "Binary";
			}
		}
		logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
	}
	
	/** Executed whenever the {@link #spinInteger_PixelPercentage} parameter changes. */
	protected void callbackPixelPercentage() {
		logService.info(this.getClass().getName() + " Pixel % set to " + spinnerInteger_PixelPercentage);
	}
	
	/** Executed whenever the {@link #spinInteger_NumAccuracy} parameter changes. */
	protected void callbackNumAccuracy() {
		logService.info(this.getClass().getName() + " Accuracy set to " + spinnerInteger_NumAcurracy);
	}
	
	/** Executed whenever the {@link #spinInteger_NumConfidence} parameter changes. */
	protected void callbackNumConfidence() {
		logService.info(this.getClass().getName() + " Confidence set to " + spinnerInteger_NumConfidence);
	}
		
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #spinInteger_NumImageSlice} parameter changes. */
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
	    	   	uiService.show(tableOutName, tableOut);
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
	    	   	uiService.show(tableOutName, tableOut);
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
		    	   	uiService.show(tableOutName, tableOut);   //Show table because it did not go over the run() method
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
		logService.info(this.getClass().getName() + " Widget canceled");
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
		//if(ij.ui().isHeadless()){
		//}	
	    startWorkflowForAllImages();
	}

	public void checkItemIOIn() {
	
		//datasetIn = imageDisplayService.getActiveDataset();
	
		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
	         (datasetIn.firstElement() instanceof FloatType) ){
			//That is OK, proceed
		} else {
					
	    	final MessageType messageType = MessageType.QUESTION_MESSAGE;
			final OptionType optionType = OptionType.OK_CANCEL_OPTION;
			final String title = "Validation result";
			final String message = "Data type not allowed: " + datasetIn.getType().getClass().getSimpleName();
			// Prompt for confirmation.
			//final UIService uiService = getContext().getService(UIService.class);
			Result result = uiService.showDialog(message, title, messageType, optionType);
			
			// Cancel the command execution if the user does not agree.
			//if (result != Result.YES_OPTION) System.exit(-1);
			//if (result != Result.YES_OPTION) return;
			return;
		}
		// get some info
		width = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		//numSlices = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		
		numDimensions = datasetIn.numDimensions();
		if (numDimensions == 2) {
			numSlices = 1; // single image
		} else if (numDimensions == 3) { // Image stack
			numSlices =datasetIn.dimension(2);
		}
		
		// get name of dataset
		datasetName = datasetIn.getName();
		
		try {
			Map<String, Object> prop = datasetIn.getProperties();
			DefaultImageMetadata metaData = (DefaultImageMetadata) prop.get("scifio.metadata.image");
			MetaTable metaTable = metaData.getTable();
			sliceLabels = (String[]) metaTable.get("SliceLabels");
			//eliminate additional image info delimited with \n (since pom-scijava 29.2.1)
			for (int i = 0; i < sliceLabels.length; i++) {
				String label = sliceLabels[i];
				int index = label.indexOf("\n");
				//if character has been found, otherwise index = -1
				if (index > 0) sliceLabels[i] = label.substring(0, index);		
			}
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to read scifio metadata."); 
		}	
		logService.info(this.getClass().getName() + " Name: "              + datasetName); 
		logService.info(this.getClass().getName() + " Image size: "        + width+"x"+height); 
		logService.info(this.getClass().getName() + " Number of images = " + numSlices); 
	}
	
	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleImage() {
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing Lacunarity, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);

		deleteExistingDisplays();
		int sliceIndex = spinnerInteger_NumImageSlice - 1;
		logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
		processSingleInputImage(sliceIndex);
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
		generateTableHeader();
		writeSingleResultToTable(sliceIndex);
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	}
	
	/**
	* This method starts the workflow for all images of the active display
	*/
	protected void startWorkflowForAllImages() {
		
		dlgProgress = new WaitingDialogWithProgressBar("Computing Lacunarity, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);
		
    	logService.info(this.getClass().getName() + " Processing all available images");
		deleteExistingDisplays();
		processAllInputImages();
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
		generateTableHeader();
		writeAllResultsToTable();
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
//			//	if (display.getName().equals("Name")) display.close(); //does not close correctly in Fiji, it is only not available any more
//			//}			
//			//List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Does not also close in Fiji
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
			if (doubleLogPlotList != null) {
				for (int l = 0; l < doubleLogPlotList.size(); l++) {
					doubleLogPlotList.get(l).setVisible(false);
					doubleLogPlotList.get(l).dispose();
					//doubleLogPlotList.remove(l);  /
				}
				doubleLogPlotList.clear();		
			}
		}
		if (optDeleteExistingTables) {
			Display<?> display;
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().contains(tableOutName)) display.close();
			}			
		}
	}
	
//	/** This method computes the maximal number of possible boxes*/
//	private int getMaxBoxNumber(long width, long height) { 
//		float boxWidth = 0;
//		int number = 0; 
//		int epsWidth = 1;
//		while ((boxWidth <= width) && (boxWidth <= height)) {	
//			boxWidth = 2 * epsWidth + 1;
//			epsWidth = epsWidth * 2;
//			number = number + 1;
//		}
//		return number - 1;
//	}
	
	
	/** This method computes the maximal number of possible boxes*/
	private int getMaxBoxNumber(long width, long height) { 
		float boxWidth = 1f;
		int number = 1; 
		while ((boxWidth <= width) && (boxWidth <= height)) {
			boxWidth = boxWidth * 2;
			number = number + 1;
		}
		return number - 1;
	}
	
	/** This method takes the active image and computes results. 
	 *
	 */
	private void processSingleInputImage(int s) {
		long startTime = System.currentTimeMillis();
		resultValuesTable = new double[(int) numSlices][numBoxes + 2]; //+2 because of weighted mean L and mean L
		
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//mg<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
		
		}

		//Compute lacunarity
		double[] lacunarities = process(rai, s);
		
		//set values for output table
		for (int i = 0; i < lacunarities.length; i++ ) {
				resultValuesTable[s][i] = lacunarities[i]; 
		}
		
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
		resultValuesTable = new double[(int) numSlices][numBoxes + 2]; //+2 because of weighted mean L and mean L
 	
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//Img<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		
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
				
				RandomAccessibleInterval<?> rai = null;	
				if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
					rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();
	
				} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
					rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
				
				}
				//lacunarities
				double[] lacunarities = process(rai, s);	
				//set values for output table
				for (int i = 0; i < lacunarities.length; i++ ) {
						resultValuesTable[s][i] = lacunarities[i]; 
				}
				
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
		IntColumn columnMaxNumBoxes        = new IntColumn("# Boxes");
		//IntColumn columnRegMin             = new IntColumn("RegMin");
		//IntColumn columnRegMax             = new IntColumn("RegMax");
		GenericColumn columnMethodType     = new GenericColumn("Method type");
		GenericColumn columnColorModelType = new GenericColumn("Color model");
		DoubleColumn columnL_RP            = new DoubleColumn("<L>-R&P"); //weighted mean L
		DoubleColumn columnL_SV            = new DoubleColumn("<L>-S&V");   //mean L
	
	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
		tableOut.add(columnMaxNumBoxes);
		//tableOut.add(columnRegMin);
		//tableOut.add(columnRegMax);
		tableOut.add(columnMethodType);
		tableOut.add(columnColorModelType);
		tableOut.add(columnL_RP);
		tableOut.add(columnL_SV);
		String preString = "L";
//		int epsWidth = 1;
//		for (int i = 0; i < numBoxes; i++) {
//			table.add(new DoubleColumn(preString + "-" + (2*epsWidth+1) + "x" + (2*epsWidth+1)));
//			epsWidth = epsWidth * 2;
//		}
		for (int n = 0; n < numBoxes; n++) {
			tableOut.add(new DoubleColumn(preString + " " + (int)Math.round(Math.pow(2, n)) + "x" + (int)Math.round(Math.pow(2, n))));
		}
	}
	
	/** 
	*  writes current result to table
	*  @param int slice number of active image.
	*/
	private void writeSingleResultToTable(int sliceNumber) { 
	
		//int numBoxes   = spinnerInteger_NumBoxes;
		//int regMin     	      = spinnerInteger_RegMin;
		//int regMax     	      = spinnerInteger_RegMax;
		String methodType     = choiceRadioButt_MethodType;
		int pixelPercentage   = spinnerInteger_PixelPercentage;
		int accuracy 	      = spinnerInteger_NumAcurracy;
		int confidence        = spinnerInteger_NumConfidence;
		String colorModelType = choiceRadioButt_ColorModelType;	
		
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
	    int s = sliceNumber;	
		//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
		//fill table with values
		tableOut.appendRow();
		tableOut.set("File name",     tableOut.getRowCount() - 1, datasetName);	
		if (sliceLabels != null)   tableOut.set("Slice name", tableOut.getRowCount() - 1, sliceLabels[s]);
		tableOut.set("# Boxes",       tableOut.getRowCount()-1, numBoxes);	
		//tableOut.set("RegMin",        tableOut.getRowCount()-1, regMin);	
		//tableOut.set("RegMax",        tableOut.getRowCount()-1, regMax);
		if (choiceRadioButt_MethodType.equals("Tug of war")) {
			tableOut.set("Method type",   tableOut.getRowCount()-1, "Tug of war Acc" + accuracy + " Conf" + confidence);
		}
		else if (choiceRadioButt_MethodType.equals("Sliding box")) {
			tableOut.set("Method type",   tableOut.getRowCount()-1, "Sliding box "  +pixelPercentage + "%");
		}
		else {
			tableOut.set("Method type",   tableOut.getRowCount()-1, methodType);
		}
		tableOut.set("Color model",       tableOut.getRowCount()-1, colorModelType);
		tableOut.set("<L>-R&P",   		   tableOut.getRowCount()-1, resultValuesTable[s][resultValuesTable[s].length - 2]); //
		tableOut.set("<L>-S&V",   		   tableOut.getRowCount()-1, resultValuesTable[s][resultValuesTable[s].length - 1]); //last entry	
		tableColLast = 6;
		
		int numParameters = resultValuesTable[s].length - 2; 
		tableColStart = tableColLast + 1;
		tableColEnd = tableColStart + numParameters;
		for (int c = tableColStart; c < tableColEnd; c++ ) {
			tableOut.set(c, tableOut.getRowCount()-1, resultValuesTable[s][c-tableColStart]);
		}	
	}
	
	/** 
	*  Writes all results to table
	*/
	private void writeAllResultsToTable() {
	
		//int numBoxes       = spinnerInteger_NumBoxes;
		//int regMin            = spinnerInteger_RegMin;
		//int regMax            = spinnerInteger_RegMax;
		String methodType     = choiceRadioButt_MethodType;
		int accuracy 	      = spinnerInteger_NumAcurracy;
		int confidence        = spinnerInteger_NumConfidence;
		String colorModelType = choiceRadioButt_ColorModelType;	
		
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;

		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			tableOut.appendRow();
			tableOut.set("File name",     tableOut.getRowCount() - 1, datasetName);	
			if (sliceLabels != null)   tableOut.set("Slice name", tableOut.getRowCount() - 1, sliceLabels[s]);
			tableOut.set("# Boxes",       tableOut.getRowCount()-1, numBoxes);	
			//tableOut.set("RegMin",        tableOut.getRowCount()-1, regMin);	
			//tableOut.set("RegMax",        tableOut.getRowCount()-1, regMax);
			if (choiceRadioButt_MethodType.equals("Tug of war")) {
				tableOut.set("Method type",   tableOut.getRowCount()-1, "Tug of war Acc"+accuracy + " Conf"+confidence);
			} else {
				tableOut.set("Method type",   tableOut.getRowCount()-1, methodType);
			}
			tableOut.set("Color model",       tableOut.getRowCount()-1, colorModelType);	
			tableOut.set("<L>-R&P",   		   tableOut.getRowCount()-1, resultValuesTable[s][resultValuesTable[s].length - 2]); //
			tableOut.set("<L>-S&V",   	       tableOut.getRowCount()-1, resultValuesTable[s][resultValuesTable[s].length - 1]); //last entry	
			tableColLast = 6;
			
			int numParameters = resultValuesTable[s].length - 2; 
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + numParameters;
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableOut.set(c, tableOut.getRowCount()-1, resultValuesTable[s][c-tableColStart]);
			}	
		}
	}
							
	/** 
	 * Processing ****************************************************************************************
	 * */
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number
	
		//int numBoxes        = spinnerInteger_NumBoxes;
		//int regMin          = spinnerInteger_RegMin;
		//int regMax          = spinnerInteger_RegMax;
		String method         = choiceRadioButt_MethodType;
		int pixelPercentage   = spinnerInteger_PixelPercentage;
		String colorModelType = choiceRadioButt_ColorModelType;	 //"Binary"  "Grey"
		int accuracy 	      = spinnerInteger_NumAcurracy;
		int confidence        = spinnerInteger_NumConfidence;
		
		boolean optShowPlot = booleanShowDoubleLogPlot;
		
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);
		//ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		
		// data array
		double[]lacunarities = new double[numBoxes + 2]; //+2 because of weighted mean L and mean L
		int[]boxSizes        = new int[numBoxes];
		// definition of eps
		for (int n = 0; n < numBoxes; n++) boxSizes[n] = (int)Math.round(Math.pow(2, n));		
		for (int l = 0; l < lacunarities.length; l++) lacunarities[l] = Double.NaN;
		
		long number_of_points = 0;
		int max_random_number = (int) (100/pixelPercentage); // Evaluate max. random number
		if (method.equals("Raster box")) max_random_number = 1; //take always all boxes 
		int random_number = 0;
		//------------------------------------------------------------------------------------------
		if (method.equals("Raster box") || method.equals("Sliding box")) {	
			double mean = 0.0;
			double var = 0.0;
			//int epsWidth = 1;
			int boxSize = 0;
			double count = 0;
			int sample = 0;
			int delta = 0;
			ArrayList<Double> countList;
			if  (max_random_number == 1) { // no statistical approach, take all image pixels		
				//loop through box sizes, each box size gives a lacunarity
				for (int l = 0; l < numBoxes; l++) {		
					int proz = (int) (l + 1) * 95 / numBoxes;
					boxSize = boxSizes[l];
					countList = new ArrayList<Double>();
					mean = 0.0;
					var = 0.0;
					sample = 0;
					if      (method.equals("Raster box")) delta = boxSize;
					else if (method.equals("Sliding box")) delta = 1;
					//Raster through image
					for (int x = 0; x <= (width-boxSize); x = x+delta){
						for (int y = 0;  y <= (height-boxSize); y = y+delta){		
							raiBox = Views.interval(rai, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
							count = 0;
							// Loop through all pixels of this box.
							cursor = Views.iterable(raiBox).localizingCursor();
							while (cursor.hasNext()) { //Box
								cursor.fwd();
								//cursorF.localize(pos); 
								sample = ((UnsignedByteType) cursor.get()).get();
								if (sample > 0) {
									// Binary Image: 0 and [1, 255]! and not: 0 and 255
									if (colorModelType.equals("Binary")) count = count + 1.0;
									if (colorModelType.equals("Grey"))   count = count + sample;
								}								
							}//while Box
							countList.add(count);
							//if (colorModelType.equals("Binary")) countList.add(count);
							//if (colorModelType.equals("Grey"))   countList.add(count/(255*boxSize*boxSize));
						} //y	
					} //x
					//mean for this box size
					for (double c: countList) mean = mean + c;
					mean = mean/countList.size();
					
					//variance for this box size 
					for (double c: countList) var = (var + (c - mean) * (c - mean));
					var = var/countList.size();
					
					// calculate and set lacunarity value
					lacunarities[l] = var/ (mean * mean) + 1; // lacunarityboxSizes[l] = boxSize;// = epsWidth; = kernelSize;
				} 
			}
			else { //statistical approach
				//loop through box sizes, each box size gives a lacunarity
				for (int l = 0; l < numBoxes; l++) {		
					int proz = (int) (l + 1) * 95 / numBoxes;
					boxSize = boxSizes[l];
					countList = new ArrayList<Double>();
					mean = 0.0;
					var = 0.0;
					sample = 0;
					if      (method.equals("Raster box")) delta = boxSize;
					else if (method.equals("Sliding box")) delta = 1;
					//Raster through image
					for (int x = 0; x <= (width-boxSize); x = x+delta){
						for (int y = 0;  y <= (height-boxSize); y = y+delta){	
							random_number = (int) (Math.random()*max_random_number+1);
							if( random_number == 1 ){ // UPDATE 07.08.2013 
								raiBox = Views.interval(rai, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
								count = 0;
								// Loop through all pixels of this box.
								cursor = Views.iterable(raiBox).localizingCursor();
								while (cursor.hasNext()) { //Box
									cursor.fwd();
									//cursorF.localize(pos); 
									sample = ((UnsignedByteType) cursor.get()).get();
									if (sample > 0) {
										// Binary Image: 0 and [1, 255]! and not: 0 and 255
										if (colorModelType.equals("Binary")) count = count + 1.0;
										if (colorModelType.equals("Grey"))   count = count + sample;
									}								
								}//while Box
								countList.add(count);
								//if (colorModelType.equals("Binary")) countList.add(count);
								//if (colorModelType.equals("Grey"))   countList.add(count/(255*boxSize*boxSize));
							}
						} //y	
					} //x
					//mean for this box size
					for (double c: countList) mean = mean + c;
					mean = mean/countList.size();
					
					//variance for this box size 
					for (double c: countList) var = (var + (c - mean) * (c - mean));
					var = var/countList.size();
					
					// calculate and set lacunarity value
					lacunarities[l] = var/ (mean * mean) + 1; // lacunarity , sometimes + 1, sometimes not
				} 
			}
		}
		
		
		//------------------------------------------------------------------------------------------
		//this seems to be not accurate as it should be
		//maybe convolution of kernel at boundaries make problems
		else if (method.equals("Fast Sliding box")) {
			RectangleShape kernel;
			Runtime runtime = Runtime.getRuntime();
			long maxMemory = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("availabel processors: " + availableProcessors);
			
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			
			//create binary image with 0 and 1
			//later neighbors can be simply counted by dilation
			imgBin = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height); //always single 2D
			//uiService.show("imgBin1", imgBin);
			raBin = imgBin.randomAccess();
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			cursor = imgBin.localizingCursor();
			double sample = 0;
			int[] pos = new int[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				sample = (double)ra.get().getInteger();
				if (sample == 0.0) {
					((UnsignedByteType) cursor.get()).set(0);
				} else {
					if (colorModelType.equals("Binary")) ((UnsignedByteType) cursor.get()).set(1);
					if (colorModelType.equals("Grey"))   ((UnsignedByteType) cursor.get()).set((int)sample); //simply a copy
				}		
			}
			//uiService.show("imgBin2", imgBin);
			double mean = 0.0;
			double var = 0.0;
			int N = 0;
			int epsWidth = 1;
			//loop through box sizes, each box size gives a lacunarity
			for (int l = 0; l < numBoxes; l++) {
				
				int proz = (int) (l + 1) * 95 / numBoxes;
				//Compute dilated image
				int kernelSize = 2 * epsWidth + 1;
	//			kernel = new RectangleShape(kernelSize, false); //kernelSize x kernelSize skipCenter = true
	//			imgDil = (Img<UnsignedByteType>) Dilation.dilate(imgBin, kernel, numThreads);
				
				// create the averageing kernel
				Img<DoubleType> avgKernel = opService.create().img(new int[] {kernelSize, kernelSize});
				for (DoubleType k : avgKernel) {
					k.setReal(1.0);
				}
				imgDil = (Img<FloatType>) opService.filter().convolve(imgBin, avgKernel);
				
				//uiService.show("imgDil", imgDil);
				// each pixel value is now the sum of the box (neighborhood values)
				// get statistics
				mean = 0.0;
				var = 0.0;
				N = 0;
				sample = 0;
				
				// mean
				cursor = imgDil.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					sample = ((FloatType) cursor.get()).getRealFloat();//-1 subtract midpoint itself from number
					//if (sample > 0f) {
						N = N + 1;
						mean = mean + sample;
					//} 	
				}
				mean = mean/N;
			
				// variance
				sample = 0.0;
				cursor = imgDil.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					sample = ((FloatType) cursor.get()).getRealFloat();//-1 subtract midpoint itself from number
					//if (sample > 0f) {
						//N=N+1; //calculated already
						var = (var + (sample - mean) * (sample - mean));
					//} 	
				}
				var = var/N;
				// calculate and set lacunarity value
				lacunarities[l] = var/ (mean * mean) + 1; // lacunarity , sometimes + 1, sometimes not
				boxSizes[l] = kernelSize;// = epsWidth; = kernelSize;
				// System.out.println("Lacunarity:  mean: " +  mean + "      stddev: "+stddev );
		
				epsWidth = epsWidth * 2;
				// epsWidth = epsWidth+1;
			} // 0>=l<=numEps loop through eps
		}	
		//------------------------------------------------------------------------------------------
		else if (method.equals("Tug of war")) {	
			/**
			 * 
			 * Martin Reiss
			 * DESCRIPTION:
			 *  Die äquivalente Definition der Lacunarity aus [1] erlaubt es nun, eine neue Approximationsmethode 
			 *  für die Fixed-Grid Lacunarity vorzuschlagen. Das erste Moment bleibt für einen Fixed-Grid Scan bei den verschiedenen Boxgrö"sen konstant. 
			 *  Dies entspricht einfach der Gesamtzahl aller Objektpixel. Somit ist die Lacunarity nur mehr vom zweiten Moment der Besetzungszahlen abhängig. 
			 *  Dieses zweite Moment kann aber mittels der Tug of War Methode berechnet werden. Der neu vorgeschlagene Algorithmus ermöglicht es also 
			 *  die Fixed-Grid Lacunarity mittels Tug of War zu approximieren. 
			 *  Das beschriebene Verfahren soll in der Folge als Tug of War Lacunarity [2] bezeichnet werden.
			 *  
			 *  [1] Tolle C.R., McJunkin T.R., Gorsich D.J., An efficient implementation of the gliding box lacunarity algorithm.
			 *  [2] Masterthesis: Fraktale Dimension von Volumsdaten, Martin Reiß
			 * 
			 *  KNOWN PROBLEMS/OPEN QUESTIONS:
			 * 	1) Method not that fast as it's supposed to be.
			 * 	2) In 1D TugOfWar Lacunarity the result is very sensitive to the value of q. (Example: Regular Line) 
			 * 
			 *  UPDATES: MR 19.10.2016 elimination of negative values in getTotals() method according to Chaos 2016 paper
			 */	
			int s1 = accuracy;   // s1 = 30; Wang paper	//accuracy 	
			int s2 = confidence; // s2 = 5;	 Wang paper //confidence
			int q  = 10501;	//prime number
			
			double[] sum2   = new double[numBoxes];
			//double[] totals = new double[numBoxes];		
			double[] count  = new double[s1];
			double[] meanS1 = new double[s2];
			
			long L = 0;		
			double sum = 0.0;		
			boolean even;	
			int xx = 0;
			int yy = 0;
			int part1 = 0;
			int part2 = 0;
			int hashFunction = 0;
			int k = 0;

			double lac;
			int epsWidth = 1;
			int boxSize = 0;
			//for (int n = 0; n < numBoxes; n++) boxSizes[n] = (int)Math.pow(2, n);
			for (int n = 0; n < numBoxes; n++) lacunarities[n] = 0.0;
			
			// Count total number of points		
			int sample;
			cursor = Views.iterable(rai).localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				sample = ((UnsignedByteType) cursor.get()).getInteger();
				if (sample > 0) L = L + 1;		
			}

			// Get coordinates
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			int[] xCoordinate = new int[(int) L];
			int[] yCoordinate = new int[(int) L];	
			
			do {
				for(int i = 0; i < width; i++){	
					for(int j = 0; j < height; j++){
						ra.setPosition(i, 0);
						ra.setPosition(j, 1);
						if (ra.get().getInteger() > 0 ){	
							xCoordinate[k] = i;
							yCoordinate[k] = j;
							k++;
						}
					}
				}
			} while ( k < L );
			
			//TugOfWar - algorithm	
			for (int c = 0; c < count.length; c++) count[c] = 0.0;
			for (int s = 0; s < sum2.length; s++) sum2[s] = 0.0;
			sum = 0.0;
			//epsWidth = 1;
			if (L > 0) {
				for (int b = 0; b < numBoxes; b++) {	
						boxSize = boxSizes[b];
						//boxSize = 2 * epsWidth + 1;
						for(int s2_i = 0; s2_i < s2; s2_i++){
							for(int s1_i = 0; s1_i < s1; s1_i++){	
								
								int a_prim = getPrimeNumber();
								int b_prim = getPrimeNumber();
								int c_prim = getPrimeNumber();
								int d_prim = getPrimeNumber();
		
								for(int i = 0; i < L; i++){	
									xx = (int)Math.round(xCoordinate[i]/boxSize); 
									yy = (int)Math.round(yCoordinate[i]/boxSize); 
									// hash function 
									part1 = a_prim*xx*xx*xx + b_prim*xx*xx + c_prim*xx + d_prim;
									part2 = a_prim*a_prim*yy*yy*yy + b_prim*b_prim*yy*yy + c_prim*c_prim*yy + d_prim*d_prim;							
									hashFunction = (part1 + part2) % q; 
									even = (hashFunction & 1) == 0;							
									if (even) {
										count[s1_i]++;
									} else {
										count[s1_i]+=-1;
									}
								}
							} 
							for(int s1_i = 0; s1_i < s1; s1_i++){
								sum += count[s1_i]*count[s1_i]; 
							}				
							meanS1[s2_i] = sum / s1; 					
							sum = 0.0;
							for(int s1_i = 0; s1_i < s1; s1_i++){	
								count[s1_i] = 0;
							}	
						} 				
						for(int s2_i = 0; s2_i < s2; s2_i++){
							sum2[b] += meanS1[s2_i] / s2;	
						}							
						lac = (double)(sum2[b]/(L*L))*(width*height/(boxSize*boxSize));
						
						//Note that lacunarity (= log[totals]) is always positive.
//							if (lac < 1) {
//								lacunarities[b] =  + 1; //Martin did it so
//							} else {
//								lacunarities[b] = lac;								
//							}
						
						//Changed by Helmut
						lacunarities[b] = lac;	
						
						for(int s2_i = 0; s2_i < s2; s2_i++){	
							meanS1[s2_i] = 0;
						}
						//boxSizes[b] = boxSize;// = epsWidth; = kernelSize;
						//epsWidth = epsWidth * 2;
						// epsWidth = epsWidth+1;
				} //Box sizes
				
			} else {
				// Set totals to zero if image is empty.
				for (int b = 0; b < numBoxes; b++) lacunarities[b] =  0;	
			}		
		}//Tug of war
		//---------------------------------------------------------------------------------------------
		//Compute weighted mean Lacunarity according to Equ.3 of Roy&Perfect, 2014, Fractals, Vol22, No3
		//DOI: 10.1142/S0218348X14400039
		double L_RP = 0.0;
		double sumBoxSize = 0.0;
		for (int n = 0; n < numBoxes; n++) {	
			L_RP = L_RP + Math.log10(lacunarities[n]) * Math.log10(boxSizes[n]);
			sumBoxSize = sumBoxSize + Math.log10(boxSizes[n]);
		}
		lacunarities[numBoxes] = L_RP/sumBoxSize; //append it to the list of lacunarities	
		
		//Compute mean Lacunarity according to Equ.29 of Sengupta and Vinoy, 2006, Fractals, Vol14 No4 p271-282
		//DOI: 10.1142/S0218348X06003313 
		double L_SV = 0.0;
		for (int n = 0; n < numBoxes; n++) {	
			L_SV = L_SV + lacunarities[n] * boxSizes[n];
		}
		lacunarities[numBoxes + 1] = Math.log(L_SV/(boxSizes[numBoxes-1] - boxSizes[0])); //append it to the list of lacunarities	
			
		//prepare plot
		double[] lnDataX = new double[numBoxes];
		double[] lnDataY = new double[numBoxes];
		for (int n = 0; n < numBoxes; n++) {	
				if (lacunarities[n] == 0) {
					lnDataY[n]= Math.log(Float.MIN_VALUE);
				} else {
					lnDataY[n] = Math.log(lacunarities[n]);
				}
				lnDataX[n] = Math.log(boxSizes[n]);
				// System.out.println("Lacunarity: " );	
		}
			
		//Create double log plot
		boolean isLineVisible = false; //?
		if (optShowPlot) {			
			String preName = "";
			if (numSlices > 1) {
				preName = "Slice-"+String.format("%03d", plane) +"-";
			}
			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plot - Lacunarity", 
					preName + datasetName, "ln(Box size)", "ln(L)", "",
					1, numBoxes);
			doubleLogPlotList.add(doubleLogPlot);
		}
		
		// Compute regression
		//LinearRegression lr = new LinearRegression();
		//regressionParams = lr.calculateParameters(lnDataX, lnDataY, regMin, regMax);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
	
		return lacunarities;
		//Output
		//uiService.show(tableOutName, table);
		//result = ops.create().img(image, new FloatType());
		//table
	}//
	//-------------------------------------------------------------------------------------------
	public int getPrimeNumber(){
		
		int[] primeNumbers = {
			  46349,48533,44123,
			  46351,48539,44129,
			  46381,48541,44131,
			  46399,48563,44159,
			  46411,48571,44171,
			  46439,48589,44179,
			  46441,48593,44189,
			  46447,48611,44201,
			  46451,48619,44203,
			  46457,48623,44207,
			  46471,48647,44221,
			  46477,48649,44249,
			  46489,48661,44257,
			  46499,48673,44263,
			  46507,48677,44267,
			  46511,48679,44269,
			  46523,48731,44273,
			  46549,48733,44279,
			  46559,48751,44281,
			  46567,48757,44293,
			  46573,48761,44351,
			  46589,48767,44357,
			  46591,48779,44371,
			  46601,48781,44381,
			  46619,48787,44383,
			  46633,48799,44389,
			  46639,48809,44417,
			  46643,48817,44449,
			  46649,48821,44453,
			  46663,48823,44483,
			  46679,48847,44491,
			  46681,48857,44497,
			  46687,48859,44501,
			  46691,48869,44507,
			  46703,48871,44519,
			  46723,48883,44531,
			  46727,48889,44533,
			  46747,48907,44537,
			  46751,48947,44543,
			  46757,48953,44549,
			  46769,48973,44563,
			  46771,48989,44579,
			  46807,48991,44587,
			  46811,49003,44617,
			  46817,49009,44621,
			  46819,49019,44623,
			  46829,49031,44633,
			  46831,49033,44641,
			  46853,49037,44647,
			  46861,49043,44651,
			  46867,49057,44657,
			  46877,49069,44683,
			  46889,49081,44687,
			  46901,49103,44699,
			  46919,49109,44701,
			  46933,49117,44711,
			  46957,49121,44729,
			  46993,49123,44741,
			  46997,49139,44753,
			  47017,49157,44771,
			  47041,49169,44773,
			  47051,49171,44777,
			  47057,49177,44789,
			  47059,49193,44797,
			  47087,49199,44809,
			  47093,49201,44819,
			  47111,49207,44839,
			  47119,49211,44843,
			  47123,49223,44851,
			  47129,49253,44867,
			  47137,49261,44879,
			  47143,49277,44887,
			  47147,49279,44893,
			  47149,49297,44909,
			  47161,49307,44917,
			  47189,49331,44927,
			  47207,49333,44939,
			  47221,49339,44953,
			  47237,49363,44959,
			  47251,49367,44963,
			  47269,49369,44971,
			  47279,49391,44983,
			  47287,49393,44987,
			  47293,49409,45007,
			  47297,49411,45013,
			  47303,49417,45053,
			  47309,49429,45061,
			  47317,49433,45077,
			  47339,49451,45083,
			  47351,49459,45119,
			  47353,49463,45121,
			  47363,49477,45127,
			  47381,49481,45131,
			  47387,49499,45137,
			  47389,49523,45139,
			  47407,49529,45161,
			  47417,49531,45179,
			  47419,49537,45181,
			  47431,49547,45191,
			  47441,49549,45197,
			  47459,49559,45233,
			  47491,49597,45247,
			  47497,49603,45259,
			  47501,49613,45263,
			  47507,49627,45281,
			  47513,49633,45289,
			  47521,49639,45293,
			  47527,49663,45307,
			  47533,49667,45317,
			  47543,49669,45319,
			  47563,49681,45329,
			  47569,49697,45337,
			  47581,49711,45341,
			  47591,49727,45343,
			  47599,49739,45361,
			  47609,49741,45377,
			  47623,49747,45389,
			  47629,49757,45403,
			  47639,49783,45413,
			  47653,49787,45427,
			  47657,49789,45433,
			  47659,49801,45439,
			  47681,49807,45481,
			  47699,49811,45491,
			  47701,49823,45497,
			  47711,49831,45503,
			  47713,49843,45523,
			  47717,49853,45533,
			  47737,49871,45541,
			  47741,49877,45553,
			  47743,49891,45557,
			  47777,49919,45569,
			  47779,49921,45587,
			  47791,49927,45589,
			  47797,49937,45599,
			  47807,49939,45613,
			  47809,49943,45631,
			  47819,49957,45641,
			  47837,49991,45659,
			  47843,49993,45667,
			  47857,49999,45673,
			  47869,50021,45677,
			  47881,50023,45691,
			  47903,50033,45697,
			  47911,50047,45707,
			  47917,50051,45737,
			  47933,50053,45751,
			  47939,50069,45757,
			  47947,50077,45763,
			  47951,50087,45767,
			  47963,50093,45779,
			  47969,50101,45817,
			  47977,50111,45821,
			  47981,50119,45823,
			  48017,50123,45827,
			  48023,50129,45833,
			  48029,50131,45841,
			  48049,50147,45853,
			  48073,50153,45863,
			  48079,50159,45869,
			  48091,50177,45887,
			  48109,50207,45893,
			  48119,50221,45943,
			  48121,50227,45949,
			  48131,50231,45953,
			  48157,50261,45959,
			  48163,50263,45971,
			  48179,50273,45979,
			  48187,50287,45989,
			  48193,50291,46021,
			  48197,50311,46027,
			  48221,50321,46049,
			  48239,50329,46051,
			  48247,50333,46061,
			  48259,50341,46073,
			  48271,50359,46091,
			  48281,50363,46093,
			  48299,50377,46099,
			  48311,50383,46103,
			  48313,50387,46133,
			  48337,50411,46141,
			  48341,50417,46147,
			  48353,50423,46153,
			  48371,50441,46171,
			  48383,50459,46181,
			  48397,50461,46183,
			  48407,50497,46187,
			  48409,50503,46199,
			  48413,50513,46219,
			  48437,50527,46229,
			  48449,50539,46237,
			  48463,50543,46261,
			  48473,50549,46271,
			  48479,50551,46273,
			  48481,50581,46279,
			  48487,50587,46301,
			  48491,50591,46307,
			  48497,50593,46309,
			  48523,50599,46327,
			  48527,50627,44119
			};
		
		int randn = (int) (Math.random()*500);
		return primeNumbers[randn]; 
		
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
			ra.setPosition(pos);
			//ra.setPosition(pos[0], 0);
			//ra.setPosition(pos[1], 1);
			ra.get().setReal(cursor.get().get());
		}  			
		uiService.show(name, datasetDisplay);
	}
	
	/**
	 * Displays a regression plot in a separate window.
	 * <p>
	 *		
	 *
	 * </p>
	 * 
	 * @param dataX data values for x-axis.
	 * @param dataY data values for y-axis.
	 * @param isLineVisible option if regression line is visible
	 * @param frameTitle title of frame
	 * @param plotLabel  label of plot
	 * @param xAxisLabel label of x-axis
	 * @param yAxisLabel label of y-axis
	 * @param regMin minimum value for regression range
	 * @param regMax maximal value for regression range 
	 * @param optDeleteExistingPlot option if existing plot should be deleted before showing a new plot
	 * @param interpolType The type of interpolation
	 * @return RegressionPlotFrame
	 */			
	private RegressionPlotFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel, int regMin, int regMax) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, regMin, regMax);
		pl.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pl.pack();
		// int horizontalPercent = 5;
		// int verticalPercent = 5;
		// RefineryUtilities.positionFrameOnScreen(pl, horizontalPercent,
		// verticalPercent);
		//CommonTools.centerFrameOnScreen(pl);
		pl.setVisible(true);
		return pl;	
	}
	
	
	
	/**
	 * 
	 * This methods creates a Img<FloatType>
	 */
	private Img<FloatType > createImgFloat(RandomAccessibleInterval<?> rai){ //rai must always be a single 2D plane
		
		imgFloat = new ArrayImgFactory<>(new FloatType()).create(rai.dimension(0), rai.dimension(1)); //always single 2D
		Cursor<FloatType> cursor = imgFloat.localizingCursor();
		final long[] pos = new long[imgFloat.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			//if (numSlices == 1) { //for only one 2D image;
			//	ra.setPosition(pos[0], 0);
			//	ra.setPosition(pos[1], 1);
			//} else { //for more than one image e.g. image stack
			//	ra.setPosition(pos[0], 0);
			//	ra.setPosition(pos[1], 1);
			//	ra.setPosition(s, 2);
			//}
			//ra.get().setReal(cursor.get().get());
			cursor.get().setReal(ra.get().getRealFloat());
		}	
		return imgFloat;
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
		//ij.command().run(Img2DLacunarity.class, true).get().getOutput("image");
		ij.command().run(Img2DLacunarity.class, true);
	}
}

