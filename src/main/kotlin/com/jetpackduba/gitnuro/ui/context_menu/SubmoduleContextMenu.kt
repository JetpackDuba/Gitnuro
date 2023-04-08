package com.jetpackduba.gitnuro.ui.context_menu

import org.eclipse.jgit.submodule.SubmoduleStatus
import org.eclipse.jgit.submodule.SubmoduleStatusType

fun submoduleContextMenuItems(
    submoduleStatus: SubmoduleStatus,
    onInitializeSubmodule: () -> Unit,
    onDeinitializeSubmodule: () -> Unit,
    onSyncSubmodule: () -> Unit,
    onUpdateSubmodule: () -> Unit,
    onOpenSubmoduleInTab: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        if (submoduleStatus.type == SubmoduleStatusType.UNINITIALIZED) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Initialize submodule",
                    onClick = onInitializeSubmodule,
                )
            )
        }
        if (submoduleStatus.type != SubmoduleStatusType.UNINITIALIZED) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Open submodule in new tab",
                    onClick = onOpenSubmoduleInTab,
                )
            )
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Sync submodule",
                    onClick = onSyncSubmodule,
                )
            )

            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Update submodule",
                    onClick = onUpdateSubmodule,
                )
            )

            add(
                ContextMenuElement.ContextTextEntry(
                    label = "DeInitialize submodule",
                    onClick = onDeinitializeSubmodule,
                )
            )
        }
    }
}