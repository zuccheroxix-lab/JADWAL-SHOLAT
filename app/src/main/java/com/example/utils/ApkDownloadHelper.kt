package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream

object ApkDownloadHelper {
    private const val TAG = "ApkDownloadHelper"

    /**
     * Ekspor & unduh Debug APK ke folder Download perangkat,
     * lalu buka lembar aksi Android (Install / Share) menggunakan FileProvider.
     */
    fun saveAndInstallDebugApk(context: Context) {
        exportApkFile(context, "app-debug.apk", "Debug APK (ZUCCHERO)")
    }

    /**
     * Ekspor & unduh Release APK ke folder Download perangkat,
     * lalu buka lembar aksi Android (Install / Share) menggunakan FileProvider.
     */
    fun saveAndInstallReleaseApk(context: Context) {
        exportApkFile(context, "app-release.apk", "Release APK (ZUCCHERO)")
    }

    private fun exportApkFile(context: Context, targetFileName: String, label: String) {
        try {
            val sourceApk = File(context.applicationInfo.sourceDir)
            if (!sourceApk.exists()) {
                Toast.makeText(context, "File source APK tidak ditemukan pada sistem runtime.", Toast.LENGTH_SHORT).show()
                return
            }

            var savedToDownloads = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        FileInputStream(sourceApk).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    savedToDownloads = true
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, targetFileName)
                sourceApk.copyTo(targetFile, overwrite = true)
                savedToDownloads = true
            }

            // Copy to local cache to generate content URI via FileProvider
            val cacheApk = File(context.cacheDir, targetFileName)
            sourceApk.copyTo(cacheApk, overwrite = true)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheApk
            )

            if (savedToDownloads) {
                Toast.makeText(
                    context,
                    "✅ Berhasil! $label tersimpan di folder Download ($targetFileName)",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Open chooser for Install or Share directly
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, label)
                putExtra(Intent.EXTRA_TEXT, "File APK $label siap pasang.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Buka / Pasang $targetFileName:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengekspor APK: ${e.message}", e)
            Toast.makeText(context, "Gagal mengekspor APK: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
