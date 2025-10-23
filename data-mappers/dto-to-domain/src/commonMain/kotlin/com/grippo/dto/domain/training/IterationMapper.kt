package com.grippo.dto.domain.training

import com.grippo.backend.dto.training.IterationResponse
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.toolkit.logger.AppLogger

public fun List<IterationResponse>.toDomain(): List<Iteration> {
    return mapNotNull { it.toDomainOrNull() }
}

public fun IterationResponse.toDomainOrNull(): Iteration? {
    val domainId = AppLogger.Mapping.log(id) {
        "IterationResponse.id is null"
    } ?: return null

    val domainWeight = AppLogger.Mapping.log(weight) {
        "IterationResponse.weight is null"
    } ?: return null

    val domainRepetitions = AppLogger.Mapping.log(repetitions) {
        "IterationResponse.repetitions is null"
    } ?: return null

    return Iteration(
        id = domainId,
        volume = domainWeight,
        repetitions = domainRepetitions,
    )
}