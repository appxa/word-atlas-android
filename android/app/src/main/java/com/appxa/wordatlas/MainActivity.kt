package com.appxa.wordatlas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var offline: OfflineManager
    private var chooser: ValueCallback<Array<Uri>>? = null
    private var backup: String? = null
    private lateinit var assets: WebViewAssetLoader
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assets = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        offline = OfflineManager.get(applicationContext)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.addJavascriptInterface(Bridge(), "NativeOffline")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val u = request.url
                if (u.host == "appassets.androidplatform.net" && u.path?.startsWith("/offline/") == true) return offlineResponse(request)
                return assets.shouldInterceptRequest(u)
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.scheme == "https" && u.host == "appassets.androidplatform.net") return false
                if (request.isForMainFrame && u.scheme in listOf("https", "http", "mailto")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (_: Exception) { }
                }
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                chooser?.onReceiveValue(null); chooser = cb
                return try { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE), 101); true }
                catch (_: Exception) { chooser = null; cb.onReceiveValue(null); false }
            }
        }
        setContentView(web)
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }
    private inner class Bridge {
        @JavascriptInterface fun status(level: String): String = offline.status(level)
        @JavascriptInterface fun isDownloaded(level: String): Boolean = offline.isDownloaded(level)
        @JavascriptInterface fun hasMedia(path: String): Boolean = offline.hasMedia(path)
        @JavascriptInterface fun start(level: String) = offline.start(level)
        @JavascriptInterface fun cancel(level: String) = offline.cancel(level)
        @JavascriptInterface fun remove(level: String) = offline.remove(level)
        @JavascriptInterface fun saveBackup(data: String, name: String) {
            runOnUiThread {
                backup = data
                try { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE, name), 102) }
                catch (_: Exception) { backup = null; Toast.makeText(this@MainActivity, "ذخیرهٔ پشتیبان ممکن نیست", Toast.LENGTH_LONG).show() }
            }
        }
    }
    private fun offlineResponse(req: WebResourceRequest): WebResourceResponse {
        val rel = req.url.path.orEmpty().removePrefix("/offline/")
        val f = File(offline.root, rel).canonicalFile
        if (!f.path.startsWith(offline.root.canonicalPath + File.separator) || !f.isFile) return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", mapOf("Cache-Control" to "no-store"), null)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "application/octet-stream"
        val length = f.length(); val range = req.requestHeaders.entries.firstOrNull { it.key.equals("Range", true) }?.value
        val match = range?.let { Regex("bytes=(\\d+)-(\\d*)").matchEntire(it) }
        if (match != null) {
            val start = match.groupValues[1].toLongOrNull() ?: 0L
            val end = match.groupValues[2].toLongOrNull()?.coerceAtMost(length - 1) ?: (length - 1)
            if (start >= length || end < start) return WebResourceResponse(mime, null, 416, "Range Not Satisfiable", mapOf("Content-Range" to "bytes */$length"), null)
            val stream = FileInputStream(f); stream.channel.position(start); val count = end - start + 1
            return WebResourceResponse(mime, null, 206, "Partial Content", mapOf("Accept-Ranges" to "bytes", "Content-Range" to "bytes $start-$end/$length", "Content-Length" to count.toString(), "Cache-Control" to "no-store"), LimitedStream(stream, count))
        }
        return WebResourceResponse(mime, null, 200, "OK", mapOf("Accept-Ranges" to "bytes", "Content-Length" to length.toString(), "Cache-Control" to "no-store"), FileInputStream(f))
    }
    private class LimitedStream(input: InputStream, private var left: Long) : FilterInputStream(input) {
        override fun read(): Int { if (left <= 0) return -1; val n = super.read(); if (n >= 0) left--; return n }
        override fun read(b: ByteArray, off: Int, len: Int): Int { if (left <= 0) return -1; val n = `in`.read(b, off, minOf(len.toLong(), left).toInt()); if (n > 0) left -= n; return n }
    }
    @Deprecated("Use activity result APIs")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) { chooser?.onReceiveValue(if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null); chooser = null }
        if (requestCode == 102) {
            val payload = backup; backup = null
            if (resultCode == RESULT_OK && data?.data != null && payload != null) try {
                contentResolver.openOutputStream(data.data!!)?.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(this, "نسخهٔ پشتیبان ذخیره شد", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this, "ذخیرهٔ پشتیبان ناموفق بود", Toast.LENGTH_LONG).show() }
        }
    }
    private var lastExitRequestAt = 0L
    @Deprecated("Use OnBackInvokedCallback")
    override fun onBackPressed() {
        web.evaluateJavascript("typeof window.androidBack === 'function' && window.androidBack() === true") { handled ->
            if (handled == "true") { lastExitRequestAt = 0L; return@evaluateJavascript }
            val now = SystemClock.elapsedRealtime()
            if (lastExitRequestAt != 0L && now - lastExitRequestAt < 2000) {
                offline.cancelActive()
                finishAndRemoveTask()
            } else {
                lastExitRequestAt = now
                Toast.makeText(this, "برای خروج دوباره دکمهٔ بازگشت را بزن", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroy() {
        (web.parent as? ViewGroup)?.removeView(web)
        web.stopLoading(); web.destroy()
        super.onDestroy()
    }
}
