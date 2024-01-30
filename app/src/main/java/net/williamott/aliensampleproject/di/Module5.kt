package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienBinds
import net.williamott.alien.AlienModule
import net.williamott.alien.AlienProvides
import net.williamott.alien.AlienSingleton

@AlienModule
interface Module5 {
    @AlienSingleton
    @AlienBinds
    fun bindBeast(animal: Animal): Beast
}