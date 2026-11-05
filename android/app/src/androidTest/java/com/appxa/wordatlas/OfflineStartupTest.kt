package com.appxa.wordatlas

import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OfflineStartupTest {
    private fun shell(cmd: String) { InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd).close() }
    private fun js(scenario: ActivityScenario<MainActivity>, code: String): String {
        val latch = CountDownLatch(1); var result = ""
        scenario.onActivity { activity ->
            val web = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as WebView
            web.evaluateJavascript(code) { value -> result = value ?: ""; latch.countDown() }
        }
        assertTrue("JavaScript timed out", latch.await(10, TimeUnit.SECONDS)); return result
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, expression: String) {
        for (i in 0 until 50) { if (js(scenario, expression) == "true") return; Thread.sleep(500) }
        throw AssertionError("Offline app did not become ready: $expression")
    }
    @Test fun launchesAndLoadsLocalDataAfterRestartWithNetworkDisabled() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ctx.getSharedPreferences("offline-levels-v1", Context.MODE_PRIVATE)
        val localFile = File(ctx.filesDir, "offline-media/img/library.webp")
        localFile.parentFile!!.mkdirs(); localFile.writeText("offline-test")
        val manifest = ctx.assets.open("offline/A1.json").bufferedReader().use { it.readText() }
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(manifest.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        prefs.edit().putString("A1-manifest", hash).putBoolean("A1-complete", true).commit()
        shell("svc wifi disable"); shell("svc data disable")
        try {
            repeat(2) {
                val scenario = ActivityScenario.launch(MainActivity::class.java)
                try {
                    waitFor(scenario, "document.body && document.body.innerText.includes('پروفایل')")
                    assertEquals("true", js(scenario, "NativeOffline.isDownloaded('A1') && !NativeOffline.isDownloaded('B1')"))
                    js(scenario, "fetch('./atlas/entries/0.json').then(r=>r.json()).then(x=>window.__entryOk=Array.isArray(x)&&x.length>0).catch(()=>window.__entryOk=false); fetch('https://appassets.androidplatform.net/offline/img/library.webp').then(r=>r.text()).then(x=>window.__mediaOk=x==='offline-test').catch(()=>window.__mediaOk=false); true")
                    waitFor(scenario, "window.__entryOk === true && window.__mediaOk === true")
                } finally { scenario.close() }
            }
        } finally { prefs.edit().clear().commit(); localFile.delete(); shell("svc wifi enable"); shell("svc data enable") }
    }
}
