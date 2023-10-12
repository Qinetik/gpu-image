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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import com.danielgergely.kgl.BitmapTextureAsset
import com.danielgergely.kgl.Texture

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.utils.OpenGlUtils.CUBE
import org.qinetik.gpuimage.utils.TextureRotationUtil
import org.qinetik.gpuimage.utils.TextureRotationUtil.TEXTURE_NO_ROTATION

class GPUImageRenderer : GLSurfaceView.Renderer, GLTextureView.Renderer, PreviewCallback {

    companion object {
        private const val NO_IMAGE: Int = -1;
    }


    private lateinit var filter: org.qinetik.gpuimage.filter.GPUImageFilter

    public val surfaceChangedWaiter: Object = Object();

    private var glTextureId: Texture? = null
    private var surfaceTexture: SurfaceTexture? = null;
    private val glCubeBuffer: FloatBuffer
    private val glTextureBuffer: FloatBuffer
    private lateinit var glRgbBuffer: IntBuffer

    private var outputWidth: Int = 0
    private var outputHeight: Int = 0
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var addedPadding: Int = 0

    private val runOnDraw: Queue<Runnable>
    private val runOnDrawEnd: Queue<Runnable>
    private lateinit var rotation: org.qinetik.gpuimage.utils.Rotation;
    private var flipHorizontal: Boolean = false
    private var flipVertical: Boolean = false
    private var scaleType: GPUImage.ScaleType = GPUImage.ScaleType.CENTER_CROP;

    private var backgroundRed: Float = 0f;
    private var backgroundGreen: Float = 0f;
    private var backgroundBlue: Float = 0f;

    public constructor(filter: org.qinetik.gpuimage.filter.GPUImageFilter) {
        this.filter = filter
        runOnDraw = LinkedList()
        runOnDrawEnd = LinkedList()

        glCubeBuffer = ByteBuffer.allocateDirect(CUBE.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        glCubeBuffer.put(CUBE).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        setRotation(org.qinetik.gpuimage.utils.Rotation.NORMAL, false, false);
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(backgroundRed, backgroundGreen, backgroundBlue, 1f);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        filter.ifNeedInit();
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        outputWidth = width;
        outputHeight = height;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glUseProgram(filter.program);
        filter.onOutputSizeChanged(width, height);
        adjustImageScaling();
        synchronized(surfaceChangedWaiter) {
            surfaceChangedWaiter.notifyAll();
        }
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(runOnDraw);
        filter.onDraw(
            glTextureId,
            com.danielgergely.kgl.FloatBuffer(glCubeBuffer),
            com.danielgergely.kgl.FloatBuffer(glTextureBuffer)
        )
        runAll(runOnDrawEnd);
        surfaceTexture?.updateTexImage()
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public fun setBackgroundColor(red: Float, green: Float, blue: Float) {
        backgroundRed = red;
        backgroundGreen = green;
        backgroundBlue = blue;
    }

    private fun runAll(queue: Queue<Runnable>) {
        synchronized(queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        val previewSize: Size = camera.getParameters().getPreviewSize();
        onPreviewFrame(data, previewSize.width, previewSize.height);
    }

    public fun onPreviewFrame(data: ByteArray, width: Int, height: Int) {
        if (glRgbBuffer == null) {
            glRgbBuffer = IntBuffer.allocate(width * height);
        }
        if (runOnDraw.isEmpty()) {
            runOnDraw {
                GPUImageNativeLibrary.YUVtoRBGA(data, width, height, glRgbBuffer.array());
                glTextureId = OpenGlUtils.loadTexture(glRgbBuffer, width, height, glTextureId);

                if (imageWidth != width) {
                    imageWidth = width;
                    imageHeight = height;
                    adjustImageScaling();
                }
            }
        }
    }

    public fun setUpSurfaceTexture(camera: Camera) {
        runOnDraw {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0);
            surfaceTexture = SurfaceTexture(textures[0]);
            try {
                camera.setPreviewTexture(surfaceTexture);
                camera.setPreviewCallback(this@GPUImageRenderer);
                camera.startPreview();
            } catch (e: IOException) {
                e.printStackTrace();
            }
        }
    }

    public fun setFilter(filter: org.qinetik.gpuimage.filter.GPUImageFilter) {
        runOnDraw {
            val oldFilter: org.qinetik.gpuimage.filter.GPUImageFilter = this@GPUImageRenderer.filter;
            this@GPUImageRenderer.filter = filter
            if (oldFilter != null) {
                oldFilter.destroy();
            }
            this@GPUImageRenderer.filter.ifNeedInit();
            GLES20.glUseProgram(this@GPUImageRenderer.filter.program);
            this@GPUImageRenderer.filter.onOutputSizeChanged(outputWidth, outputHeight);
        }
    }

    public fun deleteImage() {
        runOnDraw {
            glTextureId?.let {
                Kgl.deleteTexture(it)
                glTextureId = null;
            }
        }
    }

    public fun setImageBitmap(bitmap: Bitmap) {
        setImageBitmap(bitmap, true);
    }

    public fun setImageBitmap(bitmap: Bitmap, recycle: Boolean) {
        runOnDraw {
            var resizedBitmap: Bitmap? = null
            if (bitmap.getWidth() % 2 == 1) {
                resizedBitmap = Bitmap.createBitmap(
                    bitmap.getWidth() + 1, bitmap.getHeight(),
                    Bitmap.Config.ARGB_8888
                )
                resizedBitmap.setDensity(bitmap.getDensity())
                val can = Canvas(resizedBitmap)
                can.drawARGB(0x00, 0x00, 0x00, 0x00)
                can.drawBitmap(bitmap, 0f, 0f, null)
                addedPadding = 1;
            } else {
                addedPadding = 0;
            }

            glTextureId = org.qinetik.gpuimage.utils.OpenGlUtils.loadTexture(BitmapTextureAsset(resizedBitmap ?: bitmap), glTextureId);
            if(recycle){
                (resizedBitmap ?: bitmap).recycle()
            } else {
                resizedBitmap?.recycle()
            }
            imageWidth = bitmap.getWidth();
            imageHeight = bitmap.getHeight();
            adjustImageScaling();
        }
    }

    public fun setScaleType(scaleType: GPUImage.ScaleType) {
        this.scaleType = scaleType;
    }

    fun getFrameWidth(): Int {
        return outputWidth;
    }

    fun getFrameHeight(): Int {
        return outputHeight;
    }

    private fun adjustImageScaling() {
        var outputWidth: Float = this.outputWidth.toFloat();
        var outputHeight: Float = this.outputHeight.toFloat();
        if (rotation == org.qinetik.gpuimage.utils.Rotation.ROTATION_270 || rotation == org.qinetik.gpuimage.utils.Rotation.ROTATION_90) {
            outputWidth = this.outputHeight.toFloat();
            outputHeight = this.outputWidth.toFloat();
        }

        val ratio1: Float = outputWidth / imageWidth;
        val ratio2: Float = outputHeight / imageHeight;
        val ratioMax: Float = Math.max(ratio1, ratio2);
        val imageWidthNew: Int = Math.round(imageWidth * ratioMax);
        val imageHeightNew: Int = Math.round(imageHeight * ratioMax);

        val ratioWidth: Float = imageWidthNew / outputWidth;
        val ratioHeight: Float = imageHeightNew / outputHeight;

        var cube: FloatArray = CUBE;
        var textureCords: FloatArray = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);
        if (scaleType == GPUImage.ScaleType.CENTER_CROP) {
            val distHorizontal: Float = (1 - 1 / ratioWidth) / 2;
            val distVertical: Float = (1 - 1 / ratioHeight) / 2;
            textureCords = floatArrayOf(
                addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
            )
        } else {
            cube = floatArrayOf(
                CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            )
        }

        glCubeBuffer.clear();
        glCubeBuffer.put(cube).position(0);
        glTextureBuffer.clear();
        glTextureBuffer.put(textureCords).position(0);
    }

    private fun addDistance(coordinate: Float, distance: Float): Float {
        return if (coordinate == 0.0f) distance else 1 - distance;
    }

    public fun setRotationCamera(
        rotation: org.qinetik.gpuimage.utils.Rotation,
        flipHorizontal: Boolean,
        flipVertical: Boolean
    ) {
        setRotation(rotation, flipVertical, flipHorizontal);
    }

    public fun setRotation(rotation: org.qinetik.gpuimage.utils.Rotation) {
        this.rotation = rotation;
        adjustImageScaling();
    }

    public fun setRotation(
        rotation: org.qinetik.gpuimage.utils.Rotation,
        flipHorizontal: Boolean, flipVertical: Boolean
    ) {
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        setRotation(rotation);
    }

    public fun getRotation(): org.qinetik.gpuimage.utils.Rotation {
        return rotation;
    }

    public fun isFlippedHorizontally(): Boolean {
        return flipHorizontal;
    }

    public fun isFlippedVertically(): Boolean {
        return flipVertical;
    }

    fun runOnDraw(runnable: Runnable) {
        synchronized(runOnDraw) {
            runOnDraw.add(runnable);
        }
    }

    fun runOnDrawEnd(runnable: Runnable) {
        synchronized(runOnDrawEnd) {
            runOnDrawEnd.add(runnable);
        }
    }
}
