package net.williamott.alien.ksp

import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class ConstructData (
    val functionDeclaration: KSFunctionDeclaration
)