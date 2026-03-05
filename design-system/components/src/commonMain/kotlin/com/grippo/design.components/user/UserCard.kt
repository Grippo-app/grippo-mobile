package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.grippo.design.resources.provider.kg
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat

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
    val joinedLabel = DateCompose.rememberFormat(
        value = value.createdAt,
        format = DateFormat.DateOnly.DateMmmDdYyyy
    )

    val heightUnit = AppTokens.strings.res(Res.string.cm)
    val weightUnit = AppTokens.strings.res(Res.string.kg)

    val heightDisplay = value.height.display.takeIf { it.isNotBlank() }
    val weightDisplay = value.weight.display.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.name,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = value.email,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (heightDisplay != null || weightDisplay != null || joinedLabel.isNotBlank()) {
            Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.chip.space),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.chip.space)
            ) {
                UserInfoChip(
                    text = value.experience.title().text(),
                    icon = value.experience.icon()
                )

                heightDisplay?.let {
                    UserInfoChip(
                        text = "$it $heightUnit",
                        icon = null
                    )
                }
                weightDisplay?.let {
                    UserInfoChip(
                        text = "$it $weightUnit",
                        icon = null
                    )
                }
                if (joinedLabel.isNotBlank()) {
                    UserInfoChip(
                        text = joinedLabel,
                        icon = null
                    )
                }
            }
        }
    }
}

@Composable
private fun UserInfoChip(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector?
) {
    Row(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.userCard.chip.radius)
            )
            .padding(
                horizontal = AppTokens.dp.userCard.chip.horizontalPadding,
                vertical = AppTokens.dp.userCard.chip.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.chip.space)
    ) {
        icon?.let {
            Icon(
                modifier = Modifier.size(AppTokens.dp.userCard.chip.icon),
                imageVector = it,
                contentDescription = null,
                tint = AppTokens.colors.icon.secondary
            )
        }

        Text(
            text = text,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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