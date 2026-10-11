package com.grippo.toolkit.analytics.values

public class AnalyticsNumber(public val value: Double) { init { require(value.isFinite()) } }
