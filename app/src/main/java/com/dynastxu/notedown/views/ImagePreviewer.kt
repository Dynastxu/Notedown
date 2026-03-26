package com.dynastxu.notedown.views

import android.util.Log
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.dynastxu.notedown.R
import com.dynastxu.notedown.models.data.ImageData
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun ImagePreviewer(
    image: ImageData?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val zoomState = rememberZoomState()
    Column(
        modifier = modifier
    ) {
        AsyncImage(
            model = image?.src,
            contentDescription = image?.alt,
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                zoomState.setContentSize(state.painter.intrinsicSize)
            },
            onError = {
                Log.e("图片加载", "图片加载失败")
                Log.e("图片加载", "路径： ${image?.src}")
            },
            modifier = Modifier
                .fillMaxSize()
                .zoomable(zoomState)
                .combinedClickable(
                    onClick = onClick
                ),
            placeholder = painterResource(R.drawable.outline_image_24),
            error = painterResource(R.drawable.outline_broken_image_24)
        )
    }
}