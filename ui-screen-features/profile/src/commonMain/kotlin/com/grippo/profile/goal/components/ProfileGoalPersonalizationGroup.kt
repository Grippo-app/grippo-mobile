package com.grippo.profile.goal.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_options_collapsed
import com.grippo.design.resources.provider.goal_options_expanded
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.design.resources.provider.icons.ArrowTop
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private const val DisclosureAnimationDurationMillis = 200

@Composable
internal fun ProfileGoalPersonalizationGroup(
    modifier: Modifier = Modifier,
    category: PersonalizationKeyEnumState.Category,
    items: ImmutableList<PersonalizationKeyEnumState>,
    selected: ImmutableList<PersonalizationKeyEnumState>,
    isExpanded: Boolean,
    isExpandable: Boolean = true,
    onExpand: () -> Unit,
    onSelect: (PersonalizationKeyEnumState) -> Unit,
) {
    val expansionDescription = AppTokens.strings.res(
        if (isExpanded) Res.string.goal_options_expanded else Res.string.goal_options_collapsed,
    )
    val headingModifier = if (isExpandable) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = AppTokens.dp.menu.item.minHeight)
            .semantics { stateDescription = expansionDescription }
            .scalableClick(role = Role.Button, onClick = onExpand)
    } else {
        Modifier.fillMaxWidth()
    }
    val selectedLabels = items.filter { it in selected }.map { it.label() }
    val disclosureEnter = expandVertically(
        animationSpec = tween(DisclosureAnimationDurationMillis),
        expandFrom = Alignment.Top,
    ) + fadeIn(animationSpec = tween(DisclosureAnimationDurationMillis))
    val disclosureExit = shrinkVertically(
        animationSpec = tween(DisclosureAnimationDurationMillis),
        shrinkTowards = Alignment.Top,
    ) + fadeOut(animationSpec = tween(DisclosureAnimationDurationMillis))

    Column(modifier = modifier) {
        Row(
            modifier = headingModifier.padding(vertical = AppTokens.dp.menu.item.verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = category.label(),
                    style = AppTokens.typography.b14Semi(),
                    color = AppTokens.colors.text.primary,
                )
                AnimatedVisibility(
                    visible = !isExpanded && selectedLabels.isNotEmpty(),
                    enter = disclosureEnter,
                    exit = disclosureExit,
                ) {
                    Text(
                        modifier = Modifier.padding(top = AppTokens.dp.contentPadding.text),
                        text = selectedLabels.joinToString(separator = ", "),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
            if (isExpandable) {
                Icon(
                    modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                    imageVector = if (isExpanded) AppTokens.icons.ArrowTop else AppTokens.icons.ArrowDown,
                    contentDescription = null,
                    tint = AppTokens.colors.icon.secondary,
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = disclosureEnter,
            exit = disclosureExit,
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppTokens.dp.contentPadding.content),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                items.forEach { item ->
                    key(item) {
                        CheckSelectableCard(
                            style = CheckSelectableCardStyle.Small(title = item.label()),
                            isSelected = item in selected,
                            onSelect = { onSelect(item) },
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ProfileGoalPersonalizationEmptyPreview() {
    PreviewContainer {
        ProfileGoalPersonalizationGroup(
            category = PersonalizationKeyEnumState.Category.SCHEDULE,
            items = PersonalizationKeyEnumState.entries
                .filter { it.category() == PersonalizationKeyEnumState.Category.SCHEDULE }
                .toPersistentList(),
            selected = persistentListOf(),
            isExpanded = false,
            onExpand = {},
            onSelect = {},
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalPersonalizationLongSummaryPreview() {
    PreviewContainer {
        ProfileGoalPersonalizationGroup(
            category = PersonalizationKeyEnumState.Category.MOVEMENT_RESTRICTIONS,
            items = PersonalizationKeyEnumState.entries
                .filter { it.category() == PersonalizationKeyEnumState.Category.MOVEMENT_RESTRICTIONS }
                .toPersistentList(),
            selected = persistentListOf(
                PersonalizationKeyEnumState.AVOID_HIGH_IMPACT,
                PersonalizationKeyEnumState.AVOID_HEAVY_SPINAL_LOADING,
                PersonalizationKeyEnumState.NEEDS_SUPPORTED_MOVEMENTS,
            ),
            isExpanded = false,
            onExpand = {},
            onSelect = {},
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalPersonalizationExpandedPreview() {
    PreviewContainer {
        ProfileGoalPersonalizationGroup(
            category = PersonalizationKeyEnumState.Category.SCHEDULE,
            items = PersonalizationKeyEnumState.entries
                .filter { it.category() == PersonalizationKeyEnumState.Category.SCHEDULE }
                .toPersistentList(),
            selected = persistentListOf(
                PersonalizationKeyEnumState.MODERATE_FREQUENCY,
                PersonalizationKeyEnumState.WEEKDAYS_ONLY,
            ),
            isExpanded = true,
            onExpand = {},
            onSelect = {},
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalPersonalizationDirectChoicesPreview() {
    PreviewContainer {
        ProfileGoalPersonalizationGroup(
            category = PersonalizationKeyEnumState.Category.SESSION_DURATION,
            items = PersonalizationKeyEnumState.entries
                .filter { it.category() == PersonalizationKeyEnumState.Category.SESSION_DURATION }
                .toPersistentList(),
            selected = persistentListOf(PersonalizationKeyEnumState.MINUTES_45_TO_90),
            isExpanded = true,
            isExpandable = false,
            onExpand = {},
            onSelect = {},
        )
    }
}
