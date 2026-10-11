package com.grippo.toolkit.connectivity.internal

import com.grippo.toolkit.connectivity.Connectivity
import com.grippo.toolkit.connectivity.ConnectivityOptions
import com.grippo.toolkit.connectivity.ConnectivityProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class DefaultConnectivity(
    scope: CoroutineScope,
    private val provider: ConnectivityProvider,
    options: ConnectivityOptions,
) : Connectivity, CoroutineScope by scope {

    private val monitorLock = createConnectivityLock()
    private var job: Job? = null

    private val _statusUpdates = MutableSharedFlow<Connectivity.Status>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val statusUpdates: SharedFlow<Connectivity.Status> = _statusUpdates.asSharedFlow()

    private val _monitoring = MutableStateFlow(false)
    override val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()

    init {
        if (options.autoStart) {
            start()
        }
    }

    override suspend fun status(): Connectivity.Status {
        return provider.monitor().first().also { status ->
            _statusUpdates.emit(status)
        }
    }

    override fun start() {
        val monitoringJob = monitorLock.withLock {
            job?.cancel()
            val newJob = launch(start = CoroutineStart.LAZY) {
                provider.monitor().collect { status ->
                    _statusUpdates.emit(status)
                }
            }
            job = newJob
            _monitoring.value = true
            newJob.invokeOnCompletion {
                monitorLock.withLock {
                    if (job === newJob) {
                        job = null
                        _monitoring.value = false
                    }
                }
            }
            newJob
        }
        monitoringJob.start()
    }

    override fun stop() {
        monitorLock.withLock {
            job?.cancel()
            job = null
            _monitoring.value = false
        }
    }
}
