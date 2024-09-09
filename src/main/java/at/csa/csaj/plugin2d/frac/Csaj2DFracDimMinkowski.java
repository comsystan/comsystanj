/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimMinkowski.java
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

package at.csa.csaj.plugin2d.frac;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
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
import net.imagej.display.DefaultImageDisplayService;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.morphology.Erosion;
import net.imglib2.algorithm.morphology.StructuringElements;
import net.imglib2.algorithm.neighborhood.HorizontalLineShape;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
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

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_RegressionFrame;
import at.csa.csaj.commons.CsajRegression_Linear;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link InteractiveCommand} plugin computing
 * <the fractal Minkowski dimension </a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class, 
        headless = true,
        label = "Minkowski dimension",
        initializer = "initialPluginLaunch",
        iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
        menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "2D Image(s)"),
		@Menu(label = "Fractal analyses", weight = 6),
        @Menu(label = "Minkowski dimension")})
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * The Maven build will execute CreateCommandFiles.java which creates Csaj***Command.java files
 *
 *
 */
public class Csaj2DFracDimMinkowski<T extends RealType<T>> extends InteractiveCommand implements Previewable {
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes fractal Minkowski dimension</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String MORPHOLOGYOPTIONS_LABEL = "<html><b>Morphology options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static Img<FloatType> imgFloat; 
	private static Img<FloatType> imgU;
	private static Img<FloatType> imgB; 
	private static Img<FloatType> imgUplusOne;
	private static Img<FloatType> imgBminusOne; 
	private static Img<FloatType> imgUDil;
	private static Img<FloatType> imgBErode;
	private static Img<FloatType> imgV;
	private static Img<UnsignedByteType>imgUnsignedByte;
	private static RandomAccess<FloatType> raF1;
	private static RandomAccess<FloatType> raF2;
	private static RandomAccessibleInterval<FloatType> raiF;	
	private static Cursor<UnsignedByteType> cursorUBT = null;
	private static Cursor<FloatType> cursorF = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static ArrayList<CsajPlot_RegressionFrame> doubleLogPlotList = new ArrayList<CsajPlot_RegressionFrame>();
	
	private static final String tableOutName = "Table - Minkowski dimension";
	
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
	
	@Parameter
	private DefaultImageDisplayService defaultImageDisplayService;
	
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

    @Parameter(label = "Number of dilations",
    		   description = "Number of dilations to increase object size",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist = false,  //restore previous value default = true
	           initializer = "initialNumDilations",
	           callback    = "callbackNumDilations")
    private int spinnerInteger_NumDilations;
    
    @Parameter(label = "Regression Start",
    		   description = "Minimum x value of linear regression",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       stepSize = "1",
 		       persist = false,   //restore previous value default = true
 		       initializer = "initialNumRegStart",
 		       callback = "callbackNumRegStart")
    private int spinnerInteger_NumRegStart = 1;
 
    @Parameter(label = "Regression End",
    		   description = "Maximum x value of linear regression",
    		   style = NumberWidget.SPINNER_STYLE,
		       min = "3",
		       max = "32768",
		       stepSize = "1",
		       persist = false,   //restore previous value default = true
		       initializer = "initialNumRegEnd",
		       callback = "callbackNumRegEnd")
     private int spinnerInteger_NumRegEnd = 3;
    
	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelMethodOptions = MORPHOLOGYOPTIONS_LABEL;
     
     @Parameter(label = "Kernel shape type",
  		    	description = "Shape of morphological structuring element",
  		    	style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		    choices = {"Square", "Disk", "Diamond", "Horizontal", "Vertical"},
    		    persist = true,  //restore previous value default = true
    		    initializer = "initialShapeType",
    		    callback = "callbackShapeType")
      private String choiceRadioButt_ShapeType;
     
     @Parameter(label = "Morphological operator",
    		 	description = "Type of image and according morphological operation",
    		 	style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		 	choices = {"Binary dilation", "Blanket dilation/erosion", "Variation dilation/erosion"},
    		 	persist = true,  //restore previous value default = true
    		 	initializer = "initialMorphologicalOperator",
    		 	callback = "callbackMorphologicalOperator")
     private String choiceRadioButt_MorphologicalOperator;
     
 	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
      
     @Parameter(label = "Show double log plot",
    		    persist = true,  //restore previous value default = true
  		        initializer = "initialShowDoubleLogPlots")
	 private boolean booleanShowDoubleLogPlot;
     
     @Parameter(label = "Show last dilated/eroded image",
 		    	persist = true,  //restore previous value default = true
		        initializer = "initialShowLastMorphImg")
	 private boolean booleanShowLastMorphImg;
         
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
     
 	@Parameter(label = "Image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumImageSlice",
			   callback = "callbackNumImageSlice")
	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "   Process single image #    ", callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label = "Process single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
     
	@Parameter(label = "Process all available images", callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;

    //---------------------------------------------------------------------
 
    //The following initializer functions set initial values	
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
    protected void initialNumDilations() {
      	spinnerInteger_NumDilations = 20;
    }
    protected void initialNumRegStart() {
    	spinnerInteger_NumRegStart = 1;
    }
    protected void initialNumRegEnd() {
    	spinnerInteger_NumRegEnd =  20;
    }
    protected void initialShapeType() {
    	choiceRadioButt_ShapeType = "Square";
    }
    protected void initialMorphologicalOperator() {
    	choiceRadioButt_MorphologicalOperator = "Binary dilation";
    }
    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
    protected void initialShowLastMorphImg() {
    	booleanShowLastMorphImg = true;
    }
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
    
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #spinnerInteger_NumBoxes} parameter changes. */
	protected void callbackNumDilations() {
		
		if  (spinnerInteger_NumDilations < 3) {
			spinnerInteger_NumDilations = 3;
		}
		if (spinnerInteger_NumRegEnd > spinnerInteger_NumDilations) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumDilations;
		}
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}
		logService.info(this.getClass().getName() + " Number of dilations set to " + spinnerInteger_NumDilations);
	}
    /** Executed whenever the {@link #spinnerInteger_NumRegStart} parameter changes. */
	protected void callbackNumRegStart() {
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}
		if(spinnerInteger_NumRegStart < 1) {
			spinnerInteger_NumRegStart = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_NumRegStart);
	}
	/** Executed whenever the {@link #spinnerInteger_NumRegEnd} parameter changes. */
	protected void callbackNumRegEnd() {
		if (spinnerInteger_NumRegEnd <= spinnerInteger_NumRegStart + 2) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumRegStart + 2;
		}		
		if (spinnerInteger_NumRegEnd > spinnerInteger_NumDilations) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumDilations;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_NumRegEnd);
	}

	/** Executed whenever the {@link #choiceRadioButt_ShapeType} parameter changes. */
	protected void callbackShapeType() {
		logService.info(this.getClass().getName() + " Shape type set to " + choiceRadioButt_ShapeType);
		
	}
	
	/** Executed whenever the {@link #choiceRadioButt_MorphologicalOperator} parameter changes. */
	protected void callbackMorphologicalOperator() {
		logService.info(this.getClass().getName() + " Morphological operator set to " + choiceRadioButt_MorphologicalOperator);
		
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
		if (ij != null) { //might be null in Fiji
			if (ij.ui().isHeadless()) {
			}
		}
		if (this.getClass().getName().contains("Command")) { //Processing only if class is a Csaj***Command.class
			startWorkflowForAllImages();
		}
	}

	public void checkItemIOIn() {
	
		//datasetIn = imageDisplayService.getActiveDataset();
		if (datasetIn == null) {
			logService.error(this.getClass().getName() + " ERROR: Input image = null");
			cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
			return;
		}

		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
			 (datasetIn.firstElement() instanceof FloatType) ){
			//That is OK, proceed
		} else {
			logService.warn(this.getClass().getName() + " WARNING: Data type is not Byte or Float");
			cancel("ComsystanJ 2D plugin cannot be started - data type is not Byte or Float.");
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
			logService.warn(this.getClass().getName() + " WARNING: Grey value image(s) expected!");
			cancel("ComsystanJ 2D plugin cannot be started - grey value image(s) expected!");
		}
	}
	
	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleImage() {
				
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Minkowski dimensions, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
 
    	deleteExistingDisplays();
    	generateTableHeader();
    	int sliceIndex = spinnerInteger_NumImageSlice - 1;
    	 logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
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
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Minkowski dimensions, please wait... Open console window for further info.",
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
		
		boolean optDeleteExistingPlots  = false;
		boolean optDeleteExistingTables = false;
		boolean optDeleteExistingImgs   = false;
		if (booleanOverwriteDisplays) {
			optDeleteExistingPlots  = true;
			optDeleteExistingTables = true;
			optDeleteExistingImgs   = true;
		}
		
		if (optDeleteExistingImgs){
//			List<Display<?>> list = defaultDisplayService.getDisplays();
//			for (int i = 0; i < list.size(); i++) {
//				display = list.get(i);
//				System.out.println("display name: " + display.getName());
//				if (display.getName().contains("Last dilated image")) display.close(); //does not close correctly in Fiji, it is only not available any more
//				if (display.getName().contains("Last eroded image"))  display.close();
//			}			
//			List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Is also not closed in Fiji 
		
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				System.out.println("display name: " + frame.getTitle());
				if (frame.getTitle().contains("Last dilated image")) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
				}
				if (frame.getTitle().contains("Last eroded image")) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
				}
			}
		}
		
		Display<?> display;
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
		}
		if (optDeleteExistingTables){
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				display = list.get(i);
				System.out.println("display name: " + display.getName());
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
		CsajContainer_ProcessMethod containerPM = process(rai, s);	
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		writeToTable(0, s, containerPM); //write always to the first row
		
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
		for (int s = 0; s < numSlices; s++) { //p...planes of an image stack
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
				containerPM = process(rai, s);	
				//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
				
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
		IntColumn columnMaxNumBoxes        = new IntColumn("# Dilations");
		GenericColumn columnNumRegStart    = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd      = new GenericColumn("Reg End");
		GenericColumn columnShapeType      = new GenericColumn("Shape type");
		GenericColumn columnMorphOp        = new GenericColumn("Morphological operator");
		DoubleColumn columnDm              = new DoubleColumn("Dm");
		DoubleColumn columnR2              = new DoubleColumn("R2");
		DoubleColumn columnStdErr          = new DoubleColumn("StdErr");
		
	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
		tableOut.add(columnMaxNumBoxes);
		tableOut.add(columnNumRegStart);
		tableOut.add(columnNumRegEnd);
		tableOut.add(columnShapeType);
		tableOut.add(columnMorphOp);
		tableOut.add(columnDm);
		tableOut.add(columnR2);
		tableOut.add(columnStdErr);
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int numRow to write in the result table
	 * @param int numSlice sclice number of images from datasetIn.
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(int numRow, int numSlice, CsajContainer_ProcessMethod containerPM) { 
	
		int numRegStart      = spinnerInteger_NumRegStart;
		int numRegEnd        = spinnerInteger_NumRegEnd;
		int numDilations     = spinnerInteger_NumDilations;
		String shapeType     = choiceRadioButt_ShapeType; 
		String morphOp       = choiceRadioButt_MorphologicalOperator;	
		
		int row = numRow;
	    int s = numSlice;	
		//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
		//fill table with values
		tableOut.appendRow();
		tableOut.set("File name",   	 row, datasetName);	
		if (sliceLabels != null) 	     tableOut.set("Slice name", row, sliceLabels[s]);
		tableOut.set("# Dilations",      row, numDilations);	
		tableOut.set("Reg Start",        row, "("+numRegStart+")" + containerPM.item2_Values[0]); //(NumRegStart)epsRegStart
		tableOut.set("Reg End",      	 row, "("+numRegEnd+")"   + containerPM.item2_Values[1]); //(NumRegEnd)epsRegEnd
		tableOut.set("Shape type",       row, shapeType);	
		tableOut.set("Morphological operator",   row, morphOp);	
		tableOut.set("Dm",          	 row, containerPM.item1_Values[1]);
		tableOut.set("R2",          	 row, containerPM.item1_Values[4]);
		tableOut.set("StdErr",      	 row, containerPM.item1_Values[3]);		
	}
								
	/** 
	 * Processing ****************************************************************************************
	 * <li> Peleg S., Naor J., Hartley R., Avnir D. Multiple Resolution Texture Analysis and Classification, IEEE Trans Patt Anal Mach Intel Vol PAMI-6 No 4 1984, 518-523 !!!
	 * <li> Y.Y. Tang, E.C.M. Lam, New method for feature extraction based on fractal behavior, Patt. Rec. 35 (2002) 1071-1081   Ref. zu Peleg etal.
	 * <li> Dubuc B., Zucker S.W., Tricot C., Quiniou J.F., Wehbi D. Evaluating the fractal dimension of surfaces, Proc.R.Soc.Lond. A 425, 113-127, 1989 
	 * */
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number

		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numRegStart     = spinnerInteger_NumRegStart;
		int numRegEnd       = spinnerInteger_NumRegEnd;
		int numDilations    = spinnerInteger_NumDilations;
		String shapeType    = choiceRadioButt_ShapeType; 
		String MorphologicalType = choiceRadioButt_MorphologicalOperator;	 //binary  Blanket
		boolean optShowPlot    = booleanShowDoubleLogPlot;
		
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "8-bit";  //  "RGB"....
	
		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		double[] regressionParams = null;
		double[] resultValues = null;
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		

		double[] totals = new double[numDilations];
		// double[] totalsMax = new double[numBands]; //for binary images
		int[]      eps    = new int[numDilations];
		
		// definition of eps
		for (int n = 0; n < numDilations; n++) {
				eps[n] = n + 1;
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);	
		}		
		
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255
		if (MorphologicalType.equals("Binary dilation")) {//{"Binary dilation", "Blanket dilation/erosion", "Variation dilation/erosion"}
			//Minkowski
			//n=0  2^0 = 1 ... single pixel
			// Loop through all pixels.
			//get count(area) and copy image 
//			for (int b = 0; b < numBands; b++) {
//				cursor = (Cursor<UnsignedByteType>) Views.iterable(rai).localizingCursor();	
//				while (cursor.hasNext()) {
//					cursor.fwd();
//					//cursor.localize(pos);			
//					if (((UnsignedByteType) cursor.get()).get() > 0) totals[0][b] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
//					//totals[n][b] = totals[n][b]; // / totalsMax[b];
//				}
//			}//b
			imgUnsignedByte = createImgUnsignedByte(rai); //This copies the image, otherwise the original image would be dilated
				
			List<Shape> strel = new ArrayList<Shape>();  
			if      (shapeType.equals("Square"))  strel = StructuringElements.square(1, 2); //3x3kernel, 2 dimensions
			else if (shapeType.equals("Disk"))    strel = StructuringElements.disk(1, 2);
			else if (shapeType.equals("Diamond")) strel = StructuringElements.diamond(1, 2);
			else if (shapeType.equals("Horizontal")) strel.add(new HorizontalLineShape(1, 0, false)); //1,0 ..one step horizontal 1,1.. one step vertical
			else if (shapeType.equals("Vertical"))   strel.add(new HorizontalLineShape(1, 1, false)); //1,0 ..one step horizontal 1,1.. one step vertical			

			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			long[] pos = new long[2];
			long[] min = new long[] {0,0};
			long[] max = new long[] {rai.dimension(0), rai.dimension(1)};
			Interval interval = new FinalInterval(min, max);
		
			for (int n = 0; n < numDilations; n++) { //	
				//Compute dilated image
				Dilation.dilateInPlace(imgUnsignedByte, interval, strel, numThreads); //dilateds image
				//uiService.show("Dilated image", imgUnsignedByte);
				if ((booleanShowLastMorphImg)&&(n == numDilations -1)) uiService.show("Last dilated image", imgUnsignedByte);
				cursorUBT = imgUnsignedByte.localizingCursor();	
				while (cursorUBT.hasNext()) {
					cursorUBT.fwd();
					//cursor.localize(pos);			
					if (((UnsignedByteType) cursorUBT.get()).get() > 0) totals[n] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
					//totals[n][b] = totals[n][b]; // / totalsMax[b];
				}		                      
			} //n	
		}
		//*******************************Grey Value Image
		else if (MorphologicalType.equals("Blanket dilation/erosion")) {// {"Binary dilation", "Blanket dilation/erosion", "Variation dilation/erosion"}
			imgFloat = createImgFloat(rai); //This copies the image, otherwise the original image would be dilated
			imgU = imgFloat.copy();
			imgB = imgFloat.copy();
			imgUplusOne  = imgFloat.copy();
			imgBminusOne = imgFloat.copy();
			imgV = imgFloat.copy();
	
			
			List<Shape> strel = new ArrayList<Shape>();  
			if      (shapeType.equals("Square"))  strel = StructuringElements.square(1, 2); //3x3kernel, 2 dimensions
			else if (shapeType.equals("Disk"))    strel = StructuringElements.disk(1, 2);
			else if (shapeType.equals("Diamond")) strel = StructuringElements.diamond(1, 2);
			else if (shapeType.equals("Horizontal")) strel.add(new HorizontalLineShape(1, 0, false)); //1,0 ..one step horizontal 1,1.. one step vertical
			else if (shapeType.equals("Vertical"))   strel.add(new HorizontalLineShape(1, 1, false)); //1,0 ..one step horizontal 1,1.. one step vertical	
			
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			long[] pos = new long[2];
			float sample1;
			float sample2;

		
			for (int n = 0; n < numDilations; n++) { //
				
				//Add plus one to imgU
				cursorF = imgU.localizingCursor();
				raF1 = imgUplusOne.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();	
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF1.get().set(cursorF.get().get() + 1f); 
				}			
				//Dilated imgU
				imgUDil = Dilation.dilate(imgU, strel, numThreads);
				//uiService.show("Dilated image", imgUDil);
				if ((booleanShowLastMorphImg)&&(n == numDilations -1)) uiService.show("Last dilated image", imgUDil);
				
				//Get Max and overwrite imgU
				cursorF = imgU.localizingCursor();
				raF1 = imgUplusOne.randomAccess();
				raF2 = imgUDil.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					if ((sample2 > sample1)) cursorF.get().set(sample2);
					else cursorF.get().set(sample1);
				}	
				
				//Subtract one to imgB
				cursorF = imgB.localizingCursor();
				raF1 = imgBminusOne.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();	
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF1.get().set(cursorF.get().get() - 1f); 
				}			
				//Erode imgB
				imgBErode = Erosion.erode(imgB, strel, numThreads);
				//uiService.show("Eroded image", imgBErode);
				if ((booleanShowLastMorphImg)&&(n == numDilations -1)) uiService.show("Last eroded image", imgBErode);
				
				//Get Min and overwrite imgB
				cursorF = imgB.localizingCursor();
				raF1 = imgBminusOne.randomAccess();
				raF2 = imgBErode.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					if ((sample2 < sample1)) cursorF.get().set(sample2);
					else cursorF.get().set(sample1);
				}	
				
				//Compute volume imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					cursorF.get().set(sample1 - sample2);
				}	
				
				//Get counts with imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					totals[n] = totals[n] + cursorF.get().get(); //totalAreas
				}
				
				// for (int b = 0; b < numBands; b++) totalAreas[ee][b] =
				// totalAreas[n][b] / (2* (n+1)); //Peleg et al.
				// better is following according to Dubuc et al. equation 9
				totals[n] = totals[n] / ((n+ 1) * (n + 1) * (n + 1)); // eq.9 Dubuc et.al.			                      
			} //n
		}
		
		else if (MorphologicalType.equals("Variation dilation/erosion")) {// {"Binary dilation", "Blanket dilation/erosion", "Variation dilation/erosion"}
			imgFloat = createImgFloat(rai); //This copies the image, otherwise the original image would be dilated
			imgU = imgFloat.copy();
			imgB = imgFloat.copy();
			imgUplusOne  = imgFloat.copy();
			imgBminusOne = imgFloat.copy();
			imgV = imgFloat.copy();
			
			List<Shape> strel = new ArrayList<Shape>();  
			if      (shapeType.equals("Square"))  strel = StructuringElements.square(1, 2); //3x3kernel, 2 dimensions
			else if (shapeType.equals("Disk"))    strel = StructuringElements.disk(1, 2);
			else if (shapeType.equals("Diamond")) strel = StructuringElements.diamond(1, 2);
			else if (shapeType.equals("Horizontal")) strel.add(new HorizontalLineShape(1, 0, false)); //1,0 ..one step horizontal 1,1.. one step vertical
			else if (shapeType.equals("Vertical"))   strel.add(new HorizontalLineShape(1, 1, false)); //1,0 ..one step horizontal 1,1.. one step vertical	
			
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			long[] pos = new long[2];
			float sample1;
			float sample2;

		
			for (int n = 0; n < numDilations; n++) { //
				
				//Dilated imgU
				imgUDil = Dilation.dilate(imgU, strel, numThreads);
				//uiService.show("Dilated image", imgUDil);
				if ((booleanShowLastMorphImg)&&(n == numDilations -1)) uiService.show("Last dilated image", imgUDil);
				
				//Overwrite imgU
				cursorF = imgU.localizingCursor();
				raF2 = imgUDil.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF2.setPosition(pos);
					sample2 = raF2.get().get();
					cursorF.get().set(sample2);
				}	
							
				//Erode imgB
				imgBErode = Erosion.erode(imgB, strel, numThreads);
				//uiService.show("Eroded image", imgBErode);
				if ((booleanShowLastMorphImg)&&(n == numDilations -1)) uiService.show("Last eroded image", imgBErode);
				
				//Overwrite imgB
				cursorF = imgB.localizingCursor();
				raF2 = imgBErode.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF2.setPosition(pos);
					sample2 = raF2.get().get();
					cursorF.get().set(sample2);
				}	
				
				//Compute volume imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					cursorF.get().set(sample1 - sample2);
				}	
				
				//Get counts with imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					totals[n] = totals[n] + cursorF.get().get(); //totalAreas	
				}
			
				totals[n] = totals[n] / ((n+ 1) * (n + 1) * (n + 1)); // eq.17 Dubuc et.al.		
				//Without this normalization Dim would be: Dim = 3-slope;
			} //n
		}
		
			
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[] lnTotals = new double[numDilations];
		double[]   lnEps  = new double[numDilations];
	
		for (int n = 0; n < numDilations; n++) {
			if (totals[n] == 0) {
				lnTotals[n] = Math.log(Double.MIN_VALUE);
			} else if (Double.isNaN(totals[n])) {
				lnTotals[n] = Double.NaN;
			} else {
				lnTotals[n] = Math.log(totals[n]);
			}
			lnEps[n] = Math.log(eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n][b] );
			//logService.info(this.getClass().getName() + " n:" + n + " totals[n][b]: " + totals[n][b]);
		}
			
		//Create double log plot
		boolean isLineVisible = false; //?
	
		// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		double[] lnDataX = new double[numDilations];
		double[] lnDataY = new double[numDilations];
			
		for (int n = 0; n < numDilations; n++) {	
			lnDataY[n] = lnTotals[n];		
			lnDataX[n] = lnEps[n];
		}
	
		if (optShowPlot) {			
			String preName = "";
			if (numSlices > 1) {
				preName = "Slice-"+String.format("%03d", plane) +"-";
			}
			CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double log plot - Minkowski dimension", 
					preName + datasetName, "ln(Dilation span)", "ln(Area)", "",
					numRegStart, numRegEnd);
			doubleLogPlotList.add(doubleLogPlot);
		}
		
		// Compute regression
		CsajRegression_Linear lr = new CsajRegression_Linear();
		regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		//Compute result values
		resultValues = regressionParams;
		double dim = Double.NaN;
		if      (choiceRadioButt_MorphologicalOperator.equals("Binary dilation"))            dim  = 2-regressionParams[1]; //Standard Dm according to Peleg et al.
		else if (choiceRadioButt_MorphologicalOperator.equals("Blanket dilation/erosion"))   dim  =  -regressionParams[1];  //better for grey images "Dm" Dubuc etal..
		else if (choiceRadioButt_MorphologicalOperator.equals("Variation dilation/erosion")) dim  =  -regressionParams[1];  //	
		resultValues[1] = dim;
		logService.info(this.getClass().getName() + " Minkowski dimension: " + dim);
		
		epsRegStartEnd[0] = eps[numRegStart-1];
		epsRegStartEnd[1] = eps[numRegEnd-1];
	
		return new CsajContainer_ProcessMethod(resultValues, epsRegStartEnd);
		//Output
		//uiService.show(tableOutName, table);
		////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
		//table
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
	 * @param numRegStart minimum value for regression range
	 * @param numRegEnd maximal value for regression range 
	 * @param optDeleteExistingPlot option if existing plot should be deleted before showing a new plot
	 * @param interpolType The type of interpolation
	 * @return RegressionPlotFrame
	 */			
	private CsajPlot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel, int numRegStart, int numRegEnd) {
		// jFreeChart
		CsajPlot_RegressionFrame pl = new CsajPlot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, numRegStart, numRegEnd);
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
	 * This methods creates an Img<UnsignedByteType>
	 */
	private Img<UnsignedByteType > createImgUnsignedByte(RandomAccessibleInterval<?> rai){ //rai must always be a single 2D plane
		
		imgUnsignedByte = new ArrayImgFactory<>(new UnsignedByteType()).create(rai.dimension(0), rai.dimension(1)); //always single 2D
		Cursor<UnsignedByteType> cursor = imgUnsignedByte.localizingCursor();
		final long[] pos = new long[imgUnsignedByte.numDimensions()];
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
		
		return imgUnsignedByte;
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
		//ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}

