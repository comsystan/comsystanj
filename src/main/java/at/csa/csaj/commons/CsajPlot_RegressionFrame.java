/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajPlot_RegressionFrame.java
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
package at.csa.csaj.commons;

import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.WindowConstants;

/**
 * This method shows XY Data in an extra window. This class uses JFreeChart: a
 * free chart library for the Java(tm) platform http://www.jfree.org/jfreechart/
 * 
 * @author Helmut Ahammer
 */
@SuppressWarnings("rawtypes")
public class CsajPlot_RegressionFrame extends CsajPlot_Frame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1736340059325958928L;

	public CsajPlot_RegressionFrame(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String legendLabel, int numRegStart, int numRegEnd) {
		super(frameTitle);

		CsajPlot_Regression rp = new CsajPlot_Regression(dataX, dataY, isLineVisible,
				frameTitle, imageTitle, xLabel, yLabel, legendLabel, numRegStart, numRegEnd);

		this.setContentPane(rp);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);	
	}

	public CsajPlot_RegressionFrame(double[] dataX, double[][] dataY,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String[] legendLabels, int numRegStart, int numRegEnd) {
		super(frameTitle);

		CsajPlot_Regression rp = new CsajPlot_Regression(dataX, dataY, isLineVisible,
				frameTitle, imageTitle, xLabel, yLabel, legendLabels, numRegStart, numRegEnd);

		this.setContentPane(rp);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);
	}
	
}
