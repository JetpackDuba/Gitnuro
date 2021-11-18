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
import app.ui.components.ScrollableLazyColumn
import app.git.GitManager
import app.git.StashStatus
import org.eclipse.jgit.revwalk.RevCommit
import app.theme.headerBackground
import app.theme.headerText
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry

@Composable
fun Stashes(gitManager: GitManager) {
    val stashStatusState = gitManager.stashStatus.collectAsState()
    val stashStatus = stashStatusState.value

    val stashList = if (stashStatus is StashStatus.Loaded)
        stashStatus.stashes
    else
        listOf()


    Column {
        SideMenuEntry(
            text = "Stashes",
        )

        ScrollableLazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(items = stashList) { stash ->
                StashRow(
                    stash = stash,
                )

            }
        }
    }

}

@Composable
private fun StashRow(stash: RevCommit) {
    SideMenuSubentry(
        text = stash.name,
        iconResourcePath = "stash.svg",
    )
}