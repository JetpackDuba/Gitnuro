package com.jetpackduba.gitnuro.editor

import com.jetpackduba.gitnuro.managers.ErrorsManager
import com.jetpackduba.gitnuro.managers.IShellManager
import com.jetpackduba.gitnuro.models.errorNotification
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import javax.inject.Inject

/**
 * Use-case for opening the repository directory with an external editor.
 */
class OpenRepositoryInEditorUseCase @Inject constructor(
    private val settings: AppSettingsRepository,
    private val shellManager: IShellManager,
    private val errorsManager: ErrorsManager
) {
    /**
     * Opens the given path in the configured external editor.
     * @param path The path to open. Should be a directory.
     */
    suspend operator fun invoke(path: String) {
        val editor = settings.editor;
        val isSet = editor.isNotEmpty();

        if (!isSet) {
            errorsManager.emitNotification(
                errorNotification("No editor configured")
            )
            return
        }

        shellManager.runCommandInPath(listOf(editor, path), path)
    }
}
