/*-
 * #%L
 * Project: ImageJ plugin for computing Kolmogorov complexity and Logical depth.
 * File: KolmogorovComplexity.java
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
package at.csa.csaj.img2d.kc;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
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

import javax.imageio.ImageIO;
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
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
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
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;
import io.scif.SCIFIO;
import io.scif.codec.CompressionType;
import io.scif.config.SCIFIOConfig;


/**
 * A {@link Command} plugin computing
 * <the Kolmogorov complexity and Logical depth </a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class,
	headless = true,
	label = "KC and LD",
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Image (2D)"),
	@Menu(label = "Kolmogorov complexity and LD", weight = 34)})
public class KolmogorovComplexity<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { //non blocking GUI
//public class FractalDimensionPyramid<T extends RealType<T>> implements Command {	//modal GUI
	
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
	private static long numSlices  = 0;
	private static File kolmogorovComplexityDir;
	private static double durationReference  = Double.NaN;
	private static double megabytesReference = Double.NaN;
	private static double[][] resultValuesTable; //first column is the image index, second column are the corresponding result values
    private static final String tableName = "Table - KC and LD";
	
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
	
	@Parameter
	private IOService ioService;
	
	@Parameter(type = ItemIO.OUTPUT)
	private DefaultGenericTable table;

	
   //Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
  	//private final String labelSpace = SPACE_LABEL;
    
    //Input dataset which is updated in callback functions
  	@Parameter (type = ItemIO.INPUT)
  	private Dataset datasetIn;

    
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
  	private final String labelRegression = COMPRESSION_LABEL;

    @Parameter(label = "Compression",
		    description = "Type of image compression",
		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
  		    choices = {"LZW (lossless)", "PNG (lossless)", "ZLIB (lossless)", "GZIB (lossless)", "J2K (lossless)", "JPG (lossy)"},  //"PNG (lossless)" "ZIP (lossless)" 
  		    //persist  = false,  //restore previous value default = true
		    initializer = "initialCompression",
            callback = "callbackCompression")
    private String choiceRadioButt_Compression;
    
    @Parameter(label = "Iterations for LD",
    		   description = "Number of compressions to compute averages",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "99999999999999",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialNumIterations",
	           callback    = "callbackNumIterations")
    private int spinnerInteger_NumIterations;
    
//    @Parameter(label = "Correct system bias",
//    		 //persist  = false,  //restore previous value default = true
//  		       initializer = "initialCorrectSystemBias")
//	private boolean booleanCorrectSystemBias;
    
  //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
    
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
     
    @Parameter(label   = "Process single active image ",
    		    callback = "callbackProcessActiveImage")
 	private Button buttonProcessActiveImage;
     
    @Parameter(label   = "Process all available images",
 		        callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;


    //---------------------------------------------------------------------
 
    //The following initialzer functions set initial values
     
    protected void initialCompression() {
    	choiceRadioButt_Compression = "LZW (lossless)";
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
  
	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	
	/** Executed whenever the {@link #choiceRadioButt_Interpolation} parameter changes. */
	protected void callbackCompression() {
		logService.info(this.getClass().getName() + " Compression method set to " + choiceRadioButt_Compression);
	}
	/** Executed whenever the {@link #spinInteger_NumIterations} parameter changes. */
	protected void callbackNumIterations() {
		logService.info(this.getClass().getName() + " Number of iterations set to " + spinnerInteger_NumIterations);
	}
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed. */
	protected void callbackProcessActiveImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing KC and LD, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing KC and LD, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);

       	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	logService.info(this.getClass().getName() + " Processing active image");
            		deleteExistingDisplays();
            		getAndValidateActiveDataset();
            		int activeSliceIndex = getActiveImageIndex();
            		processActiveInputImage(activeSliceIndex);
            		dlgProgress.addMessage("Processing finished! Collecting data for table...");
            		generateTableHeader();
            		collectActiveResultAndShowTable(activeSliceIndex);
            		//deleteTempDirectory();
            		dlgProgress.setVisible(false);
            		dlgProgress.dispose();
            		Toolkit.getDefaultToolkit().beep();
                } catch(InterruptedException e){
                	 exec.shutdown();
                } finally {
                	exec.shutdown();
                }		
            }

			private void deleteTempDirectory() {
				boolean success = kolmogorovComplexityDir.delete();
				if (success)  logService.info(this.getClass().getName() + " Successfully deleted temp director " + kolmogorovComplexityDir.getName());
				else {
					logService.info(this.getClass().getName() + " Could not delete temp directory " + kolmogorovComplexityDir.getName());
				}
				
			}
        });
	}
	
	/** Executed whenever the {@link #buttonProcessAllImages} button is pressed. 
	 *  This is the main processing method usually implemented in the run() method for */
	protected void callbackProcessAllImages() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing KC and LD, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing KC and LD, please wait... Open console window for further info.",
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
	        		//deleteTempDirectory();
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

			private void deleteTempDirectory() {
				// TODO Auto-generated method stub
				
			}
        });			
	}
		
    // You can control how previews work by overriding the "preview" method.
 	// The code written in this method will be automatically executed every
 	// time a widget value changes.
 	public void preview() {
 		logService.info(this.getClass().getName() + " Preview initiated");
 		if (booleanProcessImmediately) callbackProcessActiveImage();
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
		
		logService.info(this.getClass().getName() + " Name: " + datasetName); 
		logService.info(this.getClass().getName() + " Image size: " + width+"x"+height); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
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
		
		if (optDeleteExistingTables){
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				Display<?> display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().equals(tableName)) display.close();
			}			
		}
	}
	
	
	/** This method takes the active image and computes results. 
	 *
	 **/
	private void processActiveInputImage(int s) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		resultValuesTable = new double[(int) numSlices][10];
		
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
		double[] resultValues = process(rai, s);	
			//0 Image size, 1 , 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		//set values for output table
		for (int i = 0; i < resultValues.length; i++ ) {
				resultValuesTable[s][i] = resultValues[i]; 
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
		resultValuesTable = new double[(int) numSlices][10];
		
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
				
				RandomAccessibleInterval<T> rai = null;
				if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
					rai = (RandomAccessibleInterval<T>)datasetIn.copy().getImgPlus();
	
				} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
					rai = (RandomAccessibleInterval<T>)Views.hyperSlice(datasetIn.copy(), 2, s);
				}
				//Compute result values
				double[] resultValues = process(rai, s);	
					//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
				
				//set values for output table
				for (int i = 0; i < resultValues.length; i++ ) {
					resultValuesTable[s][i] = resultValues[i]; 
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
		IntColumn columnNumbIterations     = new IntColumn("Itereations [#]");
		GenericColumn columnComprType      = new GenericColumn("Compression");	
		GenericColumn columnImgSz          = new GenericColumn("Image size [MB]");
		GenericColumn columnKC             = new GenericColumn("KC [MB]");
		GenericColumn columnImgSzMinusKC   = new GenericColumn("Image size - KC [MB]");
		GenericColumn columnKCDivImgSz     = new GenericColumn("KC/Imagesize");
		GenericColumn columnLD             = new GenericColumn("LD [ns]");
		
	    table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnComprType);
		table.add(columnImgSz);
		table.add(columnKC);
		table.add(columnImgSzMinusKC);
		table.add(columnKCDivImgSz);
		table.add(columnNumbIterations);
		table.add(columnLD);
	}
	
	/** collects current result and shows table
	 *  @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {

		String compressionType = choiceRadioButt_Compression;
		int numIterations   = spinnerInteger_NumIterations;
	
	    int s = sliceNumber;	
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",  table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
//			if (compressionType.equals("LZW (lossless)")) table.set("Compression", table.getRowCount()-1, "LZW (lossless)");
//			if (compressionType.equals("PNG (lossless)")) table.set("Compression", table.getRowCount()-1, "PNG (lossless)");
//			if (compressionType.equals("J2K (lossless)")) table.set("Compression", table.getRowCount()-1, "J2K (lossless)");	
//			if (compressionType.equals("JPG (lossy)")) table.set("Compression", table.getRowCount()-1, "JPG (lossy)");	
//			if (compressionType.equals("ZIP (lossless)")) table.set("Compression", table.getRowCount()-1, "ZIP (lossless)");
			table.set("Compression",           table.getRowCount()-1, choiceRadioButt_Compression);
			table.set("Image size [MB]",       table.getRowCount()-1, resultValuesTable[s][0]);
			table.set("KC [MB]",               table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("Image size - KC [MB]",  table.getRowCount()-1, resultValuesTable[s][2]);
			table.set("KC/Imagesize",          table.getRowCount()-1, resultValuesTable[s][3]);	
			table.set("Itereations [#]",       table.getRowCount()-1, numIterations);	
			table.set("LD [ns]",               table.getRowCount()-1, resultValuesTable[s][4]);	
		
		//Show table
		uiService.show(tableName, table);
	}
	
	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {
	
		String compressionType = choiceRadioButt_Compression;
		int numIterations   = spinnerInteger_NumIterations;
		
		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack	
			//fill table with values
			table.appendRow();
			table.set("File name",  table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
//			if (compressionType.equals("LZW (lossless)")) table.set("Compression", table.getRowCount()-1, "LZW (lossless)");
//			if (compressionType.equals("PNG (lossless)")) table.set("Compression", table.getRowCount()-1, "PNG (lossless)");	
//			if (compressionType.equals("J2K (lossless)")) table.set("Compression", table.getRowCount()-1, "J2K (lossless)");	
//			if (compressionType.equals("JPG (lossy)")) table.set("Compression", table.getRowCount()-1, "JPG (lossy)");	
//			if (compressionType.equals("ZIP (lossless)")) table.set("Compression", table.getRowCount()-1, "ZIP (lossless)");
			table.set("Compression",           table.getRowCount()-1, choiceRadioButt_Compression);
			table.set("Image size [MB]",       table.getRowCount()-1, resultValuesTable[s][0]);
			table.set("KC [MB]",               table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("Image size - KC [MB]",  table.getRowCount()-1, resultValuesTable[s][2]);
			table.set("KC/Imagesize",          table.getRowCount()-1, resultValuesTable[s][3]);		
			table.set("Itereations [#]",       table.getRowCount()-1, numIterations);	
			table.set("LD [ns]",               table.getRowCount()-1, resultValuesTable[s][4]);	
		}
		//Show table
		uiService.show(tableName, table);
	}
							
	/** 
	 * Processing 
	 * */
	private double[] process(RandomAccessibleInterval<T> rai, int plane) { //plane plane (Image) number
		
		dataset = datasetService.create(rai);
			
//		DefaultImgUtilityService dius = new DefaultImgUtilityService();
//		SCIFIOImgPlus<T> scifioImgPlus = dius.makeSCIFIOImgPlus((Img<T>) rai);
			
		String compressionType = choiceRadioButt_Compression;
		int numIterations      = spinnerInteger_NumIterations;
		double[] resultValues  = new double[5];
		int numBands = 1;
		
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "Grey";  //"Grey"  "RGB"....
			
//		//set default complexities
//		double is         = Double.NaN;  //image size of uncopressed image in MB	
//		double kc         = Double.NaN;  //Kolmogorov complexity (size of compressed image in MB)
//		double ld         = Double.NaN;  //Logical depth
//		double systemBias = Double.NaN;  //duration of system without compression
//		double ldCorr     = Double.NaN;  //corrected logical depth = ld -systemBias
		
		//prepare destination for saving and reloading	
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
				return null;
			}

		}
		
		//Delete files already in the folder
		files = new File(tempFolder).listFiles();

		//delete Reference file
		boolean success1 = false;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				success1 = files[i].delete();
				if (success1)  logService.info(this.getClass().getName() + " Successfully deleted existing temp image " + files[i].getName());
				else {
					logService.info(this.getClass().getName() + " Could not delete existing temp image " + files[i].getName());
					return null;
				}
			}		
		}
	
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
		config.writerSetCompression(CompressionType.UNCOMPRESSED.toString());
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
			long startSavingToDiskTime = System.nanoTime();
			//piReference = JAI.create("fileload", reference);
			//piReference.getHeight(); //Essential for proper time stamps. Without this, duration times are almost identical for every image  and very small
			try {
				//BufferedImage bi = ImageIO.read(referenceFile);  //faster than JAI
				dataset = (Dataset) ioService.open(referencePath);
				//uiService.show(referenceFile.getName(), dataset);
				if (dataset == null) {
					logService.info(this.getClass().getName() + " Something went wrong,  image " + referenceFile.getName() + " coud not be loaded!");
					return null;
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
		
		//double durationTarget  = Double.NaN;
		//double megabytesTarget = Double.NaN;
		String targetPath      = null;
		File   targetFile      = null;
		
		//*******************************************************************************************************************	
		if(compressionType.equals("LZW (lossless)")){
			
			this.deleteTempFile("Temp_LZW.tif");
			targetPath    = kolmogorovComplexityDir.toString() + File.separator + "Temp_LZW.tif";
			targetFile = new File(targetPath);
							
//			// save image using SciJava IoService
//			try {
//				ioService.save(img, target);
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			
			scifio = new SCIFIO();
			config = new SCIFIOConfig();
			config.writerSetCompression(CompressionType.LZW.toString());
			loc = new FileLocation(targetFile.toURI());
			//final Context ctx = new Context();
			//new ImgSaver(ctx).saveImg(loc, dataset, config);
			try {
				scifio.datasetIO().save(dataset, loc, config);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			//scifio.getContext().dispose(); //NullPointerExeption when run as Plugin in Fiji
			resultValues = computeKCandLDfromSavedFile(targetFile);
		}
		
		//*******************************************************************************************************************
		if(compressionType.equals("PNG (lossless)")){

			this.deleteTempFile("Temp.png");
			targetPath    = kolmogorovComplexityDir.toString() + File.separator + "Temp.png";
			targetFile = new File(targetPath);
					
			BufferedImage bi = new BufferedImage((int)rai.dimension(0), (int)rai.dimension(1), 10); //TYPE_BYTE_GRAY = 10;  TYPE_INT_RGB = 1;
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
					
//			//save image using SciJava IoService
//			try {
//				ioService.save(dataset, targetPath);  //does not work with png
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
						
//			scifio = new SCIFIO();
//			config = new SCIFIOConfig();
//			//config.writerSetCompression(compressionType.??????????.toString());
//			loc = new FileLocation(targetFile.toURI());
//			//final Context ctx = new Context();
//			//new ImgSaver(ctx).saveImg(loc, dataset, config);
//			try {
//				scifio.datasetIO().save(dataset, loc, config);
//			} catch (IOException e2) {
//				// TODO Auto-generated catch block
//				e2.printStackTrace();
//			}
//			scifio.getContext().dispose();
			
			
//			final APNGFormat apngFormat = new APNGFormat();   
//		    apngFormat.setContext(scifio.context()); 
//		      
//            // Get writer and set metadata
//            io.scif.Writer writer = null;
//            config = new SCIFIOConfig();
//            config.writerSetCompression(CompressionType.UNCOMPRESSED.toString());
//  
//			try {
//				writer = apngFormat.createWriter();
//			} catch (FormatException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//            try {
//            	io.scif.formats.APNGFormat.Metadata meta = new io.scif.formats.APNGFormat.Metadata();
//            	meta.createImageMetadata(1);	
//            	ImageMetadata imageMetadata = meta.get(0);
//        	    imageMetadata.setBitsPerPixel(FormatTools.getBitsPerPixel(FormatTools.UINT8));
//        	    imageMetadata.setFalseColor(true);
//        	    imageMetadata.setPixelType(FormatTools.UINT8);
//        	    imageMetadata.setPlanarAxisCount(2);
//        	    imageMetadata.setLittleEndian(false);
//        	    imageMetadata.setIndexed(false);
//        	    imageMetadata.setInterleavedAxisCount(0);
//        	    imageMetadata.setThumbnail(false);
//        	    imageMetadata.setOrderCertain(true);
//        	    imageMetadata.addAxis(Axes.X, dataset.dimension(0));
//        	    imageMetadata.addAxis(Axes.Y, dataset.dimension(1));
//        	   
//				writer.setMetadata(meta);
//			} catch (FormatException e2) {
//				// TODO Auto-generated catch block
//				e2.printStackTrace();
//			}
//            try {
//				writer.setDest(loc);
//			} catch (FormatException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}       
//            //Check metadata.
//            // Write the image
//            ImgSaver save = new ImgSaver(scifio.getContext());
//            save.saveImg(writer, dataset, config);	
//    		
//            DefaultImgUtilityService dius = new DefaultImgUtilityService();
//    		SCIFIOImgPlus<T> scifioImgPlus = dius.makeSCIFIOImgPlus((Img<T>) rai);  	
////            save.saveImg(writer, scifioImgPlus, config);	
			
			resultValues = computeKCandLDfromSavedFile(targetFile);
		}
		
		//*******************************************************************************************************************
		if(compressionType.equals("ZLIB (lossless)")){
			int[] dims = new int[rai.numDimensions()];
			int numOfBytes = 0;
			for(int d = 0 ; d < dims.length; d++) {
				numOfBytes += rai.dimension(d);
			}
			double signalSize   = 8.0 * numOfBytes; //Bytes
		    double originalSize = signalSize/1024/1024;   //[MB]
		    
			byte[] compressedSignal = null;
			compressedSignal = calcCompressedBytes_ZLIB(rai);
			double kc =  (double)compressedSignal.length/1024/1024; //[MB]	
				
			byte[] decompressedSignal;
			stats = new DescriptiveStatistics();
			for (int it = 0; it < numIterations; it++){
				long startTime = System.nanoTime();
				decompressedSignal = calcDecompressedBytes_ZLIB(compressedSignal);
				long time = System.nanoTime();
				stats.addValue((double)(time - startTime));
			}
			//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
			double ld = stats.getPercentile(50); //Median	; //[ns]	
			double is = megabytesReference;
			resultValues[0] = is;
			resultValues[1] = kc;
			resultValues[2] = is-kc;
			resultValues[3] = kc/is;
			resultValues[4] = ld; 
		}
		//*******************************************************************************************************************
		if(compressionType.equals("GZIB (lossless)")){
			int[] dims = new int[rai.numDimensions()];
			int numOfBytes = 0;
			for(int d = 0 ; d < dims.length; d++) {
				numOfBytes += rai.dimension(d);
			}
			double signalSize   = 8.0 * numOfBytes; //Bytes
		    double originalSize = signalSize/1024/1024;   //[MB]
		    
			byte[] compressedSignal = null;
			compressedSignal = calcCompressedBytes_GZIB(rai);
			double kc = (double)compressedSignal.length/1024/1024; //[MB]	
				
			byte[] decompressedSignal;
			stats = new DescriptiveStatistics();
			for (int it = 0; it < numIterations; it++){
				long startTime = System.nanoTime();
				decompressedSignal = calcDecompressedBytes_GZIB(compressedSignal);
				long time = System.nanoTime();
				stats.addValue((double)(time - startTime));
			}
			//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
			double ld = stats.getPercentile(50); //Median	; //[ns]	
			double is = megabytesReference;
			resultValues[0] = is;
			resultValues[1] = kc;
			resultValues[2] = is-kc;
			resultValues[3] = kc/is;
			resultValues[4] = ld; 
		}
	
		//*******************************************************************************************************************
		if(compressionType.equals("J2K (lossless)")){
			this.deleteTempFile("Temp.j2k");
			targetPath    = kolmogorovComplexityDir.toString() + File.separator + "Temp.j2k";
			targetFile = new File(targetPath);
			
			scifio = new SCIFIO();
			config = new SCIFIOConfig();
			config.writerSetCompression(CompressionType.J2K.toString());
			loc = new FileLocation(targetFile.toURI());
			//final Context ctx = new Context();
			//new ImgSaver(ctx).saveImg(loc, dataset, config);
			try {
				scifio.datasetIO().save(dataset, loc, config);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			//scifio.getContext().dispose(); //NullPointerExeption when run as Plugin in Fiji 
			resultValues = computeKCandLDfromSavedFile(targetFile);
		}
		//*******************************************************************************************************************
		if(compressionType.equals("JPG (lossy)")){
			this.deleteTempFile("Temp.jpg");
			targetPath    = kolmogorovComplexityDir.toString() + File.separator + "Temp.jpg";
			targetFile = new File(targetPath);
							
			scifio = new SCIFIO();
			config = new SCIFIOConfig();
			config.writerSetCompression(CompressionType.JPEG.toString());
			loc = new FileLocation(targetFile.toURI());
			//final Context ctx = new Context();
			//new ImgSaver(ctx).saveImg(loc, dataset, config);
			try {
				scifio.datasetIO().save(dataset, loc, config);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			//scifio.getContext().dispose(); //NullPointerExeption when run as Plugin in Fiji  
			resultValues = computeKCandLDfromSavedFile(targetFile);
		}
		return resultValues;
	}

	/**
	 * This method computes KC and LD from an already saved file to disk
	 * @param targetFile
	 * @return
	 */
	private double[] computeKCandLDfromSavedFile(File targetFile) {
		//set default complexities
		double is         = Double.NaN;  //image size of uncopressed image in MB	
		double kc         = Double.NaN;  //Kolmogorov complexity (size of compressed image in MB)
		double ld         = Double.NaN;  //Logical depth
		double systemBias = Double.NaN;  //duration of system without compression
		double ldCorr     = Double.NaN;  //corrected logical depth = ld -systemBias
		
		//calculate Reference file if chosen
		//double durationReference  = Double.NaN;
		//double megabytesReference = Double.NaN;
		
		int numIterations   = spinnerInteger_NumIterations;
		
		double[] resultValues  = new double[5];
		//Check if file exists
		if (targetFile.exists()){
			logService.info(this.getClass().getName() + " Successfully saved temp target image " + targetFile.getName());

		} else {
			logService.info(this.getClass().getName() + " Something went wrong,  image " + targetFile.getName() + " coud not be loaded!");

		}
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int i = 0; i < numIterations; i++) { //
           
			long startTimeOfIterations = System.nanoTime();
			//piTarget = JAI.create("fileload", target);
			//piTarget.getHeight(); //Essential for proper time stamps. Without this, duration times are almost identical for every image and very small
			try {
				//BufferedImage bi = ImageIO.read(targetFile);  //faster than JAI
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
		
	    resultValues[0] = is;
	    resultValues[1] = kc;
	    resultValues[2] = is-kc;
	    resultValues[3] = kc/is;
	    if (choiceRadioButt_Compression.equals("LZW (lossless)"))  resultValues[4] = ldCorr; //target and reference files are tiff
	    if (choiceRadioButt_Compression.equals("PNG (lossless)"))  resultValues[4] = ld;  //SCIFIO PNG algorithm (loading images including decompression) is far more faster than tiff algorithm (reference file) - ldCorr would yield negative values  
	    if (choiceRadioButt_Compression.equals("J2K (lossless)"))  resultValues[4] = ld;
	    if (choiceRadioButt_Compression.equals("JPG (lossy)"))     resultValues[4] = ld;
	 
		return resultValues;
		//0 "Image size [MB]"  1 "KC [MB]" 2 "Image size - KC [MB]"  3 "KC/Imagesize [MB]" 4 "LD [ns]"	
		
		//Output
		//uiService.show("Table - ", table);
		//result = ops.create().img(image, new FloatType());
		//table
	}
	
	
	
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
	 * @param RandomAccessibleInterval<T> rai
	 * @return byte[] compressed image
	 */
	private byte[] calcCompressedBytes_ZLIB(RandomAccessibleInterval<T> rai) {
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		byte[] data = new byte[(int)width * (int)height]; //????? * 8];?????
		
		// Loop through all pixels of this image
		Cursor<T> cursor = Views.iterable(rai).localizingCursor();
		int i = 0;
		while (cursor.hasNext()) { //Image
			cursor.fwd();
			data[i] = (byte) ((UnsignedByteType) cursor.get()).get();
			i = i + 1;
		}
		
		Deflater deflater = new Deflater(); 
		deflater.setLevel(Deflater.BEST_COMPRESSION);
		deflater.setInput(data); 
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);  
		
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
		//System.out.println("PlotOpComplLogDepth Original: " + data.length  ); 
		//System.out.println("PlotOpComplLogDepth ZLIB Compressed: " + output.length ); 
		return output;
	}
	
	/**
	 * This method calculates and returns compressed bytes
	 * @param RandomAccessibleInterval<T> rai
	 * @return byte[] compressed image
	 */
	private byte[] calcCompressedBytes_GZIB(RandomAccessibleInterval<T> rai) {
		
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		byte[] data = new byte[(int)width * (int)height]; //????? * 8];?????
		
		// Loop through all pixels of this image
		Cursor<T> cursor = Views.iterable(rai).localizingCursor();
		int i = 0;
		while (cursor.hasNext()) { //Image
			cursor.fwd();
			data[i] = (byte) ((UnsignedByteType) cursor.get()).get();
			i = i + 1;
		}
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    	try{
	            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
	            gzipOutputStream.write(data);
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
	    //System.out.println("PlotOpComplLogDepth Original: " + data.length  ); 
	    //System.out.println("PlotOpComplLogDepth GZIB Compressed: " + output.length ); 
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
	    //System.out.println("PlotOpComplLogDepth ZLIB Input: " + array.length  ); 
	    //System.out.println("PlotOpComplLogDepth Decompressed: " + output.length ); 
	    return output;
	}
	/**
	 * This method decompresses byte array
	 * @param  byte[] array  compressed
	 * @return byte[] array  decompressed
	 */
	private byte[] calcDecompressedBytes_GZIB(byte[] array) {
		
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
	    //System.out.println("PlotOpComplLogDepth GZIB Input: " + array.length  ); 
	    //System.out.println("PlotOpComplLogDepth Decompressed: " + output.length ); 
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
		//ij.command().run(KolmogorovComplexity.class, true).get().getOutput("image");
		ij.command().run(KolmogorovComplexity.class, true);
	}
}

