package com.grippo.design.components.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Check
import com.grippo.design.resources.provider.welcome_check_equipment
import com.grippo.design.resources.provider.welcome_check_experience
import com.grippo.design.resources.provider.welcome_check_muscles
import com.grippo.design.resources.provider.welcome_check_profile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class WelcomeChecklistItem(
    val text: String,
)

@Composable
public fun WelcomeChecklist(
    modifier: Modifier = Modifier,
    items: ImmutableList<WelcomeChecklistItem>,
) {
    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.welcome.card.radius)
            )
            .padding(
                horizontal = AppTokens.dp.welcome.card.horizontalPadding,
                vertical = AppTokens.dp.welcome.card.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        items.forEach { item ->
            key(item) {
                ChecklistRow(
                    modifier = Modifier.fillMaxWidth(),
                    text = item.text,
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    modifier: Modifier = Modifier,
    text: String,
) {
    val accent = AppTokens.colors.semantic.success

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.welcome.checkmark.size)
                .background(
                    color = accent.copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.welcome.checkmark.size),
                imageVector = AppTokens.icons.Check,
                contentDescription = null,
                tint = accent,
            )
        }

        Text(
            text = text,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreview
@Composable
private fun WelcomeChecklistPreview() {
    PreviewContainer {
        WelcomeChecklist(
            modifier = Modifier.fillMaxWidth(),
            items = persistentListOf(
                WelcomeChecklistItem(AppTokens.strings.res(Res.string.welcome_check_profile)),
                WelcomeChecklistItem(AppTokens.strings.res(Res.string.welcome_check_experience)),
                WelcomeChecklistItem(AppTokens.strings.res(Res.string.welcome_check_muscles)),
                WelcomeChecklistItem(AppTokens.strings.res(Res.string.welcome_check_equipment)),
            )
        )
    }
}
