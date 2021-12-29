/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing fractal dimension with Katz algorithm.
 * File: Katz.java
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

package at.csa.csaj.sig.frac.dim.katz.util;

import org.scijava.log.LogService;

/**
 * 
 * <p>
 * <b>Katz algorithm</b>
 * According to Katz, M., 1988, Comput. Biol. Med., 18, 145–156
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2021 06
 */

public class Katz {

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
	public Katz() {

	}

	/**
	 * This method calculates the "Length" of the series
	 * defined by the sum of Euclidean distances dist(p1,p2) = sqrt[(x1-x2)^2 + (y1-y2)^2];
	 * (x1-x2) = 1 in our case
	 * This method computes the Katz dimension with the maximal Euclidean distance to the first data point
	 * @param data  1D data double[]
	 * @return double Dk
	 */
	public double calcDimension(double[] data) {
		int dataLength = data.length;
		double Dk = Double.NaN;
		double distMax = 0.0;
		double distMean = 0.0;
		double[] distances = new double[dataLength - 1];
	
		double L = 0.0;
		for (int n = 0; n < dataLength-1; n++) {
			L = L + Math.sqrt(1 + (data[n+1]-data[n])*(data[n+1]-data[n]));
		}
		
		//Distances
		for (int d = 0; d < distances.length; d++) {
			distances[d] = Math.sqrt(((d+2)-1)*((d+2)-1) + (data[d+1]-data[0])*(data[d+1]-data[0]));
		}
		//Get maximal distance and mean distance
		for (int d = 1; d < distances.length; d++) {
			if (distances[d] >= distMax) distMax = distances[d]; 
			distMean += distances[d];
		}
		distMean = distMean/distances.length;
			
		//Get normalisation factor
		double  n = (double)dataLength-1.0;
		//double  n = L/distMean;
		
		Dk = Math.log10(n)/(Math.log10(n) + Math.log10(distMax/L));
		return Dk;
	}


}
