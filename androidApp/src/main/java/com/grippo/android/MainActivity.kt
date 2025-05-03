package com.grippo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.grippo.shared.Content

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootComponent = retainedComponent("RootComponentContext") {
            RootComponent(
                componentContext = it,
                onClose = ::finishAffinity,
            )
        }

        setContent { Content() }
    }
}