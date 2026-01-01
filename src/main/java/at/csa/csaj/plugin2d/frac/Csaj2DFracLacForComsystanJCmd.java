/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracLacForComsystanJCmd.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2026 Comsystan Software
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
package at.csa.csaj.plugin2d.frac;

import java.io.File;
import java.lang.invoke.MethodHandles;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.widget.FileWidget;

import fraclac.gui.GUI;
import ij.util.Java2;

/**
 * A {@link ContextCommand} plugin computing
 * <the fractal dimension and lacunarity using FracLac</a>
 * of an image.
 */
@Plugin(type = ContextCommand.class, 
		headless = true,
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "ComsystanJ"),
		@Menu(label = "2D Image(s)"),
		@Menu(label = "Fractal analyses", weight = 6),
		@Menu(label = "FracLac")})

public class Csaj2DFracLacForComsystanJCmd<T extends RealType<T>> extends ContextCommand {

	
	private static final String PLUGIN_LABEL = "<html><b>Open FracLac</b></html>";

	@Parameter
	private ImageJ ij;
	
	@Parameter
	private PrefService prefService;

	@Parameter
	private LogService logService;
	
	@Parameter
	private StatusService statusService;
	
   //Widget elements------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
	private final String labelPlugin = PLUGIN_LABEL;
 
	protected void initialPluginLaunch() {
		//checkItemIOIn();
		boolean thrown = false;
		try {
			TryToStartFracLac(); //This calls the FracLac package if available
			
		} catch (ClassNotFoundException e) {
			//FracLac cannot be found or started
			logService.info(this.getClass().getName() + " FracLac cannot be started, maybe the Frac_Lac.jar plugin is missing");
			cancel("FracLac cannot be started\nThe Frac_Lac.jar plugin should be in the plugins folder");
			thrown = true;
		} catch (NoClassDefFoundError e) {
			//FracLac cannot be found or started
			logService.info(this.getClass().getName() + " FracLac cannot be started, maybe the Frac_Lac.jar plugin is missing");
			cancel("FracLac cannot be started\nThe Frac_Lac.jar plugin should be in the plugins folder");
			thrown = true;
		} finally {
			logService.info(this.getClass().getName() + " FracLac ImageJ2 GUI canceled");
			if (!thrown) {
				cancel("");	//Without showing the cancel window with an OK button
			}
		}
	}
	
	//During compile time a local dummy GUI without any function is loaded to suppress compile errors
	//Alternatively, the Frac_Lac.jar library could be manually added as dependency to the project
	//The Maven build eliminates this dummy GUI for runtime
	private void TryToStartFracLac() throws ClassNotFoundException, NoClassDefFoundError {	
		
		new GUI(); //This calls the FracLac package if available 

	}
	
 	
 	/** The run method executes the command. */
	@Override
	public void run() {
		//Nothing, because non blocking dialog has no automatic OK button and would call this method twice during start up
	
		//ij.log().info( "Run" );
		logService.info(this.getClass().getName() + " Run");

		//Do nothing as run() can never be reached
		//The command window is closed in the intialPluginLaunch() method
//		if(ij.ui().isHeadless()){
//			//execute();
//		new GUI();
//		
//		} else {
//			new GUI();	
//		}
		
	}
	
	/** The main method enables standalone testing of the command. */
	public static void main(final String... args) throws Exception {
		Java2.setSystemLookAndFeel();
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		// display the user interface
		ij.ui().showUI();

		// open and display an image
		final File imageFile = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE);
		final Dataset image = ij.scifio().datasetIO().open(imageFile.getAbsolutePath());
		ij.ui().show(image);
	
		// execute the filter, waiting for the operation to finish.
		//ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}

