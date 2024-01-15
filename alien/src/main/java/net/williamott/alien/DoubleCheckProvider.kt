package net.williamott.alien

class DoubleCheckProvider<T>(
    private val provider: Provider<T>
): Provider<T> {

    @Volatile
    private var INSTANCE: T? = null

    override fun get(): T {
        return if (INSTANCE == null) {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = provider.get()
                }
                INSTANCE!!
            }
        } else {
            INSTANCE!!
        }
    }
}