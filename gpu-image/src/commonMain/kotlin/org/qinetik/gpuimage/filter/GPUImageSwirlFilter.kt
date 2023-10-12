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

package org.qinetik.gpuimage.filter

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter
import org.qinetik.gpuimage.utils.FloatPoint

/**
 * Creates a swirl distortion on the image.
 */
class GPUImageSwirlFilter : GPUImageFilter {
    companion object {
        const val SWIRL_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "\n" +
                "uniform highp vec2 center;\n" +
                "uniform highp float radius;\n" +
                "uniform highp float angle;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "highp vec2 textureCoordinateToUse = textureCoordinate;\n" +
                "highp float dist = distance(center, textureCoordinate);\n" +
                "if (dist < radius)\n" +
                "{\n" +
                "textureCoordinateToUse -= center;\n" +
                "highp float percent = (radius - dist) / radius;\n" +
                "highp float theta = percent * percent * angle * 8.0;\n" +
                "highp float s = sin(theta);\n" +
                "highp float c = cos(theta);\n" +
                "textureCoordinateToUse = vec2(dot(textureCoordinateToUse, vec2(c, -s)), dot(textureCoordinateToUse, vec2(s, c)));\n" +
                "textureCoordinateToUse += center;\n" +
                "}\n" +
                "\n" +
                "gl_FragColor = texture2D(inputImageTexture, textureCoordinateToUse );\n" +
                "\n" +
                "}\n"
    }

    private var angle: Float
    private var radius: Float
    private var center: FloatPoint

    private var _angleLocation: UniformLocation? = null
    private var _radiusLocation: UniformLocation? = null
    private var _centerLocation: UniformLocation? = null

    private inline var angleLocation: UniformLocation
        get() = _angleLocation!!
        set(value) {
            _angleLocation = value
        }
    private inline var radiusLocation: UniformLocation
        get() = _radiusLocation!!
        set(value) {
            _radiusLocation = value
        }
    private inline var centerLocation: UniformLocation
        get() = _centerLocation!!
        set(value) {
            _centerLocation = value
        }

    constructor() : this(0.5f, 1.0f, FloatPoint(0.5f, 0.5f))

    constructor(radius: Float, angle: Float, center: FloatPoint) : super(NO_FILTER_VERTEX_SHADER, SWIRL_FRAGMENT_SHADER) {
        this.radius = radius
        this.angle = angle
        this.center = center
    }

    override fun onInit() {
        super.onInit()
        _angleLocation = Kgl.getUniformLocation(program, "angle")
        _radiusLocation = Kgl.getUniformLocation(program, "radius")
        _centerLocation = Kgl.getUniformLocation(program, "center")
    }

    override fun onInitialized() {
        super.onInitialized()
        setRadius(radius)
        setAngle(angle)
        setCenter(center)
    }

    /**
     * The radius of the distortion, ranging from 0.0 to 1.0, with a default of 0.5.
     *
     * @param radius from 0.0 to 1.0, default 0.5
     */
    fun setRadius(radius: Float) {
        this.radius = radius
        if(_radiusLocation != null) {
            setFloat(radiusLocation, radius)
        }
    }

    /**
     * The amount of distortion to apply, with a minimum of 0.0 and a default of 1.0.
     *
     * @param angle minimum 0.0, default 1.0
     */
    fun setAngle(angle: Float) {
        this.angle = angle
        if(_angleLocation != null) {
            setFloat(angleLocation, angle)
        }
    }

    /**
     * The center about which to apply the distortion, with a default of (0.5, 0.5).
     *
     * @param center default (0.5, 0.5)
     */
    fun setCenter(center: FloatPoint) {
        this.center = center
        if(_centerLocation != null) {
            setPoint(centerLocation, center.x, center.y)
        }
    }
}
