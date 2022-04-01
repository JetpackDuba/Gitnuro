import app.extensions.runCommand
import app.viewmodels.TabViewModel
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager


fun openDirectoryDialog(): String? {
    val os = System.getProperty("os.name")
    var dirToOpen: String? = null

    if (os.lowercase() == "linux") {
        val checkZenityInstalled = runCommand("which zenity 2>/dev/null")
        val isZenityInstalled = !checkZenityInstalled.isNullOrEmpty()

        if (isZenityInstalled) {
            val openDirectory = runCommand(
                "zenity --file-selection --title=Open --directory"
            )?.replace("\n", "")

            if (!openDirectory.isNullOrEmpty())
                dirToOpen = openDirectory
        } else
            dirToOpen = openJvmDialog("", true)
    } else {
        dirToOpen = openJvmDialog("", false)
    }

    return dirToOpen
}
fun openRepositoryDialog(tabViewModel: TabViewModel) {
    val os = System.getProperty("os.name")
    val appStateManager = tabViewModel.appStateManager
    val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath
    var dirToOpen: String? = null

    if (os.lowercase() == "linux") {
        val checkZenityInstalled = runCommand("which zenity 2>/dev/null")
        val isZenityInstalled = !checkZenityInstalled.isNullOrEmpty()

        if (isZenityInstalled) {
            val openDirectory = runCommand(
                "zenity --file-selection --title=Open --directory --filename=\"$latestDirectoryOpened\""
            )?.replace("\n", "")

            if (!openDirectory.isNullOrEmpty())
                dirToOpen = openDirectory
        } else
            dirToOpen = openJvmDialog(latestDirectoryOpened, true)
    } else {
        dirToOpen = openJvmDialog(latestDirectoryOpened, false)
    }

    if(dirToOpen != null)
        tabViewModel.openRepository(dirToOpen)
}

private fun openJvmDialog(
    latestDirectoryOpened: String,
    isLinux: Boolean,
) : String? {
    if (!isLinux) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }

    val fileChooser = if (latestDirectoryOpened.isEmpty())
        JFileChooser()
    else
        JFileChooser(latestDirectoryOpened)

    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.showSaveDialog(null)

    return if (fileChooser.selectedFile != null)
        fileChooser.selectedFile.absolutePath
    else 
        null
}