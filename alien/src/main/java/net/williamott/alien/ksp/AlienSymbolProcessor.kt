package net.williamott.alien.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import net.williamott.alien.AlienProvides
import net.williamott.alien.Provider

@OptIn(KotlinPoetKspPreview::class)
class AlienSymbolProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    private val moduleMap = mutableMapOf<ClassName, KSClassDeclaration>()
    private val providerMap = mutableMapOf<TypeName, ProviderData>()

    val providerNameMap = mutableMapOf<TypeName, String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienMotherShip")
        val moduleSymbols = resolver.getSymbolsWithAnnotation("net.williamott.alien.AlienModule")

        moduleSymbols.forEach { symbol ->
            logger.warn("Found @AlienModule: $symbol")
            symbol.accept(ModuleSymbolVisitor(), Unit)
        }

        symbols.forEach { symbol ->
            logger.warn("Found @AlienMotherShip: $symbol")
            symbol.accept(ComponentSymbolVisitor(), Unit)
        }

        return emptyList()
    }

    private inner class ComponentSymbolVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            buildComponent(classDeclaration)
        }

        private fun buildComponent(classDeclaration: KSClassDeclaration) {
            val packageName = classDeclaration.containingFile?.packageName?.asString()
            val className = "AlienMotherShip${classDeclaration.toClassName().simpleName}"

            val file = FileSpec.builder(packageName!!, className)
                .addType(
                    TypeSpec.classBuilder(className)
                        .addSuperinterface(classDeclaration.toClassName())
                        .addOriginatingKSFile(classDeclaration.containingFile!!)
                        .addComponentModuleFields(classDeclaration)
                        .addComponentProviderFields(classDeclaration)
                        .addComponentGetterFunctions(classDeclaration)
                        .build()
                )
                .build()
            file.writeTo(codeGenerator = codeGenerator, aggregating = false)
        }

        private fun TypeSpec.Builder.addComponentModuleFields(
            classDeclaration: KSClassDeclaration
        ): TypeSpec.Builder{
            classDeclaration.annotations.find { it.shortName.asString() == "AlienMotherShip" }?.arguments?.forEach { ksValueArgument ->
                val module = ksValueArgument.value as java.util.ArrayList<KSType>
                module.forEach { ksType ->
                    val moduleClassName = ksType.toClassName()
                    logger.warn("maoduleClassName: $moduleClassName")
                    val moduleClassDeclaration = moduleMap[moduleClassName]
                    moduleClassDeclaration?.let { classDeclaration ->
                        logger.warn("modCD: $classDeclaration")
                        propertySpecs += PropertySpec.builder(
                            classDeclaration.simpleName.asString().replaceFirstChar { it.lowercaseChar() }, classDeclaration.toClassName()
                        ).initializer("${classDeclaration.toClassName().simpleName}()")
                            .build()
                    }
                }
            }

            return this
        }

        private fun TypeSpec.Builder.addComponentProviderFields(
            classDeclaration: KSClassDeclaration
        ): TypeSpec.Builder{
            logger.warn("providerMapSize: ${providerMap.size}")
            providerMap.toOrderedList().forEach { (typeName, providerData)  ->
                logger.warn("ordered typeName: $typeName")
                val moduleName = providerData.moduleClass.toClassName().simpleName
                val providerType = providerData.functionDeclaration.returnType?.toTypeName()!!
                val functionName = providerData.functionDeclaration.simpleName.getShortName()
                val classTypeName = Provider::class.asTypeName().plusParameter(providerType)
                val classNameString = "${moduleName}_${functionName}Provider"
                val tempName = functionName.replaceFirstChar { it.lowercaseChar() }
                val providerName = "${tempName}Provider"
                providerNameMap[typeName] = providerName
                val expression = buildString {
                    append(classNameString)
                    append("(")
                    append(moduleName.replaceFirstChar { it.lowercaseChar() })
                    if (providerData.functionDeclaration.parameters.isNotEmpty()) {
                        append(", ")
                    }
                    providerData.functionDeclaration.parameters.forEachIndexed { index, ksValueParameter ->
                        val paramTempName = ksValueParameter.name?.getShortName()
                        val paramName = providerNameMap[ksValueParameter.type.toTypeName()]
                        append(paramName)
                        if (index != providerData.functionDeclaration.parameters.size - 1) {
                            append(", ")
                        }
                    }
                    append(")")
                }

                propertySpecs += PropertySpec.builder(
                    providerName, classTypeName
                ).initializer(expression)
                    .build()
            }

            return this
        }

        private fun Map<TypeName, ProviderData>.toOrderedList(): List<Pair<TypeName, ProviderData>> {
            val providerList = mutableListOf<Pair<TypeName, ProviderData>>()
            this.forEach { (typeName, providerData) ->
                var didInsert = false
                repeat(providerList.size) { index ->
                    val paramTypes = providerList[index].second.functionDeclaration.parameters.map { it.type.toTypeName() }
                    if (paramTypes.contains(typeName)) {
                        if (!didInsert) providerList.add(index, Pair(typeName, providerData))
                        didInsert = true
                        return@repeat
                    }
                }
                if (!didInsert) providerList.add(Pair(typeName, providerData))
                logger.warn("providerList: ${providerList}")
            }

            return providerList
        }

        private fun TypeSpec.Builder.addComponentGetterFunctions(
            classDeclaration: KSClassDeclaration
        ): TypeSpec.Builder{
            classDeclaration.getDeclaredFunctions().forEach { componentFunction ->
                val functionReturnType = componentFunction.returnType?.toTypeName()!!
                val providerName = providerNameMap[functionReturnType]
                funSpecs += FunSpec.builder(componentFunction.simpleName.asString()).returns(functionReturnType).addStatement(
                    "return ${providerName}.get()"
                ).addModifiers(KModifier.OVERRIDE).build()
            }

            return this
        }
    }

    private inner class ModuleSymbolVisitor : KSVisitorVoid() {
        @OptIn(KspExperimental::class)
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            moduleMap[classDeclaration.toClassName()] = classDeclaration
            classDeclaration.getDeclaredFunctions().filter { it.isAnnotationPresent(AlienProvides::class) }.forEach { functionDeclaration ->
                val packageName = classDeclaration.containingFile?.packageName?.asString()
                val returnType = functionDeclaration.returnType?.toTypeName()
                val functionName = functionDeclaration.simpleName.getShortName()
                logger.warn("returnType print: $returnType")
                providerMap[returnType!!] = ProviderData(functionDeclaration, classDeclaration)
                val className = "${classDeclaration.toClassName().simpleName}_${functionName}Provider"
                val moduleName = classDeclaration.toClassName()
                val moduleNameLowerCase = moduleName.toString().substringAfterLast(".").replaceFirstChar { it.lowercaseChar() }

                val file = FileSpec.builder(packageName!!, className)
                    .addType(
                        TypeSpec.classBuilder(className)
                            .addOriginatingKSFile(classDeclaration.containingFile!!)
                            .primaryConstructor(FunSpec.constructorBuilder().addAllParameters(moduleNameLowerCase, moduleName, functionDeclaration).build())
                            .addSuperinterface(
                                Provider::class.asTypeName().plusParameter(returnType)
                            )
                            .addAllProperties(moduleNameLowerCase, moduleName, functionDeclaration)
                            .addModuleGetFunction(returnType, moduleNameLowerCase, functionDeclaration)
                            .build()
                    )
                    .build()
                file.writeTo(codeGenerator = codeGenerator, aggregating = false)
            }
        }

        private fun FunSpec.Builder.addAllParameters(moduleNameLowerCase: String, moduleName: ClassName, functionDeclaration: KSFunctionDeclaration): FunSpec.Builder {
            addParameter(moduleNameLowerCase, moduleName)
            functionDeclaration.parameters.forEach { ksValueParameter ->
                val providerTypeName = ksValueParameter.type.toTypeName().toString().substringAfterLast(".")
                val propertyName = "${providerTypeName.replaceFirstChar { it.lowercaseChar() }}Provider"
                val className = Provider::class.asTypeName().plusParameter(ClassName(moduleName.packageName, providerTypeName))
                addParameter(propertyName, className)
            }

            return this
        }

        private fun TypeSpec.Builder.addAllProperties(moduleNameLowerCase: String, moduleName: ClassName, functionDeclaration: KSFunctionDeclaration): TypeSpec.Builder {
            addProperty(PropertySpec.builder(moduleNameLowerCase, moduleName).initializer(moduleNameLowerCase).addModifiers(KModifier.PRIVATE).build())
            functionDeclaration.parameters.forEach { ksValueParameter ->
                val providerTypeName = ksValueParameter.type.toTypeName().toString().substringAfterLast(".")
                val propertyName = "${providerTypeName.replaceFirstChar { it.lowercaseChar() }}Provider"
                val className = Provider::class.asTypeName().plusParameter(ClassName(moduleName.packageName, providerTypeName))
                addProperty(PropertySpec.builder(propertyName, className).initializer(propertyName).addModifiers(KModifier.PRIVATE).build())
            }

            return this
        }

        private fun TypeSpec.Builder.addModuleGetFunction(
            typeName: TypeName,
            moduleNameLowerCase: String,
            functionDeclaration: KSFunctionDeclaration
        ): TypeSpec.Builder{
            val expression = buildString {
                append("return ")
                append(moduleNameLowerCase)
                append(".")
                append(functionDeclaration.simpleName.getShortName())
                append("(")
                functionDeclaration.parameters.forEachIndexed { index, ksValueParameter ->
                    val paramTempName = ksValueParameter.type.toTypeName().toString().substringAfterLast(".").replaceFirstChar { it.lowercaseChar() }
                    val paramName = "${paramTempName}Provider.get()"
                    append(paramName)
                    if (index != functionDeclaration.parameters.size - 1) {
                        append(", ")
                    }
                }
                append(")")
            }

            funSpecs += FunSpec.builder("get").returns(typeName).addStatement(
                expression
            ).addModifiers(KModifier.OVERRIDE).build()

            return this
        }
    }

    private data class ProviderData(
        val functionDeclaration: KSFunctionDeclaration,
        val moduleClass: KSClassDeclaration
    )
}