package com.camera.study

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.camera.study.ui.theme.CameraStudyTheme

class MainActivity : ComponentActivity() {

    private lateinit var sampleGlSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContent {
//            CameraStudyTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {}
//            }
//        }
        sampleGlSurfaceView = SampleGLSurfaceView(this)
        setContentView(sampleGlSurfaceView)
    }

    override fun onPause() {
        super.onPause()
        sampleGlSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        sampleGlSurfaceView.onResume()
    }
}
