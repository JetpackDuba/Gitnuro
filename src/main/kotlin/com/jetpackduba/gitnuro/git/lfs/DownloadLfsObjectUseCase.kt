package com.jetpackduba.gitnuro.git.lfs

import com.jetpackduba.gitnuro.NetworkConstants
import com.jetpackduba.gitnuro.lfs.LfsRepository
import com.jetpackduba.gitnuro.models.lfs.LfsObject
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class DownloadLfsObjectUseCase @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsUseCase: ProvideLfsCredentialsUseCase,
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
            provideLfsCredentialsUseCase(
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