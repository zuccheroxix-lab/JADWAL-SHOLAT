package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
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
     * Otomatis unduh & simpan file APK langsung ke folder Downloads perangkat
     * dan buka pemilih tindakan (Share / Install) secara otomatis tanpa ribet.
     */
    fun downloadAndSaveApk(context: Context) {
        try {
            val sourceApk = File(context.applicationInfo.sourceDir)
            val apkFileName = "ZUCCHERO_Waktu_Sholat_v1.0.apk"

            var savedSuccessfully = false

            if (sourceApk.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, apkFileName)
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
                        savedSuccessfully = true
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val targetFile = File(downloadsDir, apkFileName)
                    sourceApk.copyTo(targetFile, overwrite = true)
                    savedSuccessfully = true
                }

                // Copy to cache to grant FileProvider URI for direct install/share
                val cacheApk = File(context.cacheDir, apkFileName)
                sourceApk.copyTo(cacheApk, overwrite = true)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheApk
                )

                if (savedSuccessfully) {
                    Toast.makeText(
                        context,
                        "✅ Berhasil! APK otomatis disimpan ke folder Download: $apkFileName",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Launch chooser so user can instantly install, save to Drive, send to WhatsApp, etc.
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "APK ZUCCHERO Waktu Sholat AI")
                    putExtra(Intent.EXTRA_TEXT, "File APK ZUCCHERO Waktu Sholat AI v1.0 siap pasang.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(shareIntent, "Pilih Aksi untuk File APK:")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

            } else {
                // If running in development sandbox where sourceDir is unavailable, trigger direct browser download
                openDirectDownloadLink(context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunduh APK langsung, mengalihkan ke browser", e)
            openDirectDownloadLink(context)
        }
    }

    /**
     * Membuka link browser unduhan langsung satu klik
     */
    fun openDirectDownloadLink(context: Context) {
        try {
            val downloadUrl = "https://ais-dev-u6z6owj5wor7fna6rmoujp-775066291292.asia-east1.run.app/app-debug.apk"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            Toast.makeText(context, "Membuka link unduhan langsung...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
