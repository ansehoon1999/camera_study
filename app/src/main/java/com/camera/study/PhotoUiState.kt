package com.camera.study

import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.Immutable
import java.io.InputStream

@Immutable
data class PhotoUiState(
    val uri: Uri?,
) {
    companion object {
        fun empty() = PhotoUiState(
            uri = null
        )
    }
}