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

package org.qinetik.gpuimage.filter;

import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter
import org.qinetik.gpuimage.utils.FloatPoint

public class GPUImageBulgeDistortionFilter(
    private var radius: Float,
    private var scale: Float,
    private var center: FloatPoint
) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, BULGE_FRAGMENT_SHADER) {
    companion object {
        const val BULGE_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "\n" +
                "uniform highp float aspectRatio;\n" +
                "uniform highp vec2 center;\n" +
                "uniform highp float radius;\n" +
                "uniform highp float scale;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "highp vec2 textureCoordinateToUse = vec2(textureCoordinate.x, (textureCoordinate.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                "highp float dist = distance(center, textureCoordinateToUse);\n" +
                "textureCoordinateToUse = textureCoordinate;\n" +
                "\n" +
                "if (dist < radius)\n" +
                "{\n" +
                "textureCoordinateToUse -= center;\n" +
                "highp float percent = 1.0 - ((radius - dist) / radius) * scale;\n" +
                "percent = percent * percent;\n" +
                "\n" +
                "textureCoordinateToUse = textureCoordinateToUse * percent;\n" +
                "textureCoordinateToUse += center;\n" +
                "}\n" +
                "\n" +
                "gl_FragColor = texture2D(inputImageTexture, textureCoordinateToUse );    \n" +
                "}\n";
    }


    private var _scaleLocation: UniformLocation? = null
    private var _radiusLocation: UniformLocation? = null
    private var _centerLocation: UniformLocation? = null
    private var aspectRatio: Float = 0f
    private var _aspectRatioLocation: UniformLocation? = null

    private inline var scaleLocation: UniformLocation
        get() = _scaleLocation!!
        set(value) {
            _scaleLocation = value
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
    private inline var aspectRatioLocation: UniformLocation
        get() = _aspectRatioLocation!!
        set(value) {
            _aspectRatioLocation = value
        }

    constructor() : this(0.25f, 0.5f, FloatPoint(0.5f, 0.5f))

    override fun onInit() {
        super.onInit();
        _scaleLocation = Kgl.getUniformLocation(program, "scale")
        _radiusLocation = Kgl.getUniformLocation(program, "radius")
        _centerLocation = Kgl.getUniformLocation(program, "center")
        _aspectRatioLocation = Kgl.getUniformLocation(program, "aspectRatio")
    }

    override fun onInitialized() {
        super.onInitialized();
        setAspectRatio(aspectRatio);
        setRadius(radius);
        setScale(scale);
        setCenter(center);
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        aspectRatio = height.toFloat() / width.toFloat()
        setAspectRatio(aspectRatio);
        super.onOutputSizeChanged(width, height);
    }

    private fun setAspectRatio(aspectRatio: Float) {
        this.aspectRatio = aspectRatio;
        if(_aspectRatioLocation != null) setFloat(aspectRatioLocation, aspectRatio);
    }

    /**
     * The radius of the distortion, ranging from 0.0 to 1.0, with a default of 0.25
     *
     * @param radius from 0.0 to 1.0, default 0.25
     */
    public fun setRadius(radius: Float) {
        this.radius = radius;
        if(_radiusLocation != null) setFloat(radiusLocation, radius);
    }

    /**
     * The amount of distortion to apply, from -1.0 to 1.0, with a default of 0.5
     *
     * @param scale from -1.0 to 1.0, default 0.5
     */
    public fun setScale(scale: Float) {
        this.scale = scale;
        if(_scaleLocation != null) setFloat(scaleLocation, scale);
    }

    /**
     * The center about which to apply the distortion, with a default of (0.5, 0.5)
     *
     * @param center default (0.5, 0.5)
     */
    public fun setCenter(center: FloatPoint) {
        this.center = center;
        if(_centerLocation != null) setPoint(centerLocation, center.x, center.y);
    }
}
