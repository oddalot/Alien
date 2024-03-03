package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienMotherShip
import net.williamott.aliensampleproject.MainActivity

@AlienMotherShip(modules = [Module1::class, Module2::class, Module5::class])
interface PlanetShip {
    fun getAnimal(): Animal
    fun getWater(): Water
    fun getBeast(): Beast
    fun inject(mainActivity: MainActivity)
}