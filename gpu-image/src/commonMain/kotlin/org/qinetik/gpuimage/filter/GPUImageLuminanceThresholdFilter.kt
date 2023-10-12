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

class GPUImageLuminanceThresholdFilter : GPUImageFilter {

    companion object {
        const val LUMINANCE_THRESHOLD_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform highp float threshold;\n" +
                "\n" +
                "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "    highp float luminance = dot(textureColor.rgb, W);\n" +
                "    highp float thresholdResult = step(threshold, luminance);\n" +
                "    \n" +
                "    gl_FragColor = vec4(vec3(thresholdResult), textureColor.w);\n" +
                "}"
    }

    private var _uniformThresholdLocation: UniformLocation? = null
    private var threshold: Float

    private inline var uniformThresholdLocation: UniformLocation
        get() = _uniformThresholdLocation!!
        set(value){
            _uniformThresholdLocation = value
        }

    constructor() : this(0.5f)

    constructor(threshold: Float) : super(NO_FILTER_VERTEX_SHADER, LUMINANCE_THRESHOLD_FRAGMENT_SHADER) {
        this.threshold = threshold
    }

    override fun onInit() {
        super.onInit()
        _uniformThresholdLocation = Kgl.getUniformLocation(program, "threshold")
    }

    override fun onInitialized() {
        super.onInitialized()
        setThreshold(threshold)
    }

    fun setThreshold(threshold: Float) {
        this.threshold = threshold
        if(_uniformThresholdLocation != null) {
            setFloat(uniformThresholdLocation, threshold)
        }
    }

}
