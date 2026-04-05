package com.grippo.toolkit.permission

public sealed class PermissionStatus {
    public data object Granted : PermissionStatus()
    public data object Denied : PermissionStatus()
    public data object DeniedPermanently : PermissionStatus()
}
