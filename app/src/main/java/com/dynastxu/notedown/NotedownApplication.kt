package com.dynastxu.notedown

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

class NotedownApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 设置全局未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
        }
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        // 将错误堆栈转换为字符串
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        val stackTrace = sw.toString()

        // 启动错误显示 Activity（并清除任务栈）
        val intent = Intent(this, ErrorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ErrorActivity.EXTRA_STACK_TRACE, stackTrace)
            putExtra(ErrorActivity.EXTRA_THREAD_NAME, thread.name)
        }
        startActivity(intent)

        // 杀死当前进程，避免重复处理
        Process.killProcess(Process.myPid())
        System.exit(1)
    }
}