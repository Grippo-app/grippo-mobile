package com.grippo.toolkit.permission.internal

import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.permission.PermissionManager

internal expect fun NativeContext.getPermissionManager(): PermissionManager
