package dev.encgallery.gallery

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import android.system.ErrnoException
import android.system.OsConstants
import dev.encgallery.logging.EncLog

class ShareContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    private fun tokenOf(uri: Uri): String? = uri.lastPathSegment

    override fun getType(uri: Uri): String? =
        tokenOf(uri)?.let { ShareMemoryStore.get(it)?.mime }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val item = tokenOf(uri)?.let { ShareMemoryStore.get(it) } ?: return null
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = MatrixCursor(cols)
        val row = cursor.newRow()
        cols.forEach { col ->
            when (col) {
                OpenableColumns.DISPLAY_NAME -> row.add(col, item.name)
                OpenableColumns.SIZE -> row.add(col, (item.bytes?.size ?: 0).toLong())
                else -> row.add(col, null)
            }
        }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") {
            EncLog.w(TAG, "openFile rejected non-read mode '$mode'")
            return null
        }
        val item = tokenOf(uri)?.let { ShareMemoryStore.get(it) } ?: return null
        val sm = context?.getSystemService(StorageManager::class.java) ?: return null
        val callback = object : ProxyFileDescriptorCallback() {
            override fun onGetSize(): Long = (item.bytes?.size ?: 0).toLong()

            override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
                val b = item.bytes ?: throw ErrnoException("onRead", OsConstants.EIO)
                if (offset >= b.size) return 0

                val start = offset.toInt()
                val n = minOf(size, b.size - start)
                System.arraycopy(b, start, data, 0, n)
                return n
            }

            override fun onRelease() {}
        }
        return sm.openProxyFileDescriptor(
            ParcelFileDescriptor.MODE_READ_ONLY,
            callback,
            ShareMemoryStore.handler,
        )
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val TAG = "ShareProvider"
        fun authority(packageName: String): String = "$packageName.shareprovider"
        fun uriFor(packageName: String, token: String): Uri =
            Uri.parse("content://${authority(packageName)}/$token")
    }
}
