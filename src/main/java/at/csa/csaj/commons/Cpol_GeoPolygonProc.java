/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Cpol_GeoPolygonProc.java
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

import java.util.ArrayList;

/*
 * https://www.codeproject.com/Articles/1071168/Point-Inside-D-Convex-Polygon-in-Java
 * https://www.codeproject.com/Members/John-Jiyang-Hou
 * https://www.codeproject.com/info/cpol10.aspx
 */
public class Cpol_GeoPolygonProc {
    
    private double MaxUnitMeasureError = 0.001;

    // Polygon Boundary
    private double X0, X1, Y0, Y1, Z0, Z1;    

    // Polygon faces
    private ArrayList<Cpol_GeoFace> Faces;

    // Polygon face planes
    private ArrayList<Cpol_GeoPlane> FacePlanes;

    // Number of faces
    private int NumberOfFaces;    

    // Maximum point to face plane distance error,
    // point is considered in the face plane if its distance is less than this error
    private double MaxDisError;

    public double getX0() { return this.X0; }
    public double getX1() { return this.X1; }
    public double getY0() { return this.Y0; }
    public double getY1() { return this.Y1; }
    public double getZ0() { return this.Z0; }
    public double getZ1() { return this.Z1; }
    public ArrayList<Cpol_GeoFace> getFaces() { return this.Faces; }
    public ArrayList<Cpol_GeoPlane> GetFacePlanes() { return this.FacePlanes; }
    public int getNumberOfFaces() { return this.NumberOfFaces; }
    
    public Cpol_GeoPolygonProc(){}        
        
    public Cpol_GeoPolygonProc(Cpol_GeoPolygon polygonInst)
    {
        
        // Set boundary
        this.Set3DPolygonBoundary(polygonInst);

        // Set maximum point to face plane distance error,         
        this.Set3DPolygonUnitError(polygonInst);

        // Set faces and face planes        
        this.SetConvex3DFaces(polygonInst);      
    }    

    public boolean PointInside3DPolygon(double x, double y, double z)
    {
        Cpol_GeoPoint P = new Cpol_GeoPoint(x, y, z);
        
        for (int i = 0; i < this.NumberOfFaces; i++)
        {

            double dis = Cpol_GeoPlane.Multiple(P, this.FacePlanes.get(i));

            // If the point is in the same half space with normal vector for any face of the cube,
            // then it is outside of the 3D polygon        
            if (dis > 0)
            {
                return false;
            }
        }

        // If the point is in the opposite half space with normal vector for all 6 faces,
        // then it is inside of the 3D polygon
        return true;            
    }

    private void Set3DPolygonUnitError(Cpol_GeoPolygon polygon)
    {        
        this.MaxDisError = ((Math.abs(this.X0) + Math.abs(this.X1) +
                Math.abs(this.Y0) + Math.abs(this.Y1) +
                Math.abs(this.Z0) + Math.abs(this.Z1)) / 6 * MaxUnitMeasureError);        
    }

    private void Set3DPolygonBoundary(Cpol_GeoPolygon polygon)
    {
        ArrayList<Cpol_GeoPoint> vertices = polygon.getV();

        int n = polygon.getN();
        
        double xmin, xmax, ymin, ymax, zmin, zmax;

        xmin = xmax = vertices.get(0).getX();
        ymin = ymax = vertices.get(0).getY();
        zmin = zmax = vertices.get(0).getZ();

        for (int i = 1; i < n; i++)
        {
            if (vertices.get(i).getX() < xmin) xmin = vertices.get(i).getX();
            if (vertices.get(i).getY() < ymin) ymin = vertices.get(i).getY();
            if (vertices.get(i).getZ() < zmin) zmin = vertices.get(i).getZ();
            if (vertices.get(i).getX() > xmax) xmax = vertices.get(i).getX();
            if (vertices.get(i).getY() > ymax) ymax = vertices.get(i).getY();
            if (vertices.get(i).getZ() > zmax) zmax = vertices.get(i).getZ();
        }        
        
        this.X0 = xmin;
        this.X1 = xmax;
        this.Y0 = ymin;
        this.Y1 = ymax;
        this.Z0 = zmin;
        this.Z1 = zmax;
    }
    
    private void SetConvex3DFaces(Cpol_GeoPolygon polygon)
    {        
        ArrayList<Cpol_GeoFace> faces = new ArrayList<Cpol_GeoFace>();

        ArrayList<Cpol_GeoPlane> facePlanes = new ArrayList<Cpol_GeoPlane>();
        
        int numberOfFaces;
        
        double maxError = this.MaxDisError;
        
        // vertices of 3D polygon
        ArrayList<Cpol_GeoPoint> vertices = polygon.getV();
      
        int n = polygon.getN();

        // vertices indexes for all faces
        // vertices index is the original index value in the input polygon
        ArrayList<ArrayList<Integer>> faceVerticeIndex = new ArrayList<ArrayList<Integer>>();
        
        // face planes for all faces
        ArrayList<Cpol_GeoPlane> fpOutward = new ArrayList<Cpol_GeoPlane>();    
            
        for(int i=0; i< n; i++)
        {
            // triangle point 1
            Cpol_GeoPoint p0 = vertices.get(i);

            for(int j= i+1; j< n; j++)
            {
                // triangle point 2
                Cpol_GeoPoint p1 = vertices.get(j);

                for(int k = j + 1; k<n; k++)
                {
                    // triangle point 3
                    Cpol_GeoPoint p2 = vertices.get(k);
    
                    Cpol_GeoPlane trianglePlane = new Cpol_GeoPlane(p0, p1, p2);
                
                    int onLeftCount = 0;
                    int onRightCount = 0;

                    // indexes of points that lie in same plane with face triangle plane
                    ArrayList<Integer> pointInSamePlaneIndex = new ArrayList<Integer>();
        
                    for(int l = 0; l < n ; l ++)
                    {                        
                        // any point other than the 3 triangle points
                        if(l != i && l != j && l != k)
                        {
                            Cpol_GeoPoint p = vertices.get(l);

                            double dis = Cpol_GeoPlane.Multiple(p, trianglePlane);
                            
                            // next point is in the triangle plane
                            if(Math.abs(dis) < maxError)
                            {                            
                                pointInSamePlaneIndex.add(l);                                    
                            }
                            else
                            {
                                if(dis < 0)
                                {
                                    onLeftCount ++;                                
                                }
                                else
                                {
                                    onRightCount ++;
                                }
                            }
                        }
                    }
            
                    // This is a face for a CONVEX 3d polygon.
                    // For a CONCAVE 3d polygon, this maybe not a face.
                    if(onLeftCount == 0 || onRightCount == 0)
                    {
                        ArrayList<Integer> verticeIndexInOneFace = new ArrayList<Integer>();
                       
                        // triangle plane
                        verticeIndexInOneFace.add(i);
                        verticeIndexInOneFace.add(j);
                        verticeIndexInOneFace.add(k);
                        
                        int m = pointInSamePlaneIndex.size();

                        if(m > 0) // there are other vertices in this triangle plane
                        {
                            for(int p = 0; p < m; p ++)
                            {
                                verticeIndexInOneFace.add(pointInSamePlaneIndex.get(p));
                            }                        
                        }

                        // if verticeIndexInOneFace is a new face,
                        // add it in the faceVerticeIndex list,
                        // add the trianglePlane in the face plane list fpOutward
                        //if (!Utility.ContainsList(faceVerticeIndex, verticeIndexInOneFace))
                        if (!faceVerticeIndex.contains(verticeIndexInOneFace))
                        {
                            faceVerticeIndex.add(verticeIndexInOneFace);

                            if (onRightCount == 0)
                            {
                                fpOutward.add(trianglePlane);
                            }
                            else if (onLeftCount == 0)
                            {
                                fpOutward.add(Cpol_GeoPlane.Negative(trianglePlane));
                            }
                        }
                    }
                    else
                    {                    
                        // possible reasons:
                        // 1. the plane is not a face of a convex 3d polygon,
                        //    it is a plane crossing the convex 3d polygon.
                        // 2. the plane is a face of a concave 3d polygon
                    }

                } // k loop
            } // j loop        
        } // i loop                        

        // return number of faces
        numberOfFaces = faceVerticeIndex.size();
        
        for (int i = 0; i < numberOfFaces; i++)
        {                
            // return face planes
            facePlanes.add(new Cpol_GeoPlane(fpOutward.get(i).getA(), fpOutward.get(i).getB(), 
                                        fpOutward.get(i).getC(), fpOutward.get(i).getD()));

            ArrayList<Cpol_GeoPoint> gp = new ArrayList<Cpol_GeoPoint>();

            ArrayList<Integer> vi = new ArrayList<Integer>();
            
            int count = faceVerticeIndex.get(i).size();
            for (int j = 0; j < count; j++)
            {
                vi.add(faceVerticeIndex.get(i).get(j));
                gp.add( new Cpol_GeoPoint(vertices.get(vi.get(j)).getX(),
                                     vertices.get(vi.get(j)).getY(),
                                     vertices.get(vi.get(j)).getZ()));
            }

            // return faces
            faces.add(new Cpol_GeoFace(gp, vi));
        }
        
        this.Faces = faces;
        this.FacePlanes = facePlanes;
        this.NumberOfFaces = numberOfFaces;        
    }
}
