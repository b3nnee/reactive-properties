package net.aquadc.properties.executor

import android.os.Looper
import javafx.application.Platform
import net.aquadc.properties.android.executor.Handlecutor
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import kotlin.concurrent.getOrSet


internal object ScheduledDaemonHolder : ThreadFactory {

    @JvmField
    internal val scheduledDaemon =
            ScheduledThreadPoolExecutor(1, this)

    override fun newThread(r: Runnable): Thread =
            Thread(r).also { it.isDaemon = true }

}

internal object PlatformExecutors {
    @JvmField internal val executors = ThreadLocal<Executor>()
    private val executorFactories: Array<() -> Executor?>

    init {
        val facs = ArrayList<() -> Executor?>(2)

        try {
            Looper.myLooper() // ensure class available
            facs.add(object : () -> Executor? {
                override fun invoke(): Executor? =
                        Looper.myLooper()?.let(::Handlecutor)

                override fun toString(): String =
                        "AndroidCurrentLooperExecutorFactory(current looper: ${Looper.myLooper()})"
            })
        } catch (ignored: NoClassDefFoundError) {
            // only Android has handlers, JDK doesn't
        }

        try {
            Platform.isFxApplicationThread() // ensure class available
            facs.add(object : () -> Executor? {
                override fun invoke(): Executor? =
                        if (Platform.isFxApplicationThread()) FxApplicationThreadExecutor else null

                override fun toString(): String =
                        "JavaFxApplicationThreadExecutorFactory(on application thread: ${Platform.isFxApplicationThread()})"
            })
        } catch (ignored: NoClassDefFoundError) {
            // Android and some JDK builds do not contain JavaFX
        }

        try {
            ForkJoinTask.getPool() // ensure class available
            facs.add(object : () -> Executor? {
                override fun invoke(): Executor? =
                        ForkJoinTask.getPool() // ForkJoinPool already implements Executor; may be null

                override fun toString(): String =
                        "ForkJoinPoolExecutorFactory(current pool: ${ForkJoinTask.getPool()})"
            })
        } catch (ignored: NoClassDefFoundError) {
            // only JDK 1.7+ and Android 21+ contain FJ
        }

        executorFactories = facs.toArray(arrayOfNulls(facs.size))
    }

    internal fun executorForCurrentThread(): Executor =
            executors.getOrSet(::createForCurrentThread)

    private fun createForCurrentThread(): Executor {
        executorFactories.forEach { it()?.let { return it } }
        throw UnsupportedOperationException(
                "Can't execute task on ${Thread.currentThread()}. " +
                        "Executor factories available: ${Arrays.toString(executorFactories)}")
    }

}
