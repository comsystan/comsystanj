/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DOpener.java
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
package at.csa.csaj.plugin1d.misc;


import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.ui.UIService;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;

import at.csa.csaj.commons.Dialog_WaitingWithProgressBar;
import at.csa.csaj.commons.Plot_SequenceFrame;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
/**
 * This is an ImageJ {@link Command} plugin to open single or multiple sequences.
 * <p>
 * 
 * </p>
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "Sequence opener",
	iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "1D Sequence(s)"),
	@Menu(label = "Sequence opener ", weight = 10)}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 
public class Csaj1DOpener<T extends RealType<T>> extends ContextCommand { //modal GUI with cancel
	
	private static final String PLUGIN_LABEL = "Opens single or multiple sequences";
	private static final String SPACE_LABEL = "";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	@Parameter (label = "Sequence(s)", type = ItemIO.OUTPUT) //so that it can be displayed
	private DefaultGenericTable defaultGenericTable;
	
	@Parameter
	private DatasetService datasetService;
	
	@Parameter
    private UIService uiService;
    
    @Parameter
    private PrefService prefService;
    
    @Parameter
    private IOService ioService;
    
    //Widget elements------------------------------------------------------
	//No widget
	
	/**
	 * This runs a sequence(s) opening routine
	 * __________________________________________________________________________________
	 * WARNING:
	 * Data values must be floating numbers
	 * Each column of the file must have the same number of rows.
	 * If actual column's row number is shorter, it must be filled up with NaN
	 * NaNs will be ignored by Chart displays
	 * NaNs will be ignored by Sequence Processing Plugins.
	 * __________________________________________________________________________________
	 *  
     */
    @Override
    public void run() {
     	
    	//Dialog_WaitingWithProgressBar dlgProgress = new Dialog_WaitingWithProgressBar("<html>Opening sequences, please wait...<br>Open console window for further info.</html>");
		Dialog_WaitingWithProgressBar dlgProgress = new Dialog_WaitingWithProgressBar("Opening sequences, please wait... Open console window for further info.",
		                                                                             logService, false, null); //isCanceable = false, because no following method listens to exec.shutdown 

		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	long startTime = System.currentTimeMillis();
      	
		JFileChooser fc = null;
		try {fc = new JFileChooser();}
		catch (Throwable e) {
			Result result = uiService.showDialog("Java 2 or Swing is needed.", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			dlgProgress.setVisible(false);
			dlgProgress.dispose();
			return;
		}
		
		fc.setMultiSelectionEnabled(false);
		fc.setDialogTitle("Open a plot file");
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		String dir = System.getProperty("user.dir");
		//System.out.println("System.getProperty(user.dir): " + System.getProperty("user.dir"));		
		if (dir.endsWith(File.separator)) dir = dir.substring(0, dir.length()-1);		
		fc.setCurrentDirectory(new File(dir));
		
		int returnVal = fc.showOpenDialog(null);
		if (returnVal!=JFileChooser.APPROVE_OPTION) {
			dlgProgress.setVisible(false);
			dlgProgress.dispose();
			return;
		}
		File[] files = fc.getSelectedFiles();
		if (files.length==0) { // getSelectedFiles may not work on some JVMs
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}
		
		//set default path to new path
		dir = fc.getCurrentDirectory().getPath();
		System.setProperty("user.dir", dir);
		String path = dir + System.getProperty("file.separator");
		//System.out.println("System.getProperty(user.dir): " + System.getProperty("user.dir"));
		
		if ((files.length == 1) && (files[0] != null)){
			 //Img< T > image = ( Img< T > ) IO.open(files[i].getPath()); 
			try {
				defaultGenericTable = (DefaultGenericTable) ioService.open(files[0].getAbsolutePath());	
			} catch (IOException e) {
				logService.error(this.getClass().getName() + " ERROR: IOException, it was not possible to load a sequence");
				//e.printStackTrace();
				dlgProgress.setVisible(false);
				dlgProgress.dispose();
				return;
			} catch (NumberFormatException nfe) {
				logService.error(this.getClass().getName() + " ERROR: NumberFormatException, it was not possible to load a sequence");
				//nfe.printStackTrace();
				dlgProgress.setVisible(false);
				dlgProgress.dispose();
				return;
			} catch (ClassCastException cce) {
				logService.error(this.getClass().getName() + " ERROR: ClassCastException, it was not possible to load a sequence");
				//cce.printStackTrace();
				dlgProgress.setVisible(false);
				dlgProgress.dispose();
				return;
			}
			
			int numColumns = defaultGenericTable.getColumnCount();
			Plot_SequenceFrame pdf = null;
			if (numColumns == 1) {
				boolean isLineVisible = true;
				String sequenceTitle = files[0].getName();
				String xLabel = "#";
				String yLabel = defaultGenericTable.getColumnHeader(0);
				String seriesLabel = null;
					 
				int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the sequences?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
				if (selectedOption == JOptionPane.YES_OPTION) {
					pdf = new Plot_SequenceFrame(defaultGenericTable, 0, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel);
					pdf.setVisible(true);
				}
				
				//Show table after plot to set it as the active display
				//This is mandatory for launching a sequence processing plugin 
				uiService.show(files[0].getName(), defaultGenericTable);	  
			}
			
			if (numColumns > 1) {
				int[] cols = new int[numColumns];
				boolean isLineVisible = true;
				String sequenceTitle = files[0].getName();
				String xLabel = "#";
				String yLabel = "Value";
				String[] seriesLabels = new String[numColumns];		
				for (int c = 0; c < numColumns; c++) {
					cols[c] = c;
					seriesLabels[c] = defaultGenericTable.getColumnHeader(c);				
				}
							 
				int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the sequences?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
				if (selectedOption == JOptionPane.YES_OPTION) {
					pdf = new Plot_SequenceFrame(defaultGenericTable, cols, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel, seriesLabels);
					pdf.setVisible(true);
				}		
				
				//Show table after plot to set it as the active display
				//This is mandatory for launching a sequence processing plugin 
				uiService.show(files[0].getName(), defaultGenericTable);	
			}				
		}
		
		//This might free some memory and would be nice for a large table
		defaultGenericTable = null;
		Runtime.getRuntime().gc();
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
    }

   
	public static void main(final String... args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
//        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
//
//        // ask the user for a file to open
//        final File file = ij.ui().chooseFile(null, "open");
//
//        if (file != null) {
//            // load the dataset
//            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());
//
//            // show the image
//            ij.ui().show(dataset);
//
//            // invoke the plugin
//            ij.command().run(FracCreate3D.class, true);
//        }
//       
         //invoke the plugin
         ij.command().run(Csaj1DOpener.class, true);
    	
    }

}
