package net.williamott.aliensampleproject.di

import net.williamott.alien.AlienConstruct

class Sun {
    fun whoAmI(): String {
        return "I am the sun."
    }
}

class Water {
    fun whoAmI(): String {
        return "I am water."
    }
}


class Animal @AlienConstruct constructor(private val plant: Plant): Beast {
    fun whoAmI(): String {
        return plant.whoAmI() + "I am an animal."
    }
}

class Plant @AlienConstruct constructor(private val sun: Sun, private val water: Water) {
    fun whoAmI(): String {
        return  sun.whoAmI() + water.whoAmI() + "I am a plant."
    }
}

interface Beast