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

    private val _tags = MutableStateFlow<List<Ref>>(listOf())
    val tags: StateFlow<List<Ref>>
        get() = _tags

    suspend fun loadTags(git: Git) = withContext(Dispatchers.IO) {
        val branchList = git.tagList().call()


        _tags.value = branchList
    }

    suspend fun createTagOnCommit(git: Git, tag: String, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .tag()
            .setAnnotated(true)
            .setName(tag)
            .setObjectId(revCommit)
            .call()
    }
}