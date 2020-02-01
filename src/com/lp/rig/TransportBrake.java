//
//  TransportBrake.java
//  Transport
//
//  Created by Joseph Goldstone on 1/5/2009.
//
//       -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
//       Copyright (c) 2009 Lilliputian Pictures LLC
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

public class TransportBrake
  implements Behavior {

  private LCDDisplay                  display_                      = null;
  private SlackSensor                 slackSensor_                  = null;
  private TensionSensor               tensionSensor_                = null;
  private PerfSensor                  perfSensor_                   = null;
  private volatile TransportRole      role_                         = null;
  private Motor                       motor_                        = null;
  private volatile TransportMotor     transportMotor_               = null;
  
  private TransportMotorTachoTravel   transportMotorTachoTravel_    = null;

  private volatile int                lastDirectionChangeRequestID_ = 0;
  private volatile TransportDirection lastDirection_                = TransportDirection.ADVANCING;
  private volatile TransportBraking   desiredBraking_               = TransportBraking.FLOAT;
  
  public TransportBrake(LCDDisplay     display,
                        SlackSensor    slackSensor,
                        TensionSensor  tensionSensor,
                        PerfSensor     perfSensor,
                        TransportRole  initialRole,
                        Motor          motor,
                        TransportMotor transportMotor) {

    display_        = display;
    slackSensor_    = slackSensor;
    tensionSensor_  = tensionSensor;
    perfSensor_     = perfSensor;
    role_           = initialRole;
    motor_          = motor;
    transportMotor_ = transportMotor;

    transportMotorTachoTravel_ = new TransportMotorTachoTravel();
    
//     display_.updateRoleIndicator(this, role_, lastDirectionChangeRequestID_);

  }
  
  public Motor getMotor() {
    return motor_;
  }
  
  public boolean takeControl() {
    // Are we up-to-date with the direction we need to move? If not, ack the request and
    // return true so that we will brake this motor.
    if (perfSensor_.getDirectionChangeRequestID() != lastDirectionChangeRequestID_) {
      lastDirectionChangeRequestID_ = perfSensor_.getDirectionChangeRequestID();
      perfSensor_.ackDirectionChangeRequest(this, role_, lastDirectionChangeRequestID_);
      desiredBraking_ = TransportBraking.STOP;
      return true;
    }

    if (! motor_.isMoving()) {
      // If everyone has stopped and we all agree on the new direction, then 
      // change direction and note (visually and in our instance variable) that
      // we've done so.
      if (perfSensor_.directionChangeRequestUniversallyAcknowledged()) {
        TransportDirection currentDirection = perfSensor_.getCurrentDirection();
        if (currentDirection != lastDirection_) {
          role_ = role_ == TransportRole.SUPPLY
            ? TransportRole.TAKEUP
            : TransportRole.SUPPLY;
//           display_.updateRoleIndicator(this, role_, lastDirectionChangeRequestID_);
          lastDirection_ = currentDirection;
        }
      }
      // And, since we're already stopped, we just return false, i.e. we just 
      // changed direction as a side effect of the takeControl() method, not by
      // doing something in the takeAction() method.
      return false;
    }

    if (tensionSensor_.getInitialTensioningComplete() && perfSensor_.desiredPerfReached()) {
      desiredBraking_ = TransportBraking.STOP;
      return true;
    }

    // If we're going in the wrong direction, stop no matter what.
    if (lastDirection_ != perfSensor_.getRequiredDirection()) {
      desiredBraking_ = TransportBraking.STOP;
      return true;
    }

    TransportState state = Rig.getTransportState();

    if (! tensionSensor_.getInitialTensioningComplete()) {
      if (state == TransportState.TOO_SLACK) {
        // If we're too slack, then return true (brake) if we're the supply,
        // otherwise return false (don't brake) since we're the takeup.
        return role_ == TransportRole.SUPPLY;
      }
      if (state == TransportState.TOO_TENSE) {
        // If we're too tense, then return true (brake) if we're the takeup,
        // otherwise return false (don't brake) since we're the supply.
        return role_ == TransportRole.TAKEUP;
      }
    }

    if (   state == TransportState.TOO_SLACK
        || state == TransportState.TOO_TENSE) {
      desiredBraking_ = TransportBraking.STOP;
      return true;
    }

    if (role_ == TransportRole.SUPPLY) {
      // We're acting as a supply brake
      if (state == TransportState.KINDA_SLACK) {
        // if we're somewhat slack, then stop already
        if (perfSensor_.distanceToDesiredPerf() < 2)
          // Fine positioning, so stop cold
          desiredBraking_ = TransportBraking.STOP;
        else {
          transportMotor_.getPossiblyValidTachoTravel(transportMotorTachoTravel_);
          if (transportMotorTachoTravel_.valid)
            // working our way there, so float to speed things up
            desiredBraking_ = TransportBraking.FLOAT;
          else
            // don't trust ourselves yet, really, so go slowly
            desiredBraking_ = TransportBraking.STOP;
        }
        return true;
      }
      // must be kinda tense, so stay off the brakes.
      desiredBraking_ = TransportBraking.NONE;
      return false;
    } else {
      // We're acting as a takeup brake
      if (state == TransportState.KINDA_SLACK) {
        // if we're somewhat slack, stay off the brakes
        desiredBraking_ = TransportBraking.NONE;
        return false;
      }
      // must be kinda tense, so stop
      if (perfSensor_.distanceToDesiredPerf() < 2)
        // Fine positioning, so stop cold
        desiredBraking_ = TransportBraking.STOP;
      else {
          transportMotor_.getPossiblyValidTachoTravel(transportMotorTachoTravel_);
          if (transportMotorTachoTravel_.valid)
            // working our way there, so float to speed things up
            desiredBraking_ = TransportBraking.FLOAT;
          else
            // don't trust ourselves yet, really, so go slowly
            desiredBraking_ = TransportBraking.STOP;
        }
      return true;
    }
  }

  public void action() {
    display_.updateActionIndicator(role_ == TransportRole.SUPPLY
                                   ? LCDDisplay.SUPPLY_BRAKE_ACTION
                                   : LCDDisplay.TAKEUP_BRAKE_ACTION, true);
    if (desiredBraking_ == TransportBraking.NONE) {
    } else if (desiredBraking_ == TransportBraking.FLOAT) {
      motor_.flt();
    } else if (desiredBraking_ == TransportBraking.STOP) {
      motor_.stop();
    } else if (desiredBraking_ == TransportBraking.LOCK) {
      motor_.lock(100);
    }
    display_.updateActionIndicator(role_ == TransportRole.SUPPLY
                                   ? LCDDisplay.SUPPLY_BRAKE_ACTION
                                   : LCDDisplay.TAKEUP_BRAKE_ACTION, false);
  }
  
  public void suppress() {
  }
}

