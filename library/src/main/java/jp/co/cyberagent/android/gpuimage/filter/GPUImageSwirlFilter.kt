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
import org.qinetik.gpuimage.filter.GPUImageFilter

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
    private var angleLocation: Int = 0
    private var radius: Float
    private var radiusLocation: Int = 0
    private var center: PointF
    private var centerLocation: Int = 0

    constructor() : this(0.5f, 1.0f, PointF(0.5f, 0.5f))

    constructor(radius: Float, angle: Float, center: PointF) : super(NO_FILTER_VERTEX_SHADER, SWIRL_FRAGMENT_SHADER) {
        this.radius = radius
        this.angle = angle
        this.center = center
    }

    override fun onInit() {
        super.onInit()
        angleLocation = GLES20.glGetUniformLocation(program, "angle")
        radiusLocation = GLES20.glGetUniformLocation(program, "radius")
        centerLocation = GLES20.glGetUniformLocation(program, "center")
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
        setFloat(radiusLocation, radius)
    }

    /**
     * The amount of distortion to apply, with a minimum of 0.0 and a default of 1.0.
     *
     * @param angle minimum 0.0, default 1.0
     */
    fun setAngle(angle: Float) {
        this.angle = angle
        setFloat(angleLocation, angle)
    }

    /**
     * The center about which to apply the distortion, with a default of (0.5, 0.5).
     *
     * @param center default (0.5, 0.5)
     */
    fun setCenter(center: PointF) {
        this.center = center
        setPoint(centerLocation, center.x, center.y)
    }
}
