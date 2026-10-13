package com.grippo.shared.state

import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlinx.serialization.json.Json

public class ApplicationState private constructor(
    public val stateKeeper: StateKeeperDispatcher,
) {
    public constructor() : this(StateKeeperDispatcher())

    @Throws(Exception::class)
    public constructor(savedState: String) : this(
        StateKeeperDispatcher(
            Json.decodeFromString(SerializableContainer.serializer(), savedState)
        )
    )

    @Throws(Exception::class)
    public fun save(): String =
        Json.encodeToString(SerializableContainer.serializer(), stateKeeper.save())
}
