package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.usecases.AddSubmoduleUseCase
import com.jetpackduba.gitnuro.domain.usecases.GetWorktreeUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.transport.URIish
import java.io.File
import javax.inject.Inject

class SubmoduleDialogViewModel @Inject constructor(
    private val addSubmoduleUseCase: AddSubmoduleUseCase,
    private val getWorktreeUseCase: GetWorktreeUseCase,
) : TabViewModel() {
    private val _error = MutableStateFlow("")
    val error = _error.asStateFlow()

    private val _onDataIsValid = MutableSharedFlow<Unit>()
    val onDataIsValid = _onDataIsValid.asSharedFlow()

    fun verifyData(
        repositoryUrl: String,
        directoryPath: String,
    ) = viewModelScope.launch {
        val workTree = getWorktreeUseCase().okOrNull()

        if (workTree == null) {
            _error.value = "Worktree not found"
            return@launch
        }

        val message = when {
            directoryPath.isBlank() -> "Invalid empty directory"
            repositoryUrl.isBlank() -> "Invalid empty URL"
            else -> null
        }

        if (message != null) {
            _error.value = message
            return@launch
        }

        try {
            URIish(repositoryUrl)
        } catch (ex: Exception) {
            _error.value = "${ex.message.orEmpty()}. Check your repository URL and try again."
            return@launch
        }

        val directory = File(workTree, directoryPath)

        if (directory.exists() && (directory.listFiles()?.count() ?: 0) > 0) {
            _error.value = "Directory $directoryPath contains files. Try again with a different name."
            return@launch
        }

        _onDataIsValid.emit(Unit)
    }

    fun createSubmodule(submodulePath: String, directory: String) = addSubmoduleUseCase(
        name = directory,
        path = directory,
        uri = submodulePath
    )

    fun resetError() {
        _error.value = ""
    }

}