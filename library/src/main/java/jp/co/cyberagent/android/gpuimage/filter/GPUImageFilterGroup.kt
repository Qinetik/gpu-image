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

package jp.co.cyberagent.android.gpuimage.filter

import android.annotation.SuppressLint
import android.opengl.GLES20
import com.danielgergely.kgl.*
import org.qinetik.gpuimage.Kgl

import org.qinetik.gpuimage.filter.GPUImageFilter
import org.qinetik.gpuimage.utils.OpenGlUtils.CUBE
import org.qinetik.gpuimage.utils.TextureRotationUtil

/**
 * Resembles a filter that consists of multiple filters applied after each
 * other.
 */
open class GPUImageFilterGroup : GPUImageFilter {

    protected val filters : MutableList<GPUImageFilter>
    protected var mergedFilters : MutableList<GPUImageFilter>? = null

    private var frameBuffers : Array<Framebuffer>? = null
    private var frameBufferTextures : Array<Texture>? = null

    private val glCubeBuffer : FloatBuffer
    private val glTextureBuffer : FloatBuffer
    private val glTextureFlipBuffer : FloatBuffer

    /**
     * Instantiates a new GPUImageFilterGroup with no filters.
     */
    constructor() : this(mutableListOf())

    /**
     * Instantiates a new GPUImageFilterGroup with the given filters.
     *
     * @param filters the filters which represent this filter
     */
    constructor(filters : MutableList<GPUImageFilter>) {
        this.filters = filters
        if(filters.isNotEmpty()){
            updateMergedFilters()
        }

        val glCubeBuffer = FloatBuffer(CUBE.size * 4)
        glCubeBuffer.put(CUBE)
        glCubeBuffer.position = 0
        this.glCubeBuffer = glCubeBuffer

        val glTextureBuffer = FloatBuffer(TextureRotationUtil.TEXTURE_NO_ROTATION.size * 4)
        glTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION)
        glTextureBuffer.position = 0
        this.glTextureBuffer = glTextureBuffer

        val flipTexture : FloatArray = TextureRotationUtil.getRotation(
            org.qinetik.gpuimage.utils.Rotation.NORMAL,
            false,
            true
        )
        val glTextureFlipBuffer = FloatBuffer(flipTexture.size * 4)
        glTextureFlipBuffer.put(flipTexture)
        glTextureFlipBuffer.position = (0)
        this.glTextureFlipBuffer = glTextureFlipBuffer
    }

    fun addFilter(aFilter: GPUImageFilter) {
        filters.add(aFilter)
        updateMergedFilters()
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter#onInit()
     */
    override fun onInit() {
        super.onInit()
        for (filter in filters) {
            filter.ifNeedInit()
        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter#onDestroy()
     */
    override fun onDestroy() {
        destroyFramebuffers()
        for (filter in filters) {
            filter.destroy()
        }
        super.onDestroy()
    }

    private fun destroyFramebuffers() {
        if (frameBufferTextures != null) {
            for(texture in frameBufferTextures!!){
                Kgl.deleteTexture(texture)
            }
            frameBufferTextures = null
        }
        if (frameBuffers != null) {
            for(buffer in frameBuffers!!){
                Kgl.deleteFramebuffer(buffer)
            }
            frameBuffers = null
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter#onOutputSizeChanged(int,
     * int)
     */
    override fun onOutputSizeChanged(width : Int, height : Int) {
        super.onOutputSizeChanged(width, height)
        if (frameBuffers != null) {
            destroyFramebuffers()
        }

        var size = filters.size
        for(i in 0 until size){
            filters.get(i).onOutputSizeChanged(width, height)
        }

        if (mergedFilters != null && mergedFilters!!.size > 0) {
            size = mergedFilters!!.size

            frameBuffers = Array(size - 1){
                Kgl.createFramebuffer()
            }
            frameBufferTextures = Kgl.createTextures(size - 1)

            for(i in 0 until size - 1){
                Kgl.bindTexture(GL_TEXTURE_2D, frameBufferTextures!![i])
                GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, null)
                Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
                Kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBuffers!![i])
                Kgl.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                        GL_TEXTURE_2D, frameBufferTextures!![i], 0)

                Kgl.bindTexture(GL_TEXTURE_2D, null)
                Kgl.bindFramebuffer(GL_FRAMEBUFFER, null)
            }

        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter#onDraw(int,
     * java.nio.FloatBuffer, java.nio.FloatBuffer)
     */
    @SuppressLint("WrongCall")
    override fun onDraw(textureId : Int?, cubeBuffer : com.danielgergely.kgl.FloatBuffer, textureBuffer : com.danielgergely.kgl.FloatBuffer) {
        runPendingOnDrawTasks()
        if (!isInitialized || frameBuffers == null || frameBufferTextures == null) {
            return
        }
        if (mergedFilters != null) {
            val size : Int = mergedFilters!!.size
            var previousTexture : Int = textureId ?: 0
            for(i in 0 until size){
                val filter = mergedFilters!!.get(i)
                val isNotLast = i < size - 1
                if (isNotLast) {
                    Kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBuffers!![i])
                    Kgl.clearColor(0f, 0f, 0f, 0f)
                }

                if (i == 0) {
                    filter.onDraw(previousTexture, cubeBuffer, textureBuffer)
                } else if (i == size - 1) {
                    filter.onDraw(
                        previousTexture,
                        glCubeBuffer,
                        if(size % 2 == 0) glTextureFlipBuffer else glTextureBuffer
                    )
                } else {
                    filter.onDraw(
                        previousTexture,
                        glCubeBuffer,
                        glTextureBuffer
                    )
                }

                if (isNotLast) {
                    Kgl.bindFramebuffer(GL_FRAMEBUFFER, 0)
                    previousTexture = frameBufferTextures!![i]
                }
            }
        }
    }

    fun updateMergedFilters() {
        if (filters == null) {
            return
        }

        if (mergedFilters == null) {
            mergedFilters = ArrayList()
        } else {
            mergedFilters!!.clear()
        }

        var filters : MutableList<GPUImageFilter>?
        for (filter in this.filters) {
            if (filter is GPUImageFilterGroup) {
                filter.updateMergedFilters()
                filters = filter.mergedFilters
                if (filters == null || filters.isEmpty())
                    continue
                mergedFilters!!.addAll(filters)
                continue
            }
            mergedFilters!!.add(filter)
        }
    }
}
