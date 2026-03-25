package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.LfsError
import com.jetpackduba.gitnuro.domain.lfs.LfsObject
import org.eclipse.jgit.lfs.lib.AnyLongObjectId

interface IVerifyUploadLfsObjectGitAction {
    suspend operator fun invoke(
        lfsServerUrl: String,
        lfsObject: LfsObject,
        oid: AnyLongObjectId,
    ): Either<Unit, LfsError>
}