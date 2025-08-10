package com.grippo.design.resources.provider.impl.internal

import com.grippo.design.resources.provider.providers.StringProvider
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single

@Single(binds = [StringProvider::class])
internal class StringProviderImpl : StringProvider {
    override suspend fun get(id: StringResource, vararg args: Any): String {
        return getString(id, *args)
    }

    override suspend fun plural(id: PluralStringResource, quantity: Int, vararg args: Any): String {
        return getPluralString(id, quantity, *args)
    }
}