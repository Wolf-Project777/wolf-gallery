package dev.encgallery.gallery

enum class AlbumSortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_AZ,
    NAME_ZA;

    companion object {
        val DEFAULT = DATE_DESC
    }
}

enum class EntrySortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_AZ,
    NAME_ZA;

    companion object {
        val DEFAULT = DATE_DESC
    }
}

fun sortAlbumSummaries(
    albums: List<AlbumSummary>,
    order: AlbumSortOrder
): List<AlbumSummary> {
    val sorted = when (order) {
        AlbumSortOrder.DATE_DESC -> albums.sortedByDescending { it.meta.modifiedAt }
        AlbumSortOrder.DATE_ASC -> albums.sortedBy { it.meta.modifiedAt }
        AlbumSortOrder.NAME_AZ ->
            albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.meta.name })
        AlbumSortOrder.NAME_ZA ->
            albums.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.meta.name }
            ).asReversed()
    }

    val (pinned, rest) = sorted.partition { it.meta.pinned }
    return pinned + rest
}

fun sortAlbumGroupSummaries(
    groups: List<AlbumGroupSummary>,
    order: AlbumSortOrder
): List<AlbumGroupSummary> {
    val sorted = when (order) {
        AlbumSortOrder.DATE_DESC -> groups.sortedByDescending { it.meta.modifiedAt }
        AlbumSortOrder.DATE_ASC -> groups.sortedBy { it.meta.modifiedAt }
        AlbumSortOrder.NAME_AZ ->
            groups.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.meta.name })
        AlbumSortOrder.NAME_ZA ->
            groups.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.meta.name }
            ).asReversed()
    }

    val (pinned, rest) = sorted.partition { it.meta.pinned }
    return pinned + rest
}

fun sortEntries(
    entries: List<VaultEntry>,
    order: EntrySortOrder,
    filenameCache: Map<String, String?>
): List<VaultEntry> = when (order) {
    EntrySortOrder.DATE_DESC -> entries.sortedByDescending { it.mtimeMillis }
    EntrySortOrder.DATE_ASC -> entries.sortedBy { it.mtimeMillis }

    EntrySortOrder.NAME_AZ -> entries.sortedWith(
        compareBy<VaultEntry> { filenameCache[it.uuid] == null }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { filenameCache[it.uuid] ?: "" }
            .thenBy { it.uuid }
    )
    EntrySortOrder.NAME_ZA -> entries.sortedWith(
        compareBy<VaultEntry> { filenameCache[it.uuid] == null }
            .thenByDescending(String.CASE_INSENSITIVE_ORDER) { filenameCache[it.uuid] ?: "" }
            .thenBy { it.uuid }
    )
}
