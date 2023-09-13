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

class GPUImageTwoPassTextureSamplingFilter(
    firstVertexShader: String,
    firstFragmentShader: String,
    secondVertexShader: String,
    secondFragmentShader: String
) : GPUImageTwoPassFilter(
    firstVertexShader, firstFragmentShader,
    secondVertexShader, secondFragmentShader
) {

    override fun onInit() {
        super.onInit()
        initTexelOffsets()
    }

    protected fun initTexelOffsets() {
        var ratio = getHorizontalTexelOffsetRatio()
        var filter: GPUImageFilter = filters.get(0)
        var texelWidthOffsetLocation: Int = GLES20.glGetUniformLocation(filter.program, "texelWidthOffset")
        var texelHeightOffsetLocation: Int = GLES20.glGetUniformLocation(filter.program, "texelHeightOffset")
        filter.setFloat(texelWidthOffsetLocation, ratio / outputWidth)
        filter.setFloat(texelHeightOffsetLocation, 0f)

        ratio = getVerticalTexelOffsetRatio()
        filter = filters.get(1)
        texelWidthOffsetLocation = GLES20.glGetUniformLocation(filter.program, "texelWidthOffset")
        texelHeightOffsetLocation = GLES20.glGetUniformLocation(filter.program, "texelHeightOffset")
        filter.setFloat(texelWidthOffsetLocation, 0f)
        filter.setFloat(texelHeightOffsetLocation, ratio / outputHeight)
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        initTexelOffsets()
    }

    fun getVerticalTexelOffsetRatio(): Float {
        return 1f
    }

    fun getHorizontalTexelOffsetRatio(): Float {
        return 1f
    }
}
