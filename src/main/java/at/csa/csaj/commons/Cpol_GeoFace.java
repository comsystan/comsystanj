/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Cpol_GeoFace.java
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

import java.util.ArrayList;

/*
 * https://www.codeproject.com/Articles/1071168/Point-Inside-D-Convex-Polygon-in-Java
 * https://www.codeproject.com/Members/John-Jiyang-Hou
 * https://www.codeproject.com/info/cpol10.aspx
 */
public class Cpol_GeoFace {
    
    // Vertices in one face of the 3D polygon
    private ArrayList<Cpol_GeoPoint> v;
    
    // Vertices Index
    private ArrayList<Integer> idx;

    // Number of vertices    
    private int n;        

    public ArrayList<Cpol_GeoPoint> getV() { return this.v; }

    public ArrayList<Integer> getI() { return this.idx; }
    
    public int getN() { return this.n; }

    public Cpol_GeoFace(){}
    
    public Cpol_GeoFace(ArrayList<Cpol_GeoPoint> p, ArrayList<Integer> idx)
    {            
        this.v = new ArrayList<Cpol_GeoPoint>();

        this.idx = new ArrayList<Integer>();

        this.n = p.size();
        
        for(int i=0;i<n;i++)
        {
            this.v.add(p.get(i));
            this.idx.add(idx.get(i));
        }        
    }   
}
