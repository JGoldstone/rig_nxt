//
//  PerfSensor.java
//  Transport
//
//  Created by Joseph Goldstone on 7/6/2008.
//
//       -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
//       Copyright (c) 2008, 2009 Lilliputian Pictures LLC
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
import java.util.*;

public class PerfSensor
  extends OptoElectronicSensor
  implements OptoElectronicSensorCallback {

  private LCDDisplay									display_            = null;
  private TensionSensor								tensionSensor_      = null;

  private Network											network_            = null;
  private volatile int                desiredPerf_        = 0;
  private volatile int                currentPerf_        = 0;

  // This is used to make sure that, should we be asked to simultaneously change
  // currentPerf_ and desiredPerf_, no one will see one changed and the other not.
  private volatile Integer            perfChangeLock_           = new Integer(0);
  
  // The reason this is so elaborate is that we don't want to end up off by a perf
  // because the transport was moving while we changed direction. If we need to
  // change direction, we wait for all the TransportMotor and TransportBrake objects
  // to acknowledge that they've seen the change in direction and have stopped.
  private volatile TransportDirection currentDirection_         = TransportDirection.ADVANCING;
  private volatile TransportDirection requiredDirection_        = TransportDirection.ADVANCING;
  private volatile int                directionChangeRequestID_ = 0;
  private volatile int                supplyMotorRequestIDAckd_ = 0;
  private volatile int                takeupMotorRequestIDAckd_ = 0;
  private volatile int                supplyBrakeRequestIDAckd_ = 0;
  private volatile int                takeupBrakeRequestIDAckd_ = 0;

  private TransportMotor              takeup_                   = null;
  private TransportMotor              supply_                   = null;
		
	private volatile int								numSequentialEqualSpeeds_	= 0;

  private TransportMotorTachoTravel   takeupTravel_             = null;
  private TransportMotorTachoTravel   supplyTravel_             = null;

  public void setTakeup(TransportMotor takeup) {
    takeup_ = takeup;
  }
  
  public void setSupply(TransportMotor supply) {
    supply_ = supply;
  }
  
  public PerfSensor(LCDDisplay display, SensorPort port, TensionSensor tensionSensor) {
    super(display, port);
    display_       = display;
    tensionSensor_ = tensionSensor;
    // This is the only place where we don't synchronize on perfChangeLock_.
    //  I'm just nervous about whether it's OK to do that in a constructor.
    display_.updatePerfSensorStatus(currentPerf_, desiredPerf_);
    // These are defined as instance variables so they only get allocated once
    takeupTravel_ = new TransportMotorTachoTravel();
    supplyTravel_ = new TransportMotorTachoTravel();
  }
  
  public int getDirectionChangeRequestID() {
    return directionChangeRequestID_;
  }

  public boolean directionChangeRequestUniversallyAcknowledged() {
    return supplyMotorRequestIDAckd_ == directionChangeRequestID_
        && takeupMotorRequestIDAckd_ == directionChangeRequestID_
        && supplyBrakeRequestIDAckd_ == directionChangeRequestID_
        && takeupBrakeRequestIDAckd_ == directionChangeRequestID_;
  }
  
  private void setCurrentDirectionIfAllAckd() {
    if (directionChangeRequestUniversallyAcknowledged()) {
      currentDirection_ = requiredDirection_;
    }
  }
  
  public void ackDirectionChangeRequest(TransportMotor motor, TransportRole role, int requestIDAck) {
    if (role == TransportRole.SUPPLY) 
      supplyMotorRequestIDAckd_ = requestIDAck;
    else
      takeupMotorRequestIDAckd_ = requestIDAck;
    setCurrentDirectionIfAllAckd();
  }
  
  public void ackDirectionChangeRequest(TransportBrake brake, TransportRole role, int requestIDAck) {
    if (role == TransportRole.SUPPLY) 
      supplyBrakeRequestIDAckd_ = requestIDAck;
    else
      takeupBrakeRequestIDAckd_ = requestIDAck;
    setCurrentDirectionIfAllAckd();
  }
    
  public void setCurrentPerf(int perf) {
    synchronized(perfChangeLock_) {
      currentPerf_ = perf;
      display_.updatePerfSensorStatus(currentPerf_, desiredPerf_);
    }
  }
  
  public int getCurrentPerf() {
    synchronized(perfChangeLock_) {
      return currentPerf_;
    }
  }
  
  public int distanceToDesiredPerf() {
    synchronized(perfChangeLock_) {
      return Math.abs(desiredPerf_ - currentPerf_);
    }
  }

  // This MUST be called only with the perfChangeLock_ held.
  private void requestDirectionChangeIfRequired() {
    boolean transportMotorTachoInvalidationRequired = false;
    if (currentPerf_ < desiredPerf_) {
      requiredDirection_ = TransportDirection.ADVANCING;
      if (requiredDirection_ != currentDirection_) {
        ++directionChangeRequestID_;
        transportMotorTachoInvalidationRequired = true;
      }
    } else if (currentPerf_ > desiredPerf_) {
      requiredDirection_ = TransportDirection.REWINDING;
      if (requiredDirection_ != currentDirection_) {
        ++directionChangeRequestID_;
        transportMotorTachoInvalidationRequired = true;
      }
    }
    if (transportMotorTachoInvalidationRequired) {
      takeup_.invalidateTachoSamples();
      supply_.invalidateTachoSamples();
    }
    display_.updateDirection(requiredDirection_);
  }
  
  public void setDesiredAndCurrentPerf(int desiredPerf, int currentPerf) {
    synchronized(perfChangeLock_) {
      desiredPerf_ = desiredPerf;
      currentPerf_ = currentPerf;
      display_.updatePerfSensorStatus(currentPerf_, desiredPerf_);
    }
  }
  
  public void setDesiredPerf(int perf) {
    synchronized(perfChangeLock_) {
      desiredPerf_ = perf;
      requestDirectionChangeIfRequired();
      display_.updatePerfSensorStatus(currentPerf_, desiredPerf_);
    }
  }
  
  public int getDesiredPerf() {
    synchronized(perfChangeLock_) {
      return desiredPerf_;
    }
  }
  
  public boolean desiredPerfReached() {
    synchronized(perfChangeLock_) {
      return desiredPerf_ == currentPerf_;
    }
  }
  
  public TransportDirection getCurrentDirection() {
    return currentDirection_;
  }

  public TransportDirection getRequiredDirection() {
    return requiredDirection_;
  }

  public void setNetwork(Network network) {
    network_ = network;
  }

//   public void stateSignificantlyChanged(int oldSignificantValue, int newSignificantValue) {
//     synchronized (perfChangeLock_) {
//       if (tensionSensor_.getInitialTensioningComplete()) {
//         if (currentDirection_ == TransportDirection.ADVANCING)
//           ++currentPerf_;
//         else
//           --currentPerf_;
//       }
//       requestDirectionChangeIfRequired();
//       display_.updatePerfSensorStatus(currentPerf_, desiredPerf_);
//       takeup_.recordTachoSample();
//       supply_.recordTachoSample();
//       takeup_.getPossiblyValidTachoTravel(takeupTravel_);
//       supply_.getPossiblyValidTachoTravel(supplyTravel_);
// 			int takeupRequestedSpeed = takeup_.getRequestedSpeed();
// 			int supplyRequestedSpeed = supply_.getRequestedSpeed();
// 			display_.updateTachoCountsPerRequestedSpeeds(takeupTravel_, takeupRequestedSpeed, supplyTravel_, supplyRequestedSpeed);
//       if (takeupTravel_.valid && supplyTravel_.valid) {
//         if (takeupRequestedSpeed == TransportMotor.MAXIMUM_MOTOR_SPEED) {
// 					if (supplyRequestedSpeed == takeupRequestedSpeed) { // typically when we're in the middle of a run
// 						++numSequentialEqualSpeeds_;
// 						if (numSequentialEqualSpeeds_ == 10) { // if we've seen this for 10 perfs in a row
// 							takeup_.setSpeed(Math.round(0.95F * supplyRequestedSpeed)); // and let supply speed drive
// 							numSequentialEqualSpeeds_ = 0;
// 						}
// 					} else {
// 						numSequentialEqualSpeeds_ = 0; // reset
// 						int     rawNewSupplySpeed = (takeupRequestedSpeed * supplyTravel_.value) / takeupTravel_.value;
// 						int clampedNewSupplySpeed = Math.min(Math.max(TransportMotor.MINIMUM_MOTOR_SPEED, rawNewSupplySpeed), TransportMotor.MAXIMUM_MOTOR_SPEED);
// 						supply_.setSpeed(clampedNewSupplySpeed);
// 					}
//         } else if (supplyRequestedSpeed == TransportMotor.MAXIMUM_MOTOR_SPEED) {
// 					if (takeupRequestedSpeed == supplyRequestedSpeed) {
// 						++numSequentialEqualSpeeds_;
// 						if (numSequentialEqualSpeeds_ == 10) {
// 							supply_.setSpeed(Math.round(0.95F * takeupRequestedSpeed));
// 							numSequentialEqualSpeeds_ = 0;
// 						}
// 					} else {
// 							numSequentialEqualSpeeds_ = 0;
// 							int     rawNewTakeupSpeed = (supplyRequestedSpeed * takeupTravel_.value) / supplyTravel_.value;
// 							int clampedNewTakeupSpeed = Math.min(Math.max(TransportMotor.MINIMUM_MOTOR_SPEED, rawNewTakeupSpeed), TransportMotor.	MAXIMUM_MOTOR_SPEED);
// 							takeup_.setSpeed(clampedNewTakeupSpeed);
// 					}
//         }
//       }
//     }
//     if (network_ != null)
//       network_.queueTransmission();
//   }
    
  public void stateSignificantlyChanged(int oldSignificantValue, int newSignificantValue) {
    synchronized (perfChangeLock_) {
      if (tensionSensor_.getInitialTensioningComplete()) {
        if (currentDirection_ == TransportDirection.ADVANCING)
          ++currentPerf_;
        else
          --currentPerf_;
      }
      requestDirectionChangeIfRequired();
      display_.updatePerfSensorStatus(currentPerf_, desiredPerf_);
      takeup_.recordTachoSample();
      supply_.recordTachoSample();
      takeup_.getPossiblyValidTachoTravel(takeupTravel_);
      supply_.getPossiblyValidTachoTravel(supplyTravel_);
			int takeupRequestedSpeed = takeup_.getRequestedSpeed();
			int supplyRequestedSpeed = supply_.getRequestedSpeed();
			display_.updateTachoCountsPerRequestedSpeeds(takeupTravel_, takeupRequestedSpeed, supplyTravel_, supplyRequestedSpeed);
      if (takeupTravel_.valid && supplyTravel_.valid) {
        if (currentDirection_ == TransportDirection.ADVANCING) {
          float takeupPerSupplyRatio = (float)takeupTravel_.value / supplyTravel_.value;
          if (takeupPerSupplyRatio < 1.0F) {
            supply_.setSpeed(Math.round(TransportMotor.MAXIMUM_MOTOR_SPEED));
            takeup_.setSpeed(Math.round(TransportMotor.MAXIMUM_MOTOR_SPEED * takeupPerSupplyRatio));
          } else if (takeupPerSupplyRatio > 1.0F) {
            takeup_.setSpeed(Math.round(TransportMotor.MAXIMUM_MOTOR_SPEED));
            supply_.setSpeed(Math.round(TransportMotor.MAXIMUM_MOTOR_SPEED / takeupPerSupplyRatio));
          }
        } else { // rewinding then
          float supplyPerTakeupRatio = (float)supplyTravel_.value / takeupTravel_.value;
          if (supplyPerTakeupRatio < 1.0F) {
            takeup_.setSpeed(Math.round(TransportMotor.MAXIMUM_MOTOR_SPEED));
            supply_.setSpeed(Math.round(TransportMotor.MAXIMUM_MOTOR_SPEED * supplyPerTakeupRatio));
          } else if (supplyPerTakeupRatio > 1.0F) {
            supply_.setSpeed(Math.round(TransportMotor.MAXIMUM_MOTOR_SPEED));
            takeup_.setSpeed(Math.round(TransportMotor.MAXIMUM_MOTOR_SPEED / supplyPerTakeupRatio));
          }
        }
      }
    }
    if (network_ != null)
      network_.queueTransmission();
  }
    
  public void writeState(DataOutputStream s) throws IOException{
    // Avoid holding a lock during what might be blocking I/O
    int desiredPerf = 0;
    int currentPerf = 0;
    synchronized(perfChangeLock_) {
      desiredPerf = getDesiredPerf();
      currentPerf = getCurrentPerf();
    }
    // Still possible that these will be out of date by the time
    // they are sent, but at least they'll be a self-consistent
    // out-of-date pair.
    s.writeInt(desiredPerf);
    s.writeInt(currentPerf);
  }
}
