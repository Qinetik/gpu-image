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
 * Applies a grayscale effect to the image.
 */
class GPUImagePixelationFilter : GPUImageFilter(NO_FILTER_VERTEX_SHADER, PIXELATION_FRAGMENT_SHADER) {
    companion object {
        const val PIXELATION_FRAGMENT_SHADER: String = "" +
                "precision highp float;\n" +

                "varying vec2 textureCoordinate;\n" +

                "uniform float imageWidthFactor;\n" +
                "uniform float imageHeightFactor;\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform float pixel;\n" +

                "void main()\n" +
                "{\n" +
                "  vec2 uv  = textureCoordinate.xy;\n" +
                "  float dx = pixel * imageWidthFactor;\n" +
                "  float dy = pixel * imageHeightFactor;\n" +
                "  vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));\n" +
                "  vec3 tc = texture2D(inputImageTexture, coord).xyz;\n" +
                "  gl_FragColor = vec4(tc, 1.0);\n" +
                "}"
    }

    private var _imageWidthFactorLocation: UniformLocation? = null
    private var _imageHeightFactorLocation: UniformLocation? = null
    private var _pixelLocation: UniformLocation? = null

    private inline var imageWidthFactorLocation: UniformLocation
        get() = _imageWidthFactorLocation!!
        set(value){
            _imageWidthFactorLocation = value
        }
    private inline var imageHeightFactorLocation: UniformLocation
        get() = _imageHeightFactorLocation!!
        set(value){
            _imageHeightFactorLocation = value
        }
    private inline var pixelLocation: UniformLocation
        get() = _pixelLocation!!
        set(value){
            _pixelLocation = value
        }

    private var pixel: Float = 1.0f


    override fun onInit() {
        super.onInit()
        _imageWidthFactorLocation = Kgl.getUniformLocation(program, "imageWidthFactor")
        _imageHeightFactorLocation = Kgl.getUniformLocation(program, "imageHeightFactor")
        _pixelLocation = Kgl.getUniformLocation(program, "pixel")
    }

    override fun onInitialized() {
        super.onInitialized()
        setPixel(pixel)
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        setFloat(imageWidthFactorLocation, 1.0f / width.toFloat())
        setFloat(imageHeightFactorLocation, 1.0f / height.toFloat())
    }

    fun setPixel(pixel: Float) {
        this.pixel = pixel
        if (_pixelLocation != null) {
            setFloat(pixelLocation, this.pixel)
        }
    }
}
