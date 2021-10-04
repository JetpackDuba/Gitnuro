import git.GitManager
import javax.swing.JFileChooser

fun openRepositoryDialog(gitManager: GitManager) {
    val latestDirectoryOpened = gitManager.latestDirectoryOpened

    val f = if (latestDirectoryOpened == null)
        JFileChooser()
    else
        JFileChooser(latestDirectoryOpened)

    f.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    f.showSaveDialog(null)

    if (f.selectedFile != null)
        gitManager.openRepository(f.selectedFile)
}