/*-
 * #%L
 * Project: ImageJ plugin to open single or multiple images.
 * File: SignalOpener.java
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
package at.csa.csaj.sig.open;


import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.ui.UIService;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import at.csa.csaj.commons.plot.PlotDisplayFrame;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
/**
 * This is an ImageJ {@link Command} plugin to open single or multiple images.
 * <p>
 * 
 * </p>
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>ComsystanJ>Signal>Signal Opener")
public class SignalOpener<T extends RealType<T>> implements Command {
	
	private static final String PLUGIN_LABEL = "Opens single or multiple images";
	private static final String SPACE_LABEL = "";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	@Parameter (label = "Signal(s)", type = ItemIO.OUTPUT) //so that it can be displayed
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
	 * This runs a signal(s) open routine
	 * __________________________________________________________________________________
	 * WARNING: Each column of the file must have the same number of rows.
	 * If actual column's row number is shorter, it must be filled up with "999.999"
	 * These numbers will be ignored by Chart displays
	 * These numbers will be ignored by Signal Processing Plugins.
	 * __________________________________________________________________________________
	 *  
     * @param args whatever, it's ignored
     * @throws Exception
     */
    @Override
    public void run() {
    	//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Opening signals, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Opening signals, please wait... Open console window for further info.",
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
				defaultGenericTable = (DefaultGenericTable) ioService.open(files[0].getPath());	
			} catch (IOException e) {
				logService.info(this.getClass().getName() + " WARNING #SO31: It was not possible to load the signal");
				e.printStackTrace();
				dlgProgress.setVisible(false);
				dlgProgress.dispose();
				return;
			} catch (NumberFormatException nfe) {
				logService.info(this.getClass().getName() + " WARNING #SO32: It was not possible to load the signal");
				nfe.printStackTrace();
				dlgProgress.setVisible(false);
				dlgProgress.dispose();
				return;
			}
			
			int numColumns = defaultGenericTable.getColumnCount();
			PlotDisplayFrame pdf = null;
			if (numColumns == 1) {
				boolean isLineVisible = true;
				String imageTitle = files[0].getName();
				String xLabel = "#";
				String yLabel = defaultGenericTable.getColumnHeader(0);
				String seriesLabel = null;
				
				pdf = new PlotDisplayFrame(defaultGenericTable, 0, isLineVisible, "Plot(s)", imageTitle, xLabel, yLabel);
			}
			
			if (numColumns > 1) {
				int[] cols = new int[numColumns];
				boolean isLineVisible = true;
				String imageTitle = files[0].getName();
				String xLabel = "#";
				String yLabel = "Value";
				String[] seriesLabels = new String[numColumns];		
				for (int c = 0; c < numColumns; c++) {
					cols[c] = c;
					seriesLabels[c] = defaultGenericTable.getColumnHeader(c);				
				}
				pdf = new PlotDisplayFrame(defaultGenericTable, cols, isLineVisible, "Plot(s)", imageTitle, xLabel, yLabel, seriesLabels);
			}
			
			pdf.setVisible(true);
				
			//uiService.show(files[0].getName(), image);
		    //datasetOut = datasetService.create(image);
		    uiService.show(files[0].getName(), defaultGenericTable);
		}
		
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
         ij.command().run(SignalOpener.class, true);
    	
    }

}
