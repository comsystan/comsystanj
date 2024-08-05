/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CreateCommandFiles.java
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
package at.csa.csaj.command;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;


/**
 * This class creates ContextCommand files (usable for scripting) from all available Csaj InteractiveCommand plugin files.
 * Note: For JDK>=11 Apache FileUtils are not needed any more 
 */
public class CreateCommandFiles {
	
	public static List<String> getAllFileNames (String rootDirPath) {
	    File rootDir = new File(rootDirPath);
	    List<String> fileNames = new ArrayList<>();

	    File[] filesAndDirs = rootDir.listFiles();
	    for (File file : filesAndDirs) {
	        if (file.isFile()) {
	            fileNames.add(file.getName());
	        } else if (file.isDirectory()) {
	            fileNames.addAll(getAllFileNames(file.getAbsolutePath()));
	        }
	    }
	    return fileNames;
	}
	
	public static List<String> getAllFileAbsoluteNames (String rootDirPath) {
	    File rootDir = new File(rootDirPath);
	    List<String> fileNames = new ArrayList<>();

	    File[] filesAndDirs = rootDir.listFiles();
	    for (File file : filesAndDirs) {
	        if (file.isFile()) {
	            fileNames.add(file.getAbsolutePath());
	        } else if (file.isDirectory()) {
	            fileNames.addAll(getAllFileAbsoluteNames(file.getAbsolutePath()));
	        }
	    }
	    return fileNames;
	}
	
	
	//Note for JDK > 8 
	//JDK11 String content = Files.readString(fileName);!!!!!!!
	//Apache FileUtils
	//No Apache commons-io dependency in main pom needed any more
	public static void main(String[] args) {
			
		List<String> fileNames = CreateCommandFiles.getAllFileAbsoluteNames ("src/main/java");
		
//		System.out.println("CreateCommandFiles List of all files in directory and subdirectories:");
//		for(int i = 0; i < fileNames.size(); i++) {
//			System.out.println("CreateCommandFiles File name: " + fileNames.get(i));
//		}
		
		List<String> filteredFileNames = new ArrayList<>();
		
		//Get filtered file names
		String name;
		for(int i = 0; i < fileNames.size(); i++) {
			name = fileNames.get(i);
			if ( (name.contains("Csaj1D") || name.contains("Csaj2D") ||  name.contains("Csaj3D") ) && name.endsWith(".java") && !name.endsWith("Command.java")) {
				filteredFileNames.add(name);	
			}	
		}
		
		System.out.println("CreateCommandFiles List of Csaj***.java files:");
		for(int i = 0; i < filteredFileNames.size(); i++) {
			System.out.println("CreateCommandFiles File name: " + filteredFileNames.get(i));
		}
		
		//get root path 
		int indexRootEnd = (filteredFileNames.get(0)).indexOf("at\\csa\\csaj") - 1;
		String rootAbsPath = (filteredFileNames.get(0)).substring(0, indexRootEnd);
		
		int numberCreated = 0;
		//Loop over files to create and save
		for(int i = 0; i < filteredFileNames.size(); i++) { //all files
		//for(int i = 0; i < 1; i++) { //only first file
					
			//URL url = MethodHandles.lookup().lookupClass().getClass().getResource(filteredFileNames.get(i));		
			//File fileIn = new File(url.toString());
			
			File fileIn = new File(filteredFileNames.get(i));
			
			String nameIn = fileIn.getName(); //with ending .java
			String nameOut = nameIn;
			nameOut = nameOut.replace(".java", "Command.java");
			
			//Eliminate endings
			nameIn  = nameIn.replace(".java", "");
			nameOut = nameOut.replace(".java", "");

			//Absolute name of the the command folder
			String absNameOut = (rootAbsPath+"/at/csa/csaj/command/" + nameOut +".java");				
			File fileOut = new File(absNameOut);
			
			//Note for JDK > 8 
			//JDK11 String content = Files.readString(fileName);!!!!!!!
			//Apache FileUtils
			//No Apache commons-io dependency in main pom needed any more	
			String fileContext = null;
			try {
				fileContext = FileUtils.readFileToString(fileIn, "UTF-8");
			} catch (IOException e) {
				System.out.println("CreateCommandFiles Error reading a file"); 
				e.printStackTrace();
			}
			
			//Change package
			int indexPackageStart = fileContext.indexOf("package at.csa.csaj");
			int indexPackageEnd   = fileContext.indexOf(";", indexPackageStart);	
			String subStringPackageDetail = fileContext.substring(indexPackageStart, indexPackageEnd);
			fileContext = fileContext.replace(subStringPackageDetail, "package at.csa.csaj.command");
					
			//Eliminate specific Menu entries
			int indexMenuStart = fileContext.indexOf("menu = {");
			int indexMenuEnd   = fileContext.indexOf("})", indexMenuStart);	
			String subStringMenu = fileContext.substring(indexMenuStart, indexMenuEnd);
			String subStringDetail = subStringMenu.replace("menu = {", "");
			subStringDetail = subStringDetail.replace("})", "");
			fileContext = fileContext.replace(subStringDetail, "");
			
			//Change to ContextCommand
			fileContext = fileContext.replace("import org.scijava.command.InteractiveCommand;", "import org.scijava.command.ContextCommand;");
			fileContext = fileContext.replace("link InteractiveCommand", "link ContextCommand");
			fileContext = fileContext.replace("InteractiveCommand.class", "ContextCommand.class");
			fileContext = fileContext.replace("extends InteractiveCommand", "extends ContextCommand");
			//fileContext = fileContext.replaceAll("InteractiveCommand", "ContextCommand"); //Not possible because of the main description of the plugin  
			
			//Change public class name
			fileContext = fileContext.replace("public class " + nameIn, "public class " + nameOut);
			
			//Write Command file
			try {
				FileUtils.writeStringToFile(fileOut, fileContext, "UTF-8");
				System.out.println("CreateCommandFiles Output file: " + fileOut); 
				numberCreated = numberCreated + 1;
			} catch (IOException e) {
				System.out.println("CreateCommandFiles Error writing a new file"); 
				e.printStackTrace();
			}
		}
		
		System.out.println("CreateCommandFiles Number of available Csaj***.java files: " + filteredFileNames.size());
		System.out.println("CreateCommandFiles Number of created   Csaj***Command.java files: " + numberCreated);
		
	} //main method
}
