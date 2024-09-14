package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.models.AuthorInfoSimple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetAuthorInfoUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git) = withContext(Dispatchers.IO) {
        val config = git.repository.config
        config.load()

        val userName = config.getString("user", null, "name")
        val email = config.getString("user", null, "email")
        
        AuthorInfoSimple(userName, email)
    }
}
