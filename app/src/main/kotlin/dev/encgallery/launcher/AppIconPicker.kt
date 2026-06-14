package dev.encgallery.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.encgallery.R
import dev.encgallery.settings.AppSettings

@Composable
fun AppIconPicker() {
    val context = LocalContext.current
    val selected = AppSettings.activeIconVariant.value
    var pending by remember { mutableStateOf<Int?>(null) }

    val variants = listOf(
        Triple(1, R.drawable.ic_launcher_fg_cosmic, R.color.ic_launcher_bg_cosmic),
        Triple(2, R.drawable.ic_launcher_fg_nebula, R.color.ic_launcher_bg_nebula),
        Triple(3, R.drawable.ic_launcher_fg_shield, R.color.ic_launcher_bg_shield),
        Triple(4, R.drawable.ic_launcher_fg_blade, R.color.ic_launcher_bg_blade),
        Triple(5, R.drawable.ic_launcher_fg_copper, R.color.ic_launcher_bg_copper),
        Triple(6, R.drawable.ic_launcher_fg_lunar, R.color.ic_launcher_bg_lunar),
        Triple(7, R.drawable.ic_launcher_fg_steel, R.color.ic_launcher_bg_steel),
        Triple(8, R.drawable.ic_launcher_fg_ember, R.color.ic_launcher_bg_ember),
        Triple(9, R.drawable.ic_launcher_fg_floral, R.color.ic_launcher_bg_floral),
        Triple(10, R.drawable.ic_launcher_fg_lavender, R.color.ic_launcher_bg_lavender)
    )

    val columns = 3
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (rowVariants in variants.chunked(columns)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for ((n, fgRes, bgRes) in rowVariants) {
                    IconTile(
                        variant = n,
                        foregroundRes = fgRes,
                        backgroundColorRes = bgRes,
                        isSelected = n == selected,
                        onClick = { if (n != selected) pending = n },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }

                repeat(columns - rowVariants.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    pending?.let { target ->

        val frameShape = RoundedCornerShape(0.dp)
        val frameColor = MaterialTheme.colorScheme.outline
        AlertDialog(
            onDismissRequest = { pending = null },
            modifier = Modifier
                .border(width = 3.dp, color = frameColor.copy(alpha = 0.35f), shape = frameShape)
                .border(width = 1.dp, color = frameColor, shape = frameShape),

            shape = frameShape,
            title = { Text(stringResource(R.string.settings_app_icon_apply_title)) },
            text = { Text(stringResource(R.string.settings_app_icon_apply_subtitle)) },
            confirmButton = {
                TextButton(onClick = {
                    IconSwitcher.setActiveVariant(context, target)
                    pending = null
                }) { Text(stringResource(R.string.settings_app_icon_apply_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text(stringResource(R.string.settings_app_icon_apply_cancel))
                }
            }
        )
    }
}

@Composable
private fun IconTile(
    variant: Int,
    foregroundRes: Int,
    backgroundColorRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = 28)

    val keepShapeAndBg = foregroundRes == R.drawable.ic_launcher_fg_lavender

    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (keepShapeAndBg) {

            Box(
                modifier = Modifier
                    .fillMaxSize(0.72f)
                    .clip(shape)
                    .background(colorResource(backgroundColorRes))
                    .then(
                        if (isSelected)
                            Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), shape)
                        else
                            Modifier.border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)), shape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(foregroundRes),
                    contentDescription = stringResource(R.string.settings_app_icon_variant, variant),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {

            androidx.compose.foundation.Image(
                painter = painterResource(foregroundRes),
                contentDescription = stringResource(R.string.settings_app_icon_variant, variant),
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isSelected)
                            Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), shape)
                        else Modifier
                    )
            )
        }
    }
}
