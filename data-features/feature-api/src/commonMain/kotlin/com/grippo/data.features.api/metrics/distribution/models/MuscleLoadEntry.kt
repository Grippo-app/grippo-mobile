package com.grippo.data.features.api.metrics.distribution.models

import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public data class MuscleLoadEntry(
    val group: MuscleGroupEnum,
    val percentage: Float,
    val muscles: List<MuscleEnum>,

    /**
     * Sessions with any positive weighted-set contribution, irrespective of unrelated work.
     */
    val hitTrainingsCount: Int = 0,

    /**
     * Sessions where this group tied for the highest weighted-set contribution.
     * Zero for individual-muscle entries.
     */
    val primaryTrainingsCount: Int = 0,

    /**
     * Average estimated weighted sets per contributing session.
     */
    val avgWeightedSetsPerSession: Float = 0f,

    /**
     * Maximum estimated weighted sets in a session.
     */
    val maxWeightedSetsInOneSession: Float = 0f,

    /**
     * Average assigned measured resistance × repetitions across sessions with at least one
     * measured set for this entry, including known zero resistance. Unmeasured sets are omitted.
     */
    val avgVolumePerHitSession: Float = 0f,

    /**
     * Maximum assigned measured resistance × repetitions in a session; unmeasured sets are omitted.
     */
    val maxVolumeInOneSession: Float = 0f,

    /**
     * Up to three exercise-example IDs ranked by estimated weighted sets, then ID.
     */
    val topExampleIds: List<String> = emptyList(),
)
