package ua.com.radiokot.photoprism.features.viewer.view

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import okio.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.M)
class PhotosphereRenderer(
    private val equirectBitmap: Bitmap,
) : GLSurfaceView.Renderer,
    Closeable {

    /**
     * Positive to look up, negative to look down, up to 90 degrees.
     */
    var pitchDegrees = 0f
        set(value) {
            if (field != value) {
                field = value
                viewMatrixNeedsUpdate = true
            }
        }

    /**
     * 0 to look at the image center.
     */
    var yawDegrees = 0f
        set(value) {
            if (field != value) {
                field = value
                viewMatrixNeedsUpdate = true
            }
        }

    /**
     * The smaller the field of view, the greater the zoom.
     */
    var fovDegrees = 75f
        set(value) {
            if (field != value) {
                field = value
                projectionMatrixNeedsUpdate = true
            }
        }

    private val vertexShaderCode = """
        uniform mat4 uProjectionMatrix;
        uniform mat4 uViewMatrix;
        
        attribute vec4 aPosition;
        attribute vec2 aTextureCoordinate;
        
        varying vec2 vTextureCoordinate;
        
        void main() {
            gl_Position = uProjectionMatrix * uViewMatrix * aPosition;
            vTextureCoordinate = aTextureCoordinate;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        
        uniform samplerExternalOES uExternalTexture;
        
        varying vec2 vTextureCoordinate;
        
        void main() {
            gl_FragColor = texture2D(uExternalTexture, vTextureCoordinate);
        }
    """.trimIndent()

    private var aPositionLocation = 0
    private var aTextureCoordinateLocation = 0
    private var uProjectionMatrixLocation = 0
    private var uViewMatrixLocation = 0
    private var uExternalTextureLocation = 0

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceTextureNeedsUpdate = false
    private var surfaceTexture: SurfaceTexture? = null
    private var sphereIndexCount = 0
    private var viewMatrixNeedsUpdate = true
    private var projectionMatrixNeedsUpdate = true

    override fun onSurfaceCreated(
        gl: GL10,
        config: EGLConfig,
    ) {
        loadProgram()
        loadSphereModel()
        loadTexture()

        projectionMatrixNeedsUpdate = true
        viewMatrixNeedsUpdate = true
    }

    override fun onSurfaceChanged(
        gl: GL10,
        width: Int,
        height: Int,
    ) {
        GLES20.glViewport(0, 0, width, height)

        surfaceWidth = width
        surfaceHeight = height
        projectionMatrixNeedsUpdate = true
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (surfaceTextureNeedsUpdate) {
            surfaceTexture!!.updateTexImage()
            surfaceTextureNeedsUpdate = false
        }

        if (viewMatrixNeedsUpdate) {
            updateViewMatrix()
            viewMatrixNeedsUpdate = false
        }

        if (projectionMatrixNeedsUpdate) {
            updateProjectionMatrix()
            viewMatrixNeedsUpdate = false
        }

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            sphereIndexCount,
            GLES20.GL_UNSIGNED_SHORT,
            0,
        )
    }

    private fun loadProgram() {
        val programId = GLES20.glCreateProgram()

        GLES20.glAttachShader(
            programId,
            loadShader(
                type = GLES20.GL_VERTEX_SHADER,
                code = vertexShaderCode,
            ),
        )
        GLES20.glAttachShader(
            programId,
            loadShader(
                type = GLES20.GL_FRAGMENT_SHADER,
                code = fragmentShaderCode,
            ),
        )
        GLES20.glLinkProgram(programId)

        aPositionLocation =
            GLES20.glGetAttribLocation(programId, "aPosition")
        aTextureCoordinateLocation =
            GLES20.glGetAttribLocation(programId, "aTextureCoordinate")
        uProjectionMatrixLocation =
            GLES20.glGetUniformLocation(programId, "uProjectionMatrix")
        uViewMatrixLocation =
            GLES20.glGetUniformLocation(programId, "uViewMatrix")
        uExternalTextureLocation =
            GLES20.glGetUniformLocation(programId, "uExternalTexture")

        GLES20.glUseProgram(programId)
    }

    private fun loadShader(
        type: Int,
        code: String,
    ): Int {
        val shaderId = GLES20.glCreateShader(type)

        GLES20.glShaderSource(shaderId, code)
        GLES20.glCompileShader(shaderId)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(
            shaderId,
            GLES20.GL_COMPILE_STATUS,
            compileStatus,
            0,
        )

        if (compileStatus[0] == 0) {
            val infoLog = GLES20.glGetShaderInfoLog(shaderId)
            GLES20.glDeleteShader(shaderId)
            error("Shader compilation failed (type $type):\n$infoLog")
        }

        return shaderId
    }

    private fun loadSphereModel() {
        val stackCount = 32
        val sliceCount = 32
        val vertexCount = (stackCount + 1) * (sliceCount + 1)

        val vertexBuffer =
            ByteBuffer
                .allocateDirect(vertexCount * 3 * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

        val textureCoordinateBuffer =
            ByteBuffer
                .allocateDirect(vertexCount * 2 * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

        val pi = Math.PI.toFloat()

        for (i in 0..stackCount) {
            val lat = pi * (i.toFloat() / stackCount - 0.5f)
            val sinLat = sin(lat)
            val cosLat = cos(lat)

            for (j in 0..sliceCount) {
                val lng = 2.0f * pi * j / sliceCount
                val sinLon = sin(lng)
                val cosLon = cos(lng)

                vertexBuffer.put(RADIUS * cosLat * sinLon)
                vertexBuffer.put(RADIUS * sinLat)
                vertexBuffer.put(RADIUS * cosLat * cosLon)

                textureCoordinateBuffer.put(1.0f - (j.toFloat() / sliceCount))
                textureCoordinateBuffer.put(1.0f - (i.toFloat() / stackCount))
            }
        }

        sphereIndexCount = stackCount * sliceCount * 6

        val indexBuffer =
            ByteBuffer
                .allocateDirect(sphereIndexCount * Short.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()

        for (i in 0 until stackCount) {
            for (j in 0 until sliceCount) {
                val first = (i * (sliceCount + 1) + j).toShort()
                val second = (first + sliceCount + 1).toShort()

                indexBuffer.put(first)
                indexBuffer.put(second)
                indexBuffer.put((first + 1).toShort())

                indexBuffer.put(second)
                indexBuffer.put((second + 1).toShort())
                indexBuffer.put((first + 1).toShort())
            }
        }

        val bufferIds = IntArray(3)
        GLES20.glGenBuffers(3, bufferIds, 0)

        vertexBuffer.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIds[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexBuffer.capacity() * Float.SIZE_BYTES,
            vertexBuffer,
            GLES20.GL_STATIC_DRAW,
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)
        GLES20.glVertexAttribPointer(
            aPositionLocation,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            0,
        )

        textureCoordinateBuffer.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIds[1])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            textureCoordinateBuffer.capacity() * Float.SIZE_BYTES,
            textureCoordinateBuffer,
            GLES20.GL_STATIC_DRAW,
        )
        GLES20.glEnableVertexAttribArray(aTextureCoordinateLocation)
        GLES20.glVertexAttribPointer(
            aTextureCoordinateLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            0,
        )

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferIds[2])
        indexBuffer.position(0)
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            indexBuffer.capacity() * Short.SIZE_BYTES,
            indexBuffer,
            GLES20.GL_STATIC_DRAW,
        )
    }

    private fun loadTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(uExternalTextureLocation, 0)

        val texturesIds = IntArray(1)
        GLES20.glGenTextures(1, texturesIds, 0)

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )

        surfaceTexture = SurfaceTexture(texturesIds[0])
        surfaceTextureNeedsUpdate = true

        var textureWidth = equirectBitmap.width
        var textureHeight = equirectBitmap.height
        if (textureHeight < textureWidth / 2) {
            textureHeight = textureWidth / 2
        } else if (textureWidth < textureHeight * 2) {
            textureWidth = textureHeight * 2
        }

        surfaceTexture!!.setDefaultBufferSize(textureWidth, textureHeight)

        val surface = Surface(surfaceTexture)
        with(surface.lockHardwareCanvas()) {
            try {
                drawColor(Color.DKGRAY)
                drawBitmap(
                    equirectBitmap,
                    (textureWidth - equirectBitmap.width) / 2f,
                    (textureHeight - equirectBitmap.height) / 2f,
                    Paint()
                )
            } finally {
                surface.unlockCanvasAndPost(this)
                surface.release()
            }
        }
    }

    private fun updateProjectionMatrix() {
        if (uProjectionMatrixLocation == 0 || surfaceWidth == 0 || surfaceHeight == 0) {
            return
        }

        // Projection matrix – camera properties.

        val projectionMatrix = FloatArray(16)
        val aspectRatio = surfaceWidth.toFloat() / surfaceHeight
        Matrix.perspectiveM(
            projectionMatrix,
            0,
            fovDegrees,
            aspectRatio,
            0.1f,
            RADIUS + 1f,
        )
        GLES20.glUniformMatrix4fv(
            uProjectionMatrixLocation,
            1,
            false,
            projectionMatrix,
            0,
        )
    }

    private fun updateViewMatrix() {
        if (uViewMatrixLocation == 0) {
            return
        }

        // View matrix – camera position.

        val viewMatrix = FloatArray(16)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.rotateM(viewMatrix, 0, pitchDegrees, 1f, 0f, 0f)
        Matrix.rotateM(viewMatrix, 0, yawDegrees, 0f, 1f, 0f)
        GLES20.glUniformMatrix4fv(
            uViewMatrixLocation,
            1,
            false,
            viewMatrix,
            0,
        )
    }

    override fun close() {
        surfaceTexture?.release()
    }

    private companion object {
        private const val RADIUS = 50f
    }
}
