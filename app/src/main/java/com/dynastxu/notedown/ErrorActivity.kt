package com.dynastxu.notedown

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class ErrorActivity : ComponentActivity() {
    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
        const val EXTRA_THREAD_NAME = "extra_thread_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE)
                    ?: stringResource(R.string.error_unknown_stack)
                val threadName = intent.getStringExtra(EXTRA_THREAD_NAME)
                    ?: stringResource(R.string.error_unknown_thread)
                val hintOnCopy = stringResource(R.string.error_on_copy)
                val clipboardLabel = stringResource(R.string.error_clipboard_label)
                ErrorScreen(
                    stackTrace = stackTrace,
                    threadName = threadName,
                    onCopy = {
                        copyToClipboard(clipboardLabel, stackTrace)
                        // 弹出吐司提示
                        android.widget.Toast.makeText(
                            this,
                            hintOnCopy,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onExit = { finishAffinity() } // 关闭所有 Activity 并退出应用
                )
            }
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}

@Composable
fun ErrorScreen(
    stackTrace: String,
    threadName: String,
    onCopy: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxWidth()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(4.dp)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.error_fatal),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.error_thread_name)} $threadName",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = stackTrace,
                onValueChange = {},
                label = { Text(stringResource(R.string.error_stack)) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                minLines = 10,
                maxLines = 50
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(0.5f)
                ) {
                    Text(stringResource(R.string.error_copy))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onExit,
                    modifier = Modifier.weight(0.5f)
                ) {
                    Text(stringResource(R.string.error_confirm))
                }
            }
        }
    }
}