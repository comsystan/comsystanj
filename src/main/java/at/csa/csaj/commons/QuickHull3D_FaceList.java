/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: QuickHull3D_FaceList.java
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
package at.csa.csaj.commons;

/**
 * Maintains a single-linked list of faces for use by QuickHull3D
 */
class QuickHull3D_FaceList
{
	private QuickHull3D_Face head;
	private QuickHull3D_Face tail;

	/**
	 * Clears this list.
	 */
	public void clear()
	 {
	   head = tail = null; 
	 }

	/**
	 * Adds a vertex to the end of this list.
	 */
	public void add (QuickHull3D_Face vtx)
	 { 
	   if (head == null)
	    { head = vtx;
	    }
	   else
	    { tail.next = vtx; 
	    }
	   vtx.next = null;
	   tail = vtx;
	 }

	public QuickHull3D_Face first()
	 {
	   return head;
	 }

	/**
	 * Returns true if this list is empty.
	 */
	public boolean isEmpty()
	 {
	   return head == null;
	 }
}
