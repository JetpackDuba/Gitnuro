package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.extensions.isValid
import com.jetpackduba.gitnuro.generated.resources.*
import org.eclipse.jgit.submodule.SubmoduleStatus
import org.eclipse.jgit.submodule.SubmoduleStatusType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun submoduleContextMenuItems(
    submoduleStatus: SubmoduleStatus,
    onInitializeSubmodule: () -> Unit,
    onSyncSubmodule: () -> Unit,
    onUpdateSubmodule: () -> Unit,
    onOpenSubmoduleInTab: () -> Unit,
    onDeleteSubmodule: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        if (submoduleStatus.type == SubmoduleStatusType.UNINITIALIZED) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.submodules_context_menu_initialize_submodule) },
                onClick = onInitializeSubmodule,
            )
        }
        if (submoduleStatus.type.isValid()) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.submodules_context_menu_open_submodule_in_tab) },
                icon = { painterResource(Res.drawable.open) },
                onClick = onOpenSubmoduleInTab,
            )
            addContextMenu(
                composableLabel = { stringResource(Res.string.submodules_context_menu_sync_submodule) },
                icon = { painterResource(Res.drawable.sync) },
                onClick = onSyncSubmodule,
            )

            addContextMenu(
                composableLabel = { stringResource(Res.string.submodules_context_menu_update_submodule) },
                icon = { painterResource(Res.drawable.update) },
                onClick = onUpdateSubmodule,
            )
        }

        if (isNotEmpty()) {
            add(
                ContextMenuElement.ContextSeparator,
            )
        }

        addContextMenu(
            composableLabel = { stringResource(Res.string.submodules_context_menu_delete_submodule) },
            icon = { painterResource(Res.drawable.delete) },
            onClick = onDeleteSubmodule,
        )
    }
}