/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Semaphore;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.Rotation;

import jp.co.cyberagent.android.gpuimage.GPUImage.SURFACE_TYPE_SURFACE_VIEW;
import jp.co.cyberagent.android.gpuimage.GPUImage.SURFACE_TYPE_TEXTURE_VIEW;

public class GPUImageView : FrameLayout {

    companion object {
        const val RENDERMODE_WHEN_DIRTY : Int = 0
        const val RENDERMODE_CONTINUOUSLY : Int = 1
    }

    private var surfaceType : Int = SURFACE_TYPE_SURFACE_VIEW;
    private lateinit var surfaceView : View
    private lateinit var gpuImage : GPUImage
    private var isShowLoading : Boolean = true;
    private var filter : GPUImageFilter? = null
    public var forceSize : Size? = null;
    private var ratio : Float = 0.0f;


    constructor(context : Context) : super(context) {
        init(context, null)
    }

    constructor(context : Context, attrs : AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context : Context, attrs : AttributeSet?) {
        if (attrs != null) {
            val a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.GPUImageView, 0, 0);
            try {
                surfaceType = a.getInt(R.styleable.GPUImageView_gpuimage_surface_type, surfaceType);
                isShowLoading = a.getBoolean(R.styleable.GPUImageView_gpuimage_show_loading, isShowLoading);
            } finally {
                a.recycle();
            }
        }
        gpuImage = GPUImage(context);
        if (surfaceType == SURFACE_TYPE_TEXTURE_VIEW) {
            surfaceView = GPUImageGLTextureView(context, attrs);
            gpuImage.setGLTextureView(surfaceView as GLTextureView);
        } else {
            surfaceView = GPUImageGLSurfaceView(context, attrs);
            gpuImage.setGLSurfaceView(surfaceView as GLSurfaceView);
        }
        addView(surfaceView);
    }

    protected override fun onMeasure(widthMeasureSpec : Int, heightMeasureSpec : Int) {
        if (ratio != 0.0f) {
            val width : Int = MeasureSpec.getSize(widthMeasureSpec);
            val height : Int = MeasureSpec.getSize(heightMeasureSpec);

            val newHeight : Int
            val newWidth : Int
            if (width / ratio < height) {
                newWidth = width;
                newHeight = Math.round(width / ratio);
            } else {
                newHeight = height;
                newWidth = Math.round(height * ratio);
            }

            val newWidthSpec : Int = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY);
            val newHeightSpec : Int = MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY);
            super.onMeasure(newWidthSpec, newHeightSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * Retrieve the GPUImage instance used by this view.
     *
     * @return used GPUImage instance
     */
    public fun getGPUImage() : GPUImage {
        return gpuImage;
    }

    /**
     * Deprecated: Please call
     * {@link GPUImageView#updatePreviewFrame(byte[], int, int)} frame by frame
     * <p>
     * Sets the up camera to be connected to GPUImage to get a filtered preview.
     *
     * @param camera the camera
     */
    @Deprecated("now deprecated")
    public fun setUpCamera(camera : Camera) {
        gpuImage.setUpCamera(camera);
    }

    /**
     * Deprecated: Please call
     * {@link GPUImageView#updatePreviewFrame(byte[], int, int)} frame by frame
     * <p>
     * Sets the up camera to be connected to GPUImage to get a filtered preview.
     *
     * @param camera         the camera
     * @param degrees        by how many degrees the image should be rotated
     * @param flipHorizontal if the image should be flipped horizontally
     * @param flipVertical   if the image should be flipped vertically
     */
    @Deprecated("its depped")
    public fun setUpCamera(camera : Camera, degrees : Int, flipHorizontal : Boolean, flipVertical : Boolean) {
        gpuImage.setUpCamera(camera, degrees, flipHorizontal, flipVertical);
    }

    /**
     * Update camera preview frame with YUV format data.
     *
     * @param data   Camera preview YUV data for frame.
     * @param width  width of camera preview
     * @param height height of camera preview
     */
    public fun updatePreviewFrame(data : ByteArray, width : Int, height : Int) {
        gpuImage.updatePreviewFrame(data, width, height);
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public fun setBackgroundColor(red : Float, green : Float, blue : Float) {
        gpuImage.setBackgroundColor(red, green, blue);
    }

    /**
     * Set the rendering mode. When renderMode is
     * RENDERMODE_CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When renderMode
     * is RENDERMODE_WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when {@link #requestRender} is called. Defaults to RENDERMODE_CONTINUOUSLY.
     *
     * @param renderMode one of the RENDERMODE_X constants
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     * @see GLSurfaceView#setRenderMode(int)
     * @see GLTextureView#setRenderMode(int)
     */
    public fun setRenderMode(renderMode : Int) {
        if (surfaceView is GLSurfaceView) {
            (surfaceView as GLSurfaceView).setRenderMode(renderMode);
        } else if (surfaceView is GLTextureView) {
            (surfaceView as GLTextureView).setRenderMode(renderMode);
        }
    }

    // TODO Should be an xml attribute. But then GPUImage can not be distributed as .jar anymore.
    public fun setRatio(ratio : Float) {
        this.ratio = ratio;
        surfaceView.requestLayout();
        gpuImage.deleteImage();
    }

    /**
     * Set the scale type of GPUImage.
     *
     * @param scaleType the new ScaleType
     */
    public fun setScaleType(scaleType : GPUImage.ScaleType) {
        gpuImage.setScaleType(scaleType);
    }

    /**
     * Sets the rotation of the displayed image.
     *
     * @param rotation new rotation
     */
    public fun setRotation(rotation : Rotation) {
        gpuImage.setRotation(rotation);
        requestRender();
    }

    /**
     * Set the filter to be applied on the image.
     *
     * @param filter Filter that should be applied on the image.
     */
    public fun setFilter(filter : GPUImageFilter) {
        this.filter = filter;
        gpuImage.setFilter(filter);
        requestRender();
    }

    /**
     * Get the current applied filter.
     *
     * @return the current filter
     */
    public fun getFilter() : GPUImageFilter? {
        return filter;
    }

    /**
     * Sets the image on which the filter should be applied.
     *
     * @param bitmap the new image
     */
    public fun setImage(bitmap : Bitmap) {
        gpuImage.setImage(bitmap);
    }

    /**
     * Sets the image on which the filter should be applied from a Uri.
     *
     * @param uri the uri of the new image
     */
    public fun setImage(uri : Uri) {
        gpuImage.setImage(uri);
    }

    /**
     * Sets the image on which the filter should be applied from a File.
     *
     * @param file the file of the new image
     */
    public fun setImage(file : File) {
        gpuImage.setImage(file);
    }

    public fun requestRender() {
        if (surfaceView is GLSurfaceView) {
            (surfaceView as GLSurfaceView).requestRender();
        } else if (surfaceView is GLTextureView) {
            (surfaceView as GLTextureView).requestRender();
        }
    }

    /**
     * Save current image with applied filter to Pictures. It will be stored on
     * the default Picture folder on the phone below the given folderName and
     * fileName. <br>
     * This method is async and will notify when the image was saved through the
     * listener.
     *
     * @param folderName the folder name
     * @param fileName   the file name
     * @param listener   the listener
     */
    public fun saveToPictures(folderName : String, fileName : String, listener : OnPictureSavedListener) {
        SaveTask(folderName, fileName, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Save current image with applied filter to Pictures. It will be stored on
     * the default Picture folder on the phone below the given folderName and
     * fileName. <br>
     * This method is async and will notify when the image was saved through the
     * listener.
     *
     * @param folderName the folder name
     * @param fileName   the file name
     * @param width      requested output width
     * @param height     requested output height
     * @param listener   the listener
     */
    public fun saveToPictures(folderName : String, fileName : String,
                               width : Int, height : Int,
                               listener : OnPictureSavedListener) {
        SaveTask(folderName, fileName, width, height, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Retrieve current image with filter applied and given size as Bitmap.
     *
     * @param width  requested Bitmap width
     * @param height requested Bitmap height
     * @return Bitmap of picture with given size
     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    public fun capture(width : Int, height : Int) : Bitmap {
        // This method needs to run on a background thread because it will take a longer time
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalStateException("Do not call this method from the UI thread!");
        }

        forceSize = Size(width, height);

        val waiter : Semaphore = Semaphore(0);

        // Layout with new size
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                waiter.release();
            }
        })

        post {
            // Optionally, show loading view:
            if (isShowLoading) {
                addView(LoadingView(getContext()));
            }
            // Request layout to release waiter:
            surfaceView.requestLayout();
        }

        waiter.acquire();

        // Run one render pass
        gpuImage.runOnGLThread {
            waiter.release()
        }
        requestRender();
        waiter.acquire();
        val bitmap : Bitmap = capture();


        forceSize = null;
        post {
            surfaceView.requestLayout();
        }
        requestRender();

        if (isShowLoading) {
            postDelayed({
                removeViewAt(1);
            },3000)
        }

        return bitmap;
    }

    /**
     * Capture the current image with the size as it is displayed and retrieve it as Bitmap.
     *
     * @return current output as Bitmap
     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    public fun capture() : Bitmap {
        val waiter : Semaphore = Semaphore(0);

        val width : Int = surfaceView.getMeasuredWidth();
        val height : Int  = surfaceView.getMeasuredHeight();

        // Take picture on OpenGL thread
        val resultBitmap : Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        gpuImage.runOnGLThread {
            GPUImageNativeLibrary.adjustBitmap(resultBitmap);
            waiter.release();
        }
        requestRender();
        waiter.acquire();

        return resultBitmap;
    }

    /**
     * Pauses the Surface.
     */
    public fun onPause() {
        if (surfaceView is GLSurfaceView) {
            (surfaceView as GLSurfaceView).onPause();
        } else if (surfaceView is GLTextureView) {
            (surfaceView as GLTextureView).onPause();
        }
    }

    /**
     * Resumes the Surface.
     */
    public fun onResume() {
        if (surfaceView is GLSurfaceView) {
            (surfaceView as GLSurfaceView).onResume();
        } else if (surfaceView is GLTextureView) {
            (surfaceView as GLTextureView).onResume();
        }
    }

    data class Size(val width : Int, val height : Int)

    private inner class GPUImageGLSurfaceView : GLSurfaceView {

        constructor(context : Context) : super(context)

        constructor(context : Context, attrs : AttributeSet?) : super(context, attrs)

        override fun onMeasure(widthMeasureSpec : Int, heightMeasureSpec : Int) {
            if (forceSize != null) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(forceSize!!.width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(forceSize!!.height, MeasureSpec.EXACTLY));
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    private inner class GPUImageGLTextureView : GLTextureView {

        constructor(context : Context) : super(context)

        constructor(context : Context, attrs : AttributeSet?) : super(context,attrs)

        override fun onMeasure(widthMeasureSpec : Int, heightMeasureSpec : Int) {
            if (forceSize != null) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(forceSize!!.width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(forceSize!!.height, MeasureSpec.EXACTLY));
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    private class LoadingView : FrameLayout {
        constructor(context : Context) : super(context) {
            init();
        }

        constructor(context : Context, attrs : AttributeSet) : super(context, attrs) {
            init();
        }

        constructor(context : Context, attrs : AttributeSet, defStyle : Int) : super(context, attrs, defStyle) {
            init();
        }

        private fun init() {
            val view : ProgressBar = ProgressBar(getContext());
            view.setLayoutParams(
                    LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            addView(view);
            setBackgroundColor(Color.BLACK);
        }
    }

    private inner class SaveTask(
        private val folderName : String,
        private val fileName : String,
        private val width : Int,
        private val height : Int,
        private val listener : OnPictureSavedListener,
        private val handler : Handler = Handler()
    ) : AsyncTask<Void, Void, Void?>() {


        public constructor(folderName : String, fileName : String, listener : OnPictureSavedListener) : this(folderName, fileName, 0, 0, listener)

        override fun doInBackground(vararg params: Void): Void? {
            try {
                val result : Bitmap = if(width != 0) capture(width, height) else capture()
                saveImage(folderName, fileName, result)
            } catch (e : InterruptedException) {
                e.printStackTrace();
            }
            return null
        }

        private fun saveImage(folderName : String, fileName : String, image : Bitmap) {
            var path : File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            var file : File = File(path, folderName + "/" + fileName);
            try {
                file.getParentFile().mkdirs();
                image.compress(Bitmap.CompressFormat.JPEG, 80, FileOutputStream(file));
                MediaScannerConnection.scanFile(getContext(), arrayOf(file.toString()), null) { path, uri ->
                    if (listener != null && uri != null) {
                        handler.post {
                            listener.onPictureSaved(uri);
                        }
                    }
                };
            } catch (e : FileNotFoundException) {
                e.printStackTrace();
            }
        }
    }

    fun interface OnPictureSavedListener {
        fun onPictureSaved(uri : Uri)
    }
}
