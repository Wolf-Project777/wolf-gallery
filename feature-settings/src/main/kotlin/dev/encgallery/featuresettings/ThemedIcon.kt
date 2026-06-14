package dev.encgallery.featuresettings

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource

val LocalThemeVariant: ProvidableCompositionLocal<ThemeVariant> =
    staticCompositionLocalOf { ThemeVariant.DEFAULT }

object MedievalIcons {
    val Add: Int = R.drawable.ic_medieval_add
    val Album: Int = R.drawable.ic_medieval_album
    val ArrowBack: Int = R.drawable.ic_medieval_arrow_back
    val ArrowDown: Int = R.drawable.ic_medieval_arrow_down
    val ArrowRight: Int = R.drawable.ic_medieval_arrow_right
    val Check: Int = R.drawable.ic_medieval_check
    val Close: Int = R.drawable.ic_medieval_close
    val Edit: Int = R.drawable.ic_medieval_edit
    val Folder: Int = R.drawable.ic_medieval_folder
    val Info: Int = R.drawable.ic_medieval_info
    val ListItems: Int = R.drawable.ic_medieval_list
    val Lock: Int = R.drawable.ic_medieval_lock
    val Menu: Int = R.drawable.ic_medieval_menu
    val MoreVert: Int = R.drawable.ic_medieval_more
    val Refresh: Int = R.drawable.ic_medieval_refresh
    val Search: Int = R.drawable.ic_medieval_search
    val SelectAll: Int = R.drawable.ic_medieval_select_all
    val Send: Int = R.drawable.ic_medieval_send
    val Settings: Int = R.drawable.ic_medieval_settings
    val Share: Int = R.drawable.ic_medieval_share
    val Sort: Int = R.drawable.ic_medieval_sort
    val Trash: Int = R.drawable.ic_medieval_trash
}

@Composable
fun ThemedIcon(
    fallback: ImageVector,
    @DrawableRes medievalDrawable: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val variant = LocalThemeVariant.current
    if (variant == ThemeVariant.MEDIEVAL) {
        Icon(
            painter = painterResource(medievalDrawable),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = Color.Unspecified
        )
    } else {
        Icon(
            imageVector = fallback,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun ThemedIcon(
    vector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val variant = LocalThemeVariant.current
    val medieval = vectorToMedievalDrawable(vector)
    if (variant == ThemeVariant.MEDIEVAL && medieval != null) {
        Icon(
            painter = painterResource(medieval),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = Color.Unspecified
        )
    } else {
        Icon(
            imageVector = vector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

private fun vectorToMedievalDrawable(vector: ImageVector): Int? = when (vector.name) {
    Icons.Filled.Add.name -> MedievalIcons.Add
    Icons.Filled.ArrowBack.name -> MedievalIcons.ArrowBack
    Icons.Filled.Check.name -> MedievalIcons.Check
    Icons.Filled.Clear.name -> MedievalIcons.Close
    Icons.Filled.Close.name -> MedievalIcons.Close
    Icons.Filled.Delete.name -> MedievalIcons.Trash
    Icons.Filled.Done.name -> MedievalIcons.Check
    Icons.Filled.Edit.name -> MedievalIcons.Edit
    Icons.Filled.Info.name -> MedievalIcons.Info
    Icons.Filled.KeyboardArrowDown.name -> MedievalIcons.ArrowDown
    Icons.Filled.KeyboardArrowRight.name -> MedievalIcons.ArrowRight
    Icons.Filled.List.name -> MedievalIcons.ListItems
    Icons.Filled.Lock.name -> MedievalIcons.Lock
    Icons.Filled.Menu.name -> MedievalIcons.Menu
    Icons.Filled.MoreVert.name -> MedievalIcons.MoreVert
    Icons.Filled.Refresh.name -> MedievalIcons.Refresh
    Icons.Filled.Search.name -> MedievalIcons.Search
    Icons.Filled.Send.name -> MedievalIcons.Send
    Icons.Filled.Settings.name -> MedievalIcons.Settings
    Icons.Filled.Share.name -> MedievalIcons.Share
    else -> null
}

@Composable
fun ProvideThemeVariant(variant: ThemeVariant, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThemeVariant provides variant, content = content)
}
