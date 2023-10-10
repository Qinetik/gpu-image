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
import org.qinetik.gpuimage.filter.GPUImageFilter

/**
 * Adjusts the individual RGB channels of an image
 * red: Normalized values by which each color channel is multiplied. The range is from 0.0 up, with 1.0 as the default.
 * green:
 * blue:
 */
class GPUImageRGBFilter : GPUImageFilter {
    companion object {
        const val RGB_FRAGMENT_SHADER: String = "" +
                "  varying highp vec2 textureCoordinate;\n" +
                "  \n" +
                "  uniform sampler2D inputImageTexture;\n" +
                "  uniform highp float red;\n" +
                "  uniform highp float green;\n" +
                "  uniform highp float blue;\n" +
                "  \n" +
                "  void main()\n" +
                "  {\n" +
                "      highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "      \n" +
                "      gl_FragColor = vec4(textureColor.r * red, textureColor.g * green, textureColor.b * blue, 1.0);\n" +
                "  }\n"
    }

    private var redLocation: Int = 0
    private var red: Float
    private var greenLocation: Int = 0
    private var green: Float
    private var blueLocation: Int = 0
    private var blue: Float

    constructor() : this(1.0f, 1.0f, 1.0f)

    constructor(red: Float, green: Float, blue: Float) : super(NO_FILTER_VERTEX_SHADER, RGB_FRAGMENT_SHADER) {
        this.red = red
        this.green = green
        this.blue = blue
    }

    override fun onInit() {
        super.onInit()
        redLocation = GLES20.glGetUniformLocation(program, "red")
        greenLocation = GLES20.glGetUniformLocation(program, "green")
        blueLocation = GLES20.glGetUniformLocation(program, "blue")
    }

    override fun onInitialized() {
        super.onInitialized()
        setRed(red)
        setGreen(green)
        setBlue(blue)
    }

    fun setRed(red: Float) {
        this.red = red
        setFloat(redLocation, this.red)
    }

    fun setGreen(green: Float) {
        this.green = green
        setFloat(greenLocation, this.green)
    }

    fun setBlue(blue: Float) {
        this.blue = blue
        setFloat(blueLocation, this.blue)
    }

}
