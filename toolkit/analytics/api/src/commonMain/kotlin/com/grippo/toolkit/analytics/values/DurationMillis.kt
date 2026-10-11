package com.grippo.toolkit.analytics.values

public class DurationMillis(public val value: Long) { init { require(value >= 0) } }
