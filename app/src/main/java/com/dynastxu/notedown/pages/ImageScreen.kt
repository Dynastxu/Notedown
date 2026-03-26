package com.dynastxu.notedown.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.dynastxu.notedown.models.view.MainViewModel
import com.dynastxu.notedown.views.ImagePreviewer

@Composable
fun ImageScreen(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    val image by mainViewModel.selectedImage.collectAsState()
    ImagePreviewer(
        image = image,
        onClick = {
            navController.popBackStack()
        }
    )
}