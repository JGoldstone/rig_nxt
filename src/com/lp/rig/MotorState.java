//
//  MotorState.java
//  MotorState
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

import java.io.*;
import lejos.nxt.*;

public class MotorState {

  private static final int MOTION_FORWARD  = 0;
  private static final int MOTION_BACKWARD = 1;
  private static final int MOTION_FLOATING = 2;
  private static final int MOTION_STOPPED  = 3;

  private static boolean SPEED_REGULATION_ENABLED = true;
  private static boolean SPEED_REGULATION_DISABLED = false;
    
  public static void write(DataOutputStream s, Motor motor) throws IOException
  {
    if (motor.isForward())
      s.writeInt(MOTION_FORWARD);
    else if (motor.isBackward())
      s.writeInt(MOTION_BACKWARD);
    else if (motor.isFloating())
      s.writeInt(MOTION_FLOATING);
    else if (motor.isStopped())
    s.writeInt(MOTION_STOPPED);
    // +++ Throw exception in race condition between checking state 
    // +++ and state change?
    
    s.writeInt(motor.isRegulating() ? 1 : 0);
    s.writeInt(motor.getPower());
    s.writeInt(motor.getSpeed());
    // s.writeInt(motor.getActualSpeed());
    s.writeInt(motor.getRotationSpeed());
    s.writeInt(motor.getLimitAngle());
    s.writeInt(motor.getTachoCount());
  }
}
