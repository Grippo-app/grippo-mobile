package com.grippo.authorization.login

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
internal sealed interface LoginLoader : BaseLoader {
    @Immutable
    data object LoginButton : LoginLoader
}