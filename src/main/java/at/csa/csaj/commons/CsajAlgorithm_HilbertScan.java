/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complexity analyses of 1D signals, 2D images and 3D volumes
 * File: CsajAlgorithm_HilbertScan.java
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

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

/**
 * This class transforms a 2D image or 3D volume in a 1D sequence following the path of a Hilbert curve
 * <li> Generalized Hilbert ('gilbert') space-filling curve for arbitrary-sized 2D or 3D rectangular grids.
 * <li> Generates discrete 2D or 3D coordinates to fill  size (width x height) or (width x height x depth).
 * <li> Arouxet MaB, Bariviera AF, Hansen R, Pastor VE. A compact information-theoretic framework for texture classification: Hilbert curves, amplitude-aware permutation entropy, and explainability. Chaos. 3. August 2026;36(8):083102. doi:10.1063/5.0341167
 * <li> https://github.com/jakubcerveny/gilbert J. ˇCervený, “Gilbert: Space-filling curve for rectangular domains or arbitrary size” (2025)
 * <li> The original code in JavaScript is Copyright (c) 2018, Jakub Červený
 * <li> Converted to Java and adapted for ComsystanJ by Helmut Ahammer
 *
 * @author Helmut Ahammer
 * @since  2028 09
 */
public class CsajAlgorithm_HilbertScan {
	
	Cursor<?> cursor;
	
	private RandomAccessibleInterval<?> rai; 
	public RandomAccessibleInterval<?> getRai() {
		return rai;
	}
	public void setRai(RandomAccessibleInterval<?> rai) {
		this.rai = rai;
	}
	
	private int numDim; //2 or 3	
	public int getNumDim() {
		return numDim;
	}
	public void setNumDim(int numDim) {
		this.numDim = numDim;
	}
	
	private int width;
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	
	private int height; 
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	
	private int depth;
	public int getDepth() {
		return depth;
	}
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	
	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_HilbertScan() {
		
	}
	
	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_HilbertScan(RandomAccessibleInterval<?> rai) {	//2D or 3D rai
		this.rai = rai; 
		numDim  = rai.numDimensions();
		if (numDim == 2) { //2D grey			
			width  = (int) rai.dimension(0);
			height = (int) rai.dimension(1);
		}
		if (numDim == 3) { //3D grey			
			width  = (int) rai.dimension(0);
			height = (int) rai.dimension(1);
			depth = (int) rai.dimension(2);			
		}
	}
	
	//This methods scans through all pixels and creates an 1D sequence
	public double[] getHilbertScan() {
		
		//Initialize 1D sequence
		double[] hilbertScan = null;
		double pixelVal;
		long[] pos;
		int idx;
		
		if (numDim == 2) { //2D grey	
			hilbertScan = new double[width*height];
			for (int n=0; n<hilbertScan.length; n++) hilbertScan[n] = Double.NaN;
			
			cursor = rai.localizingCursor();
			pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos); 
				pixelVal = ((UnsignedByteType) cursor.get()).get();
				idx = gilbertXy2d((int)pos[0], (int)pos[1], width, height);
				hilbertScan[idx] = pixelVal;
			}		
		}
		if (numDim == 3) { //3D grey			
			hilbertScan = new double[width*height*depth];
			for (int n=0; n<hilbertScan.length; n++) hilbertScan[n] = Double.NaN;
			
			cursor = rai.localizingCursor();
			pos = new long[3];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos); 
				pixelVal = ((UnsignedByteType) cursor.get()).get();
				idx = gilbertXyz2d((int)pos[0], (int)pos[1], (int)pos[2], width, height, depth);
				hilbertScan[idx] = pixelVal;
			}		
		}
		
		return hilbertScan;
	}
	
    public class Point2 {
        public int x, y;

        public Point2(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Point2() {
            this.x = 0;
            this.y = 0;
        }
    }

    public class Point3 {
        public int x, y, z;

        public Point3(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Point3() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }
    }

    public int sgn(int x) {
        if (x < 0) { return -1; }
        if (x > 0) { return  1; }
        return 0;
    }

    public boolean inBounds2(Point2 p, Point2 s, Point2 a, Point2 b) {
        Point2 d = new Point2(a.x + b.x, a.y + b.y);

        if (d.x < 0) {
            if ((p.x > s.x) || (p.x <= (s.x + d.x))) { return false; }
        }
        else if ((p.x < s.x) || (p.x >= (s.x + d.x))) { return false; }

        if (d.y < 0) {
            if ((p.y > s.y) || (p.y <= (s.y + d.y))) { return false; }
        }
        else if ((p.y < s.y) || (p.y >= (s.y + d.y))) { return false; }

        return true;
    }

    public boolean inBounds3(Point3 p, Point3 s, Point3 a, Point3 b, Point3 c) {
        Point3 d = new Point3(a.x + b.x + c.x, a.y + b.y + c.y, a.z + b.z + c.z);

        if (d.x < 0) {
            if ((p.x > s.x) || (p.x <= (s.x + d.x))) { return false; }
        }
        else if ((p.x < s.x) || (p.x >= (s.x + d.x))) { return false; }

        if (d.y < 0) {
            if ((p.y > s.y) || (p.y <= (s.y + d.y))) { return false; }
        }
        else if ((p.y < s.y) || (p.y >= (s.y + d.y))) { return false; }

        if (d.z < 0) {
            if ((p.z > s.z) || (p.z <= (s.z + d.z))) { return false; }
        }
        else if ((p.z < s.z) || (p.z >= (s.z + d.z))) { return false; }

        return true;
    }

    public int gilbertXy2d(int x, int y, int w, int h) {
        Point2 _q = new Point2(x, y);
        Point2 _p = new Point2(0, 0);
        Point2 _a = new Point2(0, h);
        Point2 _b = new Point2(w, 0);

        if (w >= h) {
            _a.x = w; _a.y = 0;
            _b.x = 0; _b.y = h;
        }
        return gilbertXy2dR(0, _q, _p, _a, _b);
    }

    public Point2 gilbertD2xy(int idx, int w, int h) {
        Point2 _p = new Point2(0, 0);
        Point2 _a = new Point2(0, h);
        Point2 _b = new Point2(w, 0);

        if (w >= h) {
            _a.x = w; _a.y = 0;
            _b.x = 0; _b.y = h;
        }
        return gilbertD2xyR(idx, 0, _p, _a, _b);
    }

    public int gilbertXyz2d(int x, int y, int z, int w, int h, int d) {
        Point3 _q = new Point3(x, y, z);
        Point3 _p = new Point3(0, 0, 0);
        Point3 _a = new Point3(w, 0, 0);
        Point3 _b = new Point3(0, h, 0);
        Point3 _c = new Point3(0, 0, d);

        if ((w >= h) && (w >= d)) {
            return gilbertXyz2dR(0, _q, _p, _a, _b, _c);
        }
        else if ((h >= w) && (h >= d)) {
            return gilbertXyz2dR(0, _q, _p, _b, _a, _c);
        }
        return gilbertXyz2dR(0, _q, _p, _c, _a, _b);
    }

    public Point3 gilbertD2xyz(int idx, int w, int h, int d) {
        Point3 _p = new Point3(0, 0, 0);
        Point3 _a = new Point3(w, 0, 0);
        Point3 _b = new Point3(0, h, 0);
        Point3 _c = new Point3(0, 0, d);
        if ((w >= h) && (w >= d)) {
            return gilbertD2xyzR(idx, 0, _p, _a, _b, _c);
        }
        else if ((h >= w) && (h >= d)) {
            return gilbertD2xyzR(idx, 0, _p, _b, _a, _c);
        }
        return gilbertD2xyzR(idx, 0, _p, _c, _a, _b);
    }

//    // Placeholders for recursive implementation counterparts if needed
//    private int gilbertXy2dR(int idx, Point2 q, Point2 p, Point2 a, Point2 b) {
//        return 0;
//    }
//
//    private Point2 gilbertD2xyR(int idx, int d, Point2 p, Point2 a, Point2 b) {
//        return new Point2();
//    }
//
//    private int gilbertXyz2dR(int idx, Point3 q, Point3 p, Point3 a, Point3 b, Point3 c) {
//        return 0;
//    }
//
//    private Point3 gilbertD2xyzR(int idx, int d, Point3 p, Point3 a, Point3 b, Point3 c) {
//        return new Point3();
//    }

    public Point2 gilbertD2xyR(int dst_idx, int cur_idx, Point2 p, Point2 a, Point2 b) {
        Point2 _p, _a, _b;
        int nxt_idx = -1;

        int w = Math.abs(a.x + a.y);
        int h = Math.abs(b.x + b.y);

        Point2 da = new Point2(sgn(a.x), sgn(a.y));
        Point2 db = new Point2(sgn(b.x), sgn(b.y));
        Point2 d = new Point2(da.x + db.x, da.y + db.y);
        int d_i = dst_idx - cur_idx;

        if (h == 1) {
            return new Point2(p.x + da.x * d_i, p.y + da.y * d_i);
        }
        if (w == 1) {
            return new Point2(p.x + db.x * d_i, p.y + db.y * d_i);
        }

        Point2 a2 = new Point2(a.x / 2, a.y / 2);
        Point2 b2 = new Point2(b.x / 2, b.y / 2);

        int w2 = Math.abs(a2.x + a2.y);
        int h2 = Math.abs(b2.x + b2.y);

        if ((2 * w) > (3 * h)) {
            // prefer even steps
            if ((w2 % 2 != 0) && (w > 2)) {
                a2.x += da.x;
                a2.y += da.y;
            }

            nxt_idx = cur_idx + Math.abs((a2.x + a2.y) * (b.x + b.y));
            if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
                return gilbertD2xyR(dst_idx, cur_idx, p, a2, b);
            }
            cur_idx = nxt_idx;

            _p = new Point2(p.x + a2.x, p.y + a2.y);
            _a = new Point2(a.x - a2.x, a.y - a2.y);

            return gilbertD2xyR(dst_idx, cur_idx, _p, _a, b);
        }

        // prefer even steps
        if ((h2 % 2 != 0) && (h > 2)) {
            b2.x += db.x;
            b2.y += db.y;
        }

        // standard case: one step up, on int horizontal, one step down
        nxt_idx = cur_idx + Math.abs((b2.x + b2.y) * (a2.x + a2.y));
        if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
            return gilbertD2xyR(dst_idx, cur_idx, p, b2, a2);
        }
        cur_idx = nxt_idx;

        nxt_idx = cur_idx + Math.abs((a.x + a.y) * ((b.x - b2.x) + (b.y - b2.y)));
        if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
            _p = new Point2(p.x + b2.x, p.y + b2.y);
            _b = new Point2(b.x - b2.x, b.y - b2.y);
            return gilbertD2xyR(dst_idx, cur_idx, _p, a, _b);
        }
        cur_idx = nxt_idx;

        _p = new Point2(
                p.x + (a.x - da.x) + (b2.x - db.x),
                p.y + (a.y - da.y) + (b2.y - db.y)
        );
        _a = new Point2(-b2.x, -b2.y);
        _b = new Point2(-(a.x - a2.x), -(a.y - a2.y));

        return gilbertD2xyR(dst_idx, cur_idx, _p, _a, _b);
    }

    public int gilbertXy2dR(int idx, Point2 q, Point2 p, Point2 a, Point2 b) {
        Point2 _p, _a, _b;

        int w = Math.abs(a.x + a.y);
        int h = Math.abs(b.x + b.y);

        Point2 da = new Point2(sgn(a.x), sgn(a.y));
        Point2 db = new Point2(sgn(b.x), sgn(b.y));

        if (h == 1) {
            return idx + (da.x * (q.x - p.x)) + (da.y * (q.y - p.y));
        }
        if (w == 1) {
            return idx + (db.x * (q.x - p.x)) + (db.y * (q.y - p.y));
        }

        Point2 a2 = new Point2(a.x / 2, a.y / 2);
        Point2 b2 = new Point2(b.x / 2, b.y / 2);

        int w2 = Math.abs(a2.x + a2.y);
        int h2 = Math.abs(b2.x + b2.y);

        if ((2 * w) > (3 * h)) {
            if ((w2 % 2 != 0) && (w > 2)) {
                a2.x += da.x;
                a2.y += da.y;
            }

            if (inBounds2(q, p, a2, b)) {
                return gilbertXy2dR(idx, q, p, a2, b);
            }
            idx += Math.abs((a2.x + a2.y) * (b.x + b.y));

            _p = new Point2(p.x + a2.x, p.y + a2.y);
            _a = new Point2(a.x - a2.x, a.y - a2.y);
            return gilbertXy2dR(idx, q, _p, _a, b);
        }

        if ((h2 % 2 != 0) && (h > 2)) {
            b2.x += db.x;
            b2.y += db.y;
        }

        if (inBounds2(q, p, b2, a2)) {
            return gilbertXy2dR(idx, q, p, b2, a2);
        }
        
        idx += Math.abs((b2.x + b2.y) * (a2.x + a2.y));
        _p = new Point2(p.x + b2.x, p.y + b2.y);
        _b = new Point2(b.x - b2.x, b.y - b2.y);
        
        if (inBounds2(q, _p, a, _b)) {
            return gilbertXy2dR(idx, q, _p, a, _b);
        }
        
        idx += Math.abs((a.x + a.y) * ((b.x - b2.x) + (b.y - b2.y)));
        _p = new Point2(
                p.x + (a.x - da.x) + (b2.x - db.x),
                p.y + (a.y - da.y) + (b2.y - db.y)
        );
        
        _a = new Point2(-b2.x, -b2.y);
        _b = new Point2(-(a.x - a2.x), -(a.y - a2.y));
        return gilbertXy2dR(idx, q, _p, _a, _b);
    }


    public int gilbertXyz2dR(int cur_idx, Point3 q, Point3 p, Point3 a, Point3 b, Point3 c) {
        Point3 _p, _a, _b, _c;

        int w = Math.abs(a.x + a.y + a.z);
        int h = Math.abs(b.x + b.y + b.z);
        int d = Math.abs(c.x + c.y + c.z);

        Point3 da = new Point3(sgn(a.x), sgn(a.y), sgn(a.z));
        Point3 db = new Point3(sgn(b.x), sgn(b.y), sgn(b.z));
        Point3 dc = new Point3(sgn(c.x), sgn(c.y), sgn(c.z));

        // trivial row/column fills
        if ((h == 1) && (d == 1)) {
            return cur_idx + (int)(da.x * (q.x - p.x)) + (int)(da.y * (q.y - p.y)) + (int)(da.z * (q.z - p.z));
        } else if ((w == 1) && (d == 1)) {
            return cur_idx + (int)(db.x * (q.x - p.x)) + (int)(db.y * (q.y - p.y)) + (int)(db.z * (q.z - p.z));
        } else if ((w == 1) && (h == 1)) {
            return cur_idx + (int)(dc.x * (q.x - p.x)) + (int)(dc.y * (q.y - p.y)) + (int)(dc.z * (q.z - p.z));
        }

        Point3 a2 = new Point3(a.x / 2, a.y / 2, a.z / 2);
        Point3 b2 = new Point3(b.x / 2, b.y / 2, b.z / 2);
        Point3 c2 = new Point3(c.x / 2, c.y / 2, c.z / 2);

        int w2 = Math.abs(a2.x + a2.y + a2.z);
        int h2 = Math.abs(b2.x + b2.y + b2.z);
        int d2 = Math.abs(c2.x + c2.y + c2.z);

        // prefer even steps
        if ((w2 % 2 != 0) && (w > 2)) {
            a2.x += da.x;
            a2.y += da.y;
            a2.z += da.z;
        }

        if ((h2 % 2 != 0) && (h > 2)) {
            b2.x += db.x;
            b2.y += db.y;
            b2.z += db.z;
        }

        if ((d2 % 2 != 0) && (d > 2)) {
            c2.x += dc.x;
            c2.y += dc.y;
            c2.z += dc.z;
        }

        // wide case, split in w only
        if (((2 * w) > (3 * h)) && ((2 * w) > (3 * d))) {
            if (inBounds3(q, p, a2, b, c)) {
                return gilbertXyz2dR(cur_idx, q, p, a2, b, c);
            }
            cur_idx += (int) Math.abs((a2.x + a2.y + a2.z) * (b.x + b.y + b.z) * (c.x + c.y + c.z));

            _p = new Point3(p.x + a2.x, p.y + a2.y, p.z + a2.z);
            _a = new Point3(a.x - a2.x, a.y - a2.y, a.z - a2.z);
            return gilbertXyz2dR(cur_idx, q, _p, _a, b, c);
        } else if ((3 * h) > (4 * d)) {
            if (inBounds3(q, p, b2, c, a2)) {
                return gilbertXyz2dR(cur_idx, q, p, b2, c, a2);
            }
            cur_idx += (int) Math.abs((b2.x + b2.y + b2.z) * (c.x + c.y + c.z) * (a2.x + a2.y + a2.z));

            _p = new Point3(p.x + b2.x, p.y + b2.y, p.z + b2.z);
            _b = new Point3(b.x - b2.x, b.y - b2.y, b.z - b2.z);
            if (inBounds3(q, _p, a, _b, c)) {
                return gilbertXyz2dR(cur_idx, q, _p, a, _b, c);
            }
            cur_idx += (int) Math.abs((a.x + a.y + a.z) * ((b.x - b2.x) + (b.y - b2.y) + (b.z - b2.z)) * (c.x + c.y + c.z));

            _p = new Point3(
                p.x + (a.x - da.x) + (b2.x - db.x),
                p.y + (a.y - da.y) + (b2.y - db.y),
                p.z + (a.z - da.z) + (b2.z - db.z)
            );
            _a = new Point3(-b2.x, -b2.y, -b2.z);
            _c = new Point3(-(a.x - a2.x), -(a.y - a2.y), -(a.z - a2.z));
            return gilbertXyz2dR(cur_idx, q, _p, _a, c, _c);
        } else if ((3 * d) > (4 * h)) {
            if (inBounds3(q, p, c2, a2, b)) {
                return gilbertXyz2dR(cur_idx, q, p, c2, a2, b);
            }
            cur_idx += (int) Math.abs((c2.x + c2.y + c2.z) * (a2.x + a2.y + a2.z) * (b.x + b.y + b.z));

            _p = new Point3(p.x + c2.x, p.y + c2.y, p.z + c2.z);
            _c = new Point3(c.x - c2.x, c.y - c2.y, c.z - c2.z);
            if (inBounds3(q, _p, a, b, _c)) {
                return gilbertXyz2dR(cur_idx, q, _p, a, b, _c);
            }
            cur_idx += (int) Math.abs((a.x + a.y + a.z) * (b.x + b.y + b.z) * ((c.x - c2.x) + (c.y - c2.y) + (c.z - c2.z)));

            _p = new Point3(
                p.x + (a.x - da.x) + (c2.x - dc.x),
                p.y + (a.y - da.y) + (c2.y - dc.y),
                p.z + (a.z - da.z) + (c2.z - dc.z)
            );
            _a = new Point3(-c2.x, -c2.y, -c2.z);
            _b = new Point3(-(a.x - a2.x), -(a.y - a2.y), -(a.z - a2.z));
            return gilbertXyz2dR(cur_idx, q, _p, _a, _b, b);
        }

		// regular case, split in all w/h/d
        if (inBounds3(q, p, b2, c2, a2)) {
            return gilbertXyz2dR(cur_idx, q, p, b2, c2, a2);
        }
        cur_idx += Math.abs((b2.x + b2.y + b2.z) * (c2.x + c2.y + c2.z) * (a2.x + a2.y + a2.z));

        _p = new Point3(p.x + b2.x, p.y + b2.y, p.z + b2.z);
        _c = new Point3(b.x - b2.x, b.y - b2.y, b.z - b2.z);
        if (inBounds3(q, _p, c, a2, _c)) {
            return gilbertXyz2dR(cur_idx, q, _p, c, a2, _c);
        }
        cur_idx += Math.abs((c.x + c.y + c.z) * (a2.x + a2.y + a2.z) * ((b.x - b2.x) + (b.y - b2.y) + (b.z - b2.z)));

        _p = new Point3(
            p.x + (b2.x - db.x) + (c.x - dc.x),
            p.y + (b2.y - db.y) + (c.y - dc.y),
            p.z + (b2.z - db.z) + (c.z - dc.z)
        );
        _b = new Point3(-b2.x, -b2.y, -b2.z);
        _c = new Point3(-(c.x - c2.x), -(c.y - c2.y), -(c.z - c2.z));
        if (inBounds3(q, _p, a, _b, _c)) {
            return gilbertXyz2dR(cur_idx, q, _p, a, _b, _c);
        }
        cur_idx += Math.abs((a.x + a.y + a.z) * (-b2.x - b2.y - b2.z) * (-(c.x - c2.x) - (c.y - c2.y) - (c.z - c2.z)));

        _p = new Point3(
            p.x + (a.x - da.x) + b2.x + (c.x - dc.x),
            p.y + (a.y - da.y) + b2.y + (c.y - dc.y),
            p.z + (a.z - da.z) + b2.z + (c.z - dc.z)
        );
        _a = new Point3(-c.x, -c.y, -c.z);
        _b = new Point3(-(a.x - a2.x), -(a.y - a2.y), -(a.z - a2.z));
        _c = new Point3(b.x - b2.x, b.y - b2.y, b.z - b2.z);
        if (inBounds3(q, _p, _a, _b, _c)) {
            return gilbertXyz2dR(cur_idx, q, _p, _a, _b, _c);
        }
        cur_idx += Math.abs((-c.x - c.y - c.z) * (-(a.x - a2.x) - (a.y - a2.y) - (a.z - a2.z)) * ((b.x - b2.x) + (b.y - b2.y) + (b.z - b2.z)));

        _p = new Point3(
            p.x + (a.x - da.x) + (b2.x - db.x),
            p.y + (a.y - da.y) + (b2.y - db.y),
            p.z + (a.z - da.z) + (b2.z - db.z)
        );
        _a = new Point3(-b2.x, -b2.y, -b2.z);
        _c = new Point3(-(a.x - a2.x), -(a.y - a2.y), -(a.z - a2.z));
        return gilbertXyz2dR(cur_idx, q, _p, _a, c2, _c);
   
    }

 public Point3 gilbertD2xyzR(int dst_idx, int cur_idx, Point3 p, Point3 a, Point3 b, Point3 c) {
        int nxt_idx = -1;

        int w = Math.abs(a.x + a.y + a.z);
        int h = Math.abs(b.x + b.y + b.z);
        int d = Math.abs(c.x + c.y + c.z);

        Point3 da = new Point3(sgn(a.x), sgn(a.y), sgn(a.z));
        Point3 db = new Point3(sgn(b.x), sgn(b.y), sgn(b.z));
        Point3 dc = new Point3(sgn(c.x), sgn(c.y), sgn(c.z));
        int di = dst_idx - cur_idx;

        // trivial row/column fills
        if ((h == 1) && (d == 1)) {
            return new Point3(p.x + da.x * di, p.y + da.y * di, p.z + da.z * di);
        } else if ((w == 1) && (d == 1)) {
            return new Point3(p.x + db.x * di, p.y + db.y * di, p.z + db.z * di);
        } else if ((w == 1) && (h == 1)) {
            return new Point3(p.x + dc.x * di, p.y + dc.y * di, p.z + dc.z * di);
        }

        Point3 a2 = new Point3(a.x / 2, a.y / 2, a.z / 2);
        Point3 b2 = new Point3(b.x / 2, b.y / 2, b.z / 2);
        Point3 c2 = new Point3(c.x / 2, c.y / 2, c.z / 2);

        int w2 = Math.abs(a2.x + a2.y + a2.z);
        int h2 = Math.abs(b2.x + b2.y + b2.z);
        int d2 = Math.abs(c2.x + c2.y + c2.z);

        // prefer even steps
        if ((w2 % 2 != 0) && (w > 2)) {
            a2.x += da.x;
            a2.y += da.y;
            a2.z += da.z;
        }
        if ((h2 % 2 != 0) && (h > 2)) {
            b2.x += db.x;
            b2.y += db.y;
            b2.z += db.z;
        }
        if ((d2 % 2 != 0) && (d > 2)) {
            c2.x += dc.x;
            c2.y += dc.y;
            c2.z += dc.z;
        }

        // wide case, split in w only
        if (((2 * w) > (3 * h)) && ((2 * w) > (3 * d))) {
            nxt_idx = cur_idx + Math.abs((a2.x + a2.y + a2.z) * (b.x + b.y + b.z) * (c.x + c.y + c.z));
            if ((cur_idx <= nxt_idx) && (dst_idx < nxt_idx)) {
                return gilbertD2xyzR(dst_idx, cur_idx, p, a2, b, c);
            }
            cur_idx = nxt_idx;

            Point3 _p = new Point3(p.x + a2.x, p.y + a2.y, p.z + a2.z);
            Point3 _a = new Point3(a.x - a2.x, a.y - a2.y, a.z - a2.z);
            return gilbertD2xyzR(dst_idx, cur_idx, _p, _a, b, c);
        } else if ((3 * h) > (4 * d)) {
            nxt_idx = cur_idx + Math.abs((b2.x + b2.y + b2.z) * (c.x + c.y + c.z) * (a2.x + a2.y + a2.z));
            if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
                return gilbertD2xyzR(dst_idx, cur_idx, p, b2, c, a2);
            }
            cur_idx = nxt_idx;

            nxt_idx = cur_idx + Math.abs((a.x + a.y + a.z) * ((b.x - b2.x) + (b.y - b2.y) + (b.z - b2.z)) * (c.x + c.y + c.z));
            Point3 _p = new Point3(p.x + b2.x, p.y + b2.y, p.z + b2.z);
            Point3 _b = new Point3(b.x - b2.x, b.y - b2.y, b.z - b2.z);
            if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
                return gilbertD2xyzR(dst_idx, cur_idx, _p, a, _b, c);
            }
            cur_idx = nxt_idx;

            _p = new Point3(
                p.x + (a.x - da.x) + (b2.x - db.x),
                p.y + (a.y - da.y) + (b2.y - db.y),
                p.z + (a.z - da.z) + (b2.z - db.z)
            );
            Point3 _a = new Point3(-b2.x, -b2.y, -b2.z);
            Point3 _c = new Point3(-(a.x - a2.x), -(a.y - a2.y), -(a.z - a2.z));
            return gilbertD2xyzR(dst_idx, cur_idx, _p, _a, c, _c);
        } else if ((3 * d) > (4 * h)) {
            nxt_idx = cur_idx + Math.abs((c2.x + c2.y + c2.z) * (a2.x + a2.y + a2.z) * (b.x + b.y + b.z));
            if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
                return gilbertD2xyzR(dst_idx, cur_idx, p, c2, a2, b);
            }
            cur_idx = nxt_idx;

            nxt_idx = cur_idx + Math.abs((a.x + a.y + a.z) * (b.x + b.y + b.z) * ((c.x - c2.x) + (c.y - c2.y) + (c.z - c2.z)));
            Point3 _p = new Point3(p.x + c2.x, p.y + c2.y, p.z + c2.z);
            Point3 _c = new Point3(c.x - c2.x, c.y - c2.y, c.z - c2.z);
            if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
                return gilbertD2xyzR(dst_idx, cur_idx, _p, a, b, _c);
            }
            cur_idx = nxt_idx;
            _p = new Point3(
                p.x + (a.x - da.x) + (c2.x - dc.x),
                p.y + (a.y - da.y) + (c2.y - dc.y),
                p.z + (a.z - da.z) + (c2.z - dc.z)
            );
            Point3 _a = new Point3(-c2.x, -c2.y, -c2.z);
            Point3 _b = new Point3(-(a.x - a2.x), -(a.y - a2.y), -(a.z - a2.z));
            return gilbertD2xyzR(dst_idx, cur_idx, _p, _a, _b, b);
        }

		// regular case, split in all w/h/d	   
		nxt_idx = cur_idx + Math.abs((int) (b2.x + b2.y + b2.z) * (c2.x + c2.y + c2.z) * (a2.x + a2.y + a2.z));
		if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
			return gilbertD2xyzR(dst_idx, cur_idx, p, b2, c2, a2);
		}
		cur_idx = nxt_idx;

		nxt_idx = cur_idx + Math.abs((int) (c.x + c.y + c.z) * (a2.x + a2.y + a2.z) * ((b.x - b2.x) + (b.y - b2.y) + (b.z - b2.z)));
		Point3 _p = new Point3(p.x + b2.x, p.y + b2.y, p.z + b2.z);
		Point3 _c = new Point3(b.x - b2.x, b.y - b2.y, b.z - b2.z);
		if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
			return gilbertD2xyzR(dst_idx, cur_idx, _p, c, a2, _c);
		}
		cur_idx = nxt_idx;

		nxt_idx = cur_idx + Math.abs((int) (c.x + c.y + c.z) * (-b2.x - b2.y - b2.z) * (-(c.x - c2.x) - (c.y - c2.y) - (c.z - c2.z))); // Note: JS code had (-c.x - c.y - c.z)
		nxt_idx = cur_idx + Math.abs((int) (-c.x - c.y - c.z) * (-b2.x - b2.y - b2.z) * (-(c.x - c2.x) - (c.y - c2.y) - (c.z - c2.z)));
		_p = new Point3(
			p.x + (b2.x - db.x) + (c.x - dc.x),
			p.y + (b2.y - db.y) + (c.y - dc.y),
			p.z + (b2.z - db.z) + (c.z - dc.z)
		);
		Point3 _b = new Point3(-b2.x, -b2.y, -b2.z);
		_c = new Point3(-(c.x - c2.x), -(c.y - c2.y), -(c.z - c2.z));
		if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
			return gilbertD2xyzR(dst_idx, cur_idx, _p, a, _b, _c);
		}
		cur_idx = nxt_idx;

		nxt_idx = cur_idx + Math.abs((int) (-c.x - c.y - c.z) * (-(a.x - a2.x) - (a.y - a2.y) - (a.z - a2.z)) * ((b.x - b2.x) + (b.y - b2.y) + (b.z - b2.z)));
		_p = new Point3(
			p.x + (a.x - da.x) + b2.x + (c.x - dc.x),
			p.y + (a.y - da.y) + b2.y + (c.y - dc.y),
			p.z + (a.z - da.z) + b2.z + (c.z - dc.z)
		);
		Point3 _a = new Point3(-c.x, -c.y, -c.z);
		_b = new Point3(-(a.x - a2.x), -(a.y - a2.y), -(a.z - a2.z));
		_c = new Point3(b.x - b2.x, b.y - b2.y, b.z - b2.z);
		if ((cur_idx <= dst_idx) && (dst_idx < nxt_idx)) {
			return gilbertD2xyzR(dst_idx, cur_idx, _p, _a, _b, _c);
		}
		cur_idx = nxt_idx;

		_p = new Point3(
			p.x + (a.x - da.x) + (b2.x - db.x),
			p.y + (a.y - da.y) + (b2.y - db.y),
			p.z + (a.z - da.z) + (b2.z - db.z)
		);
		_a = new Point3(-b2.x, -b2.y, -b2.z);
		_c = new Point3(-(a.x - a2.x), -(a.y - a2.y), -(a.z - a2.z));
		return gilbertD2xyzR(dst_idx, cur_idx, _p, _a, c2, _c);
	   
    }

  public static void _main(String[] argv) {
        if (argv.length < 3) {
            System.out.println("provide args");
            System.exit(-1);
        }

        String op = argv[0];
        int w = Integer.parseInt(argv[1]);
        int h = Integer.parseInt(argv[2]);
        int d = 1;
        if (argv.length > 3) {
            d = Integer.parseInt(argv[3]);
        }

        if (op.equals("xy2d")) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                	CsajAlgorithm_HilbertScan hs = new CsajAlgorithm_HilbertScan();
                    int idx = hs.gilbertXy2d(x, y, w, h);
                    System.out.println(idx + " " + x + " " + y);
                }
            }
        } else if (op.equals("d2xy")) {
            int n = w * h;
            for (int idx = 0; idx < n; idx++) {
            	CsajAlgorithm_HilbertScan hs = new CsajAlgorithm_HilbertScan();
                Point2 xy = hs.gilbertD2xy(idx, w, h);
                System.out.println(xy.x + " " + xy.y);
            }
        } else if (op.equals("xyz2d")) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) {
                    	CsajAlgorithm_HilbertScan hs = new CsajAlgorithm_HilbertScan();
                        int idx = hs.gilbertXyz2d(x, y, z, w, h, d);
                        System.out.println(idx + " " + x + " " + y + " " + z);
                    }
                }
            }
        } else if (op.equals("d2xyz")) {
            int n = w * h * d;
            for (int idx = 0; idx < n; idx++) {
            	CsajAlgorithm_HilbertScan hs = new CsajAlgorithm_HilbertScan();
                Point3 xyz = hs.gilbertD2xyz(idx, w, h, d);
                System.out.println(xyz.x + " " + xyz.y + " " + xyz.z);
            }
        }
    }

	
	
	

}
