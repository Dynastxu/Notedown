package com.dynastxu.notedown.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.dynastxu.notedown.R
import com.dynastxu.notedown.models.view.MainViewModel
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun ImageScreen(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    val image by mainViewModel.selectedImage.collectAsState()
    val zoomState = rememberZoomState()
    Column(
        modifier = Modifier.fillMaxSize()
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
                    onClick = {
                        navController.popBackStack()
                    }
                ),
            placeholder = painterResource(R.drawable.outline_image_24),
            error = painterResource(R.drawable.outline_broken_image_24)
        )
    }
}