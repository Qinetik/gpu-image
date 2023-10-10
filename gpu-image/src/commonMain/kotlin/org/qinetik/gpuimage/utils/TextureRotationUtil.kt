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

package org.qinetik.gpuimage.utils;

import kotlin.jvm.JvmStatic

public object TextureRotationUtil {

    @JvmStatic
    val TEXTURE_NO_ROTATION = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    )
    @JvmStatic
    val TEXTURE_ROTATED_90 = floatArrayOf(
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
    )
    @JvmStatic
    val TEXTURE_ROTATED_180 = floatArrayOf(
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
    )
    @JvmStatic
    val TEXTURE_ROTATED_270 = floatArrayOf(
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    )

    public fun getRotation(rotation : Rotation, flipHorizontal : Boolean, flipVertical : Boolean) : FloatArray {
        var rotatedTex : FloatArray = when (rotation) {
            Rotation.ROTATION_90 -> TEXTURE_ROTATED_90;
            Rotation.ROTATION_180 -> TEXTURE_ROTATED_180;
            Rotation.ROTATION_270 -> TEXTURE_ROTATED_270;
            Rotation.NORMAL -> TEXTURE_NO_ROTATION;
        }
        if (flipHorizontal) {
            rotatedTex = floatArrayOf(
                    flip(rotatedTex[0]), rotatedTex[1],
                    flip(rotatedTex[2]), rotatedTex[3],
                    flip(rotatedTex[4]), rotatedTex[5],
                    flip(rotatedTex[6]), rotatedTex[7],
            )
        }
        if (flipVertical) {
            rotatedTex = floatArrayOf(
                    rotatedTex[0], flip(rotatedTex[1]),
                    rotatedTex[2], flip(rotatedTex[3]),
                    rotatedTex[4], flip(rotatedTex[5]),
                    rotatedTex[6], flip(rotatedTex[7]),
            )
        }
        return rotatedTex;
    }


    private fun flip(i : Float) : Float {
        if (i == 0.0f) {
            return 1.0f;
        }
        return 0.0f;
    }
}
