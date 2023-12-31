// From Slack by OliverO
// See: https://kotlinlang.slack.com/archives/C01F2HV7868/p1660083429206369?thread_ts=1660083398.571449&cid=C01F2HV7868

@file:Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "EXPOSED_PARAMETER_TYPE"
) // WORKAROUND: ComposeWindow and ComposeLayer are internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeWindow
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTitleElement

private const val CANVAS_ELEMENT_ID = "ComposeTarget" // Hardwired into ComposeWindow

/**
 * A Skiko/Canvas-based top-level window using the browser's entire viewport. Supports resizing.
 */
fun BrowserViewportWindow(content: @Composable ComposeWindow.() -> Unit) {

    document.getElementById("LoadingScreen")?.setAttribute("style", "display:none;")

    fun HTMLCanvasElement.fillViewportSize() {
        setAttribute("width", "${window.innerWidth}")
        setAttribute("height", "${window.innerHeight}")
    }

    val canvas = (document.getElementById(CANVAS_ELEMENT_ID) as HTMLCanvasElement).apply {
        fillViewportSize()
    }

    ComposeWindow().apply {
        window.addEventListener("resize", {
            canvas.fillViewportSize()
            layer.layer.attachTo(canvas)
            val scale = 1f // layer.layer.contentScale
            layer.setSize((canvas.width / scale).toInt(), (canvas.height / scale).toInt())
            layer.layer.needRedraw()
        })
        setContent {
            content(this)
        }
    }
}