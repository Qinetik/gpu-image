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

class GPUImageGlassSphereFilter : GPUImageFilter {
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
                "// uniform vec3 lightPosition;\n" +
                "const highp vec3 lightPosition = vec3(-0.5, 0.5, 1.0);\n" +
                "const highp vec3 ambientLightPosition = vec3(0.0, 0.0, 1.0);\n" +
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
                "highp vec3 refractedVector = 2.0 * refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);\n" +
                "refractedVector.xy = -refractedVector.xy;\n" +
                "\n" +
                "highp vec3 finalSphereColor = texture2D(inputImageTexture, (refractedVector.xy + 1.0) * 0.5).rgb;\n" +
                "\n" +
                "// Grazing angle lighting\n" +
                "highp float lightingIntensity = 2.5 * (1.0 - pow(clamp(dot(ambientLightPosition, sphereNormal), 0.0, 1.0), 0.25));\n" +
                "finalSphereColor += lightingIntensity;\n" +
                "\n" +
                "// Specular lighting\n" +
                "lightingIntensity  = clamp(dot(normalize(lightPosition), sphereNormal), 0.0, 1.0);\n" +
                "lightingIntensity  = pow(lightingIntensity, 15.0);\n" +
                "finalSphereColor += vec3(0.8, 0.8, 0.8) * lightingIntensity;\n" +
                "\n" +
                "gl_FragColor = vec4(finalSphereColor, 1.0) * checkForPresenceWithinSphere;\n" +
                "}\n"
    }

    private var center: PointF
    private var centerLocation: Int = 0
    private var radius: Float = 0f
    private var radiusLocation: Int = 0
    private var aspectRatio: Float = 0f
    private var aspectRatioLocation: Int = 0
    private var refractiveIndex: Float = 0f
    private var refractiveIndexLocation: Int = 0

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
        centerLocation = GLES20.glGetUniformLocation(program, "center")
        radiusLocation = GLES20.glGetUniformLocation(program, "radius")
        aspectRatioLocation = GLES20.glGetUniformLocation(program, "aspectRatio")
        refractiveIndexLocation = GLES20.glGetUniformLocation(program, "refractiveIndex")
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
        setFloat(aspectRatioLocation, aspectRatio)
    }

    fun setRefractiveIndex(refractiveIndex: Float) {
        this.refractiveIndex = refractiveIndex
        setFloat(refractiveIndexLocation, refractiveIndex)
    }

    fun setCenter(center: PointF) {
        this.center = center
        setPoint(centerLocation, center.x, center.y)
    }

    fun setRadius(radius: Float) {
        this.radius = radius
        setFloat(radiusLocation, radius)
    }

}
