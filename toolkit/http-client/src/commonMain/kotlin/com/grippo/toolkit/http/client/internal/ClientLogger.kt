package com.grippo.toolkit.http.client.internal

import com.grippo.logger.AppLogger
import io.ktor.client.plugins.logging.Logger
import org.koin.core.annotation.Single

@Single
internal class ClientLogger : Logger {

    override fun log(message: String) {

        val emojiLine = if ((message.contains("RESPONSE: 200") || message.contains("RESPONSE: 201"))
            && message.contains("REQUEST").not()
        ) {
            " \uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9"
        } else if (message.contains("REQUEST").not()) {
            " \uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5"
        } else {
            "────────────────────────────────────────"
        }

        val formattedMessage = message
            .split("\n")
            .joinToString("\n│ ")

        AppLogger.Network.log(
            "┌$emojiLine HTTP LOG $emojiLine\n│ $formattedMessage\n└────────────────────────────────────────"
        )
    }
}