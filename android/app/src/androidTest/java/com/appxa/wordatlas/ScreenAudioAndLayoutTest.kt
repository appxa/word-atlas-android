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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ScreenAudioAndLayoutTest {
    private fun shell(cmd: String) { InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd).close() }
    private fun js(s: ActivityScenario<MainActivity>, code: String): String {
        val latch = CountDownLatch(1); var result = ""
        s.onActivity { a -> (a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as WebView).evaluateJavascript(code) { result = it ?: ""; latch.countDown() } }
        assertTrue("JavaScript timed out", latch.await(10, TimeUnit.SECONDS)); return result
    }
    private fun until(s: ActivityScenario<MainActivity>, code: String, message: String) {
        for (i in 0 until 80) { if (js(s, code) == "true") return; Thread.sleep(250) }
        throw AssertionError(message)
    }
    @Test fun iconFiltersAndScreenOffSilence() {
        shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard")
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertTrue("Application icon missing", ctx.applicationInfo.icon != 0)
        val s = ActivityScenario.launch(MainActivity::class.java)
        try {
            until(s, "!!document.body", "Page did not start")
            js(s, "localStorage.setItem('word-atlas:settings',JSON.stringify({onboarded:true,splash:false,theme:'light',ambience:'stars'})); location.hash='#/atlas'; location.reload(); true")
            until(s, "!!document.querySelector('.atlas-status button')", "Atlas filters not ready")
            assertTrue("Five status filters should fit in one row", js(s, "(function(){const e=document.querySelector('.atlas-status'), b=[...e.querySelectorAll('button')], r=e.getBoundingClientRect(); return b.length===5 && e.scrollWidth<=e.clientWidth && b.every(x=>x.scrollWidth<=x.clientWidth && Math.abs(x.getBoundingClientRect().top-b[0].getBoundingClientRect().top)<1 && x.getBoundingClientRect().left>=r.left && x.getBoundingClientRect().right<=r.right)})()") == "true")
            js(s, "(function(){const p=AudioContext.prototype, gain=p.createGain; p.createGain=function(){window.__soundContext=this; return gain.call(this)}; window.addEventListener('wordatlas:pause',()=>window.__appPauseObserved=true); window.addEventListener('wordatlas:resume',()=>window.__appResumeObserved=true); return true})()")
            // A real user gesture starts the app's ambient Web Audio context.
            shell("input tap 200 220")
            until(s, "window.__soundContext?.state==='running'", "Ambience did not start")
            shell("input keyevent KEYCODE_SLEEP")
            until(s, "window.__appPauseObserved===true && window.__soundContext?.state==='suspended'", "Screen-off did not suspend ambience")
            shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard")
            until(s, "window.__appResumeObserved===true && window.__soundContext?.state==='running'", "Ambience did not resume after screen-on")
        } finally { shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard"); s.close() }
    }
}
