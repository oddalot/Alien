package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienModule
import net.williamott.alien.AlienProvides

@AlienModule
class Module2 {
    @AlienProvides
    fun provideWater(): Water {
        return Water()
    }

    @AlienProvides
    fun provideAnimal(plant: Plant): Animal {
        return Animal(plant)
    }
}