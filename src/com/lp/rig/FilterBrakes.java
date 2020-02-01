//
//  FilterBrakes.java
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

public class FilterBrakes
  implements Behavior{

  private LCDDisplay   display_      = null;
  private FilterSensor filterSensor_ = null;
  private Motor        motor_        = null;

  public FilterBrakes(LCDDisplay display,
                      FilterSensor filterSensor,
                      Motor motor) {
    display_      = display;
    filterSensor_ = filterSensor;
    motor_        = motor;
  }
  
  public boolean takeControl() {
    if (! motor_.isMoving())
      return false;
    return filterSensor_.getCurrentFilter() == filterSensor_.getDesiredFilter();
  }

  public void action() {
    display_.updateActionIndicator(LCDDisplay.FILTER_BRAKE_ACTION, true);
    motor_.stop();
    display_.updateActionIndicator(LCDDisplay.FILTER_BRAKE_ACTION, false);
  }
  
  public void suppress() {
  }
}
