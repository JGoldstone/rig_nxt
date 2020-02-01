//
//  Rig.java
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

public class Rig {

  public static final Motor TAKEUP_MOTOR = Motor.A;
  public static final Motor SUPPLY_MOTOR = Motor.C;
  public static final Motor FILTER_MOTOR = Motor.B;

  private static final SensorPort SLACK_SENSOR_PORT   = SensorPort.S1;
  private static final SensorPort TENSION_SENSOR_PORT = SensorPort.S2;
  private static final SensorPort PERF_SENSOR_PORT    = SensorPort.S3;
  private static final SensorPort FILTER_SENSOR_PORT  = SensorPort.S4;
  
  private static LCDDisplay								display_								= null;
	private static volatile SlackSensor			slackSensor_						= null;
	private static volatile TensionSensor		tensionSensor_					= null;
	private static volatile TransportState	transportState_					= TransportState.TOO_SLACK;
  private RigBehaviorDispatcher						rigBehaviorDispatcher_	= null;

  Rig() {
		
    display_ = new LCDDisplay();
    
    slackSensor_ = new SlackSensor(display_, SLACK_SENSOR_PORT);
    tensionSensor_ = new TensionSensor(display_, TENSION_SENSOR_PORT);

		updateTransportState();
    display_.updateTransportState(transportState_);
		
    SLACK_SENSOR_PORT.addSensorPortListener(slackSensor_);
    TENSION_SENSOR_PORT.addSensorPortListener(tensionSensor_);
    
    PerfSensor   perfSensor = new PerfSensor(display_, PERF_SENSOR_PORT, tensionSensor_);
    PERF_SENSOR_PORT.addSensorPortListener(perfSensor);

    FilterSensor filterSensor = new FilterSensor(display_, FILTER_SENSOR_PORT);

    TransportMotor takeupMotor  = new TransportMotor(display_, slackSensor_, tensionSensor_, perfSensor, TransportRole.TAKEUP, TAKEUP_MOTOR);
    TransportBrake takeupBrake  = new TransportBrake(display_, slackSensor_, tensionSensor_, perfSensor, TransportRole.TAKEUP, TAKEUP_MOTOR, takeupMotor);

    TransportMotor supplyMotor  = new TransportMotor(display_, slackSensor_, tensionSensor_, perfSensor, TransportRole.SUPPLY, SUPPLY_MOTOR);
    TransportBrake supplyBrake  = new TransportBrake(display_, slackSensor_, tensionSensor_, perfSensor, TransportRole.SUPPLY, SUPPLY_MOTOR, supplyMotor);

    perfSensor.setTakeup(takeupMotor);
    perfSensor.setSupply(supplyMotor);

    FilterMotor   filterMotor   = new FilterMotor(display_, filterSensor, FILTER_MOTOR);
    FilterBrakes  filterBrakes  = new FilterBrakes(display_, filterSensor, FILTER_MOTOR);
    
    Network network = new Network(display_,
                                  perfSensor,
                                  slackSensor_, tensionSensor_,
                                  takeupMotor, takeupBrake,
                                  supplyMotor, supplyBrake,
                                  filterSensor,
                                  filterMotor, filterBrakes);

    perfSensor.setNetwork(network);
    filterSensor.setNetwork(network);
    slackSensor_.setNetwork(network);
    tensionSensor_.setNetwork(network);

    network.start();
    
    Console console = new Console(display_, supplyMotor, takeupMotor, filterMotor);
    
    Behavior[] behaviors = {
      // IdleBehavior // 0
      filterMotor,       // 1 
      takeupMotor,       // 2
      supplyMotor,       // 3
      filterBrakes,      // 4
      supplyBrake,       // 5
      takeupBrake,       // 6
      console            // 7
    };
    
    rigBehaviorDispatcher_ = new RigBehaviorDispatcher(display_, behaviors, false);
  }
	
	public static void updateTransportState() {
		boolean slackSensorOn   = slackSensor_.getSlack();
		boolean tensionSensorOn = tensionSensor_.getTense();
		if ((! slackSensorOn) && (! tensionSensorOn))
			transportState_ = TransportState.TOO_SLACK;
		else if (slackSensorOn && (! tensionSensorOn))
			transportState_ = TransportState.KINDA_SLACK;
		else if (slackSensorOn && tensionSensorOn)
			transportState_ = TransportState.KINDA_TENSE;
		else 
			transportState_ = TransportState.TOO_TENSE;
		display_.updateTransportState(transportState_);
	}

	public static TransportState getTransportState() {
		return transportState_;
	}
	
  public void behave() {
    rigBehaviorDispatcher_.dispatchLoop();
  }

  public static void main(String[] args) {
    Rig rig = new Rig();
    rig.behave();
  }
  
}
