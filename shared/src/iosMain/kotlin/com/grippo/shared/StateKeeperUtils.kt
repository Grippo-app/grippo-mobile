package com.grippo.shared

import com.arkivanov.essenty.statekeeper.SerializableContainer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSCoder
import platform.Foundation.NSString
import platform.Foundation.decodeTopLevelObjectOfClass
import platform.Foundation.encodeObject

@Suppress("unused")
public fun save(coder: NSCoder, state: SerializableContainer) {
    coder.encodeObject(
        `object` = Json.encodeToString(SerializableContainer.serializer(), state),
        forKey = "state"
    )
}

@Suppress("unused") // Used in Swift
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public fun restore(coder: NSCoder): SerializableContainer? =
    (coder.decodeTopLevelObjectOfClass(
        aClass = NSString,
        forKey = "state",
        error = null
    ) as String?)?.let {
        try {
            Json.decodeFromString(SerializableContainer.serializer(), it)
        } catch (e: Exception) {
            null
        }
    }