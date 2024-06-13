/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Petrosian.java
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

package at.csa.csaj.plugin1d.frac.util;

import org.scijava.log.LogService;

/**
 * 
 * <p>
 * <b>Perosian algorithm</b>
 * According to Petrosian, A, 1995, IEEE Symposium on Computer Based Medical Systems, 212–217
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2021 06
 */

public class Petrosian {

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
	public Petrosian() {

	}

	/**
	 *
	 * This method computes the Petrosian dimension
	 * @param data  1D data double[]
	 * @param method of binarization "Mean", "Mean+-SD", "Sign of Difference", "SD of Differences"
	 * @return double Dp
	 */
	public double calcDimension(double[] data, String method) {
		int dataLength = data.length;
		double mean = 0.0;
		double sd = 0.0;
		double Dp = Double.NaN;
		double[] diffSequence = new double[dataLength - 1];
		long[] binarySequence = new long[dataLength];
		
		if (method.equals("Mean")) {
			for (int n = 0; n < dataLength; n++) {
				mean = mean + data[n];
			}
			mean = mean/dataLength;
			
			for (int n = 0; n < dataLength; n++) {
				if (data[n] >= mean) {
					binarySequence[n] = 1;
				} else {
					binarySequence[n] = 0;
				}
			}		
		}
		else if (method.equals("Mean+-SD")) {
			mean = 0.0;
			sd = 0.0;
			for (int n = 0; n < dataLength; n++) {
				mean = mean + data[n];
			}
			mean = mean/dataLength;
		
			for (int n = 0; n < dataLength; n++) {
				sd = sd + (data[n] - mean)*(data[n] - mean);
			}
			sd = Math.sqrt(sd/dataLength);
			
			double thres1 = mean + sd;
			double thres2 = mean - sd;
			for (int n = 0; n < dataLength; n++) {
				if ((data[n] > thres1)||(data[n] < thres2)) {
					binarySequence[n] = 1;
				} else {
					binarySequence[n] = 0;
				}
			}		
		}
		else if (method.equals("Sign of Difference")) {
			for (int n = 0; n < dataLength - 1; n++) {
			
				if ((data[n+1] - data[n]) >= 0) {
					binarySequence[n] = 1;
				} else {
					binarySequence[n] = 0;
				}
			}
		}
		else if (method.equals("SD of Differences")) {
			mean = 0.0;
			sd = 0.0;
			for (int n = 0; n < dataLength - 1; n++) {
				diffSequence[n] = data[n+1] - data[n];;

			}
			for (int n = 0; n < diffSequence.length; n++) {
				mean = mean + diffSequence[n];
			}
			mean = mean/diffSequence.length;
		
			for (int n = 0; n < diffSequence.length; n++) {
				sd = sd + (diffSequence[n] - mean)*(diffSequence[n] - mean);
			}
			sd = Math.sqrt(sd/diffSequence.length);
			
			double thres1 = mean + sd;
			double thres2 = mean - sd;
			for (int n = 0; n < diffSequence.length; n++) {
				if ((diffSequence[n] > thres1)||(diffSequence[n] < thres2)) {
					binarySequence[n] = 1;
				} else {
					binarySequence[n] = 0;
				}
			}	
		}
		
		//Compute numSignChanges;
		long numSignChanges = 0L;
		
		for (int n = 0; n < binarySequence.length-1; n++) {
			if (binarySequence[n+1] != binarySequence[n]) numSignChanges += 1;
		}
		
		Dp = Math.log10((double)dataLength)/(Math.log10((double)dataLength) + Math.log10((double)dataLength/((double)dataLength + 0.4*numSignChanges)));
		
		return Dp;
	}


}
