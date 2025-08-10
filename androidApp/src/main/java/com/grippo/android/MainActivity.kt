package com.grippo.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.arkivanov.decompose.retainedComponent
import com.grippo.date.utils.DateFormatting
import com.grippo.shared.root.RootComponent
import java.util.Locale

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
                finish = ::finishAffinity
            )
        }

        setContent {
            val languageTag = rememberAndroidLanguageTag()

            LaunchedEffect(languageTag) {
                DateFormatting.install(languageTag)
            }

            rootComponent.Render()
        }
    }
}

// Locale-tag receiver from device
@Composable
private fun rememberAndroidLanguageTag(): String? {
    val conf = LocalConfiguration.current // triggers recomposition on locale change

    // Respect per-app locales (if you use AppCompatDelegate)
    val appTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    if (appTags.isNotBlank()) return appTags

    return remember(conf) {
        conf.locales.toLanguageTags().ifBlank { Locale.getDefault().toLanguageTag() }
    }
}