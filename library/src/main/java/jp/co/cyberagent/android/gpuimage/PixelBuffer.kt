package jp.co.cyberagent.android.gpuimage

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

import javax.microedition.khronos.egl.EGL10.EGL_ALPHA_SIZE
import javax.microedition.khronos.egl.EGL10.EGL_BLUE_SIZE
import javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY
import javax.microedition.khronos.egl.EGL10.EGL_DEPTH_SIZE
import javax.microedition.khronos.egl.EGL10.EGL_GREEN_SIZE
import javax.microedition.khronos.egl.EGL10.EGL_HEIGHT
import javax.microedition.khronos.egl.EGL10.EGL_NONE
import javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT
import javax.microedition.khronos.egl.EGL10.EGL_RED_SIZE
import javax.microedition.khronos.egl.EGL10.EGL_STENCIL_SIZE
import javax.microedition.khronos.egl.EGL10.EGL_WIDTH

class PixelBuffer {

    companion object {
        private const val TAG : String = "PixelBuffer"
        private const val LIST_CONFIGS : Boolean = false
    }


    private lateinit var renderer : GLSurfaceView.Renderer // borrow this interface
    private val width : Int
    private val height : Int
    private lateinit var bitmap : Bitmap

    private lateinit var egl10 : EGL10
    private lateinit var eglDisplay : EGLDisplay
    private lateinit var eglConfigs : Array<EGLConfig>
    private lateinit var eglConfig : EGLConfig
    private lateinit var eglContext : EGLContext
    private lateinit var eglSurface : EGLSurface
    private lateinit var gl10 : GL10

    private lateinit var mThreadOwner : String

    constructor(width : Int, height : Int) {
        this.width = width;
        this.height = height;

        val version = IntArray(2)
        val attribList : IntArray = intArrayOf(
                EGL_WIDTH, this.width,
                EGL_HEIGHT, this.height,
                EGL_NONE
        )

        // No error checking performed, minimum required code to elucidate logic
        egl10 = EGLContext.getEGL() as EGL10
        eglDisplay = egl10.eglGetDisplay(EGL_DEFAULT_DISPLAY)
        egl10.eglInitialize(eglDisplay, version)
        eglConfig = chooseConfig(); // Choosing a config is a little more
        // complicated

        // eglContext = egl10.eglCreateContext(eglDisplay, eglConfig,
        // EGL_NO_CONTEXT, null);
        val EGL_CONTEXT_CLIENT_VERSION : Int = 0x3098;
        val attrib_list : IntArray = intArrayOf(
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        )
        eglContext = egl10.eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, attrib_list);

        eglSurface = egl10.eglCreatePbufferSurface(eglDisplay, eglConfig, attribList);
        egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

        gl10 = eglContext.getGL() as GL10

        // Record thread owner of OpenGL context
        mThreadOwner = Thread.currentThread().getName()
    }

    public fun setRenderer(renderer : GLSurfaceView.Renderer) {
        this.renderer = renderer

        // Does this thread own the OpenGL context?
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "setRenderer: This thread does not own the OpenGL context.");
            return;
        }

        // Call the renderer initialization routines
        this.renderer.onSurfaceCreated(gl10, eglConfig);
        this.renderer.onSurfaceChanged(gl10, width, height);
    }

    public fun getBitmap() : Bitmap? {
        // Do we have a renderer?
        if (renderer == null) {
            Log.e(TAG, "getBitmap: Renderer was not set.");
            return null;
        }

        // Does this thread own the OpenGL context?
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.");
            return null;
        }

        // Call the renderer draw routine (it seems that some filters do not
        // work if this is only called once)
        renderer.onDrawFrame(gl10);
        renderer.onDrawFrame(gl10);
        convertToBitmap();
        return bitmap;
    }

    public fun destroy() {
        renderer.onDrawFrame(gl10);
        renderer.onDrawFrame(gl10);
        egl10.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);

        egl10.eglDestroySurface(eglDisplay, eglSurface);
        egl10.eglDestroyContext(eglDisplay, eglContext);
        egl10.eglTerminate(eglDisplay);
    }

    private fun chooseConfig() : EGLConfig {
        val attribList : IntArray = intArrayOf(
                EGL_DEPTH_SIZE, 0,
                EGL_STENCIL_SIZE, 0,
                EGL_RED_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_BLUE_SIZE, 8,
                EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL_NONE
        )

        // No error checking performed, minimum required code to elucidate logic
        // Expand on this logic to be more selective in choosing a configuration
        val numConfig = IntArray(1)
        egl10.eglChooseConfig(eglDisplay, attribList, null, 0, numConfig);
        val configSize : Int = numConfig[0];
        // TODO check this syntax works
        // previously : eglConfigs = new EGLConfig[configSize];
        eglConfigs = Array(configSize) {
            object : EGLConfig() {

            }
        }

        egl10.eglChooseConfig(eglDisplay, attribList, eglConfigs, configSize, numConfig);

        if (LIST_CONFIGS) {
            listConfig();
        }

        return eglConfigs[0]; // Best match is probably the first configuration
    }

    private fun listConfig() {
        Log.i(TAG, "Config List {");

        for (config : EGLConfig in eglConfigs) {

            // Expand on this logic to dump other attributes
            val d : Int = getConfigAttrib(config, EGL_DEPTH_SIZE);
            val s : Int = getConfigAttrib(config, EGL_STENCIL_SIZE);
            val r : Int = getConfigAttrib(config, EGL_RED_SIZE);
            val g : Int = getConfigAttrib(config, EGL_GREEN_SIZE);
            val b : Int = getConfigAttrib(config, EGL_BLUE_SIZE);
            val a : Int = getConfigAttrib(config, EGL_ALPHA_SIZE);
            Log.i(TAG, "    <d,s,r,g,b,a> = <" + d + "," + s + "," +
                    r + "," + g + "," + b + "," + a + ">");
        }

        Log.i(TAG, "}");
    }

    private fun getConfigAttrib(config : EGLConfig, attribute : Int) : Int {
        val value = IntArray(1)
        return if(egl10.eglGetConfigAttrib(eglDisplay, config,
                attribute, value)) value[0] else 0
    }

    private fun convertToBitmap() {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        GPUImageNativeLibrary.adjustBitmap(bitmap)
    }
}