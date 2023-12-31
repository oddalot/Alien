package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienModule
import net.williamott.alien.AlienProvides


@AlienModule
class Module1 {
    @AlienProvides
    fun providePlant(sun: Sun, water: Water): Plant {
        return Plant(sun, water)
    }
}