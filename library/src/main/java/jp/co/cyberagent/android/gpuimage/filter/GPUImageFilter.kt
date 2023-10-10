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

package jp.co.cyberagent.android.gpuimage.filter;

import android.opengl.GLES20
import com.danielgergely.kgl.Program
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.utils.OpenGlUtils
import java.nio.FloatBuffer

open class GPUImageFilter {

    companion object {
        const val NO_FILTER_VERTEX_SHADER : String = "" +
        "attribute vec4 position;\n" +
        "attribute vec4 inputTextureCoordinate;\n" +
        " \n" +
        "varying vec2 textureCoordinate;\n" +
        " \n" +
        "void main()\n" +
        "{\n" +
        "    gl_Position = position;\n" +
        "    textureCoordinate = inputTextureCoordinate.xy;\n" +
        "}";
        const val NO_FILTER_FRAGMENT_SHADER : String = "" +
        "varying highp vec2 textureCoordinate;\n" +
        " \n" +
        "uniform sampler2D inputImageTexture;\n" +
        " \n" +
        "void main()\n" +
        "{\n" +
        "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
        "}";

    }

    private val runOnDraw : ArrayList<Runnable> = ArrayList();
    private val vertexShader : String;
    private val fragmentShader : String;
    private var glProgId : Program = 0
    private var glAttribPosition : Int = 0;
    private var glUniformTexture : Int = 0;
    private var glAttribTextureCoordinate : Int = 0;

    var outputWidth : Int = 0
    private set

    var outputHeight : Int = 0
    private set

    var isInitialized : Boolean = false
    private set

    public constructor() : this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER)

    public constructor(vertexShader : String, fragmentShader : String) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
    }

    private final fun init() {
        onInit();
        onInitialized();
    }

    open fun onInit() {
        glProgId = OpenGlUtils.loadProgram(vertexShader, fragmentShader)!!;
        glAttribPosition = Kgl.getAttribLocation(glProgId, "position");
        glUniformTexture = Kgl.getUniformLocation(glProgId, "inputImageTexture")!!;
        glAttribTextureCoordinate = Kgl.getAttribLocation(glProgId, "inputTextureCoordinate");
        isInitialized = true;
    }

    open fun onInitialized() {
    }

    fun ifNeedInit() {
        if (!isInitialized) init();
    }

    fun destroy() {
        isInitialized = false;
        Kgl.deleteProgram(glProgId);
        onDestroy();
    }

    open fun onDestroy() {
    }

    open fun onOutputSizeChanged(width : Int, height : Int) {
        outputWidth = width;
        outputHeight = height;
    }

    private fun glVertexAttribPointer(
        location: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        ptr: com.danielgergely.kgl.FloatBuffer,
        bufferSize : Int
    ) {
        val cubeBufferId = Kgl.createBuffer()
        Kgl.enableVertexAttribArray(location)
        Kgl.bindBuffer(GLES20.GL_ARRAY_BUFFER, cubeBufferId)
        Kgl.bufferData(GLES20.GL_ARRAY_BUFFER, ptr, bufferSize * 4, GLES20.GL_STATIC_DRAW)
        Kgl.vertexAttribPointer(location, size, type, normalized, stride, 0)
    }

    open fun onDraw(textureId : Int , cubeBuffer : FloatBuffer, textureBuffer : FloatBuffer) {
        Kgl.useProgram(glProgId);
        runPendingOnDrawTasks();
        if (!isInitialized) {
            return;
        }

        cubeBuffer.position(0);
        glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, com.danielgergely.kgl.FloatBuffer(cubeBuffer), GPUImageRenderer.CUBE.size);

        textureBuffer.position(0);
        glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, com.danielgergely.kgl.FloatBuffer(textureBuffer), TextureRotationUtil.TEXTURE_NO_ROTATION.size);
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            Kgl.activeTexture(GLES20.GL_TEXTURE0);
            Kgl.bindTexture(GLES20.GL_TEXTURE_2D, textureId);
            Kgl.uniform1i(glUniformTexture, 0);
        }
        onDrawArraysPre();
        Kgl.drawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        Kgl.disableVertexAttribArray(glAttribPosition);
        Kgl.disableVertexAttribArray(glAttribTextureCoordinate);
        Kgl.bindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    protected open fun onDrawArraysPre() {
    }

    protected fun runPendingOnDrawTasks() {
        synchronized (runOnDraw) {
            while (!runOnDraw.isEmpty()) {
                runOnDraw.removeFirst().run();
            }
        }
    }

    internal val program : Int get() = glProgId

    public fun getAttribPosition() : Int {
        return glAttribPosition;
    }

    public fun getAttribTextureCoordinate() : Int {
        return glAttribTextureCoordinate;
    }

    public fun getUniformTexture() : Int {
        return glUniformTexture;
    }

    protected fun setInteger(location : Int, intValue : Int) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniform1i(location, intValue);
        }
    }

    internal fun setFloat(location : Int, floatValue : Float) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniform1f(location, floatValue);
        }
    }

    protected fun setFloatVec2(location : Int, arrayValue : FloatArray) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniform2fv(location, arrayValue);
        }
    }

    protected fun setFloatVec3(location : Int, arrayValue : FloatArray) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniform3fv(location, arrayValue);
        }
    }

    protected fun setFloatVec4(location : Int, arrayValue : FloatArray) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniform4fv(location, arrayValue)
        }
    }

    protected fun setFloatArray(location : Int, arrayValue : FloatArray) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniform1fv(location, arrayValue);
        }
    }

    protected fun setPoint(location : Int, x : Float, y : Float) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniform2fv(location, floatArrayOf(x, y))
        }
    }

    protected fun setUniformMatrix3f(location : Int, matrix : FloatArray) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniformMatrix3fv(location, false, matrix)
        }
    }

    protected fun setUniformMatrix4f(location : Int, matrix : FloatArray) {
        runOnDraw {
            ifNeedInit();
            Kgl.uniformMatrix4fv(location, false, matrix)
        }
    }

    protected fun runOnDraw(runnable : Runnable) {
        synchronized (runOnDraw) {
            runOnDraw.add(runnable);
        }
    }

}
