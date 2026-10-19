package com.grippo.core.state.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.assist_weight_placeholder
import com.grippo.design.resources.provider.body_weight_placeholder
import com.grippo.design.resources.provider.extra_weight_placeholder
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.Kettlebell
import com.grippo.design.resources.provider.training_external_weight
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class ExerciseExampleComponentsState {

    public fun title(): UiText {
        val resource = when (this) {
            is External -> Res.string.training_external_weight
            is BodyOnly -> Res.string.body_weight_placeholder
            is BodyAndExtra -> Res.string.extra_weight_placeholder
            is BodyAndAssist -> Res.string.assist_weight_placeholder
        }
        return UiText.Res(resource)
    }

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            is External -> AppTokens.icons.Dumbbell
            is BodyOnly -> AppTokens.icons.Body
            is BodyAndExtra -> AppTokens.icons.Kettlebell
            is BodyAndAssist -> AppTokens.icons.Body
        }
    }

    @Immutable
    @Serializable
    public data class External(
        val required: Boolean
    ) : ExerciseExampleComponentsState()

    @Immutable
    @Serializable
    public data class BodyOnly(
        val required: Boolean,
        val bodyMultiplier: Float
    ) : ExerciseExampleComponentsState()

    @Immutable
    @Serializable
    public data class BodyAndExtra(
        val bodyRequired: Boolean,
        val bodyMultiplier: Float,
        val extraRequired: Boolean
    ) : ExerciseExampleComponentsState()

    @Immutable
    @Serializable
    public data class BodyAndAssist(
        val bodyRequired: Boolean,
        val bodyMultiplier: Float,
        val assistRequired: Boolean
    ) : ExerciseExampleComponentsState()
}
