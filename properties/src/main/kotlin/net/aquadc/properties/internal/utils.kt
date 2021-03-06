@file:JvmName("Internal\$Utils")
package net.aquadc.properties.internal

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater


internal fun Array<*>.with(lastElement: Any?): Array<Any?> {
    val size = size
    val new = arrayOfNulls<Any>(size + 1)
    System.arraycopy(this, 0, new, 0, size)
    new[size] = lastElement
    return new
}

internal fun Array<*>.copyOfWithout(idx: Int, canonicalEmptyArray: Array<Any?>?): Array<Any?>? {
    val oldSize = size
    if (idx == 0 && oldSize == 1) return canonicalEmptyArray

    val new = arrayOfNulls<Any>(oldSize-1)
    System.arraycopy(this, 0, new, 0, idx)
    System.arraycopy(this, idx+1, new, idx, oldSize-idx-1)
    return new
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Array<out T?>.withoutNulls(canonicalEmptyArray: Array<T>?): Array<T>? {
    val nulls = count { it == null }
    if (nulls == 0) return this as Array<T> // it safe since there are no actual nulls

    val newSize = size - nulls
    if (newSize == 0) return canonicalEmptyArray

    val newArray = arrayOfNulls<Any>(size - nulls)
    var destPos = 0
    forEach { if (it != null) newArray[destPos++] = it }

    return newArray as Array<T> // not actually T[], but it's OK
}

/**
 * Compacts array, where null means 'empty'.
 * @return index of first empty index after compaction, or -1, if none
 * @implNote copied from [java.util.ArrayList]
 */
internal fun <T> Array<T?>.compact(): Int {
    var head = 0
    var w = 0
    while (head < size) {
        if (this[head] != null)
            this[w++] = this[head]
        head++
    }
    for (i in w until size) {
        this[i] = null
    }
    return if (w == size) -1 else w
}

internal fun <T, V> AtomicReferenceFieldUpdater<T, V>.eagerOrLazySet(thisRef: T, confinedTo: Thread?, value: V) {
    if (confinedTo == null) set(thisRef, value)
    else lazySet(thisRef, value)
}

internal fun threadIfNot(concurrent: Boolean): Thread? =
        if (concurrent) null else Thread.currentThread()

@JvmField internal val Unset = Any()

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
internal inline fun <T> unset(): T = Unset as T
