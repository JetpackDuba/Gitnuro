package app.ui

import app.viewmodels.TabViewModel
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager


fun openDirectoryDialog(basePath: String? = null): String? {
    return openPickerDialog(
        pickerType = PickerType.DIRECTORIES,
        basePath = basePath,
    )
}

fun openFileDialog(basePath: String? = null): String? {
    return openPickerDialog(
        pickerType = PickerType.FILES,
        basePath = basePath,
    )
}

fun openRepositoryDialog(tabViewModel: TabViewModel) {
    val appStateManager = tabViewModel.appStateManager
    val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath

    val dirToOpen = openDirectoryDialog(latestDirectoryOpened)
    if (dirToOpen != null)
        tabViewModel.openRepository(dirToOpen)
}

private fun openPickerDialog(
    pickerType: PickerType,
    basePath: String?,
): String? {

    val os = System.getProperty("os.name")
    val isLinux = os.lowercase().contains("linux")

    if (!isLinux) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }

    val fileChooser = if (basePath.isNullOrEmpty())
        JFileChooser()
    else
        JFileChooser(basePath)

    fileChooser.fileSelectionMode = pickerType.value
    fileChooser.showOpenDialog(null)

    return if (fileChooser.selectedFile != null)
        fileChooser.selectedFile.absolutePath
    else
        null
}

enum class PickerType(val value: Int) {
    FILES(JFileChooser.FILES_ONLY),
    DIRECTORIES(JFileChooser.DIRECTORIES_ONLY),
    FILES_AND_DIRECTORIES(JFileChooser.FILES_AND_DIRECTORIES);
}