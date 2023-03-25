package com.jetpackduba.gitnuro.ui.context_menu

import org.eclipse.jgit.submodule.SubmoduleStatus
import org.eclipse.jgit.submodule.SubmoduleStatusType

fun submoduleContextMenuItems(
    submoduleStatus: SubmoduleStatus,
    onInitializeModule: () -> Unit,
    onOpenSubmoduleInTab: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        if (submoduleStatus.type == SubmoduleStatusType.UNINITIALIZED) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Initialize submodule",
                    onClick = onInitializeModule
                )
            )
        }
        if (submoduleStatus.type != SubmoduleStatusType.UNINITIALIZED) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Open submodule in new tab",
                    onClick = onOpenSubmoduleInTab
                )
            )
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Sync submodule",
                    onClick = onInitializeModule
                )
            )

            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Update submodule",
                    onClick = onInitializeModule
                )
            )

            add(
                ContextMenuElement.ContextTextEntry(
                    label = "DeInitialize submodule",
                    onClick = onInitializeModule
                )
            )
        }
    }
}