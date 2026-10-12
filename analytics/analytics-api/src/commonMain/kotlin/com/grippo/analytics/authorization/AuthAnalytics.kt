package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthTransition
import com.grippo.analytics.authorization.models.AuthorizationState
import com.grippo.analytics.authorization.models.EntryPoint
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.analytics.authorization.models.Step
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.FlowId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

/** Stateless feature entry point. Each call creates independent scenario state. */
public interface AuthAnalytics {
    /** A restored snapshot must belong to the supplied route flowId. */
    public fun authorization(
        entryPoint: EntryPoint,
        flowId: FlowId,
        restored: AuthorizationState? = null,
    ): Authorization

    public fun session(restored: FlowContext? = null): Session

    public interface Authorization {
        public fun presentScreen(step: Step): ScreenPresentation
        public fun trackTransition(transition: AuthTransition)
        public suspend fun <T> trackOperation(operation: AuthOperation.Attempt, block: suspend () -> T): T
        public fun save(): AuthorizationState
    }

    public interface Session {
        public fun presentScreen(): ScreenPresentation

        /** Observes execution only; caller owns business results, fallback and navigation. */
        public suspend fun <T> trackOperation(operation: AuthOperation.SessionCheck, block: suspend () -> T): T

        public fun save(): FlowContext
    }

    /** Only bounded facts cross into analytics; no provider/domain exception types are imported. */
    public interface FailureClassifier {
        public fun isProviderCancellation(error: Exception): Boolean
        public fun category(error: Exception): FailureCategory
    }
}
