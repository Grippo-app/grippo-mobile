package com.grippo.domain.state.metrics.profile

import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.profile.GoalAnalysisEvidenceState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.data.features.api.metrics.profile.models.GoalAdherence
import com.grippo.domain.state.user.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

public fun GoalAdherence.toState(now: LocalDateTime): GoalProgressState {
    return GoalProgressState(
        goal = goal.toState(),
        now = DateTimeFormatState.of(
            value = now,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateDdMmm
        ),
        score = score,
        strengthShare = strengthShare,
        hypertrophyShare = hypertrophyShare,
        enduranceShare = enduranceShare,
        sessionCount = sessionCount,
        findings = findings.map { it.toState() }.toPersistentList(),
        evidence = GoalAnalysisEvidenceState(
            range = DateRangeFormatState.of(from = analysisRange?.from, to = analysisRange?.to),
            setCount = setCount,
            excludedSetCount = excludedSetCount,
            missingMetadataSetCount = missingMetadataSetCount,
        ),
    )
}
