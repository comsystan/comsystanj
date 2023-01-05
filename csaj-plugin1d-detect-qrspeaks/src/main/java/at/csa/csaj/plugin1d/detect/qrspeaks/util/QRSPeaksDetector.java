/*
 * #%L
 * Project: ImageJ2 plugin to detect QRS peaks and RR intervals.
 * File: QRSPeaksDetector.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2023 Comsystan Software
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
 
package at.csa.csaj.plugin1d.detect.qrspeaks.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.widget.ChoiceWidget;

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.plugin1d.detect.qrspeaks.osea4java.OSEAFactory;
import at.csa.csaj.plugin1d.detect.qrspeaks.osea4java.QRSDetector;
import at.csa.csaj.plugin1d.detect.qrspeaks.osea4java.QRSDetector2;
import at.csa.csaj.plugin1d.detect.qrspeaks.osea4java.classification.BeatDetectionAndClassification;
import at.csa.csaj.plugin1d.detect.qrspeaks.osea4java.classification.ECGCODES;
import at.csa.csaj.plugin1d.detect.qrspeaks.osea4java.classification.BeatDetectionAndClassification.BeatDetectAndClassifyResult;
import at.csa.csaj.plugin1d.detect.qrspeaks.osea4java.exception.UnreadableECGFileException;
import io.scif.services.DatasetIOService;

/**
 * @author HA QRSPeaks extractor on the fly using osea4java 
 * @since  2020-08
 * @update 
 */
public class QRSPeaksDetector extends SwingWorker<Boolean, Void> {


	/**
	 * 
	 */
	private File[] files = new File[0];

	/**
	 * 
	 */
	private WaitingDialogWithProgressBar dlgProgress;
	private LogService logService;
	private StatusService statusService;
	
	private Vector<File> extractedFiles = new Vector<File>();

	private int offset       = 0;   // starting offset
	private int sampleRate   = 180; // sample rate 180 for Herbert's fiels 125 for Helena's files
	private String oseaMethod   = "QRSDetect2";   //  "QRSDetect1", "QRSDetect2", "BeatDetectAndClassify"
	private String outputOption = "RRIntervals";   // "RRIntervals", "QRSPeaksCoordinates"
	boolean saveFile = false;
	
	
	
	int numbOfReadBytes;
	byte[] bytes;
	int val;
	double timeStamp1 = 0.0;
	double timeStamp2 = 0.0;
	List<Integer> valueBuffer;	
	int bufferLength = 0; // seconds*sampleRate = number of buffered data values
	int delay;
	
	//QRS-Detection
	int indexOfValue;
	int numberOfFoundPoints = 0;
	double meanInterval = 0.0;
	
	
	BeatDetectionAndClassification bdac;		
	BeatDetectAndClassifyResult bdac_result;
	int numberOfNormalPeaks   = 0;
	int numberOfPVCPeaks      = 0;
	int numberOfUnknownPeaks  = 0;
	

	/**
	 * A simple string with tab delimited values for saving as a file
	 */
	String stringTable = null;
	
	
	public QRSPeaksDetector(WaitingDialogWithProgressBar dlgProgress, LogService logService, StatusService statusService, File[] files, int offset, int sampleRate, String oseaMethod, String outputOption, boolean saveFile) {
		this.dlgProgress = dlgProgress;
		this.logService = logService;
		this.statusService = statusService;
		this.files = files;
		this.offset = offset;
		this.sampleRate = sampleRate;
		this.oseaMethod = oseaMethod;
		//this.oseaMethod = "DoNothing";
		this.outputOption = outputOption;
		this.saveFile = saveFile;
	}
		

	/**
	 * This method reads out the plot information from a specified ECG plot
	 * <code>file</code>.
	 * 
	 * @param file - the specified file to be read
	 * @return a 2D Object array
	 */
	public Object[][] readECGMetaData(File file) throws UnreadableECGFileException {
		StringBuffer sb = new StringBuffer();
		Object[][] data = null;
		try {
			logService.info(this.getClass().getName() + ": Reading Meta Data of ECG plot...");

			//fis = FileInputStream(file);	
			//fis.close();
			
		} catch (Exception e) {
			logService.info(this.getClass().getName() + ": An error occurred: ");
			e.printStackTrace();
			throw new UnreadableECGFileException();
		}
		return data;
	}

	/**
	 * This method extracts the files with the given params.
	 * 
	 * @param files
	 *            - the file list
	 * @param params
	 *            - parameters for processing the file list:
	 *            <ul>
	 *            <li>[0]... starting offset</li>
	 *            <li>[1]... osea method</li>
	 *            <li>[2]... sample rate</li>
	 *            <li>[3]... output option</li>
	 *            </ul>
	 * @return <code>true</code> if successful, <code>false</code> otherwise
	 */
	public boolean extract() {
	
		logService.info(this.getClass().getName() + ": Detecting QRS peaks from the specified ECG files.");
		logService.info(this.getClass().getName() + ": Offset: " + offset);
		logService.info(this.getClass().getName() + ": Sample Rate: " + sampleRate + "[Hz]");
		logService.info(this.getClass().getName() + ": osea Method: " + oseaMethod);
		logService.info(this.getClass().getName() + ": Output option: " + outputOption);
		logService.info(this.getClass().getName() + ": Safe output file: " + saveFile);
		
		File currFileDir;	
		int numUnknownBeats = 0;
		int numNormalBeats = 0;
		int numPVCBeats = 0;
		
		bufferLength = 20*sampleRate; // seconds*sampleRate = number of buffered data values
		bytes = new byte[2];
	
		for (int f = 0; f < files.length; f++) {
			if (this.isCancelled()) {
				return false;
			}
			
			logService.info(this.getClass().getName() + " ");
			logService.info(this.getClass().getName() + ": Processing file " + (f + 1) + "/" + files.length);
			statusService.showStatus((f+1), files.length, "Processing " + (f+1) + "/" + files.length);
			
			int percent = (int)Math.round((  ((float)f)/((float)files.length)   *100.f   ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			
			
			long startTime = System.currentTimeMillis();
	
			try { //extract QRS peaks from a file
				logService.info(this.getClass().getName() + ": Extracting QRS peaks from: " + files[f]);
				// Open the 16bit file				
		        FileInputStream fis = new FileInputStream(files[f]);
		        //numBytes = fis.available();
				//bytes = fis.readNBytes(100000);
		        
				//Set/Reset some variables such as Result table
		    	indexOfValue = 0;
				stringTable = "";
				valueBuffer = new ArrayList<Integer>();	
				numberOfFoundPoints = 0;
						
				//define buffer for maximal QRSDetect delay
				for (int i = 1; i <= bufferLength; i++){ //create a shift register (buffer) with a number of values
					valueBuffer.add(0);	
				}
				
				//prepare output string table
				if (outputOption.equals("QRSPeaksCoordinates")) { //coordinates	
					String domainHeader = "QRS event time";
					String domainUnit   = " [s]";	
					String dataHeader   = "QRS event value";
					String dataUnit     = "";
						
					stringTable += domainHeader + domainUnit + "\t" + dataHeader + dataUnit + "\n"; // "\t" Tabulator		
					//stringTable += domainX.get(i).toString() + "\t" + coordinateY.get(i).toString() + "\n";	
				}	
				if (outputOption.equals("RRIntervals")) { //intervals
					String domainHeader = "QRS event time";
					String domainUnit   = " [s]";	
					String dataHeader   = "RR interval";
					String dataUnit     = " [s]";

					stringTable += domainHeader + domainUnit + "\t" + dataHeader + dataUnit + "\n"; // "\t" Tabulator
					//stringTable += domainX.get(i).toString() + "\t" + intervals.get(i).toString() + "\n";
				}	
				
				
		    	if (oseaMethod.contentEquals("QRSDetect1")){//QRSDetec1------------------------------------------------------------
		    		QRSDetector qrsDetector = OSEAFactory.createQRSDetector(sampleRate);	
		     		
		    		//scroll through offset
		    		int s = 1;
		    		while (s <= offset) {
		    			numbOfReadBytes = fis.read(bytes); 
		    			//indexOfValue += 1;
		    			s = s+1;
		    		}
		    		logService.info(this.getClass().getName() + ": Skipped " + offset + " initial data points.");
				  
		    		numbOfReadBytes = fis.read(bytes); 
				    
					while (numbOfReadBytes != -1) {
						
						//System.out.println("Plotparser: 16bit data: byte #: " + i + "    :" + bytes[i] );
						//int val = ((bytes[1] & 0xff) << 8) + (bytes[0] & 0xff);  unsigned short
						val = ((bytes[1] << 8) | (bytes[0] & 0xFF)); //signed short
					
						indexOfValue = indexOfValue + 1;
						//System.out.println("Found value " + val + " at index " + indexOfValue);
						
						valueBuffer.add(0, val);          //insert new value on the left, this increases the size of buffer +1
						valueBuffer.remove(bufferLength); //remove oldest value on the right
						delay = qrsDetector.QRSDet(val); //gives back the delay of preceding QRS peak;
							
						if (delay != 0) {
							numberOfFoundPoints = numberOfFoundPoints + 1;
							timeStamp2 = indexOfValue - delay; //-1?
							//System.out.println("A QRS-Complex was detected at sample: " + (timeStamp2));
							if (outputOption.equals("QRSPeaksCoordinates")) { //XY coordinates
								//domainX.add(timeStamp2); //eventually divide by sample rate to get absolute times in s
								//coordinateY.add((double) valueBuffer.get(delay));
								stringTable += String.valueOf(timeStamp2) + "\t" + valueBuffer.get(delay).toString() + "\n";	
							}
							if ((outputOption.equals("RRIntervals")) && (numberOfFoundPoints > 1)) { //intervals //skipped first value because it is too long 
								////domainX.add((double) numberOfFoundPoints); //times of beats
								//domainX.add(timeStamp2/sampleRate); //eventually divide by sample rate to get absolute times in s
								//intervals.add((timeStamp2 -timeStamp1)/sampleRate); //correction with sample rate to get absolute times in s
								stringTable += String.valueOf(timeStamp2/sampleRate) + "\t" + String.valueOf((timeStamp2 -timeStamp1)/sampleRate) + "\n";	
								meanInterval += (timeStamp2 -timeStamp1)/sampleRate;
							}					
							timeStamp1 = timeStamp2;
							timeStamp2 = 0.0;				
						}
						
						//this.fireProgressChanged((int) (i) * 95 / (sequence.size()));	
						numbOfReadBytes = fis.read(bytes);	
					}	
					logService.info(this.getClass().getName() + ": Number of detected QRS complexes using QRSDetect: "+ numberOfFoundPoints);
					if (outputOption.equals("RRIntervals")) {
						meanInterval = meanInterval/(numberOfFoundPoints - 1);
						logService.info(this.getClass().getName() + ": Mean RR interval: "+ meanInterval*1000 + " [ms]");
						logService.info(this.getClass().getName() + ": Mean HR: "+ 1.0/meanInterval*60 + " [1/min]");
					}
					//7this.setProgress(90);
					fis.close();
		    	}//oseaMethod 1----------------------------------------------------------------------------------
		    	
		    	if (oseaMethod.equals("QRSDetect2")){//QRSDetec2------------------------------------------------------------
		    		QRSDetector2 qrsDetector = OSEAFactory.createQRSDetector2(sampleRate);	
		    		
		    		//scroll through offset
		    		int s = 1;
		    		while (s <= offset) {
		    			numbOfReadBytes = fis.read(bytes); 
		    			//indexOfValue += 1;
		    			s = s+1;
		    		}
		    		logService.info(this.getClass().getName() + ": Skipped " + offset + " initial data points.");
		    		
				    numbOfReadBytes = fis.read(bytes); 
				    
					while (numbOfReadBytes != -1) {
						
						//System.out.println("Plotparser: 16bit data: byte #: " + i + "    :" + bytes[i] );
						//val = ((bytes[1] & 0xff) << 8) + (bytes[0] & 0xff);  unsigned short
						val = ((bytes[1] << 8) | (bytes[0] & 0xFF)); //signed short
									
						indexOfValue = indexOfValue + 1;
										
						//System.out.println("Found value " + val + " at index " + indexOfValue);
						
						valueBuffer.add(0, val);  //insert new value on the left, this increases the size of buffer +1
						valueBuffer.remove(bufferLength); //remove oldest value on the right
				
						delay = qrsDetector.QRSDet(val); //gives back the delay of preceding QRS peak;
							
						if (delay != 0) {
							numberOfFoundPoints = numberOfFoundPoints + 1;
							timeStamp2 = indexOfValue - delay; //-1?
							//System.out.println("A QRS-Complex was detected at sample: " + (timeStamp2));
							if (outputOption.equals("QRSPeaksCoordinates")) { //XY coordinates
								//domainX.add(timeStamp2); //eventually divide by sample rate to get absolute times in s
								//coordinateY.add((double) valueBuffer.get(delay));
								stringTable += String.valueOf(timeStamp2) + "\t" + valueBuffer.get(delay).toString() + "\n";	
							}
							if ((outputOption.equals("RRIntervals")) && (numberOfFoundPoints > 1)) { //intervals //skipped first value because it is too long 
								////domainX.add((double) numberOfFoundPoints); //times of beats
								//domainX.add(timeStamp2/sampleRate); //eventually divide by sample rate to get absolute times in s
								//intervals.add((timeStamp2 -timeStamp1)/sampleRate); //correction with sample rate to get absolute times in s
								stringTable += String.valueOf(timeStamp2/sampleRate) + "\t" + String.valueOf((timeStamp2 -timeStamp1)/sampleRate) + "\n";
								meanInterval += (timeStamp2 -timeStamp1)/sampleRate;
							}					
							timeStamp1 = timeStamp2;
							timeStamp2 = 0.0;				
						}
						
						//this.fireProgressChanged((int) (i) * 95 / (sequence.size()));	
						numbOfReadBytes = fis.read(bytes);	
					}	
					logService.info(this.getClass().getName() + ": Number of detected QRS complexes using QRSDetect2: "+ numberOfFoundPoints);
					if (outputOption.equals("RRIntervals")) {
						meanInterval = meanInterval/(numberOfFoundPoints - 1);
						logService.info(this.getClass().getName() + ": Mean RR interval: "+ meanInterval*1000 + " [ms]");
						logService.info(this.getClass().getName() + ": Mean HR: "+ 1.0/meanInterval*60 + " [1/min]");
					}
					//this.setProgress(90);
					fis.close();
		    	}//oseaMethod 2----------------------------------------------------------------------------------

				//Method3-detection and classification--------------------------------------------------------------------
				//QRSDetector2 for detection
				if (oseaMethod.equals("BeatDetectAndClassify")){
					numberOfNormalPeaks   = 0;
					numberOfPVCPeaks      = 0;
					numberOfUnknownPeaks  = 0;
					meanInterval          = 0;
					
					bdac = OSEAFactory.createBDAC(sampleRate, sampleRate/2);		
		    	
					//scroll through offset
		    		int s = 1;
		    		while (s <= offset) {
		    			numbOfReadBytes = fis.read(bytes); 
		    			//indexOfValue += 1;
		    			s = s+1;
		    		}
		    		logService.info(this.getClass().getName() + ": Skipped " + offset + " initial data points.");
		    		
				    numbOfReadBytes = fis.read(bytes); 
				    
					while (numbOfReadBytes != -1) {	
						//System.out.println("Plotparser: 16bit data: byte #: " + i + "    :" + bytes[i] );
						// val = ((bytes[1] & 0xff) << 8) + (bytes[0] & 0xff);  unsigned short
						val = ((bytes[1] << 8) | (bytes[0] & 0xFF)); //signed short
							
						indexOfValue = indexOfValue + 1;
						//System.out.println("Found value " + val + " at index " + indexOfValue);
						
						valueBuffer.add(0, val);  //insert new value on the left, this increases the size of buffer +1
						valueBuffer.remove(bufferLength-1); //remove oldest value on the right
					
						bdac_result = bdac.BeatDetectAndClassify(val);
						delay = bdac_result.samplesSinceRWaveIfSuccess;	
						
						if (delay != 0) {
							//int qrsPosition =  delay;
		
							if (bdac_result.beatType == ECGCODES.NORMAL) {
								//logService.info(this.getClass().getName() + ": A normal beat type was detected at sample: " + qrsPosition);
								numberOfNormalPeaks += 1; 
							} else if (bdac_result.beatType == ECGCODES.PVC) {
								//logService.info(this.getClass().getName() + ": A premature ventricular contraction was detected at sample: " + qrsPosition);
								numberOfPVCPeaks += 1;
							} else if (bdac_result.beatType == ECGCODES.UNKNOWN) {
								//logService.info(this.getClass().getName() + ": An unknown beat type was detected at sample: " + qrsPosition);
								numberOfUnknownPeaks +=1;
							}
							numberOfFoundPoints = numberOfFoundPoints + 1;
							timeStamp2 = indexOfValue-delay;
//							System.out.println("qrsPosition: " +qrsPosition);
//							System.out.println("timeStamp2: "  +timeStamp2);
//							System.out.println("timeStamp1: "  +timeStamp1);
							if (outputOption.equals("QRSPeaksCoordinates")) { //XY coordinates
								//domainX.add(timeStamp2); //eventually divide by sample rate to get absolute times in s
								//coordinateY.add((double) valueBuffer.get(delay));
								stringTable += String.valueOf(timeStamp2) + "\t" + valueBuffer.get(delay).toString() + "\n";	
							}
							if ((outputOption.equals("RRIntervals")) && (numberOfFoundPoints > 1)) { //intervals //skipped first value because it is too long 
								////domainX.add((double) numberOfFoundPoints); //times of beats
								//domainX.add(timeStamp2/sampleRate); //eventually divide by sample rate to get absolute times in s
								//intervals.add((timeStamp2 -timeStamp1)/sampleRate); //correction with sample rate to get absolute times in s
								stringTable += String.valueOf(timeStamp2/sampleRate) + "\t" + String.valueOf((timeStamp2 -timeStamp1)/sampleRate) + "\n";
								meanInterval += (timeStamp2 -timeStamp1)/sampleRate;
							}								
							timeStamp1 = timeStamp2;
							timeStamp2 = 0.0;
						}			
						//this.fireProgressChanged((int) (i) * 95 / (sequence.size()));	
						numbOfReadBytes = fis.read(bytes);	
					}	
					logService.info(this.getClass().getName() + ": Number of detected QRS complexes using BeatDetectAndClassify: "+ numberOfFoundPoints);
					logService.info(this.getClass().getName() + ": Number of normal QRS peaks: "+ numberOfNormalPeaks);
					logService.info(this.getClass().getName() + ": Number of premature ventricular contractions: "+ numberOfPVCPeaks);
					logService.info(this.getClass().getName() + ": Number of unknown QRS peaks: "+ numberOfUnknownPeaks);
					if (outputOption.equals("RRIntervals")) {
						meanInterval = meanInterval/(numberOfFoundPoints - 1);
						logService.info(this.getClass().getName() + ": Mean RR interval: "+ meanInterval*1000 + " [ms]");
						logService.info(this.getClass().getName() + ": Mean HR: "+ 1.0/meanInterval*60 + " [1/min]");
					}
					
					fis.close();
				}//Method 3-------------------------------------------------------------------------------------
		    	
			} catch (IOException e) { //
				logService.info(this.getClass().getName() + ": An error occurred: ");
				e.printStackTrace();
			}
			
//			if (!coordinateY.isEmpty()) {
//				//domainX.remove(0);
//				//coordinateY.remove(0);
//			}
//			if (!intervals.isEmpty()) {// eliminate first element because it is too long (osea skips first 8 beats)
//				domainX.remove(0);
//				intervals.remove(0);		
//			}
					
			boolean saveToFile = saveFile; 
			if (saveToFile) {
				try {
					//save QRS peaks file 
					logService.info(this.getClass().getName() + ": QRS peaks have been detected, now storing to disk.");
					// construct the filename
					String newFileName = null;
					if (outputOption.equals("QRSPeaksCoordinates")) {
						newFileName = files[f].toString().replace(".raw", "") + "_osea" + String.valueOf(oseaMethod) + "_XYcoordinates" + ".txt";
					}
					if (outputOption.equals("RRIntervals")) {
						newFileName = files[f].toString().replace(".raw", "") + "_osea" + String.valueOf(oseaMethod) + "_intervals" + ".txt";
					}
					
					//This replace command did not work, no idea why, so it is done in the lines above
					//newFileName.replace(".raw", ""); //Eliminate ".raw" in the middle of newFileName
					
					Files.write(Paths.get(newFileName), stringTable.getBytes(StandardCharsets.UTF_8));
					
					logService.info(this.getClass().getName() + ": " + newFileName + " has been stored to disk.");
	
					//This is for displaying files in IQM
					//extractedFiles.add(newFile);
	
					// Print results
					logService.info(this.getClass().getName() + ": Saved result file: " + (f + 1) + "/" + files.length);
					
					
					
				} catch (Exception e) {
					
					try {
						JOptionPane.showMessageDialog(new JFrame(), "Cannot store the extracted QRS peaks", "Error ", JOptionPane.ERROR_MESSAGE);
									
					} catch (NullPointerException npe) {
						logService.info(this.getClass().getName() + ": An error occurred: ");
						npe.printStackTrace();
					}
					
					
					return false;
				}
			} 
			
			long duration = System.currentTimeMillis() - startTime;
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("HHH:mm:ss:SSS");
			logService.info(this.getClass().getName() + ": Elapsed time: "+ sdf.format(duration));

			this.setProgress((int)Math.round(((float)f + 1f)/files.length*100.f)); 
			
		}// f files[] loop
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		return true;
	}

	/**
	 * This method appends String(num) to the image name
	 */
	private File appendTextToFileName(File file, String strAppend) {
		String str = file.toString();
		int dotPos = str.lastIndexOf(".");
		String name = str.substring(0, dotPos);
		String ext = str.substring(dotPos + 1); // "b16"
		
		ext = "txt";
	
		str = name + strAppend + "." + ext;
		return new File(str);
	}

	@Override
	protected Boolean doInBackground() throws Exception {
		this.firePropertyChange("singleTaskRunning", 0, 1);
		boolean success = this.extract();
		this.firePropertyChange("singleTaskRunning", 1, 0);
		logService.info(this.getClass().getName() + ": The extraction was " + (success ? "" : "NOT ")
				+ "successful, returning [" + (success ? "true" : "false")
				+ "].");
		return success;
	}

	@Override
	protected void done() {
		boolean success;
		try {
			if (this.isCancelled()) {
				success = false;
			} else {
				success = this.get();
			}
			
			if (success) {
				String message = "Extraction finsihed";
			
				//TO DO Loading into IQM
//				int selection = DialogUtil
//						.getInstance()
//						.showDefaultQuestionMessage(
//								message
//										+ "\nDo you want to load the extracted files now?");
//				if (selection == IDialogUtil.YES_OPTION) {
//					logService.info(this.getClass().getName() + ":"Loading of files selected");
//					logService.info(this.getClass().getName() + ":"Loading of files not implemented yet");
//					logService.info(this.getClass().getName() + ": QRSPeakExtractor: Loading of files not implemented yet");
////					File[] newFiles = new File[this.extractedFiles.size()];
////					for (int i = 0; i < newFiles.length; i++) {
////						newFiles[i] = this.extractedFiles.get(i);
////					}
////					Application.getTank().loadImagesFromHD(newFiles);
//					
//					//Opening should go over IqmDataBoxes?
////					List<IqmDataBox> itemList =  new ArrayList<IqmDataBox>();
////					Application.getTank().addNewItems(itemList);;
//				}
//				if (selection == IDialogUtil.NO_OPTION) {
//					logService.info(this.getClass().getName() + ":"No loading of files selected");
//				}
//				this.setProgress(0);
				this.setProgress(0);
		}
		} catch (Exception e) {
			logService.info(this.getClass().getName() + ": An error occurred: ");
			e.printStackTrace();
		} finally {
			System.gc();
		}

	}

	/**
	 * @return the files
	 */
	public File[] getFiles() {
		return files;
	}

	/**
	 * @param files
	 *            the files to set
	 */
	public void setFiles(File[] files) {
		this.files = files;
	}

	/**
	 * @return the extractedFiles
	 */
	public Vector<File> getExtractedFiles() {
		return extractedFiles;
	}

	/**
	 * @param extractedFiles
	 *            the extractedFiles to set
	 */
	public void setExtractedFiles(Vector<File> extractedFiles) {
		this.extractedFiles = extractedFiles;
	}
	
	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getOseaMethod() {
		return oseaMethod;
	}

	public void setOseaMethod(String oseaMethod) {
		this.oseaMethod = oseaMethod;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}
	
	public String getOutputOption() {
		return outputOption;
	}

	public void setOutputOption(String outputOption) {
		this.outputOption = outputOption;
	}

}
