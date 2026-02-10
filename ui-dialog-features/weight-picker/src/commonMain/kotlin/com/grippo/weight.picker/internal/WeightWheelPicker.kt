package com.grippo.weight.picker.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.design.components.wheel.WheelItemRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
import com.grippo.weight.picker.DefaultWeightSuggestions
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.WheelColumn
import kotlinx.collections.immutable.PersistentList
import kotlin.math.roundToInt

@Composable
internal fun WeightWheelPicker(
    modifier: Modifier = Modifier,
    suggestions: PersistentList<Float>,
    value: Float?,
    select: (Float) -> Unit
) {
    val weightListState = rememberLazyListState()
    val fractionListState = rememberLazyListState()

    val integerItems = remember(suggestions) {
        suggestions.map { it.toInt() }.distinct()
    }
    val fractionItems = remember { (0..9).toList() }
    val suggestionTenthsSet = remember(suggestions) {
        suggestions.map { (it * 10f).roundToInt() }.toSet()
    }

    val selectedInteger = value?.toInt()
    val selectedFraction = value?.let { ((it * 10f).roundToInt() % 10).coerceIn(0, 9) }

    fun availableFractionsForInt(integer: Int): List<Int> {
        return fractionItems.filter { fraction ->
            suggestionTenthsSet.contains(integer * 10 + fraction)
        }
    }

    fun resolveWeight(integer: Int, fraction: Int?): Float? {
        val available = availableFractionsForInt(integer)
        if (available.isEmpty()) return null
        val resolvedFraction = fraction?.takeIf { it in available } ?: available.first()
        return integer + resolvedFraction / 10f
    }

    val integerColumn = WheelColumn(
        id = "weight_integer",
        items = integerItems,
        selected = selectedInteger,
        onSelect = { integer ->
            resolveWeight(integer, selectedFraction)?.let(select)
        },
        isValid = { integer ->
            availableFractionsForInt(integer).isNotEmpty()
        },
        itemContent = { item, _ ->
            WheelItemRow(
                modifier = Modifier.fillMaxWidth(),
                text = item.toString(),
                isValid = true,
                horizontalArrangement = Arrangement.End
            )
        },
        listState = weightListState
    )

    val fractionColumn = WheelColumn(
        id = "weight_fraction",
        items = fractionItems,
        selected = selectedFraction,
        onSelect = { fraction ->
            val integer = selectedInteger ?: integerItems.firstOrNull()
            if (integer != null) {
                resolveWeight(integer, fraction)?.let(select)
            }
        },
        isValid = { fraction ->
            selectedInteger?.let { integer ->
                suggestionTenthsSet.contains(integer * 10 + fraction)
            } ?: true
        },
        itemContent = { item, isValid ->
            WheelItemRow(
                modifier = Modifier
                    .fillMaxWidth(),
                text = ".${item}",
                isValid = isValid,
                horizontalArrangement = Arrangement.Start,
                subText = AppTokens.strings.res(Res.string.kg)
            )
        },
        listState = fractionListState
    )

    MultiWheelPicker(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        selectorProperties = DefaultSelectorProperties(
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
            color = AppTokens.colors.background.card,
        ),
        columns = listOf(integerColumn, fractionColumn)
    )
}

@AppPreview
@Composable
private fun WeightWheelPickerPreview() {
    PreviewContainer {
        WeightWheelPicker(
            suggestions = DefaultWeightSuggestions,
            value = 403f,
            select = {},
        )
    }
}
