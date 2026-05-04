package com.grippo.design.components.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.user.internal.UserCardCompact
import com.grippo.design.components.user.internal.UserCardDetailed
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed interface UserCardStyle {

    @Immutable
    public data object Compact : UserCardStyle

    @Immutable
    public data object Detailed : UserCardStyle
}

@Composable
public fun UserCard(
    modifier: Modifier = Modifier,
    value: UserState,
    style: UserCardStyle,
) {
    when (style) {
        UserCardStyle.Compact -> UserCardCompact(
            modifier = modifier,
            value = value,
        )

        UserCardStyle.Detailed -> UserCardDetailed(
            modifier = modifier,
            value = value,
        )
    }
}

@AppPreview
@Composable
private fun UserCardPreview() {
    PreviewContainer {
        UserCard(
            value = stubUser(),
            style = UserCardStyle.Compact
        )

        UserCard(
            value = stubUser(),
            style = UserCardStyle.Detailed
        )
    }
}
