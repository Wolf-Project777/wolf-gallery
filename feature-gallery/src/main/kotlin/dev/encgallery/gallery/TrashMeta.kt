package dev.encgallery.gallery

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class TrashMeta(
    val uuid: String,
    val kind: TrashKind,
    val originalAlbumUuid: String,
    val originalAlbumNameHint: String?,
    val originalMtime: Long,
    val trashedAt: Long,
    val originalGroupUuid: String? = null,
    val originalName: String? = null,
    val albumUuids: List<String> = emptyList()
) {

    fun toJsonBytes(): ByteArray = JSONObject().apply {
        put("version", JSON_VERSION)
        put("uuid", uuid)
        put("kind", kind.name)
        put("originalAlbumUuid", originalAlbumUuid)
        put("originalAlbumNameHint", originalAlbumNameHint ?: JSONObject.NULL)
        put("originalMtime", originalMtime)
        put("trashedAt", trashedAt)
        put("originalGroupUuid", originalGroupUuid ?: JSONObject.NULL)
        put("originalName", originalName ?: JSONObject.NULL)
        put("albumUuids", JSONArray(albumUuids))
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {

        const val JSON_VERSION = 2

        const val TRASH_RETENTION_DAYS = 30

        const val TRASH_RETENTION_MILLIS: Long = TRASH_RETENTION_DAYS * 24L * 60L * 60L * 1000L

        fun fromJsonBytes(bytes: ByteArray, expectedUuid: String): TrashMeta? {
            return try {
                val root = JSONObject(String(bytes, Charsets.UTF_8))
                val version = root.optInt("version", 0)
                if (version < 1 || version > JSON_VERSION) return null
                val storedUuid = root.getString("uuid")
                if (storedUuid != expectedUuid) return null
                val nameHint: String? = optStringOrNull(root, "originalAlbumNameHint")
                val kind = parseKind(root.optString("kind", TrashKind.ENTRY.name))
                    ?: return null
                val originalGroupUuid = optStringOrNull(root, "originalGroupUuid")
                val originalName = optStringOrNull(root, "originalName")
                val albumUuids = root.optJSONArray("albumUuids")?.let { arr ->
                    List(arr.length()) { i -> arr.getString(i) }
                } ?: emptyList()
                TrashMeta(
                    uuid = storedUuid,
                    kind = kind,
                    originalAlbumUuid = root.getString("originalAlbumUuid"),
                    originalAlbumNameHint = nameHint,
                    originalMtime = root.getLong("originalMtime"),
                    trashedAt = root.getLong("trashedAt"),
                    originalGroupUuid = originalGroupUuid,
                    originalName = originalName,
                    albumUuids = albumUuids
                )
            } catch (_: Throwable) {
                null
            }
        }

        private fun optStringOrNull(root: JSONObject, key: String): String? = when {
            !root.has(key) -> null
            root.isNull(key) -> null
            else -> root.getString(key)
        }

        private fun parseKind(name: String): TrashKind? = try {
            TrashKind.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

enum class TrashKind { ENTRY, ALBUM, GROUP }

sealed class TrashRecord {

    abstract val uuid: String

    abstract val meta: TrashMeta

    val trashedAt: Long get() = meta.trashedAt

    fun daysUntilPurge(now: Long = System.currentTimeMillis()): Int {
        val ageMillis = now - trashedAt
        val remainingMillis = TrashMeta.TRASH_RETENTION_MILLIS - ageMillis
        return (remainingMillis / (24L * 60 * 60 * 1000L)).toInt()
    }
}

data class TrashEntry(
    override val meta: TrashMeta,
    val blobFile: File,
    val thumbFile: File,
    val blobSizeBytes: Long
) : TrashRecord() {
    override val uuid: String get() = meta.uuid

    val hasThumbnail: Boolean get() = thumbFile.exists()
}

data class TrashAlbum(
    override val meta: TrashMeta,
    val albumDir: File,
    val entryCount: Int,
    val totalSizeBytes: Long
) : TrashRecord() {
    override val uuid: String get() = meta.uuid

    val name: String get() = meta.originalName.orEmpty()
}

data class TrashGroup(
    override val meta: TrashMeta,
    val groupDir: File,
    val albumCount: Int
) : TrashRecord() {
    override val uuid: String get() = meta.uuid

    val name: String get() = meta.originalName.orEmpty()
}
