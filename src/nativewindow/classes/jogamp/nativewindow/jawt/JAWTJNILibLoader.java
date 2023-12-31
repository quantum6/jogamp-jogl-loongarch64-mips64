/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package jogamp.nativewindow.jawt;

import com.jogamp.common.util.SecurityUtil;
import com.jogamp.nativewindow.NativeWindowFactory;

import jogamp.nativewindow.NWJNILibLoader;

import java.awt.Toolkit;
import java.security.PrivilegedAction;

public class JAWTJNILibLoader extends NWJNILibLoader {
  static {
    SecurityUtil.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        // Make sure that awt.dll is loaded before loading jawt.dll. Otherwise
        // a Dialog with "awt.dll not found" might pop up.
        // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4481947.
        Toolkit.getDefaultToolkit();

        // Must pre-load JAWT on all non-Mac platforms to
        // ensure references from jogl_awt shared object
        // will succeed since JAWT shared object isn't in
        // default library path
        if ( NativeWindowFactory.TYPE_MACOSX != NativeWindowFactory.getNativeWindowType(false) ) {
            try {
                loadLibrary("jawt", null, true, JAWTJNILibLoader.class.getClassLoader());
            } catch (final Throwable t) {
                // It might be ok .. if it's already loaded
                if(DEBUG) {
                    t.printStackTrace();
                }
            }
        }
        return null;
      }
    });
  }

  public static void initSingleton() {
      // just exist to ensure static init has been run
  }

}
