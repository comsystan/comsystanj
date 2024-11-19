/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DFracDimRSECmdUI.java
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
package at.csa.csaj.plugin1d.frac;

import java.lang.invoke.MethodHandles;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.scijava.ItemIO;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultTableDisplay;
import at.csa.csaj.plugin1d.misc.Csaj1DOpenerCmd;
import net.imagej.ImageJ;

@Plugin(type = ContextCommand.class,
		headless = true,
		label = "RSE dimension",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "ComsystanJ"),
		@Menu(label = "1D Sequence(s)"),
		@Menu(label = "Fractal analyses", weight = 6),
		@Menu(label = "RSE dimension ")}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 

public class Csaj1DFracDimRSECmdUI extends ContextCommand implements Previewable{
	
	@Parameter
	LogService logService;
	
	@Parameter(type = ItemIO.INPUT, required = false)
	private DefaultTableDisplay  defaultTableDisplay;

	private Csaj1DFracDimRSEDialog dialog = null;
	

	@Override //Interface Previewable
	public void preview() { 

	}
	
	@Override //Interface Previewable
	public void cancel() {
		logService.info(this.getClass().getName() + " ComsystanJ plugin canceled");
	}
	
	/**
	 * Executed before run() is called 
	 */
	protected void initialPluginLaunch() {
		//Check if input is available
		if (defaultTableDisplay == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Missing input table");
			cancel("ComsystanJ 1D plugin cannot be started - missing input table.");
		}	
		if (context() == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Missing context");
			cancel("ComsystanJ 1D plugin cannot be started - missing context.");
		}	
	}

	/**
	 * Show the GUI dialog
	 */
	@Override
	public void run() {	
		SwingUtilities.invokeLater(() -> {
			if (dialog == null) {
				dialog = new Csaj1DFracDimRSEDialog(context(), defaultTableDisplay);
			}
			dialog.setVisible(true);
			dialog.btnProcessSingleColumn.requestFocusInWindow();
		});	
	}
	
	/** The main method enables standalone testing of the command. */
	public static void main(final String... args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		// display the user interface
		ij.ui().showUI();
		
		// open and display a sequence, waiting for the operation to finish.
		ij.command().run(Csaj1DOpenerCmd.class, true).get();
		//open and run Plugin
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
