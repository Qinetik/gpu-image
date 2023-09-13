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

    private var imageWidthFactorLocation: Int = 0
    private var imageHeightFactorLocation: Int = 0
    private var pixel: Float = 1.0f
    private var pixelLocation: Int = 0

    override fun onInit() {
        super.onInit()
        imageWidthFactorLocation = GLES20.glGetUniformLocation(program, "imageWidthFactor")
        imageHeightFactorLocation = GLES20.glGetUniformLocation(program, "imageHeightFactor")
        pixelLocation = GLES20.glGetUniformLocation(program, "pixel")
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
        setFloat(pixelLocation, this.pixel)
    }
}
