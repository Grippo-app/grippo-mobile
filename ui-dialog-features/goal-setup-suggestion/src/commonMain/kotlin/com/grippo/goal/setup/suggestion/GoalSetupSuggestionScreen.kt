package com.grippo.goal.setup.suggestion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.benefit.BenefitCard
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_setup_suggestion_primary_btn
import com.grippo.design.resources.provider.goal_setup_suggestion_secondary_btn
import com.grippo.design.resources.provider.goal_setup_suggestion_subtitle
import com.grippo.design.resources.provider.goal_setup_suggestion_title
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
            BenefitCard(
                modifier = Modifier.fillMaxWidth(),
                icon = code.icon(),
                title = title,
                subtitle = description,
                tint = AppTokens.colors.brand.color1,
            )
        }
    }

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
            onClick = contract::onLater,
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
