/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: RegressionPlotFrame.java
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
package at.csa.csaj.commons.plot;

import javax.swing.WindowConstants;

/*
 
import java.util.Vector;

/**
 * This method shows XY Data in an extra window. This class uses JFreeChart: a
 * free chart library for the Java(tm) platform http://www.jfree.org/jfreechart/
 * 
 * @author Helmut Ahammer
 */
@SuppressWarnings("rawtypes")
public class RegressionPlotFrame extends PlotDisplayFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1736340059325958928L;

	public RegressionPlotFrame(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, int regStart, int regEnd) {
		super(frameTitle);

		RegressionPlot rp = new RegressionPlot(dataX, dataY, isLineVisible,
				frameTitle, imageTitle, xLabel, yLabel, regStart, regEnd);

		this.setContentPane(rp);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		
		
	}

	public RegressionPlotFrame(double[] dataX, double[][] dataY,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String[] seriesNames, int regStart, int regEnd) {
		super(frameTitle);

		RegressionPlot rp = new RegressionPlot(dataX, dataY, isLineVisible,
				frameTitle, imageTitle, xLabel, yLabel, seriesNames, regStart, regEnd);

		this.setContentPane(rp);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
	}

}
