package com.dynastxu.notedown.views

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Loading(modifier: Modifier = Modifier, msg: String = "") {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (msg.isNotBlank()) {
            Text(msg)
            Spacer(Modifier.height(8.dp))
        }
        CircularProgressIndicator()
    }
}

@Composable
fun Loading(@StringRes msg: Int) {
    Loading(msg = stringResource(msg))
}

@Preview(showBackground = true)
@Composable
private fun PLoading(){
    Loading(msg = "Loading")
}