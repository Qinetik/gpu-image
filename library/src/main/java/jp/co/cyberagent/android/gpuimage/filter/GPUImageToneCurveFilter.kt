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

package jp.co.cyberagent.android.gpuimage.filter;

import android.graphics.Point
import android.graphics.PointF
import com.danielgergely.kgl.*
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.filter.GPUImageFilter
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

public class GPUImageToneCurveFilter() : GPUImageFilter(NO_FILTER_VERTEX_SHADER, TONE_CURVE_FRAGMENT_SHADER) {
    companion object {
        const val TONE_CURVE_FRAGMENT_SHADER : String = "" +
        " varying highp vec2 textureCoordinate;\n" +
        " uniform sampler2D inputImageTexture;\n" +
        " uniform sampler2D toneCurveTexture;\n" +
        "\n" +
        " void main()\n" +
        " {\n" +
        "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
        "     lowp float redCurveValue = texture2D(toneCurveTexture, vec2(textureColor.r, 0.0)).r;\n" +
        "     lowp float greenCurveValue = texture2D(toneCurveTexture, vec2(textureColor.g, 0.0)).g;\n" +
        "     lowp float blueCurveValue = texture2D(toneCurveTexture, vec2(textureColor.b, 0.0)).b;\n" +
        "\n" +
        "     gl_FragColor = vec4(redCurveValue, greenCurveValue, blueCurveValue, textureColor.a);\n" +
        " }";
    }

    private var toneCurveTexture : Texture? = null
    private var _toneCurveTextureUniformLocation : UniformLocation? = null;

    private var rgbCompositeControlPoints : Array<PointF>;
    private var redControlPoints : Array<PointF>;
    private var greenControlPoints : Array<PointF>;
    private var blueControlPoints : Array<PointF>;

    private lateinit var rgbCompositeCurve : ArrayList<Float>;
    private lateinit var redCurve : ArrayList<Float>;
    private lateinit var greenCurve : ArrayList<Float>;
    private lateinit var blueCurve : ArrayList<Float>;


    init {
        val defaultCurvePoints : Array<PointF> = arrayOf(PointF(0.0f, 0.0f), PointF(0.5f, 0.5f), PointF(1.0f, 1.0f))
        rgbCompositeControlPoints = defaultCurvePoints
        redControlPoints = defaultCurvePoints
        greenControlPoints = defaultCurvePoints
        blueControlPoints = defaultCurvePoints
    }

    public override fun onInit() {
        super.onInit();
        _toneCurveTextureUniformLocation = Kgl.getUniformLocation(program, "toneCurveTexture");
        Kgl.activeTexture(GL_TEXTURE3);
        toneCurveTexture = Kgl.createTexture()
        Kgl.bindTexture(GL_TEXTURE_2D, toneCurveTexture);
        Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    public override fun onInitialized() {
        super.onInitialized();
        setRgbCompositeControlPoints(rgbCompositeControlPoints);
        setRedControlPoints(redControlPoints);
        setGreenControlPoints(greenControlPoints);
        setBlueControlPoints(blueControlPoints);
    }

    protected override fun  onDrawArraysPre() {
        if (toneCurveTexture != null) {
            Kgl.activeTexture(GL_TEXTURE3);
            Kgl.bindTexture(GL_TEXTURE_2D, toneCurveTexture);
            Kgl.uniform1i(_toneCurveTextureUniformLocation!!, 3);
        }
    }

    public fun setFromCurveFileInputStream(input : InputStream) {
        try {
            val version : Short = readShort(input);
            val totalCurves : Short = readShort(input);

            val curves : ArrayList<Array<PointF>> = ArrayList(totalCurves.toInt());
            val pointRate : Float = 1.0f / 255f;

            for (i in 0 until totalCurves){
                // 2 bytes, Count of points in the curve (short integer from 2...19)
                val pointCount : Short = readShort(input);

                val points : Array<PointF> = Array(pointCount.toInt()){ PointF() }

                // point count * 4
                // Curve points. Each curve point is a pair of short integers where
                // the first number is the output value (vertical coordinate on the
                // Curves dialog graph) and the second is the input value. All coordinates have range 0 to 255.
                for (j in 0 until pointCount){
                    val y = readShort(input);
                    val x = readShort(input);

                    points[j] = PointF(x * pointRate, y * pointRate);
                }

                curves.add(points);
            }
            input.close();

            rgbCompositeControlPoints = curves.get(0);
            redControlPoints = curves.get(1);
            greenControlPoints = curves.get(2);
            blueControlPoints = curves.get(3);
        } catch (e : IOException) {
            e.printStackTrace();
        }
    }

    @Throws(IOException::class)
    private fun readShort(input : InputStream) : Short {
        return (input.read() shl 8 or input.read()).toShort();
    }

    fun setRgbCompositeControlPoints(points : Array<PointF>) {
        rgbCompositeControlPoints = points;
        rgbCompositeCurve = createSplineCurve(rgbCompositeControlPoints)!!;
        updateToneCurveTexture();
    }

    fun setRedControlPoints(points : Array<PointF>) {
        redControlPoints = points;
        redCurve = createSplineCurve(redControlPoints)!!;
        updateToneCurveTexture();
    }

    fun setGreenControlPoints(points : Array<PointF>) {
        greenControlPoints = points;
        greenCurve = createSplineCurve(greenControlPoints)!!;
        updateToneCurveTexture();
    }

    fun setBlueControlPoints(points : Array<PointF>) {
        blueControlPoints = points;
        blueCurve = createSplineCurve(blueControlPoints)!!;
        updateToneCurveTexture();
    }

    private fun updateToneCurveTexture() {
        runOnDraw {
            Kgl.activeTexture(GL_TEXTURE3);
            Kgl.bindTexture(GL_TEXTURE_2D, toneCurveTexture);

            if ((redCurve.size >= 256) && (greenCurve.size >= 256) && (blueCurve.size >= 256) && (rgbCompositeCurve.size >= 256)) {
                val toneCurveByteArray = ByteArray(256 * 4)
                for(currentCurveIndex in 0 until 256) {
                    // BGRA for upload to texture
                    toneCurveByteArray[currentCurveIndex * 4 + 2] = (min(max(currentCurveIndex + blueCurve[currentCurveIndex] + rgbCompositeCurve[currentCurveIndex], 0f), 255f).toInt() and 0xff).toByte();
                    toneCurveByteArray[currentCurveIndex * 4 + 1] = (min(max(currentCurveIndex + greenCurve[currentCurveIndex] + rgbCompositeCurve[currentCurveIndex], 0f), 255f).toInt() and 0xff).toByte();
                    toneCurveByteArray[currentCurveIndex * 4] = (min(max(currentCurveIndex + redCurve[currentCurveIndex] + rgbCompositeCurve[currentCurveIndex], 0f), 255f).toInt() and 0xff).toByte();
                    toneCurveByteArray[currentCurveIndex * 4 + 3] = (0xff).toByte();
                }

                Kgl.texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 256 /*width*/, 1 /*height*/, 0, GL_RGBA, GL_UNSIGNED_BYTE, com.danielgergely.kgl.ByteBuffer(toneCurveByteArray));
            }
//        Buffer pixels!
//        GLES20.glTexImage2D(int target,
//            int level,
//            int internalformat,
//            int width,
//            int height,
//            int border,
//            int format,
//            int type,
//            java.nio.Buffer pixels);
        }
    }

    private fun createSplineCurve(points : Array<PointF>) : ArrayList<Float>? {

        if (points == null || points.size <= 0) {
            return null;
        }

        // Sort the array
        val pointsSorted : Array<PointF> = points.clone();
        Arrays.sort(pointsSorted) { point1, point2->
            if (point1.x < point2.x) {
                -1;
            } else if (point1.x > point2.x) {
                1;
            } else {
                0;
            }
        }

        // Convert from (0, 1) to (0, 255).
        val convertedPoints : Array<Point> = Array(pointsSorted.size) { Point() };
        for (i in points.indices){
            val point = pointsSorted[i];
            convertedPoints[i] = Point((point.x * 255).toInt(),(point.y * 255).toInt());
        }

        val splinePoints : ArrayList<Point> = createSplineCurve2(convertedPoints) ?: return null;

        // If we have a first point like (0.3, 0) we'll be missing some points at the beginning
        // that should be 0.
        val firstSplinePoint : Point = splinePoints.get(0);
        if (firstSplinePoint.x > 0) {
            var i = firstSplinePoint.x
            while(i >= 0){
                splinePoints.add(0, Point(i, 0));
                i--
            }
        }

        // Insert points similarly at the end, if necessary.
        val lastSplinePoint : Point = splinePoints.get(splinePoints.size - 1);
        if (lastSplinePoint.x < 255) {
            var i = lastSplinePoint.x + 1
            while(i <= 255){
                splinePoints.add(Point(i, 255));
                i++
            }
        }

        // Prepare the spline points.
        val preparedSplinePoints : ArrayList<Float> = ArrayList<Float>(splinePoints.size);
        for (newPoint in splinePoints) {
            val origPoint = Point(newPoint.x, newPoint.x);

            var distance = sqrt(((origPoint.x - newPoint.x).toDouble()).pow(2.0) + ((origPoint.y - newPoint.y).toDouble()).pow(2.0)).toFloat();

            if (origPoint.y > newPoint.y) {
                distance = -distance;
            }

            preparedSplinePoints.add(distance);
        }

        return preparedSplinePoints;
    }

    private fun createSplineCurve2(points : Array<Point>) : ArrayList<Point>? {
        val sdA : ArrayList<Double> = createSecondDerivative(points) ?: return null;

        // Is [points count] equal to [sdA count]?
//    int n = [points count];
        val n = sdA.size;
        if (n < 1) {
            return null;
        }
        val sd = DoubleArray(n)

        // From NSMutableArray to sd[n];
        for (i in 0 until n){
            sd[i] = sdA.get(i);
        }


        val output : ArrayList<Point> = ArrayList<Point>(n + 1);

        for (i in 0 until (n - 1)) {
            val cur = points[i];
            val next = points[i + 1];
            for( x in cur.x until next.x) {
                val t : Double = (x - cur.x).toDouble() / (next.x - cur.x).toDouble();

                val a : Double = 1 - t;
                val b : Double = t;
                val h : Double = (next.x - cur.x).toDouble();

                var y : Double = a * cur.y + b * next.y + (h * h / 6) * ((a * a * a - a) * sd[i] + (b * b * b - b) * sd[i + 1]);

                if (y > 255.0) {
                    y = 255.0;
                } else if (y < 0.0) {
                    y = 0.0;
                }

                output.add( Point(x, Math.round(y).toInt()));
            }
        }

        // If the last point is (255, 255) it doesn't get added.
        if (output.size == 255) {
            output.add(points[points.size - 1]);
        }
        return output;
    }

    private fun createSecondDerivative(points : Array<Point>) : ArrayList<Double>? {
        val n = points.size;
        if (n <= 1) {
            return null;
        }

        val matrix = Array(n) { DoubleArray(3) }
        val result = DoubleArray(n)
        matrix[0][1] = 1.0
        // What about matrix[0][1] and matrix[0][0]? Assuming 0 for now (Brad L.)
        matrix[0][0] = 0.0
        matrix[0][2] = 0.0

        for (i in 1 until n - 1) {
            val P1 = points[i - 1]
            val P2 = points[i]
            val P3 = points[i + 1]
            matrix[i][0] = (P2.x - P1.x).toDouble() / 6
            matrix[i][1] = (P3.x - P1.x).toDouble() / 3
            matrix[i][2] = (P3.x - P2.x).toDouble() / 6
            result[i] = (P3.y - P2.y).toDouble() / (P3.x - P2.x) - (P2.y - P1.y).toDouble() / (P2.x - P1.x)
        }

        // What about result[0] and result[n-1]? Assuming 0 for now (Brad L.)

        // What about result[0] and result[n-1]? Assuming 0 for now (Brad L.)
        result[0] = 0.0
        result[n - 1] = 0.0

        matrix[n - 1][1] = 1.0
        // What about matrix[n-1][0] and matrix[n-1][2]? For now, assuming they are 0 (Brad L.)
        matrix[n - 1][0] = 0.0
        matrix[n - 1][2] = 0.0

        // solving pass1 (up->down)
        for (i in 1 until n) {
            val k = matrix[i][0] / matrix[i - 1][1]
            matrix[i][1] -= k * matrix[i - 1][2]
            matrix[i][0] = 0.0
            result[i] -= k * result[i - 1]
        }
        // solving pass2 (down->up)
        for (i in n - 2 downTo 0) {
            val k = matrix[i][2] / matrix[i + 1][1]
            matrix[i][1] -= k * matrix[i + 1][0]
            matrix[i][2] = 0.0
            result[i] -= k * result[i + 1]
        }

        val output = ArrayList<Double>(n)
        for (i in 0 until n) output.add(result[i] / matrix[i][1])

        return output
    }
}
