package com.grippo.exercise.components

import androidx.compose.runtime.Composable
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg

internal fun IterationState.effectiveLoadOrNull(): Float? {
    val external = externalWeight.value
    val extra = extraWeight.value
    val assist = assistWeight.value
    val body = bodyWeight.value
    val multiplier = bodyMultiplier.value
    if (body != null && multiplier == null) {
        return null
    }
    if (external == null && extra == null && assist == null && body == null) {
        return null
    }

    return (external ?: 0f) + (extra ?: 0f) + (body ?: 0f) * (multiplier ?: 0f) - (assist ?: 0f)
}

@Composable
internal fun Float?.exerciseLoadText(): String {
    val formatted = this?.let { VolumeFormatState.of(it) } ?: return "—"
    return when (formatted.value) {
        null -> "0${AppTokens.strings.res(Res.string.kg)}"
        else -> formatted.short()
    }
}

internal fun ExerciseState.maximumRecordedLoad(): Float? {
    return iterations
        .filter { (it.repetitions.value ?: 0) > 0 }
        .mapNotNull { it.effectiveLoadOrNull() }
        .maxOrNull()
}

internal fun ExerciseState.hasComparableLoadTo(other: ExerciseState): Boolean {
    val current = loadBases()
    val previous = other.loadBases()
    return current.isNotEmpty() && null !in current && current == previous
}

internal fun IterationState.hasComparableLoadTo(other: IterationState): Boolean {
    val current = loadBasis() ?: return false
    return current == other.loadBasis()
}

private fun ExerciseState.loadBases(): Set<LoadBasis?> {
    return iterations
        .filter { (it.repetitions.value ?: 0) > 0 }
        .map { it.loadBasis() }
        .toSet()
}

private fun IterationState.loadBasis(): LoadBasis? {
    effectiveLoadOrNull() ?: return null
    return LoadBasis(
        external = externalWeight.value != null,
        body = bodyWeight.value != null,
        assisted = assistWeight.value != null,
    )
}

private data class LoadBasis(
    val external: Boolean,
    val body: Boolean,
    val assisted: Boolean,
)
