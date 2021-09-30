import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import theme.GitnuroTheme
import javax.swing.JFileChooser


@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    var isOpen by remember { mutableStateOf(true) }
    val gitManager = GitManager()
    if (isOpen) {
        Window(
            title = "Gitnuro",
            onCloseRequest = {
                isOpen = false
            },
            state = rememberWindowState(placement = WindowPlacement.Maximized, size = WindowSize(1280.dp, 720.dp))
            ) {
            GitnuroTheme {
                Gitnuro(gitManager)
            }
        }
    }
}

@Composable
fun Gitnuro(gitManager: GitManager) {
    val repositorySelectionStatus by gitManager.repositorySelectionStatus.collectAsState()

    Column(
        modifier = Modifier.background(MaterialTheme.colors.background)
    ) {
        GMenu(
            onRepositoryOpen = {
                val latestDirectoryOpened = gitManager.latestDirectoryOpened

                val f = if (latestDirectoryOpened == null)
                    JFileChooser()
                else
                    JFileChooser(latestDirectoryOpened)

                f.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                f.showSaveDialog(null)

                if (f.selectedFile != null)
                    gitManager.openRepository(f.selectedFile)
            },
            onPull = { gitManager.pull() },
            onPush = { gitManager.push() },
            onStash = { gitManager.stash() },
            onPopStash = { gitManager.popStash() },
        )

        Crossfade(targetState = repositorySelectionStatus) {

            @Suppress("UnnecessaryVariable") // Don't inline it because smart cast won't work
            when (val status = repositorySelectionStatus) {
                RepositorySelectionStatus.None -> {
                    NoneRepository()
                }
                RepositorySelectionStatus.Loading -> {
                    LoadingRepository()
                }
                is RepositorySelectionStatus.Open -> {
                    RepositorySelected(gitManager = gitManager, repository = status.repository)
                }
            }
        }
    }


}

@Composable
fun LoadingRepository() {
    Box { }

}

@Composable
fun NoneRepository() {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Text("Open a repository to start using Gitnuro")
    }
}

@Composable
fun GMenu(
    onRepositoryOpen: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onStash: () -> Unit,
    onPopStash: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(
            onClick = onRepositoryOpen
        ) {
            Text("Open")
        }
        OutlinedButton(
            onClick = onPull
        ) {
            Text("Pull")
        }
        OutlinedButton(
            onClick = onPush
        ) {
            Text("Push")
        }
        OutlinedButton(
            onClick = onStash
        ) {
            Text("Stash")
        }
        OutlinedButton(
            onClick = onPopStash
        ) {
            Text("Pop stash")
        }
    }
}