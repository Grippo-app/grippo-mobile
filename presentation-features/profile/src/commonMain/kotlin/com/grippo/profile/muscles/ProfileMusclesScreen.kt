package com.grippo.profile.muscles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.muscle.MusclesColumn
import com.grippo.design.components.muscle.MusclesImage
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.apply_btn
import com.grippo.design.resources.muscles
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ProfileMusclesScreen(
    state: ProfileMusclesState,
    loaders: ImmutableSet<ProfileMusclesLoader>,
    contract: ProfileMusclesContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.muscles),
        onBack = contract::back
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        itemsIndexed(
            state.suggestions,
            key = { _, item -> item.id }) { index, group ->
            val isEven = index % 2 == 0

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = group.name,
                style = AppTokens.typography.h3(),
                textAlign = TextAlign.Center,
                color = AppTokens.colors.text.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEven) {
                    MusclesColumn(
                        modifier = Modifier.weight(1f),
                        item = group,
                        selectedIds = state.selectedMuscleIds,
                        onSelect = contract::select
                    )
                    MusclesImage(
                        modifier = Modifier.weight(1f),
                        item = group,
                        selectedIds = state.selectedMuscleIds
                    )
                } else {
                    MusclesImage(
                        modifier = Modifier.weight(1f),
                        item = group,
                        selectedIds = state.selectedMuscleIds
                    )
                    MusclesColumn(
                        modifier = Modifier.weight(1f),
                        item = group,
                        selectedIds = state.selectedMuscleIds,
                        onSelect = contract::select
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val buttonState = remember(loaders) {
        when {
            loaders.contains(ProfileMusclesLoader.ApplyButton) -> ButtonState.Loading
            else -> ButtonState.Enabled
        }
    }

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.apply_btn),
        style = ButtonStyle.Primary,
        state = buttonState,
        onClick = contract::apply
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
}