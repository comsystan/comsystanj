/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajContainer_Interface.java
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
package at.csa.csaj.commons;

public interface CsajContainer_Interface {

	void setItem1_Values(double[] item1_values); 
	void setItem2_Values(double[] item2_values);
	void setItem3_Values(double[] item3_Values);
	void setItem1_Matrix(double[][] item1_matrix);
	void setItem2_Matrix(double[][] item2_Matrix);
	
	double[] getItem1_Values();
	double[] getItem2_Values();
	double[] getItem3_Values();
	double[][] getItem1_Matrix();
	double[][] getItem2_Matrix();
}
