package com.grippo.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.arkivanov.decompose.retainedComponent
import com.grippo.shared.root.RootComponent
import com.grippo.toolkit.local.notification.LocalNotificationExtras

class MainActivity : ComponentActivity() {

    private val root: RootComponent by lazy {
        retainedComponent {
            RootComponent(
                componentContext = it,
                close = ::finishAffinity,
                deeplink = intent.getStringExtra(LocalNotificationExtras.DEEPLINK),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            SystemBarsIcons()
            root.Render()
        }
    }

    // Warm start: app already running, user tapped a notification
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(LocalNotificationExtras.DEEPLINK)
            ?.let { root.handleDeeplink(it) }
    }
}

@Composable
private fun SystemBarsIcons() {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()

    // Local helper inlined inside the composable (no top-level function)
    tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    val activity = view.context.findActivity() ?: return
    val window = activity.window
    val controller = WindowCompat.getInsetsController(window, view)
    DisposableEffect(view, darkTheme) {
        // Save previous icon modes
        val prevStatus = controller.isAppearanceLightStatusBars
        val prevNav = controller.isAppearanceLightNavigationBars

        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme

        onDispose {
            // Restore on exit
            controller.isAppearanceLightStatusBars = prevStatus
            controller.isAppearanceLightNavigationBars = prevNav
        }
    }
}
