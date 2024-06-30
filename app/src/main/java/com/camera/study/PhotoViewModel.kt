package com.camera.study

import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import java.io.InputStream


class PhotoViewModel : ViewModel(), ContainerHost<PhotoUiState, PhotoSideEffect
        > {
    override val container: Container<PhotoUiState, PhotoSideEffect> =
        container(PhotoUiState.empty())

    fun updateUri(uri: Uri) = intent {
        reduce {
            state.copy(
                uri = uri
            )
        }
    }

    fun rotateImage() = intent {
        val uri = state.uri ?: return@intent
        postSideEffect(
            PhotoSideEffect.RotateImage(
                uri = uri
            )
        )
    }

    fun moveToGallery() = intent {
        postSideEffect(PhotoSideEffect.MoveToGallery)
    }
}