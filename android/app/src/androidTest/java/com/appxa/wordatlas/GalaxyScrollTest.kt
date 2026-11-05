package com.appxa.wordatlas

import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GalaxyScrollTest {
    private fun js(s: ActivityScenario<MainActivity>, code: String): String {
        val latch = CountDownLatch(1); var result = ""
        s.onActivity { a -> (a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as WebView).evaluateJavascript(code) { result = it ?: ""; latch.countDown() } }
        assertTrue("JavaScript timed out", latch.await(10, TimeUnit.SECONDS)); return result
    }
    private fun waitFor(s: ActivityScenario<MainActivity>, condition: String) {
        for (i in 0 until 90) { if (js(s, condition) == "true") return; Thread.sleep(500) }
        throw AssertionError("Page not ready: $condition")
    }
    private fun swipeDownOnCanvas(s: ActivityScenario<MainActivity>, selector: String) {
        js(s, "window.scrollTo(0, window.scrollY + document.querySelector('$selector').getBoundingClientRect().top - 110); true")
        Thread.sleep(1200)
        val point = JSONObject(js(s, """(function(){const c=document.querySelector('$selector'),r=c.getBoundingClientRect(),x=innerWidth/2,y=Math.max(110,Math.min(r.top+r.height/2,innerHeight-180,r.bottom-150));return {x,y,dpr:devicePixelRatio,scroll:scrollY,isCanvas:document.elementFromPoint(x,y)===c}})()"""))
        assertTrue("Swipe did not start on $selector", point.getBoolean("isCanvas"))
        assertTrue("Not enough room to scroll", point.getDouble("scroll") > 120)
        val origin = IntArray(2)
        s.onActivity { a -> (a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as WebView).getLocationOnScreen(origin) }
        val dpr = point.getDouble("dpr")
        val x = origin[0] + (point.getDouble("x") * dpr).toInt()
        val y = origin[1] + (point.getDouble("y") * dpr).toInt()
        val endY = origin[1] + ((point.getDouble("y") + 110) * dpr).toInt()
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input swipe $x $y $x $endY 450").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        Thread.sleep(1000)
        val after = js(s, "window.scrollY").toDouble()
        assertTrue("Page did not scroll over $selector (before ${point.getDouble("scroll")}, after $after)", after < point.getDouble("scroll") - 20)
    }
    @Test fun topicPrecedesMiniGalaxyAndBothMapsPermitPageScrolling() {
        val s = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitFor(s, "document.body && document.body.innerText.includes('پروفایل')")
            js(s, "localStorage.setItem('word-atlas:settings', JSON.stringify({...JSON.parse(localStorage.getItem('word-atlas:settings')||'{}'),onboarded:true,splash:false,sfx:false})); location.hash='#/'; location.reload(); true")
            waitFor(s, "!!document.querySelector('.today-grid .galaxy-panel') && !document.querySelector('.splash') && !document.querySelector('.onboard')")
            assertTrue("Galaxy is not last after topics", js(s, "document.querySelector('.today-grid').lastElementChild.classList.contains('galaxy-panel') && !!(document.querySelector('.topics-panel').compareDocumentPosition(document.querySelector('.galaxy-panel')) & Node.DOCUMENT_POSITION_FOLLOWING)") == "true")
            swipeDownOnCanvas(s, ".galaxy-mini canvas")
            js(s, "location.hash='#/atlas?view=galaxy'; true")
            waitFor(s, "!!document.querySelector('.galaxy-full canvas')")
            Thread.sleep(1000)
            swipeDownOnCanvas(s, ".galaxy-full canvas")
            assertTrue("Map zoom buttons missing", js(s, "!!document.querySelector('.galaxy-controls [aria-label=\"بزرگ‌نمایی\"]') && !!document.querySelector('.galaxy-controls [aria-label=\"کوچک‌نمایی\"]')") == "true")
        } finally { s.close() }
    }
}
