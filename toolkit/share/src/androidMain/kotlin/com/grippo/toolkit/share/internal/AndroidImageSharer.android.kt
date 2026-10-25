package com.grippo.toolkit.share.internal

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.grippo.toolkit.share.ImageSharer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal class AndroidImageSharer(private val context: Context) : ImageSharer {

    override suspend fun share(image: ImageBitmap, fileName: String) {
        requirePngFileName(fileName)

        val uri = withContext(Dispatchers.IO) {
            writePng(image = image, fileName = fileName)
        }

        val sendIntent = Intent(Intent.ACTION_SEND)
            .setType(MIME_TYPE_PNG)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // The chooser needs this ClipData and its read grant to render the image preview.
        sendIntent.clipData = ClipData.newRawUri(null, uri)

        val chooser = Intent.createChooser(sendIntent, null)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(chooser)
    }

    private fun writePng(image: ImageBitmap, fileName: String): Uri {
        val directory = ShareFileProvider.sharesDirectory(context)
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Unable to create share directory: ${directory.absolutePath}")
        }

        directory.listFiles()?.forEach { previous -> previous.delete() }

        val file = File.createTempFile(FILE_PREFIX, PNG_EXTENSION, directory)
        file.outputStream().use { stream ->
            val encoded = image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
            if (!encoded) {
                throw IOException("Unable to encode shared image as PNG: ${file.absolutePath}")
            }
        }

        return ShareFileProvider.contentUri(context = context, file = file, displayName = fileName)
    }

    private companion object {
        const val FILE_PREFIX = "share"
        const val MIME_TYPE_PNG = "image/png"
    }
}
