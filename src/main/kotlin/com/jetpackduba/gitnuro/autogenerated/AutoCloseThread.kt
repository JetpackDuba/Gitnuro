import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue

class AutoCloseThread : Thread() {
    private val referenceQueue = ReferenceQueue<AutoCloseable?>()
    private val phantomStack: MutableSet<NativeResource> = HashSet()

    init {
        isDaemon = true
    }

    fun addObject(
        pTest: AutoCloseable,
        pointer: Long,
        className: String,
        drop: (Long) -> Unit,
    ): NativeResource {
        val rs = NativeResource(pointer, className, drop, pTest, referenceQueue)
        phantomStack.add(rs)
        return rs
    }

    fun remove(rs: NativeResource) {
        phantomStack.remove(rs)
    }

    override fun run() {
        try {
            while (true) {
                val rs = referenceQueue.remove() as NativeResource
                println(rs.pointer.toString() + " not properly closed, doing it now")
                phantomStack.remove(rs)
                rs.close()
            }
        } catch (e: InterruptedException) {
            println("Thread Interrupted")
        }
    }

    fun contains(rs: NativeResource): Boolean {
        return phantomStack.contains(rs)
    }
}

class NativeResource(
    val pointer: Long,
    val className: String,
    val drop: (Long) -> Unit,
    referent: AutoCloseable,
    queue: ReferenceQueue<AutoCloseable?>
) : PhantomReference<AutoCloseable?>(referent, queue) {
    fun close() {
        drop(pointer)
        println("$className destroyed $pointer")
    }
}

val thread: AutoCloseThread = AutoCloseThread().apply {
    start()
}