package jp.co.cyberagent.android.gpuimage.filter

import android.opengl.GLES20

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

    private var uniformThresholdLocation: Int = 0
    private var threshold: Float

    constructor() : this(0.5f)

    constructor(threshold: Float) : super(NO_FILTER_VERTEX_SHADER, SOLATIZE_FRAGMENT_SHADER) {
        this.threshold = threshold
    }

    override fun onInit() {
        super.onInit()
        uniformThresholdLocation = GLES20.glGetUniformLocation(program, "threshold")
    }

    override fun onInitialized() {
        super.onInitialized()
        setThreshold(threshold)
    }

    fun setThreshold(threshold: Float) {
        this.threshold = threshold
        setFloat(uniformThresholdLocation, threshold)
    }

}
