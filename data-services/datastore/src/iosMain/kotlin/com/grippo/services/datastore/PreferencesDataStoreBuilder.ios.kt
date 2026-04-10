package com.grippo.services.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.grippo.toolkit.context.NativeContext
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual fun NativeContext.getPreferencesDataStore(): DataStore<Preferences> {
    val filePath = documentDirectory() + "/grippo_settings.preferences_pb"

    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { filePath.toPath() }
    )
}

private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )

    return requireNotNull(documentDirectory?.path)
}
