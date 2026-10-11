package com.grippo.toolkit.analytics.lifecycle

/** Close on pause/destroy. An older presentation cannot clear a newer screen's context. */
public interface ScreenPresentation {
    public fun close()
}
