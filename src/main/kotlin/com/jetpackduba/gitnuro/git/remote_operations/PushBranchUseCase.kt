package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.git.branches.GetTrackingBranchUseCase
import com.jetpackduba.gitnuro.git.branches.SetTrackingBranchUseCase
import com.jetpackduba.gitnuro.git.branches.TrackingBranch
import com.jetpackduba.gitnuro.git.isRejected
import com.jetpackduba.gitnuro.git.statusMessage
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.RefLeaseSpec
import org.eclipse.jgit.transport.RefSpec
import java.io.PrintStream
import javax.inject.Inject
import kotlin.math.max

class PushBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
    private val getTrackingBranchUseCase: GetTrackingBranchUseCase,
    private val setTrackingBranchUseCase: SetTrackingBranchUseCase,
    private val appSettingsRepository: AppSettingsRepository,
) {
    suspend operator fun invoke(git: Git, force: Boolean, pushTags: Boolean) = withContext(Dispatchers.IO) {
        val currentBranch = git.repository.branch
        val fullCurrentBranch = git.repository.fullBranch

        val tracking = getTrackingBranchUseCase(git, currentBranch)
        val refSpecStr = if (tracking != null) {
            "$fullCurrentBranch:${Constants.R_HEADS}${tracking.branch}"
        } else {
            fullCurrentBranch
        }

        val remoteRefUpdate = handleTransportUseCase(git) {
            push(git, tracking, refSpecStr, force, pushTags)
        }


        if (tracking == null && remoteRefUpdate != null) {
            // [remoteRefUpdate.trackingRefUpdate.localName] should have the following format: refs/remotes/REMOTE_NAME/BRANCH_NAME
            val remoteBranchPathSplit = remoteRefUpdate.trackingRefUpdate.localName.split("/")
            val remoteName = remoteBranchPathSplit.getOrNull(2)
            val remoteBranchName = remoteBranchPathSplit.takeLast(max(0, remoteBranchPathSplit.count() - 3)).joinToString("/")
            setTrackingBranchUseCase(git, currentBranch, remoteName, remoteBranchName)
        }
    }

    private suspend fun CredentialsHandler.push(
        git: Git,
        tracking: TrackingBranch?,
        refSpecStr: String?,
        force: Boolean,
        pushTags: Boolean
    ) = withContext(Dispatchers.IO) {
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
                if (force && appSettingsRepository.pushWithLease) {

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
            .setTransportConfigCallback { handleTransport(it) }
            .setProgressMonitor(object : ProgressMonitor {
                override fun start(totalTasks: Int) {}
                override fun beginTask(title: String?, totalWork: Int) {}
                override fun update(completed: Int) {}
                override fun endTask() {}
                override fun isCancelled() = !isActive
                override fun showDuration(enabled: Boolean) {}
            })
            .setHookOutputStream(System.out)
            .setHookErrorStream(System.err)
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

        return@withContext pushResult.firstOrNull()?.remoteUpdates?.firstOrNull()
    }
}