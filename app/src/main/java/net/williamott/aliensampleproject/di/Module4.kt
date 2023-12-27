package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienModule
import net.williamott.alien.AlienProvides


@AlienModule
class Module4 {
    @AlienProvides
    fun provideWater(): Water {
        return Water()
    }
}