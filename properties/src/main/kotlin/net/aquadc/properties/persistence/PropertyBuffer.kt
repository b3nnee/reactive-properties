package net.aquadc.properties.persistence

import net.aquadc.properties.MutableProperty
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference

/**
 * [InvocationHandler] for [PropertyIo].
 * Reads properties' values into buffers;
 * writes buffered values into properties.
 */
class PropertyBuffer private constructor() : InvocationHandler {

    private val values = ArrayList<Any?>()
    private var produceNext = -1

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>): Any? {
        @Suppress("UNCHECKED_CAST") // won't explode if used properly
        val prop = args[0] as MutableProperty<Any?>
        if (produceNext == -1) { // consume
            values.add(prop.value)
        } else { // produce
            val idx = produceNext++
            prop.value = values[idx]
            values[idx] = null
        }
        return null
    }

    /**
     * Switches mode to production.
     */
    fun produceThen() {
        check(produceNext == -1)
        produceNext = 0
    }

    /**
     * Switches mode to consumption.
     */
    fun consumeThen() {
        check(produceNext == values.size)
        values.clear()
        produceNext = -1
    }

    /**
     * Checks whether buffer is clean and ready to use.
     */
    fun assertClean() {
        check(produceNext == -1)
        check(values.isEmpty())
    }

    companion object {
        @JvmField // 'private' is 'static' and produces a synthetic accessor
        internal val ref = AtomicReference<Pair<PropertyIo, PropertyBuffer>?>(null)

        /**
         * Returns either new or cached instance.
         */
        fun get(): Pair<PropertyIo, PropertyBuffer> {
            ref.getAndSet(null)?.let { return it }
            val buf = PropertyBuffer()
            val proxy = Proxy.newProxyInstance(PropertyIo::class.java.classLoader, arrayOf(PropertyIo::class.java), buf) as PropertyIo
            return proxy to buf
        }

        /**
         * Returns instance to cache.
         */
        fun recycle(buffer: Pair<PropertyIo, PropertyBuffer>) {
            check(Proxy.getInvocationHandler(buffer.first) === buffer.second)
            buffer.second.assertClean()
            ref.set(buffer)
        }

        /**
         * Calls [block] on borrowed [PropertyIo] and [PropertyBuffer], returns to cache then.
         */
        inline fun <R> borrow(block: (PropertyIo, PropertyBuffer) -> R): R {
            val pdPb = get()
            val (pd, pb) = pdPb
            val ret = block(pd, pb)
            recycle(pdPb)
            return ret
        }
    }

}
