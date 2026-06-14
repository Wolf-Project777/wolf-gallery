package dev.encgallery.session

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

object SessionState {

    val unlocked: MutableState<Boolean> = mutableStateOf(false)

    @Volatile
    var backgroundedAt: Long? = null
}
