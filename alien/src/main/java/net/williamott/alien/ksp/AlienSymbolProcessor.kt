package net.williamott.alien.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

class AlienSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    private val moduleMap = mutableMapOf<ClassName, MutableMap<TypeName, ProviderData>>()
    private val constructMap = mutableMapOf<ClassName, MutableMap<TypeName, ConstructData>>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val componentSymbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienMotherShip")
        val moduleSymbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienModule")
        val injectSymbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienConstruct")

        injectSymbols.forEach { symbol ->
            logger.warn("Found @AlienInject: $symbol")
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
                    codeGenerator = codeGenerator,
                    logger = logger
                ), Unit
            )
        }

        return emptyList()
    }
}