package net.williamott.alien.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
import net.williamott.alien.DoubleCheckProvider
import net.williamott.alien.Provider

class ComponentSymbolVisitor(
    private val moduleMap: Map<ClassName, MutableMap<TypeName, ProviderData>>,
    private val constructMap: Map<TypeName, ProviderData>,
    private val bindsMap: MutableMap<TypeName, BindsData>,
    private val injectMap: MutableMap<ClassName, InjectData>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : KSVisitorVoid() {
    private var hasScopedProvider = false

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        buildComponent(classDeclaration)
    }

    @OptIn(KotlinPoetKspPreview::class)
    private fun buildComponent(classDeclaration: KSClassDeclaration) {
        val packageName = classDeclaration.containingFile?.packageName?.asString()
        val className = "Alien${classDeclaration.toClassName().simpleName}"

        val file = FileSpec.builder(packageName!!, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(classDeclaration.toClassName())
                    .addOriginatingKSFile(classDeclaration.containingFile!!)
                    .buildProviderData(classDeclaration)
                    .build()
            )
            .addDoubleCheckImport()
            .build()
        file.writeTo(codeGenerator = codeGenerator, aggregating = true)
    }

    private fun FileSpec.Builder.addDoubleCheckImport(): FileSpec.Builder {
        if (hasScopedProvider) {
            addImport(DoubleCheckProvider::class.java.`package`.name, "DoubleCheckProvider")
        }
        return this
    }

    @OptIn(KotlinPoetKspPreview::class)
    private fun TypeSpec.Builder.buildProviderData(
        classDeclaration: KSClassDeclaration
    ): TypeSpec.Builder {
        val providerNameMap = mutableMapOf<TypeName, String>()
        val modulePrintSet = linkedSetOf<KSClassDeclaration>()
        val providerPrintSet = linkedSetOf<TypeName>()
        val functionPrintSet = linkedSetOf<ProviderFunctionData>()
        val injectPrintSet = linkedSetOf<InjectPrintData>()
        val componentProviderMap = mutableMapOf<TypeName, ProviderData>()

        classDeclaration.annotations.forEach {
            logger.warn("componentAnnotation: $it")
        }

        @Suppress("UNCHECKED_CAST")
        classDeclaration.annotations.find { it.shortName.asString() == "AlienMotherShip" }?.arguments?.forEach { ksValueArgument ->
            val moduleList = ksValueArgument.value as java.util.ArrayList<KSType>
            moduleList.forEach { ksType ->
                moduleMap[ksType.toClassName()]?.forEach { (typeName, providerData) ->
                    componentProviderMap[typeName] = providerData
                }
            }
        }

        constructMap.forEach { (typeName, providerData) ->
            componentProviderMap[typeName] = providerData
        }

        bindsMap.forEach { (typeName, bindsData) ->
            logger.warn("adding BindsData")
            val implClass = bindsData.bindsFunction.parameters.first().type.toTypeName()
            val foundProviderData =
                componentProviderMap[implClass]
            if (foundProviderData != null) {
                logger.warn("adding BindsData: not null")
                componentProviderMap[typeName] = foundProviderData.copy(bindsData = bindsData, isScoped = bindsData.isScoped)
            }
        }

        classDeclaration.getDeclaredFunctions().forEach { componentFunction ->
            val functionReturnType = componentFunction.returnType?.toTypeName()!!
            val functionName = componentFunction.simpleName.getShortName()
            if (functionName == "inject") {
                val implParameterName = componentFunction.parameters.first().name?.getShortName()!!
                val implClass = componentFunction.parameters.first().type.toTypeName()
                val injectData = injectMap[implClass]!!
                logger.warn("injectFile: ${injectData.implFile}")
                addOriginatingKSFile(injectData.implFile)
                injectPrintSet.add(
                    InjectPrintData(
                        implParameterName = implParameterName,
                        implClass = implClass,
                        memberClasses = injectData.memberClasses
                    )
                )
                injectData.memberClasses.forEach { memberInjectData ->
                    recurseThroughParams(
                        memberInjectData.memberTypeName,
                        modulePrintSet,
                        providerPrintSet,
                        componentProviderMap
                    )
                }
            } else {
                functionPrintSet.add(
                    ProviderFunctionData(
                        functionName,
                        functionReturnType
                    )
                )
                recurseThroughParams(
                    functionReturnType,
                    modulePrintSet,
                    providerPrintSet,
                    componentProviderMap
                )
            }
        }

        modulePrintSet.forEach { moduleClassDeclaration ->
            logger.warn("modCDWarn: $moduleClassDeclaration")
            propertySpecs += PropertySpec.builder(
                moduleClassDeclaration.simpleName.asString()
                    .replaceFirstChar { it.lowercaseChar() },
                moduleClassDeclaration.toClassName()
            ).initializer("${moduleClassDeclaration.toClassName().simpleName}()").build()
        }

        providerPrintSet.forEach { typeName ->
            logger.warn("ordered typeName: $typeName")
            componentProviderMap[typeName]?.let { providerData ->
                if (providerData.moduleClass != null) {
                    val moduleName = providerData.moduleClass.toClassName().simpleName
                    val providerType = if (providerData.bindsData == null) {
                        providerData.functionDeclaration.returnType?.toTypeName()!!
                    } else {
                        providerData.bindsData.bindsFunction.returnType?.toTypeName()!!
                    }
                    val functionName = providerData.functionDeclaration.simpleName.getShortName()
                    val classTypeName = Provider::class.asTypeName().plusParameter(providerType)
                    val classNameString = "${moduleName}_${functionName}Provider"
                    val tempName = if (providerData.bindsData == null) {
                        functionName.replaceFirstChar { it.lowercaseChar() }
                    } else {
                        providerData.bindsData.bindsFunction.simpleName.getShortName()
                            .replaceFirstChar { it.lowercaseChar() }
                    }
                    val providerName = "${tempName}Provider"
                    providerNameMap[typeName] = providerName
                    val expression = buildString {
                        if (providerData.isScoped) {
                            hasScopedProvider = true
                            append("DoubleCheckProvider(")
                        }
                        append(classNameString)
                        append("(")
                        append(moduleName.replaceFirstChar { it.lowercaseChar() })
                        if (providerData.functionDeclaration.parameters.isNotEmpty()) {
                            append(", ")
                        }
                        providerData.functionDeclaration.parameters.forEachIndexed { index, ksValueParameter ->
                            val paramName = providerNameMap[ksValueParameter.type.toTypeName()]
                            append(paramName)
                            if (index != providerData.functionDeclaration.parameters.size - 1) {
                                append(", ")
                            }
                        }
                        append(")")
                        if (providerData.isScoped) append(")")
                    }

                    propertySpecs += PropertySpec.builder(
                        providerName, classTypeName
                    ).initializer(expression)
                        .build()
                } else if (providerData.constructClass != null) {
                    val constructorName = providerData.constructClass.simpleName.getShortName()
                    val providerType = if (providerData.bindsData == null) {
                        providerData.functionDeclaration.returnType?.toTypeName()!!
                    } else {
                        providerData.bindsData.bindsFunction.returnType?.toTypeName()!!
                    }
                    val classTypeName = Provider::class.asTypeName().plusParameter(providerType)
                    val classNameString = "Construct_${constructorName}Provider"
                    val tempName = if (providerData.bindsData == null) {
                        constructorName.replaceFirstChar { it.lowercaseChar() }
                    } else {
                        providerData.bindsData.bindsFunction.simpleName.getShortName()
                            .replaceFirstChar { it.lowercaseChar() }
                    }
                    val providerName = "${tempName}Provider"
                    providerNameMap[typeName] = providerName
                    val expression = buildString {
                        if (providerData.isScoped) {
                            hasScopedProvider = true
                            append("DoubleCheckProvider(")
                        }
                        append(classNameString)
                        append("(")
                        providerData.functionDeclaration.parameters.forEachIndexed { index, ksValueParameter ->
                            val paramName = providerNameMap[ksValueParameter.type.toTypeName()]
                            append(paramName)
                            if (index != providerData.functionDeclaration.parameters.size - 1) {
                                append(", ")
                            }
                        }
                        append(")")
                        if (providerData.isScoped) append(")")
                    }

                    propertySpecs += PropertySpec.builder(
                        providerName, classTypeName
                    ).initializer(expression)
                        .build()
                }
            }
        }

        functionPrintSet.forEach { (functionName, typeName) ->
            val providerName = providerNameMap[typeName]
            funSpecs += FunSpec.builder(functionName).returns(typeName).addStatement(
                "return ${providerName}.get()"
            ).addModifiers(KModifier.OVERRIDE).build()
        }

        injectPrintSet.forEach { injectPrintData ->
            val implClass = injectPrintData.implClass
            val implParameterName = injectPrintData.implParameterName
            val expression = buildString {
                injectPrintData.memberClasses.forEach { memberInjectData ->
                    val providerName = providerNameMap[memberInjectData.memberTypeName]
                    appendLine("${implParameterName}.${memberInjectData.paramName} = ${providerName}.get()")
                }
            }
            funSpecs += FunSpec.builder("inject").addStatement(
                expression
            ).addParameter(name = implParameterName, type = implClass).addModifiers(KModifier.OVERRIDE).build()
        }

        return this
    }

    @OptIn(KotlinPoetKspPreview::class)
    private fun TypeSpec.Builder.recurseThroughParams(
        paramTypeName: TypeName,
        modulePrintSet: LinkedHashSet<KSClassDeclaration>,
        providerPrintSet: LinkedHashSet<TypeName>,
        componentProviderMap: Map<TypeName, ProviderData>
    ): TypeSpec.Builder {
        val providerData = componentProviderMap[paramTypeName]
        providerData?.functionDeclaration?.parameters?.forEach { ksValueParameter ->
            val typeName = ksValueParameter.type.toTypeName()
            recurseThroughParams(
                typeName,
                modulePrintSet,
                providerPrintSet,
                componentProviderMap
            )
        }

        providerData?.moduleClass?.let { moduleClass ->
            modulePrintSet.add(moduleClass)
        }
        providerPrintSet.add(paramTypeName)

        return this
    }
}