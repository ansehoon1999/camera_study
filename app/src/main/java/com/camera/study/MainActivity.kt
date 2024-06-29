package com.camera.study

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.camera.study.ui.theme.CameraStudyTheme

class MainActivity : ComponentActivity() {


    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {

        when (it) {
            true -> {
                Toast.makeText(this@MainActivity, "권한 허가", Toast.LENGTH_SHORT).show()

            }

            false -> {
                Toast.makeText(this@MainActivity, "권한 거부", Toast.LENGTH_SHORT).show()

            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            CameraStudyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AndroidView(factory = {
                        CameraGLSurfaceView(it).apply {


                            setRenderer(
                                CameraRenderer(
                                    context = this@MainActivity,
                                    surface = this,
                                    cameraUtils = CameraUtils(
                                        context = this@MainActivity,
                                        facing =  CameraCharacteristics.LENS_FACING_BACK
                                    )
                                )
                            )
                        }
                    })

                }
            }
        }

    }

    override fun onResume() {
        super.onResume()

        requestPermission.launch(Manifest.permission.CAMERA)

    }

    override fun onPause() {

        super.onPause()
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

}