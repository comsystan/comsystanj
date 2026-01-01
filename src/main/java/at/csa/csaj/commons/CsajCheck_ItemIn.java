/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajCheck_ItemIn.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2026 Comsystan Software
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
package at.csa.csaj.commons;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.scijava.log.LogService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;
import net.imagej.Dataset;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;

public class CsajCheck_ItemIn {

	public static long     width;
	public static long     height;
	public static long     depth;
	public static  int     numDimensions;
	public static int      compositeChannelCount;
	public static long     numSlices;
	private static double  numBytes;
	public static String   imageType;
	public static String   datasetName;
	public static String[] sliceLabels;
	
	public static DefaultGenericTable tableIn;
	public static String   tableInName;
	public static int      numColumns;
	public static int      numRows;
	public static String[] columnLabels;
	
	public static HashMap<String, Object> checkDatasetIn(LogService logService, Dataset datasetIn) {
		
		//This class has no context (@Parameter) possibility
		//Context must be imported from caller class (ContextCommand)
		//context.inject(this); //Important
		
		//datasetIn = imageDisplayService.getActiveDataset();
		if (datasetIn != null) {
			//It is OK
		} else {
			if (logService != null) logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Input image volume = null");
			return null;
		}
		
		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
	         (datasetIn.firstElement() instanceof FloatType) ){
			//It is OK
		} else {	
			if (logService != null) logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Data type is not byte or float");
			return null;
		}
		
		//Get some info
		HashMap<String, Object> info = new HashMap<>();	
		datasetName   = datasetIn.getName();
		width         = datasetIn.dimension(0);
		height        = datasetIn.dimension(1);
		numSlices     = datasetIn.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		numBytes      = datasetIn.getBytesOfInfo();
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
			if (logService != null) logService.info(MethodHandles.lookup().lookupClass().getName() + " WARNING: It was not possible to read scifio metadata."); 
		}
		
		if (logService != null) {
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Name: "              + datasetName); 
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Image size: "        + width+"x"+height); 
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Image type: "        + imageType); 
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Composite channel count: " + compositeChannelCount);
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Number of images = " + numSlices);
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Number of bytes: "   + numBytes); 
		}
			
//		//TO DO FOR ALL 2D PLUGINS
//		if (Arrays.asList(supportedImageTypes).contains(imageType)) {
//			//It is OK
//		} else {
//			if (logService != null) logService.warn(MethodHandles.lookup().lookupClass().getName() + " Image type: " + imageType); 
//			if (logService != null) logService.warn(MethodHandles.lookup().lookupClass().getName() + " WARNING: Image type not allowed for this plugin");
//		}
		
		info.put("width",  width);
		info.put("height", height);
		info.put("numDimensions", numDimensions);
		info.put("compositeChannelCount", compositeChannelCount);
		info.put("numSlices",   numSlices);
		info.put("numBytes",    numBytes);
		info.put("imageType",   imageType);
		info.put("datasetName", datasetName);
		info.put("sliceLabels", sliceLabels);

		return info;
	}
	
	public static HashMap<String, Object> checkVolumeDatasetIn(LogService logService, Dataset datasetIn, String[] supportedImageTypes) {
	
		//datasetIn = imageDisplayService.getActiveDataset();
		if (datasetIn != null) {
			//It is OK
		} else {
			if (logService != null) logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Input image volume = null");
			return null;
		}
		
		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
	         (datasetIn.firstElement() instanceof FloatType) ){
			//It is OK
		} else {	
			if (logService != null) logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Data type is not byte or float");
			return null;
		}
		
		//Get some info
		HashMap<String, Object> info = new HashMap<>();	
		datasetName   = datasetIn.getName();
		width         = datasetIn.dimension(0);
		height        = datasetIn.dimension(1);
		depth         = datasetIn.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		numSlices     = datasetIn.dimension(3);
		numBytes      = datasetIn.getBytesOfInfo();
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
			if (logService != null) logService.info(MethodHandles.lookup().lookupClass().getName() + " WARNING: It was not possible to read scifio metadata."); 
		}
		
		if (logService != null) {
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Name: "              + datasetName); 
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Image size: "        + width+"x"+height); 
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Image depth: "       + depth); 
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Image type: "        + imageType); 
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Composite channel count: " + compositeChannelCount);
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Number of images = " + numSlices);
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Number of bytes: "   + numBytes); 
		}
			
		if (Arrays.asList(supportedImageTypes).contains(imageType)) {
			//It is OK
		} else {
			if (logService != null) logService.warn(MethodHandles.lookup().lookupClass().getName() + " Image type: " + imageType); 
			if (logService != null) logService.warn(MethodHandles.lookup().lookupClass().getName() + " WARNING: Image type not allowed for this plugin");
		}
		
		if (numSlices > 1) { 
			//It is OK
		} else {
			if (logService != null) logService.warn(MethodHandles.lookup().lookupClass().getName() + " WARNING: Single image instead of image volume detected");
			return null;
		}
		
		info.put("width",  width);
		info.put("height", height);
		info.put("depth",  depth);
		info.put("numDimensions", numDimensions);
		info.put("compositeChannelCount", compositeChannelCount);
		info.put("numSlices",   numSlices);
		info.put("numBytes",    numBytes);
		info.put("imageType",   imageType);
		info.put("datasetName", datasetName);
		info.put("sliceLabels", sliceLabels);

		return info;
	}
	
	public static HashMap<String, Object> checkTableIn(LogService logService, DefaultTableDisplay defaultTableDisplay) {
			
		if (defaultTableDisplay != null) {
			//It is OK
		} else {
			if (logService != null) logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Input table display = null");
			return null;
		}
		
		HashMap<String, Object> info = new HashMap<>();	
		
		//DefaultGenericTable tableIn;
		//DefaultTableDisplay dtd = (DefaultTableDisplay) displays.get(0);
		try {
			tableIn = (DefaultGenericTable) defaultTableDisplay.get(0);
		} catch (NullPointerException npe) {
			if (logService != null) logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: NullPointerException, input table = null");
			//cancel("ComsystanJ 1D plugin cannot be started - missing input table.");;
			return null;
		}
	
		// get some info
		tableInName = defaultTableDisplay.getName();
		numColumns  = tableIn.getColumnCount();
		numRows     = tableIn.getRowCount();
		
		columnLabels = new String[(int) numColumns];
		if (logService != null) {
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Name: "      + tableInName); 
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Columns #: " + numColumns);
			logService.info(MethodHandles.lookup().lookupClass().getName() + " Rows #: "    + numRows); 
		}
		
		info.put("tableIn",      tableIn);
		info.put("tableInName",  tableInName); 
		info.put("numColumns",   numColumns);
		info.put("numRows",      numRows);
		info.put("columnLabels", columnLabels);
		
		return info;
	}
}
