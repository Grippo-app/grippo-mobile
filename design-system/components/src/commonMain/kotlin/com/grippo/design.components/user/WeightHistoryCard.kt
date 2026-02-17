package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.profile.WeightHistoryState
import com.grippo.core.state.profile.stubWeightHistory
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat

@Composable
public fun WeightHistoryCard(
    modifier: Modifier = Modifier,
    value: WeightHistoryState,
) {
    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.weightHistoryCard.radius)
            )
            .padding(
                horizontal = AppTokens.dp.weightHistoryCard.horizontalPadding,
                vertical = AppTokens.dp.weightHistoryCard.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        val formattedDate = DateCompose.rememberFormat(
            value.createdAt.date,
            DateFormat.DateOnly.DateMmmDdYyyy
        )

        Text(
            text = formattedDate,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = value.value.display,
                style = AppTokens.typography.h6(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

            Text(
                text = AppTokens.strings.res(Res.string.kg),
                style = AppTokens.typography.h6(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@AppPreview
@Composable
private fun WeightHistoryCardPreview() {
    PreviewContainer {
        WeightHistoryCard(
            value = stubWeightHistory(),
        )
    }
}