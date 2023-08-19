package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.awaitFirstDownEvent
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.isDark
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import kotlin.math.abs

private var lastCheck: Long = 0
private const val MIN_TIME_BETWEEN_POPUPS_IN_MS = 20
private const val BORDER_RADIUS = 4

@Composable
fun ContextMenu(items: () -> List<ContextMenuElement>, function: @Composable () -> Unit) {
    Box(modifier = Modifier.contextMenu(items), propagateMinConstraints = true) {
        function()
    }
}

@Composable
fun DropdownMenu(items: () -> List<ContextMenuElement>, function: @Composable () -> Unit) {
    Box(modifier = Modifier.dropdownMenu(items), propagateMinConstraints = true) {
        function()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.contextMenu(items: () -> List<ContextMenuElement>): Modifier {
    val (lastMouseEventState, setLastMouseEventState) = remember { mutableStateOf<MouseEvent?>(null) }

    val modifier = this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val lastMouseEvent = awaitFirstDownEvent()
                val mouseEvent = lastMouseEvent.awtEventOrNull

                if (mouseEvent != null) {

                    if (lastMouseEvent.button.isSecondary) {
                        lastMouseEvent.changes.forEach { it.consume() }

                        val currentCheck = System.currentTimeMillis()
                        if (lastCheck != 0L && currentCheck - lastCheck < MIN_TIME_BETWEEN_POPUPS_IN_MS) {
                            println("Popup ignored!")
                        } else {
                            lastCheck = currentCheck

                            setLastMouseEventState(mouseEvent)
                        }
                    }
                }
            }
        }
    }

    if (lastMouseEventState != null) {
        DisableSelection {
            showPopup(
                lastMouseEventState.x,
                lastMouseEventState.y,
                items(),
                onDismissRequest = { setLastMouseEventState(null) }
            )
        }
    }

    return modifier
}

@Composable
private fun Modifier.dropdownMenu(items: () -> List<ContextMenuElement>): Modifier {
    val (isClicked, setIsClicked) = remember { mutableStateOf(false) }
    val (offset, setOffset) = remember { mutableStateOf<Offset?>(null) }
    val mod = this
        .onGloballyPositioned { layoutCoordinates ->
            val offsetToRoot = layoutCoordinates.localToRoot(Offset.Zero)
            val offsetToBottomOfComponent = offsetToRoot.copy(y = offsetToRoot.y + layoutCoordinates.size.height)
            setOffset(offsetToBottomOfComponent)
        }
        .handMouseClickable {
            setIsClicked(true)
        }

    if (offset != null && isClicked) {
        showPopup(
            offset.x.toInt(),
            offset.y.toInt(),
            items(),
            onDismissRequest = { setIsClicked(false) })
    }

    return mod
}

@Composable
fun showPopup(x: Int, y: Int, contextMenuElements: List<ContextMenuElement>, onDismissRequest: () -> Unit) {
    Popup(
        properties = PopupProperties(
            focusable = true,
        ),
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
                .shadow(8.dp)
                .clip(RoundedCornerShape(BORDER_RADIUS.dp))
                .background(MaterialTheme.colors.background)
                .width(IntrinsicSize.Max)
                .widthIn(min = 180.dp)
                .run {
                    if (MaterialTheme.colors.isDark) {
                        this.border(
                            2.dp,
                            MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(BORDER_RADIUS.dp)
                        )
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
                    tint = (MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.8f))
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

sealed class ContextMenuElement(
    label: String,
    onClick: () -> Unit = {}
) : ContextMenuItem(label, onClick) {
    class ContextTextEntry(
        label: String,
        val icon: @Composable (() -> Painter)? = null,
        onClick: () -> Unit = {}
    ) : ContextMenuElement(label, onClick)

    object ContextSeparator : ContextMenuElement("", {})
}


@ExperimentalFoundationApi
class AppPopupMenu : TextContextMenu {
    @Composable
    override fun Area(
        textManager: TextContextMenu.TextManager,
        state: ContextMenuState,
        content: @Composable () -> Unit
    ) {
        val localization = LocalLocalization.current
        val items = {
            listOfNotNull(
                textManager.copy?.let {
                    ContextMenuElement.ContextTextEntry(
                        label = localization.copy,
                        icon = { painterResource(AppIcons.COPY) },
                        onClick = it
                    )
                },
                textManager.cut?.let {
                    ContextMenuElement.ContextTextEntry(
                        label = localization.cut,
                        icon = { painterResource(AppIcons.CUT) },
                        onClick = it
                    )
                },
                textManager.paste?.let {
                    ContextMenuElement.ContextTextEntry(
                        label = localization.paste,
                        icon = { painterResource(AppIcons.PASTE) },
                        onClick = it
                    )
                },
                textManager.selectAll?.let {
                    ContextMenuElement.ContextTextEntry(
                        label = localization.selectAll,
                        icon = null,
                        onClick = it
                    )
                },
            )
        }
        CompositionLocalProvider(
            LocalContextMenuRepresentation provides AppContextMenuRepresentation()
        ) {
            ContextMenuArea(items, state, content = content)
        }

    }
}

class AppContextMenuRepresentation : ContextMenuRepresentation {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        LightDefaultContextMenuRepresentation
        val status = state.status
        if (status is ContextMenuState.Status.Open) {
            var focusManager: FocusManager? by mutableStateOf(null)
            var inputModeManager: InputModeManager? by mutableStateOf(null)

            Popup(
                properties = PopupProperties(
                    focusable = true,
                ),
                onDismissRequest = { state.status = ContextMenuState.Status.Closed },
                popupPositionProvider = rememberPopupPositionProviderAtPosition(
                    positionPx = status.rect.center
                ),
                onKeyEvent = {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key.nativeKeyCode) {
                            KeyEvent.VK_ESCAPE -> {
                                state.status = ContextMenuState.Status.Closed
                                true
                            }

                            KeyEvent.VK_DOWN -> {
                                inputModeManager?.requestInputMode(InputMode.Keyboard)
                                focusManager?.moveFocus(FocusDirection.Next)
                                true
                            }

                            KeyEvent.VK_UP -> {
                                inputModeManager?.requestInputMode(InputMode.Keyboard)
                                focusManager?.moveFocus(FocusDirection.Previous)
                                true
                            }

                            else -> false
                        }
                    } else {
                        false
                    }
                },
            ) {
                focusManager = LocalFocusManager.current
                inputModeManager = LocalInputModeManager.current

                val border = if (MaterialTheme.colors.isDark) {
                    BorderStroke(2.dp, MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.2f))
                } else
                    null

                Column(
                    modifier = Modifier
                        .shadow(8.dp)
                        .clip(RoundedCornerShape(BORDER_RADIUS.dp))
                        .background(MaterialTheme.colors.background)
                        .width(IntrinsicSize.Max)
                        .widthIn(min = 180.dp)
                        .verticalScroll(rememberScrollState())
                        .run {
                            if (border != null)
                                border(border, RoundedCornerShape(BORDER_RADIUS.dp))
                            else
                                this
                        }

                ) {
                    items().forEach { item ->
                        when (item) {
                            is ContextMenuElement.ContextTextEntry -> TextEntry(
                                contextTextEntry = item,
                                onDismissRequest = { state.status = ContextMenuState.Status.Closed }
                            )

                            is ContextMenuElement.ContextSeparator -> Separator()
                        }
                    }
                }
            }
        }
    }
}