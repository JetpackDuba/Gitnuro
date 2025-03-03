package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.backgroundIf
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.code
import com.jetpackduba.gitnuro.generated.resources.download
import com.jetpackduba.gitnuro.generated.resources.refresh
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.backgroundSelected
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun QuickActionsDialog(
    onClose: () -> Unit,
    onAction: (QuickActionType) -> Unit,
) {

    val textFieldFocusRequester = remember { FocusRequester() }
    val items = remember {
        listOf(
            QuickAction(Res.drawable.code, "Open repository in file manager", QuickActionType.OPEN_DIR_IN_FILE_MANAGER),
            QuickAction(Res.drawable.download, "Clone new repository", QuickActionType.CLONE),
            QuickAction(Res.drawable.refresh, "Refresh repository data", QuickActionType.REFRESH),
            QuickAction(Res.drawable.sign, "Signoff config", QuickActionType.SIGN_OFF),
        )
    }

    var searchFilter by remember { mutableStateOf("") }

    val filteredItems by remember(searchFilter) {
        derivedStateOf { items.filter { it.title.contains(searchFilter, ignoreCase = true) } }
    }

    var selectedIndex by remember(filteredItems) {
        mutableStateOf(0)
    }

    MaterialDialog(
        onCloseRequested = onClose,
        background = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.width(680.dp)
                .height(400.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.DOWN)) {
                        if (selectedIndex < filteredItems.count() - 1)
                            selectedIndex++
                        true
                    } else if (keyEvent.matchesBinding(KeybindingOption.UP)) {
                        if (selectedIndex > 0)
                            selectedIndex--
                        true
                    } else if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT)) {
                        val item = filteredItems.getOrNull(selectedIndex)
                        if (item != null)
                            onAction(item.type)
                        true
                    } else
                        false
                }
        ) {
            AdjustableOutlinedTextField(
                value = searchFilter,
                hint = "Search for an action or press ESC to close the dialog", // TODO don't hardcode ESC here, fix keybinding toString
                onValueChange = { searchFilter = it },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .focusRequester(textFieldFocusRequester)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(filteredItems) { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .backgroundIf(selectedIndex == index, MaterialTheme.colors.backgroundSelected)
                            .handMouseClickable { onAction(item.type) }
                    ) {
                        Icon(
                            painterResource(item.icon),
                            contentDescription = null,
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                            tint = MaterialTheme.colors.onBackground,
                        )

                        Text(
                            item.title,
                            color = MaterialTheme.colors.onBackground,
                            style = MaterialTheme.typography.body1,
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                textFieldFocusRequester.requestFocus()
            }
        }
    }
}

data class QuickAction(val icon: DrawableResource, val title: String, val type: QuickActionType)

enum class QuickActionType {
    OPEN_DIR_IN_FILE_MANAGER,
    CLONE,
    REFRESH,
    SIGN_OFF
}