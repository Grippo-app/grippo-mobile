package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.LogEntry

internal object LogFormatter {

    fun format(entry: LogEntry): String {
        val header = "[${entry.timestamp}][${entry.category.name}]"
        val sanitized = entry.message.replace("\r\n", "\n")
        val lines = sanitized.split('\n')
        val builder = StringBuilder()
        if (lines.isEmpty()) {
            builder.append(header).append('\n')
            return builder.toString()
        }
        builder.append(header).append(' ').append(lines.first()).append('\n')
        if (lines.size == 1) {
            return builder.toString()
        }
        val continuationPrefix = " ".repeat(header.length + 1)
        for (line in lines.drop(1)) {
            builder.append(continuationPrefix).append(line).append('\n')
        }
        return builder.toString()
    }
}
