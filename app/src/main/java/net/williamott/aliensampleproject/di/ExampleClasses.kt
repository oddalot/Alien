package net.williamott.aliensampleproject.di

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

class Animal(private val plant: Plant) {
    fun whoAmI(): String {
        return plant.whoAmI() + "I am an animal."
    }
}

class Plant(private val sun: Sun, private val water: Water) {
    fun whoAmI(): String {
        return  sun.whoAmI() + water.whoAmI() + "I am a plant."
    }
}