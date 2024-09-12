/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajContainer_Items.java
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
package at.csa.csaj.commons;

public class CsajContainer_Items implements CsajContainer_Interface{

	public double[]   item1_Values;
	public double[]   item2_Values;
	public double[]   item3_Values;
	public double[][] item1_Matrix;
	public double[][] item2_Matrix;

	@Override
	public void setItem1_Values(double[] item1_values) {
		this.item1_Values = item1_values;
	}
	
	@Override
	public void setItem2_Values(double[] item2_values) {
		this.item2_Values = item2_values;
	}
	
	@Override
	public void setItem3_Values(double[] item3_Values) {
		this.item3_Values = item3_Values;
	}
	
	@Override
	public void setItem1_Matrix(double[][] item1_matrix) {
		this.item1_Matrix = item1_matrix;
	}
	
	@Override
	public void setItem2_Matrix(double[][] item2_Matrix) {
		this.item2_Matrix = item2_Matrix;
	}
	
	@Override
	public double[] getItem1_Values() {
		return item1_Values;
	}
	
	@Override
	public double[] getItem2_Values() {
		return item2_Values;
	}
	
	@Override
	public double[] getItem3_Values() {
		return item3_Values;
	}
	
	@Override
	public double[][] getItem1_Matrix() {
		return item1_Matrix;
	}
	
	@Override
	public double[][] getItem2_Matrix() {
		return item2_Matrix;
	}

	
	public CsajContainer_Items() {
		
	}
	
	public CsajContainer_Items( double[] item1_values) {
		this.item1_Values = item1_values;
	}
	
	public CsajContainer_Items(double[] item1_values, double[] item2_values) {
		this.item1_Values = item1_values;
		this.item2_Values = item2_values;
	}
	
	public CsajContainer_Items(double[][] item1_matrix) {
		this.item1_Matrix = item1_matrix;
	}
	
	public CsajContainer_Items(double[][] item1_matrix, double[] item2_values) {
		this.item1_Matrix = item1_matrix;
		this.item2_Values = item2_values;
	}
	
	public CsajContainer_Items(double[][] item1_matrix, double[][] item2_matrix, double[] item3_values) {
		this.item1_Matrix = item1_matrix;
		this.item2_Matrix = item2_matrix;
		this.item3_Values = item3_values;
	}
	
}
