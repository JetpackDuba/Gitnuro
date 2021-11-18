package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.simpleName
import app.ui.components.ScrollableLazyColumn
import app.git.GitManager
import app.git.StashStatus
import org.eclipse.jgit.revwalk.RevCommit
import app.theme.headerBackground
import app.theme.headerText
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import org.eclipse.jgit.lib.Ref

@Composable
fun Tags(gitManager: GitManager) {
    val tagsState = gitManager.tags.collectAsState()
    val tags = tagsState.value


    Column {
        SideMenuEntry(
            text = "Tags",
        )

        val branchesHeight = tags.count() * 40
        val maxHeight = if (branchesHeight < 300)
            branchesHeight
        else
            300

        Box(modifier = Modifier.heightIn(max = maxHeight.dp)) {
            ScrollableLazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items = tags) { tag ->
                    TagRow(
                        tag = tag,
                    )

                }
            }
        }
    }

}

@Composable
private fun TagRow(tag: Ref) {
    SideMenuSubentry(
        text = tag.simpleName,
        iconResourcePath = "tag.svg",
    )
}