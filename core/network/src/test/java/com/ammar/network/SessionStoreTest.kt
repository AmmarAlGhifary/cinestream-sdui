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
import org.junit.Test
import java.util.Base64

class SessionStoreTest {

    private val prefs = MemoryStore()
    private val key = newKey()

    private fun open() = SessionStore(storage(prefs, key))

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
        store.savePending(PendingSignIn("test-token", 5))

        for (value in prefs.values.values) {
            val bytes = Base64.getDecoder().decode(value).decodeToString()
            for (secret in listOf("test-session", "tester", "test-token")) {
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

        assertEquals(SessionState.SignedOut, SessionStore(storage(prefs, newKey())).state.value)

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
        assertFalse("test-token" in PendingSignIn("test-token", 5).toString())
    }
}
