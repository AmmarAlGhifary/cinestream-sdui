package com.ammar.network

import com.ammar.network.session.Account
import com.ammar.network.session.PendingSignIn
import com.ammar.network.session.SessionState
import com.ammar.network.session.SessionStore
import com.ammar.network.session.StoredSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.InvalidKeyException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import java.time.Clock

/** A Keystore key that stops working must not lock the user out until app data is cleared. */
class KeyRecoveryTest {

    private val prefs = MemoryStore()
    private val keys = FakeKeys()

    private fun open() = SessionStore(storage(prefs, keys), Clock.systemUTC())

    private val session = StoredSession("test-session", "7", "tester")
    private val pending = PendingSignIn("test-token", "test-secret", Long.MAX_VALUE)

    @Test
    fun `a write with a broken key replaces the key and succeeds`() {
        for (failure in listOf(
            InvalidKeyException(), UnrecoverableKeyException(), ProviderException("keystore failure")
        )) {
            val store = open()
            store.save(session)
            keys.broken = failure
            val before = keys.deletes

            store.savePending(pending)

            assertEquals(failure.toString(), before + 1, keys.deletes)
            // The session was sealed with the old key, so it is gone, in memory as well.
            assertEquals(SessionState.SignedOut, store.state.value)
            assertNull(store.sessionId())
            assertEquals(setOf("pending_sign_in"), prefs.values.keys)
            assertEquals("test-token", open().pending()?.requestToken)
        }
    }

    @Test
    fun `a new sign-in works after the key broke`() {
        open().save(session)
        keys.broken = InvalidKeyException()

        val store = open()
        assertEquals(SessionState.SignedOut, store.state.value)
        store.save(session)

        assertEquals(SessionState.SignedIn(Account("7", "tester")), open().state.value)
    }

    @Test
    fun `a read with a broken key drops what was stored`() {
        open().save(session)
        keys.broken = UnrecoverableKeyException()

        assertNull(open().sessionId())

        assertEquals(1, keys.deletes)
        assertEquals(emptyMap<String, String>(), prefs.values)
    }

    @Test
    fun `a key that stays broken is retried only once`() {
        val store = open()
        keys.broken = InvalidKeyException()
        keys.permanent = true

        assertThrows(InvalidKeyException::class.java) { store.save(session) }

        assertEquals(1, keys.deletes)
        assertEquals(SessionState.SignedOut, store.state.value)
    }

    @Test
    fun `an unrelated failure leaves the key alone`() {
        val store = open()
        keys.broken = IllegalStateException()

        assertThrows(IllegalStateException::class.java) { store.save(session) }

        assertEquals(0, keys.deletes)
    }
}
