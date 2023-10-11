package jp.co.cyberagent.android.gpuimage.filter

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

class GPUImageSolarizeFilter : GPUImageFilter {
    companion object {
        const val SOLATIZE_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform highp float threshold;\n" +
                "\n" +
                "const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "    highp float luminance = dot(textureColor.rgb, W);\n" +
                "    highp float thresholdResult = step(luminance, threshold);\n" +
                "    highp vec3 finalColor = abs(thresholdResult - textureColor.rgb);\n" +
                "    \n" +
                "    gl_FragColor = vec4(finalColor, textureColor.w);\n" +
                "}"
    }

    private var _uniformThresholdLocation: UniformLocation? = null

    private inline var uniformThresholdLocation : UniformLocation
        get() = _uniformThresholdLocation!!
        set(value){
            _uniformThresholdLocation = value
        }

    private var threshold: Float

    constructor() : this(0.5f)

    constructor(threshold: Float) : super(NO_FILTER_VERTEX_SHADER, SOLATIZE_FRAGMENT_SHADER) {
        this.threshold = threshold
    }

    override fun onInit() {
        super.onInit()
        _uniformThresholdLocation = Kgl.getUniformLocation(program, "threshold")
    }

    override fun onInitialized() {
        super.onInitialized()
        setThreshold(threshold)
    }

    fun setThreshold(threshold: Float) {
        this.threshold = threshold
        if(_uniformThresholdLocation != null) {
            setFloat(uniformThresholdLocation, threshold)
        }
    }

}
