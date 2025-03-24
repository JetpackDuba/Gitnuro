package com.jetpackduba.gitnuro.system

import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.managers.ShellManager
import java.awt.FileDialog
import javax.inject.Inject
import javax.swing.JFileChooser
import javax.swing.UIManager

private const val TAG = "SystemDialogs"

/**
 * Shows a picker dialog to select a file or directory
 */
class OpenFilePickerUseCase @Inject constructor(
    /**
     * We want specifically [ShellManager] implementation and not [com.jetpackduba.gitnuro.managers.IShellManager],
     * to run commands without any modification
     * (such as ones done by [com.jetpackduba.gitnuro.managers.FlatpakShellManager], because it has to run in the sandbox)
     */
    private val shellManager: ShellManager,
) {
    operator fun invoke(pickerType: PickerType, basePath: String?): String? {
        val isLinux = currentOs.isLinux()
        val isMac = currentOs.isMac()

        return if (isLinux) {
            openDirectoryDialogLinux(pickerType)
        } else
            openJvmDialog(pickerType, basePath, false, isMac)
    }

    private fun openDirectoryDialogLinux(pickerType: PickerType): String? {
        var dirToOpen: String? = null

        val checkZenityInstalled = shellManager.runCommand(listOf("which", "zenity", "2>/dev/null"))
        val isZenityInstalled = !checkZenityInstalled.isNullOrEmpty()

        printLog(TAG, "IsZenityInstalled $isZenityInstalled")

        if (isZenityInstalled) {
            val command = when (pickerType) {
                PickerType.FILES -> listOf(
                    "zenity",
                    "--file-selection",
                    "--title=Open"
                )

                PickerType.DIRECTORIES -> listOf("zenity", "--file-selection", "--title=Open", "--directory")
            }

            val openDirectory = shellManager.runCommand(command)?.replace("\n", "")

            if (!openDirectory.isNullOrEmpty())
                dirToOpen = openDirectory
        } else
            dirToOpen = openJvmDialog(pickerType, "", isLinux = true, isMac = false)

        return dirToOpen
    }

    private fun openJvmDialog(
        pickerType: PickerType,
        basePath: String?,
        isLinux: Boolean,
        isMac: Boolean,
    ): String? {
        if (!isLinux) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }

        if (isMac) {
            if (pickerType == PickerType.DIRECTORIES) {
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
            }

            val fileChooser = if (basePath.isNullOrEmpty()) {
                FileDialog(null as java.awt.Frame?, "Open", FileDialog.LOAD)
            } else {
                FileDialog(null as java.awt.Frame?, "Open", FileDialog.LOAD).apply {
                    directory = basePath
                }
            }

            fileChooser.isMultipleMode = false
            fileChooser.isVisible = true

            System.setProperty("apple.awt.fileDialogForDirectories", "false")

            if (fileChooser.file != null && fileChooser.directory != null) {
                return fileChooser.directory + fileChooser.file
            }

            return null
        } else {
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
    }
}

enum class PickerType(val value: Int) {
    FILES(JFileChooser.FILES_ONLY),
    DIRECTORIES(JFileChooser.DIRECTORIES_ONLY);
}