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

/**
 * Selectively replaces a color in the first image with the second image
 */
class GPUImageChromaKeyBlendFilter : GPUImageTwoInputFilter {
    companion object {
        const val CHROMA_KEY_BLEND_FRAGMENT_SHADER: String = " precision highp float;\n" +
                " \n" +
                " varying highp vec2 textureCoordinate;\n" +
                " varying highp vec2 textureCoordinate2;\n" +
                "\n" +
                " uniform float thresholdSensitivity;\n" +
                " uniform float smoothing;\n" +
                " uniform vec3 colorToReplace;\n" +
                " uniform sampler2D inputImageTexture;\n" +
                " uniform sampler2D inputImageTexture2;\n" +
                " \n" +
                " void main()\n" +
                " {\n" +
                "     vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "     vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
                "     \n" +
                "     float maskY = 0.2989 * colorToReplace.r + 0.5866 * colorToReplace.g + 0.1145 * colorToReplace.b;\n" +
                "     float maskCr = 0.7132 * (colorToReplace.r - maskY);\n" +
                "     float maskCb = 0.5647 * (colorToReplace.b - maskY);\n" +
                "     \n" +
                "     float Y = 0.2989 * textureColor.r + 0.5866 * textureColor.g + 0.1145 * textureColor.b;\n" +
                "     float Cr = 0.7132 * (textureColor.r - Y);\n" +
                "     float Cb = 0.5647 * (textureColor.b - Y);\n" +
                "     \n" +
                "     float blendValue = 1.0 - smoothstep(thresholdSensitivity, thresholdSensitivity + smoothing, distance(vec2(Cr, Cb), vec2(maskCr, maskCb)));\n" +
                "     gl_FragColor = mix(textureColor, textureColor2, blendValue);\n" +
                " }"

    }

    private var _thresholdSensitivityLocation: UniformLocation? = null
    private var _smoothingLocation: UniformLocation? = null
    private var _colorToReplaceLocation: UniformLocation? = null

    private inline var thresholdSensitivityLocation: UniformLocation
        get() = _thresholdSensitivityLocation!!
        set(value){
            _thresholdSensitivityLocation = value
        }
    private inline var smoothingLocation: UniformLocation
        get() = _smoothingLocation!!
        set(value){
            _smoothingLocation = value
        }
    private inline var colorToReplaceLocation: UniformLocation
        get() = _colorToReplaceLocation!!
        set(value){
            _colorToReplaceLocation = value
        }

    private var thresholdSensitivity: Float = 0.4f
    private var smoothing: Float = 0.1f
    private var colorToReplace: FloatArray = floatArrayOf(0.0f, 1.0f, 0.0f)

    constructor() : super(CHROMA_KEY_BLEND_FRAGMENT_SHADER)

    override fun onInit() {
        super.onInit()
        _thresholdSensitivityLocation = Kgl.getUniformLocation(program, "thresholdSensitivity")
        _smoothingLocation = Kgl.getUniformLocation(program, "smoothing")
        _colorToReplaceLocation = Kgl.getUniformLocation(program, "colorToReplace")
    }

    override fun onInitialized() {
        super.onInitialized()
        setSmoothing(smoothing)
        setThresholdSensitivity(thresholdSensitivity)
        setColorToReplace(colorToReplace[0], colorToReplace[1], colorToReplace[2])
    }

    /**
     * The degree of smoothing controls how gradually similar colors are replaced in the image
     * The default value is 0.1
     */
    fun setSmoothing(smoothing: Float) {
        this.smoothing = smoothing
        if(_smoothingLocation != null) setFloat(smoothingLocation, this.smoothing)
    }

    /**
     * The threshold sensitivity controls how similar pixels need to be colored to be replaced
     * The default value is 0.3
     */
    fun setThresholdSensitivity(thresholdSensitivity: Float) {
        this.thresholdSensitivity = thresholdSensitivity
        if(_thresholdSensitivityLocation != null) setFloat(thresholdSensitivityLocation, this.thresholdSensitivity)
    }

    /**
     * The color to be replaced is specified using individual red, green, and blue components (normalized to 1.0).
     * The default is green: (0.0, 1.0, 0.0).
     *
     * @param redComponent   Red component of color to be replaced
     * @param greenComponent Green component of color to be replaced
     * @param blueComponent  Blue component of color to be replaced
     */
    fun setColorToReplace(redComponent: Float, greenComponent: Float, blueComponent: Float) {
        colorToReplace = floatArrayOf(redComponent, greenComponent, blueComponent)
        if(_colorToReplaceLocation != null) setFloatVec3(colorToReplaceLocation, colorToReplace)
    }
}
