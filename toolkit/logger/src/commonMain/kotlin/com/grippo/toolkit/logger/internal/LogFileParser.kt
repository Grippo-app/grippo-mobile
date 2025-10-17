package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.LogCategory

internal object LogFileParser {

    private val headerRegex = Regex("^\\[([^\\]]+)]\\[([^\\]]+)]\\s?(.*)$")

    fun groupByCategory(content: String): Map<LogCategory, List<String>> {
        if (content.isBlank()) return emptyMap()

        val result = LinkedHashMap<LogCategory, MutableList<String>>()
        var currentCategory: LogCategory? = null
        var currentMessage = StringBuilder()
        var continuationPrefix = ""

        fun flush() {
            val category = currentCategory ?: return
            val messageBody = currentMessage.toString()
            result.getOrPut(category) { mutableListOf() }.add(messageBody)
            currentCategory = null
            currentMessage = StringBuilder()
            continuationPrefix = ""
        }

        for (rawLine in content.lineSequence()) {
            val match = headerRegex.matchEntire(rawLine)
            if (match != null) {
                flush()
                val categoryName = match.groupValues.getOrNull(2) ?: continue
                val category =
                    runCatching { LogCategory.valueOf(categoryName) }.getOrNull() ?: continue

                currentCategory = category
                val headerLength = buildString {
                    append('[').append(match.groupValues.getOrNull(1).orEmpty()).append(']')
                    append('[').append(categoryName).append(']')
                }.length
                continuationPrefix = if (headerLength > 0) " ".repeat(headerLength + 1) else ""

                currentMessage = StringBuilder()
                val messageLine = match.groupValues.getOrNull(3).orEmpty()
                if (messageLine.isNotEmpty()) {
                    currentMessage.append(messageLine)
                }
            } else if (currentCategory != null) {
                if (currentMessage.isNotEmpty()) currentMessage.append('\n')
                val cleanedLine =
                    if (continuationPrefix.isNotEmpty() && rawLine.startsWith(continuationPrefix)) {
                        rawLine.removePrefix(continuationPrefix)
                    } else {
                        rawLine
                    }
                currentMessage.append(cleanedLine)
            }
        }

        flush()

        return result.mapValues { entry -> entry.value.toList() }
    }
}
