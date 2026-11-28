package com.ammar.network

import com.ammar.network.session.KeySource
import com.ammar.network.session.PendingSignIn
import com.ammar.network.session.SessionState
import com.ammar.network.session.SessionStore
import com.ammar.network.session.StoredSession
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.Executors
import javax.crypto.SecretKey

class SessionStoreTest {

    private val prefs = MemoryStore()
    private val key = newKey()

    private fun open(now: Long = 0, origin: String = ORIGIN) = sessionStore(
        storage(prefs, key), Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC), origin
    )

    private fun SessionStore.save() = save("test-session", TESTER)

    private val signedIn = SessionState.SignedIn(TESTER)

    @Test
    fun `a saved session survives a restart`() {
        open().save()

        val reopened = open()

        assertEquals(signedIn, reopened.state.value)
        assertEquals("test-session", reopened.sessionId())
    }

    @Test
    fun `nothing is written in the clear`() {
        val store = open()
        store.save()
        store.savePending("test-token", "test-secret", 5)

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
        store.save()
        val first = prefs.values.getValue("session")
        store.save()

        assertNotEquals(first, prefs.values.getValue("session"))
    }

    @Test
    fun `a value that was tampered with or written with another key means signed out`() {
        fun saved(): String {
            open().save()
            return prefs.values.getValue("session")
        }

        saved()
        assertEquals(SessionState.SignedOut, sessionStore(storage(prefs, newKey())).state.value)

        val bytes = Base64.getDecoder().decode(saved())
        bytes[bytes.lastIndex] = (bytes.last() + 1).toByte()
        prefs.values["session"] = Base64.getEncoder().encodeToString(bytes)
        assertEquals(SessionState.SignedOut, open().state.value)

        for (garbage in listOf("not base64 !", "", "AAAA")) {
            prefs.values["session"] = garbage
            assertNull(garbage, open().sessionId())
            assertFalse(garbage, "session" in prefs.values)
        }
    }

    @Test
    fun `a session and a sign-in for another backend are discarded`() {
        for (other in listOf("http://backend.test:80", "https://backend.test:8443", "https://other.test:443")) {
            open().save()
            open().savePending("test-token", "test-secret", 5)

            val moved = open(origin = other)

            assertEquals(other, SessionState.SignedOut, moved.state.value)
            assertNull(moved.sessionId())
            assertNull(moved.pending())
            assertEquals(other, emptySet<String>(), prefs.values.keys)
        }
    }

    @Test
    fun `an entry cannot be moved to another name`() {
        open().save()
        prefs.values["pending_sign_in"] = prefs.values.getValue("session")
        val sealed = storage(prefs, key)

        assertNull(sealed.read("pending_sign_in"))
        assertTrue(sealed.read("session")!!.contains("test-session"))
    }

    @Test
    fun `storage and keys are only touched off the calling thread`() = runBlocking {
        val watched = ThreadRecordingStore(prefs)
        val keys = FakeKeys(key)
        val watchedKeys = object : KeySource by keys {
            override fun get(): SecretKey {
                assertEquals("session-io", Thread.currentThread().name.substringBefore(" @"))
                return keys.get()
            }
        }
        open().save()
        val executor = Executors.newSingleThreadExecutor { Thread(it, "session-io") }
        try {
            val store = sessionStore(storage(watched, watchedKeys), io = executor.asCoroutineDispatcher())
            store.onIo { savePending("test-token", "test-secret", Long.MAX_VALUE) }
            assertEquals(signedIn, store.state.value)

            assertEquals(setOf("session-io"), watched.threads)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `clearing removes the stored session`() {
        val store = open()
        store.save()

        store.clearSessionIfCurrent("another-session")
        assertEquals(signedIn, store.state.value)
        store.clearSessionIfCurrent("test-session")

        assertEquals(SessionState.SignedOut, store.state.value)
        assertEquals(emptyMap<String, String>(), prefs.values)
    }

    @Test
    fun `printing the stored objects does not reveal them`() {
        assertFalse("test-session" in StoredSession("test-session", "7", "tester", ORIGIN).toString())
        val pending = PendingSignIn("test-token", "test-secret", 5, ORIGIN).toString()
        assertFalse("test-token" in pending || "test-secret" in pending)
    }

    @Test
    fun `an expired pending sign-in is removed when the app starts again`() {
        open().savePending("test-token", "test-secret", 1000)

        assertEquals(setOf("pending_sign_in"), open(now = 999).let { prefs.values.keys.toSet() })
        val later = open(now = 1000)

        assertEquals(emptySet<String>(), prefs.values.keys)
        assertFalse(later.takeLapsed("another-token"))
        assertTrue(later.takeLapsed("test-token"))
        assertFalse(later.takeLapsed("test-token"))
    }
}
