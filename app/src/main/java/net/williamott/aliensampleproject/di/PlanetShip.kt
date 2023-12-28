package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienMotherShip

@AlienMotherShip(modules = [Module1::class, Module2::class])
interface PlanetShip {
    fun getAnimal(): Animal
    fun getWater(): Water
}