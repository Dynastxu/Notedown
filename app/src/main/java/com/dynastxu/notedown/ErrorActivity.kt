package com.dynastxu.notedown

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.dynastxu.notedown.pages.ErrorScreen

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