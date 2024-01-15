package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienModule
import net.williamott.alien.AlienProvides
import net.williamott.alien.AlienSingleton

@AlienModule
class Module1 {
    @AlienSingleton
    @AlienProvides
    fun provideSun(): Sun {
        return Sun()
    }
}