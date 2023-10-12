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

class GPUImageFalseColorFilter(
    private var firstColor: FloatArray,
    private var secondColor: FloatArray
) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, FALSECOLOR_FRAGMENT_SHADER) {

    companion object {
        const val FALSECOLOR_FRAGMENT_SHADER: String = "" +
                "precision lowp float;\n" +
                "\n" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform float intensity;\n" +
                "uniform vec3 firstColor;\n" +
                "uniform vec3 secondColor;\n" +
                "\n" +
                "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
                "\n" +
                "gl_FragColor = vec4( mix(firstColor.rgb, secondColor.rgb, luminance), textureColor.a);\n" +
                "}\n"
    }


    private var _firstColorLocation: UniformLocation? = null
    private var _secondColorLocation: UniformLocation? = null

    private inline var firstColorLocation: UniformLocation
        get() = _firstColorLocation!!
        set(value){
            _firstColorLocation =  value
        }
    private inline var secondColorLocation: UniformLocation
        get() = _secondColorLocation!!
        set(value){
            _secondColorLocation = value
        }

    constructor() : this(0f, 0f, 0.5f, 1f, 0f, 0f)

    constructor(
        firstRed: Float,
        firstGreen: Float,
        firstBlue: Float,
        secondRed: Float,
        secondGreen: Float,
        secondBlue: Float
    ) : this(floatArrayOf(firstRed, firstGreen, firstBlue), floatArrayOf(secondRed, secondGreen, secondBlue))

    override fun onInit() {
        super.onInit()
        _firstColorLocation = Kgl.getUniformLocation(program, "firstColor")
        _secondColorLocation = Kgl.getUniformLocation(program, "secondColor")
    }

    override fun onInitialized() {
        super.onInitialized()
        setFirstColor(firstColor)
        setSecondColor(secondColor)
    }

    fun setFirstColor(firstColor: FloatArray) {
        this.firstColor = firstColor
        if(_firstColorLocation != null) setFloatVec3(firstColorLocation, firstColor)
    }

    fun setSecondColor(secondColor: FloatArray) {
        this.secondColor = secondColor
        if(_secondColorLocation != null) setFloatVec3(secondColorLocation, secondColor)
    }
}
