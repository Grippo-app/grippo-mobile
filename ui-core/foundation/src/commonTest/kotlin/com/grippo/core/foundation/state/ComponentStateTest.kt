package com.grippo.core.foundation.state

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.internal.result.ResultEmitter
import com.grippo.core.foundation.internal.result.ResultManager
import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.foundation.models.BaseLoader
import com.grippo.core.foundation.models.PersistenceKey
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class ComponentStateTest {
    @Test
    fun savesTheLatestLiveDraftAndRestoresAFreshViewModelWithoutReplayingNavigation() = runTest {
        withApplication { koin, owners ->
            val first = Owner().also(owners::add)
            val original = DraftComponent(first.context, koin)
            first.lifecycle.resume()
            runCurrent()
            original.viewModel.edit("earlier draft")
            original.viewModel.edit("latest unfinished draft")
            original.viewModel.submit()
            runCurrent()
            assertEquals(listOf(Direction.Submitted), original.directions)

            val saved = archive(first.stateKeeper)
            assertSame(original.viewModel, original.contextViewModel())
            assertEquals(Draft("latest unfinished draft", 2), original.viewModel.state.value)
            first.destroy()
            runCurrent()
            val second = Owner(saved).also(owners::add)
            val restored = DraftComponent(second.context, koin)
            second.lifecycle.resume()
            runCurrent()
            assertNotSame(original.viewModel, restored.viewModel)
            assertEquals(Draft("latest unfinished draft", 2), restored.viewModel.state.value)
            assertTrue(restored.directions.isEmpty())
            restored.viewModel.edit("")
            assertEquals(Draft("", 3), restored.viewModel.state.value)
        }
    }

    @Test
    fun theSameDescriptorRemainsIndependentInTwoSiblingComponentContexts() = runTest {
        withApplication { koin, owners ->
            val first = Owner().also(owners::add)
            val left = DraftComponent(first.context.childContext("left"), koin)
            val right = DraftComponent(first.context.childContext("right"), koin)
            first.lifecycle.resume()
            runCurrent()
            left.viewModel.edit("left draft")
            right.viewModel.edit("right draft")
            right.viewModel.edit("updated right draft")
            val saved = archive(first.stateKeeper)
            first.destroy()
            runCurrent()

            val second = Owner(saved).also(owners::add)
            // Reverse construction order to prove the descriptor does not consume another owner's state.
            val restoredRight = DraftComponent(second.context.childContext("right"), koin)
            val restoredLeft = DraftComponent(second.context.childContext("left"), koin)
            second.lifecycle.resume()
            runCurrent()
            assertNotSame(left.viewModel, restoredLeft.viewModel)
            assertNotSame(right.viewModel, restoredRight.viewModel)
            assertEquals(Draft("left draft", 1), restoredLeft.viewModel.state.value)
            assertEquals(Draft("updated right draft", 2), restoredRight.viewModel.state.value)
            restoredLeft.viewModel.edit("left changed independently")
            assertEquals(Draft("updated right draft", 2), restoredRight.viewModel.state.value)
        }
    }

    @Test
    fun retainedFactoryKeepsCurrentMemoryAndReplacementSupplierSavesItForTheNextProcess() = runTest {
        withApplication { koin, owners ->
            val constructions = Constructions()
            val first = Owner().also(owners::add)
            val original = DraftComponent(first.context, koin, constructions)
            first.lifecycle.resume()
            runCurrent()
            original.viewModel.edit("archived draft")
            val saved = archive(first.stateKeeper)
            original.viewModel.edit("newer retained draft")
            first.lifecycle.destroy()
            runCurrent()
            assertEquals(0, constructions.destroyed)

            val second = Owner(saved, first.instanceKeeper).also(owners::add)
            val retained = DraftComponent(second.context, koin, constructions)
            second.lifecycle.resume()
            runCurrent()
            assertSame(original.viewModel, retained.viewModel)
            assertEquals(1, constructions.created)
            assertEquals(Draft("newer retained draft", 2), retained.viewModel.state.value)
            retained.viewModel.edit("latest retained draft")
            val latest = archive(second.stateKeeper)
            second.destroy()
            runCurrent()
            assertEquals(1, constructions.destroyed)

            val third = Owner(latest).also(owners::add)
            val fresh = DraftComponent(third.context, koin, constructions)
            third.lifecycle.resume()
            runCurrent()
            assertNotSame(retained.viewModel, fresh.viewModel)
            assertEquals(2, constructions.created)
            assertEquals(Draft("latest retained draft", 3), fresh.viewModel.state.value)
            assertTrue(fresh.directions.isEmpty())
        }
    }

    @Test
    fun aSecondOwnerInTheSameContextFailsWithoutReplacingTheFirstDraftSupplier() = runTest {
        withApplication { koin, owners ->
            val owner = Owner().also(owners::add)
            val component = DraftComponent(owner.context, koin)
            component.viewModel.edit("draft owned by the first component")
            assertFailsWith<IllegalStateException> { DraftComponent(owner.context, koin) }
            owner.lifecycle.resume()
            runCurrent()
            val saved = archive(owner.stateKeeper)
            owner.destroy()
            runCurrent()
            val freshOwner = Owner(saved).also(owners::add)
            val fresh = DraftComponent(freshOwner.context, koin)
            freshOwner.lifecycle.resume()
            runCurrent()
            assertEquals(Draft("draft owned by the first component", 1), fresh.viewModel.state.value)
        }
    }

    @Test
    fun aTypedDescriptorReadsAnArchiveWrittenWithTheLegacyLiteralKey() = runTest {
        withApplication { koin, owners ->
            val legacy = StateKeeperDispatcher()
            legacy.register("LegacyDraft", Draft.serializer()) { Draft("legacy unfinished input", 7) }
            val owner = Owner(archive(legacy)).also(owners::add)
            val component = DraftComponent(owner.context, koin)
            owner.lifecycle.resume()
            runCurrent()
            assertEquals(Draft("legacy unfinished input", 7), component.viewModel.state.value)
            component.viewModel.edit("edited after legacy restoration")
            val saved = archive(owner.stateKeeper)
            owner.destroy()
            runCurrent()
            val freshOwner = Owner(saved).also(owners::add)
            val fresh = DraftComponent(freshOwner.context, koin)
            freshOwner.lifecycle.resume()
            runCurrent()
            assertEquals(Draft("edited after legacy restoration", 8), fresh.viewModel.state.value)
        }
    }

    private fun TestScope.withApplication(block: TestScope.(Koin, MutableList<Owner>) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val app = koinApplication {
            modules(module {
                single { ResultEmitter() }
                factory { parameters -> ResultManager(get(), parameters.get()) }
            })
        }
        val owners = mutableListOf<Owner>()
        try {
            block(app.koin, owners)
        } finally {
            owners.forEach(Owner::destroy)
            runCurrent()
            app.close()
            Dispatchers.resetMain()
        }
    }

    private fun archive(keeper: StateKeeperDispatcher): SerializableContainer =
        Json.decodeFromString(Json.encodeToString(keeper.save()))

    private class Owner(
        saved: SerializableContainer? = null,
        val instanceKeeper: InstanceKeeperDispatcher = InstanceKeeperDispatcher(),
    ) {
        val lifecycle = LifecycleRegistry()
        val stateKeeper = StateKeeperDispatcher(saved)
        val context = DefaultComponentContext(lifecycle, stateKeeper, instanceKeeper)

        fun destroy() {
            lifecycle.destroy()
            instanceKeeper.destroy()
        }
    }

    private class DraftComponent(
        context: ComponentContext,
        private val koin: Koin,
        constructions: Constructions = Constructions(),
    ) : BaseComponent<Direction>(context) {
        public override val viewModel = context.retainedInstance {
            DraftViewModel(koin, restoreState(DRAFT) ?: Draft(), constructions)
        }
        val directions = mutableListOf<Direction>()

        init {
            saveState(DRAFT) { viewModel.state.value }
        }

        fun contextViewModel(): DraftViewModel = assertIs<DraftViewModel>(
            instanceKeeper.get(typeOf<DraftViewModel>())
        )

        override fun getKoin(): Koin = koin
        override suspend fun eventListener(direction: Direction) { directions += direction }
        @Composable
        override fun Render() = Unit
    }

    private class DraftViewModel(
        private val koin: Koin,
        initial: Draft,
        private val constructions: Constructions,
    ) : BaseViewModel<Draft, Direction, Loader>(initial) {
        init {
            constructions.created++
        }

        fun edit(text: String) { update { it.copy(text = text, revisions = it.revisions + 1) } }
        fun submit() { navigateTo(Direction.Submitted) }
        override fun getKoin(): Koin = koin
        override fun onDestroy() {
            constructions.destroyed++
            super.onDestroy()
        }
    }

    private class Constructions {
        var created = 0
        var destroyed = 0
    }

    @Serializable
    private data class Draft(val text: String = "", val revisions: Int = 0)

    private enum class Direction : BaseDirection { Submitted }
    private sealed interface Loader : BaseLoader

    private companion object {
        val DRAFT = PersistenceKey("LegacyDraft", Draft.serializer())
    }
}
