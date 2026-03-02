package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cm
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.edit_btn
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.math.roundToInt

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
    val cardShape = RoundedCornerShape(AppTokens.dp.userCard.layout.radius)
    val accentColor = value.experience.color()
    val stats = value.stats
    val joinedLabel = remember(value.createdAt) {
        DateTimeUtils.format(value.createdAt, DateFormat.DateOnly.DateMmmDdYyyy)
    }

    val heightUnit = AppTokens.strings.res(Res.string.cm)
    val weightUnit = AppTokens.strings.res(Res.string.kg)
    val trainingsLabel = AppTokens.strings.res(Res.string.trainings)
    val durationLabel = AppTokens.strings.res(Res.string.duration)
    val volumeLabel = AppTokens.strings.res(Res.string.volume)
    val repetitionsLabel = AppTokens.strings.res(Res.string.repetitions)

    val physicalLabel =
        remember(value.height.display, value.weight.display, heightUnit, weightUnit) {
            listOfNotNull(
                value.height.value?.let { "$it $heightUnit" },
                value.weight.value?.let { "${formatDecimal(it)} $weightUnit" }
            ).joinToString(separator = " • ")
        }
    val summaryLine = remember(physicalLabel, joinedLabel) {
        listOfNotNull(
            physicalLabel.takeIf { it.isNotBlank() },
            joinedLabel.takeIf { it.isNotBlank() }
        ).joinToString(separator = " • ")
    }
    val footerLine = remember(
        stats,
        volumeLabel,
        repetitionsLabel,
        weightUnit
    ) {
        stats?.let {
            "$volumeLabel: ${formatVolume(it.totalVolume.value, weightUnit)} • " +
                    "$repetitionsLabel: ${formatCount(it.totalRepetitions.value ?: 0)}"
        }
    }

    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = cardShape
            )
            .padding(
                horizontal = AppTokens.dp.userCard.layout.horizontalPadding,
                vertical = AppTokens.dp.userCard.layout.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.layout.content)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.layout.content),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.layout.subContent)
            ) {
                UserMetaChip(
                    title = value.experience.title().text(),
                    icon = value.experience.icon(),
                    accentColor = accentColor
                )

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = value.name,
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = value.email,
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (style is UserCardStyle.Interactive) {
                Button(
                    style = ButtonStyle.Transparent,
                    size = ButtonSize.Small,
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.edit_btn)
                    ),
                    onClick = style.onEditClick,
                    textStyle = AppTokens.typography.b12Semi()
                )
            }
        }

        if (summaryLine.isNotBlank() || stats != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = accentColor.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(AppTokens.dp.userCard.summary.radius)
                    )
                    .padding(
                        horizontal = AppTokens.dp.userCard.summary.horizontalPadding,
                        vertical = AppTokens.dp.userCard.summary.verticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.summary.space)
            ) {
                if (summaryLine.isNotBlank()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = summaryLine,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (stats != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.summary.space)
                    ) {
                        UserMetricPanel(
                            modifier = Modifier.weight(1f),
                            label = trainingsLabel,
                            value = formatCount(stats.trainingsCount),
                            accentColor = accentColor
                        )

                        UserMetricPanel(
                            modifier = Modifier.weight(1f),
                            label = durationLabel,
                            value = DateTimeUtils.format(stats.totalDuration),
                            accentColor = accentColor
                        )
                    }
                }

                if (footerLine != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = footerLine,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMetaChip(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    accentColor: Color,
) {
    Row(
        modifier = modifier
            .background(
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(AppTokens.dp.userCard.chip.radius)
            )
            .padding(
                horizontal = AppTokens.dp.userCard.chip.horizontalPadding,
                vertical = AppTokens.dp.userCard.chip.verticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.chip.space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.userCard.chip.icon),
            imageVector = icon,
            contentDescription = null,
            tint = accentColor
        )

        Text(
            text = title,
            style = AppTokens.typography.b11Semi(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UserMetricPanel(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
) {
    Column(
        modifier = modifier
            .background(
                color = accentColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(AppTokens.dp.userCard.highlight.radius)
            )
            .padding(
                horizontal = AppTokens.dp.userCard.highlight.horizontalPadding,
                vertical = AppTokens.dp.userCard.highlight.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.highlight.space)
    ) {
        Text(
            text = label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = value,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatCount(value: Int): String {
    return value.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()
}

private fun formatVolume(value: Float?, unit: String): String {
    val normalized = value
        ?.times(10f)
        ?.roundToInt()
        ?.div(10f)
        ?: 0f
    val intPart = normalized.toInt()
    val fraction = ((normalized - intPart) * 10f).roundToInt()
    val number = if (fraction == 0) {
        formatCount(intPart)
    } else {
        "${formatCount(intPart)},$fraction"
    }
    return "$number $unit"
}

private fun formatDecimal(value: Float): String {
    val rounded = value
        .times(10f)
        .roundToInt() / 10f
    val text = rounded.toString()
    return if (text.endsWith(".0")) {
        text.removeSuffix(".0")
    } else {
        text
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
