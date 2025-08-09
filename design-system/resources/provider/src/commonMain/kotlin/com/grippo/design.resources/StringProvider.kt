package com.grippo.design.resources

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

public interface StringProvider {
    public suspend fun get(id: StringResource, vararg args: Any): String
    public suspend fun plural(id: PluralStringResource, quantity: Int, vararg args: Any): String
}