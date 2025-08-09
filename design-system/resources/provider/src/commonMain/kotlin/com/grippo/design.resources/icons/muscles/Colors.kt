package com.grippo.design.resources.icons.muscles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public class MuscleColorProperties(
    public val muscle: Color,
    public val background: Color,
    public val defaultFront: Color,
    public val defaultBack: Color,
    public val backgroundFront: Color,
    public val backgroundBack: Color,
    public val outline: Color,
)

@Immutable
public data class MuscleColorPreset(
    val biceps: Color,
    val triceps: Color,
    val forearm: Color,
    val forearmFront: Color,
    val forearmBack: Color,

    val lateralDeltoid: Color,
    val anteriorDeltoid: Color,
    val posteriorDeltoid: Color,

    val pectoralisMajorAbdominal: Color,
    val pectoralisMajorClavicular: Color,
    val pectoralisMajorSternocostal: Color,

    val rectusAbdominis: Color,
    val obliquesAbdominis: Color,

    val rhomboids: Color,
    val latissimus: Color,
    val trapezius: Color,
    val teresMajor: Color,

    val gluteal: Color,
    val hamstrings: Color,
    val calf: Color,
    val quadriceps: Color,
    val adductors: Color,
    val abductors: Color,

    val other: Color,

    val outline: Color,
    val backgroundFront: Color,
    val backgroundBack: Color
)