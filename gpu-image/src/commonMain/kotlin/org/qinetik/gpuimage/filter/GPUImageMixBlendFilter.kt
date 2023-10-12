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

open class GPUImageMixBlendFilter : GPUImageTwoInputFilter {

    private var _mixLocation: UniformLocation? = null
    private var mix: Float

    private inline var mixLocation: UniformLocation
        get() = _mixLocation!!
        set(value){
            _mixLocation = value
        }

    constructor(fragmentShader: String) : this(fragmentShader, 0.5f)

    constructor(fragmentShader: String, mix: Float) : super(fragmentShader) {
        this.mix = mix
    }

    override fun onInit() {
        super.onInit()
        _mixLocation = Kgl.getUniformLocation(program, "mixturePercent")
    }

    override fun onInitialized() {
        super.onInitialized()
        setMix(mix)
    }

    /**
     * @param mix ranges from 0.0 (only image 1) to 1.0 (only image 2), with 0.5 (half of either) as the normal level
     */
    fun setMix(mix: Float) {
        this.mix = mix
        if(_mixLocation != null) {
            setFloat(mixLocation, this.mix)
        }
    }

}
