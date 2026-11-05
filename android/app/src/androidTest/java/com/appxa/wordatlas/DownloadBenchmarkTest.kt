package com.appxa.wordatlas

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DownloadBenchmarkTest {
    @Test fun measureFreshC2Download() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ctx.getSharedPreferences("offline-levels-v1", Context.MODE_PRIVATE)
        prefs.edit().clear().commit(); File(ctx.filesDir, "offline-media").deleteRecursively()
        val manager = OfflineManager.get(ctx)
        val start = SystemClock.elapsedRealtime()
        try {
            manager.start("C2")
            var complete = false
            for (i in 0 until 1200) {
                val status = JSONObject(manager.status("C2"))
                if (status.getString("state") == "complete") { complete = true; break }
                if (status.getString("state") == "error") throw AssertionError("C2 download failed: $status")
                Thread.sleep(500)
            }
            assertTrue("C2 download timed out", complete)
            val status = JSONObject(manager.status("C2"))
            Log.i("OfflineBenchmark", "C2 download ms=${SystemClock.elapsedRealtime() - start} bytes=${status.getLong("total")}")
        } finally { prefs.edit().clear().commit(); File(ctx.filesDir, "offline-media").deleteRecursively() }
    }
}
