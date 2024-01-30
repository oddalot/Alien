package net.williamott.alien.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.isConstructor
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
import net.williamott.alien.AlienConstruct
import net.williamott.alien.AlienSingleton
import net.williamott.alien.Provider


class ConstructSymbolVisitor(
    private val constructMap: MutableMap<TypeName, ProviderData>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : KSVisitorVoid() {

    @OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        if (!function.isAnnotationPresent(AlienConstruct::class)) return
        if (!function.isConstructor()) throw IllegalStateException("@AlienConstruct can only be applied to a Class constructor")
        val packageName = function.containingFile?.packageName?.asString()
        val returnType = function.returnType?.toTypeName()!!
        logger.warn("returnType print: $returnType")
        val constructClass = (function.parent as KSClassDeclaration)
        val typeName = constructClass.toClassName().simpleName
        val className = "Construct_${typeName}Provider"
        val isScoped = function.isAnnotationPresent(AlienSingleton::class)

        constructMap[returnType] = ProviderData(
            functionDeclaration = function,
            moduleClass = null,
            constructClass = constructClass,
            bindsData = null,
            isScoped = isScoped
        )

        val file = FileSpec.builder(packageName!!, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addOriginatingKSFile(function.containingFile!!)
                    .primaryConstructor(
                        FunSpec.constructorBuilder().addAllParameters(
                            function
                        ).build()
                    )
                    .addSuperinterface(
                        Provider::class.asTypeName().plusParameter(returnType)
                    )
                    .addAllProperties(
                        function
                    )
                    .addConstructGetFunction(
                        returnType,
                        typeName,
                        function
                    )
                    .build()
            )
            .build()
        file.writeTo(codeGenerator = codeGenerator, aggregating = false)
    }

    @OptIn(KotlinPoetKspPreview::class)
    private fun TypeSpec.Builder.addConstructGetFunction(
        typeName: TypeName,
        className: String,
        functionDeclaration: KSFunctionDeclaration
    ): TypeSpec.Builder {
        val expression = buildString {
            append("return ")
            append(className)
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

    @OptIn(KotlinPoetKspPreview::class)
    private fun FunSpec.Builder.addAllParameters(
        functionDeclaration: KSFunctionDeclaration
    ): FunSpec.Builder {
        functionDeclaration.parameters.forEach { ksValueParameter ->
            val providerTypeName =
                ksValueParameter.type.toTypeName().toString().substringAfterLast(".")
            val propertyName =
                "${providerTypeName.replaceFirstChar { it.lowercaseChar() }}Provider"
            val className = Provider::class.asTypeName()
                .plusParameter(
                    ClassName(
                        functionDeclaration.packageName.asString(),
                        providerTypeName
                    )
                )
            addParameter(propertyName, className)
        }

        return this
    }

    @OptIn(KotlinPoetKspPreview::class)
    private fun TypeSpec.Builder.addAllProperties(
        functionDeclaration: KSFunctionDeclaration
    ): TypeSpec.Builder {
        functionDeclaration.parameters.forEach { ksValueParameter ->
            val providerTypeName =
                ksValueParameter.type.toTypeName().toString().substringAfterLast(".")
            val propertyName =
                "${providerTypeName.replaceFirstChar { it.lowercaseChar() }}Provider"
            val className = Provider::class.asTypeName()
                .plusParameter(
                    ClassName(
                        functionDeclaration.packageName.asString(),
                        providerTypeName
                    )
                )
            addProperty(
                PropertySpec.builder(propertyName, className).initializer(propertyName)
                    .addModifiers(KModifier.PRIVATE).build()
            )
        }

        return this
    }
}