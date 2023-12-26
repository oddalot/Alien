package net.williamott.alien

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import junit.framework.TestCase.assertEquals
import net.williamott.alien.ksp.AlienSymbolProcessorProvider
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException

class AlienProcessorTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `test MotherShip with single Module generates file correctly`() {
        val kotlinSource = SourceFile.kotlin(
            "file1.kt", """
        package net.williamott.alien

        class Sun {
            fun whoAmI() {
                println("I am the sun.")
            }
        }

        @AlienModule
        class Module1 {
            @AlienProvides
            fun provideSun(): Sun {
                return Sun()
            }
        }

        @AlienMotherShip(modules = [Module1::class])
        interface GalaxyShip {
            fun getSun(): Sun
        }
        """
        )

        val compilationResult = compile(kotlinSource)

        assertEquals(KotlinCompilation.ExitCode.OK, compilationResult.exitCode)

        val file = compilationResult.findGeneratedFile("AlienGalaxyShip.kt")

        assertEquals(
            """
            package net.williamott.alien
        
            public class AlienGalaxyShip : GalaxyShip {
              public val module1: Module1 = Module1()
            
              public val provideSunProvider: Provider<Sun> = Module1_provideSunProvider(module1)
            
              public override fun getSun(): Sun = provideSunProvider.get()
            }
            """.trimIndent(),
            file.readText().trimIndent()
        )
    }

    private fun compile(vararg source: SourceFile) = KotlinCompilation().apply {
        sources = source.toList()
        symbolProcessorProviders = listOf(AlienSymbolProcessorProvider())
        workingDir = temporaryFolder.root
        inheritClassPath = true
        verbose = false
    }.compile()

    private fun KotlinCompilation.Result.findGeneratedFile(fileName: String): File {
        val kspWorkingDir = outputDirectory.parentFile.resolve("ksp")
        val kspGeneratedDir = kspWorkingDir.resolve("sources")
        val allFiles = kspGeneratedDir.walk().filter { it.isFile }.toList()
        println(allFiles)
        return allFiles.find { it.name == fileName }
            ?: throw FileNotFoundException("$fileName not found in generated sources")
    }
}