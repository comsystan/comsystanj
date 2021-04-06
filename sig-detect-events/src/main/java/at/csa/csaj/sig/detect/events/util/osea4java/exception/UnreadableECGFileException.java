package at.csa.csaj.sig.detect.events.util.osea4java.exception;

/*
 * #%L
 * Project: ImageJ signal plugin for detecting events
 * File: UnreadableECGFileException.java
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


/**
 * This exception is thrown at runtime, when the content of the ECG file cannot be read.
 * @author HA
 */
public class UnreadableECGFileException extends Exception {


	/**
	 * 
	 */
	private static final long serialVersionUID = -6133864410881867792L;

	/**
	 * This is the default constructor.
	 */
	public UnreadableECGFileException(){
		super();
	}
	
	/**
	 * 
	 * Add a customized description to the exception.
	 * @param description
	 */
	public UnreadableECGFileException(String description){
		super(description);
	}
}


