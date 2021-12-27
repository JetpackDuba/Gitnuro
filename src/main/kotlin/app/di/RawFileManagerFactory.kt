package app.di

import app.git.RawFileManager
import app.git.diff.HunkDiffGenerator
import dagger.assisted.AssistedFactory
import org.eclipse.jgit.lib.Repository

@AssistedFactory
interface RawFileManagerFactory {
    fun create(repository: Repository): RawFileManager
}

@AssistedFactory
interface HunkDiffGeneratorFactory {
    fun create(repository: Repository, rawFileManager: RawFileManager): HunkDiffGenerator
}