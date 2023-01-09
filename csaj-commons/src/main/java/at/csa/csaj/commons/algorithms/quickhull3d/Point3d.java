/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: Point3d.java
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
/**
  * Copyright John E. Lloyd, 2004. All rights reserved. Permission to use,
  * copy, modify and redistribute is granted, provided that this copyright
  * notice is retained and the author is given credit whenever appropriate.
  *
  * This  software is distributed "as is", without any warranty, including 
  * any implied warranty of merchantability or fitness for a particular
  * use. The author assumes no responsibility for, and shall not be liable
  * for, any special, indirect, or consequential damages, or any damages
  * whatsoever, arising out of or in connection with the use of this
  * software.
  */

package at.csa.csaj.commons.algorithms.quickhull3d;

/**
 * A three-element spatial point.
 *
 * The only difference between a point and a vector is in the
 * the way it is transformed by an affine transformation. Since
 * the transform method is not included in this reduced
 * implementation for QuickHull3D, the difference is
 * purely academic.
 *
 * @author John E. Lloyd, Fall 2004
 */
public class Point3d extends Vector3d
{
	/**
	 * Creates a Point3d and initializes it to zero.
	 */
	public Point3d ()
	 {
	 }

	/**
	 * Creates a Point3d by copying a vector
	 *
	 * @param v vector to be copied
	 */
	public Point3d (Vector3d v)
	 {
	   set (v);
	 }

	/**
	 * Creates a Point3d with the supplied element values.
	 *
	 * @param x first element
	 * @param y second element
	 * @param z third element
	 */
	public Point3d (double x, double y, double z)
	 {
	   set (x, y, z);
	 }
}
