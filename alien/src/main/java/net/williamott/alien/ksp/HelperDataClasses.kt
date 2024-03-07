package net.williamott.alien.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class ProviderData(
    val functionDeclaration: KSFunctionDeclaration,
    val moduleClass: KSClassDeclaration?,
    val constructClass: KSClassDeclaration?,
    val bindsData: BindsData?,
    val isScoped: Boolean
)

data class BindsData(
    val bindsFunction: KSFunctionDeclaration,
    val isScoped: Boolean
)

data class InjectData(
    val implFile: KSFile,
    val implClass: ClassName,
    val memberClasses: List<MemberInjectData>
)

data class MemberInjectData(
    val paramName: String,
    val memberTypeName: TypeName
)

data class InjectPrintData(
    val implParameterName: String,
    val implClass: TypeName,
    val memberClasses: List<MemberInjectData>
)