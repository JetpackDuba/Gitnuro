package app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.DefaultAlpha
import app.DialogManager
import app.LoadingRepository
import app.git.GitManager
import app.git.RepositorySelectionStatus


@Composable
fun AppTab(gitManager: GitManager, dialogManager: DialogManager, repositoryPath: String?, tabName: MutableState<String>) {
    LaunchedEffect(gitManager) {
        if (repositoryPath != null)
            gitManager.openRepository(repositoryPath)
    }


    val repositorySelectionStatus by gitManager.repositorySelectionStatus.collectAsState()
    val isProcessing by gitManager.processing.collectAsState()

    if (repositorySelectionStatus is RepositorySelectionStatus.Open) {
        tabName.value = gitManager.repositoryName
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
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
}