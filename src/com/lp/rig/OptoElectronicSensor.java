//
//  OptoElectronicSensor.java
//  Transport
//
//  Created by Joseph Goldstone on 7/6/2008.
//
//       -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
//       Copyright (c) 2008 Lilliputian Pictures LLC
//       
//       All rights reserved.
//       
//       Redistribution and use in source and binary forms, with or without
//       modification, are permitted provided that the following conditions are
//       met:
//       *       Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//       *       Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following disclaimer
//       in the documentation and/or other materials provided with the
//       distribution.
//       *       Neither the name of Lilliputian Pictures LLC nor the names of
//       its contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission. 
//       
//       THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//       "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
//       LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
//       A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
//       OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//       SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
//       LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//       DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
//       THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//       (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//       OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//      
//       -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 

package com.lp.rig;

import lejos.nxt.*;

public abstract class OptoElectronicSensor 
  implements SensorConstants, SensorPortListener {

  public static final int HIGHEST_OFF_VALUE = 200;
  public static final int LOWEST_ON_VALUE   = 800;

	private LCDDisplay		display_ = null;
	private SensorPort		port_;
  private volatile int	lastSavedValue_        = 0;

  public OptoElectronicSensor(LCDDisplay display, SensorPort port) {
		display_				= display;
    port_           = port;
		port.setTypeAndMode(TYPE_SWITCH, MODE_RAW);
    lastSavedValue_ = port.readRawValue();
  }

  protected boolean offValue(int value) {
    return value <= HIGHEST_OFF_VALUE;
  }
  
  protected boolean onValue(int value) {
    return value >= LOWEST_ON_VALUE;
  }
  
  protected boolean inBetweenValue(int value) {
    return ! (offValue(value) || onValue(value));
  }
  
  public boolean isOn() {
    int newValue = port_.readRawValue();
    if (! inBetweenValue(newValue))
      lastSavedValue_ = newValue;
    return onValue(newValue);
  }
  
  public boolean isOff() {
    int newValue = port_.readRawValue();
    if (! inBetweenValue(newValue))
      lastSavedValue_ = newValue;
    return offValue(newValue);
  }
  
  public boolean changedToOn(int anOldValue, int aNewValue) {
    return offValue(anOldValue) && onValue(aNewValue);
  }

  public boolean changedToOff(int anOldValue, int aNewValue) {
    return onValue(anOldValue) && offValue(aNewValue);
  }
  
  public boolean changed(int anOldValue, int aNewValue) {
    return changedToOn(anOldValue, aNewValue) || changedToOff(anOldValue, aNewValue);
  }
    
  public abstract void stateSignificantlyChanged(int aSignificantOldValue, int aSignificantlyNewValue);
  
  public void stateChanged(SensorPort port, int ignored, int aNewValue) {
    if (inBetweenValue(lastSavedValue_)) {
      // we can get here when the value happens to be in-between at the time this object is
      // created, if we're unlucky. We can take this path through the code any number of 
      // times, but only until we see a new value that's legit. Once we've seen one of
      // those, we never come back here.
      if (! inBetweenValue(aNewValue))
        lastSavedValue_ = aNewValue;
      return;
    }

    // If we've made it this far, then lastSavedValue_ was legitimately on or off.
    // Now if the new value isn't either on or off, ignore it and return.
    if (inBetweenValue(aNewValue))
      return;
    
    // If we've made it this far, then lastSavedValue_ was legitimately on or off,
    // and so is aNewValue.
    if (   changedToOn (lastSavedValue_, aNewValue)
        || changedToOff(lastSavedValue_, aNewValue)) {
      stateSignificantlyChanged(lastSavedValue_, aNewValue);
      lastSavedValue_ = aNewValue;
      return;
    }

    // If we've made it this far, then both lastSavedValue_ was legitmately on or off
    // and aNewValue was too, but apparently they didn't change. That said, let's check.
    boolean bothOff = offValue(lastSavedValue_) && offValue(aNewValue);
    boolean bothOn  =  onValue(lastSavedValue_) &&  onValue(aNewValue);
    if (! (bothOff || bothOn)) {
      // I guess we don't understand what's going on, so make some noise.
      Sound.beepSequence();
    }
  }
		
}

