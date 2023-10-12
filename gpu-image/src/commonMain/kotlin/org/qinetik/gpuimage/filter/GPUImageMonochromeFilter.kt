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
 * Converts the image to a single-color version, based on the luminance of each pixel
 * intensity: The degree to which the specific color replaces the normal image color (0.0 - 1.0, with 1.0 as the default)
 * color: The color to use as the basis for the effect, with (0.6, 0.45, 0.3, 1.0) as the default.
 */
class GPUImageMonochromeFilter(
    private var intensity: Float,
    private var color: FloatArray
) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, MONOCHROME_FRAGMENT_SHADER) {
    companion object {
        const val MONOCHROME_FRAGMENT_SHADER: String = "" +
                " precision lowp float;\n" +
                "  \n" +
                "  varying highp vec2 textureCoordinate;\n" +
                "  \n" +
                "  uniform sampler2D inputImageTexture;\n" +
                "  uniform float intensity;\n" +
                "  uniform vec3 filterColor;\n" +
                "  \n" +
                "  const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
                "  \n" +
                "  void main()\n" +
                "  {\n" +
                " 	//desat, then apply overlay blend\n" +
                " 	lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                " 	float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
                " 	\n" +
                " 	lowp vec4 desat = vec4(vec3(luminance), 1.0);\n" +
                " 	\n" +
                " 	//overlay\n" +
                " 	lowp vec4 outputColor = vec4(\n" +
                "                                  (desat.r < 0.5 ? (2.0 * desat.r * filterColor.r) : (1.0 - 2.0 * (1.0 - desat.r) * (1.0 - filterColor.r))),\n" +
                "                                  (desat.g < 0.5 ? (2.0 * desat.g * filterColor.g) : (1.0 - 2.0 * (1.0 - desat.g) * (1.0 - filterColor.g))),\n" +
                "                                  (desat.b < 0.5 ? (2.0 * desat.b * filterColor.b) : (1.0 - 2.0 * (1.0 - desat.b) * (1.0 - filterColor.b))),\n" +
                "                                  1.0\n" +
                "                                  );\n" +
                " 	\n" +
                " 	//which is better, or are they equal?\n" +
                " 	gl_FragColor = vec4( mix(textureColor.rgb, outputColor.rgb, intensity), textureColor.a);\n" +
                "  }"
    }

    private var _intensityLocation: UniformLocation? = null
    private var _filterColorLocation: UniformLocation? = null

    private inline var intensityLocation: UniformLocation
        get() = _intensityLocation!!
        set(value) {
            _intensityLocation = value
        }
    private inline var filterColorLocation: UniformLocation
        get() = _filterColorLocation!!
        set(value) {
            _filterColorLocation = value
        }

    constructor() : this(1.0f, floatArrayOf(0.6f, 0.45f, 0.3f, 1.0f))

    override fun onInit() {
        super.onInit()
        _intensityLocation = Kgl.getUniformLocation(program, "intensity")
        _filterColorLocation = Kgl.getUniformLocation(program, "filterColor")
    }

    override fun onInitialized() {
        super.onInitialized()
        setIntensity(1.0f)
        setColor(floatArrayOf(0.6f, 0.45f, 0.3f, 1.0f))
    }

    fun setIntensity(intensity: Float) {
        this.intensity = intensity
        if(_intensityLocation != null) {
            setFloat(intensityLocation, this.intensity)
        }
    }

    fun setColor(color: FloatArray) {
        this.color = color
        setColor(this.color[0], this.color[1], this.color[2])

    }

    fun setColor(red: Float, green: Float, blue: Float) {
        if(_filterColorLocation != null) setFloatVec3(filterColorLocation, floatArrayOf(red, green, blue))
    }
}
