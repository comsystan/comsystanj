/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Kolmogorov3D_Grey.java
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
package at.csa.csaj.plugin3d.cplx.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.scijava.app.StatusService;
import org.scijava.io.IOService;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;
import org.scijava.ui.UIService;

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import io.scif.SCIFIO;
import io.scif.codec.CompressionType;
import io.scif.config.SCIFIOConfig;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**	Kolmogorov complexity for 3D image volumes, grey and binary
  *
 * @author Helmut Ahammer
 * @param <T>
 * @param <T>
 * @since 2022-11-23  
 */

public class Kolmogorov3D_Grey implements Kolmogorov3DMethods{
	
	private Dataset dataset;
	private RandomAccessibleInterval rai;
	private String compressionType;
	private boolean skipZeroes;
	private int numIterations;
	private static File kolmogorovComplexityDir;
	private static double durationReference  = Double.NaN;
	private static double megabytesReference = Double.NaN;
	private long width = 0;
	private long height = 0;
	private long depth = 0;
	private double[] results = null;
	private CsajDialog_WaitingWithProgressBar dlgProgress;
	private StatusService statusService;
	
	private LogService logService;
	private UIService uiService;
	private IOService ioService;
	private DatasetService datasetService;
	
	@Override
	public double[] getResults() {
		return results;
	}

	@Override
	public void setResults(double[] results) {
		this.results = results;
	}


	/**
	 * This is the standard constructor
	 * @param <T>
	 * 
	 * @param operator the {@link AbstractOperator} firing progress updates
	 */
	public Kolmogorov3D_Grey(RandomAccessibleInterval<?> rai, String compressionType, boolean skipZeroes, int numIterations, LogService logService, UIService uiService, IOService ioService, DatasetService datasetService, CsajDialog_WaitingWithProgressBar dlgProgress, StatusService statusService) {
		this.rai               = rai;
		this.width             = rai.dimension(0);
		this.height            = rai.dimension(1);
		this.depth             = rai.dimension(2);
		this.compressionType   = compressionType;
		this.skipZeroes        = skipZeroes;
		this.numIterations     = numIterations;
		this.logService        = logService;
		this.uiService         = uiService;
		this.ioService         = ioService;
		this.datasetService    = datasetService;
		this.dlgProgress       = dlgProgress;
		this.statusService     = statusService;
		
		dataset = datasetService.create(this.rai);
	}

	public Kolmogorov3D_Grey() {
		
	}

	/**
	 * This method calculates the 3D Kolmogorov complexity
	 * @return totals
	 */
	@Override
	public double[] calcResults() {
		
		dlgProgress.setBarIndeterminate(true);
		int percent;
	
		// Get size 
		//long width = raiVolume.dimension(0);
		//long height = raiVolume.dimension(1);
		//long depth = raiVolume.dimension(2);
		//RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) raiVolume.randomAccess();
	
		//dataset = datasetService.create(this.rai);
		
		double[] resultValues = new double[5];
		for (int n = 0; n < resultValues.length; n++) resultValues[n] = Double.NaN;
			
		percent = 5;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
	
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
				percent = (int)Math.max(Math.round((((float)it)/((float)numIterations)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((it+1), numIterations, "Processing " + (it+1) + "/" + numIterations);
				long startTime = System.nanoTime();
				decompressedSequence = calcDecompressedBytes_ZIP(compressedSequence);
				long time = System.nanoTime();
				stats.addValue((double)(time - startTime));
			}
			//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
			double ld = stats.getPercentile(50); //Median	; //[ns]	
			//double is = megabytesReference;
			double is = originalSize;
			//double is = dataset.getBytesOfInfo()/1024.0/1024.0; //[MB]
		
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
			dlgProgress.setBarIndeterminate(false);
		
			for (int it = 0; it < numIterations; it++){
				percent = (int)Math.max(Math.round((((float)it)/((float)numIterations)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
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
			//double is = dataset.getBytesOfInfo()/1024.0/1024.0; //[MB]
			
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
			dlgProgress.setBarIndeterminate(false);
			
			for (int it = 0; it < numIterations; it++){
				percent = (int)Math.max(Math.round((((float)it)/((float)numIterations)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((it+1), numIterations, "Processing " + (it+1) + "/" + numIterations);
				long startTime = System.nanoTime();
				decompressedSequence = calcDecompressedBytes_GZIP(compressedSequence);
				long time = System.nanoTime();
				stats.addValue((double)(time - startTime));
			}
			//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
			double ld = stats.getPercentile(50); //Median	; //[ns]	
			//double is = megabytesReference;
			double is = originalSize;
			//double is = dataset.getBytesOfInfo()/1024.0/1024.0; //[MB]
			
			resultValues[0] = is;
			resultValues[1] = kc;
			resultValues[2] = is-kc;
			resultValues[3] = kc/is;
			resultValues[4] = ld; 
		}
		
		//*******************************************************************************************************************	
		//Slow because tiff files are saved to disk
		if(compressionType.equals("TIFF-LZW (lossless)")){
			createTempDirectory();
			computeTiffReferenceValues();	
			File targetFile = saveTiffLZWfile();
			resultValues = computeKCValues(targetFile);
			deleteTempDirectory();
		}
		
		return resultValues;
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
		dlgProgress.setBarIndeterminate(false);
		int percent;
		
		for (int i = 0; i < numIterations; i++){
			
			percent = Math.round((((float)i)/((float)numIterations)*100.f));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
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
	 * This method saves a compressed tiff file
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
	
	//***************************************************************************************************************************
	/*
	 * This method computes the KC and LG values
	 * @param targetFile
	 * @return
	 */
	private double[] computeKCValues(File targetFile) {
	
		int numIterations = this.numIterations;
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
		
		dlgProgress.setBarIndeterminate(false);
		int percent;
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for (int i = 0; i < numIterations; i++) { //
           
			percent = Math.round((((float)i)/((float)numIterations)*100.f));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((i+1), numIterations, "Processing " + (i+1) + "/" + numIterations);
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
	
	//*********************************************************************************************************
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
	 * @return byte[] compressed sequence
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

}
