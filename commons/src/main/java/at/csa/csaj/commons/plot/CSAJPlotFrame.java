/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: CSAJPlotFrame.java
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
public class CSAJPlotFrame extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1119864175121521755L;


	/**
	 * The default constructor.
	 */
	public CSAJPlotFrame() {
		//this.setIconImage(new ImageIcon(Resources.getImageURL("icon.application.magenta.32x32")).getImage());
		this.setIconImage(new ImageIcon(getClass().getResource("/images/comsystan-logo-grey46-64x64.png")).getImage());
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
	public CSAJPlotFrame(String frameTitle) {
		this();
		this.setTitle(frameTitle);
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
