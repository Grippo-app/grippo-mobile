package com.grippo.design.components.user.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.grippo.core.state.profile.UserState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cm
import com.grippo.design.resources.provider.icons.Height
import com.grippo.design.resources.provider.icons.Weight
import com.grippo.design.resources.provider.kg
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun rememberUserStats(value: UserState): ImmutableList<UserStatEntry> {
    val joinedLabel = value.createdAt.display

    val heightUnit = AppTokens.strings.res(Res.string.cm)
    val weightUnit = AppTokens.strings.res(Res.string.kg)

    val heightDisplay = value.height.display.takeIf { it.isNotBlank() }
    val weightDisplay = value.weight.display.takeIf { it.isNotBlank() }

    val heightIcon = AppTokens.icons.Height
    val weightIcon = AppTokens.icons.Weight

    return remember(
        heightDisplay, weightDisplay, joinedLabel,
        heightUnit, weightUnit, heightIcon, weightIcon,
    ) {
        if (heightDisplay == null && weightDisplay == null && joinedLabel.isBlank()) {
            return@remember persistentListOf()
        }
        buildList(capacity = 3) {
            if (heightDisplay != null) {
                add(UserStatEntry(icon = heightIcon, text = "$heightDisplay $heightUnit"))
            }
            if (weightDisplay != null) {
                add(UserStatEntry(icon = weightIcon, text = "$weightDisplay $weightUnit"))
            }
            if (joinedLabel.isNotBlank()) {
                add(UserStatEntry(icon = null, text = joinedLabel))
            }
        }.toPersistentList()
    }
}
