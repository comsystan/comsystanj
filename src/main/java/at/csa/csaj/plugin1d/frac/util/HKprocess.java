/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: HKprocess.java
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

package at.csa.csaj.plugin1d.frac.util;

import java.util.Arrays;

import org.apache.commons.math3.optim.MaxEval;

import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.scijava.log.LogService;

/**
 * Hurst Kolmogorov process Hurst coefficient
 * 
 * Converted to Java and adapted from an R project:
 * https://cran.r-project.org/package=HKprocess
 * 
 * According to:
 * Tyralis, Hristos, und Demetris Koutsoyiannis.
 * „A Bayesian Statistical Model for Deriving the Predictive Distribution of Hydroclimatic Variables“.
 * Climate Dynamics 42, Nr. 11 (1. Juni 2014): 2867–83.
 * https://doi.org/10.1007/s00382-013-1804-y.
 *
 * Specially suited for small sequence lengths
 * 
 * <p>
 * <b>Changes</b>
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2023 12
 */

public class HKprocess {

	private LogService logService;
	
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	private double[] sequence;
	private double hurst;

	public double[] getSequence() {
		return sequence;
	}

	public void setSequence(double[] sequence) {
		this.sequence = sequence;
	}
	
	public double getH() {
		return hurst;
	}

	public void setH(double hurst) {
		this.hurst = hurst;
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
	public HKprocess() {

	}
	
	/**
	 * This is a constructor with a LogService
	 */
	public HKprocess(LogService logService) {
		this.logService = logService;
	}
	
	
	/**
	 * This method calculates the Hurst coefficient using the Hurst-Kolmogorov method
	 * 
	 * @param sequence  1D data double[]
	 * @param iterations n   
	 * @return double H
	 */
	public double computeH(double[] data, int n) {
		double add = 0.001;
	    double minu = 0.001;
	    double maxu = 0.999;
	    return computeH(data, n, add, minu, maxu);
	}

	/**
	 * This method calculates the Hurst coefficient using the Hurst-Kolmogorov method
	 * 
	 * @param sequence  1D data double[]
	 * @param iterations n   
	 * @return double H
	 */
	public double computeH(double[] data, int n, double add, double minu, double maxu) {
		double H = 0.0;
				
	    double logM = optimprice(data); // Maximum value of logphxfunction
	    //System.out.println("HKProcess logM: " + logM);
        double[] estimatesOfH = new double[n];
        
        for (int i = 0; i < n; i++) {
            estimatesOfH[i] = acceptReject(data, logM, add, minu, maxu);
            //System.out.println("HKprocess  i(n):" + i + "("+n+")" + "  estimatesOfH[i]:"+estimatesOfH[i] );
        }

		Median median = new Median();
		H = median.evaluate(estimatesOfH);
		this.hurst = H;
		//System.out.println("HKProcess H: " + H);
		return H;
	}
	
	// Set natural logarithm of eq. 10 in Tyralis and Koutsoyiannis (2014)
    private double logphxfunction(double H, double[] x) {
        int size    = x.length;
        int maxlag  = size - 1;
        int    nx   = x.length;
        double EPS  = Double.MIN_VALUE;
        double[] a1 = new double[4]; //Result from ltz method
        int fault   = 999;
        fault = ltza(acfHKp(H, maxlag), nx, x, nx, EPS, a1); //a1 will be the result
        //fault = ltzb(acfHKp(H, maxlag), nx, x, nx, EPS, a1); //a1 will be the result //ERROR: NaN because a1[2] = 0
        //fault = ltzc(acfHKp(H, maxlag), nx, x, nx, EPS, a1); //a1 will be the result //ERROR: NaN because a1[2] = 0
        //fault = ltzd(acfHKp(H, maxlag), nx, x, nx, EPS, a1); //a1 will be the result //ERROR: NaN because a1[1] = and a1[2] = 0
        //System.out.println("HKprocess fault:" + fault);
         
        double log = -0.5 * a1[3] - 0.5 * (size - 1) * Math.log(a1[2] * a1[0] - Math.pow(a1[1], 2)) + (0.5 * size - 1) * Math.log(a1[2]);
        return log;
    }
    
    private double optimprice(double[] x) {
        UnivariateObjectiveFunction f = new UnivariateObjectiveFunction(H -> logphxfunction(H, x));
        OptimizationData bounds = new SearchInterval(0.00001, 0.99999);
        MaxEval maxEval = new MaxEval(200);
        UnivariateOptimizer optimizer = new BrentOptimizer(1e-6, 1e-12);
        UnivariatePointValuePair result = optimizer.optimize(maxEval, f, GoalType.MAXIMIZE, bounds);
        return result.getValue(); //.getPoint() would be the location of the maximum
    }
 
    //Accept-Reject algorithm to simulate from H
    //See: Robert, C.P., Casella, G., Casella, G.: Monte Carlo Statistical Methods vol. 2. Springer, New York, NY (1999)
    //Note: Acceptance-rejection sampling is is a Monte Carlo method that has largely been replaced by newer Markov Chain Monte Carlo methods. It’s usually only used when it’s challenging to sample individual distributions within a larger Markov chain (Christensen et al., 2011).Christensen, R. et al., (2011). Bayesian Ideas and Data Analysis: An Introduction for Scientists and Statisticians. CRC Press.
    private double acceptReject(double[] x, double logM, double add, double minu, double maxu) {
        double dist = 1 / (maxu - minu);
        double logM1 = logM - Math.log(dist) + add;
        RandomDataGenerator randomDataGenerator = new RandomDataGenerator();
        double u = randomDataGenerator.nextUniform(0, 1);
        double logu = Math.log(u) + logM1;
        double y = randomDataGenerator.nextUniform(minu, maxu);
        int n = 0;
        while (logu > logphxfunction(y, x) - Math.log(dist)) {
            u = randomDataGenerator.nextUniform(0, 1);
            logu = Math.log(u) + logM1;
            y = randomDataGenerator.nextUniform(minu, maxu);
            n += 1;
            if (n >= 500) {
            	//System.out.println("HKprocess: Accept/Reject algorithm aborted, reason might be a non stationary fBm signal." );
            	if (logService != null) logService.info(this.getClass().getName() + " Accept/Reject algorithm aborted, reason might be a non stationary fBm signal.");
            	return Double.NaN; //Algorithm does not converge.    	
            }
        }
        return y;
    }
    
    // Autocorrelation function
    public double[] acfHKp(double H, int maxlag) {
        double h2 = 2 * H;
        int[] k = new int[maxlag];
        for (int i = 0; i < maxlag; i++) {
            k[i] = i + 1;
        }
        double[] result = new double[maxlag + 1];
        result[0] = 1;
        for (int i = 1; i <= maxlag; i++) {
            result[i] = 0.5 * (Math.pow(k[i - 1] + 1, h2) - 2 * Math.pow(k[i - 1], h2) + Math.pow(k[i - 1] - 1, h2));
        }
        return result;
    }
    
    /*******************************************************************************
    ltza

    Takes arguments r,x, and EPSL:
        r: The autocorrelation vector of size nn x 1 for a normal stochastic process.
        x: A column vector of size nnx x 1.
        EPSL: Used to define precision in control statements.

    Changes vector y:
        y[0]: Equals to t(x) * inv(R) * x.
        y[1]: Equals to t(e3) * inv(R) * x.
        y[2]: Equals to t(e3) * inv(R) * e3.
        y[3]: Computation of the logarithm of the determinant of R.
        t(.): Denotes the transpose of a matrix.
        inv(.): Denotes the inverse of a matrix.
        R: The autocorrelation matrix formed from r.

    Possible values of fault and corresponding fault conditions are:
        0 The program is normally performed
        1 Error ("Singular Matrix")
        2 Error ("Input r[0] is not equal to 1.")
        3 Error ("The length(r) is not equal to the length(x))"

    Uses the lev and levDet functions
    *******************************************************************************/
    public int ltza(double[] r, int nn, double[] x, int nnx, double EPSL, double[] y) {
        
    	int fault;
        double EPS, s1, s2, s3;
        int n = nn, nx = nnx, i, k, _fault1;
        if (n != nx) {
            for (i = 0; i < 2; i++) y[i] = 0.0;
            fault = 3; // The length(r) is not equal to the length(x)
            return fault;
        }
        EPS = EPSL;
        double[] y1 = new double[n];
        double[] e1 = new double[n - 1];
        _fault1 = lev(r, n, x, y1, e1, EPS); // y1 solution of the system R * y1 = x
        if (_fault1 != 0) {
            for (i = 0; i < 2; i++) y[i] = 0.0;
            fault = _fault1;
            return fault;
        } else {
            fault = 0; // The program is normally performed
            double[] y2 = new double[n];
            double[] e2 = new double[n - 1];
            double[] e3 = new double[n];
            // e3: vector of size n x 1 with all elements equal to 1.
            Arrays.fill(e3, 1.0);
            lev(r, n, e3, y2, e2, EPS); // y2 solution of the system R * y2 = e3
            y[3] = levDet(n - 1, e2); // Computation of the logarithm of the determinant.
            s1 = sum(n, y2); // s1 equals to t(e3) * inv(R) * e3.
            s2 = sum(n, y1); // s2 equals to t(e3) * inv(R) * x.
            s3 = dot(n, x, y1); // s3 equals to t(x) * inv(R) * x.
            y[0] = s3; // s3 equals to t(x) * inv(R) * x.
            y[1] = s2; // s2 equals to t(e3) * inv(R) * x.
            y[2] = s1; // s1 equals to t(e3) * inv(R) * e3.
        }
        return fault;
    }
    
    /*******************************************************************************
    ltzb

    Takes arguments r,x, and EPSL:
        r: The autocorrelation vector of size nn x 1 for a normal stochastic process.
        x: A column vector of size nnx x 1.
        EPSL: Used to define precision in control statements.

    Changes vector y:
        y[0]: Equals to t(e3) * inv(R) * x.
        y[1]: Equals to t(e3) * inv(R) * e3.
        t(.): Denotes the transpose of a matrix.
        inv(.): Denotes the inverse of a matrix.
        R: The autocorrelation matrix formed from r.

    Possible values of fault and corresponding fault conditions are:
        0 The program is normally performed
        1 Error ("Singular Matrix")
        2 Error ("Input r[0] is not equal to 1.")
        3 Error ("The length(r) is not equal to the length(x))"

    Uses the lev function
    *******************************************************************************/
    public int ltzb(double[] r, int nn, double[] x, int nnx, double EPSL, double[] y) {
    	int fault;
        double EPS;
        int n = nn, nx = nnx, i, k, _fault1;
        if (n != nx) {
            for (i = 0; i < 2; i++)
                y[i] = 0.0;
            fault = 3; // The length(r) is not equal to the length(x)
            return fault;
        }
        EPS = EPSL;
        double[] y1 = new double[n];
        double[] e1 = new double[n - 1];
        _fault1 = lev(r, n, x, y1, e1, EPS); // y1 solution of the system R * y1 = x
        if (_fault1 != 0) {
            for (i = 0; i < 2; i++)
                y[i] = 0.0;
            fault = _fault1;
            return fault;
        } else {
            fault = 0; // The program is normally performed
            double[] y2 = new double[n];
            double[] e2 = new double[n - 1];
            double[] e3 = new double[n];
            // e3: vector of size n x 1 with all elements equal to 1.
            Arrays.fill(e3, 1.0);
            lev(r, n, e3, y2, e2, EPS); // y2 solution of the system R * y2 = e3
            y[0] = sum(n, y1); // s2 equals to t(e3) * inv(R) * x.
            y[1] = sum(n, y2); // s1 equals to t(e3) * inv(R) * e3.
        }
        return fault;
    }
    
    /*******************************************************************************
    ltzc

    Takes arguments r,x, and EPSL:
        r: The autocorrelation vector of size nn x 1 for a normal stochastic process.
        x: A column vector of size nnx x 1.
        EPSL: Used to define precision in control statements.

    Changes vector y:
        y[0]: Equals to t(x) * inv(R) * x.
        y[1]: Computation of the logarithm of the determinant of R.
        t(.): Denotes the transpose of a matrix.
        inv(.): Denotes the inverse of a matrix.
        R: The autocorrelation matrix formed from r.

    Possible values of fault and corresponding fault conditions are:
        0 The program is normally performed
        1 Error ("Singular Matrix")
        2 Error ("Input r[0] is not equal to 1.")
        3 Error ("The length(r) is not equal to the length(x))"

    Uses the lev and levDet functions
    *******************************************************************************/
    public int ltzc(double[] r, int nn, double[] x, int nnx, double EPSL, double[] y) {
    	int fault;
        double EPS;
        int n = nn, nx = nnx, i, _fault1;
        if (n != nx) {
            for (i = 0; i < 2; i++)
                y[i] = 0.0;
            fault = 3; // The length(r) is not equal to the length(x)
            return fault;
        }
        EPS = EPSL;
        double[] y1 = new double[n];
        double[] e1 = new double[n - 1];
        _fault1 = lev(r, n, x, y1, e1, EPS); // y1 solution of the system R * y1 = x
        if (_fault1 != 0) {
            for (i = 0; i < 2; i++)
                y[i] = 0.0;
            fault = _fault1;
            Arrays.fill(y1, 0.0);
            Arrays.fill(e1, 0.0);
            return fault;
        } else {
            fault = 0; // The program is normally performed
            y[1] = levDet(n - 1, e1); // Computation of the logarithm of the determinant.
            y[0] = dot(n, x, y1); // s3 equals to t(x) * inv(R) * x.
            Arrays.fill(y1, 0.0);
            Arrays.fill(e1, 0.0);
        }
        return fault;
    }
    
    /*******************************************************************************
    ltzd

    Takes arguments r,x, and EPSL:
        r: The autocorrelation vector of size nn x 1 for a normal stochastic process.
        x: A column vector of size nnx x 1.
        EPSL: Used to define precision in control statements.

    Changes vector y:
        y[0]: Equals to t(x) * inv(R) * x.
        t(.): Denotes the transpose of a matrix.
        inv(.): Denotes the inverse of a matrix.
        R: The autocorrelation matrix formed from r.

    Possible values of fault and corresponding fault conditions are:
        0 The program is normally performed
        1 Error ("Singular Matrix")
        2 Error ("Input r[0] is not equal to 1.")
        3 Error ("The length(r) is not equal to the length(x))"

    Uses the lev function
    *******************************************************************************/
    public int ltzd(double[] r, int nn, double[] x, int nnx, double EPSL, double[] y) {
    	int fault;
        double EPS;
        int n = nn, nx = nnx, i, _fault1;
        if (n != nx) {
            for (i = 0; i < 2; i++)
                y[i] = 0.0;
            fault = 3; // The length(r) is not equal to the length(x)
            return fault;
        }
        EPS = EPSL;
        double[] y1 = new double[n];
        double[] e1 = new double[n - 1];
        _fault1 = lev(r, n, x, y1, e1, EPS); // y1 solution of the system R * y1 = x
        if (_fault1 != 0) {
            for (i = 0; i < 2; i++)
                y[i] = 0.0;
            fault = _fault1;
            Arrays.fill(y1, 0.0);
            Arrays.fill(e1, 0.0);
            return fault;
        } else {
            fault = 0; // The program is normally performed
	        y[0] = dot(n, x, y1); // s3 equals to t(x) * inv(R) * x.
	        Arrays.fill(y1, 0.0);
	        Arrays.fill(e1, 0.0);
        }
        return fault;
    }

    public int lev(double[] r, int n, double[] x, double[] y, double[] e, double EPS) {
        double[] v = new double[n-1];
        double[] l = new double[n-1];
        double[] b = new double[n];
        double[] c = new double[n-1];
        int n1 = n - 1;
        int i, j, k, m;
        n1 = n - 1;
        if (Math.abs(r[0] - 1.0) > EPS) {
            // error("r[0] is not equal to 1");
            return 2;
        }
        e[0] = 1.0 - r[1] * r[1];
        if (e[0] < EPS) {
            // nrerror("Singular Matrix-1");
            return 1;
        }
        v[0] = -r[1];
        l[0] = x[1] - r[1] * x[0];
        b[0] = -r[1];
        b[1] = 1.0;
        y[0] = (x[0] - r[1] * x[1]) / e[0];
        y[1] = l[0] / e[0];
        for (i = 1; i < n1; i++) {
            v[i] = -dot(i + 1, Arrays.copyOfRange(r, 1, r.length), b) / e[i - 1];
            e[i] = e[i - 1] * (1 - v[i] * v[i]);
            l[i] = x[i + 1] - flipupdot(i + 1, Arrays.copyOfRange(r, 1, r.length), y);
            for (k = 0; k < i + 1; k++) {
                c[k] = b[i - k];
            }
            b[i + 1] = b[i];
            for (j = i; j > 0; j--) {
                b[j] = b[j - 1] + v[i] * c[j];
            }
            b[0] = v[i] * c[0];
            y[i + 1] = (l[i] / e[i]) * b[i + 1];
            for (m = i; m > -1; m--) {
                y[m] = y[m] + (l[i] / e[i]) * b[m];
            }
        }
        return 0;
    }
    
    public double levDet(int n, double[] e) {
        double logDet = 0.0;
        for (int i = 0; i < n; i++) {
            logDet += Math.log(e[i]);
        }
        return logDet;
    }

    private double sum(int n, double[] arr) {
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += arr[i];
        }
        return sum;
    }
    
    private double dot(int n, double[] a, double[] b) {
        double result = 0.0;
        for (int i = 0; i < n; i++) {
            result += a[i] * b[i];
        }
        return result;
    }
    
    private double flipupdot(int n, double[] a, double[] b) {
        double result = 0.0;
        for (int i = 0; i < n; i++) {
            result += a[i] * b[n - i - 1];
        }
        return result;
    }
  
}
