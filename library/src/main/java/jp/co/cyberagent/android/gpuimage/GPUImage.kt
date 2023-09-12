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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Display;
import android.view.WindowManager;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.Rotation;

import java.io.*;
import java.net.URL;

/**
 * The main accessor for GPUImage functionality. This class helps to do common
 * tasks through a simple interface.
 */
public class GPUImage {

    public enum class ScaleType {CENTER_INSIDE, CENTER_CROP}

    companion object {

        const val SURFACE_TYPE_SURFACE_VIEW : Int = 0
        const val SURFACE_TYPE_TEXTURE_VIEW : Int = 1

        /**
         * Gets the images for multiple filters on a image. This can be used to
         * quickly get thumbnail images for filters. <br>
         * Whenever a new Bitmap is ready, the listener will be called with the
         * bitmap. The order of the calls to the listener will be the same as the
         * filter order.
         *
         * @param bitmap   the bitmap on which the filters will be applied
         * @param filters  the filters which will be applied on the bitmap
         * @param listener the listener on which the results will be notified
         */
        public fun getBitmapForMultipleFilters(bitmap : Bitmap, filters : List<GPUImageFilter>, listener : ResponseListener<Bitmap>) {
            if (filters.isEmpty()) {
                return;
            }
            val renderer : GPUImageRenderer = GPUImageRenderer(filters.get(0));
            renderer.setImageBitmap(bitmap, false);
            val buffer : PixelBuffer = PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
            buffer.setRenderer(renderer);

            for (filter in filters) {
                renderer.setFilter(filter);
                listener.response(buffer.getBitmap()!!)
                filter.destroy();
            }
            renderer.deleteImage();
            buffer.destroy();
        }


    }

    private val context : Context
    private val renderer : GPUImageRenderer
    private var surfaceType : Int = SURFACE_TYPE_SURFACE_VIEW;
    private var glSurfaceView : GLSurfaceView? = null;
    private var glTextureView : GLTextureView? = null;
    private var filter : GPUImageFilter
    private var currentBitmap : Bitmap? = null;
    private var scaleType : ScaleType = ScaleType.CENTER_CROP;
    private var scaleWidth : Int = 0
    private var scaleHeight : Int = 0;

    /**
     * Instantiates a new GPUImage object.
     *
     * @param context the context
     */
    public constructor(context : Context) {
        if (!supportsOpenGLES2(context)) {
            throw IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        this.context = context;
        filter = GPUImageFilter();
        renderer = GPUImageRenderer(filter);
    }

    /**
     * Checks if OpenGL ES 2.0 is supported on the current device.
     *
     * @param context the context
     * @return true, if successful
     */
    private fun supportsOpenGLES2(context : Context) : Boolean {
        val activityManager : ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo : ConfigurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    /**
     * Sets the GLSurfaceView which will display the preview.
     *
     * @param view the GLSurfaceView
     */
    public fun setGLSurfaceView(view : GLSurfaceView) {
        surfaceType = SURFACE_TYPE_SURFACE_VIEW;
        glSurfaceView = view;
        glSurfaceView!!.setEGLContextClientVersion(2);
        glSurfaceView!!.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView!!.getHolder().setFormat(PixelFormat.RGBA_8888);
        glSurfaceView!!.setRenderer(renderer);
        glSurfaceView!!.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glSurfaceView!!.requestRender();
    }

    /**
     * Sets the GLTextureView which will display the preview.
     *
     * @param view the GLTextureView
     */
    public fun setGLTextureView(view : GLTextureView) {
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW;
        glTextureView = view;
        glTextureView!!.setEGLContextClientVersion(2);
        glTextureView!!.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glTextureView!!.setOpaque(false);
        glTextureView!!.setRenderer(renderer);
        glTextureView!!.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glTextureView!!.requestRender();
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public fun setBackgroundColor(red : Float, green : Float, blue : Float) {
        renderer.setBackgroundColor(red, green, blue);
    }

    /**
     * Request the preview to be rendered again.
     */
    public fun requestRender() {
        if (surfaceType == SURFACE_TYPE_SURFACE_VIEW) {
            if (glSurfaceView != null) {
                glSurfaceView!!.requestRender();
            }
        } else if (surfaceType == SURFACE_TYPE_TEXTURE_VIEW) {
            if (glTextureView != null) {
                glTextureView!!.requestRender();
            }
        }
    }

    /**
     * Deprecated: Please call
     * {@link GPUImageNew#updatePreviewFrame(byte[], int, int)} frame by frame
     * <p>
     * Sets the up camera to be connected to GPUImage to get a filtered preview.
     *
     * @param camera the camera
     */
    @Deprecated("its deprecated bro")
    public fun setUpCamera(camera : Camera) {
        setUpCamera(camera, 0, false, false);
    }

    /**
     * Deprecated: Please call
     * {@link GPUImageNew#updatePreviewFrame(byte[], int, int)} frame by frame
     * <p>
     * Sets the up camera to be connected to GPUImage to get a filtered preview.
     *
     * @param camera         the camera
     * @param degrees        by how many degrees the image should be rotated
     * @param flipHorizontal if the image should be flipped horizontally
     * @param flipVertical   if the image should be flipped vertically
     */
    public fun setUpCamera(camera : Camera, degrees : Int, flipHorizontal : Boolean,
                            flipVertical : Boolean) {
        if (surfaceType == SURFACE_TYPE_SURFACE_VIEW) {
            glSurfaceView!!.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        } else if (surfaceType == SURFACE_TYPE_TEXTURE_VIEW) {
            glTextureView!!.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }
        renderer.setUpSurfaceTexture(camera);
        var rotation : Rotation = Rotation.NORMAL;
        when (degrees) {
            90 -> {
                rotation = Rotation.ROTATION_90;
            }
            180 -> {
                rotation = Rotation.ROTATION_180;
            }
            270 -> {
                rotation = Rotation.ROTATION_270;
            }
        }
        renderer.setRotationCamera(rotation, flipHorizontal, flipVertical);
    }

    /**
     * Sets the filter which should be applied to the image which was (or will
     * be) set by setImage(...).
     *
     * @param filter the new filter
     */
    public fun setFilter(filter : GPUImageFilter) {
        this.filter = filter;
        renderer.setFilter(this.filter);
        requestRender();
    }

    /**
     * Sets the image on which the filter should be applied.
     *
     * @param bitmap the new image
     */
    public fun setImage(bitmap : Bitmap) {
        currentBitmap = bitmap;
        renderer.setImageBitmap(bitmap, false);
        requestRender();
    }

    /**
     * Update camera preview frame with YUV format data.
     *
     * @param data   Camera preview YUV data for frame.
     * @param width  width of camera preview
     * @param height height of camera preview
     */
    public fun updatePreviewFrame(data : ByteArray, width : Int, height : Int) {
        renderer.onPreviewFrame(data, width, height);
    }

    /**
     * This sets the scale type of GPUImage. This has to be run before setting the image.
     * If image is set and scale type changed, image needs to be reset.
     *
     * @param scaleType The new ScaleType
     */
    public fun setScaleType(scaleType : ScaleType) {
        this.scaleType = scaleType;
        renderer.setScaleType(scaleType);
        renderer.deleteImage();
        currentBitmap = null;
        requestRender();
    }

    /**
     * This gets the size of the image. This makes it easier to adjust
     * the size of your imagePreview to the the size of the scaled image.
     *
     * @return array with width and height of bitmap image
     */
    public fun getScaleSize() : IntArray {
        return intArrayOf(scaleWidth,scaleHeight)
    }

    /**
     * Sets the rotation of the displayed image.
     *
     * @param rotation new rotation
     */
    public fun setRotation(rotation : Rotation) {
        renderer.setRotation(rotation);
    }

    /**
     * Sets the rotation of the displayed image with flip options.
     *
     * @param rotation new rotation
     */
    public fun setRotation(rotation : Rotation, flipHorizontal : Boolean, flipVertical : Boolean) {
        renderer.setRotation(rotation, flipHorizontal, flipVertical);
    }

    /**
     * Deletes the current image.
     */
    public fun deleteImage() {
        renderer.deleteImage();
        currentBitmap = null;
        requestRender();
    }

    /**
     * Sets the image on which the filter should be applied from a Uri.
     *
     * @param uri the uri of the new image
     */
    public fun setImage(uri : Uri) {
        LoadImageUriTask(this, uri).execute();
    }

    /**
     * Sets the image on which the filter should be applied from a File.
     *
     * @param file the file of the new image
     */
    public fun setImage(file : File) {
        LoadImageFileTask(this, file).execute();
    }

    private fun getPath(uri : Uri) : String? {
        val projection : Array<String> = arrayOf(
                MediaStore.Images.Media.DATA,
        );
        val cursor : Cursor? = context.getContentResolver()
                .query(uri, projection, null, null, null);
        var path : String? = null;
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            val pathIndex : Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            path = cursor.getString(pathIndex);
        }
        cursor.close();
        return path;
    }

    /**
     * Gets the current displayed image with applied filter as a Bitmap.
     *
     * @return the current image with filter applied
     */
    public fun getBitmapWithFilterApplied() : Bitmap? {
        return currentBitmap?.let { getBitmapWithFilterApplied(it) };
    }

    /**
     * Gets the given bitmap with current filter applied as a Bitmap.
     *
     * @param bitmap the bitmap on which the current filter should be applied
     * @return the bitmap with filter applied
     */
    public fun getBitmapWithFilterApplied(bitmap : Bitmap) : Bitmap? {
        return getBitmapWithFilterApplied(bitmap, false);
    }

    /**
     * Gets the given bitmap with current filter applied as a Bitmap.
     *
     * @param bitmap  the bitmap on which the current filter should be applied
     * @param recycle recycle the bitmap or not.
     * @return the bitmap with filter applied
     */
    public fun getBitmapWithFilterApplied(bitmap : Bitmap, recycle : Boolean) : Bitmap? {
        if (glSurfaceView != null || glTextureView != null) {
            renderer.deleteImage();
            renderer.runOnDraw {
                synchronized (filter) {
                    filter.destroy();
                    (filter as Object).notify();
                }
            }
            synchronized (filter) {
                requestRender();
                try {
                    (filter as Object).wait();
                } catch (e : InterruptedException) {
                    e.printStackTrace();
                }
            }
        }

        val renderer : GPUImageRenderer = GPUImageRenderer(filter);
        renderer.setRotation(Rotation.NORMAL,
                this.renderer.isFlippedHorizontally(), this.renderer.isFlippedVertically());
        renderer.setScaleType(scaleType);
        val buffer : PixelBuffer = PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);
        renderer.setImageBitmap(bitmap, recycle);
        val result : Bitmap? = buffer.getBitmap();
        filter.destroy();
        renderer.deleteImage();
        buffer.destroy();

        this.renderer.setFilter(filter);
        if (currentBitmap != null) {
            this.renderer.setImageBitmap(currentBitmap!!, false);
        }
        requestRender();

        return result;
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
        saveToPictures(currentBitmap!!, folderName, fileName, listener);
    }

    /**
     * Apply and save the given bitmap with applied filter to Pictures. It will
     * be stored on the default Picture folder on the phone below the given
     * folerName and fileName. <br>
     * This method is async and will notify when the image was saved through the
     * listener.
     *
     * @param bitmap     the bitmap
     * @param folderName the folder name
     * @param fileName   the file name
     * @param listener   the listener
     */
    public fun saveToPictures(bitmap : Bitmap, folderName : String, fileName : String, listener : OnPictureSavedListener) {
        SaveTask(bitmap, folderName, fileName, listener).execute();
    }

    /**
     * Runs the given Runnable on the OpenGL thread.
     *
     * @param runnable The runnable to be run on the OpenGL thread.
     */
    fun runOnGLThread(runnable : Runnable) {
        renderer.runOnDrawEnd(runnable);
    }

    private fun getOutputWidth() : Int {
        if (renderer != null && renderer.getFrameWidth() != 0) {
            return renderer.getFrameWidth();
        } else if (currentBitmap != null) {
            return currentBitmap!!.getWidth();
        } else {
            val windowManager : WindowManager =
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager;
            val display : Display = windowManager.getDefaultDisplay();
            return display.getWidth();
        }
    }

    private fun getOutputHeight() : Int {
        return if (renderer != null && renderer.getFrameHeight() != 0) {
            renderer.getFrameHeight();
        } else if (currentBitmap != null) {
            currentBitmap!!.getHeight();
        } else {
            val windowManager : WindowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager;
            val display : Display = windowManager.getDefaultDisplay();
            display.getHeight();
        }
    }

    @Deprecated("its deprecated")
    private inner class SaveTask(
        private val bitmap : Bitmap,
        private val folderName : String,
        private val fileName : String,
        private val listener : OnPictureSavedListener,
        private val handler : Handler = Handler()
    ) : AsyncTask<Void, Void, Void>() {


        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): Void? {
            val result : Bitmap? = getBitmapWithFilterApplied(bitmap);
            saveImage(folderName, fileName, result!!);
            return null;
        }

        private fun saveImage(folderName : String, fileName : String, image : Bitmap) {
            val path : File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            val file : File = File(path, folderName + "/" + fileName);
            try {
                file.getParentFile().mkdirs();
                image.compress(CompressFormat.JPEG, 80, FileOutputStream(file));
                MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null) { path, uri ->
                    if (listener != null) {
                        handler.post {
                            listener.onPictureSaved(uri);
                        };
                    }
                };
            } catch (e : FileNotFoundException) {
                e.printStackTrace();
            }
        }
    }

    public interface OnPictureSavedListener {
        fun onPictureSaved(uri : Uri)
    }

    private inner class LoadImageUriTask(gpuImage : GPUImage, private val uri : Uri) : LoadImageTask(gpuImage) {

        protected override fun decode(options : BitmapFactory.Options) : Bitmap? {
            try {
                val inputStream : InputStream
                if (uri.getScheme()!!.startsWith("http") || uri.getScheme()!!.startsWith("https")) {
                    inputStream = URL(uri.toString()).openStream();
                } else if (uri.getPath()!!.startsWith("/android_asset/")) {
                    inputStream = context.getAssets().open(uri.getPath()!!.substring(("/android_asset/").length));
                } else {
                    inputStream = context.getContentResolver().openInputStream(uri)!!
                }
                return BitmapFactory.decodeStream(inputStream, null, options);
            } catch (e : Exception) {
                e.printStackTrace();
            }
            return null;
        }

        @Throws(IOException::class)
        protected override fun getImageOrientation() : Int {
            val cursor : Cursor? = context.getContentResolver().query(uri,
                    arrayOf(MediaStore.Images.ImageColumns.ORIENTATION), null, null, null);

            if (cursor == null || cursor.getCount() != 1) {
                return 0
            }

            cursor.moveToFirst();
            val orientation : Int = cursor.getInt(0);
            cursor.close();
            return orientation;
        }
    }

    private inner class LoadImageFileTask(gpuImage : GPUImage, private val imageFile : File) : LoadImageTask(gpuImage) {

        @Throws(IOException::class)
        protected override fun decode(options : BitmapFactory.Options) : Bitmap {
            return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        }

        @Throws(IOException::class)
        protected override fun getImageOrientation() : Int {
            val exif = ExifInterface(imageFile.getAbsolutePath());
            val orientation : Int = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            return when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> 0;
                ExifInterface.ORIENTATION_ROTATE_90 -> 90;
                ExifInterface.ORIENTATION_ROTATE_180 -> 180;
                ExifInterface.ORIENTATION_ROTATE_270 -> 270;
                else -> 0
            }
        }
    }

    private abstract inner class LoadImageTask(private val gpuImage: GPUImage) : AsyncTask<Void, Void, Bitmap?>() {

        private var outputWidth : Int = 0;
        private var outputHeight : Int = 0;

        @Deprecated("Deprecated in Java")
        protected override fun doInBackground(vararg params : Void?) : Bitmap? {
            if (renderer != null && renderer.getFrameWidth() == 0) {
                try {
                    synchronized (renderer.surfaceChangedWaiter) {
                        renderer.surfaceChangedWaiter.wait(3000);
                    }
                } catch (e : InterruptedException) {
                    e.printStackTrace();
                }
            }
            outputWidth = getOutputWidth();
            outputHeight = getOutputHeight();
            return loadResizedImage();
        }

        @Deprecated("Deprecated in Java")
        protected override fun onPostExecute(bitmap : Bitmap?) {
            super.onPostExecute(bitmap);
            gpuImage.deleteImage();
            gpuImage.setImage(bitmap!!);
        }

        protected abstract fun decode(options : BitmapFactory.Options) : Bitmap?

        private fun loadResizedImage() : Bitmap? {
            var options : BitmapFactory.Options = BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            decode(options);
            var scale = 1;
            while (checkSize(options.outWidth / scale > outputWidth, options.outHeight / scale > outputHeight)) {
                scale++;
            }

            scale--;
            if (scale < 1) {
                scale = 1;
            }
            options = BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inPurgeable = true;
            options.inTempStorage = ByteArray(32 * 1024)
            var bitmap : Bitmap? = decode(options)
            if (bitmap == null) {
                return null;
            }
            bitmap = rotateImage(bitmap)
            bitmap = scaleBitmap(bitmap)
            return bitmap;
        }

        private fun scaleBitmap(bitmap : Bitmap) : Bitmap {
            var bitmap = bitmap
            // resize to desired dimensions
            val width : Int = bitmap.getWidth();
            val height : Int = bitmap.getHeight();
            val newSize : IntArray = getScaleSize(width, height);
            var workBitmap : Bitmap = Bitmap.createScaledBitmap(bitmap, newSize[0], newSize[1], true);
            if (workBitmap != bitmap) {
                bitmap.recycle();
                bitmap = workBitmap;
                System.gc();
            }

            if (scaleType == ScaleType.CENTER_CROP) {
                // Crop it
                val diffWidth : Int = newSize[0] - outputWidth;
                val diffHeight : Int = newSize[1] - outputHeight;
                workBitmap = Bitmap.createBitmap(bitmap, diffWidth / 2, diffHeight / 2,
                        newSize[0] - diffWidth, newSize[1] - diffHeight);
                if (workBitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = workBitmap;
                }
            }

            return bitmap;
        }

        /**
         * Retrieve the scaling size for the image dependent on the ScaleType.<br>
         * <br>
         * If CROP: sides are same size or bigger than output's sides<br>
         * Else   : sides are same size or smaller than output's sides
         */
        private fun getScaleSize(width : Int, height : Int) : IntArray {
            val newWidth : Float
            val newHeight : Float

            val withRatio : Float = width.toFloat() / outputWidth.toFloat();
            val heightRatio : Float = height.toFloat() / outputHeight.toFloat();

            val adjustWidth : Boolean = if(scaleType == ScaleType.CENTER_CROP)
                    withRatio > heightRatio else withRatio < heightRatio;

            if (adjustWidth) {
                newHeight = outputHeight.toFloat();
                newWidth = (newHeight / height) * width;
            } else {
                newWidth = outputWidth.toFloat();
                newHeight = (newWidth / width) * height;
            }
            scaleWidth = Math.round(newWidth);
            scaleHeight = Math.round(newHeight);
            return intArrayOf(Math.round(newWidth),Math.round(newHeight))
        }

        private fun checkSize(widthBigger : Boolean, heightBigger : Boolean) : Boolean {
            return if (scaleType == ScaleType.CENTER_CROP) {
                widthBigger && heightBigger;
            } else {
                widthBigger || heightBigger;
            }
        }

        private fun rotateImage(bitmap : Bitmap) : Bitmap {
            var rotatedBitmap : Bitmap = bitmap;
            try {
                val orientation : Float = getImageOrientation().toFloat();
                if (orientation != 0f) {
                    val matrix = Matrix();
                    matrix.postRotate(orientation);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                            bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                }
            } catch (e : IOException) {
                e.printStackTrace();
            }
            return rotatedBitmap
        }

        @Throws(IOException::class)
        protected abstract fun getImageOrientation() : Int;
    }

    public interface ResponseListener<T> {
        fun response(item : T)
    }

    public fun getRenderer() : GPUImageRenderer {
        return renderer;
    }
}
