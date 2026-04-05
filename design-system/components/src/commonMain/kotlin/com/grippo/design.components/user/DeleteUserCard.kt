package com.grippo.design.components.user

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
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
    isDeleteLoading: Boolean,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = modifier
            .border(
                width = 1.dp,
                color = AppTokens.colors.semantic.error.copy(alpha = 0.5f),
                shape = RoundedCornerShape(AppTokens.dp.deleteUserCard.radius)
            )
            .padding(
                horizontal = AppTokens.dp.deleteUserCard.horizontalPadding,
                vertical = AppTokens.dp.deleteUserCard.verticalPadding
            ),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.danger_zone),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.semantic.error,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            val deleteState = remember(isDeleteLoading) {
                when {
                    isDeleteLoading -> ButtonState.Loading
                    else -> ButtonState.Enabled
                }
            }

            Button(
                modifier = Modifier,
                style = ButtonStyle.Error,
                state = deleteState,
                size = ButtonSize.Small,
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.delete_account)
                ),
                onClick = onDeleteClick
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            text = AppTokens.strings.res(Res.string.account_deletion_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@AppPreview
@Composable
private fun DeleteUserCardPreview() {
    PreviewContainer {
        DeleteUserCard(
            isDeleteLoading = true,
            onDeleteClick = {}
        )

        DeleteUserCard(
            isDeleteLoading = false,
            onDeleteClick = {}
        )
    }
}