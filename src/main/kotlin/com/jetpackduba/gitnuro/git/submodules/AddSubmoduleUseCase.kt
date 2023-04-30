package com.jetpackduba.gitnuro.git.submodules

import com.jetpackduba.gitnuro.models.ProgressMonitorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import javax.inject.Inject

class AddSubmoduleUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, name: String, path: String, uri: String) = withContext(Dispatchers.IO) {

        git.submoduleAdd()
            .setName(name)
            .setPath(path)
            .setURI(uri)
            .setProgressMonitor(object : ProgressMonitor {
                override fun start(totalTasks: Int) {}
                override fun beginTask(title: String?, totalWork: Int) {}
                override fun update(completed: Int) {}
                override fun endTask() {}
                override fun showDuration(enabled: Boolean) {}

                override fun isCancelled() = !isActive

            })
            .call()
    }
}