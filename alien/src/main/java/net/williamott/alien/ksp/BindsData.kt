package net.williamott.alien.ksp

import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class BindsData(
    val bindsFunction: KSFunctionDeclaration,
    val isScoped: Boolean
)