import app.extensions.runCommand
import app.viewmodels.TabViewModel
import javax.swing.JFileChooser
import javax.swing.UIManager


fun openRepositoryDialog(gitManager: TabViewModel) {
    val os = System.getProperty("os.name")
    val appStateManager = gitManager.appStateManager
    val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath


    if (os.lowercase() == "linux") {
        val checkZenityInstalled = runCommand("which zenity 2>/dev/null")
        val isZenityInstalled = !checkZenityInstalled.isNullOrEmpty()

        if (isZenityInstalled) {
            val openDirectory = runCommand(
                "zenity --file-selection --title=Open --directory --filename=\"$latestDirectoryOpened\""
            )?.replace("\n", "")

            if (!openDirectory.isNullOrEmpty())
                gitManager.openRepository(openDirectory)
        } else
            openRepositoryDialog(gitManager, latestDirectoryOpened)
    } else {
        openRepositoryDialog(gitManager, latestDirectoryOpened)
    }

}

private fun openRepositoryDialog(
    tabViewModel: TabViewModel,
    latestDirectoryOpened: String
) {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val fileChooser = if (latestDirectoryOpened.isEmpty())
        JFileChooser()
    else
        JFileChooser(latestDirectoryOpened)

    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.showSaveDialog(null)

    if (fileChooser.selectedFile != null)
        tabViewModel.openRepository(fileChooser.selectedFile)
}