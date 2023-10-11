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

import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

open class GPUImage3x3TextureSamplingFilter : GPUImageFilter {

    companion object {
        const val THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER: String = "" +
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                "\n" +
                "uniform highp float texelWidth; \n" +
                "uniform highp float texelHeight; \n" +
                "\n" +
                "varying vec2 textureCoordinate;\n" +
                "varying vec2 leftTextureCoordinate;\n" +
                "varying vec2 rightTextureCoordinate;\n" +
                "\n" +
                "varying vec2 topTextureCoordinate;\n" +
                "varying vec2 topLeftTextureCoordinate;\n" +
                "varying vec2 topRightTextureCoordinate;\n" +
                "\n" +
                "varying vec2 bottomTextureCoordinate;\n" +
                "varying vec2 bottomLeftTextureCoordinate;\n" +
                "varying vec2 bottomRightTextureCoordinate;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "\n" +
                "    vec2 widthStep = vec2(texelWidth, 0.0);\n" +
                "    vec2 heightStep = vec2(0.0, texelHeight);\n" +
                "    vec2 widthHeightStep = vec2(texelWidth, texelHeight);\n" +
                "    vec2 widthNegativeHeightStep = vec2(texelWidth, -texelHeight);\n" +
                "\n" +
                "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                "    leftTextureCoordinate = inputTextureCoordinate.xy - widthStep;\n" +
                "    rightTextureCoordinate = inputTextureCoordinate.xy + widthStep;\n" +
                "\n" +
                "    topTextureCoordinate = inputTextureCoordinate.xy - heightStep;\n" +
                "    topLeftTextureCoordinate = inputTextureCoordinate.xy - widthHeightStep;\n" +
                "    topRightTextureCoordinate = inputTextureCoordinate.xy + widthNegativeHeightStep;\n" +
                "\n" +
                "    bottomTextureCoordinate = inputTextureCoordinate.xy + heightStep;\n" +
                "    bottomLeftTextureCoordinate = inputTextureCoordinate.xy - widthNegativeHeightStep;\n" +
                "    bottomRightTextureCoordinate = inputTextureCoordinate.xy + widthHeightStep;\n" +
                "}"
    }

    private var _uniformTexelWidthLocation: UniformLocation? = null
    private var _uniformTexelHeightLocation: UniformLocation? = null

    private inline var uniformTexelWidthLocation: UniformLocation
        get() = _uniformTexelWidthLocation!!
        set(value){
            _uniformTexelWidthLocation = value
        }
    private inline var uniformTexelHeightLocation: UniformLocation
        get() = _uniformTexelHeightLocation!!
        set(value){
            _uniformTexelHeightLocation = value
        }

    private var hasOverriddenImageSizeFactor: Boolean = false
    private var texelWidth = 0f
    private var texelHeight = 0f
    private var lineSize = 1.0f

    constructor() : this(NO_FILTER_VERTEX_SHADER)

    constructor(fragmentShader: String) : super(THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER, fragmentShader)

    override fun onInit() {
        super.onInit()
        _uniformTexelWidthLocation = Kgl.getUniformLocation(program, "texelWidth")
        _uniformTexelHeightLocation = Kgl.getUniformLocation(program, "texelHeight")
    }

    override fun onInitialized() {
        super.onInitialized()
        if (texelWidth != 0f) {
            updateTexelValues()
        }
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        if (!hasOverriddenImageSizeFactor) {
            setLineSize(lineSize)
        }
    }

    fun setTexelWidth(texelWidth: Float) {
        hasOverriddenImageSizeFactor = true
        this.texelWidth = texelWidth
        setFloat(uniformTexelWidthLocation, texelWidth)
    }

    fun setTexelHeight(texelHeight: Float) {
        hasOverriddenImageSizeFactor = true
        this.texelHeight = texelHeight
        setFloat(uniformTexelHeightLocation, texelHeight)
    }

    fun setLineSize(size: Float) {
        lineSize = size
        texelWidth = size / outputWidth
        texelHeight = size / outputHeight
        updateTexelValues()
    }

    private fun updateTexelValues() {
        if(_uniformTexelWidthLocation != null) setFloat(uniformTexelWidthLocation, texelWidth)
        if(_uniformTexelHeightLocation != null) setFloat(uniformTexelHeightLocation, texelHeight)
    }
}
