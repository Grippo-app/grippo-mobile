package com.grippo.ml.translation.internal

import cocoapods.MLKitTranslate.MLKModelDownloadConditions
import cocoapods.MLKitTranslate.MLKTranslateLanguageAfrikaans
import cocoapods.MLKitTranslate.MLKTranslateLanguageAlbanian
import cocoapods.MLKitTranslate.MLKTranslateLanguageArabic
import cocoapods.MLKitTranslate.MLKTranslateLanguageBelarusian
import cocoapods.MLKitTranslate.MLKTranslateLanguageBengali
import cocoapods.MLKitTranslate.MLKTranslateLanguageBulgarian
import cocoapods.MLKitTranslate.MLKTranslateLanguageCatalan
import cocoapods.MLKitTranslate.MLKTranslateLanguageChinese
import cocoapods.MLKitTranslate.MLKTranslateLanguageCroatian
import cocoapods.MLKitTranslate.MLKTranslateLanguageCzech
import cocoapods.MLKitTranslate.MLKTranslateLanguageDanish
import cocoapods.MLKitTranslate.MLKTranslateLanguageDutch
import cocoapods.MLKitTranslate.MLKTranslateLanguageEnglish
import cocoapods.MLKitTranslate.MLKTranslateLanguageEsperanto
import cocoapods.MLKitTranslate.MLKTranslateLanguageEstonian
import cocoapods.MLKitTranslate.MLKTranslateLanguageFinnish
import cocoapods.MLKitTranslate.MLKTranslateLanguageFrench
import cocoapods.MLKitTranslate.MLKTranslateLanguageGalician
import cocoapods.MLKitTranslate.MLKTranslateLanguageGeorgian
import cocoapods.MLKitTranslate.MLKTranslateLanguageGerman
import cocoapods.MLKitTranslate.MLKTranslateLanguageGreek
import cocoapods.MLKitTranslate.MLKTranslateLanguageGujarati
import cocoapods.MLKitTranslate.MLKTranslateLanguageHaitianCreole
import cocoapods.MLKitTranslate.MLKTranslateLanguageHebrew
import cocoapods.MLKitTranslate.MLKTranslateLanguageHindi
import cocoapods.MLKitTranslate.MLKTranslateLanguageHungarian
import cocoapods.MLKitTranslate.MLKTranslateLanguageIcelandic
import cocoapods.MLKitTranslate.MLKTranslateLanguageIndonesian
import cocoapods.MLKitTranslate.MLKTranslateLanguageIrish
import cocoapods.MLKitTranslate.MLKTranslateLanguageItalian
import cocoapods.MLKitTranslate.MLKTranslateLanguageJapanese
import cocoapods.MLKitTranslate.MLKTranslateLanguageKannada
import cocoapods.MLKitTranslate.MLKTranslateLanguageKorean
import cocoapods.MLKitTranslate.MLKTranslateLanguageLatvian
import cocoapods.MLKitTranslate.MLKTranslateLanguageLithuanian
import cocoapods.MLKitTranslate.MLKTranslateLanguageMacedonian
import cocoapods.MLKitTranslate.MLKTranslateLanguageMalay
import cocoapods.MLKitTranslate.MLKTranslateLanguageMaltese
import cocoapods.MLKitTranslate.MLKTranslateLanguageMarathi
import cocoapods.MLKitTranslate.MLKTranslateLanguageNorwegian
import cocoapods.MLKitTranslate.MLKTranslateLanguagePersian
import cocoapods.MLKitTranslate.MLKTranslateLanguagePolish
import cocoapods.MLKitTranslate.MLKTranslateLanguagePortuguese
import cocoapods.MLKitTranslate.MLKTranslateLanguageRomanian
import cocoapods.MLKitTranslate.MLKTranslateLanguageRussian
import cocoapods.MLKitTranslate.MLKTranslateLanguageSlovak
import cocoapods.MLKitTranslate.MLKTranslateLanguageSlovenian
import cocoapods.MLKitTranslate.MLKTranslateLanguageSpanish
import cocoapods.MLKitTranslate.MLKTranslateLanguageSwahili
import cocoapods.MLKitTranslate.MLKTranslateLanguageSwedish
import cocoapods.MLKitTranslate.MLKTranslateLanguageTagalog
import cocoapods.MLKitTranslate.MLKTranslateLanguageTamil
import cocoapods.MLKitTranslate.MLKTranslateLanguageTelugu
import cocoapods.MLKitTranslate.MLKTranslateLanguageThai
import cocoapods.MLKitTranslate.MLKTranslateLanguageTurkish
import cocoapods.MLKitTranslate.MLKTranslateLanguageUkrainian
import cocoapods.MLKitTranslate.MLKTranslateLanguageUrdu
import cocoapods.MLKitTranslate.MLKTranslateLanguageVietnamese
import cocoapods.MLKitTranslate.MLKTranslateLanguageWelsh
import cocoapods.MLKitTranslate.MLKTranslator
import cocoapods.MLKitTranslate.MLKTranslatorOptions
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class IosMlKitTranslationEngine(
    private val sourceLangHint: String?,
    private val defaultTargetLang: String
) : TranslationEngine {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun translateOnce(
        text: String,
        sourceLang: String?,
        targetLang: String,
        domain: String
    ): String {
        val sourceTag = sourceLang ?: sourceLangHint
        ?: error("Source language is required for ML Kit translation")
        val targetTag = targetLang.ifEmpty { defaultTargetLang }

        val translator = MLKTranslator.translatorWithOptions(createOptions(sourceTag, targetTag))
        val conditions = MLKModelDownloadConditions(
            allowsCellularAccess = false,
            allowsBackgroundDownloading = true
        )

        translator.awaitDownload(conditions)
        return preservePlaceholders(text) { sanitized ->
            translator.awaitTranslate(sanitized)
        }
    }

    private fun createOptions(sourceTag: String, targetTag: String): MLKTranslatorOptions {
        val targetLanguage = tagToLanguage(targetTag)
        val sourceLanguage = tagToLanguage(sourceTag)
        return MLKTranslatorOptions(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        )
    }
}

private val iosLanguageMap = buildMap {
    put("af", MLKTranslateLanguageAfrikaans)
    put("sq", MLKTranslateLanguageAlbanian)
    put("ar", MLKTranslateLanguageArabic)
    put("be", MLKTranslateLanguageBelarusian)
    put("bn", MLKTranslateLanguageBengali)
    put("bg", MLKTranslateLanguageBulgarian)
    put("ca", MLKTranslateLanguageCatalan)
    put("zh", MLKTranslateLanguageChinese)
    put("hr", MLKTranslateLanguageCroatian)
    put("cs", MLKTranslateLanguageCzech)
    put("da", MLKTranslateLanguageDanish)
    put("nl", MLKTranslateLanguageDutch)
    put("en", MLKTranslateLanguageEnglish)
    put("eo", MLKTranslateLanguageEsperanto)
    put("et", MLKTranslateLanguageEstonian)
    put("fi", MLKTranslateLanguageFinnish)
    put("fr", MLKTranslateLanguageFrench)
    put("gl", MLKTranslateLanguageGalician)
    put("ka", MLKTranslateLanguageGeorgian)
    put("de", MLKTranslateLanguageGerman)
    put("el", MLKTranslateLanguageGreek)
    put("gu", MLKTranslateLanguageGujarati)
    put("ht", MLKTranslateLanguageHaitianCreole)
    listOf("he", "iw").forEach { put(it, MLKTranslateLanguageHebrew) }
    put("hi", MLKTranslateLanguageHindi)
    put("hu", MLKTranslateLanguageHungarian)
    put("is", MLKTranslateLanguageIcelandic)
    put("id", MLKTranslateLanguageIndonesian)
    put("ga", MLKTranslateLanguageIrish)
    put("it", MLKTranslateLanguageItalian)
    put("ja", MLKTranslateLanguageJapanese)
    put("kn", MLKTranslateLanguageKannada)
    put("ko", MLKTranslateLanguageKorean)
    put("lv", MLKTranslateLanguageLatvian)
    put("lt", MLKTranslateLanguageLithuanian)
    put("mk", MLKTranslateLanguageMacedonian)
    put("ms", MLKTranslateLanguageMalay)
    put("mt", MLKTranslateLanguageMaltese)
    put("mr", MLKTranslateLanguageMarathi)
    listOf("no", "nb").forEach { put(it, MLKTranslateLanguageNorwegian) }
    put("fa", MLKTranslateLanguagePersian)
    put("pl", MLKTranslateLanguagePolish)
    listOf("pt", "pt-br", "pt-pt").forEach { put(it, MLKTranslateLanguagePortuguese) }
    put("ro", MLKTranslateLanguageRomanian)
    put("ru", MLKTranslateLanguageRussian)
    put("sk", MLKTranslateLanguageSlovak)
    put("sl", MLKTranslateLanguageSlovenian)
    put("es", MLKTranslateLanguageSpanish)
    put("sw", MLKTranslateLanguageSwahili)
    put("sv", MLKTranslateLanguageSwedish)
    listOf("tl", "fil").forEach { put(it, MLKTranslateLanguageTagalog) }
    put("ta", MLKTranslateLanguageTamil)
    put("te", MLKTranslateLanguageTelugu)
    put("th", MLKTranslateLanguageThai)
    put("tr", MLKTranslateLanguageTurkish)
    put("uk", MLKTranslateLanguageUkrainian)
    put("ur", MLKTranslateLanguageUrdu)
    put("vi", MLKTranslateLanguageVietnamese)
    put("cy", MLKTranslateLanguageWelsh)
}

private fun tagToLanguage(tag: String): String {
    val normalized = tag.lowercase()
    return iosLanguageMap[normalized]
        ?: error("Unsupported language tag: $tag")
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun MLKTranslator.awaitDownload(conditions: MLKModelDownloadConditions) {
    return suspendCancellableCoroutine { cont ->
        downloadModelIfNeededWithConditions(conditions) { error: NSError? ->
            if (!cont.isActive) return@downloadModelIfNeededWithConditions
            if (error == null) {
                cont.resume(Unit)
            } else {
                cont.resumeWithException(NSErrorException(error))
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun MLKTranslator.awaitTranslate(text: String): String {
    return suspendCancellableCoroutine { cont ->
        translateText(text) { output: String?, error: NSError? ->
            if (!cont.isActive) return@translateText
            if (error != null) {
                cont.resumeWithException(NSErrorException(error))
            } else {
                cont.resume(output ?: "")
            }
        }
    }
}

private class NSErrorException(error: NSError) : Exception(error.localizedDescription)

internal actual fun platformEngine(
    sourceLangHint: String?,
    targetLang: String
): TranslationEngine = IosMlKitTranslationEngine(sourceLangHint, targetLang)
