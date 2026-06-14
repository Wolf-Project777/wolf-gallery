package dev.encgallery.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

class DialogHostState internal constructor() {
    private val _entries = mutableStateListOf<Entry>()
    internal val entries: List<Entry> get() = _entries

    internal class Entry(val id: Any, val body: @Composable () -> Unit)

    internal fun add(entry: Entry) { _entries.add(entry) }
    internal fun remove(entry: Entry) { _entries.remove(entry) }
}

class DropdownHostState internal constructor() {
    private val _entries = mutableStateListOf<Entry>()
    internal val entries: List<Entry> get() = _entries

    internal class Entry(val id: Any, val body: @Composable () -> Unit)

    internal fun add(entry: Entry) { _entries.add(entry) }
    internal fun remove(entry: Entry) { _entries.remove(entry) }
}

val LocalDialogHost = staticCompositionLocalOf<DialogHostState?> { null }

val LocalDropdownHost = staticCompositionLocalOf<DropdownHostState?> { null }

val LocalDialogForceInline = staticCompositionLocalOf { false }

@Composable
fun DialogHostContainer(content: @Composable () -> Unit) {
    val dialogs = remember { DialogHostState() }
    val dropdowns = remember { DropdownHostState() }
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalDialogHost provides dialogs,
            LocalDropdownHost provides dropdowns
        ) {
            content()
        }

        dropdowns.entries.forEach { entry ->
            key(entry.id) { entry.body() }
        }
        dialogs.entries.forEach { entry ->
            key(entry.id) { entry.body() }
        }
    }
}
