package com.grippo.services.database.search

/**
 * Shared utilities for local prefix-based search indexing and query parsing.
 *
 * Normalizes text (lowercase + separator collapsing), extracts unique tokens, and builds
 * token prefixes starting from [DEFAULT_MIN_PREFIX_LENGTH]. Repository/DAO code decides when
 * to use prefix search vs fallback list queries.
 */
public object PrefixSearch {

    private const val DEFAULT_MIN_PREFIX_LENGTH: Int = 3

    public data class Query(
        val phraseLowercase: String,
        val tokens: List<String>,
    )

    public data class TokenPrefix(
        val token: String,
        val prefix: String,
    )

    public fun parseQueryOrNull(value: String?): Query? {
        val raw = value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val phraseLowercase = collapseWhitespace(raw.lowercase())

        val tokens = tokenize(
            value = raw,
            minTokenLength = DEFAULT_MIN_PREFIX_LENGTH,
        )

        if (tokens.isEmpty()) return null

        return Query(
            phraseLowercase = phraseLowercase,
            tokens = tokens,
        )
    }

    public fun tokenize(value: String): List<String> {
        return tokenize(
            value = value,
            minTokenLength = DEFAULT_MIN_PREFIX_LENGTH,
        )
    }

    public fun buildTokenPrefixes(value: String): List<TokenPrefix> {
        return buildTokenPrefixes(
            value = value,
            minPrefixLength = DEFAULT_MIN_PREFIX_LENGTH,
        )
    }

    public fun normalize(value: String): String {
        val lowered = value.lowercase()
        val builder = StringBuilder(lowered.length)
        var lastWasSeparator = true

        lowered.forEach { char ->
            if (char.isLetterOrDigit()) {
                builder.append(char)
                lastWasSeparator = false
            } else if (!lastWasSeparator) {
                builder.append(' ')
                lastWasSeparator = true
            }
        }

        return builder.toString().trim()
    }

    private fun tokenize(
        value: String,
        minTokenLength: Int,
    ): List<String> {
        val normalizedMinTokenLength = minTokenLength.coerceAtLeast(1)

        return normalize(value)
            .split(' ')
            .asSequence()
            .filter { it.length >= normalizedMinTokenLength }
            .distinct()
            .toList()
    }

    private fun buildTokenPrefixes(
        value: String,
        minPrefixLength: Int,
    ): List<TokenPrefix> {
        val normalizedMinPrefixLength = minPrefixLength.coerceAtLeast(1)

        val tokens = tokenize(
            value = value,
            minTokenLength = normalizedMinPrefixLength,
        )

        if (tokens.isEmpty()) return emptyList()

        return buildList {
            tokens.forEach { token ->
                for (length in normalizedMinPrefixLength..token.length) {
                    add(
                        TokenPrefix(
                            token = token,
                            prefix = token.take(length),
                        )
                    )
                }
            }
        }
    }

    private fun collapseWhitespace(value: String): String {
        val builder = StringBuilder(value.length)
        var lastWasWhitespace = true

        value.forEach { char ->
            if (char.isWhitespace()) {
                if (!lastWasWhitespace) {
                    builder.append(' ')
                    lastWasWhitespace = true
                }
            } else {
                builder.append(char)
                lastWasWhitespace = false
            }
        }

        return builder.toString().trim()
    }
}
