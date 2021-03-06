package net.aquadc.properties.diff.internal

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.diff.DiffChangeListener
import net.aquadc.properties.diff.DiffProperty
import net.aquadc.properties.executor.ConfinedChangeListener
import net.aquadc.properties.executor.ConfinedDiffChangeListener
import net.aquadc.properties.executor.PlatformExecutors
import net.aquadc.properties.executor.UnconfinedExecutor
import net.aquadc.properties.internal.`-Listeners`
import java.util.concurrent.Executor


internal abstract class `ConcDiff-Notifier`<T, D> : `-Listeners`<T, D, Function<Unit>, Pair<T, D>>(null), DiffProperty<T, D> {

    final override fun addChangeListener(onChange: ChangeListener<T>) =
            concAddChangeListenerInternal(
                    ConfinedChangeListener(PlatformExecutors.executorForCurrentThread(), onChange)
            )

    override fun addChangeListenerOn(executor: Executor, onChange: ChangeListener<T>) =
            concAddChangeListenerInternal(
                    if (executor === UnconfinedExecutor) onChange else ConfinedChangeListener(executor, onChange)
            )

    final override fun removeChangeListener(onChange: ChangeListener<T>) =
            rm(onChange)


    final override fun addChangeListener(onChangeWithDiff: DiffChangeListener<T, D>) =
            concAddChangeListenerInternal(
                    ConfinedDiffChangeListener(PlatformExecutors.executorForCurrentThread(), onChangeWithDiff)
            )

    override fun addChangeListenerOn(executor: Executor, onChangeWithDiff: DiffChangeListener<T, D>) =
            concAddChangeListenerInternal(
                    if (executor === UnconfinedExecutor) onChangeWithDiff else ConfinedDiffChangeListener(executor, onChangeWithDiff)
            )

    final override fun removeChangeListener(onChangeWithDiff: DiffChangeListener<T, D>) =
            rm(onChangeWithDiff)


    final override fun pack(new: T, diff: D): Pair<T, D> =
            new to diff

    final override fun unpackValue(packed: Pair<T, D>): T =
            packed.first

    final override fun unpackDiff(packed: Pair<T, D>): D =
            packed.second

    private fun rm(onChange: Function<Unit>) {
        removeChangeListenerWhere { listener ->
            when {
                listener === onChange ->
                    true

                listener is ConfinedChangeListener<*> && listener.actual === onChange ->
                    true.also { listener.canceled = true }

                listener is ConfinedDiffChangeListener<*, *> && listener.actual === onChange ->
                    true.also { listener.canceled = true }

                else ->
                    false
            }
        }
    }

    @Suppress("UNCHECKED_CAST") // oh, so many of them
    final override fun notify(listener: Function<Unit>, old: T, new: T, diff: D) = when (listener) {
        is Function2<*, *, *> -> (listener as ChangeListener<T>)(old, new)
        is Function3<*, *, *, *> -> (listener as DiffChangeListener<T, D>)(old, new, diff)
        else -> throw AssertionError()
    }

}
