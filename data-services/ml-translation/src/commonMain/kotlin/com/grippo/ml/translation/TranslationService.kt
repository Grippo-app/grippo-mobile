package com.grippo.ml.translation

import com.grippo.ml.translation.internal.TranslationEngine

public interface TranslationService {
    /**
     * Convenience translation entry point that will build a platform engine on-demand.
     * Provide either [sourceLang] or [sourceLangHint] so ML Kit can determine the source model.
     */
    public suspend fun translate(
        text: String,
        sourceLang: String?,
        targetLang: String,
        domain: String = "default",
        sourceLangHint: String? = null
    ): String
}

internal fun interface TranslationEngineFactory {
    fun create(
        sourceLangHint: String?,
        targetLang: String
    ): TranslationEngine
}