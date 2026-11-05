package com.appxa.wordatlas

import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import java.security.MessageDigest

/** Verify size and SHA-256 while writing, without rereading every newly downloaded file. */
internal object MediaIntegrity {
    fun copyVerified(input: InputStream, output: OutputStream, size: Long, sha256: String, checkActive: () -> Unit) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32768)
        var count = 0L
        while (true) {
            checkActive()
            val n = input.read(buffer)
            if (n < 0) break
            if (n == 0) continue
            count += n
            if (count > size) throw IOException("فایل دانلودشده بیش از اندازه است")
            digest.update(buffer, 0, n)
            output.write(buffer, 0, n)
        }
        checkActive()
        val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        if (count != size || hash != sha256) throw IOException("فایل دانلودشده ناقص است")
    }
}
