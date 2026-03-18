package com.dynastxu.notedown.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 导出功能
 * @param context 上下文
 * @param selections 要导出的文件夹列表
 * @param destinationUri 导出到的目标 Uri
 */
fun export(context: Context, selections: List<File>, destinationUri: Uri) {
    try {
        Log.i("导出功能", "开始导出 ${selections.size} 个文件夹")
        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zipOutputStream ->
                selections.forEach { file ->
                    if (file.exists()) {
                        addFileToZip(file, file.name, zipOutputStream)
                    }
                }
            }
            Log.i("导出功能", "导出完成到：$destinationUri")
            Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("导出功能", "导出失败：${e.message}", e)
        Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun addFileToZip(file: File, path: String, zipOutputStream: ZipOutputStream) {
    if (file.isDirectory) {
        val files = file.listFiles() ?: return
        files.forEach { child ->
            addFileToZip(child, "$path/${child.name}", zipOutputStream)
        }
    } else {
        FileInputStream(file).use { inputStream ->
            val entry = ZipEntry(path)
            zipOutputStream.putNextEntry(entry)
            inputStream.copyTo(zipOutputStream)
            zipOutputStream.closeEntry()
        }
    }
}
