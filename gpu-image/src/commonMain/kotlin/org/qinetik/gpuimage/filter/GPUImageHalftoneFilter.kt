package org.qinetik.gpuimage.filter

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

class GPUImageHalftoneFilter(private var fractionalWidthOfAPixel: Float) :
    GPUImageFilter(NO_FILTER_VERTEX_SHADER, HALFTONE_FRAGMENT_SHADER) {
    companion object {
        const val HALFTONE_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +

                "uniform sampler2D inputImageTexture;\n" +

                "uniform highp float fractionalWidthOfPixel;\n" +
                "uniform highp float aspectRatio;\n" +

                "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +

                "void main()\n" +
                "{\n" +
                "  highp vec2 sampleDivisor = vec2(fractionalWidthOfPixel, fractionalWidthOfPixel / aspectRatio);\n" +
                "  highp vec2 samplePos = textureCoordinate - mod(textureCoordinate, sampleDivisor) + 0.5 * sampleDivisor;\n" +
                "  highp vec2 textureCoordinateToUse = vec2(textureCoordinate.x, (textureCoordinate.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                "  highp vec2 adjustedSamplePos = vec2(samplePos.x, (samplePos.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                "  highp float distanceFromSamplePoint = distance(adjustedSamplePos, textureCoordinateToUse);\n" +
                "  lowp vec3 sampledColor = texture2D(inputImageTexture, samplePos).rgb;\n" +
                "  highp float dotScaling = 1.0 - dot(sampledColor, W);\n" +
                "  lowp float checkForPresenceWithinDot = 1.0 - step(distanceFromSamplePoint, (fractionalWidthOfPixel * 0.5) * dotScaling);\n" +
                "  gl_FragColor = vec4(vec3(checkForPresenceWithinDot), 1.0);\n" +
                "}"
    }

    private var _fractionalWidthOfPixelLocation: UniformLocation? = null
    private var _aspectRatioLocation: UniformLocation? = null

    private inline var fractionalWidthOfPixelLocation: UniformLocation
        get() = _fractionalWidthOfPixelLocation!!
        set(value){
            _fractionalWidthOfPixelLocation = value
        }
    private inline var aspectRatioLocation: UniformLocation
        get() = _aspectRatioLocation!!
        set(value){
            _aspectRatioLocation = value
        }

    private var aspectRatio: Float = 0f

    constructor() : this(0.01f)

    override fun onInit() {
        super.onInit()
        _fractionalWidthOfPixelLocation = Kgl.getUniformLocation(program, "fractionalWidthOfPixel")
        _aspectRatioLocation = Kgl.getUniformLocation(program, "aspectRatio")
    }

    override fun onInitialized() {
        super.onInitialized()
        setFractionalWidthOfAPixel(fractionalWidthOfAPixel)
        setAspectRatio(aspectRatio)
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        setAspectRatio(height.toFloat() / width.toFloat())
    }

    fun setFractionalWidthOfAPixel(fractionalWidthOfAPixel: Float) {
        this.fractionalWidthOfAPixel = fractionalWidthOfAPixel
        if(_fractionalWidthOfPixelLocation != null) {
            setFloat(fractionalWidthOfPixelLocation, this.fractionalWidthOfAPixel)
        }
    }

    fun setAspectRatio(aspectRatio: Float) {
        this.aspectRatio = aspectRatio
        if(_aspectRatioLocation != null) {
            setFloat(aspectRatioLocation, this.aspectRatio)
        }
    }
}
