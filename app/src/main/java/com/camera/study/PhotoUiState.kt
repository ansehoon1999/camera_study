package com.camera.study

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class PhotoUiState(
    val uri: Uri?
) {
    companion object {
        fun empty() = PhotoUiState(
            uri = null
        )
    }
}