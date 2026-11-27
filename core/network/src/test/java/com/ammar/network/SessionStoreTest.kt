package com.ammar.network

import com.ammar.network.session.Account
import com.ammar.network.session.PendingSignIn
import com.ammar.network.session.SessionState
import com.ammar.network.session.SessionStore
import com.ammar.network.session.StoredSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

class SessionStoreTest {

    private val prefs = MemoryStore()
    private val key = newKey()

    private fun open(now: Long = 0) =
        SessionStore(storage(prefs, key), Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC))

    private val signedIn = SessionState.SignedIn(Account("7", "tester"))

    @Test
    fun `a saved session survives a restart`() {
        open().save(StoredSession("test-session", "7", "tester"))

        val reopened = open()

        assertEquals(signedIn, reopened.state.value)
        assertEquals("test-session", reopened.sessionId())
    }

    @Test
    fun `nothing is written in the clear`() {
        val store = open()
        store.save(StoredSession("test-session", "7", "tester"))
        store.savePending(PendingSignIn("test-token", "test-secret", 5))

        assertEquals(setOf("session", "pending_sign_in"), prefs.values.keys)
        for (value in prefs.values.values) {
            val bytes = Base64.getDecoder().decode(value).decodeToString()
            for (secret in listOf("test-session", "tester", "test-token", "test-secret")) {
                assertFalse(secret in value)
                assertFalse(secret in bytes)
            }
        }
    }

    @Test
    fun `the same value is encrypted differently every time`() {
        val store = open()
        store.save(StoredSession("test-session", "7", "tester"))
        val first = prefs.values.getValue("session")
        store.save(StoredSession("test-session", "7", "tester"))

        assertNotEquals(first, prefs.values.getValue("session"))
    }

    @Test
    fun `a value that was tampered with or lost its key means signed out`() {
        open().save(StoredSession("test-session", "7", "tester"))

        assertEquals(SessionState.SignedOut, SessionStore(storage(prefs, newKey()), Clock.systemUTC()).state.value)

        val bytes = Base64.getDecoder().decode(prefs.values.getValue("session"))
        bytes[bytes.lastIndex] = (bytes.last() + 1).toByte()
        prefs.values["session"] = Base64.getEncoder().encodeToString(bytes)
        assertEquals(SessionState.SignedOut, open().state.value)

        prefs.values["session"] = "not base64 !"
        assertNull(open().sessionId())
    }

    @Test
    fun `clearing removes the stored session`() {
        val store = open()
        store.save(StoredSession("test-session", "7", "tester"))

        store.clearSessionIfCurrent("another-session")
        assertEquals(signedIn, store.state.value)
        store.clearSessionIfCurrent("test-session")

        assertEquals(SessionState.SignedOut, store.state.value)
        assertEquals(emptyMap<String, String>(), prefs.values)
    }

    @Test
    fun `printing the stored objects does not reveal them`() {
        assertFalse("test-session" in StoredSession("test-session", "7", "tester").toString())
        val pending = PendingSignIn("test-token", "test-secret", 5).toString()
        assertFalse("test-token" in pending || "test-secret" in pending)
    }

    @Test
    fun `an expired pending sign-in is removed when the app starts again`() {
        open().savePending(PendingSignIn("test-token", "test-secret", 1000))

        assertEquals(setOf("pending_sign_in"), open(now = 999).let { prefs.values.keys.toSet() })
        val later = open(now = 1000)

        assertEquals(emptySet<String>(), prefs.values.keys)
        assertFalse(later.takeLapsed("another-token"))
        assertTrue(later.takeLapsed("test-token"))
        assertFalse(later.takeLapsed("test-token"))
    }
}
