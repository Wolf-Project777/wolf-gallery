package dev.encgallery.gallery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.KeystoreAesGcm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImportPanel(
    modifier: Modifier = Modifier,
    targetAlbumUuid: String? = null,
    onBatchComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importing by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentTotal by remember { mutableIntStateOf(0) }
    var currentDisplayName by remember { mutableStateOf("") }

    var lastSummary by remember { mutableStateOf("") }

    val runImport: (List<Uri>) -> Unit = { uris ->
        importing = true
        lastSummary = ""
        currentIndex = 0
        currentTotal = uris.size
        currentDisplayName = ""

        scope.launch {
            val importer = PhotoImporter(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
            try {
                withContext(Dispatchers.IO) {
                    val effectiveTarget = targetAlbumUuid ?: VaultPaths.IMPORTED_ALBUM_UUID
                    importer.importAll(
                        context.applicationContext,
                        uris,
                        targetAlbumUuid = effectiveTarget
                    ) { progress ->
                        when (progress) {
                            is PhotoImporter.Progress.Started -> {
                                currentTotal = progress.total
                            }
                            is PhotoImporter.Progress.FileStarted -> {
                                currentIndex = progress.index
                                currentTotal = progress.total
                                currentDisplayName = progress.displayName
                            }
                            is PhotoImporter.Progress.FileSucceeded -> {
                                currentIndex = progress.index
                            }
                            is PhotoImporter.Progress.FileFailed -> {
                                currentIndex = progress.index
                            }
                            is PhotoImporter.Progress.Done -> {
                                lastSummary = formatSummary(
                                    context, progress.successful, progress.failed, progress.total
                                )
                                onBatchComplete()
                            }
                        }
                    }
                }
            } finally {
                importing = false
                currentDisplayName = ""
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (!uris.isNullOrEmpty()) runImport(uris)

    }

    val chooserTitle = stringResource(R.string.gallery_import_chooser_title)
    val chooserLauncher = rememberLauncherForActivityResult(
        contract = GetContentsWithChooser(chooserTitle)
    ) { uris ->
        if (uris.isNotEmpty()) runImport(uris)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.gallery_import_header),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(10.dp))

        if (importing) {

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                onClick = {}
            ) {
                Text(stringResource(R.string.gallery_import_btn_busy))
            }
        } else {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.gallery_import_btn_gallery))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = {
                        chooserLauncher.launch(Unit)
                    }
                ) {
                    Text(stringResource(R.string.gallery_import_btn_files))
                }
            }
        }

        if (importing && currentTotal > 0) {
            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { currentIndex.toFloat() / currentTotal.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.gallery_import_progress_line,
                    currentIndex,
                    currentTotal,
                    currentDisplayName
                ),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }

        if (!importing && lastSummary.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    text = lastSummary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun formatSummary(
    context: android.content.Context,
    successful: Int,
    failed: Int,
    total: Int
): String = when {
    failed == 0 -> context.getString(R.string.gallery_import_done_ok, successful, total)
    successful == 0 -> context.getString(R.string.gallery_import_done_all_failed, total)
    else -> context.getString(
        R.string.gallery_import_done_partial,
        successful, total, failed
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ImportPanelPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            ImportPanel()
        }
    }
}

private class GetContentsWithChooser(
    private val chooserTitle: String
) : ActivityResultContract<Unit, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit): Intent {

        val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
        return Intent.createChooser(pick, chooserTitle)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()

        intent.clipData?.let { clip ->
            return List(clip.itemCount) { i -> clip.getItemAt(i).uri }
        }

        intent.data?.let { return listOf(it) }
        return emptyList()
    }
}
