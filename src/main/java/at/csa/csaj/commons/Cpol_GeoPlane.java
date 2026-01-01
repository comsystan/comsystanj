/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Cpol_GeoPlane.java
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

/*
 * https://www.codeproject.com/Articles/1071168/Point-Inside-D-Convex-Polygon-in-Java
 * https://www.codeproject.com/Members/John-Jiyang-Hou
 * https://www.codeproject.com/info/cpol10.aspx
 */
public class Cpol_GeoPlane {
    
    // Plane Equation: a * x + b * y + c * z + d = 0

    private double a;
    private double b;
    private double c;
    private double d;
    
    public double getA() { return this.a; }
    public double getB() { return this.b; }
    public double getC() { return this.c; }
    public double getD() { return this.d; }

    public Cpol_GeoPlane() {}
    
    public Cpol_GeoPlane(double a, double b, double c, double d)
    {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public Cpol_GeoPlane(Cpol_GeoPoint p0, Cpol_GeoPoint p1, Cpol_GeoPoint p2)
    {        
        Cpol_GeoVector v = new Cpol_GeoVector(p0, p1);

        Cpol_GeoVector u = new Cpol_GeoVector(p0, p2);

        Cpol_GeoVector n = Cpol_GeoVector.Multiple(u, v);

        // normal vector        
        double a = n.getX();
        double b = n.getY();
        double c = n.getZ();                
        double d = -(a * p0.getX() + b * p0.getY() + c * p0.getZ());
        
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;        
    }

    public static Cpol_GeoPlane Negative(Cpol_GeoPlane pl)
    {
        return new Cpol_GeoPlane(-pl.getA(), -pl.getB(), -pl.getC(), -pl.getD());
    }

    public static double Multiple(Cpol_GeoPoint pt, Cpol_GeoPlane pl)
    {
        return (pt.getX() * pl.getA() + pt.getY() * pl.getB() + 
                pt.getZ() * pl.getC() + pl.getD());
    }
}
