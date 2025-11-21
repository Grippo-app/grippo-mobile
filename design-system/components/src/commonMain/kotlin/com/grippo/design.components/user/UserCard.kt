package com.grippo.design.components.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed interface UserCardStyle {
    @Immutable
    public data object Preview : UserCardStyle

    @Immutable
    public data class Interactive(
        val onEditClick: () -> Unit
    ) : UserCardStyle
}

@Composable
public fun UserCard(
    modifier: Modifier = Modifier,
    value: UserState,
    style: UserCardStyle = UserCardStyle.Preview
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "\uD83D\uDE0E",
            style = AppTokens.typography.h1(),
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        BodyItem(
            weight = value.weight,
            height = value.height
        )
    }
}

@AppPreview
@Composable
private fun UserCardPreview() {
    PreviewContainer {
        UserCard(
            value = stubUser(),
            style = UserCardStyle.Preview
        )

        UserCard(
            value = stubUser(),
            style = UserCardStyle.Interactive(onEditClick = {})
        )
    }
}