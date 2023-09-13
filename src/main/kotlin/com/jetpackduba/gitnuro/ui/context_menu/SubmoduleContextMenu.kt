package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.isValid
import org.eclipse.jgit.submodule.SubmoduleStatus
import org.eclipse.jgit.submodule.SubmoduleStatusType

fun submoduleContextMenuItems(
    submoduleStatus: SubmoduleStatus,
    onInitializeSubmodule: () -> Unit,
//    onDeinitializeSubmodule: () -> Unit,
    onSyncSubmodule: () -> Unit,
    onUpdateSubmodule: () -> Unit,
    onOpenSubmoduleInTab: () -> Unit,
    onDeleteSubmodule: () -> Unit,
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
        if (submoduleStatus.type.isValid()) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Open submodule in new tab",
                    icon = { painterResource(AppIcons.OPEN) },
                    onClick = onOpenSubmoduleInTab,
                )
            )
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Sync submodule",
                    icon = { painterResource(AppIcons.SYNC) },
                    onClick = onSyncSubmodule,
                )
            )

            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Update submodule",
                    icon = { painterResource(AppIcons.UPDATE) },
                    onClick = onUpdateSubmodule,
                )
            )
// TODO This crashes with a NPE in jgit even when providing correct arguments, feature removed for now
//            add(
//                ContextMenuElement.ContextTextEntry(
//                    label = "DeInitialize submodule",
//                    onClick = onDeinitializeSubmodule,
//                )
//            )
        }

        if(isNotEmpty()) {
            add(
                ContextMenuElement.ContextSeparator,
            )
        }

        add(
            ContextMenuElement.ContextTextEntry(
                label = "Delete submodule",
                icon = { painterResource(AppIcons.DELETE) },
                onClick = onDeleteSubmodule,
            ),
        )
    }
}