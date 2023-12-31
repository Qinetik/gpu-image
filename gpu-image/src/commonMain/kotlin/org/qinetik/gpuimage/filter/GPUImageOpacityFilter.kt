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

package org.qinetik.gpuimage.filter;

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

/**
 * Adjusts the alpha channel of the incoming image
 * opacity: The value to multiply the incoming alpha channel for each pixel by (0.0 - 1.0, with 1.0 as the default)
 */
public class GPUImageOpacityFilter public constructor(private var opacity: Float) :
    GPUImageFilter(NO_FILTER_VERTEX_SHADER, OPACITY_FRAGMENT_SHADER) {
    companion object {
        const val OPACITY_FRAGMENT_SHADER: String = "" +
                "  varying highp vec2 textureCoordinate;\n" +
                "  \n" +
                "  uniform sampler2D inputImageTexture;\n" +
                "  uniform lowp float opacity;\n" +
                "  \n" +
                "  void main()\n" +
                "  {\n" +
                "      lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "      \n" +
                "      gl_FragColor = vec4(textureColor.rgb, textureColor.a * opacity);\n" +
                "  }\n";
    }

    private var _opacityLocation: UniformLocation? = null

    private inline var opacityLocation: UniformLocation
        get() = _opacityLocation!!
        set(value) {
            _opacityLocation = value
        }

    constructor() : this(1.0f)

    override fun onInit() {
        super.onInit();
        _opacityLocation = Kgl.getUniformLocation(program, "opacity");
    }

    override fun onInitialized() {
        super.onInitialized();
        setOpacity(opacity);
    }

    fun setOpacity(opacity: Float) {
        this.opacity = opacity;
        if(_opacityLocation != null) {
            setFloat(opacityLocation, this.opacity);
        }
    }
}
