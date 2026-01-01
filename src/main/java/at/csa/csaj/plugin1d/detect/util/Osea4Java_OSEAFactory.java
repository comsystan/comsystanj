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

/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Osea4Java_OSEAFactory.java
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


/**
 * A factory to create detection of classification objects.
 */
public class Osea4Java_OSEAFactory 
	{
	
	/**
	 * Creates a BeatDetectorAndClassificator for the given parameters.
	 * 
	 * @param sampleRate The sampleRate of the ECG samples.
	 * @param beatSampleRate The sampleRate for the classification
	 * @return An object capable of detection and classification
	 */
	public static Osea4Java_BeatDetectionAndClassification createBDAC(int sampleRate, int beatSampleRate)
		{
		Osea4Java_BDACParameters bdacParameters = new Osea4Java_BDACParameters(beatSampleRate) ;
		Osea4Java_QRSDetectorParameters qrsDetectorParameters = new Osea4Java_QRSDetectorParameters(sampleRate) ;
		
		Osea4Java_BeatAnalyzer beatAnalyzer = new Osea4Java_BeatAnalyzer(bdacParameters) ;
		Osea4Java_Classifier classifier = new Osea4Java_Classifier(bdacParameters, qrsDetectorParameters) ;
		Osea4Java_Matcher matcher = new Osea4Java_Matcher(bdacParameters, qrsDetectorParameters) ;
		Osea4Java_NoiseChecker noiseChecker = new Osea4Java_NoiseChecker(qrsDetectorParameters) ;
		Osea4Java_PostClassifier postClassifier = new Osea4Java_PostClassifier(bdacParameters) ;
		Osea4Java_QRSDetector2 qrsDetector = createQRSDetector2(sampleRate) ;
		Osea4Java_RythmChecker rythmChecker = new Osea4Java_RythmChecker(qrsDetectorParameters) ;
		Osea4Java_BeatDetectionAndClassification bdac 
			= new Osea4Java_BeatDetectionAndClassification(bdacParameters, qrsDetectorParameters) ;

		classifier.setObjects(matcher, rythmChecker, postClassifier, beatAnalyzer) ;
		matcher.setObjects(postClassifier, beatAnalyzer, classifier) ;
		postClassifier.setObjects(matcher) ;
		bdac.setObjects(qrsDetector, noiseChecker, matcher, classifier) ;
		
		return bdac;
		}

	/**
	 * Create a QRSDetector for the given sampleRate
	 * 
	 * @param sampleRate The sampleRate of the ECG samples
	 * @return A QRSDetector
	 */
	public static Osea4Java_QRSDetector createQRSDetector(int sampleRate) 
		{
		Osea4Java_QRSDetectorParameters qrsDetectorParameters = new Osea4Java_QRSDetectorParameters(sampleRate) ;
		
		Osea4Java_QRSDetector qrsDetector = new Osea4Java_QRSDetector(qrsDetectorParameters) ;
		Osea4Java_QRSFilterer qrsFilterer = new Osea4Java_QRSFilterer(qrsDetectorParameters) ;
		
		qrsDetector.setObjects(qrsFilterer) ;
		return qrsDetector ;
		}
	
	/**
	 * Create a QRSDetector2 for the given sampleRate
	 * 
	 * @param sampleRate The sampleRate of the ECG samples
	 * @return A QRSDetector2
	 */
	public static Osea4Java_QRSDetector2 createQRSDetector2(int sampleRate) 
		{
		Osea4Java_QRSDetectorParameters qrsDetectorParameters = new Osea4Java_QRSDetectorParameters(sampleRate) ;
		
		Osea4Java_QRSDetector2 qrsDetector2 = new Osea4Java_QRSDetector2(qrsDetectorParameters) ;
		Osea4Java_QRSFilterer qrsFilterer = new Osea4Java_QRSFilterer(qrsDetectorParameters) ;
		
		qrsDetector2.setObjects(qrsFilterer) ;
		return qrsDetector2 ;
		}
	}
