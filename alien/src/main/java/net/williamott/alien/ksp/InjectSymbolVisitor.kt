package net.williamott.alien.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class InjectSymbolVisitor(
    private val logger: KSPLogger,
    private val injectMap: MutableMap<ClassName, InjectData>
)  : KSVisitorVoid() {
    @OptIn(KotlinPoetKspPreview::class)
    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
        val propertyParentClass = (property.parent as KSClassDeclaration).toClassName()
        val propertyName = property.simpleName.getShortName()
        val propertyTypeName = property.type.toTypeName()
        val injectData = injectMap.getOrPut(propertyParentClass) { InjectData(propertyParentClass, memberClasses = listOf()) }
        injectMap[propertyParentClass] = injectData.copy(memberClasses = injectData.memberClasses + MemberInjectData(propertyName, propertyTypeName))
    }
}