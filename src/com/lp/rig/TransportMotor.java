//
//  TransportMotor.java
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
import java.io.*;

public class TransportMotor 
  implements Behavior, ConsoleMotor {

  private LCDDisplay                  display_                       = null;
  private SlackSensor                 slackSensor_                   = null;
  private TensionSensor               tensionSensor_                 = null;
  private PerfSensor                  perfSensor_                    = null;
  private volatile TransportRole      role_                          = TransportRole.SUPPLY;
  private Motor                       motor_                         = null;
  
  private volatile int                lastDirectionChangeRequestID_  = 0;
  private volatile TransportDirection lastDirection_                 = TransportDirection.ADVANCING;

  public static final int MINIMUM_MOTOR_SPEED = 300;
  public static final int MAXIMUM_MOTOR_SPEED = 900;
  
  public TransportMotor(LCDDisplay    display,
                        SlackSensor   slackSensor,
                        TensionSensor tensionSensor,
                        PerfSensor    perfSensor,
                        TransportRole initialRole,
                        Motor         motor) {

    display_                       = display;
    slackSensor_                   = slackSensor;
    tensionSensor_                 = tensionSensor;
    perfSensor_                    = perfSensor;
    role_                          = initialRole;
    motor_                         = motor;
//     display_.updateRoleIndicator(this, role_, lastDirectionChangeRequestID_);

    motor_.regulateSpeed(true);
    motor_.smoothAcceleration(true);
    if (role_ == TransportRole.TAKEUP)
      motor_.setSpeed(MAXIMUM_MOTOR_SPEED);
    else
      motor_.setSpeed(MAXIMUM_MOTOR_SPEED / 2);
    motor_.stop();
  }

  public Motor getMotor() {
    return motor_;
  }
  
  public int getTachoCount() {
    return motor_.getTachoCount();
  }
  
  private static final int NUM_TACHO_SAMPLES     = 10;
  private volatile     int tachoSamples_[]       = new int[NUM_TACHO_SAMPLES];
  private volatile     int tachoSampleCount_     = 0;

  public synchronized void invalidateTachoSamples() {
    tachoSampleCount_ = 0;
  }

  public synchronized void recordTachoSample() {
    tachoSamples_[tachoSampleCount_ % NUM_TACHO_SAMPLES] = getTachoCount();
    ++tachoSampleCount_;
  }
  
  public synchronized void getPossiblyValidTachoTravel(TransportMotorTachoTravel possiblyValidTachoTravel) {
    if (tachoSampleCount_ <= NUM_TACHO_SAMPLES) {
      possiblyValidTachoTravel.valid = false;
      possiblyValidTachoTravel.value = 0;
    } else {
      possiblyValidTachoTravel.valid = true;
      int latest   = tachoSamples_[(tachoSampleCount_ - 1) % NUM_TACHO_SAMPLES];
      int earliest = tachoSamples_[(tachoSampleCount_ - NUM_TACHO_SAMPLES) % NUM_TACHO_SAMPLES];
      possiblyValidTachoTravel.value = Math.abs(latest - earliest);
    }
  }
  
  public int getReportedSpeed() {
    // avoid being confused by their convention of blending direction of
    // rotation into getRotationSpeed result (i.e. ignore negative speeds)
    return Math.abs(motor_.getRotationSpeed());
  }
  
  public int getRequestedSpeed() {
    return motor_.getSpeed();
  }

  public void setSpeed(int speed) {
    motor_.setSpeed(speed);
  }
  
  public boolean takeControl() {
    
    display_.updateTachoCount    (this, getTachoCount());
    display_.updateRequestedSpeed(this, getRequestedSpeed());
    display_.updateReportedSpeed (this, getReportedSpeed());

    // Are we up-to-date with the direction we need to move? If not, ack the request and
    // return false so others can rapidly ack this request as well.
    if (perfSensor_.getDirectionChangeRequestID() != lastDirectionChangeRequestID_) {
      lastDirectionChangeRequestID_ = perfSensor_.getDirectionChangeRequestID();
      perfSensor_.ackDirectionChangeRequest(this, role_, lastDirectionChangeRequestID_);
      return false;
    }

    // Don't start something if we know change is coming.
    if (! perfSensor_.directionChangeRequestUniversallyAcknowledged())
      return false;
    
    // Even if we need to change direction, don't take control until we've stopped first.
    if (isMoving())
      return false;

    TransportDirection currentDirection = perfSensor_.getCurrentDirection();
    if (currentDirection != lastDirection_) {
      role_ = role_ == TransportRole.SUPPLY 
        ? TransportRole.TAKEUP 
        : TransportRole.SUPPLY;
//       display_.updateRoleIndicator(this, role_, lastDirectionChangeRequestID_);
      lastDirection_ = currentDirection;
    }

    if (   tensionSensor_.getInitialTensioningComplete()
        && perfSensor_.desiredPerfReached()) {
      return false;
    }

    TransportState state = Rig.getTransportState();

    if (! tensionSensor_.getInitialTensioningComplete()) {
      if (state == TransportState.TOO_SLACK) {
        // If we're too slack, then return true (start) if we're the takeup,
        // otherwise return false (don't start) since we're the supply.
        return role_ == TransportRole.TAKEUP;
      }
      if (state == TransportState.TOO_TENSE) {
        // If we're too tense, then return true (start) if we're the supply,
        // otherwise return false (don't start) since we're the takeup.
        return role_ == TransportRole.SUPPLY;
      }
    }

    if (   role_ == TransportRole.TAKEUP
        && state == TransportState.TOO_SLACK
           && ! tensionSensor_.getInitialTensioningComplete()) {
      return true;
    }
    
    if (   state == TransportState.TOO_SLACK
        || state == TransportState.TOO_TENSE)
      // We're hosed; don't start anything new
      return false;

    TransportDirection requiredDirection = perfSensor_.getRequiredDirection();

    if (role_ == TransportRole.SUPPLY) {
      if (state == TransportState.KINDA_SLACK) {
        // We're slack; don't start and make it worse
        return false;
      } else if (state == TransportState.KINDA_TENSE) {
        // We're tense; start feeding film to make things better
        return true;
      }
    } else {
      if (state == TransportState.KINDA_SLACK) {
        // We're slack; start taking up film to make things better
        return true;
      } else if (state == TransportState.KINDA_TENSE) {
        // We're tense; don't start, since that would only make things worse
        return false;
      }
    }

    // Should never get here; beep if we do.
    Sound.beepSequenceUp();
    return false;
  }

  // The following four entries make up the ConsoleMotor protocol, used
  // to move things around during an emergency halt of the transport. We
  // also use them in our own mainstream code, although not widely.
  public boolean isMoving() {
    return motor_.isMoving();
  }

  public void spool() {
    if (motor_ == Rig.TAKEUP_MOTOR)
      motor_.backward();
    else
      motor_.forward();
  }
  
  public void unspool() {
    if (motor_ == Rig.TAKEUP_MOTOR)
      motor_.forward();
    else
      motor_.backward();
  }

  public void stop() {
    motor_.stop();
  }
  
  public void action() {
    if (role_ == TransportRole.SUPPLY) {
      display_.updateActionIndicator(LCDDisplay.SUPPLY_MOTOR_ACTION, true);
      unspool();
      display_.updateActionIndicator(LCDDisplay.SUPPLY_MOTOR_ACTION, false);
    } else {
      display_.updateActionIndicator(LCDDisplay.TAKEUP_MOTOR_ACTION, true);
      spool();
      display_.updateActionIndicator(LCDDisplay.TAKEUP_MOTOR_ACTION, false);
    }
  }
  
  public void suppress() {
  }

  public void writeState(DataOutputStream s) throws IOException {
    MotorState.write(s, motor_);
  }
  
}
