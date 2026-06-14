package dev.encgallery.gallery

import android.os.Handler
import android.os.HandlerThread
import dev.encgallery.logging.EncLog
import dev.encgallery.nativec.NativeCrypto
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ShareMemoryStore {

    private const val TAG = "ShareMemoryStore"
    private const val STALE_AGE_MS = 10 * 60 * 1000L

    class Item(
        val name: String,
        val mime: String,
        @Volatile var bytes: ByteArray?,
        val createdAt: Long,
    )

    private val items = ConcurrentHashMap<String, Item>()

    private val handlerThread by lazy { HandlerThread("share-proxy-fd").apply { start() } }
    val handler: Handler by lazy { Handler(handlerThread.looper) }

    fun register(name: String, mime: String, bytes: ByteArray): String {
        sweepStale()
        val token = UUID.randomUUID().toString()
        items[token] = Item(name, mime, bytes, System.currentTimeMillis())
        return token
    }

    fun get(token: String): Item? = items[token]

    fun clear() {
        if (items.isEmpty()) return
        val n = items.size
        items.values.forEach { wipe(it) }
        items.clear()
        EncLog.d(TAG, "cleared $n in-RAM share buffer(s)")
    }

    private fun sweepStale() {
        val cutoff = System.currentTimeMillis() - STALE_AGE_MS
        val stale = items.entries.filter { it.value.createdAt < cutoff }
        stale.forEach { (token, item) ->
            wipe(item)
            items.remove(token)
        }
        if (stale.isNotEmpty()) EncLog.d(TAG, "swept ${stale.size} stale share buffer(s)")
    }

    private fun wipe(item: Item) {
        item.bytes?.let { NativeCrypto.secureZero(it) }
        item.bytes = null
    }
}
