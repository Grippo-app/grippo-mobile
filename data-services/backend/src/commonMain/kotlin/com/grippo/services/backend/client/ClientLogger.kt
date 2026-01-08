package com.grippo.services.backend.client

import com.grippo.toolkit.logger.AppLogger
import io.ktor.client.plugins.logging.Logger
import org.koin.core.annotation.Single

@Single
internal class ClientLogger : Logger {

    override fun log(message: String) {

        val emojiLine = if ((message.contains("RESPONSE: 200") || message.contains("RESPONSE: 201"))
            && message.contains("REQUEST").not()
        ) {
            "\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9\uD83D\uDFE9"
        } else if (message.contains("REQUEST").not()) {
            "\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5\uD83D\uDFE5"
        } else {
            "\uD83D\uDFE8\uD83D\uDFE8\uD83D\uDFE8\uD83D\uDFE8\uD83D\uDFE8\uD83D\uDFE8"
        }

        val formattedMessage = message
            .split("\n")
            .joinToString("\n")

        AppLogger.Network.log(
            "$emojiLine HTTP LOG $emojiLine\n$formattedMessage"
        )
    }
}