//
//  CrashLog.java
//  Transport
//
//  Created by Joseph Goldstone on 9/3/2009.
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

import java.io.*;
import lejos.nxt.*;

public class CrashLog {

  private File file_ = null;

  public CrashLog() throws IOException {
    file_ = new File("crash.log");
    file_.createNewFile();
  }
  
  public CrashLog(final String s) throws IOException {
    file_ = new File(s);
    file_.createNewFile();
  }

  public void appendRecord(final String s) {
    if (s == null)
      return;
    byte[] b = s.getBytes("US-ASCII");
    DataOutputStream stream_ = null;
    try {
      stream_ = new DataOutputStream(new FileOutputStream(file_, true));
      for (int i = 0; i < b.length; ++i)
        stream_.writeByte(b[i]);
      stream_.writeByte('\n');
      stream_.flush();
      stream_.close();
    } catch (Exception e) {
      // Just ignore everything
    }
    
  }
  
}
