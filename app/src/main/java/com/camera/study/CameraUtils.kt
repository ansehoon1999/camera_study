package com.camera.study

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import java.nio.file.Files.size


class CameraUtils {

    lateinit var connectManager: CameraManager

    var openCamera: CameraDevice? = null

    lateinit var cameraThread: WorkerThread

    constructor(context: Context, facing: Int) {
        this.cameraThread = WorkerThread("camera")
        this.cameraThread.start()
        initializeCamera(context = context, facing)
    }

    fun initializeCamera(context: Context, facing: Int) {
        try {
            connectManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val allIds = connectManager.cameraIdList

            for (id in allIds) {
                val params = connectManager.getCameraCharacteristics(id)
                connectManager.getCameraCharacteristics(id)

                if (params[CameraCharacteristics.LENS_FACING] == facing) {
                    openCamera(id!!)
                    break
                }


            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(id: String) {

        connectManager.openCamera(id, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {

                Log.d("openCamera", "onOpened: ${camera}")
                openCamera = camera
            }

            override fun onDisconnected(camera: CameraDevice) {
                openCamera?.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, null)

    }

    fun startPreviewOnTexture(texture: SurfaceTexture?) {

        Log.d("texture", texture.toString())
        texture ?: return

        try {
            Log.d("openCamera", openCamera.toString())

            openCamera?.let {

                val previewRequestBuilder =
                    it.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                texture.setDefaultBufferSize(1280, 720)
                val previewSurface = Surface(texture)
                previewRequestBuilder.addTarget(previewSurface)
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(0, 30))
                it.createCaptureSession(
                    listOf(
                        previewSurface
                    ),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if(openCamera == null) return

                            try {
                                previewRequestBuilder.set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )
                                val previewRequest = previewRequestBuilder.build()
                                session.setRepeatingRequest(
                                    previewRequest,
                                    null, cameraThread.getHandler()
                                )

                                Log.d("onConfigured", openCamera.toString())
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.d("onConfigured", "onConfigureFailed")

                        }
                    },
                    cameraThread.getHandler()
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun stopCamera() {
        openCamera?.close()
        cameraThread.release()
    }
}


class WorkerThread : HandlerThread {

    private var workHandler: android.os.Handler? = null

    constructor(name: String?) : super(name)

    override fun start() {
        super.start()

        workHandler = android.os.Handler(looper)

    }

    fun getHandler(): android.os.Handler? {
        return workHandler
    }

    fun release() {
        quit()
        workHandler = null
    }

}
