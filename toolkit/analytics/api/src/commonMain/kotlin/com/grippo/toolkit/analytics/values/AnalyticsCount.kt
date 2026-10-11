package com.grippo.toolkit.analytics.values

public class AnalyticsCount(public val value: Long) { init { require(value >= 0) } }
