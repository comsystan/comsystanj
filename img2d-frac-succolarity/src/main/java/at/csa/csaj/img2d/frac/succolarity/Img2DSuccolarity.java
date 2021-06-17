/*-
 * #%L
 * Project: ImageJ plugin for computing succolarity.
 * File: Img2DSuccolarity.java
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
package at.csa.csaj.img2d.frac.succolarity;

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
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.neighborhood.RectangleShape;
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

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link Command} plugin computing
 * <the succolarity </a>
 * of an image.
 * 
 * According to
 * de Melo, R. H. C., und A. Conci. „Succolarity: Defining a method to calculate this fractal measure“. In 2008 15th International Conference on Systems, Signals and Image Processing, 291–94, 2008. https://doi.org/10.1109/IWSSIP.2008.4604424.
 * de Melo, R. H. C., und A. Conci. „How Succolarity Could Be Used as Another Fractal Measure in Image Analysis“. Telecommunication Systems 52, Nr. 3 (1. März 2013): 1643–55. https://doi.org/10.1007/s11235-011-9657-3.
 * 
 */
@Plugin(type = InteractiveCommand.class,
	headless = true,
	label = "Succolarity",
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Image (2D)"),
	@Menu(label = "Succolarity", weight = 21)})
public class Img2DSuccolarity<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { //non blocking GUI
//public class Img2DSuccolarity<T extends RealType<T>> implements Command {	//modal GUI
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes Succolarity</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String FLOODINGOPTIONS_LABEL   = "<html><b>Flooding type</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static Img<FloatType> imgFloat;
	private static Img<UnsignedByteType> imgFlood;
	private static RandomAccess<UnsignedByteType> raFlood;
	private static Img<UnsignedByteType> imgDil;
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
	private static final String tableName = "Table - Succolarities";
	
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

	@Parameter(type = ItemIO.OUTPUT)
	private DefaultGenericTable table;

	
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
    		   description = "Number of distinct box sizes with the power of 2",
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
    
    //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelMethodOptions = METHODOPTIONS_LABEL;
    
    @Parameter(label = "Scanning type",
   		    description = "Type of box scanning",
   		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
     		choices = {"Raster box", "Sliding box"},
     		//persist  = false,  //restore previous value default = true
   		    initializer = "initialScanningType",
            callback = "callbackScanningType")
    private String choiceRadioButt_ScanningType;
    
    //-----------------------------------------------------------------------------------------------------
    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    //private final String labelFloodingOptions = FLOODINGOPTIONS_LABEL;
    
    @Parameter(label = "Flooding type",
   		    description = "Type of flooding, e.g. Top to down or Left to right.... or mean of all 4 directions",
   		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
     		choices = {"T2D", "D2T", "L2R", "R2L", "Mean"},
     		//persist  = false,  //restore previous value default = true
   		    initializer = "initialFloodingType",
            callback = "callbackFloodingType")
    private String choiceRadioButt_FloodingType;
     
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
     
     @Parameter(label   = "Process all available images",
 		        callback = "callbackProcessAllImages")
	 private Button buttonProcessAllImages;


    //---------------------------------------------------------------------
 
    //The following initialzer functions set initial values
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
    
    protected void initialScanningType() {
    	choiceRadioButt_ScanningType = "Raster box";
    }
    
    protected void initialFloodingType() {
    	choiceRadioButt_FloodingType = "T2D";
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
	
	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	/** Executed whenever the {@link #spinInteger_NumBoxes} parameter changes. */
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
    /** Executed whenever the {@link #spinInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if(spinnerInteger_RegMin < 1) {
			spinnerInteger_RegMin = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}
	/** Executed whenever the {@link #spinInteger_RegMax} parameter changes. */
	protected void callbackRegMax() {
		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
		}		
		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
		}	
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_RegMax);
	}

	/** Executed whenever the {@link #choiceRadioButt_ScanningType} parameter changes. */
	protected void callbackScanningType() {
		logService.info(this.getClass().getName() + " Scanning type set to " + choiceRadioButt_ScanningType);
		
	}
	
	/** Executed whenever the {@link #choiceRadioButt_FloodingType} parameter changes. */
	protected void callbackFloodingType() {
		logService.info(this.getClass().getName() + " Flooding type set to " + choiceRadioButt_FloodingType);
		
	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #spinInteger_NumImageSlice} parameter changes. */
	protected void callbackNumImageSlice() {
		getAndValidateActiveDataset();
		if (spinnerInteger_NumImageSlice > numSlices){
			logService.info(this.getClass().getName() + " No more images available");
			spinnerInteger_NumImageSlice = (int)numSlices;
		}
		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
	}
	

	/** Executed whenever the {@link #buttonProcessSingelImage} button is pressed. */
	protected void callbackProcessSingleImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Succolarity, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Succolarity, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);

       	exec.execute(new Runnable() {
            public void run() {
        	    try {
            		deleteExistingDisplays();
            		getAndValidateActiveDataset();
            		int sliceIndex = spinnerInteger_NumImageSlice - 1;
            		 logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
            		processSingleInputImage(sliceIndex);
            		dlgProgress.addMessage("Processing finished! Collecting data for table...");
            		generateTableHeader();
            		collectActiveResultAndShowTable(sliceIndex);
            		dlgProgress.setVisible(false);
            		dlgProgress.dispose();
            		Toolkit.getDefaultToolkit().beep();
                } catch(InterruptedException e){
                	 exec.shutdown();
                } finally {
                	exec.shutdown();
                }		
            }
        });
	}
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed. */
	protected void callbackProcessActiveImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Succolarity, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Succolarity, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);

       	exec.execute(new Runnable() {
            public void run() {
        	    try {
            		deleteExistingDisplays();
            		getAndValidateActiveDataset();
            		int activeSliceIndex = getActiveImageIndex();
            		logService.info(this.getClass().getName() + " Processing active image " + (activeSliceIndex + 1));
            		processSingleInputImage(activeSliceIndex);
            		dlgProgress.addMessage("Processing finished! Collecting data for table...");
            		generateTableHeader();
            		collectActiveResultAndShowTable(activeSliceIndex);
            		dlgProgress.setVisible(false);
            		dlgProgress.dispose();
            		Toolkit.getDefaultToolkit().beep();
                } catch(InterruptedException e){
                	 exec.shutdown();
                } finally {
                	exec.shutdown();
                }		
            }
        });
	}
	
	/** Executed whenever the {@link #buttonProcessAllImages} button is pressed. 
	 *  This is the main processing method usually implemented in the run() method for */
	protected void callbackProcessAllImages() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Succolarity, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Succolarity, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);
		
		exec.execute(new Runnable() {
            public void run() {	
            	try {
	            	logService.info(this.getClass().getName() + " Processing all available images");
	        		deleteExistingDisplays();
	        		getAndValidateActiveDataset();
	        		processAllInputImages();
	        		dlgProgress.addMessage("Processing finished! Collecting data for table...");
	        		generateTableHeader();
	        		collectAllResultsAndShowTable();
	        		dlgProgress.setVisible(false);
	        		dlgProgress.dispose();
	        		Toolkit.getDefaultToolkit().beep();
            	} catch(InterruptedException e){
                    //Thread.currentThread().interrupt();
            		exec.shutdown();
                } finally {
                	exec.shutdown();
                }      	
            }
        });		
	}
	
    // You can control how previews work by overriding the "preview" method.
 	// The code written in this method will be automatically executed every
 	// time a widget value changes.
 	public void preview() {
 		logService.info(this.getClass().getName() + " Preview initiated");
 		if (booleanProcessImmediately) callbackProcessSingleImage();
 		//statusService.showStatus(message);
 	}
 	
    // This is often necessary, for example, if your  "preview" method manipulates data;
 	// the "cancel" method will then need to revert any changes done by the previews back to the original state.
 	public void cancel() {
 		logService.info(this.getClass().getName() + " Widget canceled");
 	}
    //---------------------------------------------------------------------------
	
 	
 	/** The run method executes the command. */
	@Override
	public void run() {
		//Nothing, because non blocking dialog has no automatic OK button and would call this method twice during start up
	
		//ij.log().info( "Run" );
		logService.info(this.getClass().getName() + " Run");

		if(ij.ui().isHeadless()){
			//execute();
			this.callbackProcessAllImages();
		}
	}
	
	public void getAndValidateActiveDataset() {

		datasetIn = imageDisplayService.getActiveDataset();
	
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
//				if (frame.getTitle().equals("Name")) {
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
				if (display.getName().equals(tableName)) display.close();
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
	private void processSingleInputImage(int s) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		resultValuesTable = new double[(int) numSlices][numBoxes];
		
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//mg<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
		
		}

		//Compute succolarity
		double[] succolarities = process(rai, s);
		
		//set values for output table
		for (int i = 0; i < succolarities.length; i++ ) {
				resultValuesTable[s][i] = succolarities[i]; 
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
	private void processAllInputImages() throws InterruptedException{
		
		long startTimeAll = System.currentTimeMillis();
		resultValuesTable = new double[(int) numSlices][numBoxes];
	
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//Img<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		
		//loop over all slices of stack
		for (int s = 0; s < numSlices; s++){ //p...planes of an image stack
			if (!exec.isShutdown()){
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
				//succolarities
				double[] succolarities = process(rai, s);	
				//set values for output table
				for (int i = 0; i < succolarities.length; i++ ) {
						resultValuesTable[s][i] = succolarities[i]; 
				}
				
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			}
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
		
		GenericColumn columnFileName   = new GenericColumn("File name");
		GenericColumn columnSliceName  = new GenericColumn("Slice name");
		IntColumn columnMaxNumBoxes    = new IntColumn("# Boxes");
		IntColumn columnRegMin         = new IntColumn("RegMin");
		IntColumn columnRegMax         = new IntColumn("RegMax");
		GenericColumn columnScanType   = new GenericColumn("Scanning type");
		GenericColumn columnFloodType  = new GenericColumn("Flooding type");
	
	    table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnMaxNumBoxes);
		table.add(columnRegMin);
		table.add(columnRegMax);
		table.add(columnScanType);
		table.add(columnFloodType);
		String preString = "Succ";
		for (int i = 0; i < numBoxes; i++) {
			table.add(new DoubleColumn(preString + "-" + (int)Math.pow(2,i) + "x" + (int)Math.pow(2, i)));
		}
	}
	
	/** collects current result and shows table
	 *  @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {
	
		//int numBoxes   = spinnerInteger_NumBoxes;
		int regMin     		= spinnerInteger_RegMin;
		int regMax     		= spinnerInteger_RegMax;
		String scanningType = choiceRadioButt_ScanningType;
		String floodingType = choiceRadioButt_FloodingType;
		
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
	    int s = sliceNumber;	
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",     table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null)   table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Boxes",       table.getRowCount()-1, numBoxes);	
			table.set("RegMin",        table.getRowCount()-1, regMin);	
			table.set("RegMax",        table.getRowCount()-1, regMax);
			table.set("Scanning type", table.getRowCount()-1, scanningType);
			table.set("Flooding type", table.getRowCount()-1, floodingType);
			tableColLast = 6;
			
			int numParameters = resultValuesTable[s].length;
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + numParameters;
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				table.set(c, table.getRowCount()-1, resultValuesTable[s][c-tableColStart]);
			}	

		//Show table
		uiService.show(tableName, table);
	}
	
	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {
	
		//int numBoxes       = spinnerInteger_NumBoxes;
		int regMin          = spinnerInteger_RegMin;
		int regMax          = spinnerInteger_RegMax;
		String scanningType = choiceRadioButt_ScanningType;
		String floodingType = choiceRadioButt_FloodingType;
		
		
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;

		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",     table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null)   table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Boxes",       table.getRowCount()-1, numBoxes);	
			table.set("RegMin",        table.getRowCount()-1, regMin);	
			table.set("RegMax",        table.getRowCount()-1, regMax);
			table.set("Scanning type", table.getRowCount()-1, scanningType);
			table.set("Flooding type", table.getRowCount()-1, floodingType);
			tableColLast = 6;
			
			int numParameters = resultValuesTable[s].length;
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + numParameters;
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				table.set(c, table.getRowCount()-1, resultValuesTable[s][c-tableColStart]);
			}	
		}
		//Show table
		uiService.show(tableName, table);
	}
							
	/** 
	 * Processing ****************************************************************************************
	 * */
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number
	
		//int numBoxes        = spinnerInteger_NumBoxes;
		int regMin          = spinnerInteger_RegMin;
		int regMax          = spinnerInteger_RegMax;
		String scanningType = choiceRadioButt_ScanningType;
		String floodingType = choiceRadioButt_FloodingType;
		
		boolean optShowPlot = booleanShowDoubleLogPlot;
		
		//long width  = rai.dimension(0);
		//long height = rai.dimension(1);
		//RandomAccess<?> ra = rai.randomAccess();
		//ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		
		//String imageType = "8-bit";  //  "RGB"....
	
		double[] succolaritiesT2D = null;
		double[] succolaritiesD2T = null;
		double[] succolaritiesL2R = null;
		double[] succolaritiesR2L = null;
		double[] succolarities    = null;
			
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255	
		if (floodingType.equals("T2D")) {
			//get and initialize T2D flooded image
			initializeImgFlood_T2D(rai);
			floodingImgFlood();
			succolarities = computeSuccolarities_T2D();	
		}
		else if (floodingType.equals("D2T")) {
			initializeImgFlood_D2T(rai);
			floodingImgFlood();
			succolarities = computeSuccolarities_D2T();	
		}
		else if (floodingType.equals("L2R")) {
			initializeImgFlood_L2R(rai);
			floodingImgFlood();
			succolarities = computeSuccolarities_L2R();	
		}
		else if (floodingType.equals("R2L")) {
			initializeImgFlood_R2L(rai);
			floodingImgFlood();
			succolarities = computeSuccolarities_R2L();	
		}
		else if (floodingType.equals("Mean")) {
			initializeImgFlood_T2D(rai);
			floodingImgFlood();
			succolaritiesT2D = computeSuccolarities_T2D();	
			
			initializeImgFlood_D2T(rai);
			floodingImgFlood();
			succolaritiesD2T = computeSuccolarities_D2T();	
		
			initializeImgFlood_L2R(rai);
			floodingImgFlood();
			succolaritiesL2R = computeSuccolarities_L2R();	
			
			initializeImgFlood_R2L(rai);
			floodingImgFlood();
			succolaritiesR2L = computeSuccolarities_R2L();	
			
			succolarities = new double[numBoxes];
			for (int s = 0; s < numBoxes; s++) {
				succolarities[s] = (succolaritiesT2D[s] + succolaritiesD2T[s] + succolaritiesL2R[s] + succolaritiesR2L[s])/4.0;
			}
		}
			
		//Create double log plot
		boolean isLineVisible = false; //?
			
		// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		double[] lnDataX = new double[numBoxes];
		double[] lnDataY = new double[numBoxes];
			
		double succ;
		for (int k = 0; k < numBoxes; k++) {
			if (succolarities[k] == 0) {
				succ = Double.MIN_VALUE;
			} else  {
				succ = succolarities[k];
			};
			lnDataY[k] = Math.log(100.0*succ);		
			lnDataX[k] = Math.log(Math.pow(2, k)); //box size	
		}
		// System.out.println("FractalDimensionBoxCounting: dataY: "+ dataY);
		// System.out.println("FractalDimensionBoxCounting: dataX: "+ dataX);
	
		if (optShowPlot) {			
			String preName = "";
			if (numSlices > 1) {
				preName = "Slice-"+String.format("%03d", plane) +"-";
			}
			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plot - Succolarity", 
					preName + datasetName, "ln(Box width)", "ln(100.Succolarity)", "",
					regMin, regMax);
			doubleLogPlotList.add(doubleLogPlot);
		}
		
		// Compute regression
		//LinearRegression lr = new LinearRegression();
		//regressionParams = lr.calculateParameters(lnDataX, lnDataY, regMin, regMax);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
	
		return succolarities;
		//Output
		//uiService.show(tableName, table);
		//result = ops.create().img(image, new FloatType());
		//table
	}

	/**
	 * This method generates an image for flooding with set Top pixels
	 * @param rai
	 */
	//get and initialize T2D flooded image
	private void initializeImgFlood_T2D(RandomAccessibleInterval<?> rai) { 
		int value = 0;
		imgFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height); //always single 2D
		raFlood = imgFlood.randomAccess();
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		ra.setPosition(0,1);     //y=Top;
		raFlood.setPosition(0,1);//y=Top;
		for (int x=0; x < width; x++) {
			ra.setPosition(x, 0);
			value = ra.get().getInteger();
			if (value == 0) {
				raFlood.setPosition(x, 0);
				raFlood.get().set(255);
			}		
		}
	}
	
	/**
	 * This method generates an image for flooding with set Bottom pixels
	 * @param rai
	 */
	//get and initialize D2T flooded image
	private void initializeImgFlood_D2T(RandomAccessibleInterval<?> rai) { 
		int value = 0;
		imgFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height); //always single 2D
		raFlood = imgFlood.randomAccess();
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		ra.setPosition(height-1,1);     //y=Bottom;
		raFlood.setPosition(height-1,1);//y=Bottom;
		for (int x=0; x < width; x++) {
			ra.setPosition(x, 0);
			value = ra.get().getInteger();
			if (value == 0) {
				raFlood.setPosition(x, 0);
				raFlood.get().set(255);
			}		
		}
	}
	
	/**
	 * This method generates an image for flooding with set Left pixels
	 * @param rai
	 */
	//get and initialize L2R flooded image
	private void initializeImgFlood_L2R(RandomAccessibleInterval<?> rai) { 
		int value = 0;
		imgFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height); //always single 2D
		raFlood = imgFlood.randomAccess();
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		ra.setPosition(0,0);     //x=Left;
		raFlood.setPosition(0,0);//x=Left;
		for (int y=0; y < height; y++) {
			ra.setPosition(y, 1);
			value = ra.get().getInteger();
			if (value == 0) {
				raFlood.setPosition(y, 1);
				raFlood.get().set(255);
			}		
		}
	}
	
	/**
	 * This method generates an image for flooding with set Right pixels
	 * @param rai
	 */
	//get and initialize R2L flooded image
	private void initializeImgFlood_R2L(RandomAccessibleInterval<?> rai) { 
			int value = 0;
			imgFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height); //always single 2D
			raFlood = imgFlood.randomAccess();
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			ra.setPosition(width-1,0);     //x=Right;
			raFlood.setPosition(width-1,0);//x=Right;
			for (int y=0; y < height; y++) {
				ra.setPosition(y, 1);
				value = ra.get().getInteger();
				if (value == 0) {
					raFlood.setPosition(y, 1);
					raFlood.get().set(255);
				}		
			}
		}
	
	
	/**
	 * This methods floods the image by subsequent dilations and subtractions of the original image
	 */
	//flooding imgFlood
	private void floodingImgFlood() {
		RectangleShape kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		int availableProcessors = runtime.availableProcessors();
		//System.out.println("availabel processors: " + availableProcessors);
		
		int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
		if (numThreads > availableProcessors) numThreads = availableProcessors;
		
		int diff = 99999999;
		int pixelValueImgOrig;
		int pixelValueImgFlood;
		int pixelValueImgDil;
	
		long[] pos = new long[2];
		while (diff != 0) {
			//Compute dilated image
			imgDil = Dilation.dilate(imgFlood, kernel, numThreads);
			//subtract original image
			cursor = imgDil.localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				pixelValueImgDil = ((UnsignedByteType) cursor.get()).get();
				pixelValueImgOrig = ra.get().getInteger();
				// Binary Image: 0 and [1, 255]! and not: 0 and 255
				if ((pixelValueImgDil > 0) && (pixelValueImgOrig > 0)) {
					((UnsignedByteType) cursor.get()).setReal(0);					
				}
			}
			//get diff
			diff = 0;
			cursor = imgDil.localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raFlood.setPosition(pos);
				pixelValueImgDil = ((UnsignedByteType) cursor.get()).get();
				pixelValueImgFlood = raFlood.get().getInteger();
				// Binary Image: 0 and [1, 255]! and not: 0 and 255
				if ((pixelValueImgDil > 0) && (pixelValueImgFlood == 0)) {
					diff = diff + 1;
					break; //it is not necessary to search any more
				} else if ((pixelValueImgDil == 0) && (pixelValueImgFlood > 0)) {
					diff = diff + 1; //it is not necessary to search any more
					break;
				}	
			}		
			imgFlood = imgDil;
			//uiService.show("imgFlood", imgFlood);
			raFlood = imgFlood.randomAccess();
		}
	}
	
	//Following four methods to compute succolarities are quite identical
	//Pressure is the only variable which is differently computed
	
	/**
	 * This method computes succolarities for distinct box sizes from Top two down
	 * @return
	 */
	private double[] computeSuccolarities_T2D() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;	
		int delta = 0;
		
//		//extra for k = 0 did not be really be faster
//		//k = 0 box size == 1
//		boxSize = (int) Math.pow(2, 0); //= 1;
//		//boxSize = k+1;	
//		numOfScannedBoxes = (int) (width*height);
//		pressure = 0.0;
//		maxPR = (double)(height - 1 + height)/2.0;
//		long[] pos = new long[2];
//		cursor = imgFlood.localizingCursor();
//		while (cursor.hasNext()) { //imgFlood
//			cursor.fwd();	
//			if (((UnsignedByteType) cursor.get()).get() > 0) {
//				cursor.localize(pos); 
//				// Binary Image: 0 and [1, 255]! and not: 0 and 255
//				pressure = (double)(pos[1] + pos[1] + 1.0)/2.0;
//				succ[0] = succ[0] + pressure;     //(occ/(boxSize*boxSize)*pressure); /boxSize == 1   occ == 1 
//			}								
//		}//while imgFlood
//		//Normalization
//		succ[0] = succ[0]/((double)numOfScannedBoxes*1*maxPR);
		
		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;	
			pressure = 0.0;
			for (int y = 0;  y<= (height-boxSize); y=y+delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = ((double)y + (double)y + (double)boxSize)/2.0;
				for (int x = 0; x <= (width-boxSize); x=x+delta){
					raiBox = Views.interval(imgFlood, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
					occ = 0;
					// Loop through all pixels of this box.
					cursor = Views.iterable(raiBox).localizingCursor();
					while (cursor.hasNext()) { //Box
						cursor.fwd();
						//cursorF.localize(pos); 		
						if (((UnsignedByteType) cursor.get()).get() > 0) {
							// Binary Image: 0 and [1, 255]! and not: 0 and 255
							occ = occ + 1.0;
						}								
					}//while Box
					succ[k] = succ[k] + (occ/(boxSize*boxSize)*pressure);
					norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //K
		return succ;
	}
	
	/**
	 * This method computes succolarities for distinct box sizes from Down two top
	 * @return
	 */
	private double[] computeSuccolarities_D2T() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;	
		int delta = 0;
		
		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;
			pressure = 0.0;
			for (int y = 0;  y<= (height-boxSize); y=y+delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = height - ((double)y + (double)y + (double)boxSize)/2.0;
				for (int x = 0; x <= (width-boxSize); x=x+delta){
					raiBox = Views.interval(imgFlood, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
					occ = 0.0;
					// Loop through all pixels of this box.
					cursor = Views.iterable(raiBox).localizingCursor();
					while (cursor.hasNext()) { //Box
						cursor.fwd();
						//cursorF.localize(pos); 		
						if (((UnsignedByteType) cursor.get()).get() > 0) {
							// Binary Image: 0 and [1, 255]! and not: 0 and 255
							occ = occ + 1.0;
						}								
					}//while Box
					succ[k] = succ[k] + (occ/(boxSize*boxSize)*pressure);
					norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //k
		return succ;
	}
	
	/**
	 * This method computes succolarities for distinct box sizes from Left to right
	 * @return
	 */
	private double[] computeSuccolarities_L2R() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;	
		int delta = 0;
		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;
			pressure = 0.0;
			for (int x = 0; x <= (width-boxSize); x=x+delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = ((double)x + (double)x + (double)boxSize)/2.0;
				for (int y = 0;  y<= (height-boxSize); y=y+delta){
					raiBox = Views.interval(imgFlood, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
					occ = 0.0;
					// Loop through all pixels of this box.
					cursor = Views.iterable(raiBox).localizingCursor();
					while (cursor.hasNext()) { //Box
						cursor.fwd();
						//cursorF.localize(pos); 		
						if (((UnsignedByteType) cursor.get()).get() > 0) {
							// Binary Image: 0 and [1, 255]! and not: 0 and 255
							occ = occ + 1.0;
						}								
					}//while Box
					succ[k] = succ[k] + (occ/(boxSize*boxSize)*pressure);
					norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //k
		return succ;
	}
	
	/**
	 * This method computes succolarities for distinct box sizes from Right to left
	 * @return
	 */
	private double[] computeSuccolarities_R2L() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;
		int delta = 0;

		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;
			pressure = 0.0;
			for (int x = 0; x <= (width-boxSize); x=x+delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = width - ((double)x + (double)x + (double)boxSize)/2.0; 
				for (int y = 0;  y<= (height-boxSize); y=y+delta){
					raiBox = Views.interval(imgFlood, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
					occ = 0.0;
					// Loop through all pixels of this box.
					cursor = Views.iterable(raiBox).localizingCursor();
					while (cursor.hasNext()) { //Box
						cursor.fwd();
						//cursorF.localize(pos); 		
						if (((UnsignedByteType) cursor.get()).get() > 0) {
							// Binary Image: 0 and [1, 255]! and not: 0 and 255
							occ = occ + 1.0;
						}								
					}//while Box
					succ[k] = succ[k] + (occ/(boxSize*boxSize)*pressure);
					norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //k
		return succ;
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
		
		imgFloat = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
		Cursor<FloatType> cursor = imgFloat.localizingCursor();
		final long[] pos = new long[imgFloat.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			//if (numSlices == 1) { //for only one 2D image;
				ra.setPosition(pos[0], 0);
				ra.setPosition(pos[1], 1);
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
		//ij.command().run(Img2DSuccolarity.class, true).get().getOutput("image");
		ij.command().run(Img2DSuccolarity.class, true);
	}
}

