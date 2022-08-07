@file:OptIn(ExperimentalComposeUiApi::class)

package app.keybindings

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import app.extensions.OS
import app.extensions.getCurrentOs

data class Keybinding(
    val alt: Boolean = false,
    val control: Boolean = false,
    val meta: Boolean = false,
    val shift: Boolean = false,
    val key: Key
)

enum class KeybindingOption {
    REFRESH,

    /**
     * Used mostly for dialogs with a single input field
     */
    SIMPLE_ACCEPT,

    /**
     * Used to accept multi-line text field like the commit message
     */
    ACCEPT,

    /**
     * Used to close dialogs or components
     */
    EXIT,
}


private fun baseKeybindings() = mapOf(
    KeybindingOption.REFRESH to listOf(
        Keybinding(key = Key.F5),
        Keybinding(control = true, key = Key.R),
    ),
    KeybindingOption.SIMPLE_ACCEPT to listOf(
        Keybinding(key = Key.Enter),
    ),
    KeybindingOption.ACCEPT to listOf(
        Keybinding(control = true, key = Key.Enter),
    ),
    KeybindingOption.EXIT to listOf(
        Keybinding(key = Key.Escape),
    ),
)

private fun linuxKeybindings(): Map<KeybindingOption, List<Keybinding>> = baseKeybindings()
private fun windowsKeybindings(): Map<KeybindingOption, List<Keybinding>> = baseKeybindings()

private fun macKeybindings(): Map<KeybindingOption, List<Keybinding>> {
    val macBindings = baseKeybindings().toMutableMap()

    macBindings.apply {
        this[KeybindingOption.REFRESH] = listOf(
            Keybinding(key = Key.F5),
            Keybinding(meta = true, key = Key.R),
        )
    }

    return macBindings
}

val keybindings by lazy {
    return@lazy when(getCurrentOs()) {
        OS.LINUX -> linuxKeybindings()
        OS.WINDOWS -> windowsKeybindings()
        OS.MAC -> macKeybindings()
        OS.UNKNOWN -> baseKeybindings()
    }
}

fun KeyEvent.matchesBinding(keybindingOption: KeybindingOption): Boolean {
    val keybindings = keybindings

    val matchingKeybindingsList = keybindings[keybindingOption] ?: return false

    return matchingKeybindingsList.any { keybinding ->
        keybinding.alt == this.isAltPressed &&
                keybinding.control == this.isCtrlPressed &&
                keybinding.meta == this.isMetaPressed &&
                keybinding.shift == this.isShiftPressed &&
                keybinding.key == this.key
    }
}