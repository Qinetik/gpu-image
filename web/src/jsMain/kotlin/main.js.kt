import com.danielgergely.kgl.*
import kotlinx.browser.document
import org.jetbrains.skiko.wasm.onWasmReady
import org.khronos.webgl.WebGLRenderingContext
import org.qinetik.gpuimage.Kgl
import org.qinetik.gpuimage.setKglJsInstance
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import org.w3c.dom.get

fun main() {
//    onWasmReady {
//        BrowserViewportWindow {
//            Screen()
//        }
//    }
    val composeTarget = document.getElementsByTagName("canvas")
    if(composeTarget.length == 0){
        console.error("No Canvas element found")
    }
    val canvas = composeTarget[0] as HTMLCanvasElement
    val gl = canvas.getContext("webgl") as WebGLRenderingContext
    gl.viewport(0, 0, canvas.width, canvas.height)
    setKglJsInstance(KglJs(gl))

    // Vertex Shader
    val vertexShader = Kgl.createShader(GL_VERTEX_SHADER)!!
    Kgl.shaderSource(vertexShader, NO_FILTER_VERTEX_SHADER)
    Kgl.compileShader(vertexShader)

    // Fragment Shader
    val fragmentShader = Kgl.createShader(GL_FRAGMENT_SHADER)!!
    Kgl.shaderSource(fragmentShader, NO_FILTER_FRAGMENT_SHADER)
    Kgl.compileShader(fragmentShader)


    // Create a shader program and link the shaders
    val shaderProgram = Kgl.createProgram()!!
    Kgl.attachShader(shaderProgram, vertexShader)
    Kgl.attachShader(shaderProgram, fragmentShader)
    Kgl.linkProgram(shaderProgram)

    // Use the shader program
    Kgl.useProgram(shaderProgram)

    // Load an image as a texture
    val image = Image() // Replace this with your image loading logic
    image.src = "download.jpg"

//    val imgElement = document.createElement("img")
//    imgElement.setAttribute("src", "download.jpg")
//    imgElement.setAttribute("width", "200")
//    imgElement.setAttribute("height", "400")
//    document.body!!.appendChild(imgElement)


    val texture = Kgl.createTexture()
    Kgl.bindTexture(GL_TEXTURE_2D, texture)
    val textureResource = TextureResource(image)

    image.onload = {
        console.log("Image loaded successfully")
        // Bind the texture and set its parameters
        Kgl.bindTexture(GL_TEXTURE_2D, texture)
        // TODO check this call
//        Kgl.texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE, image)
        Kgl.texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, textureResource)
        Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        Kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        // Configure the shader program attributes
        val positionLocation = Kgl.getAttribLocation(shaderProgram, "position")
        val inputTextureCoordinateLocation = Kgl.getAttribLocation(shaderProgram, "inputTextureCoordinate")

        // Create and bind a buffer for the vertices
        val vertexBuffer = Kgl.createBuffer()
        Kgl.bindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
        val vertices = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )
        // TODO check this call
//        Kgl.bufferData(GL_ARRAY_BUFFER, FloatBuffer(vertices), GL_STATIC_DRAW)
        Kgl.bufferData(GL_ARRAY_BUFFER, FloatBuffer(vertices), vertices.size * 4, GL_STATIC_DRAW)

        // Set up attribute pointers
        Kgl.enableVertexAttribArray(positionLocation)
        Kgl.vertexAttribPointer(positionLocation, 2, GL_FLOAT, false, 0, 0)

        // Bind a buffer for texture coordinates
        val textureCoordBuffer = Kgl.createBuffer()
        Kgl.bindBuffer(GL_ARRAY_BUFFER, textureCoordBuffer)
        val textureCoordinates = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
        // TODO check this call
//        Kgl.bufferData(GL_ARRAY_BUFFER, FloatBuffer(textureCoordinates), GL_STATIC_DRAW)
        Kgl.bufferData(GL_ARRAY_BUFFER, FloatBuffer(textureCoordinates), textureCoordinates.size * 4, GL_STATIC_DRAW)

        // Set up attribute pointers for texture coordinates
        Kgl.enableVertexAttribArray(inputTextureCoordinateLocation)
        Kgl.vertexAttribPointer(inputTextureCoordinateLocation, 2, GL_FLOAT, false, 0, 0)

        // Render the full-screen quad
        Kgl.drawArrays(GL_TRIANGLE_STRIP, 0, 4)

        val error = gl.getError()
        if (error != GL_NO_ERROR) {
            console.error("WebGL error: $error")
        }
    }

    val error = gl.getError()
    if (error != GL_NO_ERROR) {
        console.error("WebGL error: $error")
    }


}


const val NO_FILTER_VERTEX_SHADER : String = "" +
        "attribute vec4 position;\n" +
        "attribute vec4 inputTextureCoordinate;\n" +
        " \n" +
        "varying vec2 textureCoordinate;\n" +
        " \n" +
        "void main()\n" +
        "{\n" +
        "    gl_Position = position;\n" +
        "    textureCoordinate = inputTextureCoordinate.xy;\n" +
        "}";
const val NO_FILTER_FRAGMENT_SHADER : String = "" +
        "varying highp vec2 textureCoordinate;\n" +
        " \n" +
        "uniform sampler2D inputImageTexture;\n" +
        " \n" +
        "void main()\n" +
        "{\n" +
        "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
        "}";
