package com.grippo.ml.translation.internal

private val placeholderRegex = Regex("@@\\d+@@")
private const val tokenPrefix = "__GRIPPO_PLACEHOLDER_"
private const val tokenSuffix = "__"

internal suspend fun preservePlaceholders(
    text: String,
    translate: suspend (String) -> String
): String {
    if (!placeholderRegex.containsMatchIn(text)) {
        return translate(text)
    }

    var counter = 0
    val placeholders = LinkedHashMap<String, String>()
    val sanitized = placeholderRegex.replace(text) { matchResult ->
        val token = buildString {
            append(tokenPrefix)
            append(counter++)
            append(tokenSuffix)
        }
        placeholders[token] = matchResult.value
        token
    }

    val translated = translate(sanitized)
    if (placeholders.isEmpty()) {
        return translated
    }

    var restored = translated
    placeholders.forEach { (token, placeholder) ->
        restored = restored.replace(token, placeholder)
    }
    return restored
}
