/*-
 * #%L
 * Project: ImageJ2 plugin for computing the 3D Correlation dimension.
 * File: CorrelationDim3D_Grey.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2022 Comsystan Software
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
package at.csa.csaj.plugin3d.frac.dim.correlation.util;

import org.scijava.app.StatusService;

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

/**	Box counting dimension for 3D image volumes, grey and binary
  *
 * @author Helmut Ahammer, Martin Reiss
 * @since 2022-11-16  
 */

public class CorrelationDim3D_Grey implements CorrelationDim3DMethods{
	
	private RandomAccessibleInterval<?> rai = null;
	private static RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<?> ra;
	private static Cursor<?> cursor = null;
	private int numBoxes = 0;
	private long width = 0;
	private long height = 0;
	private long depth = 0;
	private double[] totals = null;
	private int[] eps = null;
	private String scanningType;
	private String colorModelType;
	private int pixelPercentage;
	private WaitingDialogWithProgressBar dlgProgress;
	private StatusService statusService;
	
	@Override
	public double[] getTotals() {
		return totals;
	}

	@Override
	public void setTotals(double[] totals) {
		this.totals = totals;
	}

	@Override
	public int[] getEps() {
		return eps;
	}

	@Override
	public void setEps(int[] eps) {
		this.eps = eps;
	}


	/**
	 * This is the standard constructor
	 * 
	 * @param operator the {@link AbstractOperator} firing progress updates
	 */
	public CorrelationDim3D_Grey(RandomAccessibleInterval<?> rai, int numBoxes, String scanningType, String colorModelType, int pixelPercentage, WaitingDialogWithProgressBar dlgProgress, StatusService statusService) {
		this.rai             = rai;
		this.width           = rai.dimension(0);
		this.height          = rai.dimension(1);
		this.depth           = rai.dimension(2);
		this.numBoxes        = numBoxes;
		this.scanningType    = scanningType;
		this.colorModelType  = colorModelType;
		this.pixelPercentage = pixelPercentage;
		this.dlgProgress     = dlgProgress;
		this.statusService   = statusService;
	}

	public CorrelationDim3D_Grey() {
	}

	/**
	 * This method calculates the 3D Correlation dimension
	 * @return totals
	 */
	@Override
	public double[] calcTotals() {
		
		dlgProgress.setBarIndeterminate(false);
		int percent;

		if (eps == null) this.calcEps();
		
		// Get size 
		//long width = raiVolume.dimension(0);
		//long height = raiVolume.dimension(1);
		//long depth = raiVolume.dimension(2);
		//RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) raiVolume.randomAccess();
			
		double[] totals = new double[numBoxes];
	
		if (scanningType.equals("Raster box")) {
			//Fixed grid
			/**	KORRELATIONSDIMENSION: Fixed Grid Scan
			 *  Martin Reiss
			 * 	Eine schnelle Näherung der Korrelationsdimension wird durch einen Fixed-Grid Scan ermöglicht. 
			 * 	Der Datensatz wird einer Diskretisierung durch ein Gitter bei einer festen Boxgröße unterworfen. 
			 * 	Im Anschluss wird lediglich das Quadrat der Besetzungszahlen jeder Zelle kalkuliert. 
			 *  Wong A, Wu L,Gibbons P, Faloutsos C, Fast estimation of fractal dimension and correlation integral on stream data. Inf.Proc.Lett 93 (2005) 91-97
			 */ 
			int boxSize;		
			int delta = 0;
			long count = 0;
			int sample = 0; 
			
			percent = 1;
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus(percent, 100, "Initializing finished");
			
			for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes		
				percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
				
				boxSize = (int)eps[n];		
				delta = boxSize;
				for (int x =0; x <= (width-boxSize); x=x+delta){
					for (int y =0;  y<= (height-boxSize); y=y+delta){
						for (int z =0;  z<= (depth-boxSize); z=z+delta){
							raiBox = Views.interval(rai, new long[]{x, y, z}, new long[]{x+boxSize-1, y+boxSize-1, z+boxSize-1});
							// Loop through all pixels of this box.
							cursor = Views.iterable(raiBox).localizingCursor();
							while (cursor.hasNext()) { //Box
								cursor.fwd();
								//cursorF.localize(pos);	
								sample = ((UnsignedByteType) cursor.get()).get();
								if ( sample > 0) { //Binary Image: 0 and [1, 255]! and not: 0 and 255
									if (colorModelType.equals("Binary")) count = count + 1;
									if (colorModelType.equals("Grey"))   count = count + sample;
								}			
							}//while Box
							totals[n]   += count*count;
							//totalsMax[n][b] = totalsMax[n][b] + count*count; // calculate total count for normalization
							count = 0;
						} //Z
					} //y	
				} //x  
				//totals[n] = totals[n] / totalsMax[n];
			} //n	
		} //Fast fixed grid estimate
		
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255
		//Correlation method	
		else if (scanningType.equals("Sliding box")) {
			//Classical correlation dimension with radius over a pixel
			//radius is estimated by box
			ra = rai.randomAccess();
			long number_of_points = 0;
			int max_random_number = (int) (100/pixelPercentage); // Evaluate max. random number
			int random_number = 0;
			int radius;		
			long count = 0;
			int sample = 0;
			
			percent = 1;
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus(percent, 100, "Initializing finished");
			
			if  (max_random_number == 1) { // no statistical approach, take all image pixels
				for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes	
					percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
					
					radius = (int)eps[n];			
					for (int x = 0; x < width; x++){
						for (int y = 0; y < height; y++){	
							for (int z = 0; z < depth; z++){	
								ra.setPosition(x, 0);
								ra.setPosition(y, 1);	
								ra.setPosition(z, 2);	
								if((((UnsignedByteType) ra.get()).get() > 0) ){
									number_of_points++; // total number of points 	
									// scroll through sub-array 
									for (int xx = x - radius + 1; xx < x + radius ; xx++) {
										if(xx >= 0 && xx < width) { // catch index-out-of-bounds exception
											for (int yy = y - radius + 1; yy < y + radius; yy++) {
												if(yy >= 0 && yy < height) { // catch index-out-of-bounds exception
													for (int zz = z - radius + 1; zz < z + radius; zz++) {
														if(zz >= 0 && zz < depth) { // catch index-out-of-bounds exception
														//if (Math.sqrt((xx-x)*(xx-x)+(yy-y)*(yy-y)) <= radius) { //HA
															ra.setPosition(xx, 0);
															ra.setPosition(yy, 1);	
															ra.setPosition(zz, 2);
															sample = ((UnsignedByteType) ra.get()).get();
															if((sample > 0) ){
																if (colorModelType.equals("Binary")) count = count + 1;
																if (colorModelType.equals("Grey"))   count = count + sample;
															}
														//}//<= radius
														}
													}//zz
												}
											}//yy
										}
									}//XX
								}
							}//z
						} //y	
					} //x  
					// calculate the average number of neighboring points within distance "radius":  
					//number of neighbors = counts-total_number_of_points
					//average number of neighbors = number of neighbors / total_number_of_points		 
					//totals[n][b]=(double)(count-number_of_points)/number_of_points; //MR
					//System.out.println("Counts:"+counts+" total number of points:"+total_number_of_points);
					// set counts equal to zero
					count=0;	
					number_of_points=0;
				} //n Box sizes		
			} // no statistical approach
			else { //statistical approach
				for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes	
					percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
					
					radius = eps[n];				
					for (int x = 0; x < width; x++){
						for (int y = 0;  y < height; y++){		
							for (int z = 0;  z < depth; z++){		
								random_number = (int) (Math.random()*max_random_number+1);
								if( random_number == 1 ){ // UPDATE 07.08.2013 
									ra.setPosition(x, 0);
									ra.setPosition(y, 1);	
									ra.setPosition(z, 2);	
									if((((UnsignedByteType) ra.get()).get() > 0) ){
										number_of_points++; // total number of points 	
										// scroll through sub-array 
										for (int xx = x - radius + 1; xx < x + radius ; xx++) {
											if(xx >= 0 && xx < width) { // catch index-out-of-bounds exception
												for (int yy = y - radius + 1; yy < y + radius; yy++) {
													if(yy >= 0 && yy < height) { // catch index-out-of-bounds exception
														for (int zz = z - radius + 1; zz < z + radius; zz++) {
															if(zz >= 0 && zz < depth) { // catch index-out-of-bounds exception
																//if (Math.sqrt((xx-x)*(xx-x)+(yy-y)*(yy-y)) <= radius) { //HA
																	ra.setPosition(xx, 0);
																	ra.setPosition(yy, 1);
																	ra.setPosition(zz, 2);
																	sample = ((UnsignedByteType) ra.get()).get();
																	if((sample > 0) ){
																		if (colorModelType.equals("Binary")) count = count + 1;
																		if (colorModelType.equals("Grey"))   count = count + sample;
																	}
																//}//<= radius
															}
														}//zz	
													 }
												}//yy
											}
										}//XX
									}
								}
							} //z
						} //y	
					} //x  
					// calculate the average number of neighboring points within distance "radius":  
					//number of neighbors = counts-total_number_of_points
					//average number of neighbors = number of neighbors / total_number_of_points
					totals[n]=(double)(count-number_of_points)/number_of_points; 	
					//System.out.println("Counts:"+counts+" total number of points:"+total_number_of_points);
					// set counts equal to zero
					count=0;	
					number_of_points=0;
				} //n Box sizes		
			}
		} //
		return totals;
		
	}
	
	/**
	 * This method calculates the Boxes
	 * @return eps
	 */
	@Override
	public int[] calcEps() {

		eps = new int[numBoxes];
		for (int n = 0; n < numBoxes; n++) {
			eps[n] = (int)Math.round(Math.pow(2, n));
		}
		return eps;
	}	

}
