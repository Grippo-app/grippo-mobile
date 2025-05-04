package com.grippo.authorization.login

import com.grippo.core.models.BaseLoader

internal sealed interface LoginLoader : BaseLoader {
    data object LoginButton : LoginLoader
}