package com.grippo.authorization.profile.creation.excluded.muscles

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.muscles.stubMuscleGroup
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExcludedMusclesScreen(
    state: ExcludedMusclesState,
    loaders: ImmutableSet<ExcludedMusclesLoader>,
    contract: ExcludedMusclesContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreviewSelected() {
    PreviewContainer {
        ExcludedMusclesScreen(
            state = ExcludedMusclesState(
                suggestions = stubMuscleGroup(),
                selectedMuscleIds = stubMuscleGroup()
                    .map { it.muscles.map { it.value.id } }
                    .flatten()
                    .take(3).toPersistentList()
            ),
            loaders = persistentSetOf(),
            contract = ExcludedMusclesContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewUnselected() {
    PreviewContainer {
        ExcludedMusclesScreen(
            state = ExcludedMusclesState(
                suggestions = stubMuscleGroup(),
                selectedMuscleIds = persistentListOf()
            ),
            loaders = persistentSetOf(),
            contract = ExcludedMusclesContract.Empty
        )
    }
}
