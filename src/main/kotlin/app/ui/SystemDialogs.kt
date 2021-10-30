import app.AppStateManager
import app.extensions.runCommand
import app.git.GitManager
import java.io.IOException
import java.util.*
import javax.swing.JFileChooser


fun openRepositoryDialog(gitManager: GitManager) {
    val os = System.getProperty("os.name")
    val appStateManager = gitManager.appStateManager
    val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath


    if(os.lowercase() == "linux") {
        val checkZenityInstalled = runCommand("which zenity 2>/dev/null")
        val isZenityInstalled = !checkZenityInstalled.isNullOrEmpty()

        if(isZenityInstalled) {
            val openDirectory = runCommand(
                "zenity --file-selection --title=Open --directory --filename=\"$latestDirectoryOpened\""
            )?.replace("\n", "")

            if(!openDirectory.isNullOrEmpty())
                gitManager.openRepository(openDirectory)
        } else
            openRepositoryDialog(gitManager, latestDirectoryOpened)
    } else {
        openRepositoryDialog(gitManager, latestDirectoryOpened)
    }

}

private fun openRepositoryDialog(
    gitManager: GitManager,
    latestDirectoryOpened: String
) {

    val fileChooser = if (latestDirectoryOpened.isEmpty())
        JFileChooser()
    else
        JFileChooser(latestDirectoryOpened)

    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.showSaveDialog(null)

    if (fileChooser.selectedFile != null)
        gitManager.openRepository(fileChooser.selectedFile)
}