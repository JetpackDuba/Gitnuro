package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.SideMenuPanel
import com.jetpackduba.gitnuro.ui.components.SideMenuSubentry
import com.jetpackduba.gitnuro.ui.components.Tooltip
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.submoduleContextMenuItems
import com.jetpackduba.gitnuro.viewmodels.SubmodulesViewModel
import org.eclipse.jgit.submodule.SubmoduleStatus

@Composable
fun Submodules(
    submodulesViewModel: SubmodulesViewModel = gitnuroViewModel(),
) {
    val submodules by submodulesViewModel.submodules.collectAsState()
    val isExpanded by submodulesViewModel.isExpanded.collectAsState()

    SideMenuPanel(
        title = "Submodules",
        icon = painterResource("topic.svg"),
        items = submodules,
        isExpanded = isExpanded,
        onExpand = { submodulesViewModel.onExpand() },
        itemContent = { submodule ->
            SubmoduleLineEntry(
                submodulePair = submodule,
                onInitializeModule = { submodulesViewModel.initializeSubmodule(submodule.first) }
            )
        }
    )
}

@Composable
private fun SubmoduleLineEntry(
    submodulePair: Pair<String, SubmoduleStatus>,
    onInitializeModule: () -> Unit,
) {
    ContextMenu(
        items = {
            submoduleContextMenuItems(
                submodulePair.second,
                onInitializeModule = onInitializeModule
            )
        }
    ) {
        SideMenuSubentry(
            text = submodulePair.first,
            iconResourcePath = "topic.svg",
        ) {
            val stateName = submodulePair.second.type.toString()
            Tooltip(stateName) {
                Text(
                    text = stateName.first().toString(),
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}