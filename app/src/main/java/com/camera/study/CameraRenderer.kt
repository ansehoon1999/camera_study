package com.camera.study

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer : GLSurfaceView.Renderer {
    private var context: Context? = null


    private var surfaceTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null


    private var programHandle = 0
    private var vertexHandle = 0
    private var fragmentHandle = 0


    private var vertexPositionHandle = 0
    private var vertexMatrixHandle = 0
    private var textureOESHandle = 0
    private var vertexCoordinateHandle = 0

    private var surfaceView: GLSurfaceView? = null

    private var vertexBuffer: FloatBuffer? = null
    private val vertex_coords = floatArrayOf(
        1f, 1f,
        -1f, 1f,
        -1f, -1f,
        1f, 1f,
        -1f, -1f,
        1f, -1f
    )

    private var vertexOrderBuffer: FloatBuffer? = null
    private val vertex_coords_order = floatArrayOf(
        1f, 1f,
        0f, 1f,
        0f, 0f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )


    private val transformMatrix = FloatArray(16)

    constructor(context: Context, surface: GLSurfaceView, cameraUtils: CameraUtils) {
        this.context = context
        this.surfaceView = surface
        this.cameraUtils = cameraUtils
    }

    private var cameraUtils: CameraUtils? = null


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        createSurfaceTexture()

        Thread.sleep(1000L)

        cameraUtils?.startPreviewOnTexture(surfaceTexture)

        vertexBuffer = ByteBuffer.allocateDirect(vertex_coords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        vertexBuffer?.put(vertex_coords)?.position(0)

        vertexOrderBuffer = ByteBuffer.allocateDirect(vertex_coords_order.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertex_coords_order)

        vertexOrderBuffer?.position(0)

        programHandle = GLES20.glCreateProgram()
        vertexHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)

        val vertexShader = Utils.readShaderFromResource(context, R.raw.vertex_shader)
        GLES20.glShaderSource(vertexHandle, vertexShader)
        GLES20.glCompileShader(vertexHandle)
        GLES20.glAttachShader(programHandle, vertexHandle)

        fragmentHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)

        val fragmentShader = Utils.readShaderFromResource(context, R.raw.fragment_shader)
        GLES20.glShaderSource(fragmentHandle, fragmentShader)
        GLES20.glCompileShader(fragmentHandle)
        GLES20.glAttachShader(programHandle, fragmentHandle)

        GLES20.glLinkProgram(programHandle)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {

        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(transformMatrix)

        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(programHandle)

        vertexPositionHandle = GLES20.glGetAttribLocation(programHandle, "avVertex")
        vertexCoordinateHandle = GLES20.glGetAttribLocation(programHandle, "avVertexCoordinate")


        vertexMatrixHandle = GLES20.glGetUniformLocation(programHandle, "umTransformMatrix")
        textureOESHandle = GLES20.glGetUniformLocation(programHandle, "usTextureOes")


        GLES20.glVertexAttribPointer(
            vertexPositionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            8,
            vertexBuffer
        )
        GLES20.glVertexAttribPointer(
            vertexCoordinateHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            8,
            vertexOrderBuffer
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, surfaceTextureId)
        GLES20.glUniform1i(textureOESHandle, 0)

        GLES20.glUniformMatrix4fv(vertexMatrixHandle, 1, false, transformMatrix, 0)

        GLES20.glEnableVertexAttribArray(vertexPositionHandle)
        GLES20.glEnableVertexAttribArray(vertexCoordinateHandle)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        GLES20.glDisableVertexAttribArray(vertexPositionHandle)
        GLES20.glDisableVertexAttribArray(vertexCoordinateHandle)
    }


    private fun createSurfaceTexture() {
        if (surfaceTexture != null) {
            return
        }


        surfaceTextureId = Utils.createOESTextureObject()
        surfaceTexture = SurfaceTexture(surfaceTextureId)
        cameraUtils?.startPreviewOnTexture(surfaceTexture)

        surfaceTexture?.setOnFrameAvailableListener {
            surfaceView?.requestRender()

        }
    }
}
