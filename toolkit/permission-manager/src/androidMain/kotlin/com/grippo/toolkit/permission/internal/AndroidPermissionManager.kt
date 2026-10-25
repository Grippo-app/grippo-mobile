package com.grippo.toolkit.permission.internal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.grippo.toolkit.permission.AppPermission
import com.grippo.toolkit.permission.PermissionManager
import com.grippo.toolkit.permission.PermissionStatus
import kotlinx.coroutines.CompletableDeferred

internal class AndroidPermissionManager(private val context: Context) : PermissionManager {

    override suspend fun check(permission: AppPermission): PermissionStatus {
        val androidPermission = permission.toAndroidPermission()
            ?: return PermissionStatus.Granted

        return if (ContextCompat.checkSelfPermission(context, androidPermission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied
        }
    }

    override suspend fun request(permission: AppPermission): PermissionStatus {
        val androidPermission = permission.toAndroidPermission()
            ?: return PermissionStatus.Granted

        if (ContextCompat.checkSelfPermission(context, androidPermission)
            == PackageManager.PERMISSION_GRANTED
        ) return PermissionStatus.Granted

        val deferred = CompletableDeferred<Map<String, PermissionResult>>()
        PermissionResultHolder.deferred.set(deferred)

        val intent = Intent(context, PermissionRequestActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(PermissionRequestActivity.EXTRA_PERMISSIONS, arrayOf(androidPermission))
        }
        context.startActivity(intent)

        val results = deferred.await()
        PermissionResultHolder.deferred.set(null)

        val result = results[androidPermission]
            ?: return PermissionStatus.Denied

        return when {
            result.granted -> PermissionStatus.Granted
            result.canAskAgain -> PermissionStatus.Denied
            else -> PermissionStatus.DeniedPermanently
        }
    }

    private fun AppPermission.toAndroidPermission(): String? = when (this) {
        AppPermission.Notifications -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.POST_NOTIFICATIONS
            } else {
                null
            }
        }
    }
}
