package com.camera.study

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Environment
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat.getSystemService
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
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
//                createCameraPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                openCamera?.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, null)

    }

    private fun createCameraPreviewSession(texture: SurfaceTexture? = null) {
        texture ?: return

        try {
            texture.setDefaultBufferSize(1280, 720)
            val previewSurface = Surface(texture)
            val captureRequestBuilder =
                openCamera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(previewSurface)
            openCamera?.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        openCamera?.let {
                            val cameraCaptureSessions = session
                            captureRequestBuilder?.set(
                                CaptureRequest.CONTROL_MODE,
                                CameraMetadata.CONTROL_MODE_AUTO
                            )

                            try {
                                cameraCaptureSessions.setRepeatingRequest(
                                    captureRequestBuilder?.build() ?: return, null, null
                                )

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }

                },
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun takePicture(context: Context, surfaceTexture: SurfaceTexture) {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(openCamera!!.id)
            var jpegSizes: Array<Size>? = null
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                .getOutputSizes(ImageFormat.JPEG)

            var width = jpegSizes[0].width
            var height = jpegSizes[0].height

            val imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

            val outputSurface = ArrayList<Surface>(2)
            outputSurface.add(imageReader.surface)
            outputSurface.add(Surface(surfaceTexture))

            val captureBuilder =
                openCamera!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader.surface)

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)


            // 사진의 rotation 을 설정해준다
            var file = File(Environment.getExternalStorageDirectory().toString() + "/pic${1}.jpg")
            val readerListener = object : ImageReader.OnImageAvailableListener {
                override fun onImageAvailable(reader: ImageReader?) {
                    var image : Image? = null

                    try {
                        image = imageReader!!.acquireLatestImage()

                        val buffer = image!!.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)

                        var output: OutputStream? = null
                        try {
                            output = FileOutputStream(file)
                            output.write(bytes)
                        } finally {
                            output?.close()

                            var uri = Uri.fromFile(file)
                            Log.d("Uri", "uri 제대로 잘 바뀌었는지 확인 ${uri}")


                        }

                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        image?.close()
                    }
                }

            }


            // imageReader 객체에 위에서 만든 readerListener 를 달아서, 이미지가 사용가능하면 사진을 저장한다
            imageReader!!.setOnImageAvailableListener(readerListener, null)

            val captureListener = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    /*Toast.makeText(this@MainActivity, "Saved:$file", Toast.LENGTH_SHORT).show()*/
                    createCameraPreviewSession(surfaceTexture)
                }
            }

            // outputSurface 에 위에서 만든 captureListener 를 달아, 캡쳐(사진 찍기) 해주고 나서 카메라 미리보기 세션을 재시작한다
            openCamera!!.createCaptureSession(
                outputSurface,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {}

                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.capture(captureBuilder.build(), captureListener, null)
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                },
                null
            )


        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
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
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range.create(30, 30)
                )
                it.createCaptureSession(
                    listOf(
                        previewSurface
                    ),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if (openCamera == null) return

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
