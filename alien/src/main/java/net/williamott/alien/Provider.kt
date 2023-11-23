package net.williamott.alien

interface Provider<out T> {
    fun get(): T
}
