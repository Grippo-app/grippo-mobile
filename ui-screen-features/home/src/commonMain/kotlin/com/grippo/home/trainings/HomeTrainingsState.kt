package com.grippo.home.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.edit_btn
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.selected
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class HomeTrainingsState(
    val date: DateRange = DateTimeUtils.thisDay(),
    val limitations: DateRange = DateTimeUtils.trailingYear(),
    val trainings: ImmutableList<TrainingListValue> = persistentListOf(),
)

@Immutable
internal enum class TrainingMenu(val id: String) {
    Edit("edit"),
    Delete("delete");

    companion object {
        suspend fun title(stringProvider: StringProvider): String {
            return stringProvider.get(Res.string.selected)
        }

        fun of(id: String): TrainingMenu? {
            return TrainingMenu.entries.firstOrNull { it.id == id }
        }
    }

    suspend fun text(stringProvider: StringProvider): String {
        return when (this) {
            Delete -> stringProvider.get(Res.string.delete_btn)
            Edit -> stringProvider.get(Res.string.edit_btn)
        }
    }
}