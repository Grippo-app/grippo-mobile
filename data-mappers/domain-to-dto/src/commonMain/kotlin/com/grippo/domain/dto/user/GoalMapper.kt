package com.grippo.domain.dto.user

import com.grippo.data.features.api.goal.models.SetGoal
import com.grippo.services.backend.dto.user.SetGoalBody
import com.grippo.toolkit.date.utils.DateTimeUtils

public fun SetGoal.toBody(): SetGoalBody {
    return SetGoalBody(
        primaryGoal = primaryGoal.key,
        secondaryGoal = secondaryGoal?.key,
        target = DateTimeUtils.toUtcIso(target),
        personalizations = personalizations.map { it.key },
    )
}
