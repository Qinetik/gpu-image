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

import android.opengl.GLES20

/**
 * brightness value ranges from -1.0 to 1.0, with 0.0 as the normal level
 */
class GPUImageBrightnessFilter : GPUImageFilter {

    companion object {
        const val BRIGHTNESS_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                " \n" +
                " uniform sampler2D inputImageTexture;\n" +
                " uniform lowp float brightness;\n" +
                " \n" +
                " void main()\n" +
                " {\n" +
                "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "     \n" +
                "     gl_FragColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.w);\n" +
                " }"
    }


    private var brightnessLocation: Int = 0
    private var brightness: Float = 0f

    constructor() : this(0.0f)

    constructor(brightness: Float) : super(NO_FILTER_VERTEX_SHADER, BRIGHTNESS_FRAGMENT_SHADER) {
        this.brightness = brightness
    }

    override fun onInit() {
        super.onInit()
        brightnessLocation = GLES20.glGetUniformLocation(program, "brightness")
    }

    override fun onInitialized() {
        super.onInitialized()
        setBrightness(brightness)
    }

    fun setBrightness(brightness: Float) {
        this.brightness = brightness
        setFloat(brightnessLocation, this.brightness)
    }

}
