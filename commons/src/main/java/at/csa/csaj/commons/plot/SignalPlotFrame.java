/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: PlotDisplayFrame.java
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

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.ToolTipManager;

import org.scijava.table.DefaultGenericTable;

//import at.mug.iqm.api.Resources;
//import at.mug.iqm.commons.util.CommonTools;

/**
 * This method shows XY Data in an extra window. This class uses JFreeChart: a
 * free chart library for the Java(tm) platform http://www.jfree.org/jfreechart/
 * 
 * @author 2021 Helmut Ahammer,
 */
public class SignalPlotFrame extends CSAJPlotFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2888121190097316789L;


	/**
	 * This constructor creates an instance that displays a plot containing a
	 * single data series.
	 */
	@SuppressWarnings("rawtypes")
	public SignalPlotFrame(DefaultGenericTable defaultGenericTable, int col, boolean isLineVisible,
			String frameTitle, String imageTitle, String xLabel, String yLabel) {
		super(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(defaultGenericTable, col,
				isLineVisible, imageTitle, xLabel, yLabel);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);	
	}

	/**
	 * This constructor creates an instance that displays multiple data series
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public SignalPlotFrame(DefaultGenericTable defaultGenericTable, int[] cols,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String[] seriesLabels) {
		super(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(defaultGenericTable, cols,
				isLineVisible, imageTitle, xLabel, yLabel, seriesLabels);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);	
	}
	
	/**
	 * This constructor creates an instance that displays a plot containing a
	 * single data series.
	 */
	@SuppressWarnings("rawtypes")
	public SignalPlotFrame(double[] dataX, DefaultGenericTable defaultGenericTable, int col, boolean isLineVisible,
			String frameTitle, String imageTitle, String xLabel, String yLabel) {
		super(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(dataX, defaultGenericTable, col,
				isLineVisible, imageTitle, xLabel, yLabel);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);	
	}
	
	/**
	 * This constructor creates an instance that displays multiple data series
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public SignalPlotFrame(double[] dataX, DefaultGenericTable defaultGenericTable, int[] cols,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String[] seriesLabels) {
		super(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(dataX, defaultGenericTable, cols,
				isLineVisible, imageTitle, xLabel, yLabel, seriesLabels);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);	
	}
	
	/**
	 * This constructor creates an instance that displays multiple data series
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public SignalPlotFrame(double dataX[], double[] dataY,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String legendLabel) {
		super(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(dataX, dataY,
				isLineVisible, imageTitle, xLabel, yLabel, legendLabel);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);	
	}
	
	/**
	 * This constructor creates an instance that displays multiple data series
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public SignalPlotFrame(double dataX[], double[][] dataY,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String[] seriesLabels) {
		super(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(dataX, dataY,
				isLineVisible, imageTitle, xLabel, yLabel, seriesLabels);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);	
	}
	
	/**
	 * This constructor creates an instance that displays a data series and a second one on top (e.g. for event display)
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public SignalPlotFrame(double dataX[], double[] dataY,  double[] dataX2, double[] dataY2,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String dataLegendLabel, String data2LegendLabel) {
		super(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(dataX, dataY, dataX2, dataY2,
				isLineVisible, imageTitle, xLabel, yLabel, dataLegendLabel,  data2LegendLabel);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
		//Set position to the top right corner of the screen
		GraphicsConfiguration config = this.getGraphicsConfiguration();
	    Rectangle bounds = config.getBounds();
	    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
	    int x = bounds.x + bounds.width - insets.right - this.getWidth();
	    int y = bounds.y + insets.top;
	    this.setLocation(x, y);	
	}
	
	/**
	 * This constructor creates an instance that displays data series and second ones on top (e.g. for event display)
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public SignalPlotFrame(double dataX[], double[][] dataY,  double[] dataX2, double[][] dataY2,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String[] dataLegendLabels, String[] data2LegendLabels) {
		super(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(dataX, dataY, dataX2, dataY2,
				isLineVisible, imageTitle, xLabel, yLabel, dataLegendLabels, data2LegendLabels);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
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
