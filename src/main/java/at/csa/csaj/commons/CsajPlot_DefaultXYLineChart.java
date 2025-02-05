/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajPlot_DefaultXYLineChart.java
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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.DateRange;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;

/**
 * This class is a GUI element for a default 2D line chart.
 * 
 * @author Helmut Ahammer
 *
 */
public class CsajPlot_DefaultXYLineChart extends JPanel implements ChangeListener {

	/**
	 * The UID for serialization.
	 */
	private static final long serialVersionUID = 3141924072908771166L;

	private boolean isLineVisible = false;
	private ChartPanel chartPanel = null;

	private String title = null;
	private String xLabel = null;
	private String yLabel = null;

	private static int SLIDER_INITIAL_VALUE = 50;
	private JSlider slider;
	private DateAxis domainAxis;
	private int lastValue = SLIDER_INITIAL_VALUE;

	// one month (milliseconds, seconds, minutes, hours, days)
	private int delta = 1000 * 60 * 60 * 24 * 30;

	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(DefaultGenericTable defaultGenericTable, int col, boolean isLineVisible, String title, String xLabel, String yLabel) {

		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		
		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		int numColumns    = defaultGenericTable.getColumnCount();
		int numDataPoints = defaultGenericTable.getRowCount();
		double dataX[] = new double[numDataPoints];
		double dataY[] = new double[numDataPoints];
		
		Column<? extends Object> column = defaultGenericTable.get(col);
		for (int n = 0; n < numDataPoints; n++) {
			dataX[n] = n+1;
			double dataValue = Double.valueOf((Double)column.get(n));
			dataY[n] = dataValue;	
		}
		
		String seriesLabel = this.yLabel;
		XYDataset xyDataset = this.createXYDataset(dataX, dataY, seriesLabel);
		this.chartPanel.setChart(createChart(xyDataset));
		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);

	}

	/**
	 * This class displays multiple data series in a single plot window.
	 * 
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param title
	 * @param xLabel
	 * @param yLabel
	 */
	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(DefaultGenericTable defaultGenericTable, int[] cols, boolean isLineVisible, String title, String xLabel,
			String yLabel, String[] colNames) {
		
		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		int numColumns    = defaultGenericTable.getColumnCount();
		int numDataPoints = defaultGenericTable.getRowCount();
		double dataX[]  = new double[numDataPoints];
		double dataY[][] = new double[cols.length][numDataPoints];
		Column<? extends Object> column;
		String columnType;
		
		for (int c = 0; c < cols.length; c++) {
			column = defaultGenericTable.get(cols[c]);		
			columnType = column.get(0).getClass().getSimpleName();	
			if (columnType.equals("Double")) {
				for (int n = 0; n < numDataPoints; n++) {
					dataX[n] = n+1;
					double dataValue = Double.valueOf((Double)column.get(n));
					dataY[c][n] = dataValue;
				}
			} else { //e.g. String columns
				for (int n = 0; n < numDataPoints; n++) {
					dataX[n] = n+1;
					double dataValue = Double.NaN;
					dataY[c][n] = dataValue;
				}
			}
		}
		
		XYDataset xyDataset = this.createXYDataset(dataX, dataY, colNames);

		this.chartPanel.setChart(createChart(xyDataset));
		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);
	}
	
	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(double[] dataX, DefaultGenericTable defaultGenericTable, int col,  boolean isLineVisible, String title, String xLabel, String yLabel) {

		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		
		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		int numColumns    = defaultGenericTable.getColumnCount();
		int numDataPoints = defaultGenericTable.getRowCount();
		//double dataX[] = new double[numDataPoints];
		double dataY[] = new double[numDataPoints];
		
		Column<? extends Object> column = defaultGenericTable.get(col);
		for (int n = 0; n < numDataPoints; n++) {
			//dataX[n] = n+1;
			double dataValue = Double.valueOf((Double)column.get(n));
			dataY[n] = dataValue;	
		}
		
		String seriesLabel = this.yLabel;
		XYDataset xyDataset = this.createXYDataset(dataX, dataY, seriesLabel);
		this.chartPanel.setChart(createChart(xyDataset));
		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);

	}
	
	/**
	 * This class displays multiple data series in a single plot window.
	 * 
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param title
	 * @param xLabel
	 * @param yLabel
	 */
	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(double[] dataX, DefaultGenericTable defaultGenericTable, int[] cols, boolean isLineVisible, String title, String xLabel,
			String yLabel, String[] colNames) {
		
		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		int numColumns    = defaultGenericTable.getColumnCount();
		int numDataPoints = defaultGenericTable.getRowCount();
		//double dataX[]  = new double[numDataPoints];
		double dataY[][] = new double[cols.length][numDataPoints];
		Column<? extends Object> column;
		
		for (int c = 0; c < cols.length; c++) {
			column = defaultGenericTable.get(cols[c]);
			for (int n = 0; n < numDataPoints; n++) {
				//dataX[n] = n+1;
				double dataValue = Double.valueOf((Double)column.get(n));
				dataY[c][n] = dataValue;
			}
		}
		
		XYDataset xyDataset = this.createXYDataset(dataX, dataY, colNames);

		this.chartPanel.setChart(createChart(xyDataset));
		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);
	}
	
	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(double[] dataX, double[] dataY, boolean isLineVisible, String title, String xLabel, String yLabel, String legendLabel) {

		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;

		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		XYDataset dataset = this.createXYDataset(dataX, dataY, legendLabel);
		this.chartPanel.setChart(createChart(dataset));
		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);

	}

	/**
	 * This class displays multiple data series in a single plot window.
	 * 
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param title
	 * @param xLabel
	 * @param yLabel
	 */
	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(double[] dataX, double[][] dataY, boolean isLineVisible, String title, String xLabel,
			String yLabel, String[] legendLabels) {
		
		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		XYDataset dataset = this.createXYDataset(dataX, dataY, legendLabels);
		this.chartPanel.setChart(createChart(dataset));
		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);
	}
	
	/**
	 * This class displays multiple data series in a single plot window.
	 * 
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param title
	 * @param xLabel
	 * @param yLabel
	 */
	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(double[][] dataX, double[][] dataY, boolean isLineVisible, String title, String xLabel,
			String yLabel, String[] legendLabels) {
		
		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		XYDataset dataset = this.createXYDataset(dataX, dataY, legendLabels);
		this.chartPanel.setChart(createChart(dataset));
		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);
	}


	/**
	 * 2017-07- Adam Dolgos added second double[] for additional signs for points
	 * 
	 * @param dataX
	 * @param dataY
	 * @param dataX2
	 * @param dataY2
	 * @param isLineVisible
	 * @param title
	 * @param xLabel
	 * @param yLabel
	 */
	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(double[] dataX, double[] dataY, double[] dataX2, double[] dataY2, boolean isLineVisible,
			String title, String xLabel, String yLabel, String dataLegendLabel, String data2LegendLabel) {

		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;

		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		XYDataset xyDataset = this.createXYDataset(dataX, dataY, dataLegendLabel);
		//	this.chartPanel.setChart(createChart(xyDataset));
		//	this.chartPanel.setChart(createChart2(0, xyDataset));

		XYDataset xyDataset2 = this.createXYDataset(dataX2, dataY2, data2LegendLabel);
		this.chartPanel.setChart(createChart(xyDataset, xyDataset2));

		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);

	}

	@SuppressWarnings("rawtypes")
	public CsajPlot_DefaultXYLineChart(double[] dataX, double[][] dataY, double[] dataX2, double[][] dataY2, boolean isLineVisible,
			String title, String xLabel, String yLabel, String[] dataLegendLabels, String[] data2LegendLabels) {

		this.isLineVisible = isLineVisible;
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;

		this.chartPanel = new ChartPanel((JFreeChart) null, true);

		XYDataset xyDataset = this.createXYDataset(dataX, dataY, dataLegendLabels);
		//	this.chartPanel.setChart(createChart(xyDataset));
		//	this.chartPanel.setChart(createChart2(0, xyDataset));

		XYDataset xyDataset2 = this.createXYDataset(dataX2, dataY2, data2LegendLabels);

		this.chartPanel.setChart(createChart(xyDataset, xyDataset2));

		// this.setHorizontalAxisTrace(true);
		// this.setVerticalAxisTrace(true);
		this.chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
		this.chartPanel.setMouseZoomable(true, false);

		this.setLayout(new BorderLayout());
		this.add(this.chartPanel, BorderLayout.CENTER);

	}

	// XYDataset xyDataset2 - second series e.g. for detected events in Csaj1DDetectEventsCmd
	private JFreeChart createChart(XYDataset xyDataset, XYDataset xyDataset2) {

		//Define Them
		StandardChartTheme theme = null;
		Color colBack = null;
		Color colText = null;
		Color colGrid = null;
		//Color of first data series of a Dataset will be changed
		//Additional series are the default colors
		Color[] colArrayDataset  = ChartColor.createDefaultColorArray();	
		Color[] colArrayDataset2 = ChartColor.createDefaultColorArray();	// second series e.g. for detected events in Csaj1DDetectEventsCmd
		
		
		//Create dark theme or default theme
		LookAndFeel laf = UIManager.getLookAndFeel();	
		String lafName = laf.getName();
		if (lafName.equals("FlatLaf Dark") || lafName.equals("FlatLaf Darcula")) {//Dark theme
			theme = (StandardChartTheme) StandardChartTheme.createDarknessTheme();
			colBack = new Color(60, 63, 65); //FlatLaf dark grey
			colText = new Color(187, 187, 187); //FlatLaf bright grey
			colGrid = Color.YELLOW;
			colArrayDataset[0] = Color.WHITE;
			colArrayDataset[1] = Color.RED;
			colArrayDataset[2] = Color.BLUE;	
			colArrayDataset[3] = Color.ORANGE;	
			colArrayDataset2[0] = Color.RED; //RED is good for bright and dark themes e.g. events
			colArrayDataset2[1] = Color.YELLOW; //defined but not used in event detection
			colArrayDataset2[2] = Color.GREEN;	//defined but not used in event detection
			
		} else { //Default theme
			theme = (StandardChartTheme) StandardChartTheme.createJFreeTheme();
			colBack = Color.WHITE;
			colText = Color.BLACK;
			colGrid = Color.BLACK;
			colArrayDataset[0] = Color.BLACK;
			colArrayDataset[1] = Color.RED;
			colArrayDataset[2] = Color.BLUE;	
			colArrayDataset[3] = Color.ORANGE;	
			colArrayDataset2[0] = Color.RED; //RED is good for bright and dark themes e.g. events
			colArrayDataset2[1] = Color.YELLOW; //defined but not used in event detection
			colArrayDataset2[2] = Color.GREEN;	//defined but not used in event detection	
		}
				
		theme.setChartBackgroundPaint(colBack);
		theme.setPlotBackgroundPaint(colBack); //or null
		theme.setLegendBackgroundPaint(colBack);
		theme.setTitlePaint(colText);
		theme.setLegendItemPaint(colText);
		theme.setItemLabelPaint(colText);
		theme.setAxisLabelPaint(colText);
		theme.setTickLabelPaint(colText);
		theme.setDomainGridlinePaint(colGrid); //Domain == y-axis
		theme.setRangeGridlinePaint(colGrid);
		
		//Create a chart	
		JFreeChart chart = ChartFactory.createXYLineChart(title, // "", // title
				xLabel, // x-axis label
				yLabel, // y-axis label
				xyDataset, // data
				PlotOrientation.VERTICAL, // orientation?
				true, // generate legends?
				true, // generate tooltips?
				false // generate URLs?
		);
		
		theme.apply(chart);	
		
		//Set additional items
		chart.setTextAntiAlias(true);
		chart.setAntiAlias(true);	
		
		chart.getTitle().setFont(new Font("Arial", Font.PLAIN, 14));
		//chart.setBackgroundPaint(Color.WHITE); //already set by theme

		// legend to the right of the chart
		LegendTitle legend = (LegendTitle) chart.getSubtitle(0);
		legend.setPosition(RectangleEdge.RIGHT);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setDataset(1, xyDataset2);
		plot.setRenderer(1, new XYLineAndShapeRenderer());
		plot.getRangeAxis().setLabelFont(new Font("Arial", Font.PLAIN, 12)); //Range == x-axis
		plot.getDomainAxis().setLabelFont(new Font("Arial", Font.PLAIN, 12)); //Domain == y-axis
		//plot.setBackgroundPaint(null); //already set by theme
		//plot.setDomainGridlinePaint(Color.black); //Domain == y-axis //already set by theme
		//plot.setRangeGridlinePaint(Color.black); //Range == x-axis //already set by theme
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true); //Domain == y-axis
		plot.setRangeCrosshairVisible(true); //Range == x-axis
		
		//2017-7 Adam Dolgos ->>>>   JFreeCharts scrollable !!!!
		//JFreeCharts scrollable with ctrl + click!!!!
		plot.setDomainPannable(true); //Domain == y-axis
		plot.setRangePannable(true);  //Range == x-axis
		
		// plot.setPadding(0.0, 0.0, 0.0, 15.0);
		
		XYItemRenderer r = plot.getRenderer(0);
		if (r instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setDefaultShapesVisible(true); 
			renderer.setDefaultShapesFilled(false);
			renderer.setDefaultLinesVisible(isLineVisible);
			for (int c = 0; c < colArrayDataset.length; c++) renderer.setSeriesPaint(c, colArrayDataset[c]); 
		}
		
		r = plot.getRenderer(1); // second series e.g. for detected events in Csaj1DDetectEventsCmd
		if (r instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setDefaultShapesVisible(true); 
			renderer.setDefaultShapesFilled(true);
			renderer.setDefaultLinesVisible(isLineVisible);
			for (int c = 0; c < colArrayDataset2.length; c++) renderer.setSeriesPaint(c, colArrayDataset2[c]); 
		}
		
		return chart;

	}

	/**
	 * Creates a chart.
	 * 
	 * @param xyDataset a xyDataset.
	 * @return A chart.
	 */
	private JFreeChart createChart(XYDataset xyDataset) {
		
		//Define Them
		StandardChartTheme theme = null;
		Color colBack = null;
		Color colText = null;
		Color colGrid = null;
		//Color of first data series of a Dataset will be changed
		//Additional series are the default colors
		Color[] colArray = ChartColor.createDefaultColorArray();	
		
		//Create dark theme or default theme
		LookAndFeel laf = UIManager.getLookAndFeel();	
		String lafName = laf.getName();
		if (lafName.equals("FlatLaf Dark") || lafName.equals("FlatLaf Darcula")) {//Dark theme
			theme = (StandardChartTheme) StandardChartTheme.createDarknessTheme();
			colBack = new Color(60, 63, 65); //FlatLaf dark grey
			colText = new Color(187, 187, 187); //FlatLaf bright grey
			colGrid = Color.YELLOW;	
			colArray[0] = Color.WHITE;
			colArray[1] = Color.RED;
			colArray[2] = Color.BLUE;	
			colArray[3] = Color.ORANGE;	
		} else { //Default theme
			theme = (StandardChartTheme) StandardChartTheme.createJFreeTheme();
			colBack = Color.WHITE;
			colText = Color.BLACK;
			colGrid = Color.BLACK;
			colArray[0] = Color.BLACK;
			colArray[1] = Color.RED;
			colArray[2] = Color.BLUE;	
			colArray[3] = Color.ORANGE;	
		}
				
		theme.setChartBackgroundPaint(colBack);
		theme.setPlotBackgroundPaint(colBack); //or null
		theme.setLegendBackgroundPaint(colBack);
		theme.setTitlePaint(colText);
		theme.setLegendItemPaint(colText);
		theme.setItemLabelPaint(colText);
		theme.setAxisLabelPaint(colText);
		theme.setTickLabelPaint(colText);
		theme.setDomainGridlinePaint(colGrid); //Domain == y-axis
		theme.setRangeGridlinePaint(colGrid);
		
		//Create a chart
		JFreeChart chart = ChartFactory.createXYLineChart(title, // "", // title
				this.xLabel, // x-axis label
				this.yLabel, // y-axis label
				xyDataset, // data
				PlotOrientation.VERTICAL, // orientation?
				true, // generate legends?
				true, // generate tooltips?
				false // generate URLs?
		);
	
		theme.apply(chart);		
		
		//Set additional items
		chart.setTextAntiAlias(true);
		chart.setAntiAlias(true);	
		
		chart.getTitle().setFont(new Font("Arial", Font.PLAIN, 14));
		//chart.setBackgroundPaint(Color.WHITE); //already set by theme
		
		// legend to the right of the chart
		LegendTitle legend = (LegendTitle) chart.getSubtitle(0);
		legend.setPosition(RectangleEdge.RIGHT);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.getRangeAxis().setLabelFont(new Font("Arial", Font.PLAIN, 12)); //Range == x-axis
		plot.getDomainAxis().setLabelFont(new Font("Arial", Font.PLAIN, 12)); //Domain == y-axis
		//plot.setBackgroundPaint(null); //already set by theme
		//plot.setDomainGridlinePaint(Color.black); //Domain == y-axis //already set by theme
		//plot.setRangeGridlinePaint(Color.black); //already set by theme
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true); //Domain == y-axis
		plot.setRangeCrosshairVisible(true); //Range == x-axis
		
		//2017-7 Adam Dolgos ->>>>   JFreeCharts scrollable !!!!
		//JFreeCharts scrollable with ctrl + click!!!!
		plot.setDomainPannable(true); //Domain == y-axis
		plot.setRangePannable(true); //Range == x-axis
		
		// plot.setPadding(0.0, 0.0, 0.0, 15.0);
	
		XYItemRenderer r = plot.getRenderer(0);
		if (r instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			// Shape[] shapes = DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE;
			// //0 square, 1 circle, 2 triangle; 3 diamond; .......9
			// renderer.setSeriesShape(0, shapes[2]);
			// Shape shape = new Rectangle2D.Double(-1, -1, 2, 2); //small
			// rectangle
			// renderer.setSeriesShape(0, shape);
			renderer.setDefaultLinesVisible(isLineVisible);
			renderer.setDefaultShapesVisible(true); 
			renderer.setDefaultShapesFilled(false);
			renderer.setDefaultLinesVisible(isLineVisible);
			for (int c = 0; c < colArray.length; c++) renderer.setSeriesPaint(c, colArray[c]); 

			// renderer.setSeriesOutlinePaint(0, Color.black);
			// renderer.setUseOutlinePaint(true);
			
		}

		// NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		// domainAxis.setAutoRangeIncludesZero(false);
		// domainAxis.setTickMarkInsideLength(2.0f);
		// domainAxis.setTickMarkOutsideLength(0.0f);
		//
		// NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		// rangeAxis.setTickMarkInsideLength(2.0f);
		// rangeAxis.setTickMarkOutsideLength(0.0f);

		// ValueAxis axis = plot.getDomainAxis();
		// @SuppressWarnings("unused")
		// NumberAxis axis = (NumberAxis) plot.getDomainAxis();
		// axis.setTickUnit(new NumberTickUnit(1,new DecimalFormat("0"))); //
		// show every bin
		// axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); //
		// show integer ticker units

		// marker setzen
		// final Marker vmLow = new ValueMarker(10);
		// vmLow.setPaint(Color.BLUE);
		// vmLow.setLabel("Low");
		// vmLow.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
		// vmLow.setLabelTextAnchor(TextAnchor.TOP_LEFT);
		// plot.addDomainMarker(vmLow);
		//
		// final Marker vmHigh = new ValueMarker(200);
		// vmHigh.setPaint(Color.RED);
		// vmHigh.setLabel("High");
		// vmHigh.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
		// vmHigh.setLabelTextAnchor(TextAnchor.TOP_LEFT);
		// plot.addDomainMarker(vmHigh);
				
		return chart;
	}

	/**
	 * Creates a xySeriesColl, consisting of one double[] data.
	 * 
	 * @return the xySeriesColl
	 */
	@SuppressWarnings("rawtypes")
	private XYDataset createXYDataset(double[] dataX, double[] dataY, String seriesLabel) {

		XYSeries s = null;
		XYSeriesCollection xySeriesColl = null;

		s = new XYSeries(seriesLabel);
		for (int i = 0; i < dataX.length; i++) s.add(dataX[i], dataY[i]);
		xySeriesColl = new XYSeriesCollection();
		xySeriesColl.addSeries(s);
		//xySeriesColl.addSeries(s2);
		return xySeriesColl;

	}

	/**
	 * Creates a xySeriesColl, consisting of multiple series.
	 * 
	 * @return the xySeriesColl
	 */
	@SuppressWarnings({ "rawtypes" })
	private XYDataset createXYDataset(double[] dataX, double[][] dataY, String[] legendLabels) {

		XYSeries[] s = new XYSeries[dataY.length];
		XYSeriesCollection xySeriesColl = null;

		for (int v = 0; v < dataY.length; v++)
			//s[v] = new XYSeries("Series " + (v + 1)); // several data series
			s[v] = new XYSeries(legendLabels[v]); // several data series
			//s = new XYSeries("");
		xySeriesColl = new XYSeriesCollection();
		int countIdenticalKeys = 0; //identical keys are not allowed
		for (int v = 0; v < dataY.length; v++) {
			for (int i = 0; i < dataX.length; i++) s[v].add(dataX[i], dataY[v][i]);
			if (xySeriesColl.indexOf(s[v].getKey()) >= 0) { //found identical key
				countIdenticalKeys += 1;
				s[v].setKey(s[v].getKey().toString() + "("+countIdenticalKeys+")"); //Rename key by addin a number 
			}
			xySeriesColl.addSeries(s[v]);		
		}
		// xyDataset.addSeries(s2);
		return xySeriesColl;
	}
	
	/**
	 * Creates a xySeriesColl, consisting of multiple series.
	 * 
	 * @return the xySeriesColl
	 */
	@SuppressWarnings({ "rawtypes" })
	private XYDataset createXYDataset(double[][] dataX, double[][] dataY, String[] legendLabels) {

		XYSeries[] s = new XYSeries[dataY.length];
		XYSeriesCollection xySeriesColl = null;

		for (int v = 0; v < dataY.length; v++)
			//s[v] = new XYSeries("Series " + (v + 1)); // several data series
			s[v] = new XYSeries(legendLabels[v]); // several data series
			//s = new XYSeries("");
		xySeriesColl = new XYSeriesCollection();
		int countIdenticalKeys = 0; //identical keys are not allowed
		for (int v = 0; v < dataY.length; v++) {
			for (int i = 0; i < dataX[v].length; i++) s[v].add(dataX[v][i], dataY[v][i]);
			if (xySeriesColl.indexOf(s[v].getKey()) >= 0) { //found identical key
				countIdenticalKeys += 1;
				s[v].setKey(s[v].getKey().toString() + "("+countIdenticalKeys+")"); //Rename key by addin a number 
			}
			xySeriesColl.addSeries(s[v]);		
		}
		// xyDataset.addSeries(s2);
		return xySeriesColl;
	}

	/**
	 * Creates the chart panel (container).
	 * 
	 * @return A panel.
	 */
	@SuppressWarnings("rawtypes")
	public JPanel createPanel(double[] dataX, double[] dataY) {
		JFreeChart chart = createChart(createXYDataset(dataX, dataY, "Series 1"));
		ChartPanel chartPanel = new ChartPanel(chart);
		// chartPanel.setVerticalAxisTrace(true);
		// chartPanel.setHorizontalAxisTrace(true);
		// popup menu conflicts with axis trace
		chartPanel.setPopupMenu(null);
		chartPanel.setDomainZoomable(true);
		chartPanel.setRangeZoomable(true);

		return chartPanel;
	}

	public ChartPanel getChartPanel() {
		return chartPanel;
	}

	public void setChartPanel(ChartPanel chartPanel) {
		this.chartPanel = chartPanel;
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		int value = this.slider.getValue();
		long minimum = domainAxis.getMinimumDate().getTime();
		long maximum = domainAxis.getMaximumDate().getTime();
		if (value < lastValue) { // left
			minimum = minimum - delta;
			maximum = maximum - delta;
		} else { // right
			minimum = minimum + delta;
			maximum = maximum + delta;
		}
		DateRange range = new DateRange(minimum, maximum);
		domainAxis.setRange(range);
		lastValue = value;
	}

}
