/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Fit_Polynomial2D.java
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
package at.csa.csaj.commons;

import Jama.Matrix;

/*
 * This is an adapted version from the ImageJ plugin  https://imagej.nih.gov/ij/plugins/polynomial-fit/index.html
 * by Dwight Bartholomew (Dwight.Bartholomew at L-3com.com), L3 Communications Infrared Products
 * 
 * Warnings: Outlier pixels can adversely affect the fit so I recommend removing severe outliers prior to using this plugin.
 * Using too high a fitting polynomial order will lead to poor fitting.
 * 
 * This class computes surface fit coefficients for z.
 * For example, if the surface fit is quadratic along the vertical
 * and cubic along the horizontal than
 * z =    (p0 y^2 + p1  y^1 + p2  y^0) x^3
 *      + (p3 y^2 + p4  y^1 + p5  y^0) x^2
 *      + (p6 y^2 + p7  y^1 + p8  y^0) x^1
 *      + (p9 y^2 + p10 y^1 + p11 y^0) x^0
 * where:   x = column index
 *          y = row index
 *          z = image intensity
 * Problem expressed in matrix form as [XY]*[P] = [Z].
 * Solve for the [P] coefficients using a least squares method
 * and written in matrix notation as [P] = inv[XY]*[Z].
 * Reference = see, for example, "Numerical Methods in C", by Press, Teukolsky, Vetterling, & Flannery, 2nd Edition, page 671
 */

/**
 * @author Helmut Ahammer
 * @date   2021-08
 *
 */

//Use it in this way*****************
//Apply detrending (flattening) with polynomials of order = 1
//imgArr has flipped xy indices!!!!!
//double[][] imgArr = new double[(int)imgSubSampled.dimension(1)][(int)imgSubSampled.dimension(0)];
//// Write to image
//cursorF = img.localizingCursor();
//int[] pos = new int[2]; 
//while (cursorF.hasNext()) {
//	cursorF.fwd();
//	cursorF.localize(pos);
//	imgArr[pos[1]][pos[0]] = cursorF.get().get();
//}	
////Remove mean value to keep the math from blowing up
//double meanImage = 0.0;
//for (int y = 0; y < imgArr.length; y++) {
//	for (int x = 0; x < imgArr[0].length; x++) {
//    	meanImage += imgArr[y][x];
//    }	
//}
//meanImage = meanImage/(imgArr.length*imgArr[0].length);
//for (int y = 0; y < imgArr.length; y++) {
//	for (int x = 0; x < imgArr[0].length; x++) {
//    	imgArr[y][x] -= meanImage;
//    }	
//}
//int polyOrderX = 1;
//int polyOrderY = 1;
//PolynomialFit2D pf2D = new PolynomialFit2D();
//double[][] polyParams = pf2D.calculateParameters(imgArr, polyOrderX, polyOrderY);
//
//double dTemp;
//double yTemp;
//// Create an image of the fitted surface
//// Example:                
////    dtemp =  (polyParams[3][3]*y*y*y + polyParams[2][3]*y*y + polyParams[1][3]*y + polyParams[0][3])*x*x*x;
////    dtemp += (polyParams[3][2]*y*y*y + polyParams[2][2]*y*y + polyParams[1][2]*y + polyParams[0][2])*x*x;
////    dtemp += (polyParams[3][1]*y*y*y + polyParams[2][1]*y*y + polyParams[1][1]*y + polyParams[0][1])*x;
////    dtemp += (polyParams[3][0]*y*y*y + polyParams[2][0]*y*y + polyParams[1][0]*y + polyParams[0][0]);
//double[][] imgArrPolySurface = new double[imgArr.length][imgArr[0].length];
//for (int y = 0; y < imgArr.length; y++) {
//	for (int x = 0; x < imgArr[0].length; x++) {
//		 dTemp = 0;
//         // Determine the value of the fit at pixel iy,ix
//         for(int powx = polyOrderX; powx >= 0; powx--) {
//             yTemp = 0;
//             for(int powy = polyOrderY; powy >= 0; powy--) {
//                 yTemp += polyParams[powy][powx] * Math.pow((double)y,(double)powy);
//             }
//             dTemp += yTemp * Math.pow((double)x,(double)powx);
//         }
//         // Add back the mean of the image
//         imgArrPolySurface[y][x] = dTemp + meanImage;
//    }	
//}
////Subtract polySurface and write back to imgSubSampled
////Note that values of img will then be distributed around 0 and contain negative values 
//cursorF = img.localizingCursor();
//pos = new int[2];
//while (cursorF.hasNext()) {
//	cursorF.fwd();
//	cursorF.localize(pos);			
//	cursorF.get().set(cursorF.get().get() - (float)imgArrPolySurface[pos[1]][pos[0]]);
//}	



public class Fit_Polynomial2D {
	
	public double[][] calculateParameters(double[][] image, int polyOrderX, int polyOrderY ) {
	    int nRows = image.length;
	    int nCols = image[0].length;
	    int nPixels = nRows*nCols;
	    int n=0;
	    int r, c, cnt, i, j, k, MatVal, nCol;
	    int dim1, dim2;
	    int po_2xp1 = Math.max((2 * polyOrderX + 1), (2 * polyOrderY + 1));
	    int matSize = (polyOrderX+1)*(polyOrderY+1);

	    // Create the x, y, and z arrays from which the image to be fitted
	    double []xArr = new double[nPixels];
	    double []yArr = new double[nPixels];
	    double []zArr = new double[nPixels];
	    cnt = 0;
	    for(r=0; r<nRows; r++) {
	        for(c=0; c<nCols; c++) {
	            xArr[cnt] = c;
	            yArr[cnt] = r;
	            zArr[cnt] = image[r][c];
	            cnt++;
	        }
	    }

	    // Notation:
	    //  1)  The matrix [XY] is made up of sums (over all the pixels) of the
	    //      row & column indices raised to various powers.  For example,
	    //      sum( y^3 * x^2 ).  It turns out, within [XY] there are 
	    //      patterns to the powers and these patterns are computed
	    //      in the matrices [nnx] and [nny].
	    //  2)  [Sxyz] represents all of the possible sums that will be used to
	    //      create [XY] and [Z].  We compute all of these sums even though 
	    //      some of them might not be utilized... it's just easier.
		double [][]xyMat = new double[matSize][matSize];
	    int [][]nnx      = new int[matSize][matSize];
	    int [][]nny      = new int[matSize][matSize];
	    int []aRow       = new int[matSize];

	    // Create all the possible sums, Sxyz[][][]
	    // Preparing sums matrix
	    double[][][] Sxyz = new double[po_2xp1][po_2xp1][2];
	    double x, y, z;
	    double powx, powy, powz;
	    int nx, ny, nz;
	    // Initialize all of the sums to zero
	    for(nx=0; nx<po_2xp1; nx++) {
	        for(ny=0; ny<po_2xp1; ny++) {
	            for(nz=0; nz<2; nz++) {
	                Sxyz[nx][ny][nz] = 0.0;
	            }
	        }
	    }
	    // Produce the sums
	    for( i=0; i<nPixels; i++) {
	        x = xArr[i]; y = yArr[i]; z = zArr[i];
	        for(nx=0; nx<po_2xp1; nx++) {
	            powx = Math.pow(x,(double)nx);
	            for(ny=0; ny<po_2xp1; ny++) {
	                powy = Math.pow(y,(double)ny);
	                for(nz=0; nz<2; nz++) {
	                    powz = Math.pow(z,(double)nz);
	                    Sxyz[nx][ny][nz] += powx * powy * powz;
	                }
	            }
	        }
	    }

	    // Create the patterns of "powers" for the X (horizontal) pixel indices
	    int iStart = 2 * polyOrderX;
	    dim1 = 0;
	    while(dim1<matSize) {
	        for(i=0; i<(polyOrderY+1); i++) {
	            // A row of nnx[][] consists of an integer that starts with a value iStart and
	            //  1) is repeated (PolyOrderX+1) times
	            //  2) decremented by 1
	            //  3) Repeat steps 1 and 2 for a total of (PolyOrderY+1) times
	            nCol = 0;
	            for(j=0; j<(polyOrderX+1); j++ ) {
	                for(k=0; k<(polyOrderY+1); k++) {
	                    aRow[nCol] = iStart - j;
	                    nCol++;
	                }
	            }
	            // Place this row into the nnx matrix
	            for(dim2=0; dim2<matSize; dim2++ ) {
	                nnx[dim1][dim2] = aRow[dim2];
	            }
	            dim1++;
	        }
	        iStart--;
	    }
	    
	    // Create the patterns of "powers" for the Y (vertical) pixel indices
	    dim1 = 0;
	    while(dim1<matSize) {
	        iStart = 2 * polyOrderY;
	        for(i=0; i<(polyOrderY+1); i++) {
	            // A row of nny[][] consists of an integer that starts with a value iStart and
	            //  1) place in matrix
	            //  2) decremented by 1
	            //  3) 1 thru 2 are repeated for a total of (polyOrderX+1) times
	            //  4) 1 thru 3 are repeat a total of (polyOrderY+1) times
	            nCol = 0;
	            for(j=0; j<(polyOrderX+1); j++ ) {
	                for(k=0; k<(polyOrderY+1); k++) {
	                    aRow[nCol] = iStart - k;
	                    nCol++;
	                }
	            }
	            // Place this row into the nnx matrix
	            for(dim2=0; dim2<matSize; dim2++ ) {
	                nny[dim1][dim2] = aRow[dim2];
	            }
	            dim1++;
	            iStart--;
	        }
	    }

	    // Put together the [XY] matrix
		for(r=0; r<matSize; r++) {
			for(c=0; c<matSize; c++) {
				nx = nnx[r][c];
				ny = nny[r][c];
				xyMat[r][c] = Sxyz[nx][ny][0];
			}
		}

	    // Put together the [Z] vector
		double[] zMat = new double[matSize];
	    c = 0;
	    for(i=polyOrderX; i>=0; i--) {
			for(j=polyOrderY; j>=0; j--) {
	            zMat[c] = Sxyz[i][j][1];
	            c++;
	        }
	    }

	    // Solve the linear system [XY] [P] = [Z] using the Jama.Matrix routines
		// 	[A_mat] [x_vec] = [b_vec]
		// (see example at   http://math.nist.gov/javanumerics/jama/doc/Jama/Matrix.html)
	    // Solving linear system of equations
		Matrix aMat = new Matrix(xyMat);
		Matrix bVec = new Matrix(zMat, matSize);
		Matrix xVec = aMat.solve(bVec);

		// Place the Least Squares Fit results into the array pFit
		double[] pFit = new double[matSize];
		for(i=0; i<matSize; i++) {
			pFit[i] = xVec.get(i, 0);
		}

		// Reformat the results into a 2-D array where the array indices
	    // specify the power of pixel indices.  For example,
	    // z =    (g[2][3] y^2 + g[1][3] y^1 + g[0][3] y^0) x^3
	    //      + (g[2][2] y^2 + g[1][2] y^1 + g[0][2] y^0) x^2
	    //      + (g[2][1] y^2 + g[1][1] y^1 + g[0][1] y^0) x^1
	    //      + (g[2][0] y^2 + g[1][0] y^1 + g[0][0] y^0) x^0
	    double[][] gFit = new double[polyOrderY + 1][polyOrderX + 1];
	    c = 0;
	    for(i=polyOrderX; i>=0; i--) {
			for(j=polyOrderY; j>=0; j--) {
	            gFit[j][i] = pFit[c];
	            c++;
	        }
	    }
	    return gFit;
	}
	

}
