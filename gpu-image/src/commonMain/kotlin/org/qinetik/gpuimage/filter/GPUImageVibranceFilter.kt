package org.qinetik.gpuimage.filter

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

class GPUImageVibranceFilter : GPUImageFilter {

    companion object {
        const val VIBRANCE_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform lowp float vibrance;\n" +
                "\n" +
                "void main() {\n" +
                "    lowp vec4 color = texture2D(inputImageTexture, textureCoordinate);\n" +
                "    lowp float average = (color.r + color.g + color.b) / 3.0;\n" +
                "    lowp float mx = max(color.r, max(color.g, color.b));\n" +
                "    lowp float amt = (mx - average) * (-vibrance * 3.0);\n" +
                "    color.rgb = mix(color.rgb, vec3(mx), amt);\n" +
                "    gl_FragColor = color;\n" +
                "}"
    }


    private var _vibranceLocation: UniformLocation? = null

    private inline var vibranceLocation: UniformLocation
        get() = _vibranceLocation!!
        set(value){
            _vibranceLocation = value
        }

    private var vibrance: Float = 0f

    override fun onInit() {
        super.onInit()
        _vibranceLocation = Kgl.getUniformLocation(program, "vibrance")
    }

    constructor() : this(0f)

    constructor(vibrance: Float) : super(NO_FILTER_VERTEX_SHADER, VIBRANCE_FRAGMENT_SHADER) {
        this.vibrance = vibrance
    }

    override fun onInitialized() {
        super.onInitialized()
        setVibrance(vibrance)
    }

    fun setVibrance(vibrance: Float) {
        this.vibrance = vibrance
        if (isInitialized) {
            setFloat(vibranceLocation, vibrance)
        }
    }
}

