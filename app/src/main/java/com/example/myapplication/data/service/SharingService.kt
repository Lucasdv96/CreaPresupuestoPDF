package com.example.myapplication.data.service

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File

class SharingService(private val context: Context) {

    fun sharePdf(pdfPath: String, budgetNumber: String) {
        val pdfFile = File(pdfPath)
        if (!pdfFile.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Presupuesto $budgetNumber")
            putExtra(Intent.EXTRA_TEXT, "Adjunto encontrará el presupuesto $budgetNumber.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Compartir Presupuesto").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
    }

    fun shareViaWhatsApp(pdfPath: String, budgetNumber: String) {
        val pdfFile = File(pdfPath)
        if (!pdfFile.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Presupuesto $budgetNumber")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            // WhatsApp no instalado, fallback al selector general
            sharePdf(pdfPath, budgetNumber)
        }
    }

    fun downloadPdfToPublicStorage(pdfPath: String, budgetNumber: String): Boolean {
        val sourceFile = File(pdfPath)
        if (!sourceFile.exists()) return false
        val fileName = "Presupuesto_${budgetNumber}.pdf"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadViaMediaStore(sourceFile, fileName)
            } else {
                downloadLegacy(sourceFile, fileName)
            }
        } catch (e: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadViaMediaStore(source: File, fileName: String): Boolean {
        val resolver = context.contentResolver
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: return false
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    @SuppressLint("NewApi")
    private fun downloadLegacy(source: File, fileName: String): Boolean {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        source.copyTo(File(dir, fileName), overwrite = true)
        return true
    }

    fun shareViaEmail(pdfPath: String, budgetNumber: String) {
        val pdfFile = File(pdfPath)
        if (!pdfFile.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Presupuesto $budgetNumber")
            putExtra(Intent.EXTRA_TEXT, "Estimado cliente,\n\nAdjunto encontrará el presupuesto $budgetNumber.\n\nSaludos.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(emailIntent, "Enviar por Email").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
    }
}
