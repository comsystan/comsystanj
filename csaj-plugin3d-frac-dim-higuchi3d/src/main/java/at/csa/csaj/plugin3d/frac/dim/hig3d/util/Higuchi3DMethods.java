/*-
 * #%L
 * Project: ImageJ2 plugin for computing fractal dimension with 3D Higuchi algorithms.
 * File: Higuchi3DMethods.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2022 Comsystan Software
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

package at.csa.csaj.plugin3d.frac.dim.hig3d.util;

public interface Higuchi3DMethods{


	double[] calcEps();

	double[] calcTotals();

	void setEps(double[] eps);

	double[] getEps();

	void setTotals(double[] totals);

	double[] getTotals();
	
}
