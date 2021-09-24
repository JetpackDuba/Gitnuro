import aeab13.github.com.GitManager
import aeab13.github.com.RepositorySelected
import aeab13.github.com.RepositorySelectionStatus
import aeab13.github.com.theme.GitnuroTheme
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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

    Column {
        GMenu(
            onRepositoryOpen = {
                val latestDirectoryOpened = gitManager.latestDirectoryOpened

                val f = if(latestDirectoryOpened == null)
                    JFileChooser()
                else
                    JFileChooser(latestDirectoryOpened)

                f.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                f.showSaveDialog(null)

                if (f.selectedFile != null)
                    gitManager.openRepository(f.selectedFile)
            }
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
fun GMenu(onRepositoryOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(
//            modifier = Modifier.size(64.dp),
            onClick = onRepositoryOpen
        ) {
            Text("Open")
//            Icon(Icons.Default.Add, contentDescription = "Open repository")
        }
    }
}