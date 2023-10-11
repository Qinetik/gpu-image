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

import android.graphics.Bitmap
import com.danielgergely.kgl.*

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter
import org.qinetik.gpuimage.utils.Rotation
import org.qinetik.gpuimage.utils.TextureRotationUtil

open class GPUImageTwoInputFilter : GPUImageFilter {

    companion object {
        const val VERTEX_SHADER: String = "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                "attribute vec4 inputTextureCoordinate2;\n" +
                " \n" +
                "varying vec2 textureCoordinate;\n" +
                "varying vec2 textureCoordinate2;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
                "}"
    }

    private var filterSecondTextureCoordinateAttribute: UniformLocation? = null
    private var filterInputTextureUniform2: UniformLocation? = null
    private var filterSourceTexture2: Int = OpenGlUtils.NO_TEXTURE
    private lateinit var texture2CoordinatesBuffer: ByteBuffer
    private var bitmap: Bitmap? = null

    constructor(fragmentShader: String) : this(VERTEX_SHADER, fragmentShader)

    constructor(vertexShader: String, fragmentShader: String) : super(vertexShader, fragmentShader) {
        setRotation(Rotation.NORMAL, false, false)
    }

    override fun onInit() {
        super.onInit()

        filterSecondTextureCoordinateAttribute = Kgl.getAttribLocation(program, "inputTextureCoordinate2")
        filterInputTextureUniform2 = Kgl.getUniformLocation(
            program,
            "inputImageTexture2"
        ) // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
        Kgl.enableVertexAttribArray(filterSecondTextureCoordinateAttribute!!)
    }

    override fun onInitialized() {
        super.onInitialized()
        if (bitmap != null && !bitmap!!.isRecycled) {
            setBitmap(bitmap!!)
        }
    }

    fun setBitmap(bitmap: Bitmap) {
        if (bitmap.isRecycled) {
            return
        }
        this.bitmap = bitmap
        if (this.bitmap == null) {
            return
        }
        runOnDraw {
            if (filterSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                if (bitmap.isRecycled) {
                    return@runOnDraw
                }
                Kgl.activeTexture(GL_TEXTURE3)
                filterSourceTexture2 = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false)
            }
        }
    }

    fun getBitmap(): Bitmap? {
        return bitmap
    }

    fun recycleBitmap() {
        if (bitmap != null && !bitmap!!.isRecycled) {
            bitmap!!.recycle()
            bitmap = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Kgl.deleteTexture(filterSourceTexture2)
        filterSourceTexture2 = OpenGlUtils.NO_TEXTURE
    }

    private fun glVertexAttribPointer(
        location: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        ptr: Buffer,
        bufferSize: Int
    ) {
        val cubeBufferId = Kgl.createBuffer()
        Kgl.enableVertexAttribArray(location)
        Kgl.bindBuffer(GL_ARRAY_BUFFER, cubeBufferId)
        Kgl.bufferData(GL_ARRAY_BUFFER, ptr, bufferSize * 4, GL_STATIC_DRAW)
        Kgl.vertexAttribPointer(location, size, type, normalized, stride, 0)
    }

    override fun onDrawArraysPre() {
        Kgl.enableVertexAttribArray(filterSecondTextureCoordinateAttribute!!)
        Kgl.activeTexture(GL_TEXTURE3)
        Kgl.bindTexture(GL_TEXTURE_2D, filterSourceTexture2)
        Kgl.uniform1i(filterInputTextureUniform2!!, 3)

        texture2CoordinatesBuffer.position(0)
        glVertexAttribPointer(
            filterSecondTextureCoordinateAttribute!!,
            2,
            GL_FLOAT,
            false,
            0,
            ByteBuffer(texture2CoordinatesBuffer),
            8
        )
    }

    fun setRotation(rotation: Rotation, flipHorizontal: Boolean, flipVertical: Boolean) {
        val buffer: FloatArray = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical)

        val bBuffer: ByteBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder())
        val fBuffer: FloatBuffer = bBuffer.asFloatBuffer()
        fBuffer.put(buffer)
        fBuffer.flip()

        texture2CoordinatesBuffer = bBuffer

    }
}
