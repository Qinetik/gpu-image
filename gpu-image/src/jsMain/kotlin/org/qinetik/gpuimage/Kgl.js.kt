package org.qinetik.gpuimage

import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.KglJs

private var _KglJsInstance : KglJs? = null

fun setKglJsInstance(kglJs : KglJs) {
    _KglJsInstance = kglJs
}

actual val Kgl : Kgl get() {
    return _KglJsInstance ?: throw IllegalStateException("You must set a kgl js instance before you can use kgl, please use method setKglJsInstance and pass in the parameter")
}