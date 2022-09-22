package app.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.theme.secondaryTextColor
import app.ui.components.SideMenuPanel
import app.ui.components.SideMenuSubentry
import app.ui.components.Tooltip
import app.ui.context_menu.submoduleContextMenuItems
import app.viewmodels.SubmodulesViewModel
import org.eclipse.jgit.submodule.SubmoduleStatus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Submodules(
    submodulesViewModel: SubmodulesViewModel,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubmoduleLineEntry(
    submodulePair: Pair<String, SubmoduleStatus>,
    onInitializeModule: () -> Unit,
) {
    ContextMenuArea(
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
                    color = MaterialTheme.colors.secondaryTextColor,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}