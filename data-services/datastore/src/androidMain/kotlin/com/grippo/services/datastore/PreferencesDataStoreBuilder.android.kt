package com.grippo.services.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.grippo.toolkit.context.NativeContext
import okio.Path.Companion.toPath

internal actual fun NativeContext.getPreferencesDataStore(): DataStore<Preferences> {
    val filePath = context.filesDir.resolve("grippo_settings.preferences_pb").absolutePath

    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { filePath.toPath() }
    )
}
