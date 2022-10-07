package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding

import com.jetpackduba.gitnuro.theme.secondaryTextColor
import java.awt.event.MouseEvent
import kotlin.math.abs

private var lastCheck: Long = 0
private const val MIN_TIME_BETWEEN_POPUPS = 20

@Composable
fun ContextMenu(items: () -> List<ContextMenuElement>, function: @Composable () -> Unit) {
    Box(modifier = Modifier.contextMenu(items), propagateMinConstraints = true) {
        function()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.contextMenu(items: () -> List<ContextMenuElement>): Modifier {
    val (lastMouseEventState, setLastMouseEventState) = remember { mutableStateOf<MouseEvent?>(null) }
    val mod = this.pointerInput(Unit) {

        while (true) {
            val lastMouseEvent = awaitPointerEventScope { awaitEventFirstDown() }
            val mouseEvent = lastMouseEvent.awtEventOrNull

            if (mouseEvent != null) {
                if (lastMouseEvent.button.isSecondary) {
                    val currentCheck = System.currentTimeMillis()
                    if (lastCheck != 0L && currentCheck - lastCheck < MIN_TIME_BETWEEN_POPUPS) {
                        println("IGNORE POPUP TRIGGERED!")
                    } else {
                        lastCheck = currentCheck

                        setLastMouseEventState(mouseEvent)
                    }
                }
            }
        }
    }

    if (lastMouseEventState != null) {
        showPopup(
            lastMouseEventState.x,
            lastMouseEventState.y,
            items(),
            onDismissRequest = { setLastMouseEventState(null) })
    }

    return mod
}

@Composable
fun showPopup(x: Int, y: Int, contextMenuElements: List<ContextMenuElement>, onDismissRequest: () -> Unit) {
    LaunchedEffect(contextMenuElements) {
        println("Items count ${contextMenuElements.count()}")
    }
    Popup(
        focusable = true,
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val resultY = if (popupContentSize.height > windowSize.height) {
                    0 // If the popup is taller than the window itself
                } else if (y + popupContentSize.height > windowSize.height) {
                    // If the end of the popup would go out of bounds.
                    // Move the Y a bit to the top to make it fit
                    y - abs(windowSize.height - (y + popupContentSize.height))
                } else {
                    y
                }

                val resultX = if (x + popupContentSize.width > windowSize.width && popupContentSize.width < x) {
                    // If the end of the popup would go out of bounds.
                    // Move the X a bit to the left to make it fit
                    x - abs(windowSize.width - (x + popupContentSize.width))
                } else {
                    x
                }

                return IntOffset(resultX, resultY)
            }
        },
        onDismissRequest = onDismissRequest
    ) {

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .shadow(4.dp)
                .width(300.dp)
                .background(MaterialTheme.colors.background)
                .run {
                    return@run if (!MaterialTheme.colors.isLight) {
                        this.border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.2f))
                    } else
                        this
                }
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.EXIT)) {
                        onDismissRequest()
                        true
                    } else
                        false
                },
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (item in contextMenuElements) {
                    when (item) {
                        is ContextMenuElement.ContextTextEntry -> TextEntry(item, onDismissRequest = onDismissRequest)
                        ContextMenuElement.ContextSeparator -> Separator()
                    }

                }
            }
        }
    }
}

@Composable
fun Separator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colors.onBackground.copy(alpha = 0.4f))
    )
}

@Composable
internal fun focusRequesterAndModifier(): Pair<FocusRequester, Modifier> {
    val focusRequester = remember { FocusRequester() }
    return focusRequester to Modifier.focusRequester(focusRequester)
}

@Composable
fun TextEntry(contextTextEntry: ContextMenuElement.ContextTextEntry, onDismissRequest: () -> Unit) {
    val icon = contextTextEntry.icon

    Row(
        modifier = Modifier
            .clickable {
                onDismissRequest()
                contextTextEntry.onClick()
            }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp).padding(end = 8.dp)) {
            if (icon != null) {
                Icon(
                    painter = icon(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = (MaterialTheme.colors.secondaryTextColor.copy(alpha = 0.8f))
                )
            }
        }

        Text(
            contextTextEntry.label,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

sealed interface ContextMenuElement {
    data class ContextTextEntry(
        val label: String,
        val icon: @Composable (() -> Painter)? = null,
        val onClick: () -> Unit = {}
    ) : ContextMenuElement

    object ContextSeparator : ContextMenuElement
}

private suspend fun AwaitPointerEventScope.awaitEventFirstDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (
        !event.changes.fastAll { it.changedToDown() }
    )
    return event
}