/**
 * @author wysaid
 * @mail admin@wysaid.org
 */

package org.qinetik.gpuimage.filter

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter


class GPUImageBilateralBlurFilter : GPUImageFilter {
    companion object {
        const val BILATERAL_VERTEX_SHADER: String = "" +
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +

                "const int GAUSSIAN_SAMPLES = 9;\n" +

                "uniform vec2 singleStepOffset;\n" +

                "varying vec2 textureCoordinate;\n" +
                "varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +

                "void main()\n" +
                "{\n" +
                "	gl_Position = position;\n" +
                "	textureCoordinate = inputTextureCoordinate.xy;\n" +

                "	int multiplier = 0;\n" +
                "	vec2 blurStep;\n" +

                "	for (int i = 0; i < GAUSSIAN_SAMPLES; i++)\n" +
                "	{\n" +
                "		multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));\n" +

                "		blurStep = float(multiplier) * singleStepOffset;\n" +
                "		blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;\n" +
                "	}\n" +
                "}"

        const val BILATERAL_FRAGMENT_SHADER: String = "" +
                "uniform sampler2D inputImageTexture;\n" +

                " const lowp int GAUSSIAN_SAMPLES = 9;\n" +

                " varying highp vec2 textureCoordinate;\n" +
                " varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +

                " uniform mediump float distanceNormalizationFactor;\n" +

                " void main()\n" +
                " {\n" +
                "     lowp vec4 centralColor;\n" +
                "     lowp float gaussianWeightTotal;\n" +
                "     lowp vec4 sum;\n" +
                "     lowp vec4 sampleColor;\n" +
                "     lowp float distanceFromCentralColor;\n" +
                "     lowp float gaussianWeight;\n" +
                "     \n" +
                "     centralColor = texture2D(inputImageTexture, blurCoordinates[4]);\n" +
                "     gaussianWeightTotal = 0.18;\n" +
                "     sum = centralColor * 0.18;\n" +
                "     \n" +
                "     sampleColor = texture2D(inputImageTexture, blurCoordinates[0]);\n" +
                "     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "     gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n" +
                "     gaussianWeightTotal += gaussianWeight;\n" +
                "     sum += sampleColor * gaussianWeight;\n" +

                "     sampleColor = texture2D(inputImageTexture, blurCoordinates[1]);\n" +
                "     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "     gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);\n" +
                "     gaussianWeightTotal += gaussianWeight;\n" +
                "     sum += sampleColor * gaussianWeight;\n" +

                "     sampleColor = texture2D(inputImageTexture, blurCoordinates[2]);\n" +
                "     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "     gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);\n" +
                "     gaussianWeightTotal += gaussianWeight;\n" +
                "     sum += sampleColor * gaussianWeight;\n" +

                "     sampleColor = texture2D(inputImageTexture, blurCoordinates[3]);\n" +
                "     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "     gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);\n" +
                "     gaussianWeightTotal += gaussianWeight;\n" +
                "     sum += sampleColor * gaussianWeight;\n" +

                "     sampleColor = texture2D(inputImageTexture, blurCoordinates[5]);\n" +
                "     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "     gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);\n" +
                "     gaussianWeightTotal += gaussianWeight;\n" +
                "     sum += sampleColor * gaussianWeight;\n" +

                "     sampleColor = texture2D(inputImageTexture, blurCoordinates[6]);\n" +
                "     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "     gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);\n" +
                "     gaussianWeightTotal += gaussianWeight;\n" +
                "     sum += sampleColor * gaussianWeight;\n" +

                "     sampleColor = texture2D(inputImageTexture, blurCoordinates[7]);\n" +
                "     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "     gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);\n" +
                "     gaussianWeightTotal += gaussianWeight;\n" +
                "     sum += sampleColor * gaussianWeight;\n" +

                "     sampleColor = texture2D(inputImageTexture, blurCoordinates[8]);\n" +
                "     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n" +
                "     gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n" +
                "     gaussianWeightTotal += gaussianWeight;\n" +
                "     sum += sampleColor * gaussianWeight;\n" +
                "     gl_FragColor = sum / gaussianWeightTotal;\n" +
                " }"
    }

    private var distanceNormalizationFactor: Float = 0f

    private var _disFactorLocation: UniformLocation? = null
    private var _singleStepOffsetLocation: UniformLocation? = null

    private inline var disFactorLocation: UniformLocation
        get() = _disFactorLocation!!
        set(value){
            _disFactorLocation = value
        }
    private inline var singleStepOffsetLocation: UniformLocation
        get() = _singleStepOffsetLocation!!
        set(value){
            _singleStepOffsetLocation = value
        }

    constructor() : this(8.0f)

    constructor(distanceNormalizationFactor: Float) : super(BILATERAL_VERTEX_SHADER, BILATERAL_FRAGMENT_SHADER) {
        this.distanceNormalizationFactor = distanceNormalizationFactor
    }

    override fun onInit() {
        super.onInit()
        _disFactorLocation = Kgl.getUniformLocation(program, "distanceNormalizationFactor")
        _singleStepOffsetLocation = Kgl.getUniformLocation(program, "singleStepOffset")
    }

    override fun onInitialized() {
        super.onInitialized()
        setDistanceNormalizationFactor(distanceNormalizationFactor)
    }

    fun setDistanceNormalizationFactor(newValue: Float) {
        distanceNormalizationFactor = newValue
        if(_disFactorLocation != null) setFloat(disFactorLocation, newValue)
    }

    private fun setTexelSize(w: Float, h: Float) {
        if(_singleStepOffsetLocation != null) setFloatVec2(singleStepOffsetLocation, floatArrayOf(1.0f / w, 1.0f / h))
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        setTexelSize(width.toFloat(), height.toFloat())
    }
}
