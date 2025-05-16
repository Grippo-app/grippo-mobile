package com.grippo.konfetti.compose

import com.grippo.konfetti.core.PartySystem

public interface OnParticleSystemUpdateListener {
    public fun onParticleSystemEnded(
        system: PartySystem,
        activeSystems: Int,
    )
}