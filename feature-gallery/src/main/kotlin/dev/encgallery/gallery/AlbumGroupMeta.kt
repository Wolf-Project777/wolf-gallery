package dev.encgallery.gallery

import org.json.JSONArray
import org.json.JSONObject

data class AlbumGroupMeta(
    val uuid: String,
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val albumUuids: List<String>,

    val pinned: Boolean = false
) {

    fun toJsonBytes(): ByteArray = JSONObject().apply {
        put("version", JSON_VERSION)
        put("uuid", uuid)
        put("name", name)
        put("createdAt", createdAt)
        put("modifiedAt", modifiedAt)
        put("albumUuids", JSONArray().apply {
            albumUuids.forEach { put(it) }
        })
        if (pinned) put("pinned", true)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {

        const val JSON_VERSION = 1

        fun fromJsonBytes(bytes: ByteArray, expectedUuid: String): AlbumGroupMeta? {
            return try {
                val root = JSONObject(String(bytes, Charsets.UTF_8))
                val version = root.optInt("version", 0)
                if (version != JSON_VERSION) return null
                val storedUuid = root.getString("uuid")
                if (storedUuid != expectedUuid) return null
                val albumUuids = root.optJSONArray("albumUuids")?.let { arr ->
                    val out = ArrayList<String>(arr.length())
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i, null)
                        if (s != null && s.isNotEmpty() && s !in out) out += s
                    }
                    out.toList()
                } ?: emptyList()
                AlbumGroupMeta(
                    uuid = storedUuid,
                    name = root.getString("name"),
                    createdAt = root.getLong("createdAt"),
                    modifiedAt = root.getLong("modifiedAt"),
                    albumUuids = albumUuids,
                    pinned = root.optBoolean("pinned", false)
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}

data class AlbumGroupSummary(
    val meta: AlbumGroupMeta,
    val albums: List<AlbumSummary>
)
