/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DGeneratorCmdUI.java
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
package at.csa.csaj.plugin1d.misc;

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

import net.imagej.Dataset;
import net.imagej.ImageJ;

@Plugin(type = ContextCommand.class,
		headless = true,
		label = "1D sequence generator",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "ComsystanJ"),
		@Menu(label = "1D Sequence(s)"),
		@Menu(label = "1D sequence generator ", weight = 20)}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 

public class Csaj1DGeneratorCmdUI extends ContextCommand implements Previewable{
	
	@Parameter
	LogService logService;
	
  	@Parameter(type = ItemIO.OUTPUT)
  	private Dataset datasetOut;

	private Csaj1DGeneratorDialog dialog = null;
	

	@Override //Interface Previewable
	public void preview() { 

	}
	
	@Override //Interface Previewable
	public void cancel() {
		logService.info(this.getClass().getName() + " ComsystanJ plugin canceled");
	}

	/**
	 * Show the GUI dialog
	 */
	@Override
	public void run() {
		
		SwingUtilities.invokeLater(() -> {
			if (dialog == null) {
				dialog = new Csaj1DGeneratorDialog(context());
			}
			dialog.setVisible(true);
			dialog.btnGenerate.requestFocusInWindow();
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
		//
//      // ask the user for a file to open
//      final File file = ij.ui().chooseFile(null, "open");
//
//      if (file != null) {
//          // load the dataset
//          final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());
//
//          // show the image
//          ij.ui().show(dataset);
//
//          // invoke the plugin
//          ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
//      }
//     
       //invoke the plugin
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
