package com.grippo.core.foundation.models

public sealed interface ErrorRecovery {
    public data object Retry : ErrorRecovery
    public class Close(public val onClose: () -> Unit) : ErrorRecovery
}