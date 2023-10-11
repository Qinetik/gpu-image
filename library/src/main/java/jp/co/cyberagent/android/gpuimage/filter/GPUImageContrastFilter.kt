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

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

/**
 * Changes the contrast of the image.<br>
 * <br>
 * contrast value ranges from 0.0 to 4.0, with 1.0 as the normal level
 */
class GPUImageContrastFilter : GPUImageFilter {
    companion object {
        const val CONTRAST_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                " \n" +
                " uniform sampler2D inputImageTexture;\n" +
                " uniform lowp float contrast;\n" +
                " \n" +
                " void main()\n" +
                " {\n" +
                "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "     \n" +
                "     gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), textureColor.w);\n" +
                " }"
    }

    private var _contrastLocation: UniformLocation? = null
    private var contrast: Float = 0f

    private inline var contrastLocation: UniformLocation
        get() = _contrastLocation!!
        set(value){
            _contrastLocation = value
        }

    constructor() : this(1.2f)

    constructor(contrast: Float) : super(NO_FILTER_VERTEX_SHADER, CONTRAST_FRAGMENT_SHADER) {
        this.contrast = contrast
    }

    override fun onInit() {
        super.onInit()
        _contrastLocation = Kgl.getUniformLocation(program, "contrast")
    }

    override fun onInitialized() {
        super.onInitialized()
        setContrast(contrast)
    }

    fun setContrast(contrast: Float) {
        this.contrast = contrast
        if(_contrastLocation != null) setFloat(contrastLocation, this.contrast)
    }
}
