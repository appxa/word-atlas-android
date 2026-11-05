package com.appxa.wordatlas

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.CancellationException

@RunWith(AndroidJUnit4::class)
class MediaIntegrityTest {
    private val bytes = "a verified media file".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    @Test fun validFileIsCopiedAndVerified() {
        val out = ByteArrayOutputStream()
        MediaIntegrity.copyVerified(ByteArrayInputStream(bytes), out, bytes.size.toLong(), hash) { }
        assertArrayEquals(bytes, out.toByteArray())
    }
    @Test fun wrongHashAndSizeAreRejected() {
        assertThrows(IOException::class.java) { MediaIntegrity.copyVerified(ByteArrayInputStream(bytes), ByteArrayOutputStream(), bytes.size.toLong(), "0".repeat(64)) { } }
        assertThrows(IOException::class.java) { MediaIntegrity.copyVerified(ByteArrayInputStream(bytes), ByteArrayOutputStream(), bytes.size.toLong() + 1, hash) { } }
        assertThrows(IOException::class.java) { MediaIntegrity.copyVerified(ByteArrayInputStream(bytes), ByteArrayOutputStream(), bytes.size.toLong() - 1, hash) { } }
    }
    @Test fun cancellationInterruptsCopy() {
        assertThrows(CancellationException::class.java) { MediaIntegrity.copyVerified(ByteArrayInputStream(bytes), ByteArrayOutputStream(), bytes.size.toLong(), hash) { throw CancellationException() } }
    }
}
