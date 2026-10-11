package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.state.PushTokenFailure
import com.grippo.toolkit.messaging.state.PushTokenState
import com.grippo.toolkit.messaging.token.PushToken
import com.grippo.toolkit.messaging.transport.PushTokenParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PushTokenContractTest {
    @Test
    fun parserPreservesOpaqueTokensAtAcceptedLengthBoundaries() {
        for (rawToken in listOf("a", "ABC:def-_012", "x".repeat(4096))) {
            val state = assertIs<PushTokenState.Available>(PushTokenParser.parse(rawToken))
            assertEquals(rawToken, state.token.value)
            assertEquals(PushToken(rawToken), state.token)
        }
    }

    @Test
    fun parserAndConstructorRejectTheSameInvalidTransportInputs() {
        for (rawToken in listOf("", "x".repeat(4097), "a b", "a\tb", "a\nb", "a\u0000b", "a\u007Fb")) {
            assertFailsWith<IllegalArgumentException> { PushToken(rawToken) }
            val state = assertIs<PushTokenState.Failed>(PushTokenParser.parse(rawToken))
            assertEquals(PushTokenFailure.InvalidToken, state.reason)
        }
    }
}
