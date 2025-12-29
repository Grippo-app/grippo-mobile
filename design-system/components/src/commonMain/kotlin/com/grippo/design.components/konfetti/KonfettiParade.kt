package com.grippo.design.components.konfetti

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.konfetti.compose.Konfetti
import com.grippo.konfetti.presets.parade

@Composable
public fun KonfettiParade() {
    val konfettiColors = with(AppTokens.colors.konfetti) {
        listOf(
            confettiColor1,
            confettiColor2,
            confettiColor3,
            confettiColor4,
            confettiColor5,
            confettiColor6,
            confettiColor7,
            confettiColor8,
            confettiColor9,
            confettiColor10,
        )
    }

    val konfetti = remember { parade(konfettiColors) }

    Konfetti(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f),
        parties = konfetti,
    )
}

@AppPreview
@Composable
private fun KonfettiParadePreview() {
    PreviewContainer {
        KonfettiParade()
    }
}