/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Osea4Java_PostClassifier.java
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
/*
 * Copyright (c) 2012 Patrick S. Hamilton (pat@eplimited.com), Wolfgang Halbeisen (halbeisen.wolfgang@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies 
 * or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package at.csa.csaj.plugin1d.detect.util;

import static at.csa.csaj.plugin1d.detect.util.Osea4Java_ECGCODES.NORMAL;
import static at.csa.csaj.plugin1d.detect.util.Osea4Java_ECGCODES.PVC;
import static at.csa.csaj.plugin1d.detect.util.Osea4Java_ECGCODES.UNKNOWN;

/**
 * This file contains functions for classifying beats based after the
 * following beat is detected.
 */
public class Osea4Java_PostClassifier 
	{
	
	public int[][] PostClass ;
	public int[][] PCRhythm ;
	
	private Osea4Java_BDACParameters bdacParas ;
	private Osea4Java_Matcher        matcher ;

	private int    PCInitCount = 0 ;
	private int    PostClassify_lastRC ;
	private double PostClassify_lastMI2 ;
	
	/**
	 * Create a new post classifier with the given parameters.
	 * @param bdacParameters The sampleRate-dependent parameters
	 * @param mat The Matcher
	 */
	public Osea4Java_PostClassifier(Osea4Java_BDACParameters bdacParameters) 
		{
		bdacParas = bdacParameters;
		PostClass = new int[bdacParameters.MAXTYPES][8] ;
		PCRhythm  = new int[bdacParameters.MAXTYPES][8] ;
		}
	
	/**
	 * Injects the object.
	 * 
	 * @param matcher The matcher
	 */
	public void setObjects(Osea4Java_Matcher matcher)
		{
		this.matcher = matcher ;
		}

	/**
	 * Classify the previous beat type and rhythm type based on this beat
	 * and the preceding beat.  This classifier is more sensitive
	 * to detecting premature beats followed by compensitory pauses.
	 * 
	 * @param recentTypes
	 * @param domType
	 * @param recentRRs
	 * @param width
	 * @param mi2
	 * @param rhythmClass
	 */
	public void PostClassify(int[] recentTypes, int domType, int[] recentRRs, int width, double mi2,
		int rhythmClass)
		{
		int i, regCount, pvcCount, normRR ;
		double mi3 ;

		// If the preceeding and following beats are the same type,
		// they are generally regular, and reasonably close in shape
		// to the dominant type, consider them to be dominant.

		if((recentTypes[0] == recentTypes[2]) && (recentTypes[0] != domType)
			&& (recentTypes[0] != recentTypes[1]))
			{
			mi3 = matcher.DomCompare(recentTypes[0],domType) ;
			for(i = regCount = 0; i < 8; ++i)
				if(PCRhythm[Math.min(recentTypes[0], bdacParas.MAXTYPES-1)][i] == NORMAL)
					++regCount ;
			if((mi3 < 2.0) && (regCount > 6))
				domType = recentTypes[0] ;
			}

		// Don't do anything until four beats have gone by.

		if(PCInitCount < 3)
			{
			++PCInitCount ;
			PostClassify_lastMI2 = 0 ;
			PostClassify_lastRC = 0 ;
			return ;
			}

		if(recentTypes[1] < bdacParas.MAXTYPES)
			{

			// Find first NN interval.
			for(i = 2; (i < 7) && (recentTypes[i] != recentTypes[i+1]); ++i) ;
			if(i == 7) normRR = 0 ;
			else normRR = recentRRs[i] ;

			// Shift the previous beat classifications to make room for the
			// new classification.
			for(i = pvcCount = 0; i < 8; ++i)
				if(PostClass[recentTypes[1]][i] == PVC)
					++pvcCount ;

			for(i = 7; i > 0; --i)
				{
				PostClass[recentTypes[1]][i] = PostClass[recentTypes[1]][i-1] ;
				PCRhythm[recentTypes[1]][i] = PCRhythm[recentTypes[1]][i-1] ;
				}

			// If the beat is premature followed by a compensitory pause and the
			// previous and following beats are normal, post classify as
			// a PVC.

			if(((normRR-(normRR>>3)) >= recentRRs[1]) && ((recentRRs[0]-(recentRRs[0]>>3)) >= normRR)// && (lastMI2 > 3)
				&& (recentTypes[0] == domType) && (recentTypes[2] == domType)
					&& (recentTypes[1] != domType))
				PostClass[recentTypes[1]][0] = PVC ;

			// If previous two were classified as PVCs, and this is at least slightly
			// premature, classify as a PVC.

			else if(((normRR-(normRR>>4)) > recentRRs[1]) && ((normRR+(normRR>>4)) < recentRRs[0]) &&
				(((PostClass[recentTypes[1]][1] == PVC) && (PostClass[recentTypes[1]][2] == PVC)) ||
					(pvcCount >= 6) ) &&
				(recentTypes[0] == domType) && (recentTypes[2] == domType) && (recentTypes[1] != domType))
				PostClass[recentTypes[1]][0] = PVC ;

			// If the previous and following beats are the dominant beat type,
			// and this beat is significantly different from the dominant,
			// call it a PVC.

			else if((recentTypes[0] == domType) && (recentTypes[2] == domType) && (PostClassify_lastMI2 > 2.5))
				PostClass[recentTypes[1]][0] = PVC ;

			// Otherwise post classify this beat as UNKNOWN.

			else PostClass[recentTypes[1]][0] = UNKNOWN ;

			// If the beat is premature followed by a compensitory pause, post
			// classify the rhythm as PVC.

			if(((normRR-(normRR>>3)) > recentRRs[1]) && ((recentRRs[0]-(recentRRs[0]>>3)) > normRR))
				PCRhythm[recentTypes[1]][0] = PVC ;

			// Otherwise, post classify the rhythm as the same as the
			// regular rhythm classification.

			else PCRhythm[recentTypes[1]][0] = PostClassify_lastRC ;
			}

		PostClassify_lastMI2 = mi2 ;
		PostClassify_lastRC = rhythmClass ;
		}

	/**
	 * CheckPostClass checks to see if three of the last four or six of the
	 * last eight of a given beat type have been post classified as PVC.
	 * 
	 * @param type
	 * @return
	 */
	public int CheckPostClass(int type)
		{
		int i, pvcs4 = 0, pvcs8 ;

		if(type == bdacParas.MAXTYPES)
			return(UNKNOWN) ;

		for(i = 0; i < 4; ++i)
			if(PostClass[type][i] == PVC)
				++pvcs4 ;
		for(pvcs8=pvcs4; i < 8; ++i)
			if(PostClass[type][i] == PVC)
				++pvcs8 ;

		if((pvcs4 >= 3) || (pvcs8 >= 6))
			return(PVC) ;
		else return(UNKNOWN) ;
		}

	/**
	 * Check classification of previous beats' rhythms based on post beat
	 * classification.  If 7 of 8 previous beats were classified as NORMAL
	 * (regular) classify the beat type as NORMAL (regular).
	 * Call it a PVC if 2 of the last 8 were regular.
	 * 
	 * @param type
	 * @return
	 */
	public int CheckPCRhythm(int type)
		{
		int i ;
		int normCount ;
		int n ;

		if(type == bdacParas.MAXTYPES)
			return(UNKNOWN) ;

		if(matcher.GetBeatTypeCount(type) < 9)
			n = matcher.GetBeatTypeCount(type)-1 ;
		else n = 8 ;

		for(i = normCount = 0; i < n; ++i)
			if(PCRhythm[type][i] == NORMAL)
				++normCount;
		if(normCount >= 7)
			return(NORMAL) ;
		if(((normCount == 0) && (n < 4)) ||
			((normCount <= 1) && (n >= 4) && (n < 7)) ||
			((normCount <= 2) && (n >= 7)))
			return(PVC) ;
		return(UNKNOWN) ;
		}
	}
