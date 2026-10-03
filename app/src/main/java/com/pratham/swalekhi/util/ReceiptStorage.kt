package com.pratham.swalekhi.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/** Copies a picked receipt image into the app's private storage so it survives even if the
 * original file (gallery item, downloads, etc.) is later moved or deleted. */
object ReceiptStorage {

    private fun receiptsDir(context: Context): File =
        File(context.filesDir, "receipts").apply { mkdirs() }

    fun copyToAppStorage(context: Context, uri: Uri): String? {
        return try {
            val destination = File(receiptsDir(context), "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            destination.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
