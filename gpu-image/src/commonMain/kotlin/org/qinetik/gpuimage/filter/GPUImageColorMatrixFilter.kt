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

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

/**
 * Applies a ColorMatrix to the image.
 */
open class GPUImageColorMatrixFilter : GPUImageFilter {

    companion object {
        const val COLOR_MATRIX_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "\n" +
                "uniform lowp mat4 colorMatrix;\n" +
                "uniform lowp float intensity;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "    lowp vec4 outputColor = textureColor * colorMatrix;\n" +
                "    \n" +
                "    gl_FragColor = (intensity * outputColor) + ((1.0 - intensity) * textureColor);\n" +
                "}"

    }

    private var intensity: Float
    private var colorMatrix: FloatArray
    private var _colorMatrixLocation: UniformLocation? = null
    private var _intensityLocation: UniformLocation? = null

    private inline var colorMatrixLocation: UniformLocation
        get() = _colorMatrixLocation!!
        set(value){
            _colorMatrixLocation = value
        }
    private inline var intensityLocation: UniformLocation
        get() = _intensityLocation!!
        set(value){
            _intensityLocation = value
        }

    constructor() : this(
        1.0f, floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    )

    constructor(intensity: Float, colorMatrix: FloatArray) : super(
        NO_FILTER_VERTEX_SHADER,
        COLOR_MATRIX_FRAGMENT_SHADER
    ) {
        this.intensity = intensity
        this.colorMatrix = colorMatrix
    }

    override fun onInit() {
        super.onInit()
        _colorMatrixLocation = Kgl.getUniformLocation(program, "colorMatrix")
        _intensityLocation = Kgl.getUniformLocation(program, "intensity")
    }

    override fun onInitialized() {
        super.onInitialized()
        setIntensity(intensity)
        setColorMatrix(colorMatrix)
    }

    fun setIntensity(intensity: Float) {
        this.intensity = intensity
        if(_intensityLocation != null) setFloat(intensityLocation, intensity)
    }

    fun setColorMatrix(colorMatrix: FloatArray) {
        this.colorMatrix = colorMatrix
        if(_colorMatrixLocation != null) setUniformMatrix4f(colorMatrixLocation, colorMatrix)
    }
}
