package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienMotherShip

@AlienMotherShip(modules = [Module1::class, Module2::class, Module5::class])
interface PlanetShip {
    fun getAnimal(): Animal
    fun getWater(): Water
    fun getBeast(): Beast
}