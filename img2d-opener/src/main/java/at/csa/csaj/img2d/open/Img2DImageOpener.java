/*-
 * #%L
 * Project: ImageJ2 plugin to open single or multiple images.
 * File: Img2DImageOpener.java
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
package at.csa.csaj.img2d.open;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;


import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.image.ImagePreviewPanel;

import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;

import io.scif.DefaultImageMetadata;
import io.scif.DefaultMetaTable;
import io.scif.MetaTable;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
/**
 * This is an ImageJ {@link Command} plugin to open single or multiple images.
 * <p>
 * 
 * </p>
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 */
@Plugin(type = ContextCommand.class,
		label = "Image opener",
		//iconPath = "/images/comsystan-??.png", //Menu entry icon
		menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (2D)"),
        @Menu(label = "Image opener", weight = 1)})
public class Img2DImageOpener<T extends RealType<T>> extends ContextCommand { //modal GUI with cancel
	
	private static final String PLUGIN_LABEL = "<html><b>Opens single or multiple images</b></html>";
	private static final String SPACE_LABEL  = "";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	@Parameter (label = "Image(s)",type = ItemIO.OUTPUT) //so that it can be displayed
	private Dataset datasetOut;
	
	@Parameter
	private DatasetService datasetService;
	
	@Parameter
    private UIService uiService;
    
    @Parameter
    private PrefService prefService;
    
    @Parameter
    private IOService ioService;
    
    //Widget elements------------------------------------------------------
	//No widget
	
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
    	
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
    	//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Computing 3D fractal, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Opening images, please wait... Open console window for further info.",
		                                                                             logService, false, null); //isCanceable = false, because no following method listens to exec.shutdown 

		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	long startTimeAll = System.currentTimeMillis();
    	logService.info(this.getClass().getName() + " Opening images, please wait...");
    	
		JFileChooser fc = null;
		try {fc = new JFileChooser();}
		catch (Throwable e) {
			Result result = uiService.showDialog("Java 2 or Swing is needed.", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			dlgProgress.setVisible(false);
			dlgProgress.dispose();
			return;
			}
		fc.setMultiSelectionEnabled(true);
		
		// Add thumbnail preview
		// Thanks to Jakob Hatzl, FH Joanneum Graz for this link:
		// http://www.javalobby.org/java/forums/t49462.html
		ImagePreviewPanel preview = new ImagePreviewPanel();
		fc.setAccessory(preview);
		fc.addPropertyChangeListener(preview);
		
		String dir = System.getProperty("user.dir");
		//System.out.println("System.getProperty(user.dir): " + System.getProperty("user.dir"));		
		if (dir.endsWith(File.separator)) dir = dir.substring(0, dir.length()-1);		
		fc.setCurrentDirectory(new File(dir));
		
		int returnVal = fc.showOpenDialog(null);
		if (returnVal!=JFileChooser.APPROVE_OPTION) {
			dlgProgress.setVisible(false);
			dlgProgress.dispose();
			return;
		}
		File[] files = fc.getSelectedFiles();
		if (files.length==0) { // getSelectedFiles may not work on some JVMs
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}
		
		//set default path to new path
		dir = fc.getCurrentDirectory().getPath();
		String path = dir + System.getProperty("file.separator");
	
		System.setProperty("user.dir", dir);
		//System.out.println("System.getProperty(user.dir): " + System.getProperty("user.dir"));
		
		if (files.length == 1) {
			//Img< T > image = ( Img< T > ) IO.open(files[0].getPath());
			try {
				datasetOut = (Dataset) ioService.open(files[0].getPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logService.info(this.getClass().getName() + " WARNING #IO21: It was not possible to load the image");
				e.printStackTrace();
				dlgProgress.setVisible(false);
				dlgProgress.dispose();
				return;
			}
			//uiService.show(files[0].getName(), image);
		    //datasetOut = datasetService.create(image);
		    uiService.show(files[0].getName(), datasetOut);
		}
		
		if (files.length > 1) {
			dlgProgress.setBarIndeterminate(false);
			//Sort alphabetically, to be sure
			Arrays.sort(files);
			int numImgDimensions = 0;
			String name = "Image Stack";
			long[] dims = null;
			AxisType[] axes = null;
			int bitsPerPixel = 0;
			boolean signed   = false;
			boolean floating = false;
			boolean virtual  = false;
						
			RandomAccess<RealType<?>> ra;
			Cursor<UnsignedByteType> cursor;
			long[] pos2D = new long[2];
			long[] pos3D = new long[3];
			long[] pos4D = new long[4]; 
			float value;
			
			//ArrayList<Img<T>> hyperSlicesList = new ArrayList();
			String[] fileNames = new String[files.length];
			Img<T> img = null;
			for (int i=0; i<files.length; i++) {
				int percent = (int)Math.round((  ((float)i)/((float)files.length)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((i+1), (int)files.length, "Opening " + (i+1) + "/" + (int)files.length);

				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Opening image number " + (i+1) + "(" + files.length + ")");
				
				fileNames[i] = files[i].getName();
		        //Img< T > image = ( Img< T > ) IO.open(files[i].getPath()); 
		        //hyperSlicesList.add((Img<T>) IO.open(files[i].getPath()));
		        try {
					img = ((Img<T>) ioService.open(files[i].getPath()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logService.info(this.getClass().getName() + " WARNING #IO22 It was not possible to load the image");
					e.printStackTrace();
				}
			    //Generate dataset with first image
		        if (i==0) {
		        	if (img.numDimensions() == 2) { //Grey
		        		numImgDimensions = 2;
		        		bitsPerPixel = 8;
			        	dims = new long[]{img.dimension(0), img.dimension(1), files.length};
						axes = new AxisType[]{Axes.X, Axes.Y, Axes.Z};
						datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
		        	} 
		        	else if (img.numDimensions() == 3) { //RGB
		        		numImgDimensions = 3;
		        		bitsPerPixel = 8;
			        	dims = new long[]{img.dimension(0), img.dimension(1), 3, files.length};
						axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z};
						datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
						datasetOut.setCompositeChannelCount(3);
						datasetOut.setRGBMerged(true);
		        	} 
		        }
		        //Check image size
		    	if ((img.dimension(0) != dims[0]) || (img.dimension(1) != dims[1])) {
		    		JOptionPane.showMessageDialog(null, "Image " + (i+1)+"("+files.length +")\nName: " + files[i].getName() + "\nhas different width or height!\nOpening of images cancelled.", "Warning", JOptionPane.WARNING_MESSAGE);
		    		logService.info(this.getClass().getName() + " Image " + (i+1)+"("+files.length +") " + files[i].getName() + " has different width or height! - Opening of images cancelled");
		    		dlgProgress.setVisible(false);
		    		dlgProgress.dispose();
		    		statusService.showProgress(0, 100);
		    		statusService.clearStatus();
		    		img = null;
		    		datasetOut = null;
		    		return;
		    	}	  
		    	//Check image type 
		    	if (img.numDimensions() != numImgDimensions) {
		    		JOptionPane.showMessageDialog(null, "Image " + (i+1)+"("+files.length +")\nName: " + files[i].getName() + "\nis a different type of image!\nOpening of images cancelled.", "Warning", JOptionPane.WARNING_MESSAGE);
		    		logService.info(this.getClass().getName() + " Image " + (i+1)+"("+files.length +") " + files[i].getName() + " is a different type of image! - Opening of images cancelled");
		    		dlgProgress.setVisible(false);
		    		dlgProgress.dispose();
		    		statusService.showProgress(0, 100);
		    		statusService.clearStatus();
		    		img = null;
		    		datasetOut = null;
		    		return;
		    	}	     
		        //write to datasetOut
		        if (img.numDimensions() == 2) { //Grey
			        cursor = (Cursor<UnsignedByteType>) img.cursor();
			        ra = datasetOut.randomAccess();
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos2D);
						value= cursor.get().getRealFloat();
						pos3D = new long[] {pos2D[0], pos2D[1], i};
						ra.setPosition(pos3D);
						ra.get().setReal(value);
					}
				}
		        else if (img.numDimensions() == 3) { //RGB
			        cursor = (Cursor<UnsignedByteType>) img.cursor();
			        ra = datasetOut.randomAccess();
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos3D);
						value= cursor.get().getRealFloat();
						pos4D = new long[] {pos3D[0], pos3D[1], pos3D[2], i};
						ra.setPosition(pos4D);
						ra.get().setReal(value);
					}
				}    
		        img = null;
		        long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
		    	// Show image sequence
				//uiService.show(files[i].getName(), image);
			} 
			//StackView sv = new StackView(hyperSlicesList);
			//RandomAccessibleInterval<T> rai = Views.stack(hyperSlicesList);
			//uiService.show("Image Stack", sv); //shows stack, but without slice labels
			//datasetOut = datasetService.create(sv);
			//datasetOut.getImgPlus().setName("Image Stack");
			
			//set slice labels
			try {
				Map<String, Object> prop = datasetOut.getProperties(); //is empty or what?
				
				MetaTable metaTable = new DefaultMetaTable();		
				metaTable.put("SliceLabels", fileNames);		
				DefaultImageMetadata metaData = new DefaultImageMetadata();
				metaData.setTable(metaTable);		
				prop.put("scifio.metadata.image", metaData);
			
			} catch (NullPointerException npe) {
				// TODO Auto-generated catch block
				//npe.printStackTrace();
				logService.info(this.getClass().getName() + " WARNING #IO23: It was not possible to read scifio metadata."); 
			}
	
			uiService.show("Image Stack", datasetOut);
		}
	
//NOT NEEDED			
//		//get IJ1 functionality with following dependency in the pom
//		<dependency>
//			<groupId>net.imagej</groupId>
//			<artifactId>ij1-patcher</artifactId>
//		</dependency>
//
//		LegacyEnvironment ij1 = null;
//		try {
//			ij1 = new LegacyEnvironment(null, true);
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		ij1.runMacro("print('IJ1: Running IJ1 command ImagesToStack')","");
//		//ij1.runPlugIn("Images to Stack", "name=Stack title=[] use");
//		ij1.run("Images to Stack", "name=Stack title=[] use"); //THROWS ERROR  " No images found."
		
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time for all images: "+ sdf.format(duration));
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		
		statusService.showProgress(0, 100);
		statusService.clearStatus();
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
         ij.command().run(Img2DImageOpener.class, true);
    	
    }

}
