package com.grippo.platform.core

import android.content.Context

public actual class NativeContext actual constructor() {
    public lateinit var context: Context
        private set

    public constructor(context: Context) : this() {
        this.context = context
    }
}