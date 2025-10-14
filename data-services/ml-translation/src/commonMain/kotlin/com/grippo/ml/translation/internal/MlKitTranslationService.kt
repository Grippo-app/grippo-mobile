package com.grippo.ml.translation.internal

import com.grippo.ml.translation.TranslationEngineFactory
import com.grippo.ml.translation.TranslationService
import org.koin.core.annotation.Single

@Single(binds = [TranslationService::class])
internal class MlKitTranslationService(
    private val engineFactory: TranslationEngineFactory
) : TranslationService {
    override suspend fun translate(
        text: String,
        sourceLang: String?,
        targetLang: String,
        domain: String,
        sourceLangHint: String?
    ): String {
        val engineSourceLang = sourceLang ?: sourceLangHint
        ?: error("Source language hint is required for ML Kit translation")
        val engine = engineFactory.create(engineSourceLang, targetLang)
        return engine.translateOnce(
            text = text,
            sourceLang = sourceLang,
            targetLang = targetLang,
            domain = domain
        )
    }
}