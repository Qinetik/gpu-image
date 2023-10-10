import com.danielgergely.kgl.GL_FALSE
import com.danielgergely.kgl.GL_TRUE
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL


fun main() {

    if (!glfwInit())
    {
        System.err.println("Error initializing GLFW");
        System.exit(1);
    }

    val windowID = glfwCreateWindow(640, 480, "My GLFW Window", 0, 0)

    glfwWindowHint(GLFW_SAMPLES, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_RESIZABLE, GL_FALSE);

    glfwMakeContextCurrent(windowID);
    GL.createCapabilities();

    start(windowID)

    glfwSwapInterval(1);

}

fun start(windowID : Long) {
    var now: Float
    var last: Float
    var delta: Float
    last = 0f

    // Initialise the Game
    init()

    // Loop continuously and render and update
    while (!glfwWindowShouldClose(windowID)) {
        // Get the time
        now = glfwGetTime().toFloat()
        delta = now - last
        last = now

        // Update and render
        update(delta)
        render(delta)

        // Poll the events and swap the buffers
        glfwPollEvents()
        glfwSwapBuffers(windowID)
    }

    // Destroy the window
    glfwDestroyWindow(windowID)
    glfwTerminate()
    System.exit(0)
}