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

/**
 * This uses a similar process as the GPUImageToonFilter, only it precedes the toon effect
 * with a Gaussian blur to smooth out noise.
 */
class GPUImageSmoothToonFilter// First pass: apply a variable Gaussian blur

// Second pass: run the Sobel edge detection on this blurred image, along with a posterization effect
/**
 * Setup and Tear down
 */() : GPUImageFilterGroup() {

    private val blurFilter: GPUImageGaussianBlurFilter = GPUImageGaussianBlurFilter()
    private val toonFilter: GPUImageToonFilter

    init {
        addFilter(blurFilter)
        toonFilter = GPUImageToonFilter()
        addFilter(toonFilter)
        filters.add(blurFilter)
    }

    override fun onInitialized() {
        super.onInitialized()
        setBlurSize(0.5f)
        setThreshold(0.2f)
        setQuantizationLevels(10.0f)
    }

    /**
     * Accessors
     */
    fun setTexelWidth(value: Float) {
        toonFilter.setTexelWidth(value)
    }

    fun setTexelHeight(value: Float) {
        toonFilter.setTexelHeight(value)
    }

    fun setBlurSize(value: Float) {
        blurFilter.setBlurSize(value)
    }

    fun setThreshold(value: Float) {
        toonFilter.setThreshold(value)
    }

    fun setQuantizationLevels(value: Float) {
        toonFilter.setQuantizationLevels(value)
    }

}
