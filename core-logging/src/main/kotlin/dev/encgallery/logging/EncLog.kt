package dev.encgallery.logging

import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EncLog {

    private const val MAX_FILE_SIZE = 1L * 1024 * 1024
    private const val MAX_FILES = 5
    private const val LOG_FILENAME = "encgallery.log"
    private const val LOGS_DIR = "logs"

    @Volatile
    var enabled: Boolean = false

    @Volatile
    private var logsDir: File? = null

    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }

    private val writeLock = Any()

    fun init(filesDir: File, enabled: Boolean) {
        this.logsDir = File(filesDir, LOGS_DIR).apply { mkdirs() }
        this.enabled = enabled
    }

    fun e(tag: String, msg: String, t: Throwable? = null) = write("E", tag, msg, t)
    fun w(tag: String, msg: String, t: Throwable? = null) = write("W", tag, msg, t)
    fun i(tag: String, msg: String) = write("I", tag, msg, null)
    fun d(tag: String, msg: String) = write("D", tag, msg, null)

    private fun write(level: String, tag: String, msg: String, t: Throwable?) {
        if (!enabled) return
        val dir = logsDir ?: return

        synchronized(writeLock) {
            try {
                val current = File(dir, LOG_FILENAME)
                if (current.exists() && current.length() > MAX_FILE_SIZE) {
                    rotate(dir)
                }
                FileWriter(current,   true).use { w ->
                    val df = dateFormat.get()!!
                    w.append(df.format(Date()))
                    w.append(' ').append(level)
                    w.append(" [").append(tag).append("] ")
                    w.append(msg)
                    if (t != null) {
                        w.append('\n').append(t::class.java.simpleName).append(": ")
                            .append(t.message ?: "")
                        for (frame in t.stackTrace.take(20)) {
                            w.append("\n  at ").append(frame.toString())
                        }
                    }
                    w.append('\n')
                }
            } catch (e: Throwable) {

            }
        }
    }

    private fun rotate(dir: File) {
        val oldest = File(dir, "$LOG_FILENAME.$MAX_FILES")
        if (oldest.exists()) oldest.delete()
        for (i in (MAX_FILES - 1) downTo 1) {
            val src = File(dir, "$LOG_FILENAME.$i")
            val dst = File(dir, "$LOG_FILENAME.${i + 1}")
            if (src.exists()) src.renameTo(dst)
        }
        val current = File(dir, LOG_FILENAME)
        if (current.exists()) current.renameTo(File(dir, "$LOG_FILENAME.1"))
    }

    fun tail(n: Int = 20): List<String> {
        val dir = logsDir ?: return emptyList()
        val current = File(dir, LOG_FILENAME)
        if (!current.exists()) return emptyList()
        return try {
            current.readLines().takeLast(n)
        } catch (e: Throwable) {
            emptyList()
        }
    }

    fun currentSize(): Long {
        val dir = logsDir ?: return 0L
        val current = File(dir, LOG_FILENAME)
        return if (current.exists()) current.length() else 0L
    }

    fun readAll(): String {
        val dir = logsDir ?: return ""
        val current = File(dir, LOG_FILENAME)
        if (!current.exists()) return ""
        return try {
            current.readText()
        } catch (e: Throwable) {
            ""
        }
    }

    fun activeLogFile(): File? {
        val dir = logsDir ?: return null
        val current = File(dir, LOG_FILENAME)
        return if (current.exists()) current else null
    }

    fun clear() {
        val dir = logsDir ?: return
        synchronized(writeLock) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }
}
