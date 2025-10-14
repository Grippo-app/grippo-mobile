package com.grippo.ml.translation.internal

/**
 * Abstraction over a platform translation engine.
 */
internal interface TranslationEngine {
    /**
     * Translates [text] from [sourceLang] (or the engine's configured hint if null) to [targetLang].
     * Implementations must keep placeholder markers such as `@@0@@` untouched.
     */
    suspend fun translateOnce(
        text: String,
        sourceLang: String?,
        targetLang: String,
        domain: String = "default"
    ): String
}

/**
 * Factory method that returns the underlying platform engine implementation.
 */
internal expect fun platformEngine(
    sourceLangHint: String?,
    targetLang: String
): TranslationEngine
