/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.nativewindow.macosx;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.Point;

import java.security.PrivilegedAction;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.util.Function;
import com.jogamp.common.util.FunctionTask;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.common.util.SecurityUtil;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.ToolkitProperties;

public class OSXUtil implements ToolkitProperties {
    private static boolean isInit = false;
    private static final boolean DEBUG = Debug.debug("OSXUtil");

    private static final ThreadLocal<Boolean> tlsIsMainThread = new ThreadLocal<Boolean>();

    /** FIXME HiDPI: OSX unique and maximum value {@value} */
    public static final int MAX_PIXELSCALE = 2;

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static synchronized void initSingleton() {
      if(!isInit) {
          final boolean useMainThreadChecker = Debug.debug("OSXUtil.MainThreadChecker");
          if(DEBUG || useMainThreadChecker) {
              System.out.println("OSXUtil.initSingleton() - useMainThreadChecker "+useMainThreadChecker);
          }
          if(!NWJNILibLoader.loadNativeWindow("macosx")) {
              throw new NativeWindowException("NativeWindow MacOSX native library load error.");
          }
          if( useMainThreadChecker ) {
              final String libMainThreadChecker = "/Applications/Xcode.app/Contents/Developer/usr/lib/libMainThreadChecker.dylib";
              final NativeLibrary lib = SecurityUtil.doPrivileged(new PrivilegedAction<NativeLibrary>() {
                  @Override
                  public NativeLibrary run() {
                      return NativeLibrary.open(libMainThreadChecker, false, false, OSXUtil.class.getClassLoader(), true);
                  } } );
              if( null == lib ) {
                  System.err.println("Could not load "+libMainThreadChecker);
              } else {
                  System.err.println("Loaded "+lib);
              }
          }

          if( !initIDs0() ) {
              throw new NativeWindowException("MacOSX: Could not initialized native stub");
          }
          isInit = true;
      }
    }

    /**
     * Called by {@link NativeWindowFactory#shutdown()}
     * @see ToolkitProperties
     */
    public static void shutdown() { }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static boolean requiresToolkitLock() { return false; }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static final boolean hasThreadingIssues() { return false; }

    public static boolean isNSView(final long object) {
        return 0 != object ? isNSView0(object) : false;
    }

    public static boolean isNSWindow(final long object) {
        return 0 != object ? isNSWindow0(object) : false;
    }

    /**
     * @param windowOrView
     * @param src_x
     * @param src_y
     * @return top-left client-area position in window units
     */
    public static Point GetLocationOnScreen(final long windowOrView, final int src_x, final int src_y) {
      return (Point) GetLocationOnScreen0(windowOrView, src_x, src_y);
    }

    public static Insets GetInsets(final long windowOrView) {
      return (Insets) GetInsets0(windowOrView);
    }

    public static float GetScreenPixelScaleByDisplayID(final int displayID) {
      if( 0 != displayID ) {
          return GetScreenPixelScale1(displayID);
      } else {
          return 1.0f; // default
      }
    }
    public static float GetScreenPixelScale(final long windowOrView) {
      if( 0 != windowOrView ) {
          return GetScreenPixelScale2(windowOrView);
      } else {
          return 1.0f; // default
      }
    }
    public static float GetWindowPixelScale(final long windowOrView) {
      if( 0 != windowOrView ) {
          return GetWindowPixelScale1(windowOrView);
      } else {
          return 1.0f; // default
      }
    }
    public static void SetWindowPixelScale(final long windowOrView, final float reqPixelScale) {
      if( 0 != windowOrView ) {
          SetWindowPixelScale1(windowOrView, reqPixelScale);
      }
    }

    public static long CreateNSWindow(final int x, final int y, final int width, final int height) {
      final long res[] = { 0 };
      RunOnMainThread(true, false /* kickNSApp */, new Runnable() {
          @Override
          public void run() {
              res[0] = CreateNSWindow0(x, y, width, height);
          } } );
      return res[0];
    }
    public static void DestroyNSWindow(final long nsWindow) {
      DestroyNSWindow0(nsWindow);
    }
    public static long GetNSView(final long nsWindow) {
      return GetNSView0(nsWindow);
    }
    public static long GetNSWindow(final long nsView) {
      return GetNSWindow0(nsView);
    }

    /**
     * Create a CALayer suitable to act as a root CALayer.
     * @param width width of the CALayer in window units (points)
     * @param height height of the CALayer in window units (points)
     * @param contentsScale scale for HiDPI support: pixel-dim = window-dim x scale
     * @return the new CALayer object
     * @see #DestroyCALayer(long)
     * @see #AddCASublayer(long, long)
     */
    public static long CreateCALayer(final int width, final int height, final float contentsScale) {
      final long l = CreateCALayer0(width, height, contentsScale);
      if(DEBUG) {
          System.err.println("OSXUtil.CreateCALayer: 0x"+Long.toHexString(l)+" - "+Thread.currentThread().getName());
      }
      return l;
    }

    /**
     * Attach a sub CALayer to the root CALayer
     * <p>
     * Method will trigger a <code>display</code>
     * call to the CALayer hierarchy to enforce resource creation if required, e.g. an NSOpenGLContext.
     * </p>
     * <p>
     * Hence it is important that related resources are not locked <i>if</i>
     * they will be used for creation.
     * </p>
     * @param rootCALayer
     * @param subCALayer
     * @param x x-coord of the sub-CALayer in window units (points)
     * @param y y-coord of the sub-CALayer in window units (points)
     * @param width width of the sub-CALayer in window units (points)
     * @param height height of the sub-CALayer in window units (points)
     * @param contentsScale scale for HiDPI support: pixel-dim = window-dim x scale
     * @param caLayerQuirks
     * @see #CreateCALayer(int, int, float)
     * @see #RemoveCASublayer(long, long, boolean)
     */
    public static void AddCASublayer(final long rootCALayer, final long subCALayer,
                                     final int x, final int y, final int width, final int height,
                                     final float contentsScale, final int caLayerQuirks) {
        if(0==rootCALayer || 0==subCALayer) {
            throw new IllegalArgumentException("rootCALayer 0x"+Long.toHexString(rootCALayer)+", subCALayer 0x"+Long.toHexString(subCALayer));
        }
        if(DEBUG) {
            System.err.println("OSXUtil.AttachCALayer: caLayerQuirks "+caLayerQuirks+", 0x"+Long.toHexString(subCALayer)+" - "+Thread.currentThread().getName());
        }
        AddCASublayer0(rootCALayer, subCALayer, x, y, width, height, contentsScale, caLayerQuirks);
    }

    /**
     * Fix root and sub CALayer position to 0/0 and size
     * <p>
     * If the sub CALayer implements the Objective-C NativeWindow protocol NWDedicatedSize (e.g. JOGL's MyNSOpenGLLayer),
     * the dedicated size is passed to the layer, which propagates it appropriately.
     * </p>
     * <p>
     * On OSX/Java7 our root CALayer's frame position and size gets corrupted by its NSView,
     * hence we have created the NWDedicatedSize protocol.
     * </p>
     *
     * @param rootCALayer the root surface layer, maybe null.
     * @param subCALayer the client surface layer, maybe null.
     * @param visible TODO
     * @param width the expected width in window units (points)
     * @param height the expected height in window units (points)
     * @param caLayerQuirks TODO
     */
    public static void FixCALayerLayout(final long rootCALayer, final long subCALayer, final boolean visible, final int x, final int y, final int width, final int height, final int caLayerQuirks) {
        if( 0==rootCALayer && 0==subCALayer ) {
            return;
        }
        FixCALayerLayout0(rootCALayer, subCALayer, visible, x, y, width, height, caLayerQuirks);
    }

    /**
     * Set root and sub CALayer pixelScale / contentScale for HiDPI
     *
     * @param rootCALayer the root surface layer, maybe null.
     * @param subCALayer the client surface layer, maybe null.
     * @param contentsScale scale for HiDPI support: pixel-dim = window-dim x scale
     */
    public static void SetCALayerPixelScale(final long rootCALayer, final long subCALayer, final float contentsScale) {
        if( 0==rootCALayer && 0==subCALayer ) {
            return;
        }
        SetCALayerPixelScale0(rootCALayer, subCALayer, contentsScale);
    }

    /**
     * Detach a sub CALayer from the root CALayer.
     * @param subCALayerRelease if true, native call will issue a final {@code [subCALayerRelease release]}.
     */
    public static void RemoveCASublayer(final long rootCALayer, final long subCALayer, final boolean subCALayerRelease) {
        if(0==rootCALayer || 0==subCALayer) {
            throw new IllegalArgumentException("rootCALayer 0x"+Long.toHexString(rootCALayer)+", subCALayer 0x"+Long.toHexString(subCALayer));
        }
        if(DEBUG) {
            System.err.println("OSXUtil.DetachCALayer: 0x"+Long.toHexString(subCALayer)+" - "+Thread.currentThread().getName());
        }
        RemoveCASublayer0(rootCALayer, subCALayer, subCALayerRelease);
    }

    /**
     * Destroy a CALayer.
     * @see #CreateCALayer(int, int, float)
     */
    public static void DestroyCALayer(final long caLayer) {
        if(0==caLayer) {
            throw new IllegalArgumentException("caLayer 0x"+Long.toHexString(caLayer));
        }
        if(DEBUG) {
            System.err.println("OSXUtil.DestroyCALayer: 0x"+Long.toHexString(caLayer)+" - "+Thread.currentThread().getName());
        }
        DestroyCALayer0(caLayer);
    }

    /**
     * Run on OSX UI main thread.
     * <p>
     * 'waitUntilDone' is implemented on Java site via lock/wait on {@link RunnableTask} to not freeze OSX main thread.
     * </p>
     *
     * @param waitUntilDone
     * @param kickNSApp if <code>true</code> issues {@link #KickNSApp()}
     * @param runnable
     */
    public static void RunOnMainThread(final boolean waitUntilDone, final boolean kickNSApp, final Runnable runnable) {
        if( IsMainThread() ) {
            runnable.run(); // don't leave the JVM
        } else {
            // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
            // otherwise we may freeze the OSX main thread.
            final Object sync = new Object();
            final RunnableTask rt = new RunnableTask( runnable, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err );
            synchronized(sync) {
                RunOnMainThread0(kickNSApp, rt);
                if( waitUntilDone ) {
                    while( rt.isInQueue() ) {
                        try {
                            sync.wait();
                        } catch (final InterruptedException ie) {
                            throw new InterruptedRuntimeException(ie);
                        }
                        final Throwable throwable = rt.getThrowable();
                        if(null!=throwable) {
                            throw new RuntimeException(throwable);
                        }
                    }
                }
            }
        }
    }

    /**
     * Run later on ..
     * @param onMain if true, run on main-thread, otherwise on the current OSX thread.
     * @param runnable
     * @param delay delay to run the runnable in milliseconds
     */
    public static void RunLater(final boolean onMain, final Runnable runnable, final int delay) {
        RunLater0(onMain, false /* kickNSApp */, new RunnableTask( runnable, null, true, System.err ), delay);
    }

    /**
     * Wakes up NSApp thread by sending an empty NSEvent ..
     * <p>
     * This is deemed important <i>sometimes</i> where resources shall get freed ASAP, e.g. GL context etc.
     * </p>
     * <p>
     * The following scenarios requiring this <i>wake-up</i> are currently known:
     * <ul>
     *   <li>Destruction of an OpenGL context</li>
     *   <li>Destruction of Windows .. ?</li>
     *   <li>Stopping the NSApp</li>
     * </ul>
     * </p>
     * FIXME: Complete list of scenarios and reason it.
     */
    public static void KickNSApp() {
        KickNSApp0();
    }

    private static Runnable _nop = new Runnable() { @Override public void run() {}; };

    /** Issues a {@link #RunOnMainThread(boolean, boolean, Runnable)} w/ an <i>NOP</i> runnable, while waiting until done and issuing {@link #KickNSApp()}. */
    public static void WaitUntilFinish() {
        RunOnMainThread(true, true /* kickNSApp */, _nop);
    }

    /**
     * Run on OSX UI main thread.
     * <p>
     * 'waitUntilDone' is implemented on Java site via lock/wait on {@link FunctionTask} to not freeze OSX main thread.
     * </p>
     *
     * @param waitUntilDone
     * @param kickNSApp if <code>true</code> issues {@link #KickNSApp()}
     * @param func
     */
    public static <R,A> R RunOnMainThread(final boolean waitUntilDone, final boolean kickNSApp, final Function<R,A> func, final A... args) {
        if( IsMainThread() ) {
            return func.eval(args); // don't leave the JVM
        } else {
            // Utilize Java side lock/wait and simply pass the Runnable async to OSX main thread,
            // otherwise we may freeze the OSX main thread.
            final Object sync = new Object();
            final FunctionTask<R,A> rt = new FunctionTask<R,A>( func, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err );
            synchronized(sync) {
                rt.setArgs(args);
                RunOnMainThread0(kickNSApp, rt);
                if( waitUntilDone ) {
                    while( rt.isInQueue() ) {
                        try {
                            sync.wait();
                        } catch (final InterruptedException ie) {
                            throw new InterruptedRuntimeException(ie);
                        }
                        final Throwable throwable = rt.getThrowable();
                        if(null!=throwable) {
                            throw new RuntimeException(throwable);
                        }
                    }
                }
            }
            return rt.getResult();
        }
    }

    /**
     * Returns true if the current is the NSApplication main-thread.
     * <p>
     * Implementation utilizes a {@link ThreadLocal} storage boolean holding the answer,
     * which only gets set at the first call from each individual thread.
     * This minimizes unnecessary native callbacks.
     * </p>
     * @return {@code true} if current thread is the NSApplication main-thread, otherwise false.
     */
    public static boolean IsMainThread() {
        Boolean isMainThread = tlsIsMainThread.get();
        if( null == isMainThread ) {
            isMainThread = new Boolean(IsMainThread0());
            tlsIsMainThread.set(isMainThread);
        }
        return isMainThread.booleanValue();
    }

    /** Returns the screen refresh rate in Hz. If unavailable, returns 60Hz. */
    public static int GetScreenRefreshRate(final int scrn_idx) {
        return GetScreenRefreshRate0(scrn_idx);
    }

    private static final String getCurrentThreadName() { return Thread.currentThread().getName(); } // Callback for JNI
    private static final void dumpStack() { // Callback for JNI
        System.err.println("Stacktrace on thread "+Thread.currentThread().getName());
        ExceptionUtils.dumpStack(System.err);
    }

    private static native boolean initIDs0();
    private static native boolean isNSView0(long object);
    private static native boolean isNSWindow0(long object);
    private static native Object GetLocationOnScreen0(long windowOrView, int src_x, int src_y);
    private static native Object GetInsets0(long windowOrView);
    private static native float GetScreenPixelScale1(int displayID);
    private static native float GetScreenPixelScale2(long windowOrView);
    private static native float GetWindowPixelScale1(long windowOrView);
    private static native void SetWindowPixelScale1(final long windowOrView, final float reqPixelScale);
    private static native long CreateNSWindow0(int x, int y, int width, int height);
    private static native void DestroyNSWindow0(long nsWindow);
    private static native long GetNSView0(long nsWindow);
    private static native long GetNSWindow0(long nsView);
    private static native long CreateCALayer0(int width, int height, float contentsScale);
    private static native void AddCASublayer0(long rootCALayer, long subCALayer, int x, int y, int width, int height, float contentsScale, int caLayerQuirks);
    private static native void FixCALayerLayout0(long rootCALayer, long subCALayer, boolean visible, int x, int y, int width, int height, int caLayerQuirks);
    private static native void SetCALayerPixelScale0(long rootCALayer, long subCALayer, float contentsScale);
    private static native void RemoveCASublayer0(long rootCALayer, long subCALayer, boolean subCALayerRelease);
    private static native void DestroyCALayer0(long caLayer);
    private static native void RunOnMainThread0(boolean kickNSApp, Runnable runnable);
    private static native void RunLater0(boolean onMain, boolean kickNSApp, Runnable runnable, int delay);
    private static native void KickNSApp0();
    private static native boolean IsMainThread0();
    private static native int GetScreenRefreshRate0(int scrn_idx);
}
