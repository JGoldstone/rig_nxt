//
//  LCDDisplay.java
//  Transport
//
//  Created by Joseph Goldstone on 9/5/2008.
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

public class LCDDisplay {

  // Try splitting this across two windows, keeping this in the top as reference
  // 0000000000111111
  // 0123456789012345

  // Although we're tromping on the action indicators:
  // ub rrrr ssssssss
  // p     0 -> 99999
  // ADV nnnnn mV s c // ADV/REW, battery millivolts, scheduled and current behavior
  // S KS          IT // slack, {Kinda,Too}x{Slack,Tense}, initially tensioned, tension
  // TT  CTTSSFFD dSS // Motor A motor and brake roles (upper letter), action indicator    tops, Motor C motor and brake roles (upper letter)
  // MB  OMBMBMBE  MB // Motor A motor and brake roles (lower letter), action indicator bottoms, Motor C motor and brake roles (lower letter)
  // mot brk  mot brk // motor and brake request ID headers
  // nnn nnn  nnn nnn // motor and brake request ID values

  // EXCEPT WHEN IN CONSOLE MODE:
  // 0000000000111111
  // 0123456789012345

  // ub rrrr ssssssss
  // p     0 -> 99999
  // ADV nnnnn mV s c // ADV/REW, battery millivolts, scheduled and current behavior
  // S KS          IT // slack, {Kinda,Too}x{Slack,Tense}, initially tensioned, tension
  // TT  CTTSSFFD dSS // Motor A motor and brake roles (upper letter), action indicator    tops, Motor C motor and brake roles (upper letter)
  // MB  OMBMBMBE  MB // Motor A motor and brake roles (lower letter), action indicator bottoms, Motor C motor and brake roles (lower letter)
  // HALTED    takeup
  // unspool spool   

  private static final int NETWORK_ROW                = 0;
  private static final int PERF_ROW                   = 1;
//   private static final int FILTER_ROW                = 2;
  private static final int VOLTAGE_ROW                = 2;
  private static final int SLACK_AND_TENSION_ROW      = 3;
  private static final int ACTION_INDICATOR_UPPER_ROW = 4;
  private static final int ACTION_INDICATOR_LOWER_ROW = 5;
  public static final int CONSOLE_BANNER_ROW          = 6;
  public static final int CONSOLE_CONTROL_ROW         = 7;

	public static final int NO_CONNECTION_ACTIVE        = 0;
	public static final int USB_CONNECTION_ACTIVE       = 1;
	public static final int BT_CONNECTION_ACTIVE        = 2;
	public void updateConnected(boolean USBConnected, boolean BTConnected, int activeConnection) {
		if (USBConnected)
			LCD.drawString(activeConnection == USB_CONNECTION_ACTIVE ? "U" : "u", 0, NETWORK_ROW);
    else
      LCD.drawString("-", 0, NETWORK_ROW);
		if (BTConnected)
			LCD.drawString(activeConnection == BT_CONNECTION_ACTIVE ? "B" : "b'", 1, NETWORK_ROW);
    else
      LCD.drawString("-", 1, NETWORK_ROW);
    LCD.refresh();
  }
  
  public void updateRequestsRcvd(int requestsRcvd) {
    if (requestsRcvd > 9999)
      LCD.drawString("####", 3, NETWORK_ROW);
    else
      LCD.drawInt(requestsRcvd, 4, 3, NETWORK_ROW);
    LCD.refresh();
  }

  public void updateStatusUpdatesQueued(int statusUpdatesQueued) {
    if (statusUpdatesQueued > 99999999)
      LCD.drawString("########", 8, NETWORK_ROW);
    else
      LCD.drawInt(statusUpdatesQueued, 8, 8, NETWORK_ROW);
    LCD.refresh();
  }

  public void updatePerfSensorStatus(int currentPerf, int desiredPerf) {
    LCD.drawString("p ", 0, PERF_ROW);
    if (currentPerf <= 99999)
      LCD.drawInt(currentPerf, 5, 2, PERF_ROW);
    else
      LCD.drawString("#####", 2, PERF_ROW);
    LCD.drawString("->", 8, PERF_ROW);
    if (desiredPerf <= 99999)
      LCD.drawInt(desiredPerf, 5, 11, PERF_ROW);
    else
      LCD.drawString("#####", 11, PERF_ROW);
    LCD.refresh();
  }
  
  public void updateBatteryVoltage(int mV) {
    LCD.drawInt(mV, 4, 5, VOLTAGE_ROW);
    LCD.drawString("mV", 10, VOLTAGE_ROW);
    LCD.refresh();
  }
  
  public void updateDirection(TransportDirection direction) {
    LCD.drawString(direction == TransportDirection.ADVANCING ? "ADV" : "REW",
                   0, VOLTAGE_ROW);
    LCD.refresh();
  }

//   public void updateFilterSensorStatus(int currentFilter, int desiredFilter) {
//     LCD.drawString("f ", 0, FILTER_ROW);
//     if (currentFilter <= 5)
//       LCD.drawInt(currentFilter, 1, 2, FILTER_ROW);
//     else
//       LCD.drawString("#", 2, FILTER_ROW);
//     LCD.drawString("->", 4, FILTER_ROW);
//     if (desiredFilter <= 5)
//       LCD.drawInt(desiredFilter, 1, 7, FILTER_ROW);
//     else
//       LCD.drawString("#", 7, FILTER_ROW);
//     LCD.refresh();
//   }
  
  public void updateScheduledBehavior(int scheduledBehavior) {
    LCD.drawInt(scheduledBehavior, 13, VOLTAGE_ROW);
    LCD.refresh();
  }
    
  public void updateCurrentBehavior(int currentBehavior) {
    LCD.drawInt(currentBehavior, 15, VOLTAGE_ROW);
    LCD.refresh();
  }
    
  public void updateSlackSensorStatus(boolean slack) {
    if (slack)
      LCD.drawString("S", 0, SLACK_AND_TENSION_ROW);
    else
      LCD.drawString(" ", 0, SLACK_AND_TENSION_ROW);
    LCD.refresh();
  }

  public void updateTensionSensorStatus(boolean tense, boolean initialTensioningComplete) {
    if (tense) {
      if (initialTensioningComplete)
        LCD.drawString("IT", 14, SLACK_AND_TENSION_ROW);
      else
        LCD.drawString(" T", 14, SLACK_AND_TENSION_ROW);
    } else {
      if (initialTensioningComplete)
        LCD.drawString("I ", 14, SLACK_AND_TENSION_ROW);
      else
        LCD.drawString("  ", 14, SLACK_AND_TENSION_ROW);
    }
    LCD.refresh();
  }

  public void updateTransportState(TransportState state) {
    if (state == TransportState.TOO_SLACK)
      LCD.drawString("TS", 2, SLACK_AND_TENSION_ROW);
    else if (state == TransportState.KINDA_SLACK)
      LCD.drawString("KS", 2, SLACK_AND_TENSION_ROW);
    else if (state == TransportState.KINDA_TENSE)
      LCD.drawString("KT", 2, SLACK_AND_TENSION_ROW);
    else if (state == TransportState.TOO_TENSE)
      LCD.drawString("TT", 2, SLACK_AND_TENSION_ROW);
		LCD.refresh();
  }
	
  // Uncomment out updateSensorValues below, and comment out updateActionIndicator, and you get this:

  // Try splitting this across two windows, keeping this in the top as reference
  // 0000000000111111
  // 0123456789012345

  // ub rrrr ssssssss
  // p     0 -> 99999
  // ADV nnnnn mV s c // ADV/REW, battery millivolts, scheduled and current behavior
  // S KS Snew TnewIT // slack, {Kinda,Too}x{Slack,Tense}, slack new value, tension new value, port initially tensioned, tension
  // TT    old  oldSS // Motor A motor and brake roles (upper letter), slack old value, tension old value, Motor C motor and brake roles (upper letter)
  // MB     PF?    MB // Motor A motor and brake roles (lower letter), perf, filter, unknown sensor, Motor C motor and brake roles (lower letter)
  // mot brk  mot brk // motor and brake request ID headers
  // nnn nnn  nnn nnn // motor and brake request ID values

// 	public void updateSensorValues(int portId, int oldVal, int newVal) {
// 		if (portId == 0) {
// 			LCD.drawString("S", 5, SLACK_AND_TENSION_ROW);
// 			LCD.drawInt(newVal, 3, 6, SLACK_AND_TENSION_ROW);
// 			LCD.drawInt(oldVal, 3, 6, ACTION_INDICATOR_UPPER_ROW);
// 		} else if (portId == 1) {
// 			LCD.drawString("T", 10, SLACK_AND_TENSION_ROW);
// 			LCD.drawInt(newVal, 3, 11, SLACK_AND_TENSION_ROW);
// 			LCD.drawInt(oldVal, 3, 11, ACTION_INDICATOR_UPPER_ROW);
// 		} else if (portId == 2) {
// 			LCD.drawString("P", 7, ACTION_INDICATOR_LOWER_ROW);
// 		} else if (portId == 3) {
// 			LCD.drawString("F", 8, ACTION_INDICATOR_LOWER_ROW);
// 		} else {
// 			LCD.drawString("?", 9, ACTION_INDICATOR_LOWER_ROW);
// 		}
// 		LCD.refresh();
// 	}
  
  public static final int DEFAULT_ACTION      = 0;
  public static final int FILTER_MOTOR_ACTION = 1;
  public static final int TAKEUP_MOTOR_ACTION = 2;
  public static final int SUPPLY_MOTOR_ACTION = 3;
  public static final int FILTER_BRAKE_ACTION = 4;
  public static final int SUPPLY_BRAKE_ACTION = 5;
  public static final int TAKEUP_BRAKE_ACTION = 6;
  public static final int CONSOLE_ACTION      = 7;

  private static final int      CONSOLE_ACTION_INDICATOR_COLUMN =  4;
  private static final int      DEFAULT_ACTION_INDICATOR_COLUMN =  5;
  private static final int TAKEUP_MOTOR_ACTION_INDICATOR_COLUMN =  6;
  private static final int TAKEUP_BRAKE_ACTION_INDICATOR_COLUMN =  7;
  private static final int SUPPLY_MOTOR_ACTION_INDICATOR_COLUMN =  8;
  private static final int SUPPLY_BRAKE_ACTION_INDICATOR_COLUMN =  9;
  private static final int FILTER_MOTOR_ACTION_INDICATOR_COLUMN = 10;
  private static final int FILTER_BRAKE_ACTION_INDICATOR_COLUMN = 11;

  private static final int MOTOR_A_ROLE_COLUMN =  0;
  private static final int BRAKE_A_ROLE_COLUMN =  1;
  private static final int MOTOR_C_ROLE_COLUMN = 14;
  private static final int BRAKE_C_ROLE_COLUMN = 15;
  
  public void updateActionIndicator(int action, boolean active) {
//     int xPos;
//     if (action == CONSOLE_ACTION) {
//       xPos = CONSOLE_ACTION_INDICATOR_COLUMN;
//       LCD.drawString(active ? "C" : " ", xPos, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString(active ? "O" : " ", xPos, ACTION_INDICATOR_LOWER_ROW);
//     } else if (action == TAKEUP_MOTOR_ACTION) {
//       xPos = TAKEUP_MOTOR_ACTION_INDICATOR_COLUMN;
//       LCD.drawString(active ? "T" : " ", xPos, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString(active ? "M" : " ", xPos, ACTION_INDICATOR_LOWER_ROW);
//     } else if (action == TAKEUP_BRAKE_ACTION) {
//       xPos = TAKEUP_BRAKE_ACTION_INDICATOR_COLUMN;
//       LCD.drawString(active ? "T" : " ", xPos, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString(active ? "B" : " ", xPos, ACTION_INDICATOR_LOWER_ROW);
//     } else if (action == SUPPLY_MOTOR_ACTION) {
//       xPos = SUPPLY_MOTOR_ACTION_INDICATOR_COLUMN;
//       LCD.drawString(active ? "S" : " ", xPos, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString(active ? "M" : " ", xPos, ACTION_INDICATOR_LOWER_ROW);
//     } else if (action == SUPPLY_BRAKE_ACTION) {
//       xPos = SUPPLY_BRAKE_ACTION_INDICATOR_COLUMN;
//       LCD.drawString(active ? "S" : " ", xPos, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString(active ? "B" : " ", xPos, ACTION_INDICATOR_LOWER_ROW);
//     } else if (action == FILTER_MOTOR_ACTION) {
//       xPos = FILTER_MOTOR_ACTION_INDICATOR_COLUMN;
//       LCD.drawString(active ? "F" : " ", xPos, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString(active ? "M" : " ", xPos, ACTION_INDICATOR_LOWER_ROW);
//     } else if (action == FILTER_BRAKE_ACTION) {
//       xPos = FILTER_BRAKE_ACTION_INDICATOR_COLUMN;
//       LCD.drawString(active ? "F" : " ", xPos, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString(active ? "B" : " ", xPos, ACTION_INDICATOR_LOWER_ROW);
//     } else if (action == DEFAULT_ACTION) {
//       xPos = DEFAULT_ACTION_INDICATOR_COLUMN;
//       LCD.drawString(active ? "D" : " ", xPos, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString(active ? "E" : " ", xPos, ACTION_INDICATOR_LOWER_ROW);
//     }
//     LCD.refresh();
  }

	StringBuffer floatDrawBuffer = new StringBuffer(16);
	
	public void updateTachoCountsPerRequestedSpeeds(TransportMotorTachoTravel takeupTravel, int takeupRequestedSpeed, TransportMotorTachoTravel supplyTravel, int supplyRequestedSpeed) {
		synchronized (this) {
			if (takeupTravel.valid)
				LCD.drawString("v", 0, ACTION_INDICATOR_LOWER_ROW);
      else
				LCD.drawString("!", 0, ACTION_INDICATOR_LOWER_ROW);
			floatDrawBuffer.delete(0, floatDrawBuffer.length());
			floatDrawBuffer.append(((float)takeupTravel.value) / takeupRequestedSpeed);
			floatDrawBuffer.delete(Math.min(5, floatDrawBuffer.length()), floatDrawBuffer.length());
			LCD.drawString(floatDrawBuffer.toString(), 1, ACTION_INDICATOR_LOWER_ROW);
			if (supplyTravel.valid)
				LCD.drawString("v", 8, ACTION_INDICATOR_LOWER_ROW);
      else
				LCD.drawString("!", 8, ACTION_INDICATOR_LOWER_ROW);
			floatDrawBuffer.delete(0, floatDrawBuffer.length());
			floatDrawBuffer.append(((float)supplyTravel.value) / supplyRequestedSpeed);
			floatDrawBuffer.delete(Math.min(5, floatDrawBuffer.length()), floatDrawBuffer.length());
			LCD.drawString(floatDrawBuffer.toString(), 9, ACTION_INDICATOR_LOWER_ROW);
		}
		LCD.refresh();
	}
		
  public void updateTachoCount(TransportMotor transportMotor, int count) {
    if (transportMotor.getMotor() == Motor.A)
      LCD.drawInt(count, 0, CONSOLE_BANNER_ROW);
    else if (transportMotor.getMotor() == Motor.C)
      LCD.drawInt(count, 8, CONSOLE_BANNER_ROW);
    LCD.refresh();
  }

  public void updateRequestedSpeed(TransportMotor transportMotor, int requestedSpeed) {
    if (transportMotor.getMotor() == Motor.A) {
      LCD.drawInt(requestedSpeed, 3, 0, CONSOLE_CONTROL_ROW);
    } else if (transportMotor.getMotor() == Motor.C) {
      LCD.drawInt(requestedSpeed, 3, 9, CONSOLE_CONTROL_ROW);
    }
    LCD.refresh();
  }

  public void updateReportedSpeed(TransportMotor transportMotor, int reportedSpeed) {
    if (transportMotor.getMotor() == Motor.A) {
      LCD.drawInt(reportedSpeed, 3, 4, CONSOLE_CONTROL_ROW);
    } else if (transportMotor.getMotor() == Motor.C) {
      LCD.drawInt(reportedSpeed, 3, 13, CONSOLE_CONTROL_ROW);
    }
    LCD.refresh();
  }

//   public void updateRoleIndicator(TransportMotor transportMotor,
//                                   TransportRole  transportRole,
//                                   int requestID) {
//     if (transportMotor.getMotor() == Motor.A) {
//       LCD.drawString("mot",     0, CONSOLE_BANNER_ROW);
//       LCD.drawInt(requestID, 3, 0, CONSOLE_CONTROL_ROW);
//       LCD.drawString(transportRole == TransportRole.SUPPLY ? "S" : "T", 
//                      MOTOR_A_ROLE_COLUMN, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString("M", MOTOR_A_ROLE_COLUMN, ACTION_INDICATOR_LOWER_ROW);
//     } else if (transportMotor.getMotor() == Motor.C) {
//       LCD.drawString("mot",     9, CONSOLE_BANNER_ROW);
//       LCD.drawInt(requestID, 3, 9, CONSOLE_CONTROL_ROW);
//       LCD.drawString(transportRole == TransportRole.SUPPLY ? "S" : "T", 
//                      MOTOR_C_ROLE_COLUMN, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString("M", MOTOR_C_ROLE_COLUMN, ACTION_INDICATOR_LOWER_ROW);
//     } else {
// //       Sound.beep();
//     }
//     LCD.refresh();
//   }
  
//   public void updateRoleIndicator(TransportBrake transportBrake,
//                                   TransportRole  transportRole,
//                                   int requestID) {
//     if (transportBrake.getMotor() == Motor.A) {
//       LCD.drawString("brk",     4, CONSOLE_BANNER_ROW);
//       LCD.drawInt(requestID, 3, 4, CONSOLE_CONTROL_ROW);
//       LCD.drawString(transportRole == TransportRole.SUPPLY ? "S" : "T", 
//                      BRAKE_A_ROLE_COLUMN, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString("B", BRAKE_A_ROLE_COLUMN, ACTION_INDICATOR_LOWER_ROW);
//     } else if (transportBrake.getMotor() == Motor.C) {
//       LCD.drawString("brk",     13, CONSOLE_BANNER_ROW);
//       LCD.drawInt(requestID, 3, 13, CONSOLE_CONTROL_ROW);
//       LCD.drawString(transportRole == TransportRole.SUPPLY ? "S" : "T", 
//                      BRAKE_C_ROLE_COLUMN, ACTION_INDICATOR_UPPER_ROW);
//       LCD.drawString("B", BRAKE_C_ROLE_COLUMN, ACTION_INDICATOR_LOWER_ROW);
//     } else {
// //       Sound.beep();
//     }
//     LCD.refresh();
//   }
  
  public void updateConsoleHaltStatus(boolean halted) {
    if (halted) {
      LCD.drawString("HALTED", 0, LCDDisplay.CONSOLE_BANNER_ROW);
    } else {
      LCD.drawString("                ",  0,  LCDDisplay.CONSOLE_BANNER_ROW);
      LCD.drawString("                ",  0,  LCDDisplay.CONSOLE_CONTROL_ROW);
    }
    LCD.refresh();
  }

  public void updateConsoleSelectedMotor(String name) {
    LCD.drawString(name, 10, LCDDisplay.CONSOLE_BANNER_ROW);
    LCD.refresh();
  }
  
  public void updateConsoleSpoolingStatus(String status) {
    LCD.drawString(status, 0, LCDDisplay.CONSOLE_CONTROL_ROW);
    LCD.refresh();
  }

}
