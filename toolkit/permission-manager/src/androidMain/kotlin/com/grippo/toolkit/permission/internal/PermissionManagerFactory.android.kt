package com.grippo.toolkit.permission.internal

import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.permission.PermissionManager

internal actual fun NativeContext.getPermissionManager(): PermissionManager =
    AndroidPermissionManager(context)
