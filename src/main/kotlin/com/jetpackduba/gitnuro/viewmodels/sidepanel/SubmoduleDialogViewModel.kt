package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.transport.URIish
import java.io.File
import javax.inject.Inject

class SubmoduleDialogViewModel @Inject constructor(
    private val tabState: TabState,
) {
    private val _error = MutableStateFlow("")
    val error = _error.asStateFlow()

    private val _onDataIsValid = MutableSharedFlow<Unit>()
    val onDataIsValid = _onDataIsValid.asSharedFlow()

    fun verifyData(
        repositoryUrl: String,
        directoryPath: String,
    ) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val message = when {
            directoryPath.isBlank() -> "Invalid empty directory"
            repositoryUrl.isBlank() -> "Invalid empty URL"
            else -> null
        }

        if (message != null) {
            _error.value = message
            return@runOperation
        }

        try {
            URIish(repositoryUrl)
        } catch (ex: Exception) {
            _error.value = "${ex.message.orEmpty()}. Check your repository URL and try again."
            return@runOperation
        }

        val directory = File(git.repository.workTree, directoryPath)

        if (directory.exists() && (directory.listFiles()?.count() ?: 0) > 0) {
            _error.value = "Directory $directoryPath contains files. Try again with a different name."
            return@runOperation
        }

        _onDataIsValid.emit(Unit)
    }

    fun resetError() {
        _error.value = ""
    }

}