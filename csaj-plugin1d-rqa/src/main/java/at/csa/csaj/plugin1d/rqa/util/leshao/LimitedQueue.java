/*-
 * #%L
 * Project: ImageJ2 signal plugin for recurrence quantification analysis.
 * File: LimitedQueue.java
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

import java.util.Iterator;

/**
 * This is a class from
 * https://github.com/Leshao-Zhang/Visualisation-Cross-Recurrence-Quantification-Analysis
 */

public class LimitedQueue<T> implements Iterable<T>{  //does not support primitive type
  private int size;
  private T[] queue;
  private int start;
  private int end;
  private boolean full;
  
  @SuppressWarnings("unchecked")
  public LimitedQueue(int size){
    this.size=size;
    queue=(T[])(new Object[size]);
  }
  
  public Iterator<T> iterator(){
    return new LimitedQueueIterator<T>(this);
  }
  
  public void add(T t){
    if(end>=size){
      end=0;
      full=true;
    }
    if(full){
      if(start>=size)start=0;
      start++;
    }
    queue[end++]=t;
  }
  
  public T get(int index){
    index=start+index;
    if(index>=size)index-=size;
    return queue[index];
  }
  
  public int size(){
    if(full)return size;
    return end;
  }
}
