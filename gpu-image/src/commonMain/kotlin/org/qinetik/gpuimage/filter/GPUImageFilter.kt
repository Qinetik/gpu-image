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

package org.qinetik.gpuimage.filter

import com.danielgergely.kgl.*
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.utils.OpenGlUtils
import org.qinetik.gpuimage.utils.OpenGlUtils.CUBE
import org.qinetik.gpuimage.utils.TextureRotationUtil

open class GPUImageFilter {

    companion object {
        const val NO_FILTER_VERTEX_SHADER: String = "" +
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                " \n" +
                "varying vec2 textureCoordinate;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                "}"
        const val NO_FILTER_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                " \n" +
                "uniform sampler2D inputImageTexture;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "}"

    }

    private val runOnDraw: ArrayList<() -> Unit> = ArrayList()
    private val vertexShader: String
    private val fragmentShader: String
    private var glAttribPosition: Int = 0
    private var glAttribTextureCoordinate: Int = 0

    private var _glProgId : Program? = null
    private var _glUniformTexture : UniformLocation? = null

    private inline var glProgId: Program
        get() = _glProgId!!
        set(value){
            _glProgId = value
        }
    private inline var glUniformTexture: UniformLocation
        get() = _glUniformTexture!!
        set(value){
            _glUniformTexture = value
        }

    var outputWidth: Int = 0
        private set

    var outputHeight: Int = 0
        private set

    var isInitialized: Boolean = false
        private set

    constructor() : this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER)

    constructor(vertexShader: String, fragmentShader: String) {
        this.vertexShader = vertexShader
        this.fragmentShader = fragmentShader
    }

    private fun init() {
        onInit()
        onInitialized()
    }

    open fun onInit() {
        glProgId = OpenGlUtils.loadProgram(vertexShader, fragmentShader)!!
        glAttribPosition = Kgl.getAttribLocation(glProgId, "position")
        glUniformTexture = Kgl.getUniformLocation(glProgId, "inputImageTexture")!!
        glAttribTextureCoordinate = Kgl.getAttribLocation(glProgId, "inputTextureCoordinate")
        isInitialized = true
    }

    open fun onInitialized() {
    }

    fun ifNeedInit() {
        if (!isInitialized) init()
    }

    fun destroy() {
        isInitialized = false
        Kgl.deleteProgram(glProgId)
        onDestroy()
    }

    open fun onDestroy() {
    }

    open fun onOutputSizeChanged(width: Int, height: Int) {
        outputWidth = width
        outputHeight = height
    }

    private fun glVertexAttribPointer(
        location: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        ptr: FloatBuffer,
        bufferSize: Int
    ) {
        val cubeBufferId = Kgl.createBuffer()
        Kgl.enableVertexAttribArray(location)
        Kgl.bindBuffer(GL_ARRAY_BUFFER, cubeBufferId)
        Kgl.bufferData(GL_ARRAY_BUFFER, ptr, bufferSize * 4, GL_STATIC_DRAW)
        Kgl.vertexAttribPointer(location, size, type, normalized, stride, 0)
    }

    open fun onDraw(textureId: Texture?, cubeBuffer: FloatBuffer, textureBuffer: FloatBuffer) {
        Kgl.useProgram(glProgId)
        runPendingOnDrawTasks()
        if (!isInitialized) {
            return
        }

        cubeBuffer.position = 0
        glVertexAttribPointer(glAttribPosition, 2, GL_FLOAT, false, 0, cubeBuffer, CUBE.size)

        textureBuffer.position = 0
        glVertexAttribPointer(
            glAttribTextureCoordinate,
            2,
            GL_FLOAT,
            false,
            0,
            textureBuffer,
            TextureRotationUtil.TEXTURE_NO_ROTATION.size
        )
        if (textureId != null) {
            Kgl.activeTexture(GL_TEXTURE0)
            Kgl.bindTexture(GL_TEXTURE_2D, textureId)
            Kgl.uniform1i(glUniformTexture, 0)
        }
        onDrawArraysPre()
        Kgl.drawArrays(GL_TRIANGLE_STRIP, 0, 4)
        Kgl.disableVertexAttribArray(glAttribPosition)
        Kgl.disableVertexAttribArray(glAttribTextureCoordinate)
        Kgl.bindTexture(GL_TEXTURE_2D, null)
    }

    protected open fun onDrawArraysPre() {
    }

    protected fun runPendingOnDrawTasks() {
        while (!runOnDraw.isEmpty()) {
            runOnDraw.removeFirst().invoke()
        }
    }

    val program: Program get() = glProgId

    fun getAttribPosition(): Int {
        return glAttribPosition
    }

    fun getAttribTextureCoordinate(): Int {
        return glAttribTextureCoordinate
    }

    fun getUniformTexture(): UniformLocation {
        return glUniformTexture
    }

    protected fun setInteger(location: UniformLocation, intValue: Int) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniform1i(location, intValue)
        }
    }

    fun setFloat(location: UniformLocation, floatValue: Float) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniform1f(location, floatValue)
        }
    }

    protected fun setFloatVec2(location: UniformLocation, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniform2fv(location, arrayValue)
        }
    }

    protected fun setFloatVec3(location: UniformLocation, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniform3fv(location, arrayValue)
        }
    }

    protected fun setFloatVec4(location: UniformLocation, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniform4fv(location, arrayValue)
        }
    }

    protected fun setFloatArray(location: UniformLocation, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniform1fv(location, arrayValue)
        }
    }

    protected fun setPoint(location: UniformLocation, x: Float, y: Float) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniform2fv(location, floatArrayOf(x, y))
        }
    }

    protected fun setUniformMatrix3f(location: UniformLocation, matrix: FloatArray) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniformMatrix3fv(location, false, matrix)
        }
    }

    protected fun setUniformMatrix4f(location: UniformLocation, matrix: FloatArray) {
        runOnDraw {
            ifNeedInit()
            Kgl.uniformMatrix4fv(location, false, matrix)
        }
    }

    protected fun runOnDraw(runnable: () -> Unit) {
        runOnDraw.add(runnable)
    }

}
