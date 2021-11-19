package app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.DialogManager
import app.LoadingRepository
import app.git.GitManager
import app.git.RepositorySelectionStatus
import app.theme.tabBackground
import app.ui.components.DialogBox
import kotlinx.coroutines.delay


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppTab(
    gitManager: GitManager,
    dialogManager: DialogManager,
    repositoryPath: String?,
    tabName: MutableState<String>
) {
    LaunchedEffect(gitManager) {
        if (repositoryPath != null)
            gitManager.openRepository(repositoryPath)
    }

    val errorManager = remember(gitManager) { // TODO Is remember here necessary?
        gitManager.errorsManager
    }

    val lastError by errorManager.lastError.collectAsState()

    var showError by remember {
        mutableStateOf(false)
    }

    if (lastError != null)
        LaunchedEffect(lastError) {
            showError = true
            delay(5000)
            showError = false
        }


    val repositorySelectionStatus by gitManager.repositorySelectionStatus.collectAsState()
    val isProcessing by gitManager.processing.collectAsState()

    if (repositorySelectionStatus is RepositorySelectionStatus.Open) {
        tabName.value = gitManager.repositoryName
    }

    Box {
        Column(
            modifier = Modifier
                .background(tabBackground)
                .fillMaxSize()
        ) {

            val linearProgressAlpha = if (isProcessing)
                DefaultAlpha
            else
                0f

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(linearProgressAlpha)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = repositorySelectionStatus) {

                    @Suppress("UnnecessaryVariable") // Don't inline it because smart cast won't work
                    when (repositorySelectionStatus) {
                        RepositorySelectionStatus.None -> {
                            WelcomePage(gitManager = gitManager)
                        }
                        RepositorySelectionStatus.Loading -> {
                            LoadingRepository()
                        }
                        is RepositorySelectionStatus.Open -> {
                            RepositoryOpenPage(gitManager = gitManager, dialogManager = dialogManager)
                        }
                    }
                }

                if (isProcessing)
                    Box(modifier = Modifier.fillMaxSize()) //TODO this should block of the mouse/keyboard events while visible
            }
        }

        val safeLastError = lastError
        if (safeLastError != null) {
            AnimatedVisibility(
                visible = showError,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 32.dp)
            ) {
                val interactionSource = remember { MutableInteractionSource() }

                Card(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 200.dp, minHeight = 100.dp)
                        .clickable(
                            enabled = true,
                            onClick = {},
                            interactionSource = interactionSource,
                            indication = null
                        ),
                    backgroundColor = MaterialTheme.colors.error,
                ) {
                    Column(
                        modifier = Modifier
                        .padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "Error",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 16.dp)
                        ) // TODO Add more  descriptive title

                        Text(
                            text = safeLastError.message,
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}