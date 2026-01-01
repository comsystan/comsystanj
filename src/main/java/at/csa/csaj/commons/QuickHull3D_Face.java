/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: QuickHull3D_Face.java
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

import java.util.*;

/**
 * Basic triangular face used to form the hull.
 *
 * <p>The information stored for each face consists of a planar
 * normal, a planar offset, and a doubly-linked list of three <a
 * href=HalfEdge>HalfEdges</a> which surround the face in a
 * counter-clockwise direction.
 *
 * @author John E. Lloyd, Fall 2004 */
class QuickHull3D_Face
{
	QuickHull3D_HalfEdge he0;
	private QuickHull3D_Vector3d normal;
	double area;
	private QuickHull3D_Point3d centroid;
	double planeOffset;
	int index;
	int numVerts;

	QuickHull3D_Face next;

	static final int VISIBLE = 1;
	static final int NON_CONVEX = 2;
	static final int DELETED = 3;

	int mark = VISIBLE;

	QuickHull3D_Vertex outside;

	public void computeCentroid (QuickHull3D_Point3d centroid)
	 {
	   centroid.setZero();
	   QuickHull3D_HalfEdge he = he0;
	   do
	    { centroid.add (he.head().pnt);
	      he = he.next;
	    }
	   while (he != he0);
	   centroid.scale (1/(double)numVerts);
	 }

	public void computeNormal (QuickHull3D_Vector3d normal, double minArea)
	 {
	   computeNormal(normal);

	   if (area < minArea)
	    { 
	      // make the normal more robust by removing
	      // components parallel to the longest edge

	      QuickHull3D_HalfEdge hedgeMax = null;
	      double lenSqrMax = 0;
	      QuickHull3D_HalfEdge hedge = he0;
	      do
	       { double lenSqr = hedge.lengthSquared();
		 if (lenSqr > lenSqrMax)
		  { hedgeMax = hedge;
		    lenSqrMax = lenSqr;
		  }
		 hedge = hedge.next;
	       }
	      while (hedge != he0);

	      QuickHull3D_Point3d p2 = hedgeMax.head().pnt;
	      QuickHull3D_Point3d p1 = hedgeMax.tail().pnt;
	      double lenMax = Math.sqrt(lenSqrMax);
	      double ux = (p2.x - p1.x)/lenMax;
	      double uy = (p2.y - p1.y)/lenMax;
	      double uz = (p2.z - p1.z)/lenMax;	   
	      double dot = normal.x*ux + normal.y*uy + normal.z*uz;
	      normal.x -= dot*ux;
	      normal.y -= dot*uy;
	      normal.z -= dot*uz;

	      normal.normalize();	      
	    }
	 }

	public void computeNormal (QuickHull3D_Vector3d normal)
	 {
	   QuickHull3D_HalfEdge he1 = he0.next;
	   QuickHull3D_HalfEdge he2 = he1.next;

	   QuickHull3D_Point3d p0 = he0.head().pnt;
	   QuickHull3D_Point3d p2 = he1.head().pnt;

	   double d2x = p2.x - p0.x;
	   double d2y = p2.y - p0.y;
	   double d2z = p2.z - p0.z;

	   normal.setZero();

	   numVerts = 2;

	   while (he2 != he0)
	    { 
	      double d1x = d2x;
	      double d1y = d2y;
	      double d1z = d2z;

	      p2 = he2.head().pnt;
	      d2x = p2.x - p0.x;
	      d2y = p2.y - p0.y;
	      d2z = p2.z - p0.z;

	      normal.x += d1y*d2z - d1z*d2y;
	      normal.y += d1z*d2x - d1x*d2z;
	      normal.z += d1x*d2y - d1y*d2x;

	      he1 = he2;
	      he2 = he2.next;
	      numVerts++;
	    }
	   area = normal.norm();
	   normal.scale (1/area);
	 }

	private void computeNormalAndCentroid()
	 {
	   computeNormal (normal);
	   computeCentroid (centroid);
	   planeOffset = normal.dot(centroid);
	   int numv = 0;
	   QuickHull3D_HalfEdge he = he0;
	   do
	    { numv++;
	      he = he.next;
	    }
	   while (he != he0);
	   if (numv != numVerts)
	    {  throw new QuickHull3D_InternalErrorException (
"face " + getVertexString() + " numVerts=" + numVerts + " should be " + numv);
	    }
	 }

	private void computeNormalAndCentroid(double minArea)
	 {
	   computeNormal (normal, minArea);
	   computeCentroid (centroid);
	   planeOffset = normal.dot(centroid);
	 }

	public static QuickHull3D_Face createTriangle (QuickHull3D_Vertex v0, QuickHull3D_Vertex v1, QuickHull3D_Vertex v2)
	 {
	   return createTriangle (v0, v1, v2, 0);
	 }

	/**
	 * Constructs a triangule Face from vertices v0, v1, and v2.
	 *
	 * @param v0 first vertex
	 * @param v1 second vertex
	 * @param v2 third vertex
	 */
	public static QuickHull3D_Face createTriangle (QuickHull3D_Vertex v0, QuickHull3D_Vertex v1, QuickHull3D_Vertex v2,
					   double minArea)
	 {
	   QuickHull3D_Face face = new QuickHull3D_Face();
	   QuickHull3D_HalfEdge he0 = new QuickHull3D_HalfEdge (v0, face);
	   QuickHull3D_HalfEdge he1 = new QuickHull3D_HalfEdge (v1, face);
	   QuickHull3D_HalfEdge he2 = new QuickHull3D_HalfEdge (v2, face);

	   he0.prev = he2;
	   he0.next = he1;
	   he1.prev = he0;
	   he1.next = he2;
	   he2.prev = he1;
	   he2.next = he0;

	   face.he0 = he0;

	   // compute the normal and offset
	   face.computeNormalAndCentroid(minArea);
	   return face;
	 }

	public static QuickHull3D_Face create (QuickHull3D_Vertex[] vtxArray, int[] indices)
	 {
	   QuickHull3D_Face face = new QuickHull3D_Face();
	   QuickHull3D_HalfEdge hePrev = null;
	   for (int i=0; i<indices.length; i++)
	    { QuickHull3D_HalfEdge he = new QuickHull3D_HalfEdge (vtxArray[indices[i]], face); 
	      if (hePrev != null)
	       { he.setPrev (hePrev);
		 hePrev.setNext (he);
	       }
	      else
	       { face.he0 = he; 
	       }
	      hePrev = he;
	    }
	   face.he0.setPrev (hePrev);
	   hePrev.setNext (face.he0);

	   // compute the normal and offset
	   face.computeNormalAndCentroid();
	   return face;	   
	 }

	public QuickHull3D_Face ()
	 { 
	   normal = new QuickHull3D_Vector3d();
	   centroid = new QuickHull3D_Point3d();
	   mark = VISIBLE;
	 }

	/**
	 * Gets the i-th half-edge associated with the face.
	 * 
	 * @param i the half-edge index, in the range 0-2.
	 * @return the half-edge
	 */
	public QuickHull3D_HalfEdge getEdge(int i)
	 {
	   QuickHull3D_HalfEdge he = he0;
	   while (i > 0)
	    { he = he.next;
	      i--;
	    }
	   while (i < 0)
	    { he = he.prev;
	      i++;
	    }
	   return he;
	 }

	public QuickHull3D_HalfEdge getFirstEdge()
	 { return he0;
	 }
	
	/**
	 * Finds the half-edge within this face which has
	 * tail <code>vt</code> and head <code>vh</code>.
	 *
	 * @param vt tail point
	 * @param vh head point
	 * @return the half-edge, or null if none is found.
	 */
	public QuickHull3D_HalfEdge findEdge (QuickHull3D_Vertex vt, QuickHull3D_Vertex vh)
	 {
	   QuickHull3D_HalfEdge he = he0;
	   do
	    { if (he.head() == vh && he.tail() == vt)
	       { return he;
	       }
	      he = he.next;
	    }
	   while (he != he0);
	   return null;
	 }

	/**
	 * Computes the distance from a point p to the plane of
	 * this face.
	 *
	 * @param p the point
	 * @return distance from the point to the plane
	 */
	public double distanceToPlane (QuickHull3D_Point3d p)
	 {
	   return normal.x*p.x + normal.y*p.y + normal.z*p.z - planeOffset;
	 }

	/**
	 * Returns the normal of the plane associated with this face.
	 *
	 * @return the planar normal
	 */
	public QuickHull3D_Vector3d getNormal ()
	 {
	   return normal;
	 }

	public QuickHull3D_Point3d getCentroid ()
	 {
	   return centroid;
	 }

	public int numVertices()
	 {
	   return numVerts;
	 }

	public String getVertexString ()
	 {
	   String s = null;
	   QuickHull3D_HalfEdge he = he0;
	   do
	    { if (s == null)
	       { s = "" + he.head().index;
	       }
	      else
	       { s += " " + he.head().index;
	       }
	      he = he.next;
	    }
	   while (he != he0);
	   return s;
	 }

	public void getVertexIndices (int[] idxs)
	 {
	   QuickHull3D_HalfEdge he = he0;
	   int i = 0;
	   do
	    { idxs[i++] = he.head().index;
	      he = he.next;
	    }
	   while (he != he0);
	 }

	private QuickHull3D_Face connectHalfEdges (
	   QuickHull3D_HalfEdge hedgePrev, QuickHull3D_HalfEdge hedge)
	 {
	   QuickHull3D_Face discardedFace = null;

	   if (hedgePrev.oppositeFace() == hedge.oppositeFace())
	    { // then there is a redundant edge that we can get rid off

	      QuickHull3D_Face oppFace = hedge.oppositeFace();
	      QuickHull3D_HalfEdge hedgeOpp;

	      if (hedgePrev == he0)
	       { he0 = hedge; 
	       }
	      if (oppFace.numVertices() == 3)
	       { // then we can get rid of the opposite face altogether
		 hedgeOpp = hedge.getOpposite().prev.getOpposite();

		 oppFace.mark = DELETED;
		 discardedFace = oppFace;
	       }
	      else
	       { hedgeOpp = hedge.getOpposite().next;

		 if (oppFace.he0 == hedgeOpp.prev)
		  { oppFace.he0 = hedgeOpp; 
		  }
		 hedgeOpp.prev = hedgeOpp.prev.prev;
		 hedgeOpp.prev.next = hedgeOpp;
	       }
	      hedge.prev = hedgePrev.prev;
	      hedge.prev.next = hedge;

	      hedge.opposite = hedgeOpp;
	      hedgeOpp.opposite = hedge;

	      // oppFace was modified, so need to recompute
	      oppFace.computeNormalAndCentroid();
	    }
	   else
	    { hedgePrev.next = hedge;
	      hedge.prev = hedgePrev;
	    }
	   return discardedFace;
	 }

	void checkConsistency()
	 {
	   // do a sanity check on the face
	   QuickHull3D_HalfEdge hedge = he0; 
	   double maxd = 0;
	   int numv = 0;

	   if (numVerts < 3)
	    { throw new QuickHull3D_InternalErrorException (
		    "degenerate face: " + getVertexString());
	    }
	   do
	    { QuickHull3D_HalfEdge hedgeOpp = hedge.getOpposite();
	      if (hedgeOpp == null)
	       { throw new QuickHull3D_InternalErrorException (
		    "face " + getVertexString() + ": " +
		    "unreflected half edge " + hedge.getVertexString());
	       }
	      else if (hedgeOpp.getOpposite() != hedge)
	       { throw new QuickHull3D_InternalErrorException (
		    "face " + getVertexString() + ": " +
		    "opposite half edge " + hedgeOpp.getVertexString() +
		    " has opposite " +
		    hedgeOpp.getOpposite().getVertexString());
	       }
	      if (hedgeOpp.head() != hedge.tail() ||
		  hedge.head() != hedgeOpp.tail())
	       { throw new QuickHull3D_InternalErrorException (
		    "face " + getVertexString() + ": " +
		    "half edge " + hedge.getVertexString() +
		    " reflected by " + hedgeOpp.getVertexString());
	       }
	      QuickHull3D_Face oppFace = hedgeOpp.face;
	      if (oppFace == null)
	       { throw new QuickHull3D_InternalErrorException (
		    "face " + getVertexString() + ": " +
		    "no face on half edge " + hedgeOpp.getVertexString());
	       }
	      else if (oppFace.mark == DELETED)
	       { throw new QuickHull3D_InternalErrorException (
		    "face " + getVertexString() + ": " +
		    "opposite face " + oppFace.getVertexString() + 
		    " not on hull");
	       }
	      double d = Math.abs(distanceToPlane(hedge.head().pnt));
	      if (d > maxd)
	       { maxd = d;
	       }
	      numv++;
	      hedge = hedge.next;
	    }
	   while (hedge != he0);

	   if (numv != numVerts)
	    {  throw new QuickHull3D_InternalErrorException (
"face " + getVertexString() + " numVerts=" + numVerts + " should be " + numv);
	    }

	 }

	public int mergeAdjacentFace (QuickHull3D_HalfEdge hedgeAdj,
				      QuickHull3D_Face[] discarded)
	 {
	   QuickHull3D_Face oppFace = hedgeAdj.oppositeFace();
	   int numDiscarded = 0;

	   discarded[numDiscarded++] = oppFace;
	   oppFace.mark = DELETED;

	   QuickHull3D_HalfEdge hedgeOpp = hedgeAdj.getOpposite();

	   QuickHull3D_HalfEdge hedgeAdjPrev = hedgeAdj.prev;
	   QuickHull3D_HalfEdge hedgeAdjNext = hedgeAdj.next;
	   QuickHull3D_HalfEdge hedgeOppPrev = hedgeOpp.prev;
	   QuickHull3D_HalfEdge hedgeOppNext = hedgeOpp.next;

	   while (hedgeAdjPrev.oppositeFace() == oppFace)
	    { hedgeAdjPrev = hedgeAdjPrev.prev;
	      hedgeOppNext = hedgeOppNext.next;
	    }
	   
	   while (hedgeAdjNext.oppositeFace() == oppFace)
	    { hedgeOppPrev = hedgeOppPrev.prev;
	      hedgeAdjNext = hedgeAdjNext.next;
	    }

	   QuickHull3D_HalfEdge hedge;

	   for (hedge=hedgeOppNext; hedge!=hedgeOppPrev.next; hedge=hedge.next)
	    { hedge.face = this;
	    }

	   if (hedgeAdj == he0)
	    { he0 = hedgeAdjNext; 
	    }
	   
	   // handle the half edges at the head
	   QuickHull3D_Face discardedFace;

	   discardedFace = connectHalfEdges (hedgeOppPrev, hedgeAdjNext);
	   if (discardedFace != null)
	    { discarded[numDiscarded++] = discardedFace; 
	    }

	   // handle the half edges at the tail
	   discardedFace = connectHalfEdges (hedgeAdjPrev, hedgeOppNext);
	   if (discardedFace != null)
	    { discarded[numDiscarded++] = discardedFace; 
	    }

	   computeNormalAndCentroid ();
	   checkConsistency();

	   return numDiscarded;
	 }

	private double areaSquared (QuickHull3D_HalfEdge hedge0, QuickHull3D_HalfEdge hedge1)
	 {
	   // return the squared area of the triangle defined
	   // by the half edge hedge0 and the point at the
	   // head of hedge1.

	   QuickHull3D_Point3d p0 = hedge0.tail().pnt;
	   QuickHull3D_Point3d p1 = hedge0.head().pnt;
	   QuickHull3D_Point3d p2 = hedge1.head().pnt;

	   double dx1 = p1.x - p0.x;
	   double dy1 = p1.y - p0.y;
	   double dz1 = p1.z - p0.z;

	   double dx2 = p2.x - p0.x;
	   double dy2 = p2.y - p0.y;
	   double dz2 = p2.z - p0.z;

	   double x = dy1*dz2 - dz1*dy2;
	   double y = dz1*dx2 - dx1*dz2;
	   double z = dx1*dy2 - dy1*dx2;

	   return x*x + y*y + z*z;	   
	 }

	public void triangulate (QuickHull3D_FaceList newFaces, double minArea)
	 {
	   QuickHull3D_HalfEdge hedge;

	   if (numVertices() < 4)
	    { return; 
	    }

	   QuickHull3D_Vertex v0 = he0.head();
	   QuickHull3D_Face prevFace = null;

	   hedge = he0.next;
	   QuickHull3D_HalfEdge oppPrev = hedge.opposite;
	   QuickHull3D_Face face0 = null;

	   for (hedge=hedge.next; hedge!=he0.prev; hedge=hedge.next)	   
	    { QuickHull3D_Face face =
		 createTriangle (v0, hedge.prev.head(), hedge.head(), minArea);
	      face.he0.next.setOpposite (oppPrev);
	      face.he0.prev.setOpposite (hedge.opposite);
	      oppPrev = face.he0;
	      newFaces.add (face);
	      if (face0 == null)
	       { face0 = face; 
	       }
	    }
	   hedge = new QuickHull3D_HalfEdge (he0.prev.prev.head(), this);
	   hedge.setOpposite (oppPrev);

	   hedge.prev = he0;
	   hedge.prev.next = hedge;

	   hedge.next = he0.prev;
	   hedge.next.prev = hedge;

	   computeNormalAndCentroid (minArea);
	   checkConsistency();

	   for (QuickHull3D_Face face=face0; face!=null; face=face.next)
	    { face.checkConsistency(); 
	    }

	 }
}



