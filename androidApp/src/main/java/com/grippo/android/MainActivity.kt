package com.grippo.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.arkivanov.decompose.retainedComponent
import com.grippo.platform.core.LocalAppTheme
import com.grippo.shared.root.RootComponent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val rootComponent: RootComponent = retainedComponent("RootComponentContext") {
            RootComponent(
                componentContext = it,
                close = ::finishAffinity
            )
        }
        setContent {
            SystemBarsIcons(LocalAppTheme.current)
            rootComponent.Render()
        }
    }
}

@Composable
fun SystemBarsIcons(isDark: Boolean) {
    val view = LocalView.current

    // Local helper inlined inside the composable (no top-level function)
    tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    val activity = view.context.findActivity() ?: return
    val window = activity.window
    val controller = WindowCompat.getInsetsController(window, view)
    val wantDarkIcons = !isDark

    DisposableEffect(wantDarkIcons) {
        // Save previous icon modes
        val prevStatus = controller.isAppearanceLightStatusBars
        val prevNav = controller.isAppearanceLightNavigationBars

        // Apply
        controller.isAppearanceLightStatusBars = wantDarkIcons
        controller.isAppearanceLightNavigationBars = wantDarkIcons

        onDispose {
            // Restore on exit
            controller.isAppearanceLightStatusBars = prevStatus
            controller.isAppearanceLightNavigationBars = prevNav
        }
    }
}