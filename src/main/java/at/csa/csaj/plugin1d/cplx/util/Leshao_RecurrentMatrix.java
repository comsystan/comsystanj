/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Leshao_RecurrentMatrix.java
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
package at.csa.csaj.plugin1d.cplx.util;

import java.lang.Math;  
import java.util.HashMap;


/**
 * This is a modified and extended class from
 * https://github.com/Leshao-Zhang/Visualisation-Cross-Recurrence-Quantification-Analysis
 */

public class Leshao_RecurrentMatrix{
  private Leshao_LimitedQueue<Float> ts1, ts2;
  int dim, lag;
  private float[][] distanceMatrix, recurrentMatrix;
  private float recurrentRate;
  private float lengthMean;
  private float lengthMax;
  private float ent;
  private float det;
  private float lam;
  private float trappingTime;
  private HashMap<Integer,Integer> lineHist;
  private boolean takeDiagonal;
  private float eps;
  
  public Leshao_RecurrentMatrix(Leshao_LimitedQueue<Float> ts1,Leshao_LimitedQueue<Float> ts2, int dim,int lag, float eps, boolean takeDiagonal, String norm){
	this.ts1 = ts1;
	this.ts2 = ts2;
	this.dim = dim;
	this.lag = lag;
	this.eps = eps;
	this.takeDiagonal = takeDiagonal;
    if (norm.equals("Euclidean")){
    	calcEuclideanDistanceMatrix();
    }
    else if (norm.equals("Maximum")){
    	calcMaximumDistanceMatrix();
    }
    else if (norm.equals("Cityblock")){
    	calcCityblockDistanceMatrix();
    }
    else if (norm.equals("Angular")){
    	calcAngularDistanceMatrix();
    }
    this.calcRecurrentMatrix();
    this.calcDiagonalHisto();
    this.calcDiagonalRQAValues();
    this.calcVerticalHisto();
    this.calcVerticalRQAValues();
  }

  public Leshao_RecurrentMatrix(Leshao_LimitedQueue<Float> ts, int dim, int lag, float radius, boolean takeDiagonal, String norm){
    this(ts, ts, dim, lag, radius, takeDiagonal, norm);
  }
  
  public float[][] getDistanceMatrix(){
	  return distanceMatrix;
  }

  public float[][] getRecurrentMatrix(){
	  return recurrentMatrix;
  }

  public float getRecurrentRate(){
	  return recurrentRate;
  }

  public float getLMAX(){
	  return lengthMax;
  }

  public float getDeterminist(){
	  return det;
  }

  public float[] getRQAValues() {
	  float[] rqaValues = new float[7];
	  rqaValues[0] = recurrentRate;
	  rqaValues[1] = det;
	  rqaValues[2] = lengthMean;
	  rqaValues[3] = lengthMax;
	  rqaValues[4] = ent;
	  rqaValues[5] = lam;
	  rqaValues[6] = trappingTime;
	  return rqaValues;
  }

  private float euclideanDistance(float[] p1, float[] p2){
    float sum=0;
    for(int dim=0;dim<p1.length;dim++){ //dim: x, y, z,.....coordinate
      sum+=(p1[dim]-p2[dim])*(p1[dim]-p2[dim]);
    }
    return (float)Math.sqrt(sum);
  }
  
  private float maximumDistance(float[] p1, float[] p2){
	  float max  = Float.MIN_VALUE;
	  float dist = Float.MIN_VALUE;
	  for(int dim=0;dim<p1.length;dim++){
		  dist = Math.abs(p1[dim]-p2[dim]);
		  if(dist > max) max = dist;
	  }
	  return max;
  }
  
  private float cityBlockDistance(float[] p1, float[] p2){
	  float sum=0;
	  for(int dim=0;dim<p1.length;dim++){
	      sum+=Math.abs(p1[dim]-p2[dim]);
	  }
	  return sum;
  }
  
  //According to Marwan&Kraemer, Trends in recurrence analysis of dynamical systems, 
  //Eur. Phys. J. Spec. Top. (2023) 232:5–27
  //https://doi.org/10.1140/epjs/s11734-022-00739-8
  private float angularDistance(float[] p1, float[] p2){ 
	    float sum1 = 0;
	    float sum2 = 0;
	    float sum3 = 0;
	    
	    for(int dim=0;dim<p1.length;dim++){ //dim: x, y, z,.....coordinate	
	    	//sum  += (p1[dim] * p2[dim])/(Math.sqrt(p1[dim]*p1[dim]) * Math.sqrt(p2[dim]*p2[dim]));
	    	sum1 += p1[dim] * p2[dim];
	    	sum2 += p1[dim] * p1[dim];
	    	sum3 += p2[dim] * p2[dim];
	    }  	
	    return (float)Math.acos(sum1/(Math.sqrt(sum2) + Math.sqrt(sum3)));
  }

  public void takeDiagonal(boolean bool){
	    takeDiagonal = bool;
  }

  private void increaseHistogram(HashMap<Integer,Integer> map, int k){
    if(!map.containsKey(k)){
      map.put(k,1);
    }else{
      map.put(k,map.get(k)+1);
    }
  }

  public void calcEuclideanDistanceMatrix(){
	  Leshao_LimitedQueue<float[]> reconstruct1 = Leshao_PhaseSpaceReconstruct.go(ts1, dim, lag);
	  Leshao_LimitedQueue<float[]> reconstruct2 = Leshao_PhaseSpaceReconstruct.go(ts2, dim, lag);
	  distanceMatrix = new float[reconstruct1.size()][reconstruct2.size()];
	  for(int p1=0;p1<distanceMatrix.length;p1++){
		  for(int p2=0;p2<distanceMatrix[0].length;p2++){
	        distanceMatrix[p1][p2]=euclideanDistance(reconstruct1.get(p1),reconstruct2.get(p2));
	      }
	  } 
  }
  
  public void calcMaximumDistanceMatrix(){
	  Leshao_LimitedQueue<float[]> reconstruct1 = Leshao_PhaseSpaceReconstruct.go(ts1, dim, lag);
	  Leshao_LimitedQueue<float[]> reconstruct2 = Leshao_PhaseSpaceReconstruct.go(ts2, dim, lag);
	  distanceMatrix = new float[reconstruct1.size()][reconstruct2.size()];
	  for(int p1=0;p1<distanceMatrix.length;p1++){
		  for(int p2=0;p2<distanceMatrix[0].length;p2++){
	        distanceMatrix[p1][p2]=maximumDistance(reconstruct1.get(p1),reconstruct2.get(p2));
	      }
	  } 
  }
  
  public void calcCityblockDistanceMatrix(){
	  Leshao_LimitedQueue<float[]> reconstruct1 = Leshao_PhaseSpaceReconstruct.go(ts1, dim, lag);
	  Leshao_LimitedQueue<float[]> reconstruct2 = Leshao_PhaseSpaceReconstruct.go(ts2, dim, lag);
	  distanceMatrix = new float[reconstruct1.size()][reconstruct2.size()];
	  for(int p1=0;p1<distanceMatrix.length;p1++){
		  for(int p2=0;p2<distanceMatrix[0].length;p2++){
	        distanceMatrix[p1][p2]=cityBlockDistance(reconstruct1.get(p1),reconstruct2.get(p2));
	      }
	  } 
  }
  
  public void calcAngularDistanceMatrix(){
	  Leshao_LimitedQueue<float[]> reconstruct1 = Leshao_PhaseSpaceReconstruct.go(ts1, dim, lag);
	  Leshao_LimitedQueue<float[]> reconstruct2 = Leshao_PhaseSpaceReconstruct.go(ts2, dim, lag);
	  distanceMatrix = new float[reconstruct1.size()][reconstruct2.size()];
	  for(int p1=0;p1<distanceMatrix.length;p1++){
		  for(int p2=0;p2<distanceMatrix[0].length;p2++){
	        distanceMatrix[p1][p2]=angularDistance(reconstruct1.get(p1),reconstruct2.get(p2));
	      }
	  } 
  }

  public void calcRecurrentMatrix(){	  
	    float recurrent=0;
	    recurrentMatrix = new float[distanceMatrix.length][distanceMatrix[0].length];
	    for(int i=0;i<distanceMatrix.length;i++){
	      for(int j=0;j<distanceMatrix[0].length;j++){
	        if(takeDiagonal||i!=j){
	          if(distanceMatrix[i][j]<=eps){
	            recurrentMatrix[i][j]=1;
	            recurrent++;
	          }
	        }
	      }
	    }
	    recurrentRate=recurrent/(distanceMatrix.length*distanceMatrix[0].length);
  }

  private void calcDiagonalHisto(){
	if (lineHist != null) lineHist.clear();
    lineHist = new HashMap<Integer,Integer>();
    int line=0;
    for(int x=0;x<recurrentMatrix.length;x++){
      for(int d=0;x+d<recurrentMatrix.length;d++){ //from bottom left to top (y=0) right
          if(recurrentMatrix[x+d][d]==1){
            line++;
          }else{
            increaseHistogram(lineHist,line);
            line=0;
          }
      }
      if(line!=0)increaseHistogram(lineHist,line);
      line=0;
    }
    line=0;
    for(int y=0;y<recurrentMatrix[0].length;y++){ //y=1?????????????????????????????????????????????????????????????????????
      for(int d=0;d+y<recurrentMatrix[0].length;d++){ //from top (y=0) left to bottom right
          if(recurrentMatrix[d][y+d]==1){
            line++;
          }else{
            increaseHistogram(lineHist,line);
            line=0;
          }
      }
      if(line!=0)increaseHistogram(lineHist,line);
      line=0;
    }
  }
  
  private void calcVerticalHisto(){
	    if (lineHist != null) lineHist.clear();
	    lineHist = new HashMap<Integer,Integer>();
	    int line=0;
	    for(int x=0;x<recurrentMatrix.length;x++){
	      for(int y=0;y<recurrentMatrix[0].length;y++){ //from top (y=0) to bottom
	          if(recurrentMatrix[x][y]==1){
	            line++;
	          }else{
	            increaseHistogram(lineHist,line);
	            line=0;
	          }
	      }
	      if(line!=0)increaseHistogram(lineHist,line);
	      line=0;
	    }
  }

  private void calcHorizontalHisto(){
	    if (lineHist != null) lineHist.clear();
	    lineHist = new HashMap<Integer,Integer>();
	    int line=0;
	    for(int y=0;y<recurrentMatrix[0].length;y++){
	      for(int x=0;x<recurrentMatrix.length;x++){ //from left to right
	          if(recurrentMatrix[x][y]==1){
	            line++;
	          }else{
	            increaseHistogram(lineHist,line);
	            line=0;
	          }
	      }
	      if(line!=0)increaseHistogram(lineHist,line);
	      line=0;
	    }
}
  
  private void calcDiagonalRQAValues(){
	    float totalLine  = 0;
	    float totalPoint = 0;
	    float numLines   = 0;
	    lengthMean = Float.NaN;
	    lengthMax  = 0;
	    det = Float.NaN;
	    ent = 0;
	    for(int l:lineHist.keySet()){
	      if(l>lengthMax)lengthMax=l;
	      if(l>1){
	    	  totalLine += l*lineHist.get(l); //lines longer than lmin = 1;
	    	  numLines  += lineHist.get(l);
	      }
	      totalPoint+=l*lineHist.get(l);
	    }
	    det        = totalLine/totalPoint;
	    lengthMean = totalLine/numLines;
	   
	    //extra loop for entropy
	    for(int l:lineHist.keySet()){
	    	 if(l>1){
	    		 ent += lineHist.get(l)/numLines*Math.log(lineHist.get(l)/numLines);
	    	 }
	    }
	    ent = -ent;
  } 
  
  
  private void calcVerticalRQAValues(){
	    float totalLine  = 0;
	    float totalPoint = 0;
	    float numLines   = 0;
	    lam = Float.NaN;
	    trappingTime = Float.NaN;
	 
	    for(int l:lineHist.keySet()){
	      if(l>lengthMax)lengthMax=l;
	      if(l>1){
	    	  totalLine += l*lineHist.get(l); //lines longer than lmin = 1;
	    	  numLines  += lineHist.get(l);
	      }
	      totalPoint+=l*lineHist.get(l);
	    }
	    lam          = totalLine/totalPoint; //such as DET
	    trappingTime = totalLine/numLines;   //such as lengthMean
  } 
  
}
