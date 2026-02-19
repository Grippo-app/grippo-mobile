package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.account_deletion_description
import com.grippo.design.resources.provider.danger_zone
import com.grippo.design.resources.provider.delete_account

@Composable
public fun DeleteUserCard(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.semantic.error.copy(alpha = 0.2f),
                shape = RoundedCornerShape(AppTokens.dp.deleteUserCard.radius)
            )
            .padding(
                horizontal = AppTokens.dp.deleteUserCard.horizontalPadding,
                vertical = AppTokens.dp.deleteUserCard.verticalPadding
            ),
    ) {

        Text(
            text = AppTokens.strings.res(Res.string.danger_zone),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.semantic.error,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            text = AppTokens.strings.res(Res.string.account_deletion_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

        val deleteState = remember(isLoading) {
            when {
                isLoading -> ButtonState.Loading
                else -> ButtonState.Enabled
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Error,
            state = deleteState,
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.delete_account)
            ),
            onClick = onDeleteClick
        )
    }
}

@AppPreview
@Composable
private fun DeleteUserCardPreview() {
    PreviewContainer {
        DeleteUserCard(
            isLoading = true,
            onDeleteClick = {}
        )

        DeleteUserCard(
            isLoading = false,
            onDeleteClick = {}
        )
    }
}