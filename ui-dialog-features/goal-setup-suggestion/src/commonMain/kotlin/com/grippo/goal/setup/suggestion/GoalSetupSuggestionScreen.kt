package com.grippo.goal.setup.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_setup_suggestion_estimate
import com.grippo.design.resources.provider.goal_setup_suggestion_primary_btn
import com.grippo.design.resources.provider.goal_setup_suggestion_secondary_btn
import com.grippo.design.resources.provider.goal_setup_suggestion_subtitle
import com.grippo.design.resources.provider.goal_setup_suggestion_title
import com.grippo.design.resources.provider.icons.Timer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun GoalSetupSuggestionScreen(
    state: GoalSetupSuggestionState,
    loaders: ImmutableSet<GoalSetupSuggestionLoader>,
    contract: GoalSetupSuggestionContract,
) = BaseComposeScreen(
    background = ScreenBackground.Color(AppTokens.colors.background.dialog)
) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.goal_setup_suggestion_title),
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.goal_setup_suggestion_subtitle),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Column(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        state.benefits.forEach { code ->
            val (title, description) = code.text()
            BenefitRow(
                modifier = Modifier.fillMaxWidth(),
                icon = code.icon(),
                title = title,
                description = description,
            )
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

    EstimateRow(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth()
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Row(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Button(
            modifier = Modifier.weight(1f),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.goal_setup_suggestion_secondary_btn),
            ),
            style = ButtonStyle.Secondary,
            onClick = contract::onBack,
        )

        Button(
            modifier = Modifier.weight(1f),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.goal_setup_suggestion_primary_btn),
            ),
            style = ButtonStyle.Primary,
            onClick = contract::onConfigure,
        )
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@Composable
private fun BenefitRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.suggestionCard.radius),
            )
            .padding(
                horizontal = AppTokens.dp.suggestionCard.horizontalPadding,
                vertical = AppTokens.dp.suggestionCard.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = AppTokens.colors.brand.color1.copy(alpha = 0.18f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = icon,
                tint = AppTokens.colors.brand.color1,
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = title,
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )

            Text(
                text = description,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun EstimateRow(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = AppTokens.icons.Timer,
            tint = AppTokens.colors.text.tertiary,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.goal_setup_suggestion_estimate),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun GoalSetupSuggestionScreenPreview() {
    PreviewContainer {
        GoalSetupSuggestionScreen(
            state = GoalSetupSuggestionState(),
            loaders = persistentSetOf(),
            contract = GoalSetupSuggestionContract.Empty,
        )
    }
}
