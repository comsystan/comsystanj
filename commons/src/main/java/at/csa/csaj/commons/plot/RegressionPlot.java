/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: RegressionPlot.java
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.InternationalFormatter;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import at.csa.csaj.commons.regression.LinearRegression;


/**
 * This class represents a regression plot with additional control elements.
 * <p>
 * <b>Changes</b>
 * <ul>
 * 	<li>2010 06 display of r2
 * 	<li>2012 11 PK: added string s to "LinearFit" because of JFreechart
 *         update from 13 to 14 (each series must have a separate string)
 * </ul>
 * 
 * @author Helmut Ahammer
 * @since 2021 01 12
 * 
 */
public class RegressionPlot extends DefaultXYLineChart implements
		ChangeListener {

	/**
	 * The UID for serialization.
	 */
	private static final long serialVersionUID = 983749030307565008L;

	private JPanel jPanelSouth = null; // a panel for all components in the
										// south

	private JPanel jPanelRegression = null;
	private JLabel jLabelRegression = null;;
	private JPanel jPanelRegStart = null;
	private JLabel jLabelRegStart = null;
	private JSpinner jSpinnerRegStart = null;
	private JPanel jPanelRegEnd = null;
	private JLabel jLabelRegEnd = null;
	private JSpinner jSpinnerRegEnd = null;

	private int numPoints = 0;
	private int regStart = 0;
	private int regEnd = 0;

	private JPanel jPanelRegResult = null;
	private JLabel jLabelRegResult = null;
	private JLabel jLabelRegResultP0 = null; // y=
	private JLabel jLabelRegResultP1 = null; // x
	private JLabel jLabelRegResultP2 = null; // r2=

	private XYSeriesCollection regressionSeries = null;

	/**
	 * This constructor creates a single regression plot
	 * 
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param frameTitle
	 * @param imageTitle
	 * @param xTitle
	 * @param yTitle
	 * @param regStart
	 * @param regEnd
	 */
	@SuppressWarnings("rawtypes")
	public RegressionPlot(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String imageTitle, String xTitle, String yTitle,
			int regStart, int regEnd) {
		super(dataX, dataY, isLineVisible, imageTitle, xTitle, yTitle);
		numPoints = dataX.length;
		this.regStart = regStart;
		this.regEnd = regEnd;
		//System.out.println("RegressionPlot:  regStart:"+regStart + "   regEnd:"+regEnd);
		this.add(getJPanelSouth(), BorderLayout.SOUTH);
		this.plotRegressions();

	}

	/**
	 * This class displays various regression plots.
	 * 
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param frameTitle
	 * @param imageTitle
	 * @param xTitle
	 * @param yTitle
	 * @param seriesNames
	 * @param regStart
	 * @param regEnd
	 */
	@SuppressWarnings("rawtypes")
	public RegressionPlot(double[] dataX, double[][] dataY, boolean isLineVisible,
			String frameTitle, String imageTitle, String xTitle, String yTitle, String[] seriesNames,
			int regStart, int regEnd) {
		super(dataX, dataY, isLineVisible, imageTitle, xTitle, yTitle, seriesNames);
		numPoints = dataX.length;
		this.regStart = regStart;
		this.regEnd = regEnd;
		this.add(getJPanelSouth(), BorderLayout.SOUTH);
		this.plotRegressions();

	}

	// -----------------------------------------------------------------------------------------
	/**
	 * This method initializes jPanelSouth a panel for all components in the
	 * south of the main panel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelSouth() {
		if (jPanelSouth == null) {
			jPanelSouth = new JPanel();
			jPanelSouth.setLayout(new BoxLayout(jPanelSouth,
					BoxLayout.PAGE_AXIS));
			jPanelSouth.add(getJPanelRegression());
			jPanelSouth.add(getJPanelRegResult());
		}
		return jPanelSouth;
	}

	// -----------------------------------------------------------------------------------------
	/**
	 * This method initializes jJPanelRegression
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelRegression() {
		if (jPanelRegression == null) {
			jPanelRegression = new JPanel();
			jPanelRegression.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			jPanelRegression.add(getJLabelRegression());
			jPanelRegression.add(getJPanelRegStart());
			jPanelRegression.add(getJPanelRegEnd());
		}
		return jPanelRegression;
	}

	// ------------------------------------------------------------------------------------------
	/**
	 * This method initializes jLabelRegression
	 * 
	 * @return javax.swing.JLabel
	 */
	private JLabel getJLabelRegression() {
		if (jLabelRegression == null) {
			jLabelRegression = new JLabel("Regression: ");
		}
		return jLabelRegression;
	}

	// ----------------------------------------------------------------------------------------------------------
	/**
	 * This method initializes jJPanelRegStart
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelRegStart() {
		if (jPanelRegStart == null) {
			jPanelRegStart = new JPanel();
			jPanelRegStart.setLayout(new FlowLayout());
			jLabelRegStart = new JLabel("Start: ");
			jLabelRegStart.setPreferredSize(new Dimension(70, 22));
			jLabelRegStart.setHorizontalAlignment(SwingConstants.RIGHT);
			SpinnerModel sModel = new SpinnerNumberModel(this.regStart, 1,
					numPoints, 1); // init, min, max, step
			jSpinnerRegStart = new JSpinner(sModel);
			// jSpinnerRegStart = new JSpinner();
			jSpinnerRegStart.setPreferredSize(new Dimension(60, 22));
			JSpinner.DefaultEditor defEditor = (JSpinner.DefaultEditor) jSpinnerRegStart
					.getEditor();
			JFormattedTextField ftf = defEditor.getTextField();
			ftf.setEditable(true);
			InternationalFormatter intFormatter = (InternationalFormatter) ftf
					.getFormatter();
			DecimalFormat decimalFormat = (DecimalFormat) intFormatter
					.getFormat();
			decimalFormat.applyPattern("#"); // decimalFormat.applyPattern("#,##0.0")
												// ;
			jSpinnerRegStart.setValue(1); // only in order to set format pattern
			jSpinnerRegStart.setValue(this.regStart); // only in order to set
														// format pattern
			jSpinnerRegStart.addChangeListener(this);

			jPanelRegStart.add(jLabelRegStart);
			jPanelRegStart.add(jSpinnerRegStart);
		}
		return jPanelRegStart;
	}

	/**
	 * This method initializes jJPanelRegEnd
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelRegEnd() {
		if (jPanelRegEnd == null) {
			jPanelRegEnd = new JPanel();
			jPanelRegEnd.setLayout(new FlowLayout());
			jLabelRegEnd = new JLabel("End: ");
			jLabelRegEnd.setPreferredSize(new Dimension(70, 22));
			jLabelRegEnd.setHorizontalAlignment(SwingConstants.RIGHT);
			// System.out.println("IqmRegressionPlot this.regEnd  numPoints  " +
			// this.regEnd +"  " +numPoints);
			SpinnerModel sModel = new SpinnerNumberModel(this.regEnd, 1,
					numPoints, 1); // init, min, max, step
			jSpinnerRegEnd = new JSpinner(sModel);
			// jSpinnerRegEnd = new JSpinner();
			jSpinnerRegEnd.setPreferredSize(new Dimension(60, 22));
			JSpinner.DefaultEditor defEditor = (JSpinner.DefaultEditor) jSpinnerRegEnd
					.getEditor();
			JFormattedTextField ftf = defEditor.getTextField();
			ftf.setEditable(true);
			InternationalFormatter intFormatter = (InternationalFormatter) ftf
					.getFormatter();
			DecimalFormat decimalFormat = (DecimalFormat) intFormatter
					.getFormat();
			decimalFormat.applyPattern("#"); // decimalFormat.applyPattern("#,##0.0")
												// ;
			jSpinnerRegEnd.setValue(1); // only in order to set format pattern
			jSpinnerRegEnd.setValue(this.regEnd); // only in order to set format
													// pattern
			jSpinnerRegEnd.addChangeListener(this);

			jPanelRegEnd.add(jLabelRegEnd);
			jPanelRegEnd.add(jSpinnerRegEnd);
		}
		return jPanelRegEnd;
	}

	// --------------------------------------------------------------------------------------------
	/**
	 * This method initializes jPanelRegResult
	 * 
	 * * @return javax.swing.JPanel
	 */
	private JPanel getJPanelRegResult() {
		if (jPanelRegResult == null) {
			jPanelRegResult = new JPanel();
			jPanelRegResult.setLayout(new FlowLayout());
			jLabelRegResult = new JLabel("Result: ");
			jLabelRegResultP0 = new JLabel();
			jLabelRegResultP1 = new JLabel();
			jLabelRegResultP2 = new JLabel();
			jPanelRegResult.add(jLabelRegResult);
			jPanelRegResult.add(jLabelRegResultP0);
			jPanelRegResult.add(jLabelRegResultP1);
			jPanelRegResult.add(jLabelRegResultP2);
		}
		return jPanelRegResult;
	}

	// --------------------------------------------------------------------------------------------
	/**
	 * This method displays a regression line for each data series
	 */
	private void plotRegressions() {
		JFreeChart chart = this.getChartPanel().getChart();
		XYSeriesCollection sc = (XYSeriesCollection) chart.getXYPlot().getDataset();
		int numSeries = sc.getSeriesCount();
		// System.out.println("IqmRegressionPlot: numSeries" + numSeries);
		for (int s = 0; s < numSeries; s++) {
			// System.out.println("IqmRegressionPlot: s" + s);
			this.plotRegression(s);
		}
	}

	// --------------------------------------------------------------------------------------------
	/**
	 * This method displays a regression line
	 * 
	 * @param s
	 *            number of data Series
	 */
	@SuppressWarnings("unused")
	private void plotRegression(int s) {
		JFreeChart chart = this.getChartPanel().getChart();
		XYSeriesCollection dataPointSeriesCollection = (XYSeriesCollection) chart
				.getXYPlot().getDataset(0); // 0 Data Points, 1 Regression lines
		XYSeries series = dataPointSeriesCollection.getSeries(s); // get the data point series at index s
		int numPoints = dataPointSeriesCollection.getItemCount(s); // get the number of data points at index s
		int regStart = ((Number) jSpinnerRegStart.getValue()).intValue();
		int regEnd = ((Number) jSpinnerRegEnd.getValue()).intValue();
		int numRegPoints = regEnd - regStart + 1;
		// old method of regression using jFreeChart; gives back only a and b
		// for y = a+bx
		// double[][] regData = new double[numRegPoints][2];
		// for (int j = 0; j < numRegPoints; j++){
		// regData[j][0] = dataXY.getXValue(0, j + regStart); //series item
		// regData[j][1] = dataXY.getYValue(0, j + regStart);
		// }
		// double[] p = Regression.getOLSRegression(regData); //gives back only
		// a and b for y = a+bx

		Vector<Double> regDataX = new Vector<Double>();
		Vector<Double> regDataY = new Vector<Double>();
		for (int j = 0; j < numPoints; j++) {
			regDataX.addElement((Double) series.getX(j)); // complete series
			regDataY.addElement((Double) series.getY(j));
		}
		//double[] regressionParams = PlotTools.getLinearRegression(regDataX, regDataY, regStart, regEnd);//IQM
		
		LinearRegression lr = new LinearRegression();

		double[] dataXArray = new double[regDataX.size()];
		double[] dataYArray = new double[regDataY.size()];
		for (int i = 0; i < regDataX.size(); i++) {
			dataXArray[i] = regDataX.get(i).doubleValue();
		}
		for (int i = 0; i < regDataY.size(); i++) {
			dataYArray[i] = regDataY.get(i).doubleValue();
		}

		double regressionParams[] = lr.calculateParameters(dataXArray, dataYArray, regStart, regEnd);
		
		this.displayRegressionParameters(regressionParams);
	
		// Calculate Regression
		// //y = a +bx
		// double[] regLineX = new double[numRegPoints];
		// double[] regLineY = new double[numRegPoints];
		// for (int i = 0; i < numRegPoints; i++){
		// //regLineX[i] = dataXY.getXValue(0, i + regStart); //series item
		// regLineX[i] = (Double) series.getX(i + regStart-1); //series item
		// regLineY[i] = p[0] + p[1]*regLineX[i];
		// }
		// XYSeries regSeries = new XYSeries("Regression "+(s+1));
		// for (int i = 0; i < numRegPoints; i++) regSeries.add(regLineX[i],
		// regLineY[i]);

		// oder einfacher
		// create a line using a and b (coefficients) of the regression
		LineFunction2D linefunction2d = new LineFunction2D(regressionParams[0],
				regressionParams[1]);
		// //XYDataset dataset =
		// DatasetUtilities.sampleFunction2D(linefunction2d, dataXY.getXValue(0,
		// regStart-1), dataXY.getXValue(0, regEnd-1), 100, "Linear Fit");
		// //XYDataset dataset =
		// DatasetUtilities.sampleFunction2D(linefunction2d, (Double)
		// series.getX(regStart-1), (Double) series.getX(regEnd-1), 100,
		// "Linear Fit");
//		System.out.println("RegressionPlot:  regStart:"+regStart + "   regEnd:"+regEnd);
//		for (int i= regStart; i <= regEnd; i++){
//			System.out.println("RegressionPlot:  i:"+ i+"  (Double)series.getX(i - 1):" + (Double) series.getX(i - 1) + "       (Double)series.getY(i - 1):" + (Double) series.getY(i - 1));
//		}
		XYSeries regSeries = DatasetUtils.sampleFunction2DToSeries(
				linefunction2d, (Double) series.getX(regStart - 1),
				(Double) series.getX(regEnd - 1), 100, "Linear Fit " + s);
		// ###################################################
		// UNTIL HERE, THE REGRESSION LINE HAS BEEN CALCULATED

		// set renderer
		XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(true,
				false);
		renderer1.setSeriesPaint(1, Color.GREEN);
		renderer1.clearSeriesPaints(true);
		renderer1.setDefaultShapesVisible(false);
		renderer1.setDefaultSeriesVisibleInLegend(false);
		// Shape[] shapes = DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE; //0
		// square, 1 circle, 2 triangle; 3 diamond; .......9
		// renderer1.setSeriesShape(0, shapes[2]);
		// Shape shape = new Rectangle2D.Double(-1, -1, 2, 2); //small rectangle
		// renderer1.setSeriesShape(series, shape);
		chart.getXYPlot().setRenderer(1, renderer1);

		// chart.getXYPlot().setDataset(1, dataset);
		// chart.getXYPlot().setDataset(1, dataset);

		// draw regression lines
		regressionSeries = null;
		// get the plot
		XYPlot plot = chart.getXYPlot();
		// get the data set at index 1 (regressions)
		// for more than 1 series, the dataSet will exist
		XYDataset dataSet = plot.getDataset(1);
		// cast the dataSet in a collection
		regressionSeries = (XYSeriesCollection) dataSet;

		//System.out.println("Series " + (s));// +1) + "/" + ((XYSeriesCollection)
											// chart.getXYPlot().getDataset()).getSeriesCount());

		// the first run does not contain any line data
		// for linear regression
		// so we have to create a new regression lines dataset
		if (regressionSeries == null) {
			// System.out.println("Constructing new XYSeriesCollection at series "
			// + s + " containing 1 element.");
			regressionSeries = new XYSeriesCollection();
			regressionSeries.addSeries(regSeries);
			chart.getXYPlot().setDataset(1, regressionSeries);
		}
		// if a regression line dataset already exists (i.e. is not NULL)
		// we have to add further lines to be drawn
		// add new XYSeries for regression
		else {
			// System.out.println("Adding new element to " + s);
			regressionSeries.addSeries(regSeries);
		}
	}

	/**
	 * This method displays the regression parameters
	 */
	private void displayRegressionParameters(double[] p) {
		jLabelRegResultP0.setText("y = " + String.valueOf(p[0]));
		if (p[1] >= 0)
			jLabelRegResultP1.setText(" + " + String.valueOf(Math.abs(p[1]))
					+ "  x");
		if (p[1] < 0)
			jLabelRegResultP1.setText(" - " + String.valueOf(Math.abs(p[1]))
					+ "  x");
		jLabelRegResultP2.setText("   r2 = " + String.valueOf(Math.abs(p[4]))); // r2
	}

	// --------------------------------------------------------------------------------------------
	@Override
	public void stateChanged(ChangeEvent e) {
		jSpinnerRegStart.removeChangeListener(this);
		jSpinnerRegEnd.removeChangeListener(this);

		int start = ((Number) jSpinnerRegStart.getValue()).intValue();
		int end = ((Number) jSpinnerRegEnd.getValue()).intValue();
		if (jSpinnerRegStart == e.getSource()) {
			if (start >= (end - 1)) {
				jSpinnerRegStart.setValue(end - 2);
			}
		}
		if (jSpinnerRegEnd == e.getSource()) {
			if (end <= (start + 1)) {
				jSpinnerRegEnd.setValue(start + 2);
			}
		}
		jSpinnerRegStart.addChangeListener(this);
		jSpinnerRegEnd.addChangeListener(this);

		// remove all series from the regression data set collection
		this.regressionSeries.removeAllSeries();

		this.plotRegressions();
	}

}
