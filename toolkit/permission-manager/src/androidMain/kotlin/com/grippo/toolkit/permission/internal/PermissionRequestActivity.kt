package com.grippo.toolkit.permission.internal

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Transparent, animation-less activity that hosts the system permission dialog
 * and forwards the result back to [PermissionResultHolder].
 *
 * Started by [AndroidPermissionManager] via a regular [android.content.Intent].
 * Finishes itself as soon as the result is received — the user only ever sees
 * the system dialog overlay, not this activity.
 */
internal class PermissionRequestActivity : ComponentActivity() {

    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val mapped = results.mapValues { (permission, granted) ->
            PermissionResult(
                granted = granted,
                canAskAgain = shouldShowRequestPermissionRationale(permission),
            )
        }
        PermissionResultHolder.deferred.get()?.complete(mapped)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableTransitionAnimation()

        val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)
        if (permissions.isNullOrEmpty()) {
            PermissionResultHolder.deferred.get()?.complete(emptyMap())
            finish()
            return
        }

        launcher.launch(permissions)
    }

    override fun finish() {
        super.finish()
        disableTransitionAnimation()
    }

    private fun disableTransitionAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    internal companion object {
        const val EXTRA_PERMISSIONS = "extra_permissions"
    }
}
