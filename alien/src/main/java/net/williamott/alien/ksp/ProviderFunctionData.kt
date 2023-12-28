package net.williamott.alien.ksp

import com.squareup.kotlinpoet.TypeName

data class ProviderFunctionData(
    val functionName: String,
    val returnType: TypeName
)