package net.williamott.alien.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import kotlin.math.log

class AlienSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    private val moduleMap = mutableMapOf<ClassName, MutableMap<TypeName, ProviderData>>()
    private val constructMap = mutableMapOf<TypeName, ProviderData>()
    private val bindsMap = mutableMapOf<TypeName, BindsData>()
    private val injectMap = mutableMapOf<ClassName, InjectData>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val componentSymbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienMotherShip")
        val moduleSymbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienModule")
        val constructSymbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienConstruct")
        val injectSymbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienInject")

        injectSymbols.forEach { symbol ->
            logger.warn("found AlienInject: $symbol")
            symbol.accept(
               InjectSymbolVisitor(
                   logger = logger,
                   injectMap = injectMap
               ), Unit
            )
        }

        constructSymbols.forEach { symbol ->
            logger.warn("Found @AlienConstruct: $symbol")
            symbol.accept(
                ConstructSymbolVisitor(
                    constructMap = constructMap,
                    codeGenerator = codeGenerator,
                    logger = logger
                ), Unit
            )
        }

        moduleSymbols.forEach { symbol ->
            logger.warn("Found @AlienModule: $symbol")
            symbol.accept(
                ModuleSymbolVisitor(
                    moduleMap = moduleMap,
                    bindsMap = bindsMap,
                    codeGenerator = codeGenerator,
                    logger = logger
                ), Unit
            )
        }

        componentSymbols.forEach { symbol ->
            logger.warn("Found @AlienMotherShip: $symbol")
            symbol.accept(
                ComponentSymbolVisitor(
                    moduleMap = moduleMap,
                    constructMap = constructMap,
                    bindsMap = bindsMap,
                    injectMap = injectMap,
                    codeGenerator = codeGenerator,
                    logger = logger
                ), Unit
            )
        }

        return emptyList()
    }
}