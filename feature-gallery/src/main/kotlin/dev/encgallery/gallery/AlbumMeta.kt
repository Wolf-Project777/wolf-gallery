package dev.encgallery.gallery

import org.json.JSONArray
import org.json.JSONObject

data class AlbumMeta(
    val uuid: String,
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val coverEntryUuid: String?,
    val coverCropRect: NormalizedRect?,

    val pinned: Boolean = false
) {

    fun toJsonBytes(): ByteArray = JSONObject().apply {
        put("version", JSON_VERSION)
        put("uuid", uuid)
        put("name", name)
        put("createdAt", createdAt)
        put("modifiedAt", modifiedAt)
        if (coverEntryUuid != null) put("coverEntryUuid", coverEntryUuid)
        if (coverCropRect != null) {
            put("coverCropRect", JSONArray().apply {
                put(coverCropRect.x.toDouble())
                put(coverCropRect.y.toDouble())
                put(coverCropRect.w.toDouble())
                put(coverCropRect.h.toDouble())
            })
        }

        if (pinned) put("pinned", true)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {

        const val JSON_VERSION = 1

        fun fromJsonBytes(bytes: ByteArray, expectedUuid: String): AlbumMeta? {
            return try {
                val root = JSONObject(String(bytes, Charsets.UTF_8))
                val version = root.optInt("version", 0)
                if (version != JSON_VERSION) return null
                val storedUuid = root.getString("uuid")
                if (storedUuid != expectedUuid) return null
                AlbumMeta(
                    uuid = storedUuid,
                    name = root.getString("name"),
                    createdAt = root.getLong("createdAt"),
                    modifiedAt = root.getLong("modifiedAt"),
                    coverEntryUuid = if (root.has("coverEntryUuid") && !root.isNull("coverEntryUuid"))
                        root.getString("coverEntryUuid")
                    else null,
                    coverCropRect = if (root.has("coverCropRect") && !root.isNull("coverCropRect")) {
                        val r = root.getJSONArray("coverCropRect")
                        NormalizedRect(
                            x = r.getDouble(0).toFloat(),
                            y = r.getDouble(1).toFloat(),
                            w = r.getDouble(2).toFloat(),
                            h = r.getDouble(3).toFloat()
                        )
                    } else null,
                    pinned = root.optBoolean("pinned", false)
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}

data class NormalizedRect(val x: Float, val y: Float, val w: Float, val h: Float)

data class AlbumSummary(
    val meta: AlbumMeta,
    val entryCount: Int,
    val firstEntry: VaultEntry?
)
