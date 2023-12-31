/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.util.List;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.test.junit.util.GLTestUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.SWTTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Sharing the VBO of 3 GearsES2 instances, each in their own SWT GLCanvas.
 * <p>
 * This is achieved by using the 1st GLCanvas as the <i>master</i>
 * and using the build-in blocking mechanism to postpone creation
 * of the 2nd and 3rd GLCanvas until the 1st GLCanvas's GLContext becomes created.
 * </p>
 * <p>
 * Above method allows random creation of the 1st GLCanvas <b>in theory</b>, which triggers
 * creation of the <i>dependent</i> other GLCanvas sharing it's GLContext.<br>
 * However, since this test may perform on the <i>main thread</i> we have
 * to initialize all in order, since otherwise the <i>test main thread</i>
 * itself blocks SWT GLCanvas creation ..
 * </p>
 * <p>
 * On MacOS 12+ and SWT 4.26 while not using AWT (-Djava.awt.headless=true, -XstartOnFirstThread),
 * we recently get the following Exception from SWT (suppressed):
 * <pre>
java.lang.NullPointerException: Cannot invoke "org.eclipse.swt.internal.cocoa.NSGraphicsContext.saveGraphicsState()" because "context" is null
        at org.eclipse.swt.widgets.Widget.drawRect(Widget.java:764)
        at org.eclipse.swt.widgets.Canvas.drawRect(Canvas.java:170)
        at org.eclipse.swt.widgets.Display.windowProc(Display.java:6287)
        at org.eclipse.swt.internal.cocoa.OS.objc_msgSendSuper(Native Method)
        at org.eclipse.swt.widgets.Display.applicationNextEventMatchingMask(Display.java:5565)
        at org.eclipse.swt.widgets.Display.applicationProc(Display.java:5965)
        at org.eclipse.swt.internal.cocoa.OS.objc_msgSend(Native Method)
        at org.eclipse.swt.internal.cocoa.NSApplication.nextEventMatchingMask(NSApplication.java:92)
        at org.eclipse.swt.widgets.Display.readAndDispatch(Display.java:3983)
        at com.jogamp.opengl.test.junit.util.SWTTestUtil$WaitAction$1.run(SWTTestUtil.java:52)
        at org.eclipse.swt.widgets.Synchronizer.syncExec(Synchronizer.java:183)
        at org.eclipse.swt.widgets.Display.syncExec(Display.java:5250)
        at com.jogamp.opengl.test.junit.util.SWTTestUtil$WaitAction.run(SWTTestUtil.java:63)
        at com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2SWT3.test02AsyncEachAnimator(TestSharedContextVBOES2SWT3.java:376)
 * </pre>
 * This is not observed if running using AWT (-Djava.awt.headless=false).
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextVBOES2SWT3 extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.get(GLProfile.GL2ES2);
            Assert.assertNotNull(glp);
            caps = new GLCapabilities(glp);
            Assert.assertNotNull(caps);
            width  = 256;
            height = 256;
        } else {
            setTestSupported(false);
        }
    }

    Display display = null;
    Shell shell1 = null;
    Composite composite1 = null;
    Shell shell2 = null;
    Composite composite2 = null;
    Shell shell3 = null;
    Composite composite3 = null;

    @Before
    public void init() {
        SWTAccessor.invokeOnOSTKThread(true, new Runnable() {
            @Override
            public void run() {
                display = new Display();
                Assert.assertNotNull( display );
            }});
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                shell1 = new Shell( display );
                shell1.setLayout( new FillLayout() );
                composite1 = new Composite( shell1, SWT.NO_BACKGROUND );
                composite1.setLayout( new FillLayout() );

                shell2 = new Shell( display );
                shell2.setLayout( new FillLayout() );
                composite2 = new Composite( shell2, SWT.NO_BACKGROUND );
                composite2.setLayout( new FillLayout() );

                shell3 = new Shell( display );
                shell3.setLayout( new FillLayout() );
                composite3 = new Composite( shell3, SWT.NO_BACKGROUND );
                composite3.setLayout( new FillLayout() );
            }});
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell1 );
        Assert.assertNotNull( composite1 );
        Assert.assertNotNull( shell2 );
        Assert.assertNotNull( composite2 );
        Assert.assertNotNull( shell3 );
        Assert.assertNotNull( composite3 );
        try {
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    composite3.dispose();
                    shell3.dispose();
                    composite2.dispose();
                    shell2.dispose();
                    composite1.dispose();
                    shell1.dispose();
                }});
            SWTAccessor.invokeOnOSTKThread(true, new Runnable() {
                @Override
                public void run() {
                    display.dispose();
                }});
        }
        catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        display = null;
        shell1 = null;
        composite1 = null;
        shell2 = null;
        composite2 = null;
        shell3 = null;
        composite3 = null;
    }

    protected GLCanvas createGLCanvas(final Shell shell, final Composite composite, final int x, final int y, final GearsES2 gears) throws InterruptedException {
        final GLCanvas glCanvas = GLCanvas.create( composite, 0, caps, null);
        Assert.assertNotNull( glCanvas );
        glCanvas.addGLEventListener(gears);
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                shell.setText("SWT GLCanvas Shared Gears Test");
                shell.setSize( width, height);
                shell.setLocation(x, y);
            } } );
        return glCanvas;
    }

    @Test
    public void test01SyncedOneAnimator() throws InterruptedException {
        final Animator animator = new Animator();
        final GearsES2 g1 = new GearsES2(0);
        // g1.setUseMappedBuffers(useMappedBuffers);
        g1.setValidateBuffers(true);
        final GLCanvas c1 = createGLCanvas(shell1, composite1, 0, 0, g1);
        animator.add(c1);

        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLCanvas c2 = createGLCanvas(shell2, composite2, 0+width, 0+0, g2);
        c2.setSharedAutoDrawable(c1);
        animator.add(c2);

        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLCanvas c3 = createGLCanvas(shell3, composite3, 0, height, g3);
        c3.setSharedAutoDrawable(c1);
        animator.add(c3);

        display.syncExec(new Runnable() {
            @Override
            public void run() {
                shell1.open();  // master ..
                shell2.open();  // shall wait until f1 is ready
                shell3.open();  // shall wait until f1 is ready
            } } );
        final long t0 = System.currentTimeMillis();
        animator.start(); // kicks off GLContext .. and hence gears of f2 + f3 completion

        final SWTTestUtil.WaitAction waitAction = new SWTTestUtil.WaitAction(display, true, 200);
        Assert.assertEquals(true,  GLTestUtil.waitForRealized(c1, true, waitAction));
        Assert.assertEquals(true,  GLTestUtil.waitForContextCreated(c1, true, waitAction));
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));

        Assert.assertEquals(true,  GLTestUtil.waitForRealized(c2, true, waitAction));
        Assert.assertEquals(true,  GLTestUtil.waitForContextCreated(c2, true, waitAction));
        Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));

        Assert.assertEquals(true,  GLTestUtil.waitForRealized(c3, true, waitAction));
        Assert.assertEquals(true,  GLTestUtil.waitForContextCreated(c3, true, waitAction));
        Assert.assertTrue("Gears3 not initialized", g3.waitForInit(true));

        final GLContext ctx1 = c1.getContext();
        final GLContext ctx2 = c2.getContext();
        final GLContext ctx3 = c3.getContext();
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-C-3.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-C-3.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-C-3.3", ctx3);

            Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
            Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());
            Assert.assertTrue("Ctx3 is not shared", ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 2, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 2, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 2, ctx3Shares.size());
        }

        Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
        Assert.assertTrue("Gears2 is not shared", g2.usesSharedGears());
        Assert.assertTrue("Gears3 is not shared", g3.usesSharedGears());

        while(animator.isAnimating() && System.currentTimeMillis()-t0<duration) {
            waitAction.run();
        }

        // Stopped animator allows native windowing system 'repaint' event
        // to trigger GLAD 'display'
        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());

        display.syncExec(new Runnable() {
            @Override
            public void run() {
                c3.dispose();
                c2.dispose();
                c1.dispose();
            } } );
    }

    @Test
    public void test02AsyncEachAnimator() throws InterruptedException {
        final Animator a1 = new Animator();
        final GearsES2 g1 = new GearsES2(0);
        g1.setSyncObjects(g1); // this is master, since rendered we must use it as sync
        // g1.setUseMappedBuffers(useMappedBuffers);
        g1.setValidateBuffers(true);
        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 0.0");
        final GLCanvas c1 = createGLCanvas(shell1, composite1, 0, 0, g1);
        a1.add(c1);
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                shell1.open();
            } } );
        a1.start();

        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 1.0");
        final SWTTestUtil.WaitAction waitAction = new SWTTestUtil.WaitAction(display, true, 200);
        Assert.assertEquals(true,  GLTestUtil.waitForRealized(c1, true, waitAction));
        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 1.1");
        Assert.assertEquals(true,  GLTestUtil.waitForContextCreated(c1, true, waitAction));
        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 1.2");
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));

        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 2.0");
        final Animator a2 = new Animator();
        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLCanvas c2 = createGLCanvas(shell2, composite2, width, 0, g2);
        c2.setSharedAutoDrawable(c1);
        a2.add(c2);
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                shell2.open();
            } } );
        a2.start();

        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 3.0");
        final Animator a3 = new Animator();
        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLCanvas c3 = createGLCanvas(shell3, composite3, 0, height, g3);
        c3.setSharedAutoDrawable(c1);
        a3.add(c3);
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                shell3.open();
            } } );
        a3.start();

        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 4.0");
        Assert.assertEquals(true,  GLTestUtil.waitForRealized(c2, true, waitAction));
        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 4.1: Exception "+(null != waitAction.getException(true)));
        Assert.assertEquals(true,  GLTestUtil.waitForContextCreated(c2, true, waitAction));
        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 4.2: Exception "+(null != waitAction.getException(true)));
        Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));

        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 5.0: Exception "+(null != waitAction.getException(true)));
        Assert.assertEquals(true,  GLTestUtil.waitForRealized(c3, true, waitAction));
        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 5.1: Exception "+(null != waitAction.getException(true)));
        Assert.assertEquals(true,  GLTestUtil.waitForContextCreated(c3, true, waitAction));
        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 5.2: Exception "+(null != waitAction.getException(true)));
        Assert.assertTrue("Gears3 not initialized", g3.waitForInit(true));

        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 6.0: Exception "+(null != waitAction.getException(true)));
        final long t0 = System.currentTimeMillis();

        final GLContext ctx1 = c1.getContext();
        final GLContext ctx2 = c2.getContext();
        final GLContext ctx3 = c3.getContext();
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-C-3.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-C-3.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-C-3.2", ctx3);

            Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
            Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());
            Assert.assertTrue("Ctx3 is not shared", ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 2, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 2, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 2, ctx3Shares.size());
        }

        Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
        Assert.assertTrue("Gears2 is not shared", g2.usesSharedGears());
        Assert.assertTrue("Gears3 is not shared", g3.usesSharedGears());

        while(System.currentTimeMillis()-t0<duration) {
            waitAction.run();
        }
        System.err.println("TestSharedContextVBOES2SWT3.test02AsyncEachAnimator: 8.0: Exception "+(null != waitAction.getException(true)));
        // Stopped animator allows native windowing system 'repaint' event
        // to trigger GLAD 'display'
        a1.stop();
        Assert.assertEquals(false, a1.isAnimating());
        a2.stop();
        Assert.assertEquals(false, a2.isAnimating());
        a3.stop();
        Assert.assertEquals(false, a3.isAnimating());

        display.syncExec(new Runnable() {
            @Override
            public void run() {
                c3.dispose();
                c2.dispose();
                c1.dispose();
            } } );
    }

    static long duration = 2000; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES2SWT3.class.getName());
    }
}
