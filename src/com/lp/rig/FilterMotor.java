//
//  FilterMotor.java
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

import lejos.robotics.subsumption.*;
import lejos.nxt.*;
import lejos.nxt.comm.*;
import java.io.*;

public class FilterMotor
  implements Behavior, ConsoleMotor {

  private LCDDisplay   display_      = null;
  private FilterSensor filterSensor_ = null;
  private Motor        motor_        = null;
  
  public FilterMotor(LCDDisplay display, FilterSensor filterSensor, Motor motor) {

    display_      = display;
    filterSensor_ = filterSensor;
    motor_        = motor;

    motor_.regulateSpeed(true);
    motor_.smoothAcceleration(true);
    motor_.setSpeed(900);
    motor_.stop();
  }
  
  public boolean takeControl() {
    // If we're already moving, don't change anything...
    if (isMoving())
      return false;
    
    // If we're not on the filter we want, then start moving towards it..
    return filterSensor_.getCurrentFilter() != filterSensor_.getDesiredFilter();
  }

  // The following four entries make up the ConsoleMotor protocol, used
  // to move things around during an emergency halt of the transport. We
  // also use them in our own mainstream code, although not widely.
  public boolean isMoving() {
    return motor_.isMoving();
  }

  public void spool() {
    motor_.forward();
  }
  
  public void unspool() {
    motor_.backward();
  }
  
  public void stop() {
    motor_.stop();
  }
  
  public void action() {
    display_.updateActionIndicator(LCDDisplay.FILTER_MOTOR_ACTION, true);
    spool();
    display_.updateActionIndicator(LCDDisplay.FILTER_MOTOR_ACTION, false);
  }
  
  public void suppress() {
  }

  public void writeState(DataOutputStream s) throws IOException {
    MotorState.write(s, motor_);
  }
  
}
