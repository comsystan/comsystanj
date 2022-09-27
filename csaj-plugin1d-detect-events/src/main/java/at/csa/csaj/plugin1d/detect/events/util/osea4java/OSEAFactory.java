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

package at.csa.csaj.plugin1d.detect.events.util.osea4java;

import at.csa.csaj.plugin1d.detect.events.util.osea4java.classification.BDACParameters;
import at.csa.csaj.plugin1d.detect.events.util.osea4java.classification.BeatAnalyzer;
import at.csa.csaj.plugin1d.detect.events.util.osea4java.classification.BeatDetectionAndClassification;
import at.csa.csaj.plugin1d.detect.events.util.osea4java.classification.Classifier;
import at.csa.csaj.plugin1d.detect.events.util.osea4java.classification.Matcher;
import at.csa.csaj.plugin1d.detect.events.util.osea4java.classification.NoiseChecker;
import at.csa.csaj.plugin1d.detect.events.util.osea4java.classification.PostClassifier;
import at.csa.csaj.plugin1d.detect.events.util.osea4java.classification.RythmChecker;


/*-
 * #%L
 * Project: ImageJ2 signal plugin for detecting events
 * File: OSEAFactory.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2022 Comsystan Software
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
public class OSEAFactory 
	{
	
	/**
	 * Creates a BeatDetectorAndClassificator for the given parameters.
	 * 
	 * @param sampleRate The sampleRate of the ECG samples.
	 * @param beatSampleRate The sampleRate for the classification
	 * @return An object capable of detection and classification
	 */
	public static BeatDetectionAndClassification createBDAC(int sampleRate, int beatSampleRate)
		{
		BDACParameters bdacParameters = new BDACParameters(beatSampleRate) ;
		QRSDetectorParameters qrsDetectorParameters = new QRSDetectorParameters(sampleRate) ;
		
		BeatAnalyzer beatAnalyzer = new BeatAnalyzer(bdacParameters) ;
		Classifier classifier = new Classifier(bdacParameters, qrsDetectorParameters) ;
		Matcher matcher = new Matcher(bdacParameters, qrsDetectorParameters) ;
		NoiseChecker noiseChecker = new NoiseChecker(qrsDetectorParameters) ;
		PostClassifier postClassifier = new PostClassifier(bdacParameters) ;
		QRSDetector2 qrsDetector = createQRSDetector2(sampleRate) ;
		RythmChecker rythmChecker = new RythmChecker(qrsDetectorParameters) ;
		BeatDetectionAndClassification bdac 
			= new BeatDetectionAndClassification(bdacParameters, qrsDetectorParameters) ;

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
	public static QRSDetector createQRSDetector(int sampleRate) 
		{
		QRSDetectorParameters qrsDetectorParameters = new QRSDetectorParameters(sampleRate) ;
		
		QRSDetector qrsDetector = new QRSDetector(qrsDetectorParameters) ;
		QRSFilterer qrsFilterer = new QRSFilterer(qrsDetectorParameters) ;
		
		qrsDetector.setObjects(qrsFilterer) ;
		return qrsDetector ;
		}
	
	/**
	 * Create a QRSDetector2 for the given sampleRate
	 * 
	 * @param sampleRate The sampleRate of the ECG samples
	 * @return A QRSDetector2
	 */
	public static QRSDetector2 createQRSDetector2(int sampleRate) 
		{
		QRSDetectorParameters qrsDetectorParameters = new QRSDetectorParameters(sampleRate) ;
		
		QRSDetector2 qrsDetector2 = new QRSDetector2(qrsDetectorParameters) ;
		QRSFilterer qrsFilterer = new QRSFilterer(qrsDetectorParameters) ;
		
		qrsDetector2.setObjects(qrsFilterer) ;
		return qrsDetector2 ;
		}
	}
