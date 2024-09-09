/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DFracDimMinkowskiCommand.java
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
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
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
import at.csa.csaj.plugin3d.frac.util.Minkowski3DMethods;
import at.csa.csaj.plugin3d.frac.util.Minkowski3D_Grey;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link ContextCommand} plugin computing <the 3D Minkowski dimension</a>
 * of an image volume.
 */
@Plugin(type = ContextCommand.class,
headless = true,
label = "3D Minkowski dimension",
initializer = "initialPluginLaunch",
iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
menu = {})
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * The Maven build will execute CreateCommandFiles.java which creates Csaj***Command.java files
 *
 *
 */
public class Csaj3DFracDimMinkowskiCommand<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "Computes 3D Minkowski dimension";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String MORPHOLOGYOPTIONS_LABEL = "<html><b>Morphology options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static String datasetName;
	private static long width = 0;
	private static long height = 0;
	private static long depth = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static int numVolumes = 0;
	private static long compositeChannelCount = 0;
	private static String imageType = "";
	private static int  numDilations = 0;
	private static ArrayList<CsajPlot_RegressionFrame> doubleLogPlotList = new ArrayList<CsajPlot_RegressionFrame>();
	
	private static final String tableOutName = "Table - 3D Minkowski dimension";
	
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

	@Parameter(label = tableOutName, type = ItemIO.OUTPUT)
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
    		    choices = {"Square", "Disk", "Diamond"},
    		    persist = true,  //restore previous value default = true
    		    initializer = "initialShapeType",
    		    callback = "callbackShapeType")
      private String choiceRadioButt_ShapeType;
     
     @Parameter(label = "Morphological operator",
    		 	description = "Type of image and according morphological operation",
    		 	style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		 	choices = {"Binary dilation", "Blanket dilation/erosion", "Variation dilation/erosion"}, //Grey value algorithms not ready yet
    		 	persist = true,  //restore previous value default = true
    		 	initializer = "initialMorphologicalOperator",
    		 	callback = "callbackMorphologicalOperator")
     private String choiceRadioButt_MorphologicalOperator;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   persist = true, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;

     @Parameter(label = "Show last dilated/eroded volume",
 		    	persist = true,  //restore previous value default = true
		        initializer = "initialShowLastMorphImg")
	 private boolean booleanShowLastMorphImg;
         

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
	
//	@Parameter(label = "Image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumImageSlice",
//			   callback = "callbackNumImageSlice")
//	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "    Process single volume     ", callback = "callbackProcessSingleVolume")
	private Button buttonProcessSingleVolume;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label = "Process single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

//	@Parameter(label = "Process all available images", callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;
	
	// ---------------------------------------------------------------------
		
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
    	spinnerInteger_NumRegEnd = 20;
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
	
//	protected void initialNumImageSlice() {
//    	spinnerInteger_NumImageSlice = 1;
//	}
	
	// ------------------------------------------------------------------------------
	

	/** Executed whenever the {@link #spinnerInteger_NumDilations} parameter changes. */
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

		numDilations = spinnerInteger_NumDilations;
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
	        	startWorkflowForSingleVolume();
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
		    	    startWorkflowForSingleVolume();
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
			startWorkflowForSingleVolume();
		}
	}

	public void checkItemIOIn() {

		//datasetIn = imageDisplayService.getActiveDataset();
		if (datasetIn == null) {
			logService.error(this.getClass().getName() + " ERROR: Input image volume = null");
			cancel("ComsystanJ 3D plugin cannot be started - missing input image volume.");
			return;
		}

		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
			 (datasetIn.firstElement() instanceof FloatType) ){
			//That is OK, proceed
		} else {
			logService.warn(this.getClass().getName() + " WARNING: Data type is not Byte or Float");
			cancel("ComsystanJ 3D plugin cannot be started - data type is not Byte or Float.");
			return;
		}
		
		// get some info
		width = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		//depth = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		numDimensions = datasetIn.numDimensions();
	
		//compositeChannelCount = datasetIn.getImgPlus().getCompositeChannelCount(); //1  Grey,   3 RGB
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

		// get the name of dataset
		datasetName = datasetIn.getName();
		
		try {
			Map<String, Object> prop = datasetIn.getProperties();
			DefaultImageMetadata metaData = (DefaultImageMetadata) prop.get("scifio.metadata.image");
			MetaTable metaTable = metaData.getTable();
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to read scifio metadata."); 
		}
  	
		logService.info(this.getClass().getName() + " Name: " + datasetName); 
		logService.info(this.getClass().getName() + " Image size = " + width+"x"+height); 
		logService.info(this.getClass().getName() + " Image type: " + imageType); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
		
		//RGB not allowed
		if (!imageType.equals("Grey")) { 
			logService.warn(this.getClass().getName() + " WARNING: Grey value image volume expected!");
			cancel("ComsystanJ 3D plugin cannot be started - grey value image volume expected!");
		}
		//Image volume expected
		if (numSlices == 1) { 
			logService.warn(this.getClass().getName() + " WARNING: Single image instead of image volume detected");
			cancel("ComsystanJ 3D plugin cannot be started - image volume expected!");
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleVolume() {
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing 3D Minkowski dimension, please wait... Open console window for further info.",
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
			if (doubleLogPlotList != null) {
				for (int l = 0; l < doubleLogPlotList.size(); l++) {
					doubleLogPlotList.get(l).setVisible(false);
					doubleLogPlotList.get(l).dispose();
					// doubleLogPlotList.remove(l); /
				}
				doubleLogPlotList.clear();
			}
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
				if (display.getName().contains(tableOutName)) display.close();
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

		writeToTable(containerPM);
		
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
		
		GenericColumn columnFileName    = new GenericColumn("File name");
		IntColumn columnMaxNumDilations = new IntColumn("# Dilations");;
		GenericColumn columnNumRegStart = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd   = new GenericColumn("Reg End");
		GenericColumn columnShapeType   = new GenericColumn("Shape type");
		GenericColumn columnMorphOp     = new GenericColumn("Morphological operator");
		DoubleColumn columnDm           = new DoubleColumn("3D Dm");
		DoubleColumn columnR2           = new DoubleColumn("R2");
		DoubleColumn columnStdErr       = new DoubleColumn("StdErr");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnMaxNumDilations);
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
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(CsajContainer_ProcessMethod containerPM) { 

		int numRegStart  = spinnerInteger_NumRegStart;
		int numRegEnd    = spinnerInteger_NumRegEnd;
		int numDilations = spinnerInteger_NumDilations;
		String shapeType = choiceRadioButt_ShapeType; 
		String morphOp   = choiceRadioButt_MorphologicalOperator;	
		
		int row = 0;
		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",				row, datasetName);	
		tableOut.set("# Dilations",				row, numDilations);
		tableOut.set("Reg Start",       		row, "("+numRegStart+")" + containerPM.item2_Values[0]); //(NumRegStart)epsRegStart
		tableOut.set("Reg End",   			   	row, "("+numRegEnd+")"   + containerPM.item2_Values[1]); //(NumRegEnd)epsRegEnd
		tableOut.set("Shape type",      		row, shapeType);	
		tableOut.set("Morphological operator",  row, morphOp);	
		tableOut.set("3D Dm",					row, containerPM.item1_Values[1]);
		tableOut.set("R2",						row, containerPM.item1_Values[4]);
		tableOut.set("StdErr",					row, containerPM.item1_Values[3]);
	}

	/**
	*
	* Processing
	*/
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numRegStart          = spinnerInteger_NumRegStart;
		int numRegEnd            = spinnerInteger_NumRegEnd;
		int numDilations         = spinnerInteger_NumDilations;
		String shapeType         = choiceRadioButt_ShapeType; 
		String morphologicalType = choiceRadioButt_MorphologicalOperator;	
		boolean showLastMorphImg = booleanShowLastMorphImg;
		boolean optShowPlot      = booleanShowDoubleLogPlot;


		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		long depth  = rai.dimension(2);
		
		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		double[] regressionParams = null;
		double[] resultValues = null;
		
		String plot_method = "3D dimension";
		String xAxis = "ln(eps)";
		String yAxis = "ln(Count)";
		
		Minkowski3DMethods m3D = null;
		
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
			//*****************************************************************************************************************************************
				m3D = new Minkowski3D_Grey(rai, numDilations, shapeType, morphologicalType, showLastMorphImg, uiService, dlgProgress, statusService);	
				plot_method="3D Minkowski";
				xAxis = "ln(Dilation span)";
				yAxis = "ln(Volume)";
			//******************************************************************************************************************************************		
		} else if (imageType.equals("RGB")) { // RGB image  //additional check, is already checked during validation of active dataset
		
			//no method implemented

		}
		
		double[] eps    = m3D.calcEps();
		double[] totals = m3D.calcTotals();
		
		if (eps == null || totals == null) return null;
		
		double[] lnEps    = new double[numDilations];
		double[] lnTotals = new double[numDilations];
	
		//logService.info(this.getClass().getName() + " 3D Minkowski");
		//logService.info(this.getClass().getName() + " lnEps: \t  lnTotals:");	
		for (int n = 0; n < eps.length; n++) {
			if (totals[n] == 0) {
				lnTotals[n] = Math.log(Double.MIN_VALUE);
			} else if (Double.isNaN(totals[n])) {
				lnTotals[n] = Double.NaN;
			} else {
				lnTotals[n] = Math.log(totals[n]);
			}
			lnEps[n] = Math.log(eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " totals[n][b]: " + totals[n]);
		}
				
		//Create double log plot	
		boolean isLineVisible = false; //?		
		if (optShowPlot) {
			if ((imageType.equals("Grey")) || (imageType.equals("RGB"))) { //both are OK
				String preName = "Volume-";
				CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnEps, lnTotals, isLineVisible, plot_method, 
						preName + datasetName, xAxis, yAxis, "",
						numRegStart, numRegEnd);
				doubleLogPlotList.add(doubleLogPlot);
			}
			else {
	
			}
		}
		
		// Compute regression
		CsajRegression_Linear lr = new CsajRegression_Linear();

		regressionParams = lr.calculateParameters(lnEps, lnTotals, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		//Compute result values
		resultValues = regressionParams;
		double dim = Double.NaN;	
		if      (choiceRadioButt_MorphologicalOperator.equals("Binary dilation"))            dim  = 3-regressionParams[1]; //Standard Dm according to Peleg et al.
		else if (choiceRadioButt_MorphologicalOperator.equals("Blanket dilation/erosion"))   dim  = 4-regressionParams[1]; //high FDs are too low;  better for grey images "Dm" Dubuc etal..
		else if (choiceRadioButt_MorphologicalOperator.equals("Variation dilation/erosion")) dim  = 4-regressionParams[1]; //high FDs are too low
		resultValues[1] = dim;
		logService.info(this.getClass().getName() + " 3D Minkowski dimension: " + dim);
		
		epsRegStartEnd[0] = eps[numRegStart-1];
		epsRegStartEnd[1] = eps[numRegEnd-1];
	
		return new CsajContainer_ProcessMethod(resultValues, epsRegStartEnd);
	
		// Output
		// uiService.show("Table - 3D Minkowski dimension", table);
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int numRegStart, int numRegEnd) {
		if (imageType.equals("Grey")) {
			if (lnDataX == null) {
				logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
				return;
			}
			if (lnDataY == null) {
				logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
				return;
			}
			if (numRegStart >= numRegEnd) {
				logService.info(this.getClass().getName() + " numRegStart >= numRegEnd, cannot display the plot!");
				return;
			}
			if (numRegEnd <= numRegStart) {
				logService.info(this.getClass().getName() + " numRegEnd <= numRegStart, cannot display the plot!");
				return;
			}
			// String preName = "";
			if (preName == null) {
				preName = "Volume-";
			} else {
				preName = "Volume-";
			}
			boolean isLineVisible = false; // ?
			CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
					"Double log plot - 3D Minkowski dimension", preName + datasetName, "ln(k)", "ln(L)", "", numRegStart, numRegEnd);
			doubleLogPlotList.add(doubleLogPlot);
		}
		if (!imageType.equals("Grey")) {

		}
	}
	

	/**
	 * Displays a regression plot in a separate window.
	 * <p>
	 * 
	 *
	 * </p>
	 * 
	 * @param dataX                 data values for x-axis.
	 * @param dataY                 data values for y-axis.
	 * @param isLineVisible         option if regression line is visible
	 * @param frameTitle            title of frame
	 * @param plotLabel             label of plot
	 * @param xAxisLabel            label of x-axis
	 * @param yAxisLabel            label of y-axis
	 * @param numRegStart                minimum value for regression range
	 * @param numRegEnd                maximal value for regression range
	 * @param optDeleteExistingPlot option if existing plot should be deleted before
	 *                              showing a new plot
	 * @param interpolType          The type of interpolation
	 * @return RegressionPlotFrame
	 */
	private CsajPlot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel,String legendLabel,
			int numRegStart, int numRegEnd) {
		// jFreeChart
		CsajPlot_RegressionFrame pl = new CsajPlot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, numRegStart, numRegEnd);
		pl.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pl.pack();
		// int horizontalPercent = 5;
		// int verticalPercent = 5;
		// RefineryUtilities.positionFrameOnScreen(pl, horizontalPercent,
		// verticalPercent);
		// CommonTools.centerFrameOnScreen(pl);
		pl.setVisible(true);
		return pl;
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
