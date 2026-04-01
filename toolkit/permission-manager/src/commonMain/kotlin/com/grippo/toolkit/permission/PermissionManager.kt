package com.grippo.toolkit.permission

public interface PermissionManager {

    /**
     * Returns the current [PermissionStatus] without showing any system dialog.
     * Safe to call from any coroutine context.
     */
    public suspend fun check(permission: AppPermission): PermissionStatus

    /**
     * Shows the system permission dialog (if applicable) and returns the
     * resulting [PermissionStatus].
     *
     * If the permission is already [PermissionStatus.Granted] or
     * [PermissionStatus.DeniedPermanently] no dialog is shown and the
     * current status is returned immediately.
     */
    public suspend fun request(permission: AppPermission): PermissionStatus
}
