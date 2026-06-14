package dev.encgallery.gallery

import org.json.JSONObject

data class EntryMeta(
    val uuid: String,
    val originalFilename: String?,
    val importedAt: Long
) {

    fun toJsonBytes(): ByteArray = JSONObject().apply {
        put("version", JSON_VERSION)
        put("uuid", uuid)
        put("originalFilename", originalFilename ?: JSONObject.NULL)
        put("importedAt", importedAt)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {

        const val JSON_VERSION = 1

        fun fromJsonBytes(bytes: ByteArray, expectedUuid: String): EntryMeta? {
            return try {
                val root = JSONObject(String(bytes, Charsets.UTF_8))
                val version = root.optInt("version", 0)
                if (version != JSON_VERSION) return null
                val storedUuid = root.getString("uuid")
                if (storedUuid != expectedUuid) return null
                val name: String? = when {
                    !root.has("originalFilename") -> null
                    root.isNull("originalFilename") -> null
                    else -> root.getString("originalFilename")
                }
                EntryMeta(
                    uuid = storedUuid,
                    originalFilename = name,
                    importedAt = root.getLong("importedAt")
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}
