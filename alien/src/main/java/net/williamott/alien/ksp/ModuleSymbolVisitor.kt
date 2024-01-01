package net.williamott.alien.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
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


class ModuleSymbolVisitor(
    private val moduleMap: MutableMap<ClassName, MutableMap<TypeName, ProviderData>>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : KSVisitorVoid() {
    @OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val providerMap = mutableMapOf<TypeName, ProviderData>()
        moduleMap[classDeclaration.toClassName()] = providerMap
        classDeclaration.getDeclaredFunctions()
            .filter { it.isAnnotationPresent(AlienProvides::class) }
            .forEach { functionDeclaration ->
                val packageName = classDeclaration.containingFile?.packageName?.asString()
                val returnType = functionDeclaration.returnType?.toTypeName()
                val functionName = functionDeclaration.simpleName.getShortName()
                logger.warn("returnType print: $returnType")
                providerMap[returnType!!] = ProviderData(
                    functionDeclaration = functionDeclaration,
                    moduleClass = classDeclaration,
                    constructClass = null
                )
                val className =
                    "${classDeclaration.toClassName().simpleName}_${functionName}Provider"
                val moduleName = classDeclaration.toClassName()
                val moduleNameLowerCase = moduleName.toString().substringAfterLast(".")
                    .replaceFirstChar { it.lowercaseChar() }

                val file = FileSpec.builder(packageName!!, className)
                    .addType(
                        TypeSpec.classBuilder(className)
                            .addOriginatingKSFile(classDeclaration.containingFile!!)
                            .primaryConstructor(
                                FunSpec.constructorBuilder().addAllParameters(
                                    moduleNameLowerCase,
                                    moduleName,
                                    functionDeclaration
                                ).build()
                            )
                            .addSuperinterface(
                                Provider::class.asTypeName().plusParameter(returnType)
                            )
                            .addAllProperties(
                                moduleNameLowerCase,
                                moduleName,
                                functionDeclaration
                            )
                            .addModuleGetFunction(
                                returnType,
                                moduleNameLowerCase,
                                functionDeclaration
                            )
                            .build()
                    )
                    .build()
                file.writeTo(codeGenerator = codeGenerator, aggregating = false)
            }
    }


    @OptIn(KotlinPoetKspPreview::class)
    private fun FunSpec.Builder.addAllParameters(
        moduleNameLowerCase: String,
        moduleName: ClassName,
        functionDeclaration: KSFunctionDeclaration
    ): FunSpec.Builder {
        addParameter(moduleNameLowerCase, moduleName)
        functionDeclaration.parameters.forEach { ksValueParameter ->
            val providerTypeName =
                ksValueParameter.type.toTypeName().toString().substringAfterLast(".")
            val propertyName =
                "${providerTypeName.replaceFirstChar { it.lowercaseChar() }}Provider"
            val className = Provider::class.asTypeName()
                .plusParameter(ClassName(moduleName.packageName, providerTypeName))
            addParameter(propertyName, className)
        }

        return this
    }

    @OptIn(KotlinPoetKspPreview::class)
    private fun TypeSpec.Builder.addAllProperties(
        moduleNameLowerCase: String,
        moduleName: ClassName,
        functionDeclaration: KSFunctionDeclaration
    ): TypeSpec.Builder {
        addProperty(
            PropertySpec.builder(moduleNameLowerCase, moduleName)
                .initializer(moduleNameLowerCase).addModifiers(KModifier.PRIVATE).build()
        )
        functionDeclaration.parameters.forEach { ksValueParameter ->
            val providerTypeName =
                ksValueParameter.type.toTypeName().toString().substringAfterLast(".")
            val propertyName =
                "${providerTypeName.replaceFirstChar { it.lowercaseChar() }}Provider"
            val className = Provider::class.asTypeName()
                .plusParameter(ClassName(moduleName.packageName, providerTypeName))
            addProperty(
                PropertySpec.builder(propertyName, className).initializer(propertyName)
                    .addModifiers(KModifier.PRIVATE).build()
            )
        }

        return this
    }

    @OptIn(KotlinPoetKspPreview::class)
    private fun TypeSpec.Builder.addModuleGetFunction(
        typeName: TypeName,
        moduleNameLowerCase: String,
        functionDeclaration: KSFunctionDeclaration
    ): TypeSpec.Builder {
        val expression = buildString {
            append("return ")
            append(moduleNameLowerCase)
            append(".")
            append(functionDeclaration.simpleName.getShortName())
            append("(")
            functionDeclaration.parameters.forEachIndexed { index, ksValueParameter ->
                val paramTempName =
                    ksValueParameter.type.toTypeName().toString().substringAfterLast(".")
                        .replaceFirstChar { it.lowercaseChar() }
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