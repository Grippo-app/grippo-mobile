package com.grippo.design.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Check
import com.grippo.design.resources.provider.onboarding_check_equipment
import com.grippo.design.resources.provider.onboarding_check_experience
import com.grippo.design.resources.provider.onboarding_check_muscles
import com.grippo.design.resources.provider.onboarding_check_profile
import com.grippo.design.resources.provider.onboarding_checklist_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class OnboardingChecklistItem(
    val text: String,
)

@Composable
public fun OnboardingChecklist(
    modifier: Modifier = Modifier,
    title: String,
    items: ImmutableList<OnboardingChecklistItem>,
) {
    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.onboarding.card.radius)
            )
            .padding(
                horizontal = AppTokens.dp.onboarding.card.horizontalPadding,
                vertical = AppTokens.dp.onboarding.card.verticalPadding
            ),
    ) {
        Text(
            text = title,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        items.forEachIndexed { index, item ->
            ChecklistRow(
                modifier = Modifier.fillMaxWidth(),
                text = item.text,
            )

            if (index < items.lastIndex) {
                Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))
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
                .size(AppTokens.dp.onboarding.checkmark.size)
                .background(
                    color = accent.copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.onboarding.checkmark.size),
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
private fun OnboardingChecklistPreview() {
    PreviewContainer {
        OnboardingChecklist(
            modifier = Modifier.fillMaxWidth(),
            title = AppTokens.strings.res(Res.string.onboarding_checklist_title),
            items = persistentListOf(
                OnboardingChecklistItem(AppTokens.strings.res(Res.string.onboarding_check_profile)),
                OnboardingChecklistItem(AppTokens.strings.res(Res.string.onboarding_check_experience)),
                OnboardingChecklistItem(AppTokens.strings.res(Res.string.onboarding_check_muscles)),
                OnboardingChecklistItem(AppTokens.strings.res(Res.string.onboarding_check_equipment)),
            )
        )
    }
}
