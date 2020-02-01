//
//  BehaviorDispatcher.java
//  Transport
//
//  Created by Joseph Goldstone on 1/20/2009.
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

import lejos.robotics.subsumption.Behavior;

public class BehaviorDispatcher {
	private Behavior[]	behavior_;
	private boolean			returnWhenIdle_;

  private class IdleBehavior implements Behavior {
    public boolean takeControl() { return true; }
    public void action() {}
    public void suppress() {}
  }
      
  private static final int IDLE_BEHAVIOR = 0;
  
	public BehaviorDispatcher(Behavior[] behavior, boolean returnWhenIdle) {
    behavior_ = new Behavior[behavior.length + 1];
    behavior_[IDLE_BEHAVIOR] = new IdleBehavior();
    for (int i = 0, j = 1; i < behavior.length; ++i, ++j)
      behavior_[j] = behavior[i];
		returnWhenIdle_	= returnWhenIdle;
	}
	
	public BehaviorDispatcher(Behavior[] behavior) {
		this(behavior, true);
	}
	
  private Scheduler scheduler = new Scheduler();
  
  private volatile int currentBehavior_ = 0;
  private volatile int scheduledBehavior_ = 0;

  public void setCurrentBehavior(int behavior) {
    currentBehavior_ = behavior;
  }
  
  public void setScheduledBehavior(int behavior) {
    scheduledBehavior_ = behavior;
  }
  
  public void dispatchLoop() {
    int priorBehavior = IDLE_BEHAVIOR;
    currentBehavior_ = IDLE_BEHAVIOR;
    scheduler.start();
    while (true) {

      synchronized(scheduler) {
        // This synchronized block is the only place in this thread where scheduledBehavior_
        // is read; it is also the only place in this thread where currentBehavior_ is written.
        if (currentBehavior_ == IDLE_BEHAVIOR && priorBehavior != IDLE_BEHAVIOR && returnWhenIdle_)
          return;
        setCurrentBehavior(scheduledBehavior_);
      }

      if (currentBehavior_ != priorBehavior)
      {
        behavior_[currentBehavior_].action();
        priorBehavior = currentBehavior_;
      }
      Thread.yield();
    }
  }

  private class Scheduler extends Thread {

    private int getHighestPriorityRequestingControl() {
      for (int i = behavior_.length - 1; i >= IDLE_BEHAVIOR; --i)
        if (behavior_[i].takeControl())
          return i;
      // Unless the compiler has mighty powers of static analysis indeed,
      // it's going to complain if we leave the next statement out.
      // That said, because IdleBehavior's takeAction() always returns true,
			// and there is ALWAYS an IdleBehavior in there, then the statement
			// below will never be reached.
      return IDLE_BEHAVIOR;
    }

    public Scheduler() {
      setDaemon(true);
    }

    public void run() {
      while (true) {
        boolean behaviorSuppressionNeeded = false;
        // We give it an initial value only to shut up the compiler. The only path through
        // the code that uses this always initializes it before use, but static analysis, again,
        // isn't convinced or isn't happening at all.
        int behaviorToSuppress = IDLE_BEHAVIOR;

        synchronized(this) {
          // This synchronized block is the only place in this thread where where currentBehavior_
          // is read; it is also the only place in this thread where scheduledBehavior_ is written.
          int behaviorRequestingControl = getHighestPriorityRequestingControl();
          if (behaviorRequestingControl > currentBehavior_) {
            behaviorSuppressionNeeded = true;
            behaviorToSuppress = currentBehavior_;
          }
          setScheduledBehavior(behaviorRequestingControl);
        }

        if (behaviorSuppressionNeeded)
          behavior_[behaviorToSuppress].suppress();
        Thread.yield();
      }
    }
  }
}
