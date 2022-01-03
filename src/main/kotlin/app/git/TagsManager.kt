package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class TagsManager @Inject constructor() {
    suspend fun getTags(git: Git) = withContext(Dispatchers.IO) {
        return@withContext git.tagList().call()
    }

    suspend fun createTagOnCommit(git: Git, tag: String, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .tag()
            .setAnnotated(true)
            .setName(tag)
            .setObjectId(revCommit)
            .call()
    }

    suspend fun deleteTag(git: Git, tag: Ref) = withContext(Dispatchers.IO) {
        git
            .tagDelete()
            .setTags(tag.name)
            .call()
    }
}