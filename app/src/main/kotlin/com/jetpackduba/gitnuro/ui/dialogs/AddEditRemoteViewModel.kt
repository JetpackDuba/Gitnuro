package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.usecases.AddRemoteUseCase
import com.jetpackduba.gitnuro.domain.usecases.UpdateRemoteUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class AddEditRemoteViewModel @AssistedInject constructor(
    private val addRemoteUseCase: AddRemoteUseCase,
    private val updateRemoteUseCase: UpdateRemoteUseCase,
    @Assisted val remoteToEdit: Remote?,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(commit: Remote?): AddEditRemoteViewModel
    }

    private val _remote = MutableStateFlow(remoteToEdit ?: Remote("", "", ""))
    val remote = _remote.asStateFlow()

    val isNewRemote = remoteToEdit == null

    fun save() {
        if (remoteToEdit == null) {
            addRemoteUseCase(remote.value)
        } else {
            updateRemoteUseCase(remoteToEdit)
        }
    }

    fun updateRemoteName(name: String) {
        _remote.update { it.copy(name = name) }
    }

    fun updateAllUri(uri: String) {
        _remote.update { it.copy(fetchUri = uri, pushUri = uri) }
    }

    fun updateFetchUri(uri: String) {
        _remote.update { it.copy(fetchUri = uri) }
    }

    fun updatePushUri(uri: String) {
        _remote.update { it.copy(pushUri = uri) }
    }
}