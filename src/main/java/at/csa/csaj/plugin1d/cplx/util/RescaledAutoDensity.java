/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: RescaledAutoDensity.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2025 Comsystan Software
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

package at.csa.csaj.plugin1d.cplx.util;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.scijava.log.LogService;

import at.csa.csaj.commons.CsajRegression_Linear;

/**
 * 
 * <p>
 * <b>Rescaled auto-density (RAD)</b>
 * 
 * according to Harris, L. Gollo, B.D. Fulcher. "Tracking the distance to criticality in systems with unknown noise", Physical Review X 14: 031021 (2024)
 * https://doi.org/10.1103/PhysRevX.14.031021
 * 
 * https://github.com/DynamicsAndNeuralSystems/RAD/tree/v0.1.0?tab=readme-ov-file
 * https://time-series-features.gitbook.io/time-series-analysis-tools/time-series-features/rad
 * 
 * noise-insensitive measure of the distance to criticality.
 * Product of the spread of differences and tailedness of the distribution
 * RAD takes negative values far from criticality and approaches zero when a system is close to the critical point.
 * 
 * The centered RAD denoted as cRAD is applicable to nonradial data.
 * 
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2025 02
 */

public class RescaledAutoDensity {

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
	public RescaledAutoDensity() {

	}

	/**
	 * This method calculates the RAD and cRAD (centered RAD)
	 * @param data  1D data double[]
	 * @param delay tau 
	 * @return double[] RAD cRAD
	 */
	public double[] calcDensities(double[] data, int tau) {
		int dataLength = data.length;
		double[] rad = new double[2]; //RAD and cRAD
		double[] deltaX  = new double[dataLength];
		double[] centerX = new double[dataLength];
		
		double value;
		double median;
		double deltaSD;
		double belowSD;
		double aboveSD;
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		//Get RAD	
		//Set vectors
		for (int i = 0; i < dataLength; i++) {
			deltaX[i] = Double.NaN;
		}
		
		//Get delta sequence
		for (int i = 0; i < (dataLength - tau); i++) {
			deltaX[i] = data[i + tau] - data[i];
		}
			
		//Get SD of delta sequence
		stats.clear();
		for( int i = 0; i < deltaX.length; i++) {
			value = deltaX[i];
			if (!Double.isNaN(value)) stats.addValue(value);
		}
		deltaSD = stats.getStandardDeviation();
		
		//Get median of original sequence
		stats.clear();
		for( int i = 0; i < data.length; i++) {
			value = data[i];
			if (!Double.isNaN(value)) stats.addValue(value);	
		}
		median = stats.getPercentile(50);
			
		//Get below median SD
		stats.clear();
		for( int i = 0; i < data.length; i++) {
			value = data[i];
			if (!Double.isNaN(value) && value < median) stats.addValue(value);
		}
		belowSD = stats.getStandardDeviation();
		
		//Get above median SD
		stats.clear();
		for( int i = 0; i < data.length; i++) {
			value = data[i];
			if (!Double.isNaN(value) && value >= median) stats.addValue(value);
		}
		aboveSD = stats.getStandardDeviation();
		
		rad[0] = deltaSD*(1.0/aboveSD - 1.0/belowSD);//RAD
		
		//*************************************************************************************************
		//Repeat all for centered RAD:
		for (int i = 0; i < dataLength; i++) {
			deltaX[i]  = Double.NaN;
			centerX[i] = Double.NaN;
		}
			
		//Get centered sequence
		for (int i = 0; i < dataLength; i++) {
			centerX[i] = Math.abs(data[i]-median);
		}
		
		//Get delta sequence
		for (int i = 0; i < (dataLength - tau); i++) {
			deltaX[i] = centerX[i+tau]-centerX[i];
		}
			
		//Get SD of delta sequence
		stats.clear();
		for( int i = 0; i < deltaX.length; i++) {
			value = deltaX[i];
			if (!Double.isNaN(value)) stats.addValue(value);
		}
		deltaSD = stats.getStandardDeviation();
		
		//Get median of sequence
		stats.clear();
		for( int i = 0; i < centerX.length; i++) {
			value = centerX[i];
			if (!Double.isNaN(value)) stats.addValue(value);
		}
		median = stats.getPercentile(50);
			
		//Get below media SD
		stats.clear();
		for( int i = 0; i < centerX.length; i++) {
			value = centerX[i];
			if (!Double.isNaN(value) && value < median) stats.addValue(value);
		}
		belowSD = stats.getStandardDeviation();
		
		//Get above media SD
		stats.clear();
		for( int i = 0; i < centerX.length; i++) {
			value = centerX[i];
			if (!Double.isNaN(value) && value >= median) stats.addValue(value);
		}
		aboveSD = stats.getStandardDeviation();
		
		rad[1] = deltaSD*(1.0/aboveSD - 1.0/belowSD);//cRAD
		

		return rad;
	}
}
