import app.git.GitManager
import javax.swing.JFileChooser

fun openRepositoryDialog(gitManager: GitManager) {
    val latestDirectoryOpened = gitManager.latestOpenedRepositoryPath

    val fileChooser = if (latestDirectoryOpened.isEmpty())
        JFileChooser()
    else
        JFileChooser(latestDirectoryOpened)

    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.showSaveDialog(null)

    if (fileChooser.selectedFile != null)
        gitManager.openRepository(fileChooser.selectedFile)
}