package dev.encgallery.gallery

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AlbumExportBundle(
    val folderName: String,
    val entries: List<VaultEntry>
)

object EntryExport {

    private const val TAG = "EntryExport"

    suspend fun suggestedName(
        context: Context,
        entry: VaultEntry,
        keystore: KeystoreAesGcm
    ): String = withContext(Dispatchers.IO) {
        resolveName(EntriesRepository(keystore), entry)
    }

    suspend fun exportToUri(
        context: Context,
        entry: VaultEntry,
        uri: Uri,
        keystore: KeystoreAesGcm
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val opened = resolver.openOutputStream(uri)
            if (opened == null) {
                EncLog.w(TAG, "export ${entry.uuid}: null OutputStream for destination")
                return@withContext false
            }
            opened.use { out ->
                EncryptedFileBlob(keystore).decryptToStream(entry.blobFile, out)
            }
            EncLog.i(TAG, "export ${entry.uuid} ok (${entry.blobSizeBytes} enc bytes)")
            true
        } catch (t: Throwable) {
            EncLog.w(TAG, "export ${entry.uuid} failed: ${t.javaClass.simpleName}: ${t.message}")

            try {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } catch (_: Throwable) {
            }
            false
        }
    }

    suspend fun exportToTree(
        context: Context,
        entries: List<VaultEntry>,
        treeUri: Uri,
        keystore: KeystoreAesGcm
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext 0 to 0
        val resolver = context.contentResolver
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val repo = EntriesRepository(keystore)
        val usedNames = HashSet<String>()
        var ok = 0
        entries.forEach { entry ->
            val name = uniqueName(resolveName(repo, entry), usedNames)
            usedNames.add(name)
            if (writeEntryInto(resolver, parentDocUri, name, entry, keystore)) ok++
        }
        EncLog.i(TAG, "exportToTree: $ok/${entries.size} files written")
        ok to entries.size
    }

    suspend fun exportAlbumsToTree(
        context: Context,
        albums: List<AlbumExportBundle>,
        treeUri: Uri,
        keystore: KeystoreAesGcm
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val totalFiles = albums.sumOf { it.entries.size }
        if (totalFiles == 0) return@withContext 0 to 0
        val resolver = context.contentResolver
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val repo = EntriesRepository(keystore)
        val usedFolders = HashSet<String>()
        var ok = 0
        albums.forEach { album ->
            val folderName = uniqueName(
                sanitize(album.folderName) ?: "WolfGallery", usedFolders
            )
            usedFolders.add(folderName)
            val dirUri: Uri? = try {
                DocumentsContract.createDocument(
                    resolver,
                    rootDocUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    folderName
                )
            } catch (t: Throwable) {
                EncLog.w(TAG, "exportAlbumsToTree: mkdir '$folderName' failed: ${t.javaClass.simpleName}")
                null
            }
            if (dirUri == null) {
                EncLog.w(
                    TAG,
                    "exportAlbumsToTree: no dir for '$folderName', skipping ${album.entries.size} file(s)"
                )
                return@forEach
            }
            val usedNames = HashSet<String>()
            album.entries.forEach { entry ->
                val name = uniqueName(resolveName(repo, entry), usedNames)
                usedNames.add(name)
                if (writeEntryInto(resolver, dirUri, name, entry, keystore)) ok++
            }
        }
        EncLog.i(TAG, "exportAlbumsToTree: $ok/$totalFiles file(s) into ${albums.size} folder(s)")
        ok to totalFiles
    }

    private fun writeEntryInto(
        resolver: ContentResolver,
        parentDocUri: Uri,
        name: String,
        entry: VaultEntry,
        keystore: KeystoreAesGcm
    ): Boolean {
        var docUri: Uri? = null
        return try {
            docUri = DocumentsContract.createDocument(
                resolver,
                parentDocUri,
                "application/octet-stream",
                name
            )
            if (docUri == null) {
                EncLog.w(TAG, "export: createDocument null for ${entry.uuid}")
                return false
            }
            val opened = resolver.openOutputStream(docUri)
            if (opened == null) {
                EncLog.w(TAG, "export: null OutputStream for ${entry.uuid}")
                DocumentsContract.deleteDocument(resolver, docUri)
                return false
            }
            opened.use { out ->
                EncryptedFileBlob(keystore).decryptToStream(entry.blobFile, out)
            }
            true
        } catch (t: Throwable) {
            EncLog.w(TAG, "export: ${entry.uuid} failed: ${t.javaClass.simpleName}")
            docUri?.let {
                try {
                    DocumentsContract.deleteDocument(resolver, it)
                } catch (_: Throwable) {
                }
            }
            false
        }
    }

    private fun resolveName(repo: EntriesRepository, entry: VaultEntry): String {
        val original = try {
            repo.getMeta(entry.blobFile)?.originalFilename
        } catch (t: Throwable) {
            EncLog.w(TAG, "name read failed for ${entry.uuid}: ${t.javaClass.simpleName}")
            null
        }
        return sanitize(original) ?: "WolfGallery-${entry.uuid.take(8)}"
    }

    private fun sanitize(name: String?): String? {
        if (name == null) return null
        val cleaned = name.substringAfterLast('/').substringAfterLast('\\')
            .filter { it != '\r' && it != '\n' && it.code != 0 }
            .trim()
            .take(200)
        return cleaned.takeIf { it.isNotEmpty() }
    }

    private fun uniqueName(base: String, used: Set<String>): String {
        if (base !in used) return base
        val dot = base.lastIndexOf('.')
        val stem = if (dot > 0) base.substring(0, dot) else base
        val ext = if (dot > 0) base.substring(dot) else ""
        var i = 1
        while (true) {
            val candidate = "$stem ($i)$ext"
            if (candidate !in used) return candidate
            i++
        }
    }
}
