//
//  Console.java
//  Transport
//
//  Created by Joseph Goldstone on 7/31/2008.
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

public class Console
  implements Behavior {

  LCDDisplay   display_     = null;
  ConsoleMotor supplyMotor_ = null;
  ConsoleMotor takeupMotor_ = null;
  ConsoleMotor filterMotor_ = null;

  boolean      halted_      = false;
  ConsoleMotor activeMotor_ = null;
  boolean      unspooling_  = false;
  boolean      spooling_    = false;

  public void displayState() {
    if (halted_) {
      display_.updateActionIndicator(LCDDisplay.CONSOLE_ACTION, true);
      display_.updateConsoleHaltStatus(true);
      if (activeMotor_ == supplyMotor_)
        display_.updateConsoleSelectedMotor("supply");
      else if (activeMotor_ == takeupMotor_)
        display_.updateConsoleSelectedMotor("takeup");
      else if (activeMotor_ == filterMotor_)
        display_.updateConsoleSelectedMotor("filter");

      if (! unspooling_ && ! spooling_) {
        display_.updateConsoleSpoolingStatus("unspool spool   ");
      } else if (unspooling_) {
        display_.updateConsoleSpoolingStatus("unspooling      ");
      } else if (spooling_) {
        display_.updateConsoleSpoolingStatus("        spooling");
      } else {
        display_.updateConsoleSpoolingStatus("**BOTH : ERROR**");
      }
    } else {
      display_.updateActionIndicator(LCDDisplay.CONSOLE_ACTION, false);
      display_.updateConsoleHaltStatus(false);
    }
  }
      
  class EnterButton implements ButtonListener {

    public void buttonPressed(Button b) {
      halted_ = !halted_;
      supplyMotor_.stop();
      takeupMotor_.stop();
      filterMotor_.stop();
      unspooling_ = false;
      spooling_   = false;
      displayState();
    }
    public void buttonReleased(Button b) {
    }
  }
  
  class EscapeButton implements ButtonListener {

    public void buttonPressed(Button b) {
      if (! halted_)
        return;
      activeMotor_.stop();
      if (activeMotor_ == supplyMotor_)
        activeMotor_ = takeupMotor_;
      else if (activeMotor_ == takeupMotor_)
//        activeMotor_ = filterMotor_;
//      else if (activeMotor_ == filterMotor_)
        activeMotor_ = supplyMotor_;
      displayState();
    }

    public void buttonReleased(Button b) {
    }
  }
  
  class LeftButton implements ButtonListener {

    public void buttonPressed(Button b) {
      if (! halted_)
        return;
      if (activeMotor_.isMoving()) {
        activeMotor_.stop();
        unspooling_ = false;
        spooling_ = false;
      } else {
        activeMotor_.unspool();
        unspooling_ = true;
      }
      displayState();
    }

    public void buttonReleased(Button b) {
    }
  }
  
  class RightButton implements ButtonListener {

    public void buttonPressed(Button b) {
      if (! halted_)
        return;
      if (activeMotor_.isMoving()) {
        activeMotor_.stop();
        unspooling_ = false;
        spooling_ = false;
      } else {
        activeMotor_.spool();
        spooling_ = true;
      }
      displayState();
    }

    public void buttonReleased(Button b) {
    }
  }
    
  public boolean takeControl() {
    return halted_;
  }
  
  public void action() 
  {
  }

  public void suppress() {
  }

  public Console(LCDDisplay   display,
                 ConsoleMotor supplyMotor,
                 ConsoleMotor takeupMotor,
                 ConsoleMotor filterMotor) 
  {
    display_     = display;
    supplyMotor_ = supplyMotor;
    takeupMotor_ = takeupMotor;
    filterMotor_ = filterMotor;
    
    activeMotor_ = supplyMotor_;
    
    Button.ENTER.addButtonListener(new EnterButton());
    Button.ESCAPE.addButtonListener(new EscapeButton());
    Button.LEFT.addButtonListener(new LeftButton());
    Button.RIGHT.addButtonListener(new RightButton());
  }
  
}
