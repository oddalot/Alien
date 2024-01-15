package net.williamott.alien

class DoubleCheckProvider<T>(
    private val provider: Provider<T>
): Provider<T> {

    @Volatile
    private var INSTANCE: T? = null

    override fun get(): T {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: provider.get().also { INSTANCE = it }
        }
    }
}