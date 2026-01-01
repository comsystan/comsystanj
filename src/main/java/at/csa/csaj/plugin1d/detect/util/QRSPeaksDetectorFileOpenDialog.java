/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: QRSPeaksDetectorFileOpenDialog.java
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
package at.csa.csaj.plugin1d.detect.util;

import java.awt.Dimension;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.scijava.log.LogService;


/**
 * This class is responsible for opening an ECG file and extracting QRS peak times.
 * Hamilton, Tompkins, W. J., "Quantitative investigation of QRS detection rules using the MIT/BIH arrhythmia database",IEEE Trans. Biomed. Eng., BME-33, pp. 1158-1165, 1987.
 * osea4java	 
 * <p>
 * <b>Changes</b>
 * <ul>
 * 	<li>
 * 	<li>
 * </ul>
 * 
 * @author HA
 * @since  2018-11
 *
 */
public class QRSPeaksDetectorFileOpenDialog extends Thread{

	
	private LogService logService;

	private File currImgDir;
	
	private File[] files;
	

	
	
	public File[] getFiles() {
		return files;
	}

	public void setFiles(File[] files) {
		this.files = files;
	}

	/**
	 * Standard constructor.
	 */
	public QRSPeaksDetectorFileOpenDialog(){};

	/**
	 * Runs the dialog.
	 */
	public void run() {

		JFileChooser fc = new JFileChooser();

		fc.setMultiSelectionEnabled(true);
		fc.setDialogTitle("Choose files for detecting QRS peaks and RR intervals");
		FileNameExtensionFilter filt_b16 = new FileNameExtensionFilter("b16", "b16");
		FileNameExtensionFilter filt_dat = new FileNameExtensionFilter("dat", "dat");
		FileNameExtensionFilter filt_raw = new FileNameExtensionFilter("raw", "raw");
		fc.addChoosableFileFilter(filt_raw);
		fc.addChoosableFileFilter(filt_b16);
		fc.addChoosableFileFilter(filt_dat);
		fc.setFileFilter(filt_raw);        //default setting

		//currImgDir = ConfigManager.getCurrentInstance().getImagePath();
		fc.setCurrentDirectory(currImgDir);
		fc.setPreferredSize(new Dimension(700, 500));

		JFrame frame = new JFrame();
		//frame.setIconImage(new ImageIcon(Resources.getImageURL("icon.menu.file.extractSVS")).getImage());

		int returnVal = fc.showOpenDialog(frame);
		if (returnVal!=JFileChooser.APPROVE_OPTION) {
			logService.info(this.getClass().getName() + ": No ECG files(s) selected for extraction.");
			return;
		} else{
			currImgDir = fc.getCurrentDirectory();
			
			// update configuration
			//ConfigManager.getCurrentInstance().setImagePath(currImgDir);
			
			// get the selected files
			files = fc.getSelectedFiles();	
			if (files.length==0) { // getSelectedFiles does not work on some JVMs
				files = new File[1];
				files[0] = fc.getSelectedFile();
			}

		}
	}

}
