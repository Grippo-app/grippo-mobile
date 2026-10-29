package com.grippo.profile.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.goal.GoalMark
import com.grippo.design.components.menu.MenuValueRow
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_course_title
import com.grippo.design.resources.provider.goal_optional
import com.grippo.design.resources.provider.goal_options_collapsed
import com.grippo.design.resources.provider.goal_options_expanded
import com.grippo.design.resources.provider.goal_picker_secondary_title
import com.grippo.design.resources.provider.goal_recovery_section
import com.grippo.design.resources.provider.goal_routine_section
import com.grippo.design.resources.provider.goal_save_btn
import com.grippo.design.resources.provider.goal_selected_options
import com.grippo.design.resources.provider.goal_setup_suggestion_primary_btn
import com.grippo.design.resources.provider.goal_target_date_invalid
import com.grippo.design.resources.provider.goal_target_date_placeholder
import com.grippo.design.resources.provider.goal_title
import com.grippo.design.resources.provider.goal_training_preferences_section
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.ArrowTop
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.icons.Tune
import com.grippo.design.resources.provider.select_date
import com.grippo.profile.goal.components.ProfileGoalPersonalizationGroup
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Composable
internal fun ProfileGoalScreen(
    state: ProfileGoalState,
    loaders: ImmutableSet<ProfileGoalLoader>,
    contract: ProfileGoalContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val onSectionExpand: (String) -> Unit = { section ->
        expandedSection = if (expandedSection == section) null else section
        expandedCategory = null
    }
    val onCategoryExpand: (PersonalizationKeyEnumState.Category) -> Unit = { category ->
        expandedCategory = if (expandedCategory == category.name) null else category.name
    }
    val target = state.targetDate
    val buttonState = when {
        ProfileGoalLoader.SaveButton in loaders -> ButtonState.Loading
        state.selectedPrimary == null || target !is DateTimeFormatState.Valid -> ButtonState.Disabled
        else -> ButtonState.Enabled
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.goal_title),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    OverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            start = AppTokens.dp.screen.horizontalPadding,
            end = AppTokens.dp.screen.horizontalPadding,
            top = AppTokens.dp.contentPadding.block,
        ),
        overlay = AppTokens.colors.background.screen,
        content = { resolvedPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                contentPadding = resolvedPadding,
            ) {
                item(key = "main_goal") {
                    MainGoalSection(
                        modifier = Modifier.fillMaxWidth(),
                        state = state,
                        onPrimaryClick = contract::onPrimaryGoalPickerClick,
                        onSecondaryClick = contract::onSecondaryGoalPickerClick,
                        onTargetClick = contract::onTargetDatePickerClick,
                    )
                }
                item(key = "training_routine") {
                    PersonalizationSection(
                        state = state,
                        title = AppTokens.strings.res(Res.string.goal_routine_section),
                        icon = AppTokens.icons.Calendar,
                        categories = persistentListOf(
                            PersonalizationKeyEnumState.Category.SCHEDULE,
                            PersonalizationKeyEnumState.Category.SESSION_DURATION,
                            PersonalizationKeyEnumState.Category.LIFESTYLE,
                        ),
                        isExpanded = expandedSection == "training_routine",
                        expandedCategory = expandedCategory,
                        expandCategories = true,
                        onExpand = { onSectionExpand("training_routine") },
                        onCategoryExpand = onCategoryExpand,
                        onSelect = contract::onPersonalizationClick,
                    )
                }
                item(key = "training_preferences") {
                    PersonalizationSection(
                        state = state,
                        title = AppTokens.strings.res(Res.string.goal_training_preferences_section),
                        icon = AppTokens.icons.Tune,
                        categories = persistentListOf(
                            PersonalizationKeyEnumState.Category.SPORTS,
                            PersonalizationKeyEnumState.Category.FOCUS_AREAS,
                            PersonalizationKeyEnumState.Category.PHYSICAL_QUALITIES,
                            PersonalizationKeyEnumState.Category.TRAINING_PHASE,
                            PersonalizationKeyEnumState.Category.PROGRAM_PREFERENCES,
                            PersonalizationKeyEnumState.Category.ROUTINE_STRUCTURE,
                            PersonalizationKeyEnumState.Category.SET_METHOD_PREFERENCES,
                            PersonalizationKeyEnumState.Category.EQUIPMENT_PREFERENCES,
                        ),
                        isExpanded = expandedSection == "training_preferences",
                        expandedCategory = expandedCategory,
                        onExpand = { onSectionExpand("training_preferences") },
                        onCategoryExpand = onCategoryExpand,
                        onSelect = contract::onPersonalizationClick,
                    )
                }
                item(key = "recovery_limitations") {
                    PersonalizationSection(
                        state = state,
                        title = AppTokens.strings.res(Res.string.goal_recovery_section),
                        icon = AppTokens.icons.Body,
                        categories = persistentListOf(
                            PersonalizationKeyEnumState.Category.RECOVERY_CONTEXT,
                            PersonalizationKeyEnumState.Category.CARE_ZONES,
                            PersonalizationKeyEnumState.Category.HEALTH_CONDITIONS,
                            PersonalizationKeyEnumState.Category.MOVEMENT_RESTRICTIONS,
                        ),
                        isExpanded = expandedSection == "recovery_limitations",
                        expandedCategory = expandedCategory,
                        onExpand = { onSectionExpand("recovery_limitations") },
                        onCategoryExpand = onCategoryExpand,
                        onSelect = contract::onPersonalizationClick,
                    )
                }
            }
        },
        bottom = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = AppTokens.dp.screen.horizontalPadding,
                        end = AppTokens.dp.screen.horizontalPadding,
                        bottom = AppTokens.dp.screen.verticalPadding,
                    ),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.goal_save_btn),
                    ),
                    style = ButtonStyle.Primary,
                    state = buttonState,
                    onClick = contract::onSave,
                )
            }
        },
    )
}

@Composable
private fun MainGoalSection(
    modifier: Modifier = Modifier,
    state: ProfileGoalState,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    onTargetClick: () -> Unit,
) {
    val target = state.targetDate
    val targetValue = target.value
    val targetLabel = if (targetValue != null) {
        DateCompose.rememberFormat(value = targetValue, format = target.format)
    } else {
        AppTokens.strings.res(Res.string.select_date)
    }
    val dateError = if (target is DateTimeFormatState.Invalid) {
        val earliestDate = DateCompose.rememberFormat(
            value = state.limitations.from,
            format = state.selectedTarget.format,
        )
        val latestDate = DateCompose.rememberFormat(
            value = state.limitations.to,
            format = state.selectedTarget.format,
        )
        AppTokens.strings.res(Res.string.goal_target_date_invalid, earliestDate, latestDate)
    } else {
        null
    }
    val targetTitle = AppTokens.strings.res(Res.string.goal_target_date_placeholder)
    val shape = RoundedCornerShape(AppTokens.dp.menu.radius)

    Column(
        modifier = modifier
            .animateContentSize()
            .background(color = AppTokens.colors.background.card, shape = shape)
            .border(
                width = AppTokens.dp.menu.borderWidth,
                color = AppTokens.colors.border.default,
                shape = shape,
            )
            .padding(
                start = AppTokens.dp.menu.contentPadding,
                top = AppTokens.dp.menu.contentPadding,
                end = AppTokens.dp.menu.contentPadding,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppTokens.dp.menu.item.minHeight)
                .scalableClick(role = Role.Button, onClick = onPrimaryClick)
                .padding(vertical = AppTokens.dp.menu.item.verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoalMark(goal = state.selectedPrimary)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = AppTokens.strings.res(Res.string.goal_course_title),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
                Text(
                    text = state.selectedPrimary?.label()
                        ?: AppTokens.strings.res(Res.string.goal_setup_suggestion_primary_btn),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
            Icon(
                modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                imageVector = AppTokens.icons.ArrowRight,
                contentDescription = null,
                tint = AppTokens.colors.icon.secondary,
            )
        }
        ContentDivider()
        MenuValueRow(
            modifier = Modifier.fillMaxWidth(),
            title = AppTokens.strings.res(Res.string.goal_picker_secondary_title),
            value = state.selectedSecondary?.label() ?: AppTokens.strings.res(Res.string.goal_optional),
            icon = state.selectedSecondary?.icon(),
            iconColor = AppTokens.colors.goal.accent,
            onClick = onSecondaryClick,
        )
        MenuValueRow(
            modifier = if (dateError != null) {
                Modifier.clearAndSetSemantics {
                    contentDescription = listOf(targetTitle, targetLabel).joinToString(", ")
                    role = Role.Button
                    error(dateError)
                    onClick {
                        onTargetClick()
                        true
                    }
                }
            } else {
                Modifier
            },
            title = targetTitle,
            value = targetLabel,
            icon = AppTokens.icons.Calendar,
            iconColor = if (dateError != null) {
                AppTokens.colors.semantic.error
            } else {
                AppTokens.colors.icon.secondary
            },
            showDivider = false,
            titleColor = if (dateError != null) {
                AppTokens.colors.semantic.error
            } else {
                AppTokens.colors.text.primary
            },
            onClick = onTargetClick,
        )
        if (dateError != null) {
            Text(
                modifier = Modifier.padding(bottom = AppTokens.dp.menu.contentPadding),
                text = dateError,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.semantic.error,
            )
        }
    }
}

@Composable
private fun PersonalizationSection(
    modifier: Modifier = Modifier,
    state: ProfileGoalState,
    title: String,
    icon: ImageVector,
    categories: ImmutableList<PersonalizationKeyEnumState.Category>,
    isExpanded: Boolean,
    expandedCategory: String?,
    expandCategories: Boolean = false,
    onExpand: () -> Unit,
    onCategoryExpand: (PersonalizationKeyEnumState.Category) -> Unit,
    onSelect: (PersonalizationKeyEnumState) -> Unit,
) {
    val groupedItems = remember(state.personalization) {
        state.personalization
            .groupBy(PersonalizationKeyEnumState::category)
            .mapValues { (_, items) -> items.toPersistentList() }
    }
    val availableCategories = categories.filter { !groupedItems[it].isNullOrEmpty() }
    if (availableCategories.isEmpty()) return

    val selectedOptions = state.selectedPersonalization.filter { it.category() in availableCategories }
    val routineOptions = selectedOptions.filter {
        it.category() == PersonalizationKeyEnumState.Category.SCHEDULE ||
            it.category() == PersonalizationKeyEnumState.Category.SESSION_DURATION
    }
    val summary = when {
        selectedOptions.isEmpty() -> null
        selectedOptions.size == 1 -> selectedOptions.single().label()
        expandCategories && routineOptions.size in 1..2 -> {
            routineOptions.map { it.label() }.joinToString(separator = " · ")
        }
        else -> AppTokens.strings.res(Res.plurals.goal_selected_options, selectedOptions.size, selectedOptions.size)
    }
    val expansionDescription = AppTokens.strings.res(
        if (isExpanded) Res.string.goal_options_expanded else Res.string.goal_options_collapsed,
    )
    val shape = RoundedCornerShape(AppTokens.dp.menu.radius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTokens.colors.background.card)
            .border(
                width = AppTokens.dp.menu.borderWidth,
                color = AppTokens.colors.border.default,
                shape = shape,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppTokens.dp.menu.item.minHeight)
                .semantics { stateDescription = expansionDescription }
                .scalableClick(role = Role.Button, onClick = onExpand)
                .padding(AppTokens.dp.menu.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AppTokens.dp.menu.headerIconContainer)
                    .background(
                        color = AppTokens.colors.background.screen,
                        shape = RoundedCornerShape(AppTokens.dp.menu.headerIconRadius),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(AppTokens.dp.menu.headerIcon),
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppTokens.colors.icon.secondary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = title,
                    style = AppTokens.typography.b14Semi(),
                    color = AppTokens.colors.text.primary,
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
            Icon(
                modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                imageVector = if (isExpanded) AppTokens.icons.ArrowTop else AppTokens.icons.ArrowDown,
                contentDescription = null,
                tint = AppTokens.colors.icon.secondary,
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(
                    start = AppTokens.dp.menu.contentPadding,
                    end = AppTokens.dp.menu.contentPadding,
                    bottom = AppTokens.dp.menu.contentPadding,
                ),
            ) {
                ContentDivider()
                availableCategories.forEachIndexed { index, category ->
                    key(category) {
                        ProfileGoalPersonalizationGroup(
                            modifier = Modifier.fillMaxWidth(),
                            category = category,
                            items = groupedItems.getValue(category),
                            selected = state.selectedPersonalization,
                            isExpanded = expandCategories || expandedCategory == category.name,
                            isExpandable = !expandCategories,
                            onExpand = { onCategoryExpand(category) },
                            onSelect = onSelect,
                        )
                        if (index < availableCategories.lastIndex) {
                            ContentDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun profileGoalPreviewState(): ProfileGoalState {
    val range = DateRange(
        from = LocalDateTime(2026, 11, 15, 0, 0),
        to = LocalDateTime(2027, 10, 18, 23, 59),
    )
    return ProfileGoalState(
        selectedPrimary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
        selectedSecondary = GoalSecondaryGoalEnumState.GET_STRONGER,
        limitations = range,
        selectedTarget = DateTimeFormatState.of(
            value = LocalDateTime(2027, 1, 15, 0, 0),
            range = range,
            format = DateFormat.DateOnly.DateDdMmmYyyy,
        ),
        selectedPersonalization = persistentListOf(
            PersonalizationKeyEnumState.MODERATE_FREQUENCY,
            PersonalizationKeyEnumState.WEEKDAYS_ONLY,
            PersonalizationKeyEnumState.MINUTES_45_TO_90,
            PersonalizationKeyEnumState.SEDENTARY_JOB,
            PersonalizationKeyEnumState.RECOVERY_FOCUS,
            PersonalizationKeyEnumState.LOWER_BACK_CARE,
            PersonalizationKeyEnumState.SPORT_BODYBUILDING,
        ),
    )
}

@AppPreview
@Composable
private fun ProfileGoalScreenPopulatedPreview() {
    PreviewContainer {
        ProfileGoalScreen(
            state = profileGoalPreviewState(),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalScreenEmptyPreview() {
    PreviewContainer {
        ProfileGoalScreen(
            state = profileGoalPreviewState().copy(
                selectedPrimary = null,
                selectedSecondary = null,
                selectedTarget = DateTimeFormatState.Empty(format = DateFormat.DateOnly.DateDdMmmYyyy),
                selectedPersonalization = persistentListOf(),
            ),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalScreenInvalidDatePreview() {
    PreviewContainer {
        val state = profileGoalPreviewState()
        ProfileGoalScreen(
            state = state.copy(
                selectedTarget = DateTimeFormatState.of(
                    value = LocalDateTime(2026, 8, 15, 0, 0),
                    range = state.limitations,
                    format = state.selectedTarget.format,
                ),
            ),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalScreenSavingPreview() {
    PreviewContainer {
        ProfileGoalScreen(
            state = profileGoalPreviewState(),
            loaders = persistentSetOf(ProfileGoalLoader.SaveButton),
            contract = ProfileGoalContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalRoutineExpandedPreview() {
    PreviewContainer {
        PersonalizationSection(
            state = profileGoalPreviewState().copy(
                selectedPersonalization = persistentListOf(
                    PersonalizationKeyEnumState.MODERATE_FREQUENCY,
                    PersonalizationKeyEnumState.MINUTES_45_TO_90,
                ),
            ),
            title = AppTokens.strings.res(Res.string.goal_routine_section),
            icon = AppTokens.icons.Calendar,
            categories = persistentListOf(
                PersonalizationKeyEnumState.Category.SCHEDULE,
                PersonalizationKeyEnumState.Category.SESSION_DURATION,
                PersonalizationKeyEnumState.Category.LIFESTYLE,
            ),
            isExpanded = true,
            expandedCategory = null,
            expandCategories = true,
            onExpand = {},
            onCategoryExpand = {},
            onSelect = {},
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalPreferencesExpandedPreview() {
    PreviewContainer {
        PersonalizationSection(
            state = profileGoalPreviewState(),
            title = AppTokens.strings.res(Res.string.goal_training_preferences_section),
            icon = AppTokens.icons.Tune,
            categories = persistentListOf(
                PersonalizationKeyEnumState.Category.SPORTS,
                PersonalizationKeyEnumState.Category.FOCUS_AREAS,
                PersonalizationKeyEnumState.Category.PHYSICAL_QUALITIES,
                PersonalizationKeyEnumState.Category.TRAINING_PHASE,
                PersonalizationKeyEnumState.Category.PROGRAM_PREFERENCES,
                PersonalizationKeyEnumState.Category.ROUTINE_STRUCTURE,
                PersonalizationKeyEnumState.Category.SET_METHOD_PREFERENCES,
                PersonalizationKeyEnumState.Category.EQUIPMENT_PREFERENCES,
            ),
            isExpanded = true,
            expandedCategory = PersonalizationKeyEnumState.Category.SPORTS.name,
            onExpand = {},
            onCategoryExpand = {},
            onSelect = {},
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalRecoveryExpandedPreview() {
    PreviewContainer {
        PersonalizationSection(
            state = profileGoalPreviewState(),
            title = AppTokens.strings.res(Res.string.goal_recovery_section),
            icon = AppTokens.icons.Body,
            categories = persistentListOf(
                PersonalizationKeyEnumState.Category.RECOVERY_CONTEXT,
                PersonalizationKeyEnumState.Category.CARE_ZONES,
                PersonalizationKeyEnumState.Category.HEALTH_CONDITIONS,
                PersonalizationKeyEnumState.Category.MOVEMENT_RESTRICTIONS,
            ),
            isExpanded = true,
            expandedCategory = PersonalizationKeyEnumState.Category.CARE_ZONES.name,
            onExpand = {},
            onCategoryExpand = {},
            onSelect = {},
        )
    }
}
