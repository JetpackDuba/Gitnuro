package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.git.branches.GetTrackingBranchUseCase
import com.jetpackduba.gitnuro.git.isRejected
import com.jetpackduba.gitnuro.git.statusMessage
import com.jetpackduba.gitnuro.preferences.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.RefLeaseSpec
import org.eclipse.jgit.transport.RefSpec
import javax.inject.Inject

class PushBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
    private val getTrackingBranchUseCase: GetTrackingBranchUseCase,
    private val appSettings: AppSettings,
) {
    suspend operator fun invoke(git: Git, force: Boolean, pushTags: Boolean) = withContext(Dispatchers.IO) {
        val currentBranch = git.repository.fullBranch
        val tracking = getTrackingBranchUseCase(git, git.repository.branch)
        val refSpecStr = if (tracking != null) {
            "$currentBranch:${Constants.R_HEADS}${tracking.branch}"
        } else {
            currentBranch
        }

        val pushResult = git
            .push()
            .setRefSpecs(RefSpec(refSpecStr))
            .run {
                if (tracking != null) {
                    setRemote(tracking.remote)
                } else {
                    this
                }
            }
            .setForce(force)
            .run {
                if (force && appSettings.pushWithLease) {

                    if (tracking != null) {
                        val remoteBranchName = "${Constants.R_REMOTES}$remote/${tracking.branch}"

                        val remoteBranchRef = git.repository.findRef(remoteBranchName)
                        if (remoteBranchRef != null) {
                            return@run setRefLeaseSpecs(
                                RefLeaseSpec(
                                    "${Constants.R_HEADS}${tracking.branch}",
                                    remoteBranchRef.objectId.name
                                )
                            )
                        }
                    }
                }

                return@run this
            }
            .run {
                if (pushTags) {
                    setPushTags()
                } else {
                    this
                }
            }
            .setTransportConfigCallback { handleTransportUseCase(it, git) }
            .setProgressMonitor(object : ProgressMonitor {
                override fun start(totalTasks: Int) {}
                override fun beginTask(title: String?, totalWork: Int) {}
                override fun update(completed: Int) {}
                override fun endTask() {}
                override fun isCancelled() = !isActive
                override fun showDuration(enabled: Boolean) {}
            })
            .call()

        val results = pushResult
            .map {
                it.remoteUpdates.filter { remoteRefUpdate -> remoteRefUpdate.status.isRejected }
            }
            .flatten()
        if (results.isNotEmpty()) {
            val error = StringBuilder()

            results.forEach { result ->
                val statusMessage = result.statusMessage
                val extraMessage = if (statusMessage == "Ref rejected, old object id in remote has changed.") {
                    "Force push can't be completed without fetching first the remote changes."
                } else
                    null

                error.append(statusMessage)

                if (extraMessage != null) {
                    error.append("\n")
                    error.append(extraMessage)
                }

                error.append("\n")
            }

            throw Exception(error.toString())
        }
    }

}