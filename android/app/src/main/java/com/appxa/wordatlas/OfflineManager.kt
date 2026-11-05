package com.appxa.wordatlas

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CancellationException

/** App-private persistent media. A level is complete only when every file is verified. */
class OfflineManager private constructor(private val context: Context) {
    data class Item(val path: String, val size: Long, val hash: String, val origin: String)
    data class Pack(val total: Long, val items: List<Item>, val fingerprint: String)
    data class State(val total: Long, @Volatile var downloaded: Long, @Volatile var status: String, @Volatile var error: String = "")
    private val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    private val prefs = context.getSharedPreferences("offline-levels-v1", Context.MODE_PRIVATE)
    private val runner = Executors.newSingleThreadExecutor()
    val root = File(context.filesDir, "offline-media")
    private val packs: Map<String, Pack> = levels.associateWith { name ->
        val manifest = context.assets.open("offline/$name.json").bufferedReader().use { it.readText() }
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(manifest.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val json = JSONObject(manifest)
        val arr = json.getJSONArray("files")
        val items = (0 until arr.length()).map { i -> val a = arr.getJSONArray(i); Item(a.getString(0), a.getLong(1), a.getString(2), a.optString(3, "primary")) }
        Pack(json.getLong("total"), items, fingerprint)
    }
    private val states: Map<String, State> = packs.mapValues { (level, p) ->
        val complete = prefs.getBoolean("$level-complete", false) && prefs.getString("$level-manifest", "") == p.fingerprint
        State(p.total, if (complete) p.total else prefs.getLong("$level-bytes", 0).coerceIn(0, p.total), if (complete) "complete" else "idle")
    }
    private class DownloadJob {
        val cancelled = AtomicBoolean(false)
        val connections = ConcurrentHashMap.newKeySet<HttpURLConnection>()
    }
    @Volatile private var active = ""
    @Volatile private var activeJob: DownloadJob? = null
    private fun ensureActive(job: DownloadJob) {
        if (job.cancelled.get()) throw CancellationException("Cancelled")
    }
    @Synchronized fun isDownloaded(level: String): Boolean = states[level]?.status == "complete"
    fun hasMedia(path: String): Boolean = try { fileFor(path).isFile } catch (_: Exception) { false }
    @Synchronized fun status(level: String): String {
        val s = states[level] ?: return "{}"
        return JSONObject().put("level", level).put("total", s.total).put("downloaded", s.downloaded).put("state", s.status).put("error", s.error).toString()
    }
    @Synchronized fun start(level: String) {
        val state = states[level] ?: return
        if (active.isNotEmpty() || state.status == "complete") return
        val job = DownloadJob()
        active = level; activeJob = job; state.status = "checking"; state.error = ""
        runner.execute { download(level, job) }
    }
    @Synchronized fun cancel(level: String) {
        val job = activeJob ?: return
        if (active != level || states[level]?.status !in listOf("checking", "downloading") || job.cancelled.getAndSet(true)) return
        states[level]?.status = "cancelling"
        job.connections.toList().forEach { conn -> Thread { try { conn.disconnect() } catch (_: Exception) { } }.start() }
    }
    @Synchronized fun cancelActive() { if (activeJob != null) cancel(active) }
    private fun fileFor(path: String): File {
        val f = File(root, path).canonicalFile
        require(f.path.startsWith(root.canonicalPath + File.separator))
        return f
    }
    private fun valid(f: File, item: Item, job: DownloadJob): Boolean {
        if (!f.isFile || f.length() != item.size) return false
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().buffered().use { input -> val b = ByteArray(32768); while (true) { ensureActive(job); val n = input.read(b); if (n < 0) break; digest.update(b, 0, n) } }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } == item.hash
    }
    private fun download(level: String, job: DownloadJob) {
        val p = packs.getValue(level); val state = states.getValue(level)
        try {
            root.mkdirs(); val missing = ArrayList<Item>(); var have = 0L
            for (item in p.items) {
                ensureActive(job)
                val f = fileFor(item.path)
                if (valid(f, item, job)) have += item.size else { f.delete(); missing.add(item) }
            }
            state.downloaded = have
            if (root.usableSpace < p.total - have + 16L * 1024 * 1024) throw IllegalStateException("فضای کافی برای دانلود وجود ندارد.")
            synchronized(this) { ensureActive(job); state.status = "downloading" }
            val downloaded = AtomicLong(have); val errors = java.util.concurrent.atomic.AtomicReference<String?>(null)
            val nextItem = AtomicInteger(0)
            // Eight total workers, with per-host caps to avoid overwhelming either source.
            val primarySlots = Semaphore(6); val upstreamSlots = Semaphore(4)
            var lastSaved = 0L
            val pool = Executors.newFixedThreadPool(8)
            try {
                val workers = (0 until 8).map { pool.submit {
                    while (!job.cancelled.get() && errors.get() == null) {
                        val index = nextItem.getAndIncrement()
                        if (index >= missing.size) break
                        val item = missing[index]
                        val slots = when (item.origin) { "primary" -> primarySlots; "upstream" -> upstreamSlots; else -> null }
                        if (slots == null) { errors.compareAndSet(null, "منبع فایل نامعتبر است"); break }
                        var acquired = false
                        try {
                            while (!acquired && !job.cancelled.get() && errors.get() == null) acquired = slots.tryAcquire(200, TimeUnit.MILLISECONDS)
                            if (!acquired || job.cancelled.get() || errors.get() != null) break
                            fetch(item, job)
                            synchronized(this@OfflineManager) {
                                val n = downloaded.addAndGet(item.size); state.downloaded = n
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastSaved >= 1000) {
                                    prefs.edit().putLong("$level-bytes", n).apply(); lastSaved = now
                                }
                            }
                        } catch (e: Exception) {
                            if (!job.cancelled.get()) errors.compareAndSet(null, e.message ?: "دانلود ناموفق بود.")
                        } finally { if (acquired) slots.release() }
                    }
                } }
                workers.forEach { it.get() }
            } finally { pool.shutdown() }
            ensureActive(job)
            errors.get()?.let { throw IllegalStateException(it) }
            synchronized(this) {
                ensureActive(job)
                state.downloaded = p.total; state.status = "complete"
                prefs.edit().putLong("$level-bytes", p.total).putString("$level-manifest", p.fingerprint).putBoolean("$level-complete", true).apply()
            }
        } catch (e: Exception) {
            synchronized(this) {
                if (job.cancelled.get()) { state.status = "idle"; state.error = "" }
                else {
                    state.status = "error"
                    state.error = if (e.message?.contains("فضای") == true) e.message!! else "دانلود متوقف شد؛ اتصال اینترنت را بررسی و دوباره تلاش کن."
                }
                prefs.edit().putBoolean("$level-complete", false).putLong("$level-bytes", state.downloaded).apply()
            }
        } finally {
            synchronized(this) { activeJob = null; active = "" }
        }
    }
    private fun fetch(item: Item, job: DownloadJob) {
        val dest = fileFor(item.path); dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        var failure: Exception? = null
        repeat(3) { attempt ->
            ensureActive(job)
            try {
                val base = when (item.origin) {
                    "primary" -> "https://appxa.github.io/word-atlas/atlas/"
                    "upstream" -> "https://appxa.github.io/word-atlas-audio/"
                    else -> throw IllegalStateException("منبع فایل نامعتبر است")
                }
                val conn = URL(base + item.path).openConnection() as HttpURLConnection
                job.connections.add(conn)
                conn.connectTimeout = 20000; conn.readTimeout = 30000; conn.instanceFollowRedirects = true
                try {
                    ensureActive(job)
                    if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
                    conn.inputStream.use { input -> tmp.outputStream().buffered().use { output ->
                        MediaIntegrity.copyVerified(input, output, item.size, item.hash) { ensureActive(job) }
                    } }
                } finally { job.connections.remove(conn); conn.disconnect() }
                ensureActive(job)
                if (tmp.length() != item.size) throw IllegalStateException("فایل دانلودشده ناقص است")
                ensureActive(job)
                if (!tmp.renameTo(dest)) throw IllegalStateException("ذخیره‌سازی ناموفق بود")
                return
            } catch (e: Exception) {
                failure = e; tmp.delete()
                ensureActive(job)
                if (attempt < 2) Thread.sleep(400L * (attempt + 1))
            }
        }
        throw failure ?: IllegalStateException("دانلود ناموفق بود")
    }
    @Synchronized fun remove(level: String) {
        val p = packs[level] ?: return
        if (active.isNotEmpty()) return
        val s = states.getValue(level); active = level; s.status = "deleting"
        prefs.edit().putBoolean("$level-complete", false).apply()
        runner.execute {
            try { p.items.forEach { fileFor(it.path).delete() }; s.downloaded = 0; s.status = "idle"; s.error = ""; prefs.edit().putLong("$level-bytes", 0).apply() }
            finally { active = "" }
        }
    }
    companion object {
        @Volatile private var instance: OfflineManager? = null
        fun get(context: Context): OfflineManager = instance ?: synchronized(this) { instance ?: OfflineManager(context.applicationContext).also { instance = it } }
    }
}
