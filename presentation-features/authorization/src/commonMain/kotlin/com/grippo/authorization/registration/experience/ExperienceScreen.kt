package com.grippo.authorization.registration.experience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.SelectableCard
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_experience_description
import com.grippo.design.resources.registration_experience_title
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ExperienceScreen(
    state: ExperienceState,
    loaders: ImmutableSet<ExperienceLoader>,
    contract: ExperienceContract
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = AppTokens.dp.paddings.screenHorizontal,
                vertical = AppTokens.dp.paddings.screenVertical
            ).imePadding(),
    ) {

        Spacer(modifier = Modifier.size(60.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_experience_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_experience_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(20.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {

            items(state.suggestions, key = { it.ordinal }) { item ->
                val selectProvider = remember { { contract.select(item) } }
                val isSelected = remember(state.selected) { state.selected == item }

                SelectableCard(
                    modifier = Modifier.fillMaxWidth(),
                    select = selectProvider,
                    isSelected = isSelected,
                    title = item.title(),
                    description = item.description(),
                    icon = item.icon()
                )
            }
        }

        val buttonState = remember(loaders, state.selected) {
            when {
                state.selected != null -> ButtonState.Enabled
                else -> ButtonState.Disabled
            }
        }

        Spacer(modifier = Modifier.size(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.continue_btn),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::next
        )
    }
}