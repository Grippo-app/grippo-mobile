package com.grippo.data.features.api.training.models

/**
 * Validation result for a single exercise.
 *
 * Contains the full iteration list so the UI can render the complete exercise
 * in context — showing every set with its optional artifact highlighted —
 * rather than receiving a disconnected list of flagged positions.
 *
 * A `null` [ExerciseArtifacts] from
 * [com.grippo.data.features.api.training.ExerciseValidatorUseCase] means the
 * exercise looks clean.
 *
 * @property exerciseName  display name of the exercise
 * @property iterations    every iteration of the exercise, in original order,
 *                         paired with an [Artifact] when something looks wrong
 *                         or `null` when the iteration appears normal
 */
public data class ExerciseArtifacts(
    val exerciseName: String,
    val iterations: List<Pair<SetIteration, Artifact?>>,
) {
    /**
     * Describes what looks wrong in a single iteration.
     *
     * [expectedMedian] is always the median of the *other* iterations in the
     * same exercise, giving the UI a concrete "did you mean X?" hint.
     */
    public sealed interface Artifact {

        /**
         * The external weight looks anomalous.
         * Typical cause: a missing digit when typing (e.g. "10" instead of "100").
         */
        public data class SuspiciousWeight(
            val actual: Float,
            val expectedMedian: Float,
        ) : Artifact

        /**
         * The repetition count looks anomalous.
         * Typical cause: an extra or missing digit (e.g. "50" instead of "5"),
         * or a zero entered by accident.
         */
        public data class SuspiciousRepetitions(
            val actual: Int,
            val expectedMedian: Float,
        ) : Artifact

        /**
         * The computed volume (total load) looks anomalous.
         * Reported for bodyweight and assisted exercises where `externalWeight`
         * is absent.  Typical cause: a wrong `bodyMultiplier` or `assistWeight`.
         */
        public data class SuspiciousVolume(
            val actual: Float,
            val expectedMedian: Float,
        ) : Artifact
    }
}
