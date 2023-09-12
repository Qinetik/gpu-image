package jp.co.cyberagent.android.gpuimage;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLDebugHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

/*
 * Copyright (C) 2018 Wasabeef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public open class GLTextureView : GLTextureViewHelper, TextureView.SurfaceTextureListener, View.OnLayoutChangeListener {

    companion object {
        private val TAG : String = "GLTextureView";

        private val LOG_ATTACH_DETACH : Boolean = false;
        private val LOG_THREADS : Boolean = false;
        private val LOG_PAUSE_RESUME : Boolean = false;
        private val LOG_SURFACE : Boolean = false;
        private val LOG_RENDERER : Boolean = false;
        private val LOG_RENDERER_DRAW_FRAME : Boolean = false;
        private val LOG_EGL : Boolean = false;

        /**
         * The renderer only renders
         * when the surface is created, or when {@link #requestRender} is called.
         *
         * @see #getRenderMode()
         * @see #setRenderMode(int)
         * @see #requestRender()
         */
        public val RENDERMODE_WHEN_DIRTY : Int = 0;
        /**
         * The renderer is called
         * continuously to re-render the scene.
         *
         * @see #getRenderMode()
         * @see #setRenderMode(int)
         */
        public val RENDERMODE_CONTINUOUSLY : Int = 1;

        /**
         * Check glError() after every GL call and throw an exception if glError indicates
         * that an error has occurred. This can be used to help track down which OpenGL ES call
         * is causing an error.
         *
         * @see #getDebugFlags
         * @see #setDebugFlags
         */
        public val DEBUG_CHECK_GL_ERROR : Int = 1;

        /**
         * Log GL calls to the system log at "verbose" level with tag "GLTextureView".
         *
         * @see #getDebugFlags
         * @see #setDebugFlags
         */
        public val DEBUG_LOG_GL_CALLS : Int = 2;

        private val glThreadManager : GLThreadManager = GLThreadManager();

    }

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public constructor(context : Context) : super(context) {
        init();
    }

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public constructor(context : Context, attrs : AttributeSet?) : super(context, attrs) {
        init();
    }

    @Throws(Throwable::class)
    protected override fun onFinalize() {
        try {
            if (glThread != null) {
                // GLThread may still be running if this view was never
                // attached to a window.
                glThread!!.requestExitAndWait();
            }
        } finally {
            super.finalize();
        }
    }

    private fun init() {
        setSurfaceTextureListener(this);
    }

    /**
     * Set the glWrapper. If the glWrapper is not null, its
     * {@link GLWrapper#wrap(javax.microedition.khronos.opengles.GL)} method is called
     * whenever a surface is created. A GLWrapper can be used to wrap
     * the GL object that's passed to the renderer. Wrapping a GL
     * object enables examining and modifying the behavior of the
     * GL calls made by the renderer.
     * <p>
     * Wrapping is typically used for debugging purposes.
     * <p>
     * The default value is null.
     *
     * @param glWrapper the new GLWrapper
     */
    public fun setGLWrapper(glWrapper : GLWrapper) {
        this.glWrapper = glWrapper;
    }

    /**
     * Set the debug flags to a new value. The value is
     * constructed by OR-together zero or more
     * of the DEBUG_CHECK_* constants. The debug flags take effect
     * whenever a surface is created. The default value is zero.
     *
     * @param debugFlags the new debug flags
     * @see #DEBUG_CHECK_GL_ERROR
     * @see #DEBUG_LOG_GL_CALLS
     */
    public fun setDebugFlags(debugFlags : Int) {
        this.debugFlags = debugFlags;
    }

    /**
     * Get the current value of the debug flags.
     *
     * @return the current value of the debug flags.
     */
    public fun getDebugFlags() : Int {
        return debugFlags;
    }

    /**
     * Control whether the EGL context is preserved when the GLTextureView is paused and
     * resumed.
     * <p>
     * If set to true, then the EGL context may be preserved when the GLTextureView is paused.
     * Whether the EGL context is actually preserved or not depends upon whether the
     * Android device that the program is running on can support an arbitrary number of EGL
     * contexts or not. Devices that can only support a limited number of EGL contexts must
     * release the  EGL context in order to allow multiple applications to share the GPU.
     * <p>
     * If set to false, the EGL context will be released when the GLTextureView is paused,
     * and recreated when the GLTextureView is resumed.
     * <p>
     * <p>
     * The default is false.
     *
     * @param preserveOnPause preserve the EGL context when paused
     */
    public fun setPreserveEGLContextOnPause(preserveOnPause : Boolean) {
        preserveEGLContextOnPause = preserveOnPause;
    }

    /**
     * @return true if the EGL context will be preserved when paused
     */
    public fun getPreserveEGLContextOnPause() : Boolean {
        return preserveEGLContextOnPause;
    }

    /**
     * Set the renderer associated with this view. Also starts the thread that
     * will call the renderer, which in turn causes the rendering to start.
     * <p>This method should be called once and only once in the life-cycle of
     * a GLTextureView.
     * <p>The following GLTextureView methods can only be called <em>before</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #setEGLConfigChooser(boolean)}
     * <li>{@link #setEGLConfigChooser(EGLConfigChooser)}
     * <li>{@link #setEGLConfigChooser(int, int, int, int, int, int)}
     * </ul>
     * <p>
     * The following GLTextureView methods can only be called <em>after</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #getRenderMode()}
     * <li>{@link #onPause()}
     * <li>{@link #onResume()}
     * <li>{@link #queueEvent(Runnable)}
     * <li>{@link #requestRender()}
     * <li>{@link #setRenderMode(int)}
     * </ul>
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    public fun setRenderer(renderer : Renderer) {
        checkRenderThreadState();
        if (eglConfigChooser == null) {
            eglConfigChooser = SimpleEGLConfigChooser(true);
        }
        if (eglContextFactory == null) {
            eglContextFactory = DefaultContextFactory();
        }
        if (eglWindowSurfaceFactory == null) {
            eglWindowSurfaceFactory = DefaultWindowSurfaceFactory();
        }
        this.renderer = renderer;
        glThread = GLThread(mThisWeakRef);
        glThread!!.start();
    }

    /**
     * Install a custom EGLContextFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a context will be created with no shared context and
     * with a null attribute list.
     */
    public fun setEGLContextFactory(factory : EGLContextFactory) {
        checkRenderThreadState();
        eglContextFactory = factory;
    }

    /**
     * Install a custom EGLWindowSurfaceFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a window surface will be created with a null attribute list.
     */
    public fun setEGLWindowSurfaceFactory(factory : EGLWindowSurfaceFactory) {
        checkRenderThreadState();
        eglWindowSurfaceFactory = factory;
    }

    /**
     * Install a custom EGLConfigChooser.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose an EGLConfig that is compatible with the current
     * android.view.Surface, with a depth buffer depth of
     * at least 16 bits.
     */
    public fun setEGLConfigChooser(configChooser : EGLConfigChooser) {
        checkRenderThreadState();
        eglConfigChooser = configChooser;
    }

    /**
     * Install a config chooser which will choose a config
     * as close to 16-bit RGB as possible, with or without an optional depth
     * buffer as close to 16-bits as possible.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose an RGB_888 surface with a depth buffer depth of
     * at least 16 bits.
     */
    public fun setEGLConfigChooser(needDepth : Boolean) {
        setEGLConfigChooser(SimpleEGLConfigChooser(needDepth));
    }

    /**
     * Install a config chooser which will choose a config
     * with at least the specified depthSize and stencilSize,
     * and exactly the specified redSize, greenSize, blueSize and alphaSize.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose an RGB_888 surface with a depth buffer depth of
     * at least 16 bits.
     */
    public fun setEGLConfigChooser(redSize : Int, greenSize : Int, blueSize : Int, alphaSize : Int,
                                   depthSize : Int, stencilSize : Int) {
        setEGLConfigChooser(
                ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize));
    }

    /**
     * Inform the default EGLContextFactory and default EGLConfigChooser
     * which EGLContext client version to pick.
     * <p>Use this method to create an OpenGL ES 2.0-compatible context.
     * Example:
     * <pre class="prettyprint">
     * public MyView(Context context) {
     * super(context);
     * setEGLContextClientVersion(2); // Pick an OpenGL ES 2.0 context.
     * setRenderer(new MyRenderer());
     * }
     * </pre>
     * <p>Note: Activities which require OpenGL ES 2.0 should indicate this by
     * setting @lt;uses-feature android:glEsVersion="0x00020000" /> in the activity's
     * AndroidManifest.xml file.
     * <p>If this method is called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>This method only affects the behavior of the default EGLContexFactory and the
     * default EGLConfigChooser. If
     * {@link #setEGLContextFactory(EGLContextFactory)} has been called, then the supplied
     * EGLContextFactory is responsible for creating an OpenGL ES 2.0-compatible context.
     * If
     * {@link #setEGLConfigChooser(EGLConfigChooser)} has been called, then the supplied
     * EGLConfigChooser is responsible for choosing an OpenGL ES 2.0-compatible config.
     *
     * @param version The EGLContext client version to choose. Use 2 for OpenGL ES 2.0
     */
    public fun setEGLContextClientVersion(version : Int) {
        checkRenderThreadState();
        eglContextClientVersion = version;
    }

    /**
     * Set the rendering mode. When renderMode is
     * RENDERMODE_CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When renderMode
     * is RENDERMODE_WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when {@link #requestRender} is called. Defaults to RENDERMODE_CONTINUOUSLY.
     * <p>
     * Using RENDERMODE_WHEN_DIRTY can improve battery life and overall system performance
     * by allowing the GPU and CPU to idle when the view does not need to be updated.
     * <p>
     * This method can only be called after {@link #setRenderer(Renderer)}
     *
     * @param renderMode one of the RENDERMODE_X constants
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     */
    public fun setRenderMode(renderMode : Int) {
        glThread!!.setRenderMode(renderMode);
    }

    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     *
     * @return the current rendering mode.
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     */
    public fun getRenderMode() : Int {
        return glThread!!.getRenderMode();
    }

    /**
     * Request that the renderer render a frame.
     * This method is typically used when the render mode has been set to
     * {@link #RENDERMODE_WHEN_DIRTY}, so that frames are only rendered on demand.
     * May be called
     * from any thread. Must not be called before a renderer has been set.
     */
    public fun requestRender() {
        glThread!!.requestRender();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLTextureView.
     */
    public fun surfaceCreated(texture : SurfaceTexture) {
        glThread!!.surfaceCreated();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLTextureView.
     */
    public fun surfaceDestroyed(texture : SurfaceTexture) {
        // Surface will be destroyed when we return
        glThread!!.surfaceDestroyed();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLTextureView.
     */
    public fun surfaceChanged(texture : SurfaceTexture, format : Int, w : Int, h : Int) {
        glThread!!.onWindowResize(w, h);
    }

    /**
     * Inform the view that the activity is paused. The owner of this view must
     * call this method when the activity is paused. Calling this method will
     * pause the rendering thread.
     * Must not be called before a renderer has been set.
     */
    public fun onPause() {
        glThread!!.onPause();
    }

    /**
     * Inform the view that the activity is resumed. The owner of this view must
     * call this method when the activity is resumed. Calling this method will
     * recreate the OpenGL display and resume the rendering
     * thread.
     * Must not be called before a renderer has been set.
     */
    public fun onResume() {
        glThread!!.onResume();
    }

    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     *
     * @param r the runnable to be run on the GL rendering thread.
     */
    public fun queueEvent(r : Runnable) {
        glThread!!.queueEvent(r);
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLTextureView.
     */
    protected override fun onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onAttachedToWindow reattach =" + detached);
        }
        if (detached && (renderer != null)) {
            var renderMode : Int = RENDERMODE_CONTINUOUSLY;
            if (glThread != null) {
                renderMode = glThread!!.getRenderMode();
            }
            glThread = GLThread(mThisWeakRef);
            if (renderMode != RENDERMODE_CONTINUOUSLY) {
                glThread!!.setRenderMode(renderMode);
            }
            glThread!!.start();
        }
        detached = false;
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLTextureView.
     * Must not be called before a renderer has been set.
     */
    protected override fun onDetachedFromWindow() {
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onDetachedFromWindow");
        }
        if (glThread != null) {
            glThread!!.requestExitAndWait();
        }
        detached = true;
        super.onDetachedFromWindow();
    }

    public override fun onLayoutChange(v : View, left : Int, top : Int, right : Int, bottom : Int, oldLeft : Int,
                              oldTop : Int, oldRight : Int, oldBottom : Int) {
        surfaceChanged(getSurfaceTexture()!!, 0, right - left, bottom - top);
    }

    public fun addSurfaceTextureListener(listener : SurfaceTextureListener) {
        surfaceTextureListeners.add(listener);
    }

    public override fun onSurfaceTextureAvailable(surface : SurfaceTexture, width : Int, height : Int) {
        surfaceCreated(surface);
        surfaceChanged(surface, 0, width, height);

        for (l : SurfaceTextureListener in surfaceTextureListeners) {
            l.onSurfaceTextureAvailable(surface, width, height);
        }
    }

    public override fun onSurfaceTextureSizeChanged(surface : SurfaceTexture, width : Int, height : Int) {
        surfaceChanged(surface, 0, width, height);

        for (l : SurfaceTextureListener in surfaceTextureListeners) {
            l.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    public override fun onSurfaceTextureDestroyed(surface : SurfaceTexture) : Boolean {
        surfaceDestroyed(surface);

        for (l : SurfaceTextureListener in surfaceTextureListeners) {
            l.onSurfaceTextureDestroyed(surface);
        }

        return true;
    }

    public override fun onSurfaceTextureUpdated(surface : SurfaceTexture) {
        requestRender();

        for (l : SurfaceTextureListener in surfaceTextureListeners) {
            l.onSurfaceTextureUpdated(surface);
        }
    }

    // ----------------------------------------------------------------------

    /**
     * An interface used to wrap a GL interface.
     * <p>Typically
     * used for implementing debugging and tracing on top of the default
     * GL interface. You would typically use this by creating your own class
     * that implemented all the GL methods by delegating to another GL instance.
     * Then you could add your own behavior before or after calling the
     * delegate. All the GLWrapper would do was instantiate and return the
     * wrapper GL instance:
     * <pre class="prettyprint">
     * class MyGLWrapper implements GLWrapper {
     * GL wrap(GL gl) {
     * return new MyGLImplementation(gl);
     * }
     * static class MyGLImplementation implements GL,GL10,GL11,... {
     * ...
     * }
     * }
     * </pre>
     *
     * @see #setGLWrapper(GLWrapper)
     */
    public interface GLWrapper {
        /**
         * Wraps a gl interface in another gl interface.
         *
         * @param gl a GL interface that is to be wrapped.
         * @return either the input argument or another GL object that wraps the input argument.
         */
        fun wrap(gl : GL) : GL;
    }

    /**
     * A generic renderer interface.
     * <p>
     * The renderer is responsible for making OpenGL calls to render a frame.
     * <p>
     * GLTextureView clients typically create their own classes that implement
     * this interface, and then call {@link GLTextureView#setRenderer} to
     * register the renderer with the GLTextureView.
     * <p>
     *
     * <div class="special reference">
     * <h3>Developer Guides</h3>
     * <p>For more information about how to use OpenGL, read the
     * <a href="{@docRoot}guide/topics/graphics/opengl.html">OpenGL</a> developer guide.</p>
     * </div>
     *
     * <h3>Threading</h3>
     * The renderer will be called on a separate thread, so that rendering
     * performance is decoupled from the UI thread. Clients typically need to
     * communicate with the renderer from the UI thread, because that's where
     * input events are received. Clients can communicate using any of the
     * standard Java techniques for cross-thread communication, or they can
     * use the {@link GLTextureView#queueEvent(Runnable)} convenience method.
     * <p>
     * <h3>EGL Context Lost</h3>
     * There are situations where the EGL rendering context will be lost. This
     * typically happens when device wakes up after going to sleep. When
     * the EGL context is lost, all OpenGL resources (such as textures) that are
     * associated with that context will be automatically deleted. In order to
     * keep rendering correctly, a renderer must recreate any lost resources
     * that it still needs. The {@link #onSurfaceCreated(javax.microedition.khronos.opengles.GL10,
     * javax.microedition.khronos.egl.EGLConfig)} method
     * is a convenient place to do this.
     *
     * @see #setRenderer(Renderer)
     */
    public interface Renderer {
        /**
         * Called when the surface is created or recreated.
         * <p>
         * Called when the rendering thread
         * starts and whenever the EGL context is lost. The EGL context will typically
         * be lost when the Android device awakes after going to sleep.
         * <p>
         * Since this method is called at the beginning of rendering, as well as
         * every time the EGL context is lost, this method is a convenient place to put
         * code to create resources that need to be created when the rendering
         * starts, and that need to be recreated when the EGL context is lost.
         * Textures are an example of a resource that you might want to create
         * here.
         * <p>
         * Note that when the EGL context is lost, all OpenGL resources associated
         * with that context will be automatically deleted. You do not need to call
         * the corresponding "glDelete" methods such as glDeleteTextures to
         * manually delete these lost resources.
         * <p>
         *
         * @param gl     the GL interface. Use <code>instanceof</code> to
         *               test if the interface supports GL11 or higher interfaces.
         * @param config the EGLConfig of the created surface. Can be used
         *               to create matching pbuffers.
         */
        fun onSurfaceCreated(gl : GL10, config : EGLConfig);

        /**
         * Called when the surface changed size.
         * <p>
         * Called after the surface is created and whenever
         * the OpenGL ES surface size changes.
         * <p>
         * Typically you will set your viewport here. If your camera
         * is fixed then you could also set your projection matrix here:
         * <pre class="prettyprint">
         * void onSurfaceChanged(GL10 gl, int width, int height) {
         * gl.glViewport(0, 0, width, height);
         * // for a fixed camera, set the projection too
         * float ratio = (float) width / height;
         * gl.glMatrixMode(GL10.GL_PROJECTION);
         * gl.glLoadIdentity();
         * gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
         * }
         * </pre>
         *
         * @param gl the GL interface. Use <code>instanceof</code> to
         *           test if the interface supports GL11 or higher interfaces.
         */
        fun onSurfaceChanged(gl : GL10, width : Int, height : Int);

        /**
         * Called to draw the current frame.
         * <p>
         * This method is responsible for drawing the current frame.
         * <p>
         * The implementation of this method typically looks like this:
         * <pre class="prettyprint">
         * void onDrawFrame(GL10 gl) {
         * gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
         * //... other gl calls to render the scene ...
         * }
         * </pre>
         *
         * @param gl the GL interface. Use <code>instanceof</code> to
         *           test if the interface supports GL11 or higher interfaces.
         */
        fun onDrawFrame(gl : GL10)
    }

    /**
     * An interface for customizing the eglCreateContext and eglDestroyContext calls.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link GLTextureView#setEGLContextFactory(EGLContextFactory)}
     */
    public interface EGLContextFactory {
        fun createContext(egl : EGL10, display : EGLDisplay, eglConfig : EGLConfig) : EGLContext;

        fun destroyContext(egl : EGL10, display : EGLDisplay, context : EGLContext);
    }

    private inner class DefaultContextFactory : EGLContextFactory {
        private val EGL_CONTEXT_CLIENT_VERSION : Int = 0x3098;

        public override fun createContext(egl : EGL10, display : EGLDisplay, config : EGLConfig) : EGLContext {
            val attrib_list = intArrayOf(
                    EGL_CONTEXT_CLIENT_VERSION, eglContextClientVersion, EGL10.EGL_NONE
            )

            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                    if(eglContextClientVersion != 0) attrib_list else null);
        }

        public override fun destroyContext(egl : EGL10, display : EGLDisplay, context : EGLContext) {
            if (!egl.eglDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                if (LOG_THREADS) {
                    Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId());
                }
                EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
            }
        }
    }

    /**
     * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link GLTextureView#setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory)}
     */
    public interface EGLWindowSurfaceFactory {
        /**
         * @return null if the surface cannot be constructed.
         */
        fun createWindowSurface(egl : EGL10, display : EGLDisplay, config : EGLConfig,
                                       nativeWindow : Object) : EGLSurface?;

        fun destroySurface(egl : EGL10, display : EGLDisplay, surface : EGLSurface);
    }

    private class DefaultWindowSurfaceFactory : EGLWindowSurfaceFactory {

        public override fun createWindowSurface(egl : EGL10, display : EGLDisplay, config : EGLConfig,
                                              nativeWindow : Object) : EGLSurface? {
            var result : EGLSurface? = null;
            try {
                result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
            } catch (e : IllegalArgumentException) {
                // This exception indicates that the surface flinger surface
                // is not valid. This can happen if the surface flinger surface has
                // been torn down, but the application has not yet been
                // notified via SurfaceHolder.Callback.surfaceDestroyed.
                // In theory the application should be notified first,
                // but in practice sometimes it is not. See b/4588890
                Log.e(TAG, "eglCreateWindowSurface", e);
            }
            return result;
        }

        public override fun destroySurface(egl : EGL10, display : EGLDisplay, surface : EGLSurface) {
            egl.eglDestroySurface(display, surface);
        }
    }

    /**
     * An interface for choosing an EGLConfig configuration from a list of
     * potential configurations.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link GLTextureView#setEGLConfigChooser(EGLConfigChooser)}
     */
    public interface EGLConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * {@link EGL10#eglChooseConfig} and iterating through the results. Please consult the
         * EGL specification available from The Khronos Group to learn how to call eglChooseConfig.
         *
         * @param egl     the EGL10 for the current display.
         * @param display the current display.
         * @return the chosen configuration.
         */
        fun chooseConfig(egl : EGL10, display : EGLDisplay) : EGLConfig;
    }

    private abstract inner class BaseConfigChooser : EGLConfigChooser {

        public constructor(configSpec : IntArray) {
            mConfigSpec = filterConfigSpec(configSpec);
        }

        public override fun chooseConfig(egl : EGL10, display : EGLDisplay) : EGLConfig {
            val num_config : IntArray = IntArray(1);
            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0, num_config)) {
                throw IllegalArgumentException("eglChooseConfig failed");
            }

            val numConfigs : Int = num_config[0];

            if (numConfigs <= 0) {
                throw IllegalArgumentException("No configs match configSpec");
            }

            val configs : Array<EGLConfig> = Array(numConfigs) {
                object : EGLConfig() {

                }
            }
            if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs, num_config)) {
                throw IllegalArgumentException("eglChooseConfig#2 failed");
            }
            val config : EGLConfig? = chooseConfig(egl, display, configs);
            if (config == null) {
                throw IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract fun chooseConfig(egl : EGL10, display : EGLDisplay, configs : Array<EGLConfig>) : EGLConfig?

        protected val mConfigSpec : IntArray;

        private fun filterConfigSpec(configSpec : IntArray) : IntArray {
            if (eglContextClientVersion != 2) {
                return configSpec;
            }
            /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
             * And we know the configSpec is well formed.
             */
            val len : Int = configSpec.size;
            val newConfigSpec : IntArray = IntArray(len + 2);
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
            newConfigSpec[len] = 0x0004; /* EGL_OPENGL_ES2_BIT */
            newConfigSpec[len + 1] = EGL10.EGL_NONE;
            return newConfigSpec;
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes,
     * and at least the specified depth and stencil sizes.
     */
    private open inner class ComponentSizeChooser(
        // Subclasses can adjust these values:
        protected val redSize : Int,
        protected val greenSize : Int,
        protected val blueSize : Int,
        protected val alphaSize : Int,
        protected val depthSize : Int,
        protected val stencilSize : Int,
        private val value : IntArray = IntArray(1)
    ) : BaseConfigChooser(
        intArrayOf(
            EGL10.EGL_RED_SIZE, redSize, EGL10.EGL_GREEN_SIZE, greenSize, EGL10.EGL_BLUE_SIZE,
            blueSize, EGL10.EGL_ALPHA_SIZE, alphaSize, EGL10.EGL_DEPTH_SIZE, depthSize,
            EGL10.EGL_STENCIL_SIZE, stencilSize, EGL10.EGL_NONE
        )
    ) {

        public override fun chooseConfig(egl : EGL10, display : EGLDisplay, configs : Array<EGLConfig>) : EGLConfig? {
            for (config in configs) {
                val d : Int = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                val s : Int = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
                if ((d >= depthSize) && (s >= stencilSize)) {
                    val r : Int = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                    val g : Int = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                    val b : Int = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                    val a : Int = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
                    if ((r == redSize) && (g == greenSize) && (b == blueSize) && (a == alphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private fun findConfigAttrib(egl : EGL10, display : EGLDisplay, config : EGLConfig, attribute : Int,
                                     defaultValue : Int) : Int {

            if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                return value[0];
            }
            return defaultValue;
        }

    }

    /**
     * This class will choose a RGB_888 surface with
     * or without a depth buffer.
     */
    private inner class SimpleEGLConfigChooser(withDepthBuffer: Boolean) :
        ComponentSizeChooser(8, 8, 8, 0, if (withDepthBuffer) 16 else 0, 0) {
    }

    /**
     * An EGL helper class.
     */

    private class EglHelper {
        public constructor(glTextureViewWeakReference : WeakReference<GLTextureView>) {
            this.glTextureViewWeakRef = glTextureViewWeakReference;
        }

        /**
         * Initialize EGL for a given configuration spec.
         */
        public fun start() {
            if (LOG_EGL) {
                Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
            }
            /*
             * Get an EGL instance
             */
            egl = EGLContext.getEGL() as EGL10;

            /*
             * Get to the default display.
             */
            eglDisplay = egl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw RuntimeException("eglGetDisplay failed");
            }

            /*
             * We can now initialize EGL for that display
             */
            val version : IntArray = IntArray(2)
            if (!egl!!.eglInitialize(eglDisplay, version)) {
                throw RuntimeException("eglInitialize failed");
            }
            val view : GLTextureView? = glTextureViewWeakRef.get();
            if (view == null) {
                eglConfig = null;
                eglContext = null;
            } else {
                eglConfig = view.eglConfigChooser!!.chooseConfig(egl!!, eglDisplay!!);

                /*
                 * Create an EGL context. We want to do this as rarely as we can, because an
                 * EGL context is a somewhat heavy object.
                 */
                eglContext = view.eglContextFactory!!.createContext(egl!!, eglDisplay!!, eglConfig!!);
            }
            if (eglContext == null || eglContext == EGL10.EGL_NO_CONTEXT) {
                eglContext = null;
                throwEglException("createContext");
            }
            if (LOG_EGL) {
                Log.w("EglHelper",
                        "createContext " + eglContext + " tid=" + Thread.currentThread().getId());
            }

            eglSurface = null;
        }

        /**
         * Create an egl surface for the current SurfaceHolder surface. If a surface
         * already exists, destroy it before creating the new surface.
         *
         * @return true if the surface was created successfully.
         */
        public fun createSurface() : Boolean {
            if (LOG_EGL) {
                Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
            }
            /*
             * Check preconditions.
             */
            if (egl == null) {
                throw RuntimeException("egl not initialized");
            }
            if (eglDisplay == null) {
                throw RuntimeException("eglDisplay not initialized");
            }
            if (eglConfig == null) {
                throw RuntimeException("eglConfig not initialized");
            }

            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            destroySurfaceImp();

            /*
             * Create an EGL surface we can render into.
             */
            val view : GLTextureView? = glTextureViewWeakRef.get();
            if (view != null) {
                eglSurface = view.eglWindowSurfaceFactory!!.createWindowSurface(egl!!, eglDisplay!!, eglConfig!!,
                        view.getSurfaceTexture() as Object);
            } else {
                eglSurface = null;
            }

            if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
                val error : Int = egl!!.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
                return false;
            }

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */
            if (!egl!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                /*
                 * Could not make the context current, probably because the underlying
                 * TextureView surface has been destroyed.
                 */
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", egl!!.eglGetError());
                return false;
            }

            return true;
        }

        /**
         * Create a GL object for the current EGL context.
         */
        fun createGL() : GL {

            var gl : GL = eglContext!!.getGL();
            val view : GLTextureView? = glTextureViewWeakRef.get();
            if (view != null) {
                if (view.glWrapper != null) {
                    gl = view.glWrapper!!.wrap(gl);
                }

                if ((view.debugFlags and (DEBUG_CHECK_GL_ERROR or DEBUG_LOG_GL_CALLS)) != 0) {
                    var configFlags : Int = 0;
                    var log : Writer? = null;
                    if ((view.debugFlags and DEBUG_CHECK_GL_ERROR) != 0) {
                        configFlags = configFlags or GLDebugHelper.CONFIG_CHECK_GL_ERROR;
                    }
                    if ((view.debugFlags and DEBUG_LOG_GL_CALLS) != 0) {
                        log = LogWriter();
                    }
                    gl = GLDebugHelper.wrap(gl, configFlags, log);
                }
            }
            return gl;
        }

        /**
         * Display the current render surface.
         *
         * @return the EGL error code from eglSwapBuffers.
         */
        public fun swap() : Int {
            if (!egl!!.eglSwapBuffers(eglDisplay, eglSurface)) {
                return egl!!.eglGetError();
            }
            return EGL10.EGL_SUCCESS;
        }

        public fun destroySurface() {
            if (LOG_EGL) {
                Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
            }
            destroySurfaceImp();
        }

        private fun destroySurfaceImp() {
            if (eglSurface != null && eglSurface != EGL10.EGL_NO_SURFACE) {
                egl!!.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);
                val view : GLTextureView? = glTextureViewWeakRef.get();
                if (view != null) {
                    view.eglWindowSurfaceFactory!!.destroySurface(egl!!, eglDisplay!!, eglSurface!!);
                }
                eglSurface = null;
            }
        }

        public fun finish() {
            if (LOG_EGL) {
                Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
            }
            if (eglContext != null) {
                val view : GLTextureView? = glTextureViewWeakRef.get();
                if (view != null) {
                    view.eglContextFactory!!.destroyContext(egl!!, eglDisplay!!, eglContext!!);
                }
                eglContext = null;
            }
            if (eglDisplay != null) {
                egl!!.eglTerminate(eglDisplay);
                eglDisplay = null;
            }
        }

        private fun throwEglException(function : String) {
            throwEglException(function, egl!!.eglGetError());
        }

        companion object {
            public fun throwEglException(function : String, error : Int) {
                val message : String = formatEglError(function, error);
                if (LOG_THREADS) {
                    Log.e("EglHelper",
                        "throwEglException tid=" + Thread.currentThread().getId() + " " + message);
                }
                throw RuntimeException(message);
            }

            public fun logEglErrorAsWarning(tag : String, function : String, error : Int) {
                Log.w(tag, formatEglError(function, error));
            }

            public fun formatEglError(function : String, error : Int) : String {
                return function + " failed: " + error;
            }
        }

        private val glTextureViewWeakRef : WeakReference<GLTextureView>;
        var egl : EGL10? = null;
        var eglDisplay : EGLDisplay? = null
        var eglSurface : EGLSurface? = null;
        var eglConfig : EGLConfig? = null
        var eglContext : EGLContext? = null;
    }

    /**
     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
     * to a Renderer instance to do the actual drawing. Can be configured to
     * render continuously or on request.
     * <p>
     * All potentially blocking synchronization is done through the
     * glThreadManager object. This avoids multiple-lock ordering issues.
     */
    class GLThread : Thread {
        constructor(glTextureViewWeakRef : WeakReference<GLTextureView>) : super() {
            width = 0;
            height = 0;
            requestRender = true;
            renderMode = RENDERMODE_CONTINUOUSLY;
            this.glTextureViewWeakRef = glTextureViewWeakRef;
        }

        public override fun run() {
            setName("GLThread " + getId());
            if (LOG_THREADS) {
                Log.i("GLThread", "starting tid=" + getId());
            }

            try {
                guardedRun();
            } catch (e : InterruptedException) {
                // fall thru and exit normally
            } finally {
                glThreadManager.threadExiting(this);
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(glThreadManager) block.
         */
        private fun stopEglSurfaceLocked() {
            if (haveEglSurface) {
                haveEglSurface = false;
                eglHelper.destroySurface();
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(glThreadManager) block.
         */
        private fun stopEglContextLocked() {
            if (haveEglContext) {
                eglHelper.finish();
                haveEglContext = false;
                glThreadManager.releaseEglContextLocked(this);
            }
        }

        @Throws(InterruptedException::class)
        private fun guardedRun() {
            eglHelper = EglHelper(glTextureViewWeakRef);
            haveEglContext = false;
            haveEglSurface = false;
            try {
                var gl : GL10? = null;
                var createEglContext : Boolean = false;
                var createEglSurface : Boolean = false;
                var createGlInterface : Boolean = false;
                var lostEglContext : Boolean = false;
                var sizeChanged : Boolean = false;
                var wantRenderNotification : Boolean = false;
                var doRenderNotification : Boolean = false;
                var askedToReleaseEglContext : Boolean = false;
                var w : Int = 0;
                var h : Int = 0;
                var event : Runnable? = null;

                while (true) {
                    synchronized (glThreadManager) {
                        while (true) {
                            if (shouldExit) {
                                return;
                            }

                            if (!eventQueue.isEmpty()) {
                                event = eventQueue.removeAt(0);
                                break;
                            }

                            // Update the pause state.
                            var pausing : Boolean = false;
                            if (paused != requestPaused) {
                                pausing = requestPaused;
                                paused = requestPaused;
                                (glThreadManager as Object).notifyAll();
                                if (LOG_PAUSE_RESUME) {
                                    Log.i("GLThread", "paused is now " + paused + " tid=" + getId());
                                }
                            }

                            // Do we need to give up the EGL context?
                            if (shouldReleaseEglContext) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "releasing EGL context because asked to tid=" + getId());
                                }
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                shouldReleaseEglContext = false;
                                askedToReleaseEglContext = true;
                            }

                            // Have we lost the EGL context?
                            if (lostEglContext) {
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                lostEglContext = false;
                            }

                            // When pausing, release the EGL surface:
                            if (pausing && haveEglSurface) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "releasing EGL surface because paused tid=" + getId());
                                }
                                stopEglSurfaceLocked();
                            }

                            // When pausing, optionally release the EGL Context:
                            if (pausing && haveEglContext) {
                                val view : GLTextureView? = glTextureViewWeakRef.get();
                                val preserveEglContextOnPause : Boolean =
                                        if(view == null) false else view.preserveEGLContextOnPause;
                                if (!preserveEglContextOnPause
                                        || glThreadManager.shouldReleaseEGLContextWhenPausing()) {
                                    stopEglContextLocked();
                                    if (LOG_SURFACE) {
                                        Log.i("GLThread", "releasing EGL context because paused tid=" + getId());
                                    }
                                }
                            }

                            // When pausing, optionally terminate EGL:
                            if (pausing) {
                                if (glThreadManager.shouldTerminateEGLWhenPausing()) {
                                    eglHelper.finish();
                                    if (LOG_SURFACE) {
                                        Log.i("GLThread", "terminating EGL because paused tid=" + getId());
                                    }
                                }
                            }

                            // Have we lost the TextureView surface?
                            if ((!hasSurface) && (!waitingForSurface)) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "noticed textureView surface lost tid=" + getId());
                                }
                                if (haveEglSurface) {
                                    stopEglSurfaceLocked();
                                }
                                waitingForSurface = true;
                                surfaceIsBad = false;
                                (glThreadManager as Object).notifyAll();
                            }

                            // Have we acquired the surface view surface?
                            if (hasSurface && waitingForSurface) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "noticed textureView surface acquired tid=" + getId());
                                }
                                waitingForSurface = false;
                                (glThreadManager as Object).notifyAll();
                            }

                            if (doRenderNotification) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "sending render notification tid=" + getId());
                                }
                                wantRenderNotification = false;
                                doRenderNotification = false;
                                renderComplete = true;
                                (glThreadManager as Object).notifyAll();
                            }

                            // Ready to draw?
                            if (readyToDraw()) {

                                // If we don't have an EGL context, try to acquire one.
                                if (!haveEglContext) {
                                    if (askedToReleaseEglContext) {
                                        askedToReleaseEglContext = false;
                                    } else if (glThreadManager.tryAcquireEglContextLocked(this)) {
                                        try {
                                            eglHelper.start();
                                        } catch (t : RuntimeException) {
                                            glThreadManager.releaseEglContextLocked(this);
                                            throw t;
                                        }
                                        haveEglContext = true;
                                        createEglContext = true;

                                        (glThreadManager as Object).notifyAll();
                                    }
                                }

                                if (haveEglContext && !haveEglSurface) {
                                    haveEglSurface = true;
                                    createEglSurface = true;
                                    createGlInterface = true;
                                    sizeChanged = true;
                                }

                                if (haveEglSurface) {
                                    if (this.sizeChanged) {
                                        sizeChanged = true;
                                        w = width;
                                        h = height;
                                        wantRenderNotification = true;
                                        if (LOG_SURFACE) {
                                            Log.i("GLThread", "noticing that we want render notification tid=" + getId());
                                        }

                                        // Destroy and recreate the EGL surface.
                                        createEglSurface = true;

                                        this.sizeChanged = false;
                                    }
                                    requestRender = false;
                                    (glThreadManager as Object).notifyAll();
                                    break;
                                }
                            }

                            // By design, this is the only place in a GLThread thread where we wait().
                            if (LOG_THREADS) {
                                Log.i("GLThread", "waiting tid=" + getId() + " haveEglContext: " + haveEglContext
                                        + " haveEglSurface: " + haveEglSurface + " paused: " + paused + " hasSurface: "
                                        + hasSurface + " surfaceIsBad: " + surfaceIsBad + " waitingForSurface: "
                                        + waitingForSurface + " width: " + width + " height: " + height
                                        + " requestRender: " + requestRender + " renderMode: " + renderMode);
                            }
                            (glThreadManager as Object).wait();
                        }
                    } // end of synchronized(glThreadManager)

                    if (event != null) {
                        event!!.run();
                        event = null;
                        continue;
                    }

                    if (createEglSurface) {
                        if (LOG_SURFACE) {
                            Log.w("GLThread", "egl createSurface");
                        }
                        if (!eglHelper.createSurface()) {
                            synchronized (glThreadManager) {
                                surfaceIsBad = true;
                                (glThreadManager as Object).notifyAll();
                            }
                            continue;
                        }
                        createEglSurface = false;
                    }

                    if (createGlInterface) {
                        gl = eglHelper.createGL() as GL10;

                        glThreadManager.checkGLDriver(gl);
                        createGlInterface = false;
                    }

                    if (createEglContext) {
                        if (LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceCreated");
                        }
                        val view : GLTextureView? = glTextureViewWeakRef.get();
                        if (view != null) {
                            view.renderer!!.onSurfaceCreated(gl!!, eglHelper.eglConfig!!);
                        }
                        createEglContext = false;
                    }

                    if (sizeChanged) {
                        if (LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceChanged(" + w + ", " + h + ")");
                        }
                        val view : GLTextureView? = glTextureViewWeakRef.get();
                        if (view != null) {
                            view.renderer!!.onSurfaceChanged(gl!!, w, h);
                        }
                        sizeChanged = false;
                    }

                    if (LOG_RENDERER_DRAW_FRAME) {
                        Log.w("GLThread", "onDrawFrame tid=" + getId());
                    }
                    run {
                        val view : GLTextureView? = glTextureViewWeakRef.get();
                        if (view != null) {
                            view.renderer!!.onDrawFrame(gl!!);
                        }
                    }
                    val swapError : Int = eglHelper.swap();
                    when (swapError) {
                        EGL10.EGL_SUCCESS -> {}
                        EGL11.EGL_CONTEXT_LOST -> {
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "egl context lost tid=" + getId());
                            }
                            lostEglContext = true;
                        }
                        else -> {
                            // Other errors typically mean that the current surface is bad,
                            // probably because the TextureView surface has been destroyed,
                            // but we haven't been notified yet.
                            // Log the error to help developers understand why rendering stopped.
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);

                            synchronized(glThreadManager) {
                                surfaceIsBad = true;
                                (glThreadManager as Object).notifyAll();
                            }
                            break;
                        }
                    }

                    if (wantRenderNotification) {
                        doRenderNotification = true;
                    }
                }
            } finally {
                /*
                 * clean-up everything...
                 */
                synchronized (glThreadManager) {
                    stopEglSurfaceLocked();
                    stopEglContextLocked();
                }
            }
        }

        fun ableToDraw() : Boolean {
            return haveEglContext && haveEglSurface && readyToDraw();
        }

        private fun readyToDraw() : Boolean {
            return (!paused) && hasSurface && (!surfaceIsBad) && (width > 0) && (height > 0) && (
                    requestRender || (renderMode == RENDERMODE_CONTINUOUSLY));
        }

        public fun setRenderMode(renderMode : Int) {
            if (!((RENDERMODE_WHEN_DIRTY <= renderMode) && (renderMode <= RENDERMODE_CONTINUOUSLY))) {
                throw IllegalArgumentException("renderMode");
            }
            synchronized (glThreadManager) {
                this.renderMode = renderMode;
                (glThreadManager as Object).notifyAll();
            }
        }

        public fun getRenderMode() : Int {
            synchronized (glThreadManager) {
                return renderMode;
            }
        }

        public fun requestRender() {
            synchronized (glThreadManager) {
                requestRender = true;
                (glThreadManager as Object).notifyAll();
            }
        }

        public fun surfaceCreated() {
            synchronized (glThreadManager) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "surfaceCreated tid=" + getId());
                }
                hasSurface = true;
                (glThreadManager as Object).notifyAll();
                while ((waitingForSurface) && (!exited)) {
                    try {
                        (glThreadManager as Object).wait();
                    } catch (e : InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public fun surfaceDestroyed() {
            synchronized (glThreadManager) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "surfaceDestroyed tid=" + getId());
                }
                hasSurface = false;
                (glThreadManager as Object).notifyAll();
                while ((!waitingForSurface) && (!exited)) {
                    try {
                        (glThreadManager as Object).wait();
                    } catch (e : InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public fun onPause() {
            synchronized (glThreadManager) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onPause tid=" + getId());
                }
                requestPaused = true;
                (glThreadManager as Object).notifyAll();
                while ((!exited) && (!paused)) {
                    if (LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onPause waiting for paused.");
                    }
                    try {
                        (glThreadManager as Object).wait();
                    } catch (ex : InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public fun onResume() {
            synchronized (glThreadManager) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onResume tid=" + getId());
                }
                requestPaused = false;
                requestRender = true;
                renderComplete = false;
                (glThreadManager  as Object).notifyAll();
                while ((!exited) && paused && (!renderComplete)) {
                    if (LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onResume waiting for !paused.");
                    }
                    try {
                        (glThreadManager as Object).wait();
                    } catch (ex : InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public fun onWindowResize(w : Int, h : Int) {
            synchronized (glThreadManager) {
                width = w;
                height = h;
                sizeChanged = true;
                requestRender = true;
                renderComplete = false;
                (glThreadManager as Object).notifyAll();

                // Wait for thread to react to resize and render a frame
                while (!exited && !paused && !renderComplete && ableToDraw()) {
                    if (LOG_SURFACE) {
                        Log.i("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
                    }
                    try {
                        (glThreadManager as Object).wait();
                    } catch (ex : InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public fun requestExitAndWait() {
            // don't call this from GLThread thread or it is a guaranteed
            // deadlock!
            synchronized (glThreadManager) {
                shouldExit = true;
                (glThreadManager as Object).notifyAll();
                while (!exited) {
                    try {
                        (glThreadManager as Object).wait();
                    } catch (ex : InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public fun requestReleaseEglContextLocked() {
            shouldReleaseEglContext = true;
            (glThreadManager as Object).notifyAll();
        }

        /**
         * Queue an "event" to be run on the GL rendering thread.
         *
         * @param r the runnable to be run on the GL rendering thread.
         */
        public fun queueEvent(r : Runnable) {
            if (r == null) {
                throw IllegalArgumentException("r must not be null");
            }
            synchronized (glThreadManager) {
                eventQueue.add(r);
                (glThreadManager as Object).notifyAll();
            }
        }

        // Once the thread is started, all accesses to the following member
        // variables are protected by the glThreadManager monitor
        private var shouldExit : Boolean = false;
        internal var exited : Boolean = false;
        private var requestPaused : Boolean = false;
        private var paused : Boolean = false;
        private var hasSurface : Boolean = false;
        private var surfaceIsBad : Boolean = false;
        private var waitingForSurface : Boolean = false;
        private var haveEglContext : Boolean = false;
        private var haveEglSurface : Boolean = false;
        private var shouldReleaseEglContext : Boolean = false;
        private var width : Int = 0;
        private var height : Int = 0;
        private var renderMode : Int = 0;
        private var requestRender : Boolean = false;
        private var renderComplete : Boolean = false;
        private val eventQueue : ArrayList<Runnable> = ArrayList<Runnable>();
        private var sizeChanged : Boolean = true;

        // End of member variables protected by the glThreadManager monitor.

        private lateinit var eglHelper : EglHelper

        /**
         * Set once at thread construction time, nulled out when the parent view is garbage
         * called. This weak reference allows the GLTextureView to be garbage collected while
         * the GLThread is still alive.
         */
        private var glTextureViewWeakRef : WeakReference<GLTextureView>;
    }

    class LogWriter : Writer() {

        public override fun close() {
            flushBuilder();
        }

        public override fun flush() {
            flushBuilder();
        }

        public override fun write(buf : CharArray, offset : Int, count : Int) {
            for(i in 0 until count) {
                val c : Char = buf[offset + i];
                if (c == '\n') {
                    flushBuilder();
                } else {
                    builder.append(c);
                }
            }
        }

        private fun flushBuilder() {
            if (builder.length > 0) {
                Log.v("GLTextureView", builder.toString());
                builder.delete(0, builder.length);
            }
        }

        private val builder : StringBuilder = StringBuilder();
    }

    private fun checkRenderThreadState() {
        if (glThread != null) {
            throw IllegalStateException("setRenderer has already been called for this instance.");
        }
    }

    private class GLThreadManager {

        @Synchronized
        public fun threadExiting(thread : GLThread) {
            if (LOG_THREADS) {
                Log.i("GLThread", "exiting tid=" + thread.getId());
            }
            thread.exited = true;
            if (eglOwner == thread) {
                eglOwner = null;
            }
            (this as Object).notifyAll();
        }

        /*
         * Tries once to acquire the right to use an EGL
         * context. Does not block. Requires that we are already
         * in the glThreadManager monitor when this is called.
         *
         * @return true if the right to use an EGL context was acquired.
         */
        public fun tryAcquireEglContextLocked(thread : GLThread) : Boolean {
            if (eglOwner == thread || eglOwner == null) {
                eglOwner = thread;
                (this as Object).notifyAll();
                return true;
            }
            checkGLESVersion();
            if (multipleGLESContextsAllowed) {
                return true;
            }
            // Notify the owning thread that it should release the context.
            // TODO: implement a fairness policy. Currently
            // if the owning thread is drawing continuously it will just
            // reacquire the EGL context.
            if (eglOwner != null) {
                eglOwner!!.requestReleaseEglContextLocked();
            }
            return false;
        }

        /*
         * Releases the EGL context. Requires that we are already in the
         * glThreadManager monitor when this is called.
         */
        public fun releaseEglContextLocked(thread : GLThread) {
            if (eglOwner == thread) {
                eglOwner = null;
            }
            (this as Object).notifyAll();
        }

        @Synchronized
        public fun shouldReleaseEGLContextWhenPausing() : Boolean {
            // Release the EGL context when pausing even if
            // the hardware supports multiple EGL contexts.
            // Otherwise the device could run out of EGL contexts.
            return limitedGLESContexts;
        }

        @Synchronized
        public fun shouldTerminateEGLWhenPausing() : Boolean {
            checkGLESVersion();
            return !multipleGLESContextsAllowed;
        }

        @Synchronized
        public fun checkGLDriver(gl : GL10) {
            if (!glesDriverCheckComplete) {
                checkGLESVersion();
                val renderer : String = gl.glGetString(GL10.GL_RENDERER);
                if (glesVersion < kGLES_20) {
                    multipleGLESContextsAllowed = !renderer.startsWith(kMSM7K_RENDERER_PREFIX);
                    (this as Object).notifyAll();
                }
                limitedGLESContexts = !multipleGLESContextsAllowed;
                if (LOG_SURFACE) {
                    Log.w(TAG, "checkGLDriver renderer = \"" + renderer + "\" multipleContextsAllowed = "
                            + multipleGLESContextsAllowed + " limitedGLESContexts = " + limitedGLESContexts);
                }
                glesDriverCheckComplete = true;
            }
        }

        private fun checkGLESVersion() {
            if (!glesVersionCheckComplete) {
                glesVersionCheckComplete = true;
            }
        }

        /**
         * This check was required for some pre-Android-3.0 hardware. Android 3.0 provides
         * support for hardware-accelerated views, therefore multiple EGL contexts are
         * supported on all Android 3.0+ EGL drivers.
         */
        private var glesVersionCheckComplete = false;
        private var glesVersion : Int = 0;
        private var glesDriverCheckComplete : Boolean = false;
        private var multipleGLESContextsAllowed : Boolean = false;
        private var limitedGLESContexts : Boolean = false;

        private var eglOwner : GLThread? = null;

        companion object {
            private val kGLES_20 : Int = 0x20000;
            private val kMSM7K_RENDERER_PREFIX : String = "Q3Dimension MSM7500 ";
            private val TAG : String = "GLThreadManager";
        }
    }

    private val mThisWeakRef : WeakReference<GLTextureView> = WeakReference<GLTextureView>(this);
    private var glThread : GLThread? = null;
    private var renderer : Renderer? = null;
    private var detached : Boolean = false;
    private var eglConfigChooser : EGLConfigChooser? = null
    private var eglContextFactory : EGLContextFactory? = null;
    private var eglWindowSurfaceFactory : EGLWindowSurfaceFactory? = null;
    private var glWrapper : GLWrapper? = null
    private var debugFlags : Int = 0;
    private var eglContextClientVersion : Int = 0;
    private var preserveEGLContextOnPause : Boolean = false;
    private val surfaceTextureListeners : MutableList<SurfaceTextureListener> = ArrayList<SurfaceTextureListener>()
}
