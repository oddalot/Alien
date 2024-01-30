package net.williamott.alien.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration


data class ProviderData(
    val functionDeclaration: KSFunctionDeclaration,
    val moduleClass: KSClassDeclaration?,
    val constructClass: KSClassDeclaration?,
    val bindsData: BindsData?,
    val isScoped: Boolean
)
