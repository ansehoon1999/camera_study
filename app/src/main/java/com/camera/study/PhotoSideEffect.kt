package com.camera.study

import android.net.Uri

sealed class PhotoSideEffect {

    data class RotateImage(val uri: Uri) : PhotoSideEffect()

    object MoveToGallery : PhotoSideEffect()

}