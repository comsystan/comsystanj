/*-
 * #%L
 * Project: ImageJ2 plugin for computing the Generalized fractal dimensions
 * File: Img2DFractalDimensionGeneralized.java
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

package at.csa.csaj.img2d.frac.dim.generalized;

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
import at.csa.csaj.commons.plot.SignalPlotFrame;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.regression.LinearRegression;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link ContextCommand} plugin computing
 * <a>the generalized fractal dimensions</a>
 * of an image.
 */
@Plugin(type = ContextCommand.class, 
        headless = true,
        label = "Generalized dimensions",
        initializer = "initialPluginLaunch",
        //iconPath = "/images/comsystan-??.png", //Menu entry icon
        menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (2D)"),
        @Menu(label = "Generalized dimensions", weight = 8)})
//public class Img2DFractalDimensionGeneralized<T extends RealType<T>> extends InteractiveCommand { //non blocking GUI
public class Img2DFractalDimensionGeneralized<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes Generalized fractal dimensions</b></html>";
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
	private static RandomAccess<?> ra;
	private static Cursor<?> cursor = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static int  numBoxes = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static ArrayList<SignalPlotFrame> genDimPlotList       = new ArrayList<SignalPlotFrame>();
	private static ArrayList<SignalPlotFrame> fSpecPlotList        = new ArrayList<SignalPlotFrame>();
	private static double[][][] resultValuesTable; //First column is q, then the image index, second column are the corresponding regression values
	private static final String tableOutName = "Table - Generalized dimensions";
	
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
    		   description = "Number of distinct radii following the power of 2",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialNumBoxes",
	           callback    = "callbackNumBoxes")
    private int spinnerInteger_NumBoxes;
    
    @Parameter(label = "Regression Min",
    		   description = "Minimum x value of linear regression",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       stepSize = "1",
 		       //persist  = false,   //restore previous value default = true
 		       initializer = "initialRegMin",
 		       callback = "callbackRegMin")
    private int spinnerInteger_RegMin = 1;
 
    @Parameter(label = "Regression Max",
    		   description = "Maximum x value of linear regression",
    		   style = NumberWidget.SPINNER_STYLE,
		       min = "3",
		       max = "32768",
		       stepSize = "1",
		       persist  = false,   //restore previous value default = true
		       initializer = "initialRegMax",
		       callback = "callbackRegMax")
     private int spinnerInteger_RegMax = 3;
        
//     @Parameter(label = "Box size distribution",
//		    description = "linear or logarithm distribution of box sizes",
//		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
//  		    choices = {"Lin", "Log"},
//  		    //persist  = false,  //restore previous value default = true
//		    initializer = "initialBoxSizeDistribution",
//            callback = "callbackBoxSizeDistribution")
//     private String choiceRadioButt_BoxSizeDistribution;
	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelMethodOptions = METHODOPTIONS_LABEL;
     
     @Parameter(label = "Min q",
  		   description = "Number of minimal q",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "-32768",
	           max = "32768",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialNumMinQ",
	           callback    = "callbackNumMinQ")
     private int spinnerInteger_NumMinQ;
     
     @Parameter(label = "Max q",
    		   description = "Number of maximal q",
  	       	   style = NumberWidget.SPINNER_STYLE,
  	           min = "-32768",
  	           max = "32768",
  	           stepSize = "1",
  	           persist  = false,  //restore previous value default = true
  	           initializer = "initialNumMaxQ",
  	           callback    = "callbackNumMaxQ")
     private int spinnerInteger_NumMaxQ;
     
     @Parameter(label = "Scanning type",
 		    description = "Fixed raster boxes or sliding boxes",
 		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
   		    choices = {"Raster box", "Sliding box"}, //"Fast sliding box"}, //Fast sliding box with image dilation does not work properly
   		    //persist  = false,  //restore previous value default = true
 		    initializer = "initialScanningType",
             callback = "callbackScanningType")
     private String choiceRadioButt_ScanningType;
     
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
     
 	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
      
     @Parameter(label = "Show double log plot",
    		    //persist  = false,  //restore previous value default = true
  		        initializer = "initialShowDoubleLogPlots")
	 private boolean booleanShowDoubleLogPlot;
       
     @Parameter(label = "Show Dq plot",
  		    //persist  = false,  //restore previous value default = true
 		        initializer = "initialShowDqPlot")
 	 private boolean booleanShowDqPlot;
      
      @Parameter(label = "Show F spectrum",
  		    //persist  = false,  //restore previous value default = true
 		        initializer = "initialShowFSpectrum")
 	 private boolean booleanShowFSpectrum;
     
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
// 		     callback = "callbackProcessAllImages")
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
    protected void initialRegMin() {
    	spinnerInteger_RegMin = 1;
    }
    protected void initialRegMax() {
    	numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
    	spinnerInteger_RegMax =  numBoxes;
    }
//    protected void initialBoxSizeDistribution() {
//    	choiceRadioButt_BoxSizeDistribution = "Lin";
//    }
    protected void initialNumMinQ() {
      	spinnerInteger_NumMinQ = -5;
    }
    protected void initialNumMaxQ() {
      	spinnerInteger_NumMaxQ = 5;
    }
    protected void initialScanningType() {
    	choiceRadioButt_ScanningType = "Raster box";
    }
    protected void initialColorModelType() {
    	choiceRadioButt_ColorModelType = "Binary";
    }
    protected void initialPixelPercentage() {
      	spinnerInteger_PixelPercentage = 10;
    }
    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
    protected void initialShowDqPlot() {
    	booleanShowDqPlot = true;
    }
    protected void initialShowFSpectrum() {
    	booleanShowFSpectrum = true;
    }
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}

	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #spinnerInteger_NumBoxes} parameter changes. */
	protected void callbackNumBoxes() {
		
		if  (spinnerInteger_NumBoxes < 3) {
			spinnerInteger_NumBoxes = 3;
		}
		int numMaxBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));	
		if (spinnerInteger_NumBoxes > numMaxBoxes) {
			spinnerInteger_NumBoxes = numMaxBoxes;
		};
		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
		}
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}

		numBoxes = spinnerInteger_NumBoxes;
		logService.info(this.getClass().getName() + " Number of boxes set to " + spinnerInteger_NumBoxes);
	}
	/** Executed whenever the {@link #spinnerInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if(spinnerInteger_RegMin < 1) {
			spinnerInteger_RegMin = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}
	
	/** Executed whenever the {@link #spinnerInteger_RegMax} parameter changes. */
	protected void callbackRegMax() {
		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
		}		
		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_RegMax);
	}

//	/** Executed whenever the {@link #choiceRadioButt_BoxSizeDistribution} parameter changes. */
//	protected void callbackBoxSizeDistribution() {
//		logService.info(this.getClass().getName() + " Box size distribution set to " + choiceRadioButt_BoxSizeDistribution);
//		
//	}
	 /** Executed whenever the {@link #spinnerInteger_NumMinQ} parameter changes. */
		protected void callbackNumMinQ() {
			if (spinnerInteger_NumMinQ >= spinnerInteger_NumMaxQ) {
				spinnerInteger_NumMinQ  = spinnerInteger_NumMaxQ - 1;
			}
			logService.info(this.getClass().getName() + " Minimal q set to " + spinnerInteger_NumMinQ);
		}
		  /** Executed whenever the {@link #spinnerInteger_NumMaxQ} parameter changes. */
			protected void callbackNumMaxQ() {
				if (spinnerInteger_NumMaxQ <= spinnerInteger_NumMinQ) {
					spinnerInteger_NumMaxQ  = spinnerInteger_NumMinQ + 1;
				}
				logService.info(this.getClass().getName() + " Minimal q set to " + spinnerInteger_NumMinQ);
			}
	/** Executed whenever the {@link #choiceRadioButt_ScanningType} parameter changes. */
	protected void callbackScanningType() {
		logService.info(this.getClass().getName() + " Scanning method set to " + choiceRadioButt_ScanningType);
		
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
	protected void callbackColorModelType() {
		logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
	}
	/** Executed whenever the {@link #spinnerInteger_PixelPercentage} parameter changes. */
	protected void callbackPixelPercentage() {
		logService.info(this.getClass().getName() + " Pixel % set to " + spinnerInteger_PixelPercentage);
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
		compositeChannelCount = datasetIn.getCompositeChannelCount();
		if ((numDimensions == 2) && (compositeChannelCount == 1)) { //single Grey image
			numSlices = 1;
			imageType = "Grey";
		} else if ((numDimensions == 3) && (compositeChannelCount == 1)) { // Grey stack	
			numSlices = datasetIn.dimension(2); //x,y,z
			imageType = "Grey";
		} else if ((numDimensions == 3) && (compositeChannelCount == 3)) { //Single RGB image	
			numSlices = 1;
			imageType = "RGB";
		} else if ((numDimensions == 4) && (compositeChannelCount == 3)) { // RGB stack	x,y,composite,z
			numSlices = datasetIn.dimension(3); //x,y,composite,z
			imageType = "RGB";
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
		logService.info(this.getClass().getName() + " Name: " + datasetName); 
		logService.info(this.getClass().getName() + " Image size: " + width+"x"+height); 
		logService.info(this.getClass().getName() + " Image type: " + imageType); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
		
		//RGB not allowed
		if (!imageType.equals("Grey")) { 
			logService.info(this.getClass().getName() + " WARNING: Grey value image(s) expected!");
			this.cancel("WARNING: Grey value image(s) expected!");
		}
	}
	
	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleImage() {
		
		dlgProgress = new WaitingDialogWithProgressBar("Computing Generalized dimensions, please wait... Open console window for further info.",
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
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing Generalized dimensions, please wait... Open console window for further info.",
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
		if (optDeleteExistingPlots){
//			//This dose not work with DisplayService because the JFrame is not "registered" as an ImageJ display	
			if (doubleLogPlotList != null) {
				for (int l = 0; l < doubleLogPlotList.size(); l++) {
					doubleLogPlotList.get(l).setVisible(false);
					doubleLogPlotList.get(l).dispose();
					//doubleLogPlotList.remove(l);  /
				}
				doubleLogPlotList.clear();		
			}
			if (genDimPlotList != null) {
				for (int l = 0; l < genDimPlotList.size(); l++) {
					genDimPlotList.get(l).setVisible(false);
					genDimPlotList.get(l).dispose();
					//genDimPlotList.remove(l);  /
				}
				genDimPlotList.clear();		
			}
			if (fSpecPlotList != null) {
				for (int l = 0; l < fSpecPlotList.size(); l++) {
					fSpecPlotList.get(l).setVisible(false);
					fSpecPlotList.get(l).dispose();
					//fSpecPlotList.remove(l);  /
				}
				fSpecPlotList.clear();		
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
		int numMinQ = this.spinnerInteger_NumMinQ;
		int numMaxQ = this.spinnerInteger_NumMaxQ;
		int numQ = numMaxQ - numMinQ + 1;
		resultValuesTable = new double[numQ][(int) numSlices][10];
		
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//mg<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
		
		}

		//Compute regression parameters
		double[][] regressionValues = process(rai, s);	
			//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		//set values for output table
		for (int q = 0; q < numQ; q++) {
			for (int i = 0; i < regressionValues[0].length; i++ ) {
					resultValuesTable[q][s][i] = regressionValues[q][i]; 
			}
			//Compute dimension
			double dim = Double.NaN;
			if ((q + numMinQ) == 1) dim = regressionValues[q][1]; //Slope
			else                    dim = regressionValues[q][1]/(q + numMinQ - 1);
			resultValuesTable[q][s][1] = dim;
		} //q
		
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
		int numMinQ = this.spinnerInteger_NumMinQ;
		int numMaxQ = this.spinnerInteger_NumMaxQ;
		int numQ = numMaxQ - numMinQ + 1;
		resultValuesTable = new double[numQ][(int) numSlices][10];
	
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
				//Compute regression parameters
				double[][] regressionValues = process(rai, s);	
				//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
				
				//set values for output table
				for (int q = 0; q < numQ; q++) {
					for (int i = 0; i < regressionValues[0].length; i++ ) {
						resultValuesTable[q][s][i] = regressionValues[q][i]; 
					}
					//Compute dimension
					double dim = Double.NaN;
					if ((q + numMinQ) == 1) dim = regressionValues[q][1]; //Slope
					else                    dim = regressionValues[q][1]/(q + numMinQ - 1);
					resultValuesTable[q][s][1] = dim;
				}//q
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
		IntColumn columnRegMin             = new IntColumn("RegMin");
		IntColumn columnRegMax             = new IntColumn("RegMax");
		IntColumn columnNumMinQ            = new IntColumn("Min q");
		IntColumn columnNumMaxQ            = new IntColumn("Max q");
		GenericColumn columnScanningType   = new GenericColumn("Scanning type");
		GenericColumn columnColorModelType = new GenericColumn("Color model");
		IntColumn columnPixelPercentage    = new IntColumn("(Sliding box) Pixel %");
	
	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
		tableOut.add(columnMaxNumBoxes);
		tableOut.add(columnRegMin);
		tableOut.add(columnRegMax);
		tableOut.add(columnNumMinQ);
		tableOut.add(columnNumMaxQ);
		tableOut.add(columnScanningType);
		tableOut.add(columnColorModelType);
		tableOut.add(columnPixelPercentage);
		
		int numMinQ = spinnerInteger_NumMinQ;
		int numMaxQ = spinnerInteger_NumMaxQ;
		int numQ = numMaxQ - numMinQ + 1;
		for (int q = 0; q < numQ; q++) {
			tableOut.add(new GenericColumn("D" + (numMinQ + q)));
		}
	}
	
	/** 
	*  writes current result to table
	*  @param int slice number of active image.
	*/
	private void writeSingleResultToTable(int sliceNumber) {
	
		int numBoxes          = spinnerInteger_NumBoxes;
		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		int numMinQ           = spinnerInteger_NumMinQ;
		int numMaxQ           = spinnerInteger_NumMaxQ;
		int pixelPercentage   = spinnerInteger_PixelPercentage;
		String scanningType   = choiceRadioButt_ScanningType;
		String colorModelType = choiceRadioButt_ColorModelType;	
		
	    int s = sliceNumber;	
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			tableOut.appendRow();
			tableOut.set("File name",   	 tableOut.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) 	 tableOut.set("Slice name", tableOut.getRowCount() - 1, sliceLabels[s]);
			tableOut.set("# Boxes",    	 tableOut.getRowCount()-1, numBoxes);	
			tableOut.set("RegMin",      	 tableOut.getRowCount()-1, regMin);
			tableOut.set("RegMax",      	 tableOut.getRowCount()-1, regMax);
			tableOut.set("Min q",      	 tableOut.getRowCount()-1, numMinQ);	
			tableOut.set("Max q",      	 tableOut.getRowCount()-1, numMaxQ);	
			tableOut.set("Scanning type",   tableOut.getRowCount()-1, scanningType);
			tableOut.set("Color model",     tableOut.getRowCount()-1, colorModelType);
			if (scanningType.equals("Sliding box")) tableOut.set("(Sliding box) Pixel %", tableOut.getRowCount()-1, pixelPercentage);	
			
			int numQ = numMaxQ - numMinQ + 1;
			for (int q = 0; q < numQ; q++) {
				tableOut.set("D" + (numMinQ + q), tableOut.getRowCount()-1, resultValuesTable[q][s][1]);
			}	
	}
	
	/** 
	*  Writes all results to table
	*/
	private void writeAllResultsToTable() {
	
		int numBoxes          = spinnerInteger_NumBoxes;
		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		int numMinQ           = spinnerInteger_NumMinQ;
		int numMaxQ           = spinnerInteger_NumMaxQ;
		int pixelPercentage   = spinnerInteger_PixelPercentage;
		String scanningType   = choiceRadioButt_ScanningType;	
		String colorModelType = choiceRadioButt_ColorModelType;	
		
		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			tableOut.appendRow();
			tableOut.set("File name",	   	 tableOut.getRowCount() - 1, datasetName);	
			if (sliceLabels != null)	 tableOut.set("Slice name", tableOut.getRowCount() - 1, sliceLabels[s]);
			tableOut.set("# Boxes",    	 tableOut.getRowCount()-1, numBoxes);	
			tableOut.set("RegMin",      	 tableOut.getRowCount()-1, regMin);	
			tableOut.set("RegMax",      	 tableOut.getRowCount()-1, regMax);	
			tableOut.set("Min q",      	 tableOut.getRowCount()-1, numMinQ);	
			tableOut.set("Max q",      	 tableOut.getRowCount()-1, numMaxQ);	
			tableOut.set("Scanning type",   tableOut.getRowCount()-1, scanningType);
			tableOut.set("Color model",     tableOut.getRowCount()-1, colorModelType);
			if (scanningType.equals("Sliding box")) tableOut.set("(Sliding box) Pixel %", tableOut.getRowCount()-1, pixelPercentage);	
			
			int numQ = numMaxQ - numMinQ + 1;
			for (int q = 0; q < numQ; q++) {
				tableOut.set("D" + (numMinQ + q), tableOut.getRowCount()-1, resultValuesTable[q][s][1]);
			}			
		}
	}
							
	/** 
	 * Processing ****************************************************************************************
	 * 
	 * */
	private double[][] process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number

		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numBoxes                 = spinnerInteger_NumBoxes;
		int regMin                   = spinnerInteger_RegMin;
		int regMax                   = spinnerInteger_RegMax;
		//String boxSizeDistribution = this.choiceRadioButt_BoxSizeDistribution;
		int numMinQ                  = spinnerInteger_NumMinQ;
		int numMaxQ                  = spinnerInteger_NumMaxQ;
		String scanningType          = choiceRadioButt_ScanningType;	 
		String colorModelType        = choiceRadioButt_ColorModelType;	
		int pixelPercentage          = spinnerInteger_PixelPercentage;
		boolean optShowDoubleLogPlot = booleanShowDoubleLogPlot;
		boolean optShowDqPlot        = booleanShowDqPlot;
		boolean optShowFSpectrum     = booleanShowFSpectrum;
	
		int numBands = 1;
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "8-bit";  //  "RGB"....
	
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		
		int numQ = numMaxQ - numMinQ + 1;
		double[][] regressionParams = new double[numQ][5];
		double[][][] totals         = new double[numQ][numBoxes][numBands];
		double[][] totalsMax        = new double[numBoxes][numBands]; //for binary images
		int[] eps = new int[numBoxes];
		
		// definition of eps
		for (int n = 0; n < numBoxes; n++) {
			//for (int b = 0; b < numBands; b++) {	
				eps[n] = (int)Math.round(Math.pow(2, n));
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n][b]);	
			//}
		}		
		// set eps values
//		if (boxSizeDistribution.equals("Lin")) { // linear distributed
//			for (int i = 0; i < numBoxes; i++) {
//				eps[i] = Math.round((i + 1) / (float) numBoxes * numBoxes);
//				System.out.println("IqmOpFracGenDim: i, eps[i]: " + i + "   " + eps[i]);
//			}
//		}
//		else if (boxSizeDistribution.equals("Log")) { // log distributed
//			// eps[0] = 1;
//			for (int i = 0; i < numBoxes; i++) {
//				eps[i] = (int) Math.round(Math.exp((i + 1)
//						* Math.log(regMax) / numBoxes));
//				if (i == 0) {
//					if (eps[0] == 0)
//						eps[0] = 1;
//				} else {
//					if (eps[i] <= eps[i - 1])
//						eps[i] = eps[i - 1] + 1;
//				}
//				System.out.println(" i, eps[i]: " + i + "   " + eps[i]);
//			}
//		}
		
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255	
		
		if ((scanningType.equals("Raster box")) || (scanningType.equals("Sliding box"))) {
			//long numObjectPixels; // number of pixel > 0
			//numObjectPixels = this.getNumberOfNonZeroPixels(rai);
			long number_of_points = 0;
			int max_random_number = (int) (100/pixelPercentage); // Evaluate max. random number
			if (scanningType.equals("Raster box")) max_random_number = 1; //take always all boxes 
			int random_number = 0;
			//int radius;		
			double count = 0.0;
			int sample = 0;
			int delta = 0;
			int boxSize = 0;;	
				
			if  (max_random_number == 1) { // no statistical approach, take all image pixels		
				for (int b = 0; b < numBands; b++) {
					for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes		
						boxSize = eps[n];
						if (scanningType.equals("Raster box"))  delta = boxSize;
						if (scanningType.equals("Sliding box")) delta = 1;
						for (int x =0; x <= (width-boxSize); x = x+delta){
							for (int y =0;  y <= (height-boxSize); y = y+delta){
								count = 0;
								raiBox = Views.interval(rai, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
								// Loop through all pixels of this box.
								cursor = Views.iterable(raiBox).localizingCursor();
								while (cursor.hasNext()) { //Box
									cursor.fwd();
									//cursor.localize(pos);
									sample = ((UnsignedByteType) cursor.get()).get();
									if ( sample > 0) {
										if      (colorModelType.equals("Binary")) count = count + 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
										else if (colorModelType.equals("Grey"))   count = count + sample;
									}			
								}//while Box
								//count = count / numObjectPixels; // normalized mass of current box
								totalsMax[n][b] = totalsMax[n][b] + count; // calculate total count for normalization
								// System.out.println("IqmOpFracGendim: b: "+b+ "   count: "+ count );
								if (count > 0) {
									for (int q = 0; q < numQ; q++) {
										if ((q + numMinQ) == 1) totals[q][n][b] = totals[q][n][b] + count * Math.log(count); // GenDim
										else                    totals[q][n][b] = totals[q][n][b] + Math.pow(count, (q + numMinQ)); // GenDim
									}
								}
							} //y	
						} //x                                          
					} //n
				}//b band
			} // no statistical approach
			else { //statistical approach
				for (int b = 0; b < numBands; b++) {
					for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes		
						//radius = eps[n];	
						boxSize = eps[n];
						if (scanningType.equals("Raster box"))  delta = boxSize;
						if (scanningType.equals("Sliding box")) delta = 1;
						for (int x = 0; x <= (width-boxSize); x = x+delta){
							for (int y = 0;  y <= (height-boxSize); y = y+delta){		
								random_number = (int) (Math.random()*max_random_number+1);
								if( random_number == 1 ){ // UPDATE 07.08.2013 
									count = 0;
									raiBox = Views.interval(rai, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
									// Loop through all pixels of this box.
									cursor = Views.iterable(raiBox).localizingCursor();
									while (cursor.hasNext()) { //Box
										cursor.fwd();
										//cursor.localize(pos);
										sample = ((UnsignedByteType) cursor.get()).get();
										if ( sample > 0) {
											if      (colorModelType.equals("Binary")) count = count + 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
											else if (colorModelType.equals("Grey"))   count = count + sample;
										}			
									}//while Box
									//count = count / numObjectPixels; // normalized mass of current box
									totalsMax[n][b] = totalsMax[n][b] + count; // calculate total count for normalization
									// System.out.println("IqmOpFracGendim: b: "+b+ "   count: "+ count );
									if (count > 0) {
										for (int q = 0; q < numQ; q++) {
											if ((q + numMinQ) == 1)
												totals[q][n][b] = totals[q][n][b] + count * Math.log(count); // GenDim
											else
												totals[q][n][b] = totals[q][n][b] + Math.pow(count, (q + numMinQ)); // GenDim
										}
									}
								}
							} //y	
						} //x  
					} //n Box sizes		
				}//b band
			}
		}
		//Fast sliding box does not work properly
		//for some kernel sizes 5x5 it produces for  negative q strange results
		//e.g. for Menger carpet
		//and largest kernels are larger than image itself! 
		else if (scanningType.equals("Fast sliding box")) {
			RectangleShape kernel;
			Runtime runtime = Runtime.getRuntime();
			long maxMemory = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("Available processors: " + availableProcessors);
			
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			
			//create binary image with 0 and 1
			//later neighbors can be simply counted by dilation
			imgBin = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height); //always single 2D
			//uiService.show("imgBin1", imgBin);
			raBin = imgBin.randomAccess();
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			cursor = imgBin.localizingCursor();
			double sample = 0.0;
			double count = 0.0;
			int[] pos = new int[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				sample = (double)((UnsignedByteType) ra.get()).getInteger();
				if (sample == 0.0) {
					((UnsignedByteType) cursor.get()).set(0);
				} else {
					//((UnsignedByteType) cursor.get()).set(1);
					if (colorModelType.equals("Binary")) ((UnsignedByteType) cursor.get()).set(1);
					if (colorModelType.equals("Grey"))   ((UnsignedByteType) cursor.get()).set((int)sample); //simply a copy
				}		
			}
			//uiService.show("imgBin2", imgBin);
			int epsWidth = 1;
			//loop through box sizes
			for (int n = 0; n < numBoxes; n++) {
				
				int proz = (int) (n + 1) * 95 / numBoxes;
				//Compute dilated image
				int kernelSize = 2 * epsWidth + 1;
				eps[n]   = kernelSize;// = epsWidth; = kernelSize; //overwrite eps[n]	
				
	//			kernel = new RectangleShape(kernelSize, false); //kernelSize x kernelSize skipCenter = true
	//			imgDil = (Img<UnsignedByteType>) Dilation.dilate(imgBin, kernel, numThreads);
				
				// create the averaging kernel
				Img<DoubleType> avgKernel = opService.create().img(new int[] {kernelSize, kernelSize});
				for (DoubleType k : avgKernel) {
					k.setReal(1.0);
				}
				imgDil = (Img<FloatType>) opService.filter().convolve(imgBin, avgKernel);
				
				//uiService.show("n=" + n + " imgDil", imgDil);
				// each pixel value is now the sum of the box (neighborhood values)
				count = 0.0;
				cursor = imgDil.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					count = ((FloatType) cursor.get()).getRealFloat();//-1 subtract midpoint itself from number
					for (int b = 0; b < numBands; b++) {
						totalsMax[n][b] = totalsMax[n][b] + count; // calculate total count for normalization
					
						// System.out.println("IqmOpFracGendim: b: "+b+ "   count: "+ count );
						if (count > 0) {
							for (int q = 0; q < numQ; q++) {
								if ((q + numMinQ) == 1)
									totals[q][n][b] = totals[q][n][b] + count * Math.log(count); // GenDim
								else
									totals[q][n][b] = totals[q][n][b] + Math.pow(count, (q + numMinQ)); // GenDim
							}
						}
					}//bands
				}				
				epsWidth = epsWidth * 2;
				// epsWidth = epsWidth+1;
			} // 0>=l<=numEps loop through eps
		}	
		
		// normalization
		for (int n = 0; n < numBoxes; n++) {
			for (int b = 0; b < numBands; b++) { // several bands
				for (int q = 0; q < numQ; q++) {
					totals[q][n][b] = totals[q][n][b] / totalsMax[n][b];
				}
			}
		}
		
		
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[][][] lnTotals = new double[numQ][numBoxes][numBands];
		double[] lnEps        = new double[numBoxes];
		for (int n = 0; n < numBoxes; n++) {
			//lnEps[n] = Math.log(eps[numBoxes - n - 1 ]); //IQM
			lnEps[n] = Math.log(eps[n]);
			for (int b = 0; b < numBands; b++) {
				for (int q = 0; q < numQ; q++) {
					//logService.info(this.getClass().getName() +  "n: " + n + "  q: " + (q + numMinQ) + "    totals[q][n][0]: "+ totals[q][n][0]);
					if (totals[q][n][b] == 0)
						totals[q][n][b] = Double.MIN_VALUE; // damit logarithmus nicht undefiniert ist
					if ((q + numMinQ) != 1)
						lnTotals[q][n][b] = Math.log(totals[q][n][b]);
					if ((q + numMinQ) == 1)
						lnTotals[q][n][b] = totals[q][n][b];
				}
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n][b]);
				//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n][b] );
				//logService.info(this.getClass().getName() + " n:" + n + " totals[n]: " + totals[n]);
			}
		}
		
		//Create double log plot
	
		for (int b = 0; b < numBands; b++) { // mehrere Bands
			
			// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			double[]   lnDataX = new double[numBoxes];
			double[][] lnDataY = new double[numQ][numBoxes];
				
			for (int n = 0; n < numBoxes; n++) {	
				for (int q = 0; q < numQ; q++) {	
					lnDataY[q][n] = lnTotals[q][n][b];		
					lnDataX[n]    = lnEps[n];
				}
			}
			// System.out.println("FractalDimensionBoxCounting: dataY: "+ dataY);
			// System.out.println("FractalDimensionBoxCounting: dataX: "+ dataX);
		
			if (optShowDoubleLogPlot) {			
				boolean isLineVisible = false; //?
				String preName = "";
				String axisNameX = "";
				String axisNameY = "";
				if (numSlices > 1) {
					preName = "Slice-"+String.format("%03d", plane) +"-";
				}
				if (scanningType.equals("Raster box")) {
					axisNameX = "ln(Box size)";
					axisNameY = "ln(Count)";
				}
				else if (scanningType.equals("Sliding box")) {
					axisNameX = "ln(Box size)";
					axisNameY = "ln(Count)";
				}
				String[] legendLabels = new String[numQ];
				for (int q = 0; q < numQ; q++) {
					legendLabels[q] = "q=" + (q + numMinQ); 
				}
				RegressionPlotFrame doubleLogPlot = DisplayMultipleRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plot - Generalized dimensions", 
						preName + datasetName, axisNameX, axisNameY, legendLabels,
						regMin, regMax);
				doubleLogPlotList.add(doubleLogPlot);
			}
			
			// Compute regressions
			for (int q = 0; q < numQ; q++) {
				LinearRegression lr = new LinearRegression();
				regressionParams[q] = lr.calculateParameters(lnDataX, lnDataY[q], regMin, regMax);
				//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			}
			
			// Compute   Dq's
			double [] genDimList = new double[numQ];
			double [] qList   = new double[numQ];
			for (int q = 0; q < numQ; q++) {
				qList[q] = q + numMinQ;
				if (qList[q] == 1) genDimList[q] = regressionParams[q][1]; //Slope
				else               genDimList[q] = regressionParams[q][1]/(qList[q] - 1);
			}
			
			if (optShowDqPlot) {	
				boolean isLineVisible = false; //?
				String preName = "";
				String axisNameX = "";
				String axisNameY = "";
				if (numSlices > 1) {
					preName = "Slice-"+String.format("%03d", plane) +"-";
				}
				axisNameX = "q";
				axisNameY = "Dq";
			
				SignalPlotFrame dimGenPlot = DisplaySinglePlotXY(qList, genDimList, isLineVisible, "Generalized dimensions", 
						preName + datasetName, axisNameX, axisNameY, "");
				genDimPlotList.add(dimGenPlot);
			}
			
			if (optShowFSpectrum) {		
				// see Vicsek Fractal Growth Phenomena p55
				boolean isLineVisible = false; //?
				String preName = "";
				String axisNameX = "";
				String axisNameY = "";
				if (numSlices > 1) {
					preName = "Slice-"+String.format("%03d", plane) +"-";
				}
				axisNameX = "alpha";
				axisNameY = "f";
				
				double[] alphas = new double[numQ];
				double[] fSpec  = new double[numQ];
				//first point
				alphas[0] = (((qList[1] - 1)*genDimList[1]) - ((qList[0] - 1)*genDimList[0])) / 1.0;
				for (int q = 1; q < numQ-1; q++) {
					alphas[q] = (((qList[q+1] - 1)*genDimList[q+1]) - ((qList[q-1] - 1)*genDimList[q-1])) / 2.0;	
				}
				//Last point
				alphas[numQ-1] = (((qList[numQ-1] - 1)*genDimList[numQ-1]) - ((qList[numQ-2] - 1)*genDimList[numQ-2]))  /1.0;
				
				for (int q = 0; q < numQ; q++) {
					fSpec[q] = qList[q]*alphas[q] - ((qList[q]-1)*genDimList[q]);
				}
				SignalPlotFrame fSpecPlot = DisplaySinglePlotXY(alphas, fSpec, isLineVisible, "f spectrum", 
						preName + datasetName, axisNameX, axisNameY, "");
				fSpecPlotList.add(fSpecPlot);
			}
			
			
		}
		
		return regressionParams;
		//Output
		//uiService.show(tableOutName, table);
		//result = ops.create().img(image, new FloatType());
		//table
	}

	/**
	 * This method calculates the number of pixels >0 param RandomAccessibleInterval<?> rai
	 * return double
	 */
	private long getNumberOfNonZeroPixels(RandomAccessibleInterval<?> rai) {
		long total = 0;
		cursor = Views.iterable(rai).localizingCursor();
		while (cursor.hasNext()) { //Box
			cursor.fwd();
			//cursor.localize(pos);				
			if (((UnsignedByteType) cursor.get()).get() > 0) {
				total++; // Binary Image: 0 and [1, 255]! and not: 0 and 255
			}			
		}//while 
		return total;
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
	 * Displays a multiple regression plot in a separate window.
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
	private RegressionPlotFrame DisplayMultipleRegressionPlotXY(double[] dataX, double[][] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String[] legendLabels, int regMin, int regMax) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabels, regMin, regMax);
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
	 * Displays a single plot in a separate window.
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param frameTitle
	 * @param plotLabel
	 * @param xAxisLabel
	 * @param yAxisLabel
	 * @param legendLabel
	 * @param regMin
	 * @param regMax
	 * @return
	 */
	private SignalPlotFrame DisplaySinglePlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel) {
		// jFreeChart
		SignalPlotFrame pl = new SignalPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel);
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
		//ij.command().run(Img2DFractalDimensionGeneralized.class, true).get().getOutput("image");
		ij.command().run(Img2DFractalDimensionGeneralized.class, true);
	}
}

