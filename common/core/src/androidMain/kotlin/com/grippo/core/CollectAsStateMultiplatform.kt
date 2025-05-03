package com.grippo.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

@Composable
public actual fun <T> StateFlow<T>.collectAsStateMultiplatform(
    context: CoroutineContext,
): State<T> = collectAsStateWithLifecycle(context = context)
