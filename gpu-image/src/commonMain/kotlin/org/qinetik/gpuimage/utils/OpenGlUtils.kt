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

package org.qinetik.gpuimage.utils

import com.danielgergely.kgl.*
import org.qinetik.gpuimage.Kgl
import org.qinetik.logger.QLog

object OpenGlUtils {

    const val NO_TEXTURE: Int = -1

    fun loadShader(strSource: String, iType: Int): Shader? {
        val iShader = Kgl.createShader(iType) ?: return null
        Kgl.shaderSource(iShader, strSource)
        Kgl.compileShader(iShader)
        val compileStatus = Kgl.getShaderParameter(iShader, GL_COMPILE_STATUS)
        if (compileStatus == 0) {
            QLog.debug("Load Shader Failed", "Compilation\n" + Kgl.getShaderInfoLog(iShader))
            return null
        }
        return iShader
    }

    fun loadProgram(strVSource: String, strFSource: String): Program? {
        val iVShader = loadShader(strVSource, GL_VERTEX_SHADER)
        if (iVShader == null) {
            QLog.debug("Load Program", "Vertex Shader Failed")
            return null
        }
        val iFShader = loadShader(strFSource, GL_FRAGMENT_SHADER)
        if (iFShader == null) {
            QLog.debug("Load Program", "Fragment Shader Failed")
            return null
        }

        val iProgId = Kgl.createProgram()!!

        Kgl.attachShader(iProgId, iVShader)
        Kgl.attachShader(iProgId, iFShader)

        Kgl.linkProgram(iProgId)

        val linkStatus = Kgl.getProgramParameter(iProgId, GL_LINK_STATUS)
        if (linkStatus <= 0) {
            QLog.debug("Load Program", "Linking Failed")
            return null
        }
        Kgl.deleteShader(iVShader)
        Kgl.deleteShader(iFShader)
        return iProgId
    }

}
