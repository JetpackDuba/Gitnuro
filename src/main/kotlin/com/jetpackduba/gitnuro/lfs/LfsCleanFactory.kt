package com.jetpackduba.gitnuro.lfs

import org.eclipse.jgit.attributes.FilterCommand
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.LfsPointer
import org.eclipse.jgit.lfs.errors.CorruptMediaFile
import org.eclipse.jgit.lfs.internal.AtomicObjectOutputStream
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.util.FileUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LfsCleanFilter(
    repository: Repository,
    input: InputStream,
    output: OutputStream,
) :
    FilterCommand(input, output) {
    private val lfs = Lfs(repository)

    // the size of the original content
    private var size: Long = 0

    // a temporary file into which the original content is written. When no
    // errors occur this file will be renamed to the mediafile
    private val tmpFile: Path by lazy {
        Files.createDirectories(lfs.lfsTmpDir)
        lfs.createTmpFile()
    }

    // Used to compute the hash for the original content
    private var aOut: AtomicObjectOutputStream? = AtomicObjectOutputStream(tmpFile.toAbsolutePath())

    private val input: InputStream = this.`in`

    @Throws(IOException::class)
    override fun run(): Int {
        try {
            val buf = ByteArray(8192)
            val length = input.read(buf)
            if (length != -1) {
                aOut?.write(buf, 0, length)
                size += length.toLong()
                return length
            }

            aOut?.close()

            val oid = checkNotNull(aOut?.id)
            aOut = null

            val mediaFile = lfs.getMediaFile(oid)

            if (Files.isRegularFile(mediaFile)) {
                val fsSize = Files.size(mediaFile)
                if (fsSize != size) {
                    throw CorruptMediaFile(mediaFile, size, fsSize)
                }
                FileUtils.delete(tmpFile.toFile())
            } else {
                val parent = mediaFile.parent
                if (parent != null) {
                    FileUtils.mkdirs(parent.toFile(), true)
                }

                FileUtils.rename(
                    tmpFile.toFile(), mediaFile.toFile(),
                    StandardCopyOption.ATOMIC_MOVE
                )
            }

            val lfsPointer = LfsPointer(oid, size)
            lfsPointer.encode(out)

            input.close()
            out.close()

            return -1
        } catch (e: IOException) {
            aOut?.abort()
            input.close()
            out.close()
            throw e
        }
    }
}
