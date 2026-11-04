package com.appxa.wordatlas

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** App-private persistent media. A level is complete only when every file is verified. */
class OfflineManager private constructor(private val context: Context) {
    data class Item(val path: String, val size: Long, val hash: String)
    data class Pack(val total: Long, val items: List<Item>)
    data class State(val total: Long, @Volatile var downloaded: Long, @Volatile var status: String, @Volatile var error: String = "")
    private val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    private val prefs = context.getSharedPreferences("offline-levels-v1", Context.MODE_PRIVATE)
    private val runner = Executors.newSingleThreadExecutor()
    val root = File(context.filesDir, "offline-media")
    private val packs: Map<String, Pack> = levels.associateWith { name ->
        val json = JSONObject(context.assets.open("offline/$name.json").bufferedReader().use { it.readText() })
        val arr = json.getJSONArray("files")
        val items = (0 until arr.length()).map { i -> val a = arr.getJSONArray(i); Item(a.getString(0), a.getLong(1), a.getString(2)) }
        Pack(json.getLong("total"), items)
    }
    private val states: Map<String, State> = packs.mapValues { (level, p) ->
        val complete = prefs.getBoolean("$level-complete", false)
        State(p.total, if (complete) p.total else prefs.getLong("$level-bytes", 0).coerceIn(0, p.total), if (complete) "complete" else "idle")
    }
    @Volatile private var active = ""
    @Synchronized fun isDownloaded(level: String): Boolean = states[level]?.status == "complete"
    @Synchronized fun status(level: String): String {
        val s = states[level] ?: return "{}"
        return JSONObject().put("level", level).put("total", s.total).put("downloaded", s.downloaded).put("state", s.status).put("error", s.error).toString()
    }
    @Synchronized fun start(level: String) {
        val state = states[level] ?: return
        if (active.isNotEmpty() || state.status == "complete") return
        active = level; state.status = "checking"; state.error = ""
        runner.execute { download(level) }
    }
    private fun fileFor(path: String): File {
        val f = File(root, path).canonicalFile
        require(f.path.startsWith(root.canonicalPath + File.separator))
        return f
    }
    private fun valid(f: File, item: Item): Boolean {
        if (!f.isFile || f.length() != item.size) return false
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().buffered().use { input -> val b = ByteArray(32768); while (true) { val n = input.read(b); if (n < 0) break; digest.update(b, 0, n) } }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } == item.hash
    }
    private fun download(level: String) {
        val p = packs.getValue(level); val state = states.getValue(level)
        try {
            root.mkdirs(); val missing = ArrayList<Item>(); var have = 0L
            for (item in p.items) { val f = fileFor(item.path); if (valid(f, item)) have += item.size else { f.delete(); missing.add(item) } }
            state.downloaded = have
            if (root.usableSpace < p.total - have + 16L * 1024 * 1024) throw IllegalStateException("فضای کافی برای دانلود وجود ندارد.")
            state.status = "downloading"
            val downloaded = AtomicLong(have); val errors = java.util.concurrent.atomic.AtomicReference<String?>(null)
            val pool = Executors.newFixedThreadPool(4)
            try {
                val tasks = missing.map { item -> pool.submit {
                    if (errors.get() != null) return@submit
                    try {
                        fetch(item)
                        val n = downloaded.addAndGet(item.size); state.downloaded = n
                        prefs.edit().putLong("$level-bytes", n).apply()
                    } catch (e: Exception) { errors.compareAndSet(null, e.message ?: "دانلود ناموفق بود.") }
                } }
                tasks.forEach { it.get() }
            } finally { pool.shutdown() }
            errors.get()?.let { throw IllegalStateException(it) }
            // All files have been hashed before marking the level complete.
            state.downloaded = p.total; state.status = "complete"
            prefs.edit().putLong("$level-bytes", p.total).putBoolean("$level-complete", true).apply()
        } catch (e: Exception) {
            state.status = "error"; state.error = if (e.message?.contains("فضای") == true) e.message!! else "دانلود متوقف شد؛ اتصال اینترنت را بررسی و دوباره تلاش کن."
            prefs.edit().putBoolean("$level-complete", false).putLong("$level-bytes", state.downloaded).apply()
        } finally { active = "" }
    }
    private fun fetch(item: Item) {
        val dest = fileFor(item.path); dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        var failure: Exception? = null
        repeat(3) { attempt ->
            try {
                val conn = URL("https://appxa.github.io/word-atlas/atlas/${item.path}").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 20000; conn.readTimeout = 30000; conn.instanceFollowRedirects = true
                try {
                    if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
                    conn.inputStream.use { input -> tmp.outputStream().buffered().use { output -> input.copyTo(output) } }
                } finally { conn.disconnect() }
                if (!valid(tmp, item)) throw IllegalStateException("فایل دانلودشده ناقص است")
                if (!tmp.renameTo(dest)) throw IllegalStateException("ذخیره‌سازی ناموفق بود")
                return
            } catch (e: Exception) { failure = e; tmp.delete(); if (attempt < 2) Thread.sleep(400L * (attempt + 1)) }
        }
        throw failure ?: IllegalStateException("دانلود ناموفق بود")
    }
    @Synchronized fun remove(level: String) {
        val p = packs[level] ?: return
        if (active.isNotEmpty()) return
        val s = states.getValue(level); active = level; s.status = "checking"
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
