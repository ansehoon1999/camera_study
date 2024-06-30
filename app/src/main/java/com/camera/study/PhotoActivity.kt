package com.camera.study

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.camera.study.ui.theme.CameraStudyTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

class PhotoActivity : ComponentActivity() {

    private val viewModel: PhotoViewModel by viewModels()

    private val requestGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let { imageUri ->
                    viewModel.updateUri(imageUri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            CameraStudyTheme {

                val uiState = viewModel.collectAsState().value

                viewModel.collectSideEffect {

                    when (it) {
                        is PhotoSideEffect.MoveToGallery -> {
                            val intent = Intent(Intent.ACTION_PICK)
                            intent.type = MediaStore.Images.Media.CONTENT_TYPE
                            intent.setDataAndType(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                "image/*"
                            )
                            requestGalleryLauncher.launch(intent)
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        Button(onClick = viewModel::moveToGallery) {
                            Text("이미지 가져오기")
                        }
                    }
                ) {
                    AndroidView(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxSize(),
                        factory = { context ->
                            ImageView(context)
                        }, update = { imageView ->
                            Glide.with(imageView)
                                .load(uiState.uri)
                                .apply(
                                    RequestOptions()
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .skipMemoryCache(true)
                                )
                                .into(imageView)
                        })
                }
            }
        }
    }
}