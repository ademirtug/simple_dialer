package info.pakkudev.simple_dialer.telecom

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application-owned visibility state; never stores an Activity. */
class CallUiVisibility {
    private var startedScreens = 0
    private val mutableVisible = MutableStateFlow(false)
    val visible = mutableVisible.asStateFlow()
    fun entered() { startedScreens++; mutableVisible.value = true }
    fun left() { startedScreens = (startedScreens - 1).coerceAtLeast(0); mutableVisible.value = startedScreens > 0 }
}
