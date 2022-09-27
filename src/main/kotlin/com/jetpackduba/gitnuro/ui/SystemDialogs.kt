package com.jetpackduba.gitnuro.ui

import com.jetpackduba.gitnuro.extensions.getCurrentOs
import com.jetpackduba.gitnuro.extensions.runCommand
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import javax.swing.JFileChooser
import javax.swing.UIManager

private const val TAG = "SystemDialogs"

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
    val os = getCurrentOs()
    val isLinux = os.isLinux()

    return if (isLinux) {
        openDirectoryDialogLinux(pickerType)
    } else
        openJvmDialog(pickerType, basePath, false)
}

enum class PickerType(val value: Int) {
    FILES(JFileChooser.FILES_ONLY),
    DIRECTORIES(JFileChooser.DIRECTORIES_ONLY),
    FILES_AND_DIRECTORIES(JFileChooser.FILES_AND_DIRECTORIES);
}


fun openDirectoryDialogLinux(pickerType: PickerType): String? {
    var dirToOpen: String? = null

    val checkZenityInstalled = runCommand("which zenity 2>/dev/null")
    val isZenityInstalled = !checkZenityInstalled.isNullOrEmpty()

    printLog(TAG, "IsZenityInstalled $isZenityInstalled")

    if (isZenityInstalled) {

        val command = when (pickerType) {
            PickerType.FILES, PickerType.FILES_AND_DIRECTORIES -> "zenity --file-selection --title=Open"
            PickerType.DIRECTORIES -> "zenity --file-selection --title=Open --directory"
        }

        val openDirectory = runCommand(command)?.replace("\n", "")

        if (!openDirectory.isNullOrEmpty())
            dirToOpen = openDirectory
    } else
        dirToOpen = openJvmDialog(pickerType, "", true)

    return dirToOpen
}

private fun openJvmDialog(
    pickerType: PickerType,
    basePath: String?,
    isLinux: Boolean,
): String? {
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