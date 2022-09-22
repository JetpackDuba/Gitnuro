package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import org.eclipse.jgit.submodule.SubmoduleStatus
import org.eclipse.jgit.submodule.SubmoduleStatusType

@OptIn(ExperimentalFoundationApi::class)
fun submoduleContextMenuItems(
    submoduleStatus: SubmoduleStatus,
    onInitializeModule: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf<ContextMenuItem>().apply {
        if (submoduleStatus.type == SubmoduleStatusType.UNINITIALIZED) {
            add(
                ContextMenuItem(
                    label = "Initialize submodule",
                    onClick = onInitializeModule
                )
            )
        }
    }
}