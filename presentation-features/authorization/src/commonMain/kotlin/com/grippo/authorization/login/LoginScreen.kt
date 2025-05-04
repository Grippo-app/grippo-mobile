package com.grippo.authorization.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputEmail
import com.grippo.design.components.inputs.InputPassword
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.login_description
import com.grippo.design.resources.login_title
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun LoginScreen(
    state: LoginState,
    loaders: ImmutableSet<LoginLoader>,
    contract: LoginContract
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
            text = AppTokens.strings.res(Res.string.login_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = AppTokens.strings.res(Res.string.login_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(modifier = Modifier.size(26.dp))

        InputEmail(
            value = state.email.value,
            onValueChange = contract::setEmail
        )

        Spacer(modifier = Modifier.size(16.dp))

        InputPassword(
            value = state.password.value,
            onValueChange = contract::setPassword
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = "SignIn",
            state = ButtonState.Loading,
            style = ButtonStyle.Primary,
            onClick = {}
        )
    }
}