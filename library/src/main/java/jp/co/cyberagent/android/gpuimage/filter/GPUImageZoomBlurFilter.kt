package jp.co.cyberagent.android.gpuimage.filter

import android.graphics.PointF
import android.opengl.GLES20

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


    private var blurCenter: PointF
    private var blurCenterLocation: Int = 0
    private var blurSize: Float
    private var blurSizeLocation: Int = 0

    constructor() : this(PointF(0.5f, 0.5f), 1.0f)

    constructor(blurCenter: PointF, blurSize: Float) : super(
        NO_FILTER_VERTEX_SHADER,
        ZOOM_BLUR_FRAGMENT_SHADER
    ) {
        this.blurCenter = blurCenter
        this.blurSize = blurSize
    }

    override fun onInit() {
        super.onInit()
        blurCenterLocation = GLES20.glGetUniformLocation(program, "blurCenter")
        blurSizeLocation = GLES20.glGetUniformLocation(program, "blurSize")
    }

    override fun onInitialized() {
        super.onInitialized()
        setBlurCenter(blurCenter)
        setBlurSize(blurSize)
    }

    fun setBlurCenter(blurCenter: PointF) {
        this.blurCenter = blurCenter
        setPoint(blurCenterLocation, blurCenter.x, blurCenter.y)
    }

    fun setBlurSize(blurSize: Float) {
        this.blurSize = blurSize
        setFloat(blurSizeLocation, blurSize)
    }

}
