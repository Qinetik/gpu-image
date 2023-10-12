package org.qinetik.gpuimage.filter

/**
 * Applies sobel edge detection on the image.
 */
class GPUImageThresholdEdgeDetectionFilter : GPUImageFilterGroup() {

    init {
        addFilter(GPUImageGrayscaleFilter())
        addFilter(GPUImageSobelThresholdFilter())
    }

    fun setLineSize(size: Float) {
        (filters.get(1) as GPUImage3x3TextureSamplingFilter).setLineSize(size)
    }

    fun setThreshold(threshold: Float) {
        (filters.get(1) as GPUImageSobelThresholdFilter).setThreshold(threshold)
    }

}
