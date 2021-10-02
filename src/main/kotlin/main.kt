import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import theme.*
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.JFileChooser
import javax.swing.UIManager


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
        MenuButton(
            title = "Open",
            icon = painterResource("open.svg"),
            onClick = onRepositoryOpen
        )
        MenuButton(
            title = "Pull",
            icon = painterResource("download.svg"),
            onClick = onPull,
        )
        MenuButton(
            title = "Push",
            icon = painterResource("upload.svg"),
            onClick = onPush,
        )
        MenuButton(
            title = "Stash",
            icon = painterResource("stash.svg"),
            onClick = onStash,
        )
        MenuButton(
            title = "Apply",
            icon = painterResource("apply_stash.svg"),
            onClick = onPopStash,
        )
    }
}

@Composable
fun MenuButton(modifier: Modifier = Modifier, title: String, icon: Painter, onClick: () -> Unit) {

    var hovering by remember { mutableStateOf(false) }

    val backgroundColor = if(hovering)
        MaterialTheme.colors.primary.copy(alpha = 0.15F)
    else
        MaterialTheme.colors.primary.copy(alpha = 0F)

    TextButton(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .pointerMoveFilter(
                onEnter = {
                    hovering = true
                    false
                },
                onExit = {
                    hovering = false
                    false
                }
            )
        ,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = backgroundColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(4.dp)
                    .size(24.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colors.primaryTextColor
            )
        }

    }
}