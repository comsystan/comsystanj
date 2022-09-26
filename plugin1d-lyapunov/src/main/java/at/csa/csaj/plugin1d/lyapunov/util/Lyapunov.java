/*-
 * #%L
 * Project: ImageJ2 sequence plugin for computing the Lyapunov exponent.
 * File: Lyapunov.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2022 Comsystan Software
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

package at.csa.csaj.plugin1d.lyapunov.util;

import org.scijava.log.LogService;
import at.csa.csaj.commons.regression.LinearRegression;
import at.csa.csaj.commons.utils.DoubleSort;

/**
 * Lyapunov exponent from a sequence
 * @author Helmut Ahammer
 * @since 2022 02
 */

public class Lyapunov {

	private LogService logService;
	
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	private double[] dataX;
	private double[]   dataY;
	private double[][] dataYs;
	

	public double[] getDataX() {
		return dataX;
	}

	public void setDataX(double[] dataX) {
		this.dataX = dataX;
	}

	public double[] getDataY() {
		return dataY;
	}
	public double[][] getDataYs() {
		return dataYs;
	}

	public void setDataY(double[] dataY) {
		this.dataY = dataY;
	}
	
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
	public Lyapunov() {

	}
	
	/**
	 * This method scales the sequence to [0,1]
	 * @param sequence1d
	 * @return scaled sequence 
	 */
	public double[] scaleToUnity(double[] sequence1d) {
		
		double min =  Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		double maxMinusMin = 0.0;

		for (int i = 0; i < sequence1d.length - 1; i++) {
			if (sequence1d[i] < min) min = sequence1d[i];
			if (sequence1d[i] > max) max = sequence1d[i];
		}
		 maxMinusMin = max - min;
	 
		//scale the sequence to [0,1]
		for (int i = 0; i < sequence1d.length - 1; i++) {
			sequence1d[i] = (sequence1d[i]-min)/maxMinusMin;
		}	
	
//		//Check scaling
//		min =  Double.MAX_VALUE;
//		max = -Double.MAX_VALUE;
//		maxMinusMin = 0.0;	
//		for (int i = 0; i < sequence1d.length - 1; i++) {
//			if (sequence1d[i] < min) min = sequence1d[i];
//			if (sequence1d[i] > max) max = sequence1d[i];
//		}
//		maxMinusMin = max - min;
		
		return sequence1d;
	}
	
	/**
	 * This method computes the Lyapunov exponent λ(x0) according to Schuster Equ. 3.9 on p2
	 * @param sequence1d
	 * @return
	 */
	public double calcLyaX0(double[] sequence1d) {
		return  calcLyaX0(sequence1d, 1);
	}
	
	/**
	 * *****NOTE: DOES NOT COMPUTE RELIABLE RESULTS*******************************************************
	 * This method computes the Lyapunov exponent λ(x0) according to Schuster Equ. 3.9 on p22
	 * This means that e^λ(x0) is the average factor by which the distance between closely adjacent
	 * points becomes stretched after one iteration.
	 * It is simply the sum of log of abs of derivatives
	 * Or one may say that it is the sum of local Lyapunov exponents.
	 * @param sequence1d
	 * @param eps == delta of x-axis from one point to another
	 * @return
	 */
	public double calcLyaX0(double[] sequence1d, int eps) {
		
		double lyaX0 = 0.0;	
	
		sequence1d = this.scaleToUnity(sequence1d);
		int num = 0;
		double dist = 0;
		for (int i = 0; i < sequence1d.length - eps; i++) {
			dist = Math.abs(sequence1d[i+eps] - sequence1d[i]);
			if (dist != 0) {
				lyaX0 = lyaX0 + Math.log(dist/eps);
				num = num + 1;
			}			
		}
		return lyaX0/num;
	}

	
	/**
	 * Author HA
	 * Searches for nearby points that have a value distance < eps and 
	 * sums up the value distances at a delay k
	 * 
	 * Is not reliable for e.g. Sine function
	 *  
	 * @param sequence1d
	 * @param eps neighborhood delta of x-axis from one point to another
	 * @param k  maximum  delay/lag
	 * @return divergences
	 */
	public double[] calcLyaDirect(double[] sequence1d, double eps, int kMax) {
		
		int N = sequence1d.length;
		int M = 0;
		double dist    = 0.0;
		double sumDist = 0.0;
		double[]   divergences = new double[kMax];
		double[][] diffs       = new double[N][kMax];
		
		for (int k = 0; k < kMax; k++) {
			M = N-k;
			for (int i = 0; i < M; i++) {
				for (int j = i + 1; j < M; j++) {
					if (Math.abs(sequence1d[i] - sequence1d[j]) < eps) { //neighboring points
						//if (((i+k) < N) && ((j+k) < N)) {
							dist = Math.abs(sequence1d[i+k] - sequence1d[j+k]); //distance of neighboring points at distance k
							if (dist != 0) diffs[i][k] = Math.log(dist);		 
						//}
					}
				}
			}
		}
		//Get divergences for each k
		//Get mean of all differences 
		for (int k = 0; k < kMax; k++) {
			//M = N-k;
			sumDist = 0.0;	
			int num = 0;
			for (int i = 0; i < N; i++) {
				dist = diffs[i][k];
				if (dist != 0) {
					sumDist = sumDist + dist;
					num = num + 1;
				}
			}
			divergences[k] = sumDist/num; 	
			//System.out.println("Lypunov: Lyapunov("+k+"): "+dist);		
		}
		
		return divergences;
	}
	
	/**
	 * This method computes the phase spcae reconstruction
	 * 
	 * @param data  1D data double[]
	 * @param m embedding dimension
	 * @param tau time lag
	 * 
	 * @return double[][] psr   Mxm matrix
 	 */
	public double[][] calcPSR(double[] data, int m,  int tau) {
		
		int numVec = data.length - (m-1)*tau;
		return calcPSR(data, m, tau, numVec);
	}
	
	/**
	 * This method computes the phase space reconstruction
	 * 
	 * @param data  1D data double[]
	 * @param m embedding dimension
	 * @param tau time lag
	 * @param numVec number of reconstructed vectors
	 * 
	 * @return double[][] psr   M x m matrix
 	 */
	public double[][] calcPSR(double[] data, int m,  int tau, int numVec) {
		
		int M = numVec;
		double[][] psr = new double[M][m];

		for (int i = 0; i < m; i++) {
			for (int jj = 0; jj < M; jj++) {
				psr[jj][i] = data[jj+i*tau];
			}
		}
		return psr;
	}
	
	/**
	 * This method calculates the "Divergences" of the series according to the Rosenstein paper
	 * Rosenstein, Michael T., James J. Collins, und Carlo J. De Luca. „A Practical Method for Calculating Largest Lyapunov Exponents from Small Data Sets“. Physica D: Nonlinear Phenomena 65, Nr. 1 (15. Mai 1993): 117–34. https://doi.org/10.1016/0167-2789(93)90009-P.
	 * Adapted from Merve Kizilkaya's MatLab code
	 * https://de.mathworks.com/matlabcentral/fileexchange/38424-largest-lyapunov-exponent-with-rosenstein-s-algorithm
	 * 
	 * Point pairs with position distances (not value distances!) <= periodMean are not considered 
	 * NOTE: Must be at least 0 to remove point pairs constructed of one single point with value distance = 0
	 * periodMean >= 0: 
	 * 
	 * @param data  1D data double[]
	 * @param m embedding dimension
	 * @param tau time lag
	 * @param periodMean
	 * @param kMax
	 * @param numInitialPoint
	 * 
	 * @return double[] divergences
	 */
	public double[] calcDivergencesRosenstein(double[] data, int m, int tau, int periodMean, int kMax) {
		int N = data.length;
		int M = N - (m-1)*tau;
		
		//Normalization to [0,1] is not necessary, does not change the result
		//data = this.scaleToUnity(data);
		
		double[][] psr         = new double[M][m]; //M data points   m embedding dimension
		double[]   divergences = new double[kMax]; 
		//double[][] x0          = new double[M][m];
		double[][] diffs       = new double[M][m];
		double[]   distances   = new double[M];
		double[]   nearDist    = new double[M];
		int[]      nearIndx    = new int[M];
		double min        = Double.MAX_VALUE;
		int    indxMax    = 0;
		int    indxMin    = 0;
		double sumDist    = 0.0;
		double countDist  = 0.0;
		double dist       = 0.0;
			
		//Compute phase space reconstruction
		psr = calcPSR(data, m, tau, M);
		
		for (int i = 0; i < M; i++) {
			
			distances   = new double[M];
			
			//Generate Initial points matrix
//			for (int ii = 0; ii < M; ii++) {
//				for (int jj = 0; jj < m; jj++) {
//					x0[ii][jj] = psr[i][jj]; //The content of psr in row i is copied to every row of X0  index i and not ii for psr[i][jj]!!!!!!!!!
//				}
//			}
							
			//Diff Matrix
			//Simply all differences of all pairs of data points
			//Without restricting to a neigborhood
			//According to Schreiber 1995 this is a god way for n<1000 data points  
			for (int ii = 0; ii < M; ii++) {
				for (int jj = 0; jj < m; jj++) {
					diffs[ii][jj] = psr[ii][jj] - psr[i][jj]; //This is shorter as with additional matrix - x0[ii][jj];      //All differences to the row i
					diffs[ii][jj] = diffs[ii][jj]*diffs[ii][jj];
				}
			}
			
			//Distances, Sum of rows	
			for (int ii = 0; ii < M; ii++) {	
				for (int jj = 0; jj < m; jj++) {
					distances[ii] = distances[ii] + diffs[ii][jj];
				}
				distances[ii] = Math.sqrt(distances[ii]); //Wurzel(a^2 + b^2 +....)
			}
			
			//Neglect distances of very near points
			//Point pairs with position distances (not value distances!) <= periodMean are not considered 
			//NOTE: Must be at least 0 to remove point pairs constructed of one single point with value distance = 0
			//periodMean >= 0: 
			for (int ii = 0; ii < M; ii++) {	
				if (Math.abs(ii-i) <= periodMean) distances[ii] = Double.MAX_VALUE; //These distances are not of interest any more
			}
			
			//Searching for the smallest distance
			min = Double.MAX_VALUE;
			for (int ii = 0; ii < M; ii++) {	
				if (distances[ii] < min) {
					min     = distances[ii];
					indxMin = ii;
				}
			}					
			nearDist[i] = min;      //The distance to the nearest neighbor for each data point i
			nearIndx[i] = indxMin;  //The index    of the nearest neighbor for each data point i			
		} //i  [0,M-1]

		//Compute divergences with time lag k
		for (int k = 0; k < kMax; k++) {
		
			indxMax   = M-k;
			dist      = 0.0;
			sumDist   = 0.0;
			countDist = 0.0;
		
			for (int i = 0; i < M; i++) {	
				if ((i < indxMax) && (nearIndx[i] < indxMax)) {				
					dist = 0.0;
					for (int jj = 0; jj < m; jj++) {	
						dist = dist + ((psr[i+k][jj]-psr[nearIndx[i]+k][jj])*(psr[i+k][jj]-psr[nearIndx[i]+k][jj]));
					}
					dist = Math.sqrt(dist);
					if (dist != 0) {
						sumDist   = sumDist + Math.log(dist);
						countDist = countDist + 1.0;
					}			
				}
			}			
			if (countDist > 0) {
				divergences[k] = sumDist/countDist; //Mean divergence over all points i for k
			}
			else {
				divergences[k] = 0.0;		
			}
		} //k
		return divergences;
	}
	
	/**
	 * This method calculates the "Divergences" according to the Kantz paper
	 * Kantz, Holger. „A Robust Method to Estimate the Maximal Lyapunov Exponent of a Time Series“. Physics Letters A 185, Nr. 1 (31. Januar 1994): 77–87. https://doi.org/10.1016/0375-9601(94)90991-1.
	 * 
	 * Point pairs with position distances (not value distances!) <= periodMean are not considered 
	 * NOTE: Must be at least 0 to remove point pairs constructed of one single point with value distance = 0
	 * periodMean >= 0: 
	 * 
	 * @param data  1D data double[]
	 * @param m embedding dimension
	 * @param tau time lag
	 * @param periodMean
	 * @param kMax
	 * @param mumInitialPoints
	 * 
	 * @return double[] divergences
	 */
	public double[][] calcDivergencesKantz(double[] data, int m, int tau, int periodMean, int kMax, int numInitialPoints) {
		int N = data.length;
		int M = N - (m-1)*tau;
		
		//Normalization to [0,1] is not necessary, does not change the result
		//data = this.scaleToUnity(data);
	
		int numEps = numInitialPoints; //Number of maximal neighboring points;		
			
		double[][] psr         = new double[M][m]; //M data points   m embedding dimension
		double[][] divergences = new double[numEps][kMax]; 
		//double[][] x0          = new double[M][m];
		double[][] diffs       = new double[M][m];
		double[]   distances   = new double[M];
		double[][] nearDist    = new double[numEps][M];
		int[][]    nearIndx    = new int[numEps][M];
		//double min        = Double.MAX_VALUE;
		int    indxMax    = 0;
		//int    indxMin    = 0;
		double meanDist  = 0.0;
		double countDist = 0.0;
		double dist      = 0.0;
		double[] sortedDoubles;
		int[] sortedIndxs; 
			
		//Compute phase space reconstruction
		psr = calcPSR(data, m, tau, M);
		
		for (int i = 0; i < M; i++) {
			
			distances   = new double[M];
			
			//Generate Initial points matrix
//			for (int ii = 0; ii < M; ii++) {
//				for (int jj = 0; jj < m; jj++) {
//					x0[ii][jj] = psr[i][jj]; //The content of psr in row i is copied to every row of X0  index i and not ii for psr[i][jj]!!!!!!!!!
//				}
//			}
							
			//Diff Matrix
			//Simply all differences of all pairs of data points
			//Without restricting to a neighborhood
			//According to Schreiber 1995 this is a god way for n<1000 data points  
			for (int ii = 0; ii < M; ii++) {
				for (int jj = 0; jj < m; jj++) {
					//Kantz uses absolute distances instead of Euclidean norm
					diffs[ii][jj] = Math.abs(psr[ii][jj] - psr[i][jj]); //This is shorter as with additional matrix - x0[ii][jj];      //All differences to the row i
					//diffs[ii][jj] = diffs[ii][jj]*diffs[ii][jj];
				}
			}
			
			//Distances, Sum of rows	
			for (int ii = 0; ii < M; ii++) {	
				for (int jj = 0; jj < m; jj++) {
					distances[ii] = distances[ii] + diffs[ii][jj];
				}
				//distances[ii] = Math.sqrt(distances[ii]);
			}
			
			//Neglect distances of very near points
			//Point pairs with position distances (not value distances!) <= periodMean are not considered 
			//NOTE: Must be at least 0 to remove point pairs constructed of one single point with value distance = 0
			//periodMean >= 0: 
			for (int ii = 0; ii < M; ii++) {	
				if (Math.abs(ii-i) <= periodMean) distances[ii] = Double.MAX_VALUE; //These distances are not of interest any more
			}
			
			//Searching for the smallest distances	
			DoubleSort ds = new DoubleSort(distances);
			sortedDoubles = ds.getSortedDoubles();
			sortedIndxs   = ds.getSortedIndices(); 
	
			for (int e = 0; e < numEps; e++) { 
				nearDist[e][i] = sortedDoubles[e]; //numEps smallest values 
				nearIndx[e][i] = sortedIndxs[e];   //numEps indices to these smallest values
			}			
		} //i  [0,M-1]
	
		//Compute divergences with time lag k
		for (int k = 0; k < kMax; k++) {
			
			indxMax   = M-k;
			dist      = 0.0;
			meanDist  = 0.0;
			countDist = 0.0;
		
			for (int e = 0; e < numEps; e++) { //initial neighboring points
				for (int i = 0; i < M; i++) {	
					if ((i < indxMax) && (nearIndx[e][i] < indxMax)) {				
						dist = 0.0;
						for (int jj = 0; jj < m; jj++) {	
							//dist = dist + ((psr[i+k][jj]-psr[nearIndx[e][i]+k][jj])*(psr[i+k][jj]-psr[nearIndx[e][i]+k][jj]));
							//Kantz uses absolute distances instead of Euclidean norm
							dist = dist + Math.abs(psr[i+k][jj]-psr[nearIndx[e][i]+k][jj]);
						}
						//dist = Math.sqrt(dist);
						if (dist != 0) {
							meanDist = meanDist + Math.log(dist);
							countDist = countDist + 1.0;
						}			
					}	
				}			
				if (countDist > 0) divergences[e][k] = meanDist/countDist; //Mean divergence for k
				else divergences[e][k] = 0.0;
			}
		}
		return divergences;
	}

	/**
	 * 
	 * @param divergences Lyapunov divergences
	 * @param regStart
	 * @param regEnd
	 * @return double regression parameters
	 */
	public double[] calcRegressionsMean(double[][] divergences, int regStart, int regEnd) {
		double[] regressionParams;
		double[] regressionParamsMean = null;
	
		int numEps = divergences.length;
		dataYs = new double[numEps][ divergences[0].length];
	
		for (int e = 0; e < numEps; e++) {
			regressionParams = calcRegression(divergences[e], regStart, regEnd);
			dataYs[e] = dataY;
			if (e == 0) {
				regressionParamsMean = new double[regressionParams.length];
			}
		
			for (int r = 0; r < regressionParams.length; r ++) {
				regressionParamsMean[r] = regressionParamsMean[r] + regressionParams[r];
			}
		}
		for (int r = 0; r < regressionParamsMean.length; r ++) {
			regressionParamsMean[r] = regressionParamsMean[r]/numEps;
		}
		return regressionParamsMean;
	}
	
	
	/**
	 * 
	 * @param divergences Lyapunov divergences
	 * @param regStart
	 * @param regEnd
	 * @return double regression parameters
	 */
	public double[] calcRegression(double[] divergences, int regStart, int regEnd) {
	
		dataX = new double[divergences.length]; //k
		dataY = new double[divergences.length];

		for (int i = 0; i < divergences.length; i++) {
			dataX[i] = i + 1;
			dataY[i] = divergences[i];
		}
	
		// Compute regression
		LinearRegression lr = new LinearRegression();

		//double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, regStart, regEnd);
		double[] regressionParams = lr.calculateParameters(dataX, dataY, regStart, regEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		return regressionParams;
	}
	
}
