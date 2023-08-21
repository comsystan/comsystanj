/*-
 * #%L
 * Project: ImageJ2 signal plugin for recurrence quantification analysis.
 * File: PhaseSpaceReconstruct.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2023 Comsystan Software
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
package at.csa.csaj.plugin1d.rqa.util.leshao;

/**
 * This is a class from
 * https://github.com/Leshao-Zhang/Visualisation-Cross-Recurrence-Quantification-Analysis
 */

public class PhaseSpaceReconstruct{
  
  public static LimitedQueue<float[]> go(LimitedQueue<Float> ts, int dim, int lag){
    LimitedQueue<float[]> reconstruct = new LimitedQueue<float[]>(ts.size()-dim*lag);
    for(int t=0;t+dim*lag<ts.size();t++){
      float[] point=new float[dim];
      for(int i=0;i<dim;i++){
        point[i]=ts.get(t+i*lag);      
      }
      reconstruct.add(point);
    }
    return reconstruct;
  }
  
}
