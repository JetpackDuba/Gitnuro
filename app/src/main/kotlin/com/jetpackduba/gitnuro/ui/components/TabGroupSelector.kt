package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.theme.dialogOverlay
import com.jetpackduba.gitnuro.ui.dialogs.base.SingleTextFieldDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TabGroupSelector(
    groups: List<String>,
    activeGroup: String,
    onSwitchGroup: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (old: String, new: String) -> Unit,
    onDeleteGroup: (String) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
        TextButton(
            onClick = { showMenu = true },
            modifier = Modifier.handOnHover(),
        ) {
            Text(
                text = activeGroup,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 160.dp),
            )

            Icon(
                painter = painterResource(Res.drawable.dropdown),
                contentDescription = null,
                tint = MaterialTheme.colors.onBackground,
            )
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            for (group in groups) {
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        onSwitchGroup(group)
                    },
                ) {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.body2,
                        fontWeight = if (group == activeGroup) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )

                    val isActive = group == activeGroup

                    IconButton(
                        onClick = {
                            showMenu = false
                            if (isActive) renaming = group else onDeleteGroup(group)
                        },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            painter = painterResource(if (isActive) Res.drawable.edit else Res.drawable.delete),
                            contentDescription = stringResource(
                                if (isActive) Res.string.tab_groups_rename_group else Res.string.tab_groups_delete_group
                            ),
                        )
                    }
                }
            }

            Divider()

            DropdownMenuItem(
                onClick = {
                    showMenu = false
                    creating = true
                },
            ) {
                Text(stringResource(Res.string.tab_groups_new_group), style = MaterialTheme.typography.body2)
            }
        }
    }

    if (creating || renaming != null) {
        var name by remember { mutableStateOf(renaming.orEmpty()) }
        val trimmed = name.trim()
        val onClose = {
            creating = false
            renaming = null
        }

        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                scrimColor = MaterialTheme.colors.dialogOverlay,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        ) {
            SingleTextFieldDialog(
                icon = painterResource(Res.drawable.folder),
                title = stringResource(
                    if (renaming != null) Res.string.tab_groups_rename_group else Res.string.tab_groups_new_group_title
                ),
                subtitle = stringResource(Res.string.tab_groups_dialog_subtitle),
                value = name,
                onValueChange = { name = it },
                isPrimaryActionEnabled = trimmed.isNotEmpty() && trimmed !in groups,
                primaryActionText = stringResource(
                    if (renaming != null) Res.string.tab_groups_dialog_rename else Res.string.tab_groups_dialog_create
                ),
                onDismiss = onClose,
                onPrimaryActionClicked = {
                    renaming?.let { onRenameGroup(it, trimmed) } ?: onCreateGroup(trimmed)
                    onClose()
                },
            )
        }
    }
}
