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

/**
 * The haze filter can be used to add or remove haze.
 * <p>
 * This is similar to a UV filter.
 */
class GPUImageHazeFilter(private var distance: Float, private var slope: Float) :
    GPUImageFilter(NO_FILTER_VERTEX_SHADER, HAZE_FRAGMENT_SHADER) {
    companion object {
        const val HAZE_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "\n" +
                "uniform lowp float distance;\n" +
                "uniform highp float slope;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "	//todo reconsider precision modifiers	 \n" +
                "	 highp vec4 color = vec4(1.0);//todo reimplement as a parameter\n" +
                "\n" +
                "	 highp float  d = textureCoordinate.y * slope  +  distance; \n" +
                "\n" +
                "	 highp vec4 c = texture2D(inputImageTexture, textureCoordinate) ; // consider using unpremultiply\n" +
                "\n" +
                "	 c = (c - d * color) / (1.0 -d);\n" +
                "\n" +
                "	 gl_FragColor = c; //consider using premultiply(c);\n" +
                "}\n"

    }

    private var _distanceLocation: UniformLocation? = null
    private var _slopeLocation: UniformLocation? = null

    private inline var distanceLocation: UniformLocation
        get() = _distanceLocation!!
        set(value){
            _distanceLocation = value
        }
    private inline var slopeLocation: UniformLocation
        get() = _slopeLocation!!
        set(value){
            _slopeLocation = value
        }

    constructor() : this(0.2f, 0.0f)

    override fun onInit() {
        super.onInit()
        _distanceLocation = Kgl.getUniformLocation(program, "distance")
        _slopeLocation = Kgl.getUniformLocation(program, "slope")
    }

    override fun onInitialized() {
        super.onInitialized()
        setDistance(distance)
        setSlope(slope)
    }

    /**
     * Strength of the color applied. Default 0. Values between -.3 and .3 are best.
     *
     * @param distance -0.3 to 0.3 are best, default 0
     */
    fun setDistance(distance: Float) {
        this.distance = distance
        if(_distanceLocation != null) setFloat(distanceLocation, distance)
    }

    /**
     * Amount of color change. Default 0. Values between -.3 and .3 are best.
     *
     * @param slope -0.3 to 0.3 are best, default 0
     */
    fun setSlope(slope: Float) {
        this.slope = slope
        if(_slopeLocation != null) setFloat(slopeLocation, slope)
    }

}
