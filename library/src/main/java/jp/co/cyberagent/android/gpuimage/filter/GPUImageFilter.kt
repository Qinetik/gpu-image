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

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.PointF;
import android.opengl.GLES20;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;

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

        public fun loadShader(file : String, context : Context) : String {
            try {
                val assetManager : AssetManager = context.getAssets();
                val ims : InputStream = assetManager.open(file);
                val re : String = convertStreamToString(ims);
                ims.close();
                return re;
            } catch (e : Exception) {
                e.printStackTrace();
            }

            return "";
        }

        public fun convertStreamToString(stream : InputStream) : String {
            val s = java.util.Scanner(stream).useDelimiter("\\A");
            return if(s.hasNext()) s.next() else "";
        }

    }

    private val runOnDraw : LinkedList<Runnable> = LinkedList();
    private val vertexShader : String;
    private val fragmentShader : String;
    private var glProgId : Int = 0;
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
        glProgId = OpenGlUtils.loadProgram(vertexShader, fragmentShader);
        glAttribPosition = GLES20.glGetAttribLocation(glProgId, "position");
        glUniformTexture = GLES20.glGetUniformLocation(glProgId, "inputImageTexture");
        glAttribTextureCoordinate = GLES20.glGetAttribLocation(glProgId, "inputTextureCoordinate");
        isInitialized = true;
    }

    open fun onInitialized() {
    }

    fun ifNeedInit() {
        if (!isInitialized) init();
    }

    fun destroy() {
        isInitialized = false;
        GLES20.glDeleteProgram(glProgId);
        onDestroy();
    }

    open fun onDestroy() {
    }

    open fun onOutputSizeChanged(width : Int, height : Int) {
        outputWidth = width;
        outputHeight = height;
    }

    open fun onDraw(textureId : Int , cubeBuffer : FloatBuffer, textureBuffer : FloatBuffer) {
        GLES20.glUseProgram(glProgId);
        runPendingOnDrawTasks();
        if (!isInitialized) {
            return;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(glUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
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
            GLES20.glUniform1i(location, intValue);
        }
    }

    internal fun setFloat(location : Int, floatValue : Float) {
        runOnDraw {
            ifNeedInit();
            GLES20.glUniform1f(location, floatValue);
        }
    }

    protected fun setFloatVec2(location : Int, arrayValue : FloatArray) {
        runOnDraw {
            ifNeedInit();
            GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
        }
    }

    protected fun setFloatVec3(location : Int, arrayValue : FloatArray) {
        runOnDraw {
            ifNeedInit();
            GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
        }
    }

    protected fun setFloatVec4(location : Int, arrayValue : FloatArray) {
        runOnDraw {
            ifNeedInit();
            GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
        }
    }

    protected fun setFloatArray(location : Int, arrayValue : FloatArray) {
        runOnDraw {
            ifNeedInit();
            GLES20.glUniform1fv(location, arrayValue.size, FloatBuffer.wrap(arrayValue));
        }
    }

    protected fun setPoint(location : Int, point : PointF) {
        runOnDraw {
                ifNeedInit();
                val vec2 = FloatArray(2);
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
        }
    }

    protected fun setUniformMatrix3f(location : Int, matrix : FloatArray) {
        runOnDraw {
            ifNeedInit();
            GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
        }
    }

    protected fun setUniformMatrix4f(location : Int, matrix : FloatArray) {
        runOnDraw {
            ifNeedInit();
            GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
        }
    }

    protected fun runOnDraw(runnable : Runnable) {
        synchronized (runOnDraw) {
            runOnDraw.addLast(runnable);
        }
    }

}
