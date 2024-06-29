package com.camera.study

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.camera.study.ui.theme.CameraStudyTheme
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.IntBuffer
import java.text.SimpleDateFormat
import java.util.Date
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.opengles.GL10


class MainActivity : ComponentActivity() {


    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionMap ->

        Log.d("permission", permissionMap.toString())

        if(!permissionMap.containsValue(false)) {
            Toast.makeText(this@MainActivity, "권한 허가", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            CameraStudyTheme {

                var isCaptureEnabled by remember {
                    mutableStateOf(false)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                isCaptureEnabled = true
                            }) {
                                Text(text = "캡처")
                        }
                    }
                ) {
                    AndroidView(
                        modifier = Modifier.padding(it),
                        factory = {
                        CameraGLSurfaceView(it).apply {


                            setRenderer(
                                CameraRenderer(
                                    context = this@MainActivity,
                                    surface = this,
                                    cameraUtils = CameraUtils(
                                        context = this@MainActivity,
                                        facing = CameraCharacteristics.LENS_FACING_BACK
                                    )
                                )
                            )



//                            this.queueEvent {
//
//                            }
                        }
                    }, update = {

                            if(isCaptureEnabled) {
                                captureFrame(it)
                                isCaptureEnabled = false
                            }
                        })

                }
            }
        }

    }

    private fun captureFrame(cameraGLSurfaceView: CameraGLSurfaceView) {
        cameraGLSurfaceView.queueEvent {
            val egl = EGLContext.getEGL() as EGL10
            val gl = egl.eglGetCurrentContext().gl as GL10
            val snapshotBitmap = createBitmapFromGLSurface(0, 0, cameraGLSurfaceView.width, cameraGLSurfaceView.height, gl)

            runOnUiThread {
                val path = Environment.getExternalStorageDirectory().absolutePath+ "/Pictures"

                val file = File(path)
                if(!file.exists()) {
                    file.mkdirs()
                    Toast.makeText(this@MainActivity, "폴더 생성", Toast.LENGTH_SHORT).show()
                }

                val day = SimpleDateFormat("yyyyMMddHHmmss")
                val date = Date()
                val captureview = snapshotBitmap

                val fos: FileOutputStream
                try {
                    fos = FileOutputStream(path + "/Capture" + day.format(date) + ".jpeg")
                    captureview?.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.parse("file://" + path + "/Capture" + day.format(date) + ".JPEG")
                        )
                    )
                    Toast.makeText(
                        this@MainActivity,
                        "촬영 완료. 갤러리를 확인해주세요 (폴더명 - arcapture)",
                        Toast.LENGTH_SHORT
                    ).show()
                    fos.flush()
                    fos.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createBitmapFromGLSurface(x: Int, y: Int, width: Int, height: Int, gl: GL10): Bitmap? {
        val bitmapBuffer = IntArray(width * height)
        val bitmapSource = IntArray(width * height)
        val intBuffer = IntBuffer.wrap(bitmapBuffer)
        intBuffer.position(0)

        try {
            gl.glReadPixels(x, y, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer)
            var offset1: Int
            var offset2: Int
            for (i in 0 until height) {
                offset1 = i * width
                offset2 = (height - i - 1) * width
                for (j in 0 until width) {
                    val texturePixel = bitmapBuffer[offset1 + j]
                    val blue = texturePixel shr 16 and 0xff
                    val red = texturePixel shl 16 and 0x00ff0000
                    val pixel = texturePixel and -0xff0100 or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }

        } catch (e: Exception) {
            return null
        }

        return Bitmap.createBitmap(bitmapSource, width, height, Bitmap.Config.ARGB_8888)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()

        requestPermission.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                )
        )
    }
}