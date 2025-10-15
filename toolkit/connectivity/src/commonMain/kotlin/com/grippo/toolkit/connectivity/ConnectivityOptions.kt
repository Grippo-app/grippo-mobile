package com.grippo.toolkit.connectivity

public open class ConnectivityOptions(
    public val autoStart: Boolean = DEFAULT_AUTO_START,
) {

    public class Builder internal constructor() {
        public var autoStart: Boolean = DEFAULT_AUTO_START
        public fun autoStart(autoStart: Boolean): Builder = apply { this.autoStart = autoStart }
        public fun build(): ConnectivityOptions = ConnectivityOptions(autoStart)
    }

    public companion object {

        private const val DEFAULT_AUTO_START: Boolean = false

        public fun build(
            block: Builder.() -> Unit,
        ): ConnectivityOptions = Builder().apply(block).build()
    }
}
