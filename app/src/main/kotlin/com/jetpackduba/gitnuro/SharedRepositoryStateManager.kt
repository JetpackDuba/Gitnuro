package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseInteractiveStateGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

private const val TAG = "SharedRepositoryStateMa"

@TabScope
class SharedRepositoryStateManager @Inject constructor() {
    private val _repositoryState = MutableStateFlow(RepositoryState.SAFE)
    val repositoryState = _repositoryState.asStateFlow()

    private val _rebaseInteractiveState = MutableStateFlow<RebaseInteractiveState>(RebaseInteractiveState.None)
    val rebaseInteractiveState = _rebaseInteractiveState.asStateFlow()

}