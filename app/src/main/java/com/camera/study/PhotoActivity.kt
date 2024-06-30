package com.camera.study

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.camera.study.ui.theme.CameraStudyTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.io.File
import java.io.FileOutputStream

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

                        is PhotoSideEffect.RotateImage -> {
                            var inputStream = contentResolver.openInputStream(it.uri) ?: return@collectSideEffect
                            val exifInterface = ExifInterface(inputStream)
                            var orientation = exifInterface.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )

                            Log.d("orientation", orientation.toString())
                            when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> {
                                    orientation = 90
                                    Log.d("orientation", "ORIENTATION_ROTATE_90")
                                }

                                ExifInterface.ORIENTATION_ROTATE_180 -> {
                                    orientation = 180
                                    Log.d("orientation", "ORIENTATION_ROTATE_180")
                                }

                                ExifInterface.ORIENTATION_ROTATE_270 -> {
                                    orientation = 270
                                    Log.d("orientation", "ORIENTATION_ROTATE_270")
                                }
                            }


                            Log.d("orientation", inputStream.toString())
                            if (orientation >= 180) {
                                inputStream = contentResolver.openInputStream(it.uri) ?: return@collectSideEffect
                                val bitmap = BitmapFactory.decodeStream(inputStream)
                                inputStream.close()

                                val matrix = Matrix()
                                matrix.setRotate(90f, bitmap.width.toFloat(), bitmap.height.toFloat())
                                val newImg = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                val cacheFile = File(
                                    applicationContext.cacheDir,
                                    "newImage.jpg"
                                )

                                val outputStream = FileOutputStream(cacheFile)
                                newImg.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                outputStream.close()

                                viewModel.updateUri(cacheFile.toUri())
                            }
                        }
                    }
                }

                Scaffold(
                    bottomBar = {

                        Row {
                            Button(onClick = viewModel::moveToGallery) {
                                Text("이미지 가져오기")
                            }

                            Button(onClick = viewModel::rotateImage) {
                                Text("화면 회전하기")
                            }
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