package org.qinetik.gpuimage.filter

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

/**
 * Created by vashisthg 30/05/14.
 */
class GPUImageLevelsFilter private constructor(
    private val min: FloatArray,
    private val mid: FloatArray,
    private val max: FloatArray,
    minOUt: FloatArray,
    maxOut: FloatArray
) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, LEVELS_FRAGMET_SHADER) {

    companion object {

        const val LOGTAG: String = "GPUImageLevelsFilter"

        const val LEVELS_FRAGMET_SHADER: String = " varying highp vec2 textureCoordinate;\n" +
                " \n" +
                " uniform sampler2D inputImageTexture;\n" +
                " uniform mediump vec3 levelMinimum;\n" +
                " uniform mediump vec3 levelMiddle;\n" +
                " uniform mediump vec3 levelMaximum;\n" +
                " uniform mediump vec3 minOutput;\n" +
                " uniform mediump vec3 maxOutput;\n" +
                " \n" +
                " void main()\n" +
                " {\n" +
                "     mediump vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "     \n" +
                "     gl_FragColor = vec4( mix(minOutput, maxOutput, pow(min(max(textureColor.rgb -levelMinimum, vec3(0.0)) / (levelMaximum - levelMinimum  ), vec3(1.0)), 1.0 /levelMiddle)) , textureColor.a);\n" +
                " }\n"
    }

    private var _minLocation: UniformLocation? = null
    private var _midLocation: UniformLocation? = null
    private var _maxLocation: UniformLocation? = null
    private var _minOutputLocation: UniformLocation? = null
    private var _maxOutputLocation: UniformLocation? = null

    private val minOutput: FloatArray = minOUt
    private val maxOutput: FloatArray = maxOut

    private inline var minLocation: UniformLocation
        get() = _minLocation!!
        set(value){
            _minLocation = value
        }
    private inline var midLocation: UniformLocation
        get() = _midLocation!!
        set(value){
            _midLocation = value
        }
    private inline var maxLocation: UniformLocation
        get() = _maxLocation!!
        set(value){
            _maxLocation = value
        }
    private inline var minOutputLocation: UniformLocation
        get() = _minOutputLocation!!
        set(value){
            _minOutputLocation = value
        }
    private inline var maxOutputLocation: UniformLocation
        get() = _maxOutputLocation!!
        set(value){
            _maxOutputLocation = value
        }

    constructor() : this(
        floatArrayOf(0.0f, 0.0f, 0.0f),
        floatArrayOf(1.0f, 1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f, 1.0f),
        floatArrayOf(0.0f, 0.0f, 0.0f),
        floatArrayOf(1.0f, 1.0f, 1.0f),
    )

    override fun onInit() {
        super.onInit()
        _minLocation = Kgl.getUniformLocation(program, "levelMinimum")
        _midLocation = Kgl.getUniformLocation(program, "levelMiddle")
        _maxLocation = Kgl.getUniformLocation(program, "levelMaximum")
        _minOutputLocation = Kgl.getUniformLocation(program, "minOutput")
        _maxOutputLocation = Kgl.getUniformLocation(program, "maxOutput")
    }

    override fun onInitialized() {
        super.onInitialized()
        setMin(0.0f, 1.0f, 1.0f, 0.0f, 1.0f)
        updateUniforms()
    }


    fun updateUniforms() {
        if(_minLocation != null) setFloatVec3(minLocation, min)
        if(_midLocation != null) setFloatVec3(midLocation, mid)
        if(_maxLocation != null) setFloatVec3(maxLocation, max)
        if(_minOutputLocation != null) setFloatVec3(minOutputLocation, minOutput)
        if(_maxOutputLocation != null) setFloatVec3(maxOutputLocation, maxOutput)
    }

    fun setMin(min: Float, mid: Float, max: Float, minOut: Float, maxOut: Float) {
        setRedMin(min, mid, max, minOut, maxOut)
        setGreenMin(min, mid, max, minOut, maxOut)
        setBlueMin(min, mid, max, minOut, maxOut)
    }

    fun setMin(min: Float, mid: Float, max: Float) {
        setMin(min, mid, max, 0.0f, 1.0f)
    }

    fun setRedMin(min: Float, mid: Float, max: Float, minOut: Float, maxOut: Float) {
        this.min[0] = min
        this.mid[0] = mid
        this.max[0] = max
        minOutput[0] = minOut
        maxOutput[0] = maxOut
        updateUniforms()
    }

    fun setRedMin(min: Float, mid: Float, max: Float) {
        setRedMin(min, mid, max, 0f, 1f)
    }

    fun setGreenMin(min: Float, mid: Float, max: Float, minOut: Float, maxOut: Float) {
        this.min[1] = min
        this.mid[1] = mid
        this.max[1] = max
        minOutput[1] = minOut
        maxOutput[1] = maxOut
        updateUniforms()
    }

    fun setGreenMin(min: Float, mid: Float, max: Float) {
        setGreenMin(min, mid, max, 0f, 1f)
    }

    fun setBlueMin(min: Float, mid: Float, max: Float, minOut: Float, maxOut: Float) {
        this.min[2] = min
        this.mid[2] = mid
        this.max[2] = max
        minOutput[2] = minOut
        maxOutput[2] = maxOut
        updateUniforms()
    }

    fun setBlueMin(min: Float, mid: Float, max: Float) {
        setBlueMin(min, mid, max, 0f, 1f)
    }
}
