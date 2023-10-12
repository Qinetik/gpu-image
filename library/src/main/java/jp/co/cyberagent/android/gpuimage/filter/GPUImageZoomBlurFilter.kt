package jp.co.cyberagent.android.gpuimage.filter

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter
import org.qinetik.gpuimage.utils.FloatPoint

class GPUImageZoomBlurFilter : GPUImageFilter {
    companion object {
        const val ZOOM_BLUR_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "\n" +
                "uniform highp vec2 blurCenter;\n" +
                "uniform highp float blurSize;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    // TODO: Do a more intelligent scaling based on resolution here\n" +
                "    highp vec2 samplingOffset = 1.0/100.0 * (blurCenter - textureCoordinate) * blurSize;\n" +
                "    \n" +
                "    lowp vec4 fragmentColor = texture2D(inputImageTexture, textureCoordinate) * 0.18;\n" +
                "    fragmentColor += texture2D(inputImageTexture, textureCoordinate + samplingOffset) * 0.15;\n" +
                "    fragmentColor += texture2D(inputImageTexture, textureCoordinate + (2.0 * samplingOffset)) *  0.12;\n" +
                "    fragmentColor += texture2D(inputImageTexture, textureCoordinate + (3.0 * samplingOffset)) * 0.09;\n" +
                "    fragmentColor += texture2D(inputImageTexture, textureCoordinate + (4.0 * samplingOffset)) * 0.05;\n" +
                "    fragmentColor += texture2D(inputImageTexture, textureCoordinate - samplingOffset) * 0.15;\n" +
                "    fragmentColor += texture2D(inputImageTexture, textureCoordinate - (2.0 * samplingOffset)) *  0.12;\n" +
                "    fragmentColor += texture2D(inputImageTexture, textureCoordinate - (3.0 * samplingOffset)) * 0.09;\n" +
                "    fragmentColor += texture2D(inputImageTexture, textureCoordinate - (4.0 * samplingOffset)) * 0.05;\n" +
                "    \n" +
                "    gl_FragColor = fragmentColor;\n" +
                "}\n"
    }


    private var blurCenter: FloatPoint
    private var blurSize: Float

    private var _blurCenterLocation: UniformLocation? = null
    private var _blurSizeLocation: UniformLocation? = null

    private inline var blurCenterLocation: UniformLocation
        get() = _blurCenterLocation!!
        set(value){
            _blurCenterLocation = value
        }

    private inline var blurSizeLocation: UniformLocation
        get() = _blurSizeLocation!!
        set(value){
            _blurSizeLocation = value
        }

    constructor() : this(FloatPoint(0.5f, 0.5f), 1.0f)

    constructor(blurCenter: FloatPoint, blurSize: Float) : super(
        NO_FILTER_VERTEX_SHADER,
        ZOOM_BLUR_FRAGMENT_SHADER
    ) {
        this.blurCenter = blurCenter
        this.blurSize = blurSize
    }

    override fun onInit() {
        super.onInit()
        _blurCenterLocation = Kgl.getUniformLocation(program, "blurCenter")
        _blurSizeLocation = Kgl.getUniformLocation(program, "blurSize")
    }

    override fun onInitialized() {
        super.onInitialized()
        setBlurCenter(blurCenter)
        setBlurSize(blurSize)
    }

    fun setBlurCenter(blurCenter: FloatPoint) {
        this.blurCenter = blurCenter
        if(_blurCenterLocation != null) {
            setPoint(blurCenterLocation, blurCenter.x, blurCenter.y)
        }
    }

    fun setBlurSize(blurSize: Float) {
        this.blurSize = blurSize
        if(_blurSizeLocation != null) {
            setFloat(blurSizeLocation, blurSize)
        }
    }

}
