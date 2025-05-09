package com.grippo.authorization.registration.missing.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_equipment_description
import com.grippo.design.resources.registration_equipment_title
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun MissingEquipmentScreen(
    state: MissingEquipmentState,
    loaders: ImmutableSet<MissingEquipmentLoader>,
    contract: MissingEquipmentContract
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = AppTokens.dp.paddings.screenHorizontal,
                vertical = AppTokens.dp.paddings.screenVertical
            ),
    ) {

        Spacer(modifier = Modifier.size(60.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_equipment_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_equipment_description),
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
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            items(state.suggestions, key = { it.id }) { group ->

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = group.name,
                    style = AppTokens.typography.h4(),
                    textAlign = TextAlign.Center,
                    color = AppTokens.colors.text.primary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                EquipmentsColumn(
                    modifier = Modifier.fillMaxWidth(),
                    item = group,
                    selectedIds = state.selectedEquipmentIds,
                    onSelect = contract::select
                )
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