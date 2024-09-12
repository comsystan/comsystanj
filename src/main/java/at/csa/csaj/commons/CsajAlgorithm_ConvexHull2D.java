/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajAlgorithm_ConvexHull2D.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 Comsystan Software
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.imglib2.Point;

import static java.util.Collections.emptyList;

/*
 * Rosetta Code
 * https://rosettacode.org/wiki/Convex_hull
 */
public class CsajAlgorithm_ConvexHull2D {
   
    public List<Point> convexHull(List<Point> p) {
        if (p.isEmpty()) return emptyList();
        
        //p.sort(Point::compareTo);
        Collections.sort(p, new Comparator<Point>() {
        	public int compare(Point o1, Point o2) {
        	    return Integer.compare(o1.getIntPosition(0), o2.getIntPosition(0));
        	}
        });
     
        List<Point> h = new ArrayList<>();
        // lower hull
        for (Point pt : p) {
            while (h.size() >= 2 && !ccw(h.get(h.size() - 2), h.get(h.size() - 1), pt)) {
                h.remove(h.size() - 1);
            }
            h.add(pt);
        }

        // upper hull
        int t = h.size() + 1;
        for (int i = p.size() - 1; i >= 0; i--) {
        	Point pt = p.get(i);
            while (h.size() >= t && !ccw(h.get(h.size() - 2), h.get(h.size() - 1), pt)) {
                h.remove(h.size() - 1);
            }
            h.add(pt);
        }

        h.remove(h.size() - 1);
        return h;
    }

    // ccw returns true if the three points make a counter-clockwise turn
    private static boolean ccw(Point a, Point b, Point c) {
       // return ((b.x - a.x) * (c.y - a.y)) > ((b.y - a.y) * (c.x - a.x));
       return ((b.getIntPosition(0) - a.getIntPosition(0)) * (c.getIntPosition(1) - a.getIntPosition(1))) > ((b.getIntPosition(1) - a.getIntPosition(1)) * (c.getIntPosition(0) - a.getIntPosition(0)));
    }

    public static void main(String[] args) {
//        List<Point> points = Arrays.asList(new Point(16, 3),
//                                           new Point(12, 17),
//                                           new Point(0, 6),
//                                           new Point(-4, -6),
//                                           new Point(16, 6),
//
//                                           new Point(16, -7),
//                                           new Point(16, -3),
//                                           new Point(17, -4),
//                                           new Point(5, 19),
//                                           new Point(19, -8),
//
//                                           new Point(3, 16),
//                                           new Point(12, 13),
//                                           new Point(3, -4),
//                                           new Point(17, 5),
//                                           new Point(-3, 15),
//
//                                           new Point(-3, -9),
//                                           new Point(0, 11),
//                                           new Point(-9, -3),
//                                           new Point(-4, -2),
//                                           new Point(12, 10));
//
//        ConvexHull2D convexHull2D = new ConvexHull2D();
//        List<Point> hull = convexHull2D.convexHull(points);
//        System.out.printf("Convex Hull: %s\n", hull);
    }
}
