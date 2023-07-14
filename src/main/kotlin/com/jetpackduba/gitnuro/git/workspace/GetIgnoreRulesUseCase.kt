package com.jetpackduba.gitnuro.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.ignore.FastIgnoreRule
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Repository
import java.io.File
import java.nio.file.FileSystems
import javax.inject.Inject


class GetIgnoreRulesUseCase @Inject constructor() {
    suspend operator fun invoke(repository: Repository): List<FastIgnoreRule> = withContext(Dispatchers.IO) {
        val ignoreLines = mutableListOf<String>()

        val repositoryExcludeFile = File(repository.directory, ".git/info/exclude")
        val ignoreFile = File(repository.workTree, ".gitignore")

        repository.config.load()
        val baseConfig: Config? = repository.config.baseConfig

        if (repositoryExcludeFile.exists() && repositoryExcludeFile.isFile) {
            ignoreLines.addAll(repositoryExcludeFile.readLines())
        }

        if (ignoreFile.exists() && ignoreFile.isFile) {
            ignoreLines.addAll(ignoreFile.readLines())
        }

        if (baseConfig != null) {
            var excludesFilePath = baseConfig.getString("core", null, "excludesFile")

            if (excludesFilePath.startsWith("~")) {
                excludesFilePath = excludesFilePath.replace("~", System.getProperty("user.home").orEmpty())
            }

            val excludesFile = FileSystems
                .getDefault()
                .getPath(excludesFilePath)
                .normalize()
                .toFile()

            if (excludesFile.exists() && excludesFile.isFile) {
                ignoreLines.addAll(excludesFile.readLines())
            }
        }

        ignoreLines.map { FastIgnoreRule(it) }
    }
}