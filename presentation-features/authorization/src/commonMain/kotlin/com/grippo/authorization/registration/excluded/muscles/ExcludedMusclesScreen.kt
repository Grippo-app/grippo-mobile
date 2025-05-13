package com.grippo.authorization.registration.excluded.muscles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.authorization.registration.excluded.muscles.internal.MusclesColumn
import com.grippo.authorization.registration.excluded.muscles.internal.MusclesImage
import com.grippo.authorization.registration.excluded.muscles.internal.MusclesSkeleton
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_muscles_description
import com.grippo.design.resources.registration_muscles_title
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ExcludedMusclesScreen(
    state: ExcludedMusclesState,
    loaders: ImmutableSet<ExcludedMusclesLoader>,
    contract: ExcludedMusclesContract
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = AppTokens.dp.paddings.screenHorizontal,
                vertical = AppTokens.dp.paddings.screenVertical
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(60.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_muscles_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_muscles_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(20.dp))

        if (state.suggestions.isEmpty() && loaders.contains(ExcludedMusclesLoader.MuscleList)) {
            MusclesSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                itemsIndexed(state.suggestions, key = { _, item -> item.id }) { index, group ->
                    val isEven = index % 2 == 0

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = group.name,
                        style = AppTokens.typography.h4(),
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
        }

        Spacer(modifier = Modifier.size(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.continue_btn),
            style = ButtonStyle.Primary,
            onClick = contract::next
        )
    }
}