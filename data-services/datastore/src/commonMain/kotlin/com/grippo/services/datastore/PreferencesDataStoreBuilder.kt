package com.grippo.services.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.grippo.toolkit.context.NativeContext

internal expect fun NativeContext.getPreferencesDataStore(): DataStore<Preferences>
