/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: LinearRegression.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2023 Comsystan Software
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
package at.csa.csaj.commons.regression;

import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * @author Helmut Ahammer
 * @update 2020-05
 *
 */
public class LinearRegression {
	/**
	 * This method computes regression parameters
	 * @param dataX
	 * @param dataY
	 * @param regStart  first value for regression 
	 * @param regEnd    last value for regression
	 * @return parameters including errors
	 */
	@SuppressWarnings("unused")
	public double[] calculateParameters(double[] dataX, double[] dataY, int regStart, int regEnd){
		double[] parameters = new double[5]; //p0, p1, StDErr1 Sterr2, r2     
		//number of data points for regression 
		int numRegPoints = (regEnd-regStart+1);
		//Apache Math3
		SimpleRegression simpleReg = new SimpleRegression(); //Apache Math3
		for (int i = 0; i < numRegPoints; i++){
			simpleReg.addData(dataX[i+regStart-1], dataY[i+regStart-1]);
		}	
		parameters[0] = simpleReg.getIntercept();    //y = parameters[0] + parameters[1] . x
		parameters[1] = simpleReg.getSlope();
		parameters[2] = simpleReg.getInterceptStdErr();
		parameters[3] = simpleReg.getSlopeStdErr();
		parameters[4] = simpleReg.getRSquare();       //R^2 Bestimmheitsmaß
		return parameters;
	}
	
	
	/**
	 * This method computes residuals
	 * @param dataX
	 * @param dataY
	 * @return parameters including errors
	 */
	@SuppressWarnings("unused")
	public double[] calculateResiduals(double[] dataX, double[] dataY){
		final int N = dataX.length;
		final double[] residuals = new double[N];
		//Apache Math3
		SimpleRegression simpleReg = new SimpleRegression(); //Apache Math3
		for (int i = 0; i < N; i++){
			simpleReg.addData(dataX[i], dataY[i]);
		}	
		
		for (int i = 0; i < N; i++) {
			residuals[i] = dataY[i] - simpleReg.predict(dataX[i]); //Simply the differences of the data y values and the computed regression y values.
		}
		
		return residuals; 
	}
	

}
