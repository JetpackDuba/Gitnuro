package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.submodule.SubmoduleStatusType
import javax.inject.Inject

class SubmodulesManager @Inject constructor() {
    suspend fun uninitializedSubmodules(git: Git) = withContext(Dispatchers.IO) {
        return@withContext git
            .submoduleStatus()
            .call()
            .filter {
                it.value.type == SubmoduleStatusType.UNINITIALIZED
            }
    }
}