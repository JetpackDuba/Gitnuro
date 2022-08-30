package app.git.tags

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class DeleteTagUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, tag: Ref): Unit = withContext(Dispatchers.IO) {
        git
            .tagDelete()
            .setTags(tag.name)
            .call()
    }
}