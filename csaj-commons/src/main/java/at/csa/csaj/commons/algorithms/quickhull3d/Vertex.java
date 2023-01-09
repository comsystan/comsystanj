/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: Vertex.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2023 Comsystan Software
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
package at.csa.csaj.commons.algorithms.quickhull3d;

/**
 * Represents vertices of the hull, as well as the points from
 * which it is formed.
 *
 * @author John E. Lloyd, Fall 2004
 */
class Vertex
{
	/**
	 * Spatial point associated with this vertex.
	 */
	Point3d pnt;

	/**
	 * Back index into an array.
	 */
	int index;

	/**
	 * List forward link.
	 */
 	Vertex prev;

	/**
	 * List backward link.
	 */
 	Vertex next;

	/**
	 * Current face that this vertex is outside of.
	 */
 	Face face;

	/**
	 * Constructs a vertex and sets its coordinates to 0.
	 */
	public Vertex()
	 { pnt = new Point3d();
	 }

	/**
	 * Constructs a vertex with the specified coordinates
	 * and index.
	 */
	public Vertex (double x, double y, double z, int idx)
	 {
	   pnt = new Point3d(x, y, z);
	   index = idx;
	 }

}
