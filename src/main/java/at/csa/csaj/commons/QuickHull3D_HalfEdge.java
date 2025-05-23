/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: QuickHull3D_HalfEdge.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2025 Comsystan Software
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
 /*
  * Copyright John E. Lloyd, 2003. All rights reserved. Permission
  * to use, copy, and modify, without fee, is granted for non-commercial 
  * and research purposes, provided that this copyright notice appears 
  * in all copies.
  *
  * This  software is distributed "as is", without any warranty, including 
  * any implied warranty of merchantability or fitness for a particular
  * use. The authors assume no responsibility for, and shall not be liable
  * for, any special, indirect, or consequential damages, or any damages
  * whatsoever, arising out of or in connection with the use of this
  * software.
  */

package at.csa.csaj.commons;

/**
 * Represents the half-edges that surround each
 * face in a counter-clockwise direction.
 *
 * @author John E. Lloyd, Fall 2004 */
class QuickHull3D_HalfEdge
{
	/**
	 * The vertex associated with the head of this half-edge.
	 */
	QuickHull3D_Vertex vertex;

	/**
	 * Triangular face associated with this half-edge.
	 */
	QuickHull3D_Face face;

	/**
	 * Next half-edge in the triangle.
	 */
	QuickHull3D_HalfEdge next;

	/**
	 * Previous half-edge in the triangle.
	 */
	QuickHull3D_HalfEdge prev;

	/**
	 * Half-edge associated with the opposite triangle
	 * adjacent to this edge.
	 */
	QuickHull3D_HalfEdge opposite;

	/**
	 * Constructs a HalfEdge with head vertex <code>v</code> and
	 * left-hand triangular face <code>f</code>.
	 *
	 * @param v head vertex
	 * @param f left-hand triangular face
	 */
	public QuickHull3D_HalfEdge (QuickHull3D_Vertex v, QuickHull3D_Face f)
	 {
	   vertex = v;
	   face = f;
	 }

	public QuickHull3D_HalfEdge ()
	 { 
	 }

	/**
	 * Sets the value of the next edge adjacent
	 * (counter-clockwise) to this one within the triangle.
	 *
	 * @param edge next adjacent edge */
	public void setNext (QuickHull3D_HalfEdge edge)
	 {
	   next = edge;
	 }
	
	/**
	 * Gets the value of the next edge adjacent
	 * (counter-clockwise) to this one within the triangle.
	 *
	 * @return next adjacent edge */
	public QuickHull3D_HalfEdge getNext()
	 {
	   return next;
	 }

	/**
	 * Sets the value of the previous edge adjacent (clockwise) to
	 * this one within the triangle.
	 *
	 * @param edge previous adjacent edge */
	public void setPrev (QuickHull3D_HalfEdge edge)
	 {
	   prev = edge;
	 }
	
	/**
	 * Gets the value of the previous edge adjacent (clockwise) to
	 * this one within the triangle.
	 *
	 * @return previous adjacent edge
	 */
	public QuickHull3D_HalfEdge getPrev()
	 {
	   return prev;
	 }

	/**
	 * Returns the triangular face located to the left of this
	 * half-edge.
	 *
	 * @return left-hand triangular face
	 */
	public QuickHull3D_Face getFace()
	 {
	   return face;
	 }

	/**
	 * Returns the half-edge opposite to this half-edge.
	 *
	 * @return opposite half-edge
	 */
	public QuickHull3D_HalfEdge getOpposite()
	 {
	   return opposite;
	 }

	/**
	 * Sets the half-edge opposite to this half-edge.
	 *
	 * @param edge opposite half-edge
	 */
	public void setOpposite (QuickHull3D_HalfEdge edge)
	 {
	   opposite = edge;
	   edge.opposite = this;
	 }

	/**
	 * Returns the head vertex associated with this half-edge.
	 *
	 * @return head vertex
	 */
	public QuickHull3D_Vertex head()
	 {
	   return vertex;
	 }

	/**
	 * Returns the tail vertex associated with this half-edge.
	 *
	 * @return tail vertex
	 */
	public QuickHull3D_Vertex tail()
	 {
	   return prev != null ? prev.vertex : null;
	 }

	/**
	 * Returns the opposite triangular face associated with this
	 * half-edge.
	 *
	 * @return opposite triangular face
	 */
	public QuickHull3D_Face oppositeFace()
	 {
	   return opposite != null ? opposite.face : null;
	 }

	/**
	 * Produces a string identifying this half-edge by the point
	 * index values of its tail and head vertices.
	 *
	 * @return identifying string
	 */
	public String getVertexString()
	 {
	   if (tail() != null)
	    { return "" +
		 tail().index + "-" +
		 head().index;
	    }
	   else
	    { return "?-" + head().index;
	    }
	 }

	/**
	 * Returns the length of this half-edge.
	 *
	 * @return half-edge length
	 */
	public double length()
	 {
	   if (tail() != null)
	    { return head().pnt.distance(tail().pnt);
	    }
	   else
	    { return -1; 
	    }
	 }

	/**
	 * Returns the length squared of this half-edge.
	 *
	 * @return half-edge length squared
	 */
	public double lengthSquared()
	 {
	   if (tail() != null)
	    { return head().pnt.distanceSquared(tail().pnt);
	    }
	   else
	    { return -1; 
	    }
	 }


// 	/**
// 	 * Computes nrml . (del0 X del1), where del0 and del1
// 	 * are the direction vectors along this halfEdge, and the
// 	 * halfEdge he1.
// 	 *
// 	 * A product > 0 indicates a left turn WRT the normal
// 	 */
// 	public double turnProduct (HalfEdge he1, Vector3d nrml)
// 	 { 
// 	   Point3d pnt0 = tail().pnt;
// 	   Point3d pnt1 = head().pnt;
// 	   Point3d pnt2 = he1.head().pnt;

// 	   double del0x = pnt1.x - pnt0.x;
// 	   double del0y = pnt1.y - pnt0.y;
// 	   double del0z = pnt1.z - pnt0.z;

// 	   double del1x = pnt2.x - pnt1.x;
// 	   double del1y = pnt2.y - pnt1.y;
// 	   double del1z = pnt2.z - pnt1.z;

// 	   return (nrml.x*(del0y*del1z - del0z*del1y) + 
// 		   nrml.y*(del0z*del1x - del0x*del1z) + 
// 		   nrml.z*(del0x*del1y - del0y*del1x));
// 	 }
}


