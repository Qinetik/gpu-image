/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage.filter

import android.graphics.PointF
import android.opengl.GLES20
import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

/**
 * Performs a vignetting effect, fading out the image at the edges
 * x:
 * y: The directional intensity of the vignetting, with a default of x = 0.75, y = 0.5
 */
class GPUImageVignetteFilter : GPUImageFilter {
    companion object {
        const val VIGNETTING_FRAGMENT_SHADER: String = "" +
                " uniform sampler2D inputImageTexture;\n" +
                " varying highp vec2 textureCoordinate;\n" +
                " \n" +
                " uniform lowp vec2 vignetteCenter;\n" +
                " uniform lowp vec3 vignetteColor;\n" +
                " uniform highp float vignetteStart;\n" +
                " uniform highp float vignetteEnd;\n" +
                " \n" +
                " void main()\n" +
                " {\n" +
                "     /*\n" +
                "     lowp vec3 rgb = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
                "     lowp float d = distance(textureCoordinate, vec2(0.5,0.5));\n" +
                "     rgb *= (1.0 - smoothstep(vignetteStart, vignetteEnd, d));\n" +
                "     gl_FragColor = vec4(vec3(rgb),1.0);\n" +
                "      */\n" +
                "     \n" +
                "     lowp vec3 rgb = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
                "     lowp float d = distance(textureCoordinate, vec2(vignetteCenter.x, vignetteCenter.y));\n" +
                "     lowp float percent = smoothstep(vignetteStart, vignetteEnd, d);\n" +
                "     gl_FragColor = vec4(mix(rgb.x, vignetteColor.x, percent), mix(rgb.y, vignetteColor.y, percent), mix(rgb.z, vignetteColor.z, percent), 1.0);\n" +
                " }"
    }


    private var _vignetteCenterLocation: UniformLocation? = null
    private var _vignetteColorLocation: UniformLocation? = null
    private var _vignetteStartLocation: UniformLocation? = null
    private var _vignetteEndLocation: UniformLocation? = null

    private inline var vignetteCenterLocation: UniformLocation
        get() = _vignetteCenterLocation!!
        set(value){
            _vignetteCenterLocation = value
        }
    private inline var vignetteColorLocation: UniformLocation
        get() = _vignetteColorLocation!!
        set(value){
            _vignetteColorLocation = value
        }
    private inline var vignetteStartLocation: UniformLocation
        get() = _vignetteStartLocation!!
        set(value){
            _vignetteStartLocation = value
        }
    private inline var vignetteEndLocation: UniformLocation
        get() = _vignetteEndLocation!!
        set(value){
            _vignetteEndLocation = value
        }

    private var vignetteCenter: PointF
    private var vignetteColor: FloatArray
    private var vignetteStart: Float
    private var vignetteEnd: Float

    constructor() : this(PointF(), floatArrayOf(0.0f, 0.0f, 0.0f), 0.3f, 0.75f)

    constructor(vignetteCenter: PointF, vignetteColor: FloatArray, vignetteStart: Float, vignetteEnd: Float) : super(
        NO_FILTER_VERTEX_SHADER,
        VIGNETTING_FRAGMENT_SHADER
    ) {
        this.vignetteCenter = vignetteCenter
        this.vignetteColor = vignetteColor
        this.vignetteStart = vignetteStart
        this.vignetteEnd = vignetteEnd
    }

    override fun onInit() {
        super.onInit()
        _vignetteCenterLocation = Kgl.getUniformLocation(program, "vignetteCenter")
        _vignetteColorLocation = Kgl.getUniformLocation(program, "vignetteColor")
        _vignetteStartLocation = Kgl.getUniformLocation(program, "vignetteStart")
        _vignetteEndLocation = Kgl.getUniformLocation(program, "vignetteEnd")
    }

    override fun onInitialized() {
        super.onInitialized()
        setVignetteCenter(vignetteCenter)
        setVignetteColor(vignetteColor)
        setVignetteStart(vignetteStart)
        setVignetteEnd(vignetteEnd)
    }

    fun setVignetteCenter(vignetteCenter: PointF) {
        this.vignetteCenter = vignetteCenter
        if(_vignetteCenterLocation != null) {
            setPoint(vignetteCenterLocation, this.vignetteCenter.x, this.vignetteCenter.y)
        }
    }

    fun setVignetteColor(vignetteColor: FloatArray) {
        this.vignetteColor = vignetteColor
        if(_vignetteColorLocation != null) {
            setFloatVec3(vignetteColorLocation, this.vignetteColor)
        }
    }

    fun setVignetteStart(vignetteStart: Float) {
        this.vignetteStart = vignetteStart
        if(_vignetteStartLocation != null) {
            setFloat(vignetteStartLocation, this.vignetteStart)
        }
    }

    fun setVignetteEnd(vignetteEnd: Float) {
        this.vignetteEnd = vignetteEnd
        if(_vignetteEndLocation != null) {
            setFloat(vignetteEndLocation, this.vignetteEnd)
        }
    }
}
