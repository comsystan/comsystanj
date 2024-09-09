/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Util_GenerateInterval.java
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
import java.util.List;

/*
 * This class is a helper class to construct vectors of interval values
 */

public class CsajUtil_GenerateInterval {
	
	/*
	 * This method generates logarithmically distributed values in between an interval
	 * min included minimum value
	 * max included maximum value
	 * num number of values 
	 * 
	 * Note:
	 * If num is higher than the number of generated unique values, the interval is automatically restricted to a smaller number.
	 * In addition, the individual values may differ slightly from one another.
	 *  
	 */
	public static int[] getIntLogDistributedInterval(int min, int max, int num) {
		double start = Math.log(min);
		double end   = Math.log(max);
		int number = num;
		
		
		double[] linspaceArr = new double[number];
		double step = (end - start) / (number - 1);	
		for (int i = 0; i < number; i++) {
			linspaceArr[i] = start + i * step;
		}
        
		double[] expArr = new double[number];
        for (int i = 0; i < number; i++) {
            expArr[i] = Math.exp(linspaceArr[i]);
        }
       
        int[] roundedArr = new int[number];
        for (int i = 0; i < number; i++) {
            roundedArr[i] = (int) Math.round(expArr[i]);
        }
	  
        List<Integer> uniqueList = new ArrayList<>();
        for (int value : roundedArr) {
            if (!uniqueList.contains(value)) {
                uniqueList.add(value);
            }
        }
        
        int[] uniqueArr = new int[uniqueList.size()];
        for (int i = 0; i < uniqueList.size(); i++) {
            uniqueArr[i] = uniqueList.get(i);
        }
        
        //Do it again with the correct maximal number
        //Otherwise result values may slightly differ.
        if (uniqueArr.length < num) {
        	return getIntLogDistributedInterval(min, max, uniqueArr.length); 
        }
        
        return uniqueArr;
	}	
}
