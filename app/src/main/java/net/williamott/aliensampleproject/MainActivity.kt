package net.williamott.aliensampleproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.williamott.alien.AlienInject
import net.williamott.aliensampleproject.di.AlienPlanetShip
import net.williamott.aliensampleproject.di.Animal
import net.williamott.aliensampleproject.di.Beast
import net.williamott.aliensampleproject.di.Plant
import net.williamott.aliensampleproject.ui.theme.AlienSampleProjectTheme


class MainActivity : ComponentActivity() {
    private val motherShip = AlienPlanetShip()
    private val animal = motherShip.getAnimal()

    @AlienInject
    lateinit var plant: Plant


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        motherShip.inject(this)
        setContent {
            AlienSampleProjectTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting(animal.whoAmI())
                        Greeting(plant.whoAmI())
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AlienSampleProjectTheme {
        Greeting("Android")
    }
}