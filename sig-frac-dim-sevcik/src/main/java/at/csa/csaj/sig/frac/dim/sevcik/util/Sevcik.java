/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing fractal dimension with Sevcik algorithm.
 * File: Sevcik.java
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

package at.csa.csaj.sig.frac.dim.sevcik.util;

import org.scijava.log.LogService;

/**
 * 
 * <p>
 * <b>Sevcik algorithm</b>
 * According to  Sevcik, C., 1998, Complexity International, 5
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2021 06
 */

public class Sevcik {

	private LogService logService;

	private int progressBarMin = 0;
	private int progressBarMax = 100;
		
	public int getProgressBarMin() {
		return progressBarMin;
	}

	public void setProgressBarMin(int progressBarMin) {
		this.progressBarMin = progressBarMin;
	}

	public int getProgressBarMax() {
		return progressBarMax;
	}

	public void setProgressBarMax(int progressBarMax) {
		this.progressBarMax = progressBarMax;
	}

	/**
	 * This is the standard constructor
	 */
	public Sevcik() {

	}

	/**
	 * This method computes the Sevcik dimension
	 * @param data  1D data double[]
	 * @return double Dk
	 */
	public double calcDimension(double[] data) {
		int dataLength = data.length;
		double Ds = Double.NaN;
		double sample = Double.NaN;
		double[] dataX = new double[dataLength];
		double[] dataY = new double[dataLength];
	
		double minX = 0;
		double maxX = dataLength - 1;
		
		double minY =  Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		
		for (int n = 0; n < dataLength; n++) {
			sample = data[n];
			if (sample > maxY) maxY = sample;
			if (sample < minY) minY = sample;
		}
		
		//Normalise signal to unit square
		for (int n = 0; n < dataLength; n++) {
			dataX[n] = (double)n/maxX;
			dataY[n] = (data[n] - minY)/(maxY - minY);		
		}
		
		
		//Check normalisation
//		minX =  Double.MAX_VALUE;
//		maxX = -Double.MAX_VALUE;
//		minY =  Double.MAX_VALUE;
//		maxY = -Double.MAX_VALUE;
//		
//		for (int n = 0; n < dataLength; n++) {
//			if (dataX[n] > maxX) maxX = dataX[n];
//			if (dataX[n] < minX) minX = dataX[n];
//			if (dataY[n] > maxY) maxY = dataY[n];
//			if (dataY[n] < minY) minY = dataY[n];
//		}
//		System.out.println("Sevcik: minX " + minX +"   maxX " + maxX);
//		System.out.println("Sevcik: minY " + minY +"   maxY " + maxY);
		
		double L = 0.0;
		for (int n = 0; n < dataLength-1; n++) {
			L = L + Math.sqrt((dataX[n+1]-dataX[n])*(dataX[n+1]-dataX[n]) + (dataY[n+1]-dataY[n])*(dataY[n+1]-dataY[n]));
		}
		
		if (dataLength >= 1000) {
			//for large number of data points
			Ds = 1.0 + Math.log(L)/Math.log(2.0*(dataLength-1));
		}
		else {
			//for small number of data points
			Ds = 1.0 + (Math.log(L) - Math.log(2))/Math.log(2.0*(dataLength-1));
		}

		return Ds;
	}


}
