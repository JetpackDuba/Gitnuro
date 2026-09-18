package com.jetpackduba.gitnuro.keybindings

import androidx.compose.ui.input.key.Key
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KeybindingTest {
    @Test
    fun macOsTabLeftUsesOnlyControlShiftTab() {
        val bindings = macKeybindings().getValue(KeybindingOption.CHANGE_CURRENT_TAB_LEFT)

        assertEquals(
            listOf(Keybinding(key = Key.Tab, control = true, shift = true)),
            bindings,
        )
        assertTrue(bindings.none { it.alt || it.meta })
    }

    @Test
    fun macOsTabRightUsesOnlyControlTab() {
        val bindings = macKeybindings().getValue(KeybindingOption.CHANGE_CURRENT_TAB_RIGHT)

        assertEquals(
            listOf(Keybinding(key = Key.Tab, control = true)),
            bindings,
        )
        assertTrue(bindings.none { it.alt || it.meta })
    }

    @Test
    fun macOsDoesNotUseOptionArrowOrCommandTabForTabNavigation() {
        val macBindings = macKeybindings()
        val tabBindings = tabNavigationBindings(macBindings)

        assertFalse(tabBindings.contains(Keybinding(key = Key.DirectionLeft, alt = true)))
        assertFalse(tabBindings.contains(Keybinding(key = Key.DirectionRight, alt = true)))
        assertFalse(tabBindings.contains(Keybinding(key = Key.Tab, meta = true)))
        assertFalse(tabBindings.contains(Keybinding(key = Key.Tab, meta = true, shift = true)))
        assertTrue(tabBindings.none { it.alt || it.meta })
    }

    @Test
    fun baseBindingsRetainAltArrowAndControlTab() {
        val base = baseKeybindings()

        assertEquals(
            listOf(
                Keybinding(key = Key.DirectionLeft, alt = true),
                Keybinding(key = Key.Tab, control = true, shift = true),
            ),
            base.getValue(KeybindingOption.CHANGE_CURRENT_TAB_LEFT),
        )
        assertEquals(
            listOf(
                Keybinding(key = Key.DirectionRight, alt = true),
                Keybinding(key = Key.Tab, control = true),
            ),
            base.getValue(KeybindingOption.CHANGE_CURRENT_TAB_RIGHT),
        )
    }

    @Test
    fun linuxWindowsAndUnknownOsUseUnchangedBaseMap() {
        val base = baseKeybindings()

        assertEquals(base, linuxKeybindings())
        assertEquals(base, windowsKeybindings())
        assertEquals(
            base.getValue(KeybindingOption.CHANGE_CURRENT_TAB_LEFT),
            linuxKeybindings().getValue(KeybindingOption.CHANGE_CURRENT_TAB_LEFT),
        )
        assertEquals(
            base.getValue(KeybindingOption.CHANGE_CURRENT_TAB_RIGHT),
            windowsKeybindings().getValue(KeybindingOption.CHANGE_CURRENT_TAB_RIGHT),
        )
    }

    @Test
    fun macOsOpenCloseTabAndTextAcceptUseCommand() {
        val macBindings = macKeybindings()

        assertEquals(
            listOf(Keybinding(key = Key.T, meta = true)),
            macBindings.getValue(KeybindingOption.OPEN_NEW_TAB),
        )
        assertEquals(
            listOf(Keybinding(key = Key.W, meta = true)),
            macBindings.getValue(KeybindingOption.CLOSE_CURRENT_TAB),
        )
        assertEquals(
            listOf(Keybinding(key = Key.Enter, meta = true)),
            macBindings.getValue(KeybindingOption.TEXT_ACCEPT),
        )
    }

    private fun tabNavigationBindings(
        bindings: Map<KeybindingOption, List<Keybinding>>,
    ): List<Keybinding> {
        return bindings.getValue(KeybindingOption.CHANGE_CURRENT_TAB_LEFT) +
                bindings.getValue(KeybindingOption.CHANGE_CURRENT_TAB_RIGHT)
    }
}
