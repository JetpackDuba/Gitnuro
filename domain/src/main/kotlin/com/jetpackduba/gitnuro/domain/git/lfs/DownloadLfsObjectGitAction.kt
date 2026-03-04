package com.jetpackduba.gitnuro.domain.git.lfs

import com.jetpackduba.gitnuro.domain.lfs.LfsObject
import com.jetpackduba.gitnuro.domain.network.NetworkConstants
import com.jetpackduba.gitnuro.domain.repositories.LfsRepository
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class
DownloadLfsObjectGitAction @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsGitAction: ProvideLfsCredentialsGitAction,
) {
    suspend operator fun invoke(
        repository: Repository,
        lfsServerUrl: String,
        lfsObject: LfsObject,
        oid: AnyLongObjectId,
    ) {
        val lfs = Lfs(repository)
        val downloadUrl = lfsObject.actions?.download?.href ?: return
        val headers = lfsObject.actions.download.header.orEmpty()

        if (headers.containsKey(NetworkConstants.AUTH_HEADER)) {
            lfsRepository.downloadObject(
                downloadUrl = downloadUrl,
                outPath = lfs.getMediaFile(oid),
                headers = headers,
                username = null,
                password = null
            )
        } else {
            provideLfsCredentialsGitAction(
                lfsServerUrl
            ) { user, password ->
                lfsRepository.downloadObject(
                    downloadUrl = downloadUrl,
                    outPath = lfs.getMediaFile(oid),
                    headers = headers,
                    username = user,
                    password = password,
                )
            }
        }
    }
}