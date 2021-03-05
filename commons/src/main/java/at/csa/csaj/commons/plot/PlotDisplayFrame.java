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
public class PlotDisplayFrame extends JFrame {

	/**
	 * The UID for serialization.
	 */
	private static final long serialVersionUID = -2779916167018195984L;

	/**
	 * The default constructor.
	 */
	public PlotDisplayFrame() {
		//this.setIconImage(new ImageIcon(Resources.getImageURL("icon.application.magenta.32x32")).getImage());
		this.setIconImage(new ImageIcon(getClass().getResource("/images/comsystan-logo.png")).getImage());
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.setAlwaysOnTop(false);
		ToolTipManager ttm = ToolTipManager.sharedInstance();
		ttm.setInitialDelay(0); // ms
		ttm.setReshowDelay(10000);
		ttm.setDismissDelay(5000);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeAndDestroyFrame();
			}
		});
	}

	/**
	 * Constructor to be used, if a title should be directly set to the
	 * {@link JFrame}.
	 * 
	 * @param frameTitle
	 */
	public PlotDisplayFrame(String frameTitle) {
		this();
		this.setTitle(frameTitle);
	}

	/**
	 * This constructor creates an instance that displays a plot containing a
	 * single data series.
	 */
	@SuppressWarnings("rawtypes")
	public PlotDisplayFrame(DefaultGenericTable defaultGenericTable, int col, boolean isLineVisible,
			String frameTitle, String imageTitle, String xLabel, String yLabel) {
		this(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(defaultGenericTable, col,
				isLineVisible, imageTitle, xLabel, yLabel);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
	}

	/**
	 * This constructor creates an instance that displays multiple data series
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public PlotDisplayFrame(DefaultGenericTable defaultGenericTable, int[] cols,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String[] seriesLabels) {
		this(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(defaultGenericTable, cols,
				isLineVisible, imageTitle, xLabel, yLabel, seriesLabels);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
	}
	
	
	/**
	 * This constructor creates an instance that displays multiple data series
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public PlotDisplayFrame(double dataX[], double[] dataY,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String legendLabel) {
		this(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(dataX, dataY,
				isLineVisible, imageTitle, xLabel, yLabel, legendLabel);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
	}
	
	/**
	 * This constructor creates an instance that displays multiple data series
	 * of various lengths in a single plot.
	 */
	@SuppressWarnings("rawtypes")
	public PlotDisplayFrame(double dataX[], double[][] dataY,
			boolean isLineVisible, String frameTitle, String imageTitle,
			String xLabel, String yLabel, String[] seriesLabels) {
		this(frameTitle);

		DefaultXYLineChart chartPanel = new DefaultXYLineChart(dataX, dataY,
				isLineVisible, imageTitle, xLabel, yLabel, seriesLabels);

		this.getContentPane().add(chartPanel, BorderLayout.CENTER);
		this.pack();
	}

	/**
	 * This method disposes the frame. It also resets some ToolTip parameters.
	 */
	private void closeAndDestroyFrame() {
		ToolTipManager ttm = ToolTipManager.sharedInstance();
		ttm.setInitialDelay(50); // ms
		ttm.setReshowDelay(50);
		ttm.setDismissDelay(50);
		this.setVisible(false);
		this.dispose();
	}
}
