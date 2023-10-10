package jp.co.cyberagent.android.gpuimage.filter

import android.opengl.GLES20
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

    private var minLocation: Int = 0
    private var midLocation: Int = 0
    private var maxLocation: Int = 0
    private var minOutputLocation: Int = 0
    private val minOutput: FloatArray = minOUt
    private var maxOutputLocation: Int = 0
    private val maxOutput: FloatArray = maxOut

    constructor() : this(
        floatArrayOf(0.0f, 0.0f, 0.0f),
        floatArrayOf(1.0f, 1.0f, 1.0f),
        floatArrayOf(1.0f, 1.0f, 1.0f),
        floatArrayOf(0.0f, 0.0f, 0.0f),
        floatArrayOf(1.0f, 1.0f, 1.0f),
    )

    override fun onInit() {
        super.onInit()
        minLocation = GLES20.glGetUniformLocation(program, "levelMinimum")
        midLocation = GLES20.glGetUniformLocation(program, "levelMiddle")
        maxLocation = GLES20.glGetUniformLocation(program, "levelMaximum")
        minOutputLocation = GLES20.glGetUniformLocation(program, "minOutput")
        maxOutputLocation = GLES20.glGetUniformLocation(program, "maxOutput")
    }

    override fun onInitialized() {
        super.onInitialized()
        setMin(0.0f, 1.0f, 1.0f, 0.0f, 1.0f)
        updateUniforms()
    }


    fun updateUniforms() {
        setFloatVec3(minLocation, min)
        setFloatVec3(midLocation, mid)
        setFloatVec3(maxLocation, max)
        setFloatVec3(minOutputLocation, minOutput)
        setFloatVec3(maxOutputLocation, maxOutput)
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
