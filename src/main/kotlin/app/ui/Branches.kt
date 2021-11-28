package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ui.components.ScrollableLazyColumn
import app.extensions.simpleName
import app.git.GitManager
import org.eclipse.jgit.lib.Ref
import app.theme.headerBackground
import app.theme.headerText
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry

@Composable
fun Branches(gitManager: GitManager) {
    val branches by gitManager.branches.collectAsState()
    val currentBranch by gitManager.currentBranch.collectAsState()

    Column {
        SideMenuEntry("Branches")

        val branchesHeight = branches.count() * 40
        val maxHeight = if(branchesHeight < 300)
            branchesHeight
        else
            300

        Box(modifier = Modifier.heightIn(max = maxHeight.dp)) {
            ScrollableLazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(branches) { _, branch ->
                    BranchRow(
                        branch = branch,
                        isCurrentBranch = currentBranch == branch.name
                    )

                }
            }
        }
    }
}

@Composable
private fun BranchRow(
    branch: Ref,
    isCurrentBranch: Boolean
) {
    SideMenuSubentry(
        text = branch.simpleName,
        iconResourcePath = "branch.svg",
        bold = isCurrentBranch,
    ) {
        if (isCurrentBranch) {
            Icon(
                painter = painterResource("location.svg"),
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 4.dp),
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}