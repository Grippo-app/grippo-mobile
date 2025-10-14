package com.grippo.ml.translation.internal

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class AndroidMlKitTranslationEngine(
    private val sourceLangHint: String?,
    private val defaultTargetLang: String
) : TranslationEngine {

    override suspend fun translateOnce(
        text: String,
        sourceLang: String?,
        targetLang: String,
        domain: String
    ): String {
        val sourceTag = sourceLang ?: sourceLangHint
        ?: error("Source language is required for ML Kit translation")
        val targetTag = targetLang.ifEmpty { defaultTargetLang }
        val translator = createTranslator(sourceTag, targetTag)

        return translator.useAsync { client ->
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            client.awaitDownload(conditions)
            preservePlaceholders(text) { sanitized ->
                client.awaitTranslate(sanitized)
            }
        }
    }

    private fun createTranslator(sourceTag: String, targetTag: String): Translator {
        val targetLanguage = tagToMlKit(targetTag)
        val builder = TranslatorOptions.Builder()
            .setTargetLanguage(targetLanguage)

        val sourceLanguage = tagToMlKit(sourceTag)
        val options = builder.setSourceLanguage(sourceLanguage).build()

        return Translation.getClient(options)
    }

    private fun tagToMlKit(tag: String): String {
        return TranslateLanguage.fromLanguageTag(tag)
            ?: error("Unsupported language tag: $tag")
    }
}

private suspend fun <T> Translator.useAsync(block: suspend (Translator) -> T): T {
    return try {
        block(this)
    } finally {
        close()
    }
}

private suspend fun Translator.awaitDownload(conditions: DownloadConditions) {
    return suspendCancellableCoroutine { cont ->
        downloadModelIfNeeded(conditions)
            .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            .addOnFailureListener { error ->
                if (cont.isActive) cont.resumeWithException(error)
            }
    }
}

private suspend fun Translator.awaitTranslate(text: String): String {
    return suspendCancellableCoroutine { cont ->
        translate(text)
            .addOnSuccessListener { output ->
                if (cont.isActive) cont.resume(output)
            }
            .addOnFailureListener { error ->
                if (cont.isActive) cont.resumeWithException(error)
            }
    }
}

internal actual fun platformEngine(
    sourceLangHint: String?,
    targetLang: String
): TranslationEngine = AndroidMlKitTranslationEngine(sourceLangHint, targetLang)
