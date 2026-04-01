package com.grippo.toolkit.permission.internal

import com.grippo.toolkit.permission.AppPermission
import com.grippo.toolkit.permission.PermissionManager
import com.grippo.toolkit.permission.PermissionStatus
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class IosPermissionManager : PermissionManager {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun check(permission: AppPermission): PermissionStatus = when (permission) {
        AppPermission.Notifications -> checkNotificationStatus()
    }

    override suspend fun request(permission: AppPermission): PermissionStatus = when (permission) {
        AppPermission.Notifications -> requestNotifications()
    }

    private suspend fun checkNotificationStatus(): PermissionStatus = suspendCoroutine { cont ->
        notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined
            cont.resume(status.toPermissionStatus())
        }
    }

    private suspend fun requestNotifications(): PermissionStatus = suspendCoroutine { cont ->
        val options = UNAuthorizationOptionAlert or
                UNAuthorizationOptionSound or
                UNAuthorizationOptionBadge

        notificationCenter.requestAuthorizationWithOptions(options) { granted, _ ->
            // After the dialog, fetch the authoritative status so we can tell
            // NotDetermined → Denied vs explicit Denied (DeniedPermanently).
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                val status = settings?.authorizationStatus
                    ?: if (granted) UNAuthorizationStatusAuthorized else UNAuthorizationStatusDenied
                cont.resume(status.toPermissionStatus())
            }
        }
    }

    private fun UNAuthorizationStatus.toPermissionStatus(): PermissionStatus = when (this) {
        UNAuthorizationStatusAuthorized -> PermissionStatus.Granted
        UNAuthorizationStatusProvisional -> PermissionStatus.Granted
        UNAuthorizationStatusEphemeral -> PermissionStatus.Granted
        UNAuthorizationStatusNotDetermined -> PermissionStatus.Denied
        UNAuthorizationStatusDenied -> PermissionStatus.DeniedPermanently
        else -> PermissionStatus.Denied
    }
}
