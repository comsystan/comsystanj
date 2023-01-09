/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: GeoVector.java
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
package at.csa.csaj.commons.algorithms.geopolygon3d;

/*
 * https://www.codeproject.com/Articles/1071168/Point-Inside-D-Convex-Polygon-in-Java
 * https://www.codeproject.com/Members/John-Jiyang-Hou
 * https://www.codeproject.com/info/cpol10.aspx
 */
public class GeoVector {
    
    private GeoPoint p0; // vector begin point
    private GeoPoint p1; // vector end point
    private double x; // vector x axis projection value
    private double y; // vector y axis projection value
    private double z; // vector z axis projection value
    
    public GeoPoint getP0() {return this.p0;}
    public GeoPoint getP1() {return this.p1;}    
    public double getX() {return this.x;}
    public double getY() {return this.y;}
    public double getZ() {return this.z;}  

    public GeoVector() {}
        
    public GeoVector(GeoPoint p0, GeoPoint p1)
    {
        this.p0 = p0;
        this.p1 = p1;            
        this.x = p1.getX() - p0.getX();
        this.y = p1.getY() - p0.getY();
        this.z = p1.getZ() - p0.getZ();
    }        

    public static GeoVector Multiple(GeoVector u, GeoVector v)
    {
        double x = u.getY() * v.getZ() - u.getZ() * v.getY();
        double y = u.getZ() * v.getX() - u.getX() * v.getZ();
        double z = u.getX() * v.getY() - u.getY() * v.getX();
        
        GeoPoint p0 = v.getP0();
        GeoPoint p1 = GeoPoint.Add(p0, new GeoPoint(x, y, z));

        return new GeoVector(p0, p1);
    }
}
