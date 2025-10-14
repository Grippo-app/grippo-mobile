package com.grippo.ml.translation.internal

import com.grippo.ml.translation.TranslationEngineFactory
import org.koin.core.annotation.Single

@Single(binds = [TranslationEngineFactory::class])
internal class PlatformTranslationEngineFactory : TranslationEngineFactory {
    override fun create(sourceLangHint: String?, targetLang: String): TranslationEngine {
        return platformEngine(sourceLangHint = sourceLangHint, targetLang = targetLang)
    }
}