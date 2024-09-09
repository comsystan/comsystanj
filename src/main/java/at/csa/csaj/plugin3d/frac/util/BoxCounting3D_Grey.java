/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: BoxCounting3D_Grey.java
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
package at.csa.csaj.plugin3d.frac.util;

import org.scijava.app.StatusService;

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**	Box counting dimension for 3D image volumes, grey and binary
  *
 * @author Helmut Ahammer, Martin Reiss
 * @since 2022-11-11  
 */

public class BoxCounting3D_Grey implements BoxCounting3DMethods{
	
	private RandomAccessibleInterval<?> rai = null;
	private int numBoxes = 0;
	private long width = 0;
	private long height = 0;
	private long depth = 0;
	private double[] totals = null;
	private double[] eps = null;
	private String scanningType;
	private String colorModelType;
	private CsajDialog_WaitingWithProgressBar dlgProgress;
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
	public double[] getEps() {
		return eps;
	}

	@Override
	public void setEps(double[] eps) {
		this.eps = eps;
	}


	/**
	 * This is the standard constructor
	 * 
	 * @param operator the {@link AbstractOperator} firing progress updates
	 */
	public BoxCounting3D_Grey(RandomAccessibleInterval<?> rai, int numBoxes, String scanningType, String colorModelType, CsajDialog_WaitingWithProgressBar dlgProgress, StatusService statusService) {
		this.rai             = rai;
		this.width           = rai.dimension(0);
		this.height          = rai.dimension(1);
		this.depth           = rai.dimension(2);
		this.numBoxes        = numBoxes;
		this.scanningType    = scanningType;
		this.colorModelType  = colorModelType;
		this.dlgProgress     = dlgProgress;
		this.statusService   = statusService;
	}

	public BoxCounting3D_Grey() {
	}

	/**
	 * This method calculates the 3D Box counting dimension
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
		long L1 = width;
	    long L2 = height;
		long L3 = depth;
		int delta = 0;
		RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		long[] pos = new long[3];
		int pixelValue;
		boolean pixelFound = false;
		
		percent = 1;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
	
		
		for (int b = 0; b < numBoxes; b++) {

			percent = (int)Math.max(Math.round((((float)b)/((float)numBoxes)*100.f)), percent);
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((b+1), numBoxes, "Processing " + (b+1) + "/" + numBoxes);
			
			int boxSize = (int)eps[b];
			if      (scanningType.equals("Raster box"))  delta = boxSize;
			else if (scanningType.equals("Sliding box")) delta = 1;
			for (int x = 0; x <= L1 - boxSize; x += delta) {
				for (int y = 0; y <= L2 - boxSize; y += delta) {
					for (int z = 0; z <= L3 - boxSize; z += delta) {
			
						pixelFound = false;
						for (int xx = x; xx <= x + (boxSize - 1); xx++) {
							for (int yy = y; yy <= y + (boxSize - 1); yy++) {
								for (int zz = z; zz <= z + (boxSize - 1); zz++) {	
									//if (boxSize == 256) System.out.println("pos  xx:" + xx + "  yy:" +yy + "  zz:"+zz ) ;
									pos[0] = xx;
									pos[1] = yy;
									pos[2] = zz;
									ra.setPosition(pos);
									pixelValue = (int)ra.get().getRealFloat();
									if (pixelValue > 0) { //Binary 0 and >0
										totals[b] += 1;
										pixelFound = true;
									    break;
									}	
								}
								if (pixelFound) break;
							}
							if (pixelFound) break;
						}
					}
				}
			}
		}//b
		return totals;
	}
	
	/**
	 * This method calculates the Boxes
	 * @return eps
	 */
	@Override
	public double[] calcEps() {

		eps = new double[numBoxes];
		for (int n = 0; n < numBoxes; n++) {
			eps[n] = (int)Math.pow(2, n);
		}
		return eps;
	}	

}
