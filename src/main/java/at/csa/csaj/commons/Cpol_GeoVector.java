/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Cpol_GeoVector.java
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

/*
 * https://www.codeproject.com/Articles/1071168/Point-Inside-D-Convex-Polygon-in-Java
 * https://www.codeproject.com/Members/John-Jiyang-Hou
 * https://www.codeproject.com/info/cpol10.aspx
 */
public class Cpol_GeoVector {
    
    private Cpol_GeoPoint p0; // vector begin point
    private Cpol_GeoPoint p1; // vector end point
    private double x; // vector x axis projection value
    private double y; // vector y axis projection value
    private double z; // vector z axis projection value
    
    public Cpol_GeoPoint getP0() {return this.p0;}
    public Cpol_GeoPoint getP1() {return this.p1;}    
    public double getX() {return this.x;}
    public double getY() {return this.y;}
    public double getZ() {return this.z;}  

    public Cpol_GeoVector() {}
        
    public Cpol_GeoVector(Cpol_GeoPoint p0, Cpol_GeoPoint p1)
    {
        this.p0 = p0;
        this.p1 = p1;            
        this.x = p1.getX() - p0.getX();
        this.y = p1.getY() - p0.getY();
        this.z = p1.getZ() - p0.getZ();
    }        

    public static Cpol_GeoVector Multiple(Cpol_GeoVector u, Cpol_GeoVector v)
    {
        double x = u.getY() * v.getZ() - u.getZ() * v.getY();
        double y = u.getZ() * v.getX() - u.getX() * v.getZ();
        double z = u.getX() * v.getY() - u.getY() * v.getX();
        
        Cpol_GeoPoint p0 = v.getP0();
        Cpol_GeoPoint p1 = Cpol_GeoPoint.Add(p0, new Cpol_GeoPoint(x, y, z));

        return new Cpol_GeoVector(p0, p1);
    }
}
