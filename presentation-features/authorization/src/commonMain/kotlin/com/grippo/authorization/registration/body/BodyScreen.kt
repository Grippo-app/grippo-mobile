package com.grippo.authorization.registration.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputHeight
import com.grippo.design.components.inputs.InputWeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_body_description
import com.grippo.design.resources.registration_body_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun BodyScreen(
    state: BodyState,
    loaders: ImmutableSet<BodyLoader>,
    contract: BodyContract
) = BaseComposeScreen {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = AppTokens.dp.paddings.screenHorizontal,
                vertical = AppTokens.dp.paddings.screenVertical
            ).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(60.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_body_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_body_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(26.dp))

        InputWeight(
            value = state.weight,
            onClick = contract::openWeightPicker
        )

        Spacer(modifier = Modifier.size(16.dp))

        InputHeight(
            value = state.height,
            onClick = contract::openHeightPicker
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.continue_btn),
            style = ButtonStyle.Primary,
            onClick = contract::next
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        BodyScreen(
            state = BodyState(
                weight = 65f,
                height = 144
            ),
            loaders = persistentSetOf(),
            contract = BodyContract.Empty
        )
    }
}