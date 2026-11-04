package com.appxa.wordatlas

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LevelDownloadTest {
    private fun shell(cmd: String) { InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd).close() }
    private fun js(s: ActivityScenario<MainActivity>, code: String): String {
        val latch = CountDownLatch(1); var result = ""
        s.onActivity { a -> (a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as WebView).evaluateJavascript(code) { result = it ?: ""; latch.countDown() } }
        assertTrue("JavaScript timed out", latch.await(10, TimeUnit.SECONDS)); return result
    }
    private fun waitReady(s: ActivityScenario<MainActivity>) {
        for (i in 0 until 60) { if (js(s, "document.body && document.body.innerText.includes('پروفایل')") == "true") return; Thread.sleep(500) }
        throw AssertionError("App did not start")
    }
    @Test fun downloadC2ThenRestartWithoutInternet() {
        shell("svc wifi enable"); shell("svc data enable")
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ctx.getSharedPreferences("offline-levels-v1", Context.MODE_PRIVATE)
        prefs.edit().clear().commit(); File(ctx.filesDir, "offline-media").deleteRecursively()
        val s = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitReady(s); js(s, "NativeOffline.start('C2'); true")
            var complete = false
            for (i in 0 until 900) {
                val state = js(s, "JSON.parse(NativeOffline.status('C2')).state")
                if (state.contains("complete")) { complete = true; break }
                if (state.contains("error")) throw AssertionError("Real level download failed: " + js(s, "NativeOffline.status('C2')"))
                Thread.sleep(1000)
            }
            assertTrue("C2 download timed out", complete)
        } finally { s.close() }
        shell("svc wifi disable"); shell("svc data disable")
        val offline = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitReady(offline)
            assertTrue("level not retained", js(offline, "NativeOffline.isDownloaded('C2')") == "true")
            js(offline, "Promise.all(['img/avenge.webp','audio/uk/avenge.mp3'].map(p=>fetch('https://appassets.androidplatform.net/offline/'+p).then(r=>r.ok?r.arrayBuffer():Promise.reject()).then(b=>b.byteLength>0))).then(x=>window.__downloadedMediaOk=x.every(Boolean)).catch(()=>window.__downloadedMediaOk=false); true")
            var ok = false
            for (i in 0 until 60) { if (js(offline, "window.__downloadedMediaOk===true") == "true") { ok = true; break }; Thread.sleep(500) }
            assertTrue("Downloaded image/audio unavailable offline", ok)
        } finally { offline.close(); prefs.edit().clear().commit(); File(ctx.filesDir, "offline-media").deleteRecursively(); shell("svc wifi enable"); shell("svc data enable") }
    }
}
