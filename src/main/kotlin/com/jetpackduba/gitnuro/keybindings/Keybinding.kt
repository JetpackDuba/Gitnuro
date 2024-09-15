@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.keybindings

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import com.jetpackduba.gitnuro.system.OS
import com.jetpackduba.gitnuro.system.currentOs

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
    TEXT_ACCEPT,

    /**
     * Used to close dialogs or components
     */
    EXIT,

    /**
     * Used to go up in lists
     */
    UP,

    /**
     * Used to go down in lists
     */
    DOWN,

    /**
     * Used to pull in current repository
     */
    PULL,

    /**
     * Used to push in current repository
     */
    PUSH,

    /**
     * Used to show branch creation dialog
     */
    BRANCH_CREATE,

    /**
     * Used to stash workspace changes
     */
    STASH,

    /**
     * Used to pop stash changes to workspace
     */
    STASH_POP,

    /**
     * Used to open a repository
     */
    OPEN_REPOSITORY,

    /**
     * Used to open a new tab
     */
    OPEN_NEW_TAB,

    /**
     * Used to close current tab
     */
    CLOSE_CURRENT_TAB,

    /**
     * Used to change current tab to the one in the left
     */
    CHANGE_CURRENT_TAB_LEFT,

    /**
     * Used to change current tab to the one in the right
     */
    CHANGE_CURRENT_TAB_RIGHT,
}


@OptIn(ExperimentalComposeUiApi::class)
private fun baseKeybindings() = mapOf(
    KeybindingOption.REFRESH to listOf(
        Keybinding(key = Key.F5),
        Keybinding(control = true, key = Key.R),
    ),
    KeybindingOption.SIMPLE_ACCEPT to listOf(
        Keybinding(key = Key.Enter),
    ),
    KeybindingOption.TEXT_ACCEPT to listOf(
        Keybinding(control = true, key = Key.Enter),
    ),
    KeybindingOption.EXIT to listOf(
        Keybinding(key = Key.Escape),
    ),
    KeybindingOption.UP to listOf(
        Keybinding(key = Key.DirectionUp),
    ),
    KeybindingOption.DOWN to listOf(
        Keybinding(key = Key.DirectionDown),
    ),
    KeybindingOption.PULL to listOf(
        Keybinding(key = Key.U, control = true),
    ),
    KeybindingOption.PUSH to listOf(
        Keybinding(key = Key.P, control = true),
    ),
    KeybindingOption.BRANCH_CREATE to listOf(
        Keybinding(key = Key.B, control = true),
    ),
    KeybindingOption.STASH to listOf(
        Keybinding(key = Key.S, control = true),
    ),
    KeybindingOption.STASH_POP to listOf(
        Keybinding(key = Key.S, control = true, shift = true),
    ),
    KeybindingOption.OPEN_REPOSITORY to listOf(
        Keybinding(key = Key.O, control = true),
    ),
    KeybindingOption.OPEN_NEW_TAB to listOf(
        Keybinding(key = Key.T, control = true),
    ),
    KeybindingOption.CLOSE_CURRENT_TAB to listOf(
        Keybinding(key = Key.W, control = true),
    ),
    KeybindingOption.CHANGE_CURRENT_TAB_LEFT to listOf(
        Keybinding(key = Key.DirectionLeft, alt = true),
        Keybinding(key = Key.Tab, control = true, shift = true),
    ),
    KeybindingOption.CHANGE_CURRENT_TAB_RIGHT to listOf(
        Keybinding(key = Key.DirectionRight, alt = true),
        Keybinding(key = Key.Tab, control = true),
    ),
)

private fun linuxKeybindings(): Map<KeybindingOption, List<Keybinding>> = baseKeybindings()
private fun windowsKeybindings(): Map<KeybindingOption, List<Keybinding>> = baseKeybindings()

private fun macKeybindings(): Map<KeybindingOption, List<Keybinding>> {
    val macBindings = baseKeybindings().toMutableMap()

    macBindings.apply {
        val keysToReplaceControlWithCommand = listOf(
            KeybindingOption.REFRESH,
            KeybindingOption.PULL,
            KeybindingOption.PUSH,
            KeybindingOption.BRANCH_CREATE,
            KeybindingOption.STASH,
            KeybindingOption.STASH_POP,
            KeybindingOption.OPEN_REPOSITORY,
            KeybindingOption.OPEN_NEW_TAB,
            KeybindingOption.CLOSE_CURRENT_TAB,
        )

        for (key in keysToReplaceControlWithCommand) {
            val originalKeybindings = this[key] ?: emptyList()
            val newKeybindings = originalKeybindings.map {
                it.copy(meta = it.control, control = false)
            }

            this[key] = newKeybindings
        }
    }

    return macBindings
}

val keybindings by lazy {
    return@lazy when (currentOs) {
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
    } && this.type == KeyEventType.KeyDown
}

val KeybindingOption.keyBinding
    get() = keybindings[this]?.firstOrNull()