/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Leshao_LorenzAttractor.java
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
package at.csa.csaj.plugin1d.cplx.util;

/**
 * This is a class from
 * https://github.com/Leshao-Zhang/Visualisation-Cross-Recurrence-Quantification-Analysis
 */

public class Leshao_LorenzAttractor{
  
  private float sigma=10f;
  private float beta=8/3f;
  private float rho=28f;
  private float dt=0.01f;
  private int size=1000;
  private float[] xyz = {1f,1f,1f};
  private Leshao_LimitedQueue<float[]> trajectory;
  
  public Leshao_LorenzAttractor(){
    trajectory=new Leshao_LimitedQueue<float[]>(size);
  }
  
  public Leshao_LorenzAttractor(int size){
    this.size=size;
    trajectory=new Leshao_LimitedQueue<float[]>(size);
  }
  
  public Leshao_LorenzAttractor(float[] xyz, float sigma,float beta,float rho,float dt,int size){
    this.xyz=xyz;
    this.sigma=sigma;
    this.beta=beta;
    this.rho=rho;
    this.dt=dt;
    this.size=size;
    trajectory=new Leshao_LimitedQueue<float[]>(size);
  }
  
  public void run(){
    float dx=(sigma*(xyz[1]-xyz[0]))*dt;
    float dy=(xyz[0]*(rho-xyz[2])-xyz[1])*dt;
    float dz=(xyz[0]*xyz[1]-beta*xyz[2])*dt;
    xyz[0]+=dx;
    xyz[1]+=dy;
    xyz[2]+=dz;
    trajectory.add(xyz.clone());    
  }

  public float[] getPoint(){
    return xyz;
  }
  
  public Leshao_LimitedQueue<float[]> getTrajectory(){
    return trajectory; //<>// //<>//
  }
}
