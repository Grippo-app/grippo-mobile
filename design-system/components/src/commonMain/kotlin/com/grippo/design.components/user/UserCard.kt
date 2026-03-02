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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.UserStatsState
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
import com.grippo.design.resources.provider.home_empty_subtitle
import com.grippo.design.resources.provider.home_empty_title
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat

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
    val accentColor = value.experience.color()
    val stats = value.stats

    val joinedLabel = DateCompose.rememberFormat(
        value = value.createdAt,
        format = DateFormat.DateOnly.DateMmmDdYyyy
    )

    val heightUnit = AppTokens.strings.res(Res.string.cm)
    val weightUnit = AppTokens.strings.res(Res.string.kg)
    val trainingsLabel = AppTokens.strings.res(Res.string.trainings)
    val durationLabel = AppTokens.strings.res(Res.string.duration)
    val volumeLabel = AppTokens.strings.res(Res.string.volume)
    val repetitionsLabel = AppTokens.strings.res(Res.string.repetitions)
    val emptyTitle = AppTokens.strings.res(Res.string.home_empty_title)
    val emptySubtitle = AppTokens.strings.res(Res.string.home_empty_subtitle)

    val hasDuration = stats.totalDuration.display.isNotBlank()
    val hasVolume = stats.totalVolume.display.isNotBlank()
    val hasRepetitions = stats.totalRepetitions.display.isNotBlank()
    val hasStatsProgress = stats.trainingsCount > 0 || hasDuration || hasVolume || hasRepetitions

    val physicalSummary = listOfNotNull(
        value.height.display
            .takeIf { it.isNotBlank() }
            ?.let { "$it $heightUnit" },
        value.weight.display
            .takeIf { it.isNotBlank() }
            ?.let { "$it $weightUnit" }
    ).joinToString(separator = " • ")

    val summaryLine = listOfNotNull(
        physicalSummary.takeIf { it.isNotBlank() },
        joinedLabel.takeIf { it.isNotBlank() }
    ).joinToString(separator = " • ")

    val volumeValue = if (hasVolume) stats.totalVolume.short() else null
    val repetitionsValue = if (hasRepetitions) stats.totalRepetitions.short() else null

    val primaryMetrics = buildList<Pair<String, String>> {
        if (stats.trainingsCount > 0) {
            add(trainingsLabel to stats.trainingsCount.toString())
        }
        if (hasDuration) {
            add(durationLabel to stats.totalDuration.display)
        }
    }

    val footerLine = listOfNotNull(
        volumeValue?.let { "$volumeLabel $it" },
        repetitionsValue?.let { "$repetitionsLabel $it" }
    ).joinToString(separator = " • ")

    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.userCard.layout.radius)
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTokens.colors.text.primary.copy(alpha = 0.03f),
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

            if (hasStatsProgress) {
                when (primaryMetrics.size) {
                    0 -> Unit

                    1 -> {
                        val (label, metricValue) = primaryMetrics.first()
                        UserMetricPanel(
                            modifier = Modifier.fillMaxWidth(),
                            label = label,
                            value = metricValue
                        )
                    }

                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.summary.space)
                        ) {
                            primaryMetrics.take(2).forEach { (label, metricValue) ->
                                UserMetricPanel(
                                    modifier = Modifier.weight(1f),
                                    label = label,
                                    value = metricValue
                                )
                            }
                        }
                    }
                }

                if (footerLine.isNotBlank()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = footerLine,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                UserEmptyState(
                    title = emptyTitle,
                    subtitle = emptySubtitle
                )
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
                color = AppTokens.colors.text.primary.copy(alpha = 0.05f),
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
) {
    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.text.primary.copy(alpha = 0.04f),
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

@Composable
private fun UserEmptyState(
    title: String,
    subtitle: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.empty.space)
    ) {
        Text(
            text = title,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = subtitle,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
            value = stubUser()
                .copy(
                    stats = UserStatsState(
                        totalVolume = VolumeFormatState.Empty(),
                        totalRepetitions = RepetitionsFormatState.Empty(),
                        totalDuration = DurationFormatState.Empty(),
                        trainingsCount = 0
                    )
                ),
            style = UserCardStyle.Interactive(onEditClick = {})
        )
    }
}
