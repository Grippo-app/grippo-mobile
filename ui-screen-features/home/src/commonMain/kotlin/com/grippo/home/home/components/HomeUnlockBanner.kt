package com.grippo.home.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.FormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.profile.UserStatsState
import com.grippo.design.components.banner.BannerCard
import com.grippo.design.components.banner.BannerCardStyle
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.home_unlock_set_goal_cta
import com.grippo.design.resources.provider.home_unlock_set_goal_description
import com.grippo.design.resources.provider.home_unlock_set_goal_title
import com.grippo.design.resources.provider.icons.Lock
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import com.grippo.home.home.HomeUnlock
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun HomeUnlockBanner(
    stats: UserStatsState,
    hasGoal: Boolean,
    onAddGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifetimeCount = stats.trainingsCount
    if (!HomeUnlock.shouldShowBanner(lifetimeCount = lifetimeCount, hasGoal = hasGoal)) return

    val isNewUser = lifetimeCount < HomeUnlock.NEW_USER_THRESHOLD
    val showGoalSection = !hasGoal

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        if (isNewUser) {
            LifetimeStatsRow(
                modifier = Modifier.fillMaxWidth(),
                stats = stats,
            )

            HomeUnlock.entries
                .filterNot { it.isUnlocked(lifetimeCount) }
                .forEach { milestone ->
                    MilestoneCard(
                        title = AppTokens.strings.res(milestone.titleRes),
                        description = AppTokens.strings.res(milestone.descriptionRes),
                        progressLabel = "$lifetimeCount / ${milestone.requiredLifetimeCount}",
                    )
                }
        }

        if (showGoalSection) {
            BannerCard(
                modifier = Modifier.fillMaxWidth(),
                style = BannerCardStyle.Custom(AppTokens.colors.text.tertiary),
                icon = AppTokens.icons.Lock,
                title = AppTokens.strings.res(Res.string.home_unlock_set_goal_title),
                description = AppTokens.strings.res(Res.string.home_unlock_set_goal_description),
                trailing = {
                    Button(
                        onClick = onAddGoal,
                        style = ButtonStyle.Secondary,
                        size = ButtonSize.Small,
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.home_unlock_set_goal_cta),
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun MilestoneCard(
    title: String,
    description: String,
    progressLabel: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppTokens.icons.Lock,
) {
    BannerCard(
        modifier = modifier.fillMaxWidth(),
        style = BannerCardStyle.Custom(AppTokens.colors.text.tertiary),
        icon = icon,
        title = title,
        description = description,
        trailing = {
            Text(
                modifier = Modifier
                    .background(
                        AppTokens.colors.text.tertiary.copy(alpha = 0.14f),
                        CircleShape,
                    )
                    .padding(
                        horizontal = AppTokens.dp.contentPadding.subContent,
                        vertical = AppTokens.dp.contentPadding.text,
                    ),
                text = progressLabel,
                style = AppTokens.typography.b11Semi(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun LifetimeStatsRow(
    modifier: Modifier = Modifier,
    stats: UserStatsState,
) {
    val primary = AppTokens.colors.text.primary
    val tertiary = AppTokens.colors.text.tertiary

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.Top,
    ) {
        StatsCell(
            modifier = Modifier.weight(1f),
            value = stats.trainingsCount.toString(),
            valueColor = if (stats.trainingsCount == 0) tertiary else primary,
            label = AppTokens.strings.res(Res.string.trainings),
        )
        StatsCell(
            modifier = Modifier.weight(1f),
            value = stats.totalVolume.short(),
            valueColor = if (stats.totalVolume is FormatState.Empty<*>) tertiary else primary,
            label = AppTokens.strings.res(Res.string.volume),
        )
        StatsCell(
            modifier = Modifier.weight(1f),
            value = stats.totalDuration.display.ifBlank { "—" },
            valueColor = if (stats.totalDuration is FormatState.Empty<*>) tertiary else primary,
            label = AppTokens.strings.res(Res.string.duration),
        )
    }
}

@Composable
private fun StatsCell(
    modifier: Modifier = Modifier,
    value: String,
    valueColor: Color,
    label: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = value,
            style = AppTokens.typography.h5(),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun previewStats(
    trainingsCount: Int,
    totalVolumeKg: Float,
    totalDurationMinutes: Long,
): UserStatsState = UserStatsState(
    trainingsCount = trainingsCount,
    totalDuration = DurationFormatState.of(totalDurationMinutes.minutes),
    totalVolume = VolumeFormatState.of(totalVolumeKg),
    totalRepetitions = RepetitionsFormatState.of(0),
)

@AppPreview
@Composable
private fun HomeUnlockBannerZeroTrainingsPreview() {
    PreviewContainer {
        HomeUnlockBanner(
            stats = previewStats(
                trainingsCount = 0,
                totalVolumeKg = 0f,
                totalDurationMinutes = 0,
            ),
            hasGoal = false,
            onAddGoal = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeUnlockBannerNewUserPreview() {
    PreviewContainer {
        HomeUnlockBanner(
            stats = previewStats(
                trainingsCount = 1,
                totalVolumeKg = 3_240f,
                totalDurationMinutes = 47,
            ),
            hasGoal = false,
            onAddGoal = {},
        )
    }
}

// Verifies `filterNot { isUnlocked }` hides DurationTrend once it's unlocked,
// keeping only the still-locked PerformanceTrends milestone.
@AppPreview
@Composable
private fun HomeUnlockBannerDurationUnlockedPreview() {
    PreviewContainer {
        HomeUnlockBanner(
            stats = previewStats(
                trainingsCount = 2,
                totalVolumeKg = 6_800f,
                totalDurationMinutes = 95,
            ),
            hasGoal = false,
            onAddGoal = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeUnlockBannerZeroTrainingsHasGoalPreview() {
    PreviewContainer {
        HomeUnlockBanner(
            stats = previewStats(
                trainingsCount = 0,
                totalVolumeKg = 0f,
                totalDurationMinutes = 0,
            ),
            hasGoal = true,
            onAddGoal = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeUnlockBannerNewUserHasGoalPreview() {
    PreviewContainer {
        HomeUnlockBanner(
            stats = previewStats(
                trainingsCount = 2,
                totalVolumeKg = 6_800f,
                totalDurationMinutes = 95,
            ),
            hasGoal = true,
            onAddGoal = {},
        )
    }
}

// Layout stress test: large numbers exercise `short()` compaction,
// multi-hour duration display, and ellipsis.
@AppPreview
@Composable
private fun HomeUnlockBannerHighVolumePreview() {
    PreviewContainer {
        HomeUnlockBanner(
            stats = previewStats(
                trainingsCount = 2,
                totalVolumeKg = 1_250_000f,
                totalDurationMinutes = 9_600,
            ),
            hasGoal = false,
            onAddGoal = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeUnlockBannerGoalOnlyPreview() {
    PreviewContainer {
        HomeUnlockBanner(
            stats = previewStats(
                trainingsCount = 10,
                totalVolumeKg = 32_400f,
                totalDurationMinutes = 480,
            ),
            hasGoal = false,
            onAddGoal = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeUnlockBannerVeteranNoGoalPreview() {
    PreviewContainer {
        HomeUnlockBanner(
            stats = previewStats(
                trainingsCount = 487,
                totalVolumeKg = 5_400_000f,
                totalDurationMinutes = 86_400,
            ),
            hasGoal = false,
            onAddGoal = {},
        )
    }
}
