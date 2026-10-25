package com.grippo.toolkit.share.internal

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// A dedicated subclass stops the manifest merger from combining this provider with other FileProviders in the app.
internal class ShareFileProvider : FileProvider() {

    companion object {
        private const val AUTHORITY_SUFFIX = ".share.fileprovider"
        private const val SHARES_DIRECTORY = "shares"

        fun sharesDirectory(context: Context): File {
            return File(context.cacheDir, SHARES_DIRECTORY)
        }

        fun contentUri(context: Context, file: File, displayName: String): Uri {
            val authority = "${context.packageName}$AUTHORITY_SUFFIX"
            return FileProvider.getUriForFile(context, authority, file, displayName)
        }
    }
}
