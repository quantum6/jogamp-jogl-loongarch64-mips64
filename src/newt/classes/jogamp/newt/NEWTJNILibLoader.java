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

package jogamp.newt;

import java.security.PrivilegedAction;

import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.SecurityUtil;
import com.jogamp.common.util.cache.TempJarCache;

public class NEWTJNILibLoader extends JNILibLoaderBase {
    /**
     * Loads the NEWT native library for the main head display,
     * i.e. X11 for Unix, GDI for Windows .. and so forth.
     */
    public static boolean loadNEWTHead() {
        return SecurityUtil.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                Platform.initSingleton();
                final String libName = "newt_head";
                if( TempJarCache.isInitialized(true) && null == TempJarCache.findLibrary(libName) ) {
                    JNILibLoaderBase.addNativeJarLibsJoglCfg(new Class<?>[] { jogamp.nativewindow.Debug.class, jogamp.newt.Debug.class });
                }
                return Boolean.valueOf(loadLibrary(libName, false, NEWTJNILibLoader.class.getClassLoader()));
            }
        }).booleanValue();
    }
    /**
     * Loads the NEWT native library for the drm/gbm display.
     */
    public static boolean loadNEWTDrmGbm() {
        return SecurityUtil.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                Platform.initSingleton();
                final String libName = "newt_drm";
                if( TempJarCache.isInitialized(true) && null == TempJarCache.findLibrary(libName) ) {
                    JNILibLoaderBase.addNativeJarLibsJoglCfg(new Class<?>[] { jogamp.nativewindow.Debug.class, jogamp.newt.Debug.class });
                }
                return Boolean.valueOf(loadLibrary(libName, false, NEWTJNILibLoader.class.getClassLoader()));
            }
        }).booleanValue();
    }
}
