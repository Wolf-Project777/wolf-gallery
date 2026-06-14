package dev.encgallery.gallery

object SupportedMimeTypes {

    val IMAGE: List<String> = listOf(
        "image/jpeg",
        "image/png",
        "image/heic",
        "image/heif",
        "image/webp",
        "image/avif",
        "image/gif",
        "image/bmp"
    )

    val VIDEO: List<String> = listOf(
        "video/mp4",
        "video/x-matroska",
        "video/webm",
        "video/3gpp"
    )

    private val ALL: Set<String> = (IMAGE + VIDEO).toSet()

    fun isAccepted(mimeType: String?): Boolean =
        mimeType != null && mimeType.lowercase() in ALL
}
