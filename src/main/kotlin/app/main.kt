import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.zIndex
import app.Main
import app.di.DaggerAppComponent
import app.git.GitManager
import app.git.RepositorySelectionStatus
import app.theme.*
import app.ui.RepositoryOpenPage
import app.ui.WelcomePage
import app.ui.components.RepositoriesTabPanel
import app.ui.components.TabInformation

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val main = Main()
    main.app()
}