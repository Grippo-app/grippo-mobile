package com.grippo.design.components.user.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun UserAvatar(
    modifier: Modifier = Modifier,
    name: String,
    color: Color,
    size: Dp,
    textStyle: TextStyle,
    ringWidth: Dp = 0.dp,
) {
    val initials = remember(name) { name.toInitials() }

    val gradient = remember(color) {
        Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = 0.95f),
                color.copy(alpha = 0.55f),
            )
        )
    }

    val ringBox = if (ringWidth.value > 0f) {
        Modifier
            .border(
                width = ringWidth,
                color = color.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .padding(ringWidth)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(ringBox)
            .background(brush = gradient, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = textStyle,
            color = AppTokens.colors.static.white,
        )
    }
}

private fun String.toInitials(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].first().toString() + parts[1].first().toString()).uppercase()
    }
}

@AppPreview
@Composable
private fun UserAvatarSmallPreview() {
    PreviewContainer {
        UserAvatar(
            name = "Max Voitenko",
            color = ExperienceEnumState.PRO.color(),
            size = AppTokens.dp.userCard.avatar.small,
            textStyle = AppTokens.typography.h5(),
        )
        UserAvatar(
            name = "Max Voitenko",
            color = ExperienceEnumState.ADVANCED.color(),
            size = AppTokens.dp.userCard.avatar.small,
            textStyle = AppTokens.typography.h5(),
        )
        UserAvatar(
            name = "Max Voitenko",
            color = ExperienceEnumState.INTERMEDIATE.color(),
            size = AppTokens.dp.userCard.avatar.small,
            textStyle = AppTokens.typography.h5(),
        )
        UserAvatar(
            name = "Max Voitenko",
            color = ExperienceEnumState.BEGINNER.color(),
            size = AppTokens.dp.userCard.avatar.small,
            textStyle = AppTokens.typography.h5(),
        )
    }
}

@AppPreview
@Composable
private fun UserAvatarLargePreview() {
    PreviewContainer {
        UserAvatar(
            name = "Max Voitenko",
            color = ExperienceEnumState.PRO.color(),
            size = AppTokens.dp.userCard.avatar.large,
            textStyle = AppTokens.typography.h1(),
            ringWidth = AppTokens.dp.userCard.detailed.avatarRing,
        )
        UserAvatar(
            name = "Mark",
            color = ExperienceEnumState.ADVANCED.color(),
            size = AppTokens.dp.userCard.avatar.large,
            textStyle = AppTokens.typography.h1(),
            ringWidth = AppTokens.dp.userCard.detailed.avatarRing,
        )
        UserAvatar(
            name = "",
            color = ExperienceEnumState.INTERMEDIATE.color(),
            size = AppTokens.dp.userCard.avatar.large,
            textStyle = AppTokens.typography.h1(),
            ringWidth = AppTokens.dp.userCard.detailed.avatarRing,
        )
    }
}
