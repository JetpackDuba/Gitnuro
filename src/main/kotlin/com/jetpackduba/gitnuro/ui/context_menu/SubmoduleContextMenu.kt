package com.jetpackduba.gitnuro.ui.context_menu

import org.eclipse.jgit.submodule.SubmoduleStatus
import org.eclipse.jgit.submodule.SubmoduleStatusType

fun submoduleContextMenuItems(
    submoduleStatus: SubmoduleStatus,
    onInitializeModule: () -> Unit,
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
    }
}