//
//  Network.java
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

import lejos.robotics.subsumption.*;
import lejos.nxt.comm.*;
import lejos.nxt.*;
import lejos.util.*;
import java.io.*;
import java.util.*;

public class Network extends Thread {

	
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	// References to other rig objects
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	
  // So we can display information about the network connection
  private LCDDisplay								display_											= null;

  // So we can get, then send to the host, info on these other objects
  private PerfSensor								perfSensor_										= null;
  private FilterSensor							filterSensor_									= null;
  private TensionSensor							tensionSensor_								= null;
  private SlackSensor								slackSensor_									= null;
  private TransportMotor						takeupMotor_									= null;
  private TransportBrake						takeupBrakes_									= null;
  private TransportMotor						supplyMotor_									= null;
  private TransportBrake						supplyBrakes_									= null;
  private FilterMotor								filterMotor_									= null;
  private FilterBrakes							filterBrakes_									= null;
	
	
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	// Communication between threads
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 

	// Shared between the heartbeat timer and the status update loop
  private int timebase_ = 0;
	private synchronized int getTimebase() {
		return timebase_;
	}
	private synchronized void setTimebase(int timebase) {
		timebase_ = timebase;
	}
	
  private int lastStatusUpdateStartTime_ = 0;
	private synchronized int getLastStatusUpdateStartTime() {
		return lastStatusUpdateStartTime_;
	}
	private synchronized void setLastStatusUpdateStartTime(int lastStatusUpdateStartTime) {
		lastStatusUpdateStartTime_ = lastStatusUpdateStartTime;
	}
	
	// Shared between the status update loop and the request loop
  private volatile int							lastRequestSerialNumberRcvd_  = 0;
	
	// Shared between the request loop and the Network instance
  private volatile Integer					statusUpdateLoopMonitor_			= null;
	
	// Shared between the status update loop, the request loop and the Network instance
  private volatile StatusUpdateLoop	statusUpdateLoop_							= null;

	// Shared between the USB and Bluetooth Pander threads and the Network instance

  // initialized in Network's run() method before Panders are created
	private NXTConnection    tetheredConnection_;
  private synchronized NXTConnection tetheredConnection() {
    return tetheredConnection_;
  }
  private synchronized void setTetheredConnection(NXTConnection connection) {
    tetheredConnection_ = connection;
  }

  // initialized in Network's run() method before Panders are created
	private NXTConnection    untetheredConnection_;
  private synchronized NXTConnection untetheredConnection() {
    return untetheredConnection_;
  }
  private synchronized void setUntetheredConnection(NXTConnection connection) {
    untetheredConnection_ = connection;
  }
	
	
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	// Thread communicating status back to host (so nothing that needs to communicate
	// that status ever need block on i/o)
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	
	private class StatusUpdateLoop extends Thread {
    private Network						network_;
		private DataInputStream		in_;
		private DataOutputStream	out_;

		private int								lastRequestSerialNumberAckd_         = 0;
		private int								lastStatusUpdateSubSerialNumberSent_ = 0;
		
		public StatusUpdateLoop(Network network, DataInputStream in, DataOutputStream out) {
      super();
      network_ = network;
			in_      = in;
			out_     = out;
		}
		
    public void send() throws IOException {
      final int STATUS_UPDATE_START_MARKER = 0x0C01FFED;
			final int STATUS_UPDATE_END_MARKER   = 0x0000BABE;
			
      // response 'on-the-wire' format
      //   response start marker (int)
      //   NXT ms since Rig start (int)
      //   battery voltage in millivolts (int)
      //   ackd request serial number (int)
      //   response sub-serial number (int)
      //   perf sensor state
      //     desired perf (int)
      //     current perf (int)
      //   slack sensor state
      //     too slack (int)
      //   tension sensor state
      //     was initially tensioned? (int)
      //     too tense (int)
      //   supply motor state
      //   takeup motor state
      //   filter sensor state
      //     desired filter (int)
      //     current filter (int)
      //   filter motor state
      //   response end marker (int)
			
			setLastStatusUpdateStartTime((int)System.currentTimeMillis() - getTimebase());
      
      out_.writeInt(STATUS_UPDATE_START_MARKER);
      out_.writeInt(getLastStatusUpdateStartTime());
      out_.writeInt(Battery.getVoltageMilliVolt());
      if (lastRequestSerialNumberAckd_ != lastRequestSerialNumberRcvd_)
        lastStatusUpdateSubSerialNumberSent_ = 0;
      out_.writeInt(lastRequestSerialNumberRcvd_);
      lastRequestSerialNumberAckd_ = lastRequestSerialNumberRcvd_;
      out_.writeInt(lastStatusUpdateSubSerialNumberSent_++);
      perfSensor_.writeState(out_);
      slackSensor_.writeState(out_);
      tensionSensor_.writeState(out_);
      supplyMotor_.writeState(out_);
      takeupMotor_.writeState(out_);
      filterSensor_.writeState(out_);
      filterMotor_.writeState(out_);
      out_.writeInt(STATUS_UPDATE_END_MARKER);
      out_.flush();
    }

		public void run() {
      while (true) {
        try {
          synchronized(this) {
            try {
              wait();
            } catch (InterruptedException ignored) {}
          }
          send();
	} catch (IOException e) {
	    try {
		CrashLog log = new CrashLog("StatusUpdateCrash.log");
		log.appendRecord(e.toString());
	    } catch (IOException ignored) {}
          // If we get here, it is because either the host closed the
          // stream on us while we were rewriting, or 'something else
          // bad happened'.
          // When we return, the status update thread terminates,
          // which causes the Network thread to close the streams
          // (possibly a second time; such errors are caught and
          // ignored) and to close the connection to the client, and to
          // wait for the client to re-connect.
          try {
						in_.close();  // suicide pact with sibling
					} catch (IOException ignored) {};
          synchronized(network_) {
            network_.notify(); // suicide pact with parent
          }
        }
			}
		}
	}
  
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	// Thread pulling in requests from the host (so no other process ever ends up
	// waiting while such requests are coming in bit-by-bit)
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	
	private class RequestLoop extends Thread {
		
		// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
		// Inner inner class to hold decode and hold request data
		// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
		private class Request {
			private int serialNumber_;
			private int type_;
			private int perf_;
			private int filter_;
			
			public void receive(DataInputStream s) throws IOException {
				final int REQUEST_START_MARKER = 0x0000CAFE;
				final int REQUEST_END_MARKER   = 0x00C0FFEE;
				
				// request 'on-the-wire' format
				//   request start marker (int)
				//   request serial number (int)
				//   request type (int)
				//   adopted/requested perf (int)
				//   requested filter (int)
				//   request end marker (int)
				
				if (s.readInt() != REQUEST_START_MARKER)
					throw new IOException("missing or damaged request start");
				serialNumber_= s.readInt();
				type_ = s.readInt();
				perf_ = s.readInt();
				filter_ = s.readInt();
				if (s.readInt() != REQUEST_END_MARKER)
					throw new IOException("missing or damaged request end");
			}
			
			int getSerialNumber() {
				return serialNumber_;
			}
			
			int getType() {
				return type_;
			}
			
			int getPerf() {
				return perf_;
			}
			
			int getFilter() {
				return filter_;
			}
		}
		
    private Network           network_;
    private DataInputStream		in_;
		private DataOutputStream  out_;
    private int               requestsRcvd_;
		private Request						request_;
		
    public RequestLoop(Network network, DataInputStream in, DataOutputStream out, StatusUpdateLoop statusUpdateLoop) {
      super();
      network_ = network;
      in_ = in;
			out_ = out;
      requestsRcvd_ = 0;
      request_ = new Request();
    }

		private static final int REQUEST_SEND_STATUS_UPDATE           = 0;
		private static final int REQUEST_ADOPT_CURRENT_AS_PERF        = 1;
		private static final int REQUEST_SET_DESIRED_PERF             = 2;
		private static final int REQUEST_SET_DESIRED_FILTER           = 3;
		private static final int REQUEST_SET_DESIRED_FILTER_AND_PERF  = 4;
		
    public void run() {
      try {
        while(true) {
					request_.receive(in_);
          display_.updateRequestsRcvd(++requestsRcvd_);
          lastRequestSerialNumberRcvd_ = request_.getSerialNumber();
					if (request_.getType() == REQUEST_SEND_STATUS_UPDATE) {
						; // just fall through
					} else if (request_.getType() == REQUEST_ADOPT_CURRENT_AS_PERF) {
						perfSensor_.setDesiredAndCurrentPerf(request_.getPerf(), request_.getPerf());
					} else if (request_.getType() == REQUEST_SET_DESIRED_PERF) {
						perfSensor_.setDesiredPerf(request_.getPerf());
          } else if (request_.getType() == REQUEST_SET_DESIRED_FILTER) {
						filterSensor_.setDesiredFilter(request_.getFilter());
					} else if (request_.getType() == REQUEST_SET_DESIRED_FILTER_AND_PERF) {
						perfSensor_.setDesiredPerf(request_.getPerf());
						filterSensor_.setDesiredFilter(request_.getFilter());
					}
          synchronized(statusUpdateLoopMonitor_) {
            if (statusUpdateLoop_ != null)
              synchronized(statusUpdateLoop_) {
                statusUpdateLoop_.notify();
              }
          }
        }
      } catch (IOException e) {
	  try {
	      CrashLog log = new CrashLog("RequestLoopCrash.log");
	      log.appendRecord(e.toString());
	  } catch (IOException ignored) {}
        // If we get here, it is because either someone else closed the
        // stream on us in mid-read, and our read threw an IOException, or
        // because we read something malformed. When we return, the 
        // request assembly thread terminates, which causes the Network
        // thread to close the streams (possibly a second time; such
        // errors are caught and ignored) and to close the connection to
        // the client, and to wait for the client to re-connect.
				try { out_.close(); } catch (IOException ignored) {}; // suicide pact with sibling
        synchronized(network_) {
          network_.notify(); // suicide pact with parent
        }
      }
    }
  }
	
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	// Thread handling connections to other rig objects, setup and takedown of the
	// connection to the host, providing a single point of contact for those other
	// rig objects, etc.
	// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
	
	
  public Network(LCDDisplay     display,
                 PerfSensor     perfSensor,
                 SlackSensor    slackSensor,
                 TensionSensor  tensionSensor,
                 TransportMotor takeupMotor,
                 TransportBrake takeupBrakes,
                 TransportMotor supplyMotor,
                 TransportBrake supplyBrakes,
                 FilterSensor   filterSensor,
                 FilterMotor    filterMotor,
                 FilterBrakes   filterBrakes) {

		super();
    display_ = display;
    
    display_.updateConnected(false, false, LCDDisplay.NO_CONNECTION_ACTIVE);
    display_.updateRequestsRcvd(0);
    display_.updateStatusUpdatesQueued(0);

    perfSensor_           = perfSensor;
    slackSensor_          = slackSensor;
    tensionSensor_        = tensionSensor;
    takeupMotor_          = takeupMotor;
    takeupBrakes_         = takeupBrakes;
    supplyMotor_          = supplyMotor;
    supplyBrakes_         = supplyBrakes;
    filterSensor_         = filterSensor;
    filterMotor_          = filterMotor;
    filterBrakes_         = filterBrakes;
    
    setTimebase((int)System.currentTimeMillis());

    statusUpdateLoopMonitor_ = new Integer(0);
    statusUpdateLoop_        = null;
  }
	
  private int statusUpdatesQueued_ = 0;

	public void queueTransmission() {
    synchronized(statusUpdateLoopMonitor_) {
      if (statusUpdateLoop_ != null)
        synchronized(statusUpdateLoop_) {
          statusUpdateLoop_.notify();
        }
    }
    display_.updateStatusUpdatesQueued(++statusUpdatesQueued_);
	}
	
	public void run() {
		
		// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
		// send status updates to the rig without prompting
		// -=-=- -=-=- -=-=- -=-=- -=-=- -=-=- 
		class Heartbeat implements TimerListener {
			public void timedOut() {
        display_.updateBatteryVoltage(Battery.getVoltageMilliVolt());
				// only bother sending something if we haven't sent an update in the last half-second
				if (((int)System.currentTimeMillis() - getTimebase()) - getLastStatusUpdateStartTime() > 500) {
					queueTransmission();
				}
			}
		}
		
		abstract class Pander extends Thread {
			Network network_;
			public Pander(Network network) {
				network_ = network;
			}
			public abstract NXTConnection acceptConnection();
			public abstract NXTConnection getConnection();
			public abstract void setConnection(NXTConnection connection);
			public void run() {
				while (true) {
					NXTConnection acceptedConnection = null;
					if (getConnection() == null) {
						acceptedConnection = acceptConnection();
						if (acceptedConnection != null)
							setConnection(acceptedConnection);
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ignored) {}
				}
			}
		}
		
		class TetheredPander extends Pander {
			public TetheredPander(Network network) {
				super(network);
			}
			public NXTConnection acceptConnection() {
				return USB.waitForConnection();
			}
			public NXTConnection getConnection() {
        return tetheredConnection();
      }
			public void setConnection(NXTConnection connection) {
        setTetheredConnection(connection);
			}
		}
		
		class UntetheredPander extends Pander {
			public UntetheredPander(Network network) {
				super(network);
			}
			public NXTConnection acceptConnection() {
				return Bluetooth.waitForConnection();
			}
			public NXTConnection getConnection() {
        return untetheredConnection();
			}
			public void setConnection(NXTConnection connection) {
        setUntetheredConnection(connection);
			}
		}
		
    tetheredConnection_   = null;
    untetheredConnection_ = null;
    
		TetheredPander     tetheredPander = new   TetheredPander(this);
		UntetheredPander untetheredPander = new UntetheredPander(this);
		tetheredPander.start();
		untetheredPander.start();
		
		NXTConnection activeConnection = null;
		
    display_.updateConnected(false, false, LCDDisplay.NO_CONNECTION_ACTIVE);
    
		while (true) {
			// If we're waiting around for a connection, don't run through the loop more than 10X/sec
			try {
				sleep(100);
			} catch (InterruptedException ignored) {}
      if (activeConnection == null) {
        // prefer USB to Bluetooth when the former is available
        if ((activeConnection = tetheredConnection()) != null)
          display_.updateConnected(true, untetheredConnection() != null, LCDDisplay.USB_CONNECTION_ACTIVE);
        else if ((activeConnection = untetheredConnection()) != null)
          display_.updateConnected(false, true, LCDDisplay.BT_CONNECTION_ACTIVE);
        else
          display_.updateConnected(false, false, LCDDisplay.NO_CONNECTION_ACTIVE);
			}
			if (activeConnection != null) {
				NXTConnection sharedConnection = activeConnection;
				DataInputStream fromHost = sharedConnection.openDataInputStream();
				DataOutputStream  toHost = sharedConnection.openDataOutputStream();
				statusUpdateLoop_ = new StatusUpdateLoop(this, fromHost, toHost);
				RequestLoop requestLoop = new RequestLoop(this, fromHost, toHost, statusUpdateLoop_);
				requestLoop.start();
				statusUpdateLoop_.start();
				// units for this are msec
				Timer heartbeatTimer = new Timer(200, new Heartbeat());
				heartbeatTimer.start();
				// quite possibly sit here quietly for an entire run; when the client
				// closes the connection, the request loop thread and/or the update 
				// loop thread will close the sibling connection and notify us.
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException ignored) {
					} finally {
						synchronized(statusUpdateLoopMonitor_) {
							statusUpdateLoop_ = null;
						}
					}
				}
				heartbeatTimer.stop();
				// Most likely these are redundant actions, but it's possible that a
				// thread died from some exception other than I/O, in which case, 
				// although it will cause its sibling to close the 'other' stream,
				// the stream of the process that hit the non-IOException exception
				// might still be open. So close it, possibly redundantly.
				try { fromHost.close(); } catch (IOException ignored) {}
				try { toHost.close(); } catch (IOException ignored) {}
				activeConnection.close();
				synchronized(this) {
					if (sharedConnection == tetheredConnection())
						setTetheredConnection(null);
					else if (sharedConnection == untetheredConnection())
						setUntetheredConnection(null);
					display_.updateConnected(tetheredConnection() == null, untetheredConnection() == null, LCDDisplay.NO_CONNECTION_ACTIVE);
					activeConnection = null;
				}
			}
		}
	}
	
}
