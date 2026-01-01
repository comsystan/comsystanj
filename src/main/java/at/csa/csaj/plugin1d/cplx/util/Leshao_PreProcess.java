/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Leshao_PreProcess.java
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
package at.csa.csaj.plugin1d.cplx.util;

/**
 * This is a class from
 * https://github.com/Leshao-Zhang/Visualisation-Cross-Recurrence-Quantification-Analysis
 */

public class Leshao_PreProcess{
  
  public static Leshao_LimitedQueue<Float> downSample(Leshao_LimitedQueue<Float> ts, int rate){
    Leshao_LimitedQueue<Float> out=new Leshao_LimitedQueue<Float>(ts.size()/rate);
    for(int i=0;i<ts.size();i+=rate){
      out.add(ts.get(i));
    }
    return out;
  }
  
  public static Leshao_LimitedQueue<Float> dimension(Leshao_LimitedQueue<float[]> ts, int dim){
    Leshao_LimitedQueue<Float> out=new Leshao_LimitedQueue<Float>(ts.size());
    for(int i=0;i<ts.size();i++){
      out.add(ts.get(i)[dim]);
    }
    return out;
  }
   
}
