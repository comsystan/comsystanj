/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: PortaGuzikEhler.java
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

package at.csa.csaj.plugin1d.cplx.util;

import org.scijava.log.LogService;

import at.csa.csaj.commons.CsajRegression_Linear;

/**
 * 
 * <p>
 * <b>Porta Guzig Ehler indices using distances from the diagonal line in the Poincare plot</b>
 * According to Porta et al, Temporal asymmetries of short-term heart period variability are linked to autonomic regulation“.
 * American Journal of Physiology-Regulatory, Integrative and Comparative Physiology, 2008, 295, Nr. 2 R550–57.
 * https://doi.org/10.1152/ajpregu.00129.2008.
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2024 01
 */

public class PortaGuzikEhler {

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
	public PortaGuzikEhler() {

	}

	/**
	 * This method calculates the Porta index
	 * with perpendicular distances to the linear regression line in a Poincare plot
	 * @param data  1D data double[]
	 * @param delay tau for Poincare plot
	 * @return double[] Porta Guzig Ehler indices
	 */
	public double[] calcIndices(double[] data, int tau) {
		int dataLength = data.length;
		double[] dataY = new double[dataLength];
		CsajRegression_Linear lr;
		double[] pgi = new double[3];
		
	
		//Generate Poincare plot
		for (int i = 0; i < (dataLength - tau); i++) {
			dataY[i] = data[i + tau];
		}
		
		// Compute regression
//		lr= new LinearRegression();
//		double[] regressionParams = lr.calculateParameters(data, dataY, 1, dataLength);
//		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
//		
//		//ax + by + c = 0 //line equation in a plane
//		//a, b, c of the regression line
//		double a = regressionParams[1];
//		double b = -1.0;
//		double c = regressionParams[0];
		

		//ax + by + c = 0  //line equation in a plane
		//a, b, c of the diagonal line
		double a = 1;
		double b = -1.0;
		double c = 0;
		
		double dist;
		//for Porta
		float numMinus = 0;
		float numNotZero = 0;
		
		//for Guzig
		float sumDistPlusSqu = 0;
		
		//for Guzig and Ehler
		float sumDistAllSqu  = 0;
		
		//for Ehler
		float sumDistAllThird = 0;
		
		//Scroll over all points
		for (int x = 0; x < dataLength; x++) {
			for (int y = 0; y < dataLength; y++) {	
				// Perpendicular (shortest) distance of a point to a line
				dist= ((a*x + b*y + c)) / (Math.sqrt(a*a + b*b));
				
				if (dist < 0)  numMinus   += 1f;
				if (dist != 0) numNotZero += 1f;
				
				if (dist > 0) sumDistPlusSqu += dist*dist;
				sumDistAllSqu   += dist*dist;
				sumDistAllThird += dist*dist*dist;		
			}//y
		}//x
		pgi[0] = numMinus/numNotZero;//*100f; //Porta index
		pgi[1] = sumDistPlusSqu/sumDistAllSqu;//*100f; //Guzig index
		pgi[2] = sumDistAllThird/Math.pow(sumDistAllSqu, 3.0/2.0); //Ehler index
		
		return pgi;
	}


}
