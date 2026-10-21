package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InFlightTest {

    private fun <V> TestScope.flights(): InFlight<String, V> =
        InFlight(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))

    @Test
    fun `concurrent joins on one key run the block once and share its value`() = runTest {
        val inFlight = flights<Int>()
        val gate = CompletableDeferred<Unit>()
        var runs = 0
        val first = async { inFlight.join("k") { runs++; gate.await(); 42 } }
        val second = async { inFlight.join("k") { runs++; gate.await(); 7 } }
        runCurrent()
        gate.complete(Unit)

        assertThat(first.await()).isEqualTo(42)
        assertThat(second.await()).isEqualTo(42)
        assertThat(runs).isEqualTo(1)
    }

    @Test
    fun `a finished flight is forgotten so the next join runs again`() = runTest {
        val inFlight = flights<Int>()
        var runs = 0

        assertThat(inFlight.join("k") { ++runs }).isEqualTo(1)
        assertThat(inFlight.join("k") { ++runs }).isEqualTo(2)
    }

    @Test
    fun `different keys run independently`() = runTest {
        val inFlight = flights<String>()
        val gate = CompletableDeferred<Unit>()
        val slow = async { inFlight.join("a") { gate.await(); "A" } }
        val fast = async { inFlight.join("b") { "B" } }

        assertThat(fast.await()).isEqualTo("B")
        assertThat(slow.isCompleted).isFalse()
        gate.complete(Unit)
        assertThat(slow.await()).isEqualTo("A")
    }

    @Test
    fun `cancelling one waiter does not cancel the shared work`() = runTest {
        val inFlight = flights<Int>()
        val gate = CompletableDeferred<Unit>()
        val first = async { inFlight.join("k") { gate.await(); 5 } }
        val second = async { inFlight.join("k") { gate.await(); 6 } }
        runCurrent()
        first.cancel()
        gate.complete(Unit)

        assertThat(second.await()).isEqualTo(5)
    }

    @Test
    fun `a failing block fails every waiter and is not cached`() = runTest {
        val inFlight = flights<Int>()
        val gate = CompletableDeferred<Unit>()
        val first = async { runCatching { inFlight.join("k") { gate.await(); error("boom") } } }
        val second = async { runCatching { inFlight.join("k") { 1 } } }
        runCurrent()
        gate.complete(Unit)

        assertThat(first.await().isFailure).isTrue()
        assertThat(second.await().isFailure).isTrue()
        assertThat(inFlight.join("k") { 9 }).isEqualTo(9)
    }
}
