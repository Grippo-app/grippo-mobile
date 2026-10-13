package com.grippo.analytics.muscle.loading.details

import com.grippo.analytics.muscle.loading.details.models.MuscleLoadingDetailsEvent

public interface MuscleLoadingDetailsAnalytics {
    public fun trackEvent(event: MuscleLoadingDetailsEvent)
}
