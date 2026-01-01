/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: QuickHull3D_Vertex.java
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

/**
 * Represents vertices of the hull, as well as the points from
 * which it is formed.
 *
 * @author John E. Lloyd, Fall 2004
 */
class QuickHull3D_Vertex
{
	/**
	 * Spatial point associated with this vertex.
	 */
	QuickHull3D_Point3d pnt;

	/**
	 * Back index into an array.
	 */
	int index;

	/**
	 * List forward link.
	 */
 	QuickHull3D_Vertex prev;

	/**
	 * List backward link.
	 */
 	QuickHull3D_Vertex next;

	/**
	 * Current face that this vertex is outside of.
	 */
 	QuickHull3D_Face face;

	/**
	 * Constructs a vertex and sets its coordinates to 0.
	 */
	public QuickHull3D_Vertex()
	 { pnt = new QuickHull3D_Point3d();
	 }

	/**
	 * Constructs a vertex with the specified coordinates
	 * and index.
	 */
	public QuickHull3D_Vertex (double x, double y, double z, int idx)
	 {
	   pnt = new QuickHull3D_Point3d(x, y, z);
	   index = idx;
	 }

}
