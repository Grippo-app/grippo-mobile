package com.grippo.design.components.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

internal class ValueSlot(
    val value: String,
    val appendValue: AnnotatedString.Builder.(value: String) -> Unit = { append(it) },
) {
    companion object {
        private const val FIRST_MARKER: Char = '\uE000'
        private const val MAX_SLOTS: Int = 64

        fun marker(index: Int): String {
            require(index in 0 until MAX_SLOTS) { "Value slot index $index is outside 0 until $MAX_SLOTS" }
            return (FIRST_MARKER + index).toString()
        }

        fun indexOf(char: Char): Int? {
            val index = char - FIRST_MARKER
            return index.takeIf { it in 0 until MAX_SLOTS }
        }
    }
}

internal fun buildValueSlotString(
    sentence: String,
    slots: List<ValueSlot>,
    appendRest: AnnotatedString.Builder.(text: String) -> Unit = { append(it) },
): AnnotatedString {
    return buildAnnotatedString {
        var restStart = 0

        sentence.forEachIndexed { position, char ->
            val index = ValueSlot.indexOf(char) ?: return@forEachIndexed
            val slot = requireNotNull(slots.getOrNull(index)) { "No value slot for marker $index" }

            if (position > restStart) {
                appendRest(sentence.substring(restStart, position))
            }
            slot.appendValue(this, slot.value)
            restStart = position + 1
        }

        if (restStart < sentence.length) {
            appendRest(sentence.substring(restStart))
        }
    }
}
