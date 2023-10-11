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
import com.danielgergely.kgl.UniformLocation
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter

class GPUImageSphereRefractionFilter : GPUImageFilter {
    companion object {
        const val SPHERE_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "\n" +
                "uniform highp vec2 center;\n" +
                "uniform highp float radius;\n" +
                "uniform highp float aspectRatio;\n" +
                "uniform highp float refractiveIndex;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "highp vec2 textureCoordinateToUse = vec2(textureCoordinate.x, (textureCoordinate.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                "highp float distanceFromCenter = distance(center, textureCoordinateToUse);\n" +
                "lowp float checkForPresenceWithinSphere = step(distanceFromCenter, radius);\n" +
                "\n" +
                "distanceFromCenter = distanceFromCenter / radius;\n" +
                "\n" +
                "highp float normalizedDepth = radius * sqrt(1.0 - distanceFromCenter * distanceFromCenter);\n" +
                "highp vec3 sphereNormal = normalize(vec3(textureCoordinateToUse - center, normalizedDepth));\n" +
                "\n" +
                "highp vec3 refractedVector = refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);\n" +
                "\n" +
                "gl_FragColor = texture2D(inputImageTexture, (refractedVector.xy + 1.0) * 0.5) * checkForPresenceWithinSphere;     \n" +
                "}\n"
    }

    private var _centerLocation: UniformLocation? = null
    private var _radiusLocation: UniformLocation? = null
    private var _aspectRatioLocation: UniformLocation? = null
    private var _refractiveIndexLocation: UniformLocation? = null

    private inline var centerLocation : UniformLocation
        get() = _centerLocation!!
        set(value){
            _centerLocation = value
        }
    private inline var radiusLocation : UniformLocation
        get() = _radiusLocation!!
        set(value){
            _radiusLocation = value
        }
    private inline var aspectRatioLocation : UniformLocation
        get() = _aspectRatioLocation!!
        set(value){
            _aspectRatioLocation = value
        }
    private inline var refractiveIndexLocation : UniformLocation
        get() = _refractiveIndexLocation!!
        set(value){
            _refractiveIndexLocation = value
        }

    private var center: PointF
    private var radius: Float
    private var aspectRatio: Float = 0.0f
    private var refractiveIndex: Float


    constructor() : this(PointF(0.5f, 0.5f), 0.25f, 0.71f)

    constructor(center: PointF, radius: Float, refractiveIndex: Float) : super(
        NO_FILTER_VERTEX_SHADER,
        SPHERE_FRAGMENT_SHADER
    ) {
        this.center = center
        this.radius = radius
        this.refractiveIndex = refractiveIndex
    }

    override fun onInit() {
        super.onInit()
        _centerLocation = Kgl.getUniformLocation(program, "center")
        _radiusLocation = Kgl.getUniformLocation(program, "radius")
        _aspectRatioLocation = Kgl.getUniformLocation(program, "aspectRatio")
        _refractiveIndexLocation = Kgl.getUniformLocation(program, "refractiveIndex")
    }

    override fun onInitialized() {
        super.onInitialized()
        setAspectRatio(aspectRatio)
        setRadius(radius)
        setCenter(center)
        setRefractiveIndex(refractiveIndex)
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        aspectRatio = height.toFloat() / width.toFloat()
        setAspectRatio(aspectRatio)
        super.onOutputSizeChanged(width, height)
    }

    private fun setAspectRatio(aspectRatio: Float) {
        this.aspectRatio = aspectRatio
        if(_aspectRatioLocation != null) {
            setFloat(aspectRatioLocation, aspectRatio)
        }
    }

    /**
     * The index of refraction for the sphere, with a default of 0.71
     *
     * @param refractiveIndex default 0.71
     */
    fun setRefractiveIndex(refractiveIndex: Float) {
        this.refractiveIndex = refractiveIndex
        if(_refractiveIndexLocation != null) {
            setFloat(refractiveIndexLocation, refractiveIndex)
        }
    }

    /**
     * The center about which to apply the distortion, with a default of (0.5, 0.5)
     *
     * @param center default (0.5, 0.5)
     */
    fun setCenter(center: PointF) {
        this.center = center
        if(_centerLocation != null) {
            setPoint(centerLocation, center.x, center.y)
        }
    }

    /**
     * The radius of the distortion, ranging from 0.0 to 1.0, with a default of 0.25
     *
     * @param radius from 0.0 to 1.0, default 0.25
     */
    fun setRadius(radius: Float) {
        this.radius = radius
        if(_radiusLocation != null) {
            setFloat(radiusLocation, radius)
        }
    }

}
