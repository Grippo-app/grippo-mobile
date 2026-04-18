package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.UserStatsState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cm
import com.grippo.design.resources.provider.icons.Height
import com.grippo.design.resources.provider.icons.Weight
import com.grippo.design.resources.provider.kg
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
public sealed interface UserCardStyle {
    @Immutable
    public data object Preview : UserCardStyle
}

@Composable
public fun UserCard(
    modifier: Modifier = Modifier,
    value: UserState,
    style: UserCardStyle = UserCardStyle.Preview
) {
    val joinedLabel = value.createdAt.display

    val heightUnit = AppTokens.strings.res(Res.string.cm)
    val weightUnit = AppTokens.strings.res(Res.string.kg)

    val heightDisplay = value.height.display.takeIf { it.isNotBlank() }
    val weightDisplay = value.weight.display.takeIf { it.isNotBlank() }

    val experienceColor = value.experience.color()
    val experienceIcon = value.experience.icon()
    val experienceTitle = value.experience.title().text()

    val heightIcon = AppTokens.icons.Height
    val weightIcon = AppTokens.icons.Weight

    val stats: ImmutableList<StatEntry> = remember(
        heightDisplay, weightDisplay, joinedLabel,
        heightUnit, weightUnit, heightIcon, weightIcon,
    ) {
        if (heightDisplay == null && weightDisplay == null && joinedLabel.isBlank()) {
            return@remember persistentListOf()
        }
        buildList(capacity = 3) {
            if (heightDisplay != null) {
                add(StatEntry(icon = heightIcon, text = "$heightDisplay $heightUnit"))
            }
            if (weightDisplay != null) {
                add(StatEntry(icon = weightIcon, text = "$weightDisplay $weightUnit"))
            }
            if (joinedLabel.isNotBlank()) {
                add(StatEntry(icon = null, text = joinedLabel))
            }
        }.toPersistentList()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ExperienceBadge(
            text = experienceTitle,
            icon = experienceIcon,
            color = experienceColor,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        Text(
            text = value.name,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

        Text(
            text = value.email,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        if (stats.isNotEmpty()) {

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            StatsStrip(stats = stats)
        }
    }
}

@Composable
private fun ExperienceBadge(
    text: String,
    icon: ImageVector,
    color: Color,
) {
    Row(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(AppTokens.dp.userCard.experience.radius)
            )
            .padding(
                horizontal = AppTokens.dp.userCard.experience.horizontalPadding,
                vertical = AppTokens.dp.userCard.experience.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.experience.space)
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.userCard.experience.icon),
            imageVector = icon,
            contentDescription = null,
            tint = color,
        )

        Text(
            text = text,
            style = AppTokens.typography.b13Semi(),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatsStrip(stats: ImmutableList<StatEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.userCard.stats.radius)
            )
            .height(intrinsicSize = IntrinsicSize.Min)
            .padding(vertical = AppTokens.dp.userCard.stats.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEachIndexed { index, stat ->
            StatItem(
                modifier = Modifier.weight(1f),
                icon = stat.icon,
                text = stat.text,
            )

            if (index < stats.lastIndex) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = AppTokens.dp.userCard.stats.dividerWidth,
                    color = AppTokens.colors.divider.default
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    text: String,
) {
    Row(
        modifier = modifier.padding(horizontal = AppTokens.dp.userCard.stats.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.userCard.stats.icon),
                imageVector = icon,
                contentDescription = null,
                tint = AppTokens.colors.icon.secondary,
            )

            Spacer(Modifier.width(AppTokens.dp.userCard.stats.space))
        }

        Text(
            text = text,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Immutable
private data class StatEntry(
    val icon: ImageVector?,
    val text: String,
)

@AppPreview
@Composable
private fun UserCardAdvancedPreview() {
    PreviewContainer {
        UserCard(
            value = stubUser().copy(
                experience = ExperienceEnumState.ADVANCED
            ),
            style = UserCardStyle.Preview
        )

        UserCard(
            value = stubUser().copy(
                experience = ExperienceEnumState.ADVANCED
            ).copy(
                stats = UserStatsState(
                    totalVolume = VolumeFormatState.Empty(),
                    totalRepetitions = RepetitionsFormatState.Empty(),
                    totalDuration = DurationFormatState.Empty(),
                    trainingsCount = 0
                )
            ),
        )
    }
}

@AppPreview
@Composable
private fun UserCardProPreview() {
    PreviewContainer {
        UserCard(
            value = stubUser().copy(
                experience = ExperienceEnumState.PRO
            ),
            style = UserCardStyle.Preview
        )

        UserCard(
            value = stubUser().copy(
                experience = ExperienceEnumState.PRO
            ).copy(
                stats = UserStatsState(
                    totalVolume = VolumeFormatState.Empty(),
                    totalRepetitions = RepetitionsFormatState.Empty(),
                    totalDuration = DurationFormatState.Empty(),
                    trainingsCount = 0
                )
            ),
        )
    }
}

@AppPreview
@Composable
private fun UserCardIntermediatePreview() {
    PreviewContainer {
        UserCard(
            value = stubUser().copy(
                experience = ExperienceEnumState.INTERMEDIATE
            ),
            style = UserCardStyle.Preview
        )

        UserCard(
            value = stubUser().copy(
                experience = ExperienceEnumState.INTERMEDIATE
            ).copy(
                stats = UserStatsState(
                    totalVolume = VolumeFormatState.Empty(),
                    totalRepetitions = RepetitionsFormatState.Empty(),
                    totalDuration = DurationFormatState.Empty(),
                    trainingsCount = 0
                )
            ),
        )
    }
}

@AppPreview
@Composable
private fun UserCardBeginnerPreview() {
    PreviewContainer {
        UserCard(
            value = stubUser().copy(
                experience = ExperienceEnumState.BEGINNER
            ),
            style = UserCardStyle.Preview
        )

        UserCard(
            value = stubUser().copy(
                experience = ExperienceEnumState.BEGINNER
            ).copy(
                stats = UserStatsState(
                    totalVolume = VolumeFormatState.Empty(),
                    totalRepetitions = RepetitionsFormatState.Empty(),
                    totalDuration = DurationFormatState.Empty(),
                    trainingsCount = 0
                )
            ),
        )
    }
}
