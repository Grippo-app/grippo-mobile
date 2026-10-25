package com.grippo.toolkit.permission

public interface PermissionManager {

    public suspend fun check(permission: AppPermission): PermissionStatus

    public suspend fun request(permission: AppPermission): PermissionStatus
}
