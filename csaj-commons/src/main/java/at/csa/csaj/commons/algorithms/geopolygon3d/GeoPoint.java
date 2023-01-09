/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: GeoPoint.java
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
public class GeoPoint {
    
    private double x;
    private double y;
    private double z;
    
    public double getX() { return x; }    
    public void setX(double x) { this.x = x;}
    
    public double getY() { return y; }    
    public void setY(double y) { this.y = y;}
    
    public double getZ() { return z; }    
    public void setZ(double z) { this.z = z;}
    
    public GeoPoint(){}
    
    public GeoPoint(double x, double y, double z)        
    {
        this.x=x;
        this.y=y;
        this.z=z;    
    }    

    public static GeoPoint Add(GeoPoint p0, GeoPoint p1)
    {
        return new GeoPoint(p0.x + p1.x, p0.y + p1.y, p0.z + p1.z);
    }
}
