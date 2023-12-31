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

package jp.co.cyberagent.android.gpuimage.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.util.Log;
import com.danielgergely.kgl.*
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.utils.OpenGlUtils

import java.nio.IntBuffer;

public object OpenGlUtils {

    public const val NO_TEXTURE : Int = -1;

    public fun loadTexture(data : IntBuffer, width : Int, height : Int, usedTexId : Texture?) : Int {
        val textures : IntArray = IntArray(1)
        if (usedTexId == null || usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat());
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat());
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat());
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat());
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                    height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
            textures[0] = usedTexId;
        }
        return textures[0];
    }

    public fun loadTextureAsBitmap(data : IntBuffer, size : Size, usedTexId : Texture?) : Int {
         val bitmap : Bitmap = Bitmap
                .createBitmap(data.array(), size.width, size.height, Config.ARGB_8888);
        val result = OpenGlUtils.loadTexture(BitmapTextureAsset(bitmap), usedTexId);
        bitmap.recycle()
        return result
    }

    public fun loadShader(strSource : String, iType : Int) : Int {
        val iShader : Int = Kgl.createShader(iType)!!;
        Kgl.shaderSource(iShader, strSource);
        Kgl.compileShader(iShader);
        // TODO this command ain't available
        val compileStatus = Kgl.getShaderParameter(iShader, GLES20.GL_COMPILE_STATUS);
        if (compileStatus == 0) {
            Log.d("Load Shader Failed", "Compilation\n" + GLES20.glGetShaderInfoLog(iShader));
            return 0;
        }
        return iShader;
    }

    public fun loadProgram(strVSource : String, strFSource : String) : Int {
        val iVShader : Int = loadShader(strVSource, GLES20.GL_VERTEX_SHADER);
        if (iVShader == 0) {
            Log.d("Load Program", "Vertex Shader Failed");
            return 0;
        }
        val iFShader : Int = loadShader(strFSource, GLES20.GL_FRAGMENT_SHADER);
        if (iFShader == 0) {
            Log.d("Load Program", "Fragment Shader Failed");
            return 0;
        }

        val iProgId : Int = Kgl.createProgram()!!;

        Kgl.attachShader(iProgId, iVShader);
        Kgl.attachShader(iProgId, iFShader);

        Kgl.linkProgram(iProgId);

        val linkStatus = Kgl.getProgramParameter(iProgId, GLES20.GL_LINK_STATUS)
        if (linkStatus <= 0) {
            Log.d("Load Program", "Linking Failed");
            return 0;
        }
        Kgl.deleteShader(iVShader);
        Kgl.deleteShader(iFShader);
        return iProgId;
    }

    public fun rnd(min : Float, max : Float) : Float {
        val fRandNum : Float = Math.random().toFloat();
        return min + (max - min) * fRandNum;
    }
}
