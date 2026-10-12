package com.grippo.core.foundation

import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.foundation.models.BaseLoader
import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.ksp.generated.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BaseViewModelErrorTest {

    @Test
    fun mappingOnlyChangesPresentationAndCloseCallbackRemainsDeferred() = runTest {
        for (kind in LaunchKind.entries) {
            withViewModel { viewModel, recording ->
                val original = IllegalStateException("Original failure")
                val display = IllegalArgumentException("Presentation failure")
                val mapped = mutableListOf<Throwable>()
                var continued = false
                var closed = false
                val job = viewModel.fail(
                    kind = kind,
                    error = original,
                    mapError = {
                        recording.order += "map"
                        mapped += it
                        display
                    },
                    onError = {
                        recording.order += "close"
                        closed = true
                    },
                    afterFailure = {
                        continued = true
                    },
                )

                assertEquals(kind.initialLoaders, viewModel.loaders.value)
                advanceUntilIdle()

                assertOriginal(original, recording.crashes.single())
                assertSame(recording.crashes.single(), mapped.single())
                assertSame(display, recording.dialogs.single())
                assertEquals(listOf("report", "map", "dialog"), recording.order)
                assertTrue(job.isCompleted)
                assertTrue(viewModel.loaders.value.isEmpty())
                assertFalse(continued)
                assertFalse(closed)

                recording.closeCallbacks.single().invoke()

                assertTrue(closed)
                assertEquals(listOf("report", "map", "dialog", "close"), recording.order)
            }
        }
    }

    @Test
    fun defaultHandlerPassesTheOriginalFailureToBothProviders() = runTest {
        for (kind in LaunchKind.entries) {
            withViewModel { viewModel, recording ->
                val original = IllegalStateException("Original failure")
                viewModel.fail(kind, original)

                advanceUntilIdle()

                assertOriginal(original, recording.crashes.single())
                assertSame(recording.crashes.single(), recording.dialogs.single())
                assertEquals(listOf("report", "dialog"), recording.order)
                assertTrue(viewModel.loaders.value.isEmpty())
            }
        }
    }

    @Test
    fun cancellationBypassesMappingReportingAndDialogAndReleasesLoader() = runTest {
        for (kind in LaunchKind.entries) {
            withViewModel { viewModel, recording ->
                var mapped = false
                var closed = false
                var continued = false
                val job = viewModel.fail(
                    kind = kind,
                    error = CancellationException("Cancelled operation"),
                    mapError = {
                        mapped = true
                        it
                    },
                    onError = {
                        closed = true
                    },
                    afterFailure = {
                        continued = true
                    },
                )

                advanceUntilIdle()

                assertTrue(job.isCancelled)
                assertTrue(viewModel.loaders.value.isEmpty())
                assertTrue(recording.order.isEmpty())
                assertTrue(recording.crashes.isEmpty())
                assertTrue(recording.dialogs.isEmpty())
                assertTrue(recording.closeCallbacks.isEmpty())
                assertFalse(mapped)
                assertFalse(closed)
                assertFalse(continued)
            }
        }
    }

    @Test
    fun destructionCancelsPendingWorkWithoutCallingErrorHooks() = runTest {
        for (kind in LaunchKind.entries) {
            withViewModel { viewModel, recording ->
                var mapped = false
                var closed = false
                val mapError: (Throwable) -> Throwable = {
                    mapped = true
                    it
                }
                val onError: () -> Unit = {
                    closed = true
                }
                val job = when (kind) {
                    LaunchKind.Block -> viewModel.launchBlock(mapError, onError) {
                        awaitCancellation()
                    }

                    LaunchKind.Flow -> viewModel.launchFlow(
                        upstream = flow<Unit> {
                            awaitCancellation()
                        },
                        mapError = mapError,
                        onError = onError,
                    )
                }

                runCurrent()
                assertTrue(job.isActive)
                assertEquals(kind.initialLoaders, viewModel.loaders.value)

                viewModel.onDestroy()
                runCurrent()

                assertTrue(job.isCancelled)
                assertTrue(viewModel.loaders.value.isEmpty())
                assertTrue(recording.order.isEmpty())
                assertTrue(recording.crashes.isEmpty())
                assertTrue(recording.dialogs.isEmpty())
                assertFalse(mapped)
                assertFalse(closed)
            }
        }
    }

    @Test
    fun fatalErrorsStayInTheExistingPipelineAndAreNeverMapped() = runTest {
        for (kind in LaunchKind.entries) {
            withViewModel { viewModel, recording ->
                val original = Error("Fatal failure")
                var mapped = false
                viewModel.fail(
                    kind = kind,
                    error = original,
                    mapError = {
                        mapped = true
                        IllegalStateException("Must not replace a fatal error")
                    },
                )

                advanceUntilIdle()

                assertOriginal(original, recording.crashes.single())
                assertSame(recording.crashes.single(), recording.dialogs.single())
                assertEquals(listOf("report", "dialog"), recording.order)
                assertFalse(mapped)
                assertTrue(viewModel.loaders.value.isEmpty())
            }
        }
    }

    @Test
    fun successfulOperationsDoNotEnterTheErrorPipeline() = runTest {
        for (kind in LaunchKind.entries) {
            withViewModel { viewModel, recording ->
                var completed = false
                var mapped = false
                var closed = false
                val mapError: (Throwable) -> Throwable = {
                    mapped = true
                    it
                }
                val onError: () -> Unit = {
                    closed = true
                }
                val job = when (kind) {
                    LaunchKind.Block -> viewModel.launchBlock(mapError, onError) {
                        completed = true
                    }

                    LaunchKind.Flow -> viewModel.launchFlow(
                        upstream = flow {
                            completed = true
                            emit(Unit)
                        },
                        mapError = mapError,
                        onError = onError,
                    )
                }

                advanceUntilIdle()

                assertTrue(job.isCompleted)
                assertFalse(job.isCancelled)
                assertTrue(completed)
                assertFalse(mapped)
                assertFalse(closed)
                assertTrue(recording.order.isEmpty())
                assertTrue(recording.crashes.isEmpty())
                assertTrue(recording.dialogs.isEmpty())
                assertTrue(recording.closeCallbacks.isEmpty())
                assertTrue(viewModel.loaders.value.isEmpty())
            }
        }
    }

    private fun TestScope.withViewModel(
        verify: TestScope.(TestViewModel, RecordingErrors) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val recording = RecordingErrors()
        val app = koinApplication {
            modules(
                CoreModule().module,
                module {
                    single<ErrorProvider> {
                        recording
                    }
                    single<CrashReporting> {
                        recording
                    }
                },
            )
        }
        val viewModel = TestViewModel(app.koin)
        try {
            verify(viewModel, recording)
        } finally {
            viewModel.onDestroy()
            app.close()
            Dispatchers.resetMain()
        }
    }

    private fun TestViewModel.fail(
        kind: LaunchKind,
        error: Throwable,
        mapError: ((Throwable) -> Throwable)? = null,
        onError: () -> Unit = {},
        afterFailure: () -> Unit = {},
    ): Job {
        val fail: suspend () -> Unit = {
            throw error
        }
        return when (kind) {
            LaunchKind.Block -> launchBlock(mapError, onError) {
                fail()
                afterFailure()
            }

            LaunchKind.Flow -> launchFlow(
                upstream = flow<Unit> {
                    fail()
                    afterFailure()
                },
                mapError = mapError,
                onError = onError,
            )
        }
    }

    private fun assertOriginal(expected: Throwable, reported: Throwable) {
        // JVM coroutine stack recovery may copy an exception with its original as the cause.
        assertTrue(reported === expected || reported.cause === expected)
    }

    private enum class LaunchKind {
        Block,
        Flow;

        val initialLoaders: Set<TestLoader>
            get() = when (this) {
                Block -> setOf(TestLoader.Operation)
                Flow -> emptySet()
            }
    }

    private enum class TestLoader : BaseLoader {
        Operation,
    }

    private sealed interface TestDirection : BaseDirection

    private class TestViewModel(private val koin: Koin) :
        BaseViewModel<Unit, TestDirection, TestLoader>(Unit) {

        override fun getKoin(): Koin = koin

        fun launchBlock(
            mapError: ((Throwable) -> Throwable)?,
            onError: () -> Unit,
            block: suspend CoroutineScope.() -> Unit,
        ): Job {
            return if (mapError == null) {
                safeLaunch(
                    loader = TestLoader.Operation,
                    onError = onError,
                    block = block,
                )
            } else {
                safeLaunch(
                    loader = TestLoader.Operation,
                    onError = onError,
                    mapError = mapError,
                    block = block,
                )
            }
        }

        fun launchFlow(
            upstream: Flow<Unit>,
            mapError: ((Throwable) -> Throwable)?,
            onError: () -> Unit,
        ): Job {
            return if (mapError == null) {
                upstream.safeLaunch(
                    processing = Processing.Infinity,
                    onError = onError,
                )
            } else {
                upstream.safeLaunch(
                    processing = Processing.Infinity,
                    onError = onError,
                    mapError = mapError,
                )
            }
        }
    }

    private class RecordingErrors : ErrorProvider, CrashReporting {
        val order = mutableListOf<String>()
        val crashes = mutableListOf<Throwable>()
        val dialogs = mutableListOf<Throwable>()
        val closeCallbacks = mutableListOf<() -> Unit>()

        override fun reportException(throwable: Throwable, context: CrashReportContext) {
            order += "report"
            crashes += throwable
        }

        override fun logBreadcrumb(code: CrashDiagnosticCode) = Unit

        override suspend fun provide(exception: Throwable, callback: () -> Unit) {
            order += "dialog"
            dialogs += exception
            closeCallbacks += callback
        }
    }
}
