package com.appxa.wordatlas

import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BackNavigationTest {
    private fun js(s: ActivityScenario<MainActivity>, code: String): String {
        val latch = CountDownLatch(1); var result = ""
        s.onActivity { a -> (a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as WebView).evaluateJavascript(code) { result = it ?: ""; latch.countDown() } }
        assertTrue("JavaScript timed out", latch.await(10, TimeUnit.SECONDS)); return result
    }
    private fun waitFor(s: ActivityScenario<MainActivity>, expression: String) {
        for (i in 0 until 90) { if (js(s, expression) == "true") return; Thread.sleep(500) }
        throw AssertionError("Page not ready: $expression")
    }
    private fun back() { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK) }
    @Test fun backClosesContentReturnsHomeAndExitsOnDoublePress() {
        val s = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitFor(s, "document.body && document.body.innerText.includes('پروفایل')")
            js(s, "localStorage.setItem('word-atlas:settings', JSON.stringify({...JSON.parse(localStorage.getItem('word-atlas:settings')||'{}'),onboarded:true,splash:false,sfx:false})); location.hash='#/practice'; location.reload(); true")
            waitFor(s, "!!document.querySelector('.practice-grid') && !document.querySelector('.onboard') && !document.querySelector('.splash')")
            js(s, "document.querySelector('.practice-grid .btn-primary').click(); true")
            waitFor(s, "!!document.querySelector('.typing')")
            // Calling Activity's back handler avoids a virtual keyboard consuming the first key.
            s.onActivity { it.onBackPressed() }
            waitFor(s, "!!document.querySelector('.practice-grid')")
            js(s, "location.hash='#/me?w=library'; true")
            waitFor(s, "!!document.querySelector('.entry-detail .headword') && !!document.querySelector('.page.me')")
            back()
            waitFor(s, "!document.querySelector('.sheet-root') && !!document.querySelector('.page.me')")
            back()
            waitFor(s, "!!document.querySelector('.page.today')")
            back(); Thread.sleep(350)
            assertTrue("First back on Today should not close the app", s.state != Lifecycle.State.DESTROYED)
            back()
            for (i in 0 until 30) { if (s.state == Lifecycle.State.DESTROYED) break; Thread.sleep(200) }
            assertTrue("Second back on Today should close the app", s.state == Lifecycle.State.DESTROYED)
        } finally { s.close() }
    }
}
