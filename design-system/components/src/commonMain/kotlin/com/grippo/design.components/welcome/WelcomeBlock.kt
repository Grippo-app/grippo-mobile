package com.grippo.design.components.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun WelcomeBlock(
    modifier: Modifier = Modifier,
    experience: ExperienceEnumState,
) {
    val description = experience.description().text()
    val motto = experience.motto()
    val accent = experience.color()
    val cardColor = AppTokens.colors.background.card

    val gradient = remember(accent) {
        Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = 0.18f),
                Color.Transparent,
            )
        )
    }

    val shape = RoundedCornerShape(AppTokens.dp.welcome.card.radius)

    Column(
        modifier = modifier
            .background(color = cardColor, shape = shape)
            .background(brush = gradient, shape = shape)
            .padding(
                horizontal = AppTokens.dp.welcome.card.horizontalPadding,
                vertical = AppTokens.dp.welcome.card.verticalPadding
            ),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = motto,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = description,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun WelcomeBlockBeginnerPreview() {
    PreviewContainer {
        WelcomeBlock(
            modifier = Modifier.fillMaxWidth(),
            experience = ExperienceEnumState.BEGINNER,
        )
    }
}

@AppPreview
@Composable
private fun WelcomeBlockProPreview() {
    PreviewContainer {
        WelcomeBlock(
            modifier = Modifier.fillMaxWidth(),
            experience = ExperienceEnumState.PRO,
        )
    }
}
