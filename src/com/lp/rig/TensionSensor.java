//
//  TensionSensor.java
//  Transport
//
//  Created by Joseph Goldstone on 7/30/2008.
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
import java.io.*;

public class TensionSensor
  extends OptoElectronicSensor
  implements OptoElectronicSensorCallback {

  private LCDDisplay  display_     = null;
  private Network     network_     = null;

  // We consider ourselves tense when the 'tension' hole comes underneath the
  // sensor. With Scott's sensors, the free passage of IR will have the raw code
  // values be somewhere above 950 and the OptoElectronicSensor code registering
  // the sensor being in the "on" state.

  private boolean changedToTense(int oldValue, int newValue) {
    return changedToOn(oldValue, newValue);
  }
  
  private boolean changedToNotTense(int oldValue, int newValue) {
    return changedToOff(oldValue, newValue);
  }
  
  private volatile boolean initialTensioningComplete_ = false;
  private volatile boolean tense_ = false;

  public TensionSensor(LCDDisplay display, SensorPort port) {
    super(display, port);
    display_ = display;
    tense_ = isOn();
    initialTensioningComplete_ = Rig.getTransportState() != TransportState.TOO_SLACK;
    display_.updateTensionSensorStatus(tense_, initialTensioningComplete_);
  }
  
  public void setNetwork(Network network) {
    network_ = network;
  }
  
  public void stateSignificantlyChanged(int oldSignificantValue, int newSignificantValue) {
    tense_ = onValue(newSignificantValue);
    if (! initialTensioningComplete_) {
      initialTensioningComplete_ = tense_ && Rig.getTransportState() != TransportState.TOO_SLACK;
    }
    display_.updateTensionSensorStatus(tense_, initialTensioningComplete_);
		Rig.updateTransportState();
    if (network_ != null)
      network_.queueTransmission();
  }
    
  public boolean getInitialTensioningComplete() {
    return initialTensioningComplete_;
  }
    
  public boolean getTense() {
    return tense_;
  }
  
  public void writeState(DataOutputStream s) throws IOException{
    s.writeInt(initialTensioningComplete_ ? 1 : 0);
    s.writeInt(tense_ ? 1 : 0);
  }
}
