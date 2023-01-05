/*-
 * #%L
 * Project: ImageJ2 plugin for computing the 3D Generalized fractal dimensions.
 * File: GeneralizedDim3DMethods.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2022 - 2023 Comsystan Software
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

package at.csa.csaj.plugin3d.frac.dim.generalized.util;

public interface GeneralizedDim3DMethods{


	int[] calcEps();

	double[][] calcTotals();

	void setEps(int[] eps);

	int[] getEps();

	void setTotals(double[][] totals);

	double[][] getTotals();
	
}
