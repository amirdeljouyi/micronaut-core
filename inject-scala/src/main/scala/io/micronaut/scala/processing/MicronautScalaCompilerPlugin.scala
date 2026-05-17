/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.scala.processing

import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Symbols
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.Types.AppliedType
import dotty.tools.dotc.core.Types.MethodType
import dotty.tools.dotc.core.Types.Type
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.plugins.StandardPlugin
import dotty.tools.dotc.report
import dotty.tools.dotc.transform.Pickler
import dotty.tools.dotc.transform.PostTyper
import io.micronaut.inject.ast.ElementModifier
import io.micronaut.scala.processing.visitor.ScalaAnnotationData
import io.micronaut.scala.processing.visitor.ScalaAnnotationMemberData
import io.micronaut.scala.processing.visitor.ScalaAnnotationTypeData
import io.micronaut.scala.processing.visitor.ScalaClassValueData
import io.micronaut.scala.processing.visitor.ScalaClassData
import io.micronaut.scala.processing.visitor.ScalaFieldData
import io.micronaut.scala.processing.visitor.ScalaMethodData
import io.micronaut.scala.processing.visitor.ScalaParameterData
import io.micronaut.scala.processing.visitor.ScalaProcessingEngine
import io.micronaut.scala.processing.visitor.ScalaPropertyData
import io.micronaut.scala.processing.visitor.ScalaTypeData

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Map as JMap
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

/**
 * Scala 3 compiler plugin that adapts typed Scala symbols to Micronaut's Element API.
 */
final class MicronautScalaCompilerPlugin extends StandardPlugin:

  override val name: String = "micronaut-scala"
  override val description: String = "Generates Micronaut metadata for Scala 3 sources"
  override val optionsHelp: Option[String] = None

  override def initialize(options: List[String])(using Context): List[PluginPhase] =
    val delegate = MicronautScalaCompilerPlugin.delegate(getClass)
    val initializeMethod = delegate.getClass.getMethod("initialize", classOf[List[?]], classOf[Context])
    initializeMethod.invoke(delegate, options, summon[Context]).asInstanceOf[List[PluginPhase]]

private object MicronautScalaCompilerPlugin:

  private val DelegateClassName = "io.micronaut.scala.processing.MicronautScalaCompilerPluginImpl"

  def delegate(pluginClass: Class[?]): AnyRef =
    val codeSource = pluginClass.getProtectionDomain.getCodeSource
    val urls = if codeSource == null then Array.empty[URL] else Array(codeSource.getLocation)
    val classLoader = MicronautScalaPluginClassLoader(urls, pluginClass.getClassLoader)
    Class.forName(DelegateClassName, true, classLoader).getDeclaredConstructor().newInstance()

private final class MicronautScalaPluginClassLoader(urls: Array[URL], parent: ClassLoader)
    extends URLClassLoader(urls, parent):

  private val platformClassLoader = ClassLoader.getPlatformClassLoader
  private val parentLoadsJavaCompiler = canLoadFromParent("javax.lang.model.element.Modifier")

  override protected def loadClass(name: String, resolve: Boolean): Class[?] =
    getClassLoadingLock(name).synchronized {
      var loaded = findLoadedClass(name)
      if loaded == null then
        loaded = loadUncachedClass(name)
      if resolve then
        resolveClass(loaded)
      loaded
    }

  private def loadUncachedClass(name: String): Class[?] =
    if name.startsWith("javax.lang.model.") then
      platformClassLoader.loadClass(name)
    else if isParentFirst(name) then
      loadFromParent(name)
    else
      try
        findClass(name)
      catch
        case _: ClassNotFoundException => loadFromParent(name)

  private def loadFromParent(name: String): Class[?] =
    try
      getParent.loadClass(name)
    catch
      case parentFailure: ClassNotFoundException =>
        try
          platformClassLoader.loadClass(name)
        catch
          case _: ClassNotFoundException =>
            try
              findClass(name)
            catch
              case _: ClassNotFoundException => throw parentFailure

  private def isParentFirst(name: String): Boolean =
    name.startsWith("scala.") ||
      name.startsWith("dotty.") ||
      name.startsWith("java.") ||
      name.startsWith("jdk.") ||
      name.startsWith("io.micronaut.core.") ||
      (parentLoadsJavaCompiler && isParentMicronautApi(name)) ||
      name == "io.micronaut.scala.processing.MicronautScalaCompilerPlugin" ||
      name == "io.micronaut.scala.processing.MicronautScalaCompilerPlugin$"

  private def isParentMicronautApi(name: String): Boolean =
    name.startsWith("io.micronaut.aop.") ||
      name.startsWith("io.micronaut.context.") ||
      name.startsWith("io.micronaut.expressions.context.") ||
      name.startsWith("io.micronaut.inject.annotation.") ||
      name.startsWith("io.micronaut.inject.ast.") ||
      name.startsWith("io.micronaut.inject.visitor.") ||
      name.startsWith("io.micronaut.inject.writer.") ||
      name.startsWith("io.micronaut.sourcegen.")

  private def canLoadFromParent(name: String): Boolean =
    try
      getParent.loadClass(name)
      true
    catch
      case _: ClassNotFoundException =>
        false
      case _: LinkageError =>
        false

final class MicronautScalaCompilerPluginImpl:

  def initialize(options: List[String], context: Context): List[PluginPhase] =
    given Context = context
    val state = ProcessingState(parseOptions(options), summon[Context])
    List(TypeVisitorPhase(state), BeanDefinitionPhase(state))

  private def parseOptions(options: List[String]): JMap[String, String] =
    val parsed = LinkedHashMap[String, String]()
    options.foreach { option =>
      val separator = option.indexOf('=')
      if separator > -1 then
        parsed.put(option.substring(0, separator), option.substring(separator + 1))
      else
        parsed.put(option, "true")
    }
    parsed

private final class ProcessingState(options: JMap[String, String], initialContext: Context):

  private var currentContext: Context = initialContext
  private var engine: ScalaProcessingEngine | Null = null
  private var typeUnitsSeen = 0
  private var typeVisitorsProcessed = false
  private var beanDefinitionsProcessed = false

  def addClasses(classes: List[ScalaClassData])(using Context): Unit =
    currentContext = summon[Context]
    engineInstance.addClasses(classes.asJava)
    typeUnitsSeen += 1
    if !typeVisitorsProcessed && typeUnitsSeen >= unitCount then
      typeVisitorsProcessed = true
      engineInstance.processTypeVisitors()
      processBeanDefinitions()

  def processBeanDefinitions()(using Context): Unit =
    currentContext = summon[Context]
    if !beanDefinitionsProcessed then
      beanDefinitionsProcessed = true
      engineInstance.processBeanDefinitions()

  private def unitCount(using ctx: Context): Int =
    math.max(1, ctx.run.units.size)

  private def engineInstance: ScalaProcessingEngine =
    var current = engine
    if current == null then
      current = ScalaProcessingEngine(
        outputDirectory(using currentContext),
        classpath(using currentContext).asJava,
        options,
        message => report.inform(message)(using currentContext),
        message => report.warning(message)(using currentContext),
        message => report.error(message)(using currentContext)
      )
      engine = current
    current

  private def outputDirectory(using ctx: Context): File =
    val value = ctx.settings.outputDir.valueIn(ctx.settingsState)
    val output = File(value.path)
    output.mkdirs()
    output

  private def classpath(using ctx: Context): List[File] =
    val value = ctx.settings.classpath.valueIn(ctx.settingsState)
    value.split(File.pathSeparator).toList.filter(_.nonEmpty).map(File(_))

private final class TypeVisitorPhase(state: ProcessingState) extends PluginPhase:

  override val phaseName: String = "micronaut-scala-type-visitors"
  override val runsAfter: Set[String] = Set(PostTyper.name)
  override val runsBefore: Set[String] = Set(BeanDefinitionPhase.PhaseName)

  override def run(using Context): Unit =
    val classes = ScalaModelExtractor.collect(summon[Context].compilationUnit)
    state.addClasses(classes)

private object BeanDefinitionPhase:
  val PhaseName = "micronaut-scala-bean-definitions"

private final class BeanDefinitionPhase(state: ProcessingState) extends PluginPhase:

  override val phaseName: String = BeanDefinitionPhase.PhaseName
  override val runsAfter: Set[String] = Set(TypeVisitorPhase(state).phaseName)
  override val runsBefore: Set[String] = Set(Pickler.name)

  override def run(using Context): Unit =
    state.processBeanDefinitions()

private object ScalaModelExtractor:

  private val ScalaPrimitiveNames = Map(
    "scala.Boolean" -> "boolean",
    "scala.Byte" -> "byte",
    "scala.Char" -> "char",
    "scala.Double" -> "double",
    "scala.Float" -> "float",
    "scala.Int" -> "int",
    "scala.Long" -> "long",
    "scala.Short" -> "short",
    "scala.Unit" -> "void"
  )

  private val ScalaClassLiteralAliases = Map(
    "Boolean" -> "boolean",
    "Byte" -> "byte",
    "Char" -> "char",
    "Double" -> "double",
    "Float" -> "float",
    "Int" -> "int",
    "Long" -> "long",
    "Short" -> "short",
    "String" -> classOf[String].getName,
    "Unit" -> "void"
  )

  private val VoidTypeData = ScalaTypeData("void", primitive = true, arrayDimensions = 0, interfaceType = false, java.util.Map.of())

  private case class AnnotationMemberType(
      name: String,
      array: Boolean,
      classType: Boolean,
      enumType: Boolean,
      annotationType: Boolean
  )

  private case class AnnotationDefaults(values: Map[String, Map[String, Object]])

  private case class TypeHierarchy(superType: ScalaTypeData | Null, interfaces: List[ScalaTypeData])

  private val PositionalAnnotationMemberPrefix = "$micronaut$pos$"

  def collect(unit: CompilationUnit)(using Context): List[ScalaClassData] =
    given AnnotationDefaults = AnnotationDefaults(annotationDefaultValues(unit.tpdTree))
    val classes = ListBuffer.empty[ScalaClassData]
    collectTree(unit.tpdTree, classes, null)
    classes.toList

  private def collectTree(tree: tpd.Tree, classes: ListBuffer[ScalaClassData], enclosingTypeName: String | Null)(using Context, AnnotationDefaults): Unit =
    tree match
      case packageDef: tpd.PackageDef =>
        packageDef.stats.foreach(stat => collectTree(stat, classes, null))
      case typeDef: tpd.TypeDef if typeDef.isClassDef =>
        val classData = toClassData(typeDef, enclosingTypeName)
        classData.foreach(classes += _)
        typeDef.rhs match
          case template: tpd.Template =>
            val nestedEnclosingTypeName = classData
              .map(_.name())
              .orElse(companionClassName(typeDef.symbol))
              .orNull
            if nestedEnclosingTypeName != null then
              template.body.foreach(stat => collectTree(stat, classes, nestedEnclosingTypeName))
          case _ =>
      case _ =>

  private def annotationDefaultValues(tree: tpd.Tree)(using Context): Map[String, Map[String, Object]] =
    given AnnotationDefaults = AnnotationDefaults(Map.empty)
    val parameterNames = scala.collection.mutable.LinkedHashMap[String, List[String]]()
    val defaultValues = scala.collection.mutable.LinkedHashMap[String, scala.collection.mutable.LinkedHashMap[String, Object]]()
    collectAnnotationParameterNames(tree, parameterNames)
    collectAnnotationDefaultValues(tree, parameterNames.toMap, defaultValues)
    defaultValues.view.mapValues(_.toMap).toMap

  private def collectAnnotationParameterNames(
      tree: tpd.Tree,
      parameterNames: scala.collection.mutable.LinkedHashMap[String, List[String]]
  )(using Context): Unit =
    tree match
      case packageDef: tpd.PackageDef =>
        packageDef.stats.foreach(collectAnnotationParameterNames(_, parameterNames))
      case typeDef: tpd.TypeDef if typeDef.isClassDef =>
        typeDef.rhs match
          case template: tpd.Template =>
            val symbol = typeDef.symbol
            if isAnnotationSymbol(symbol) then
              parameterNames.put(className(symbol), template.constr.termParamss.flatten.map(_.name.toString))
            template.body.foreach(collectAnnotationParameterNames(_, parameterNames))
          case _ =>
      case _ =>

  private def collectAnnotationDefaultValues(
      tree: tpd.Tree,
      parameterNames: Map[String, List[String]],
      defaultValues: scala.collection.mutable.LinkedHashMap[String, scala.collection.mutable.LinkedHashMap[String, Object]]
  )(using Context, AnnotationDefaults): Unit =
    tree match
      case packageDef: tpd.PackageDef =>
        packageDef.stats.foreach(collectAnnotationDefaultValues(_, parameterNames, defaultValues))
      case typeDef: tpd.TypeDef if typeDef.isClassDef =>
        typeDef.rhs match
          case template: tpd.Template =>
            val ownerName = className(typeDef.symbol).stripSuffix("$")
            parameterNames.get(ownerName).foreach { names =>
              template.body.collect { case method: tpd.DefDef => method }.foreach { method =>
                defaultGetterIndex(method.name.toString).foreach { index =>
                  if index > 0 && index <= names.size then
                    val value = annotationValue(method.rhs)
                    if value != null then
                      val values = defaultValues.getOrElseUpdate(ownerName, scala.collection.mutable.LinkedHashMap[String, Object]())
                      values.put(names(index - 1), value)
                }
              }
            }
            template.body.foreach(collectAnnotationDefaultValues(_, parameterNames, defaultValues))
          case _ =>
      case _ =>

  private def defaultGetterIndex(name: String): Option[Int] =
    val prefix = "$lessinit$greater$default$"
    if name.startsWith(prefix) then
      name.stripPrefix(prefix).toIntOption
    else
      None

  private def toClassData(typeDef: tpd.TypeDef, enclosingTypeName: String | Null)(using Context, AnnotationDefaults): Option[ScalaClassData] =
    val symbol = typeDef.symbol
    if skipClass(symbol) then
      None
    else
      typeDef.rhs match
        case template: tpd.Template =>
          val declarations = symbol.info.decls.toList
          val allMethods = template.body.collect { case method: tpd.DefDef => method }
          val methodByName = LinkedHashMap[String, ScalaMethodData]()
          allMethods.foreach { method =>
            if !skipAccessorCandidate(method.symbol) then
              methodByName.put(method.name.toString, methodData(method, constructor = false, owner = symbol))
          }
          declarations.foreach { declaration =>
            val declarationName = declaration.name.toString
            if isPropertyDeclaration(declaration, declarationName) || isPropertySetterDeclaration(declaration, declarationName) then
              methodByName.put(declarationName, methodData(declaration))
          }
          val methods = allMethods
            .filterNot(method => skipMethod(method.symbol))
            .map(method => methodByName.get(method.name.toString))
            .filter(_ != null)
          val fields = template.body.collect {
            case field: tpd.ValDef if !skipField(field.symbol) => fieldData(field)
          }
          val enumConstants = enumConstantSymbols(symbol)
            .map(enumConstantFieldData(_, symbol))
          val allFields = (fields ++ enumConstants).distinctBy(_.name())
          val constructors = List(methodData(template.constr, constructor = true, owner = symbol))
          val constructorProps = constructorProperties(template.constr, methodByName, allFields)
          val properties = constructorProps ++ bodyProperties(declarations, methodByName, allFields, constructorProps.map(_.name).toSet)
          val parents = symbol.info.parents
            .filterNot(parent => typeName(parent) == classOf[Object].getName)
            .map(typeData)
          val superType = parents.find(parent => !parent.interfaceType()).orNull
          val interfaces = parents.filter(_.interfaceType())
          Some(ScalaClassData(
            className(symbol),
            annotations(symbol).asJava,
            modifiers(symbol).asJava,
            isInterfaceSymbol(symbol),
            hasFlag(symbol, Flags.Enum),
            superType,
            interfaces.asJava,
            constructors.asJava,
            methods.asJava,
            allFields.asJava,
            properties.asJava,
            enclosingTypeName,
            typeDef
          ))
        case _ =>
          None

  private def constructorProperties(
      constructor: tpd.DefDef,
      methods: LinkedHashMap[String, ScalaMethodData],
      fields: List[ScalaFieldData]
  )(using Context, AnnotationDefaults): List[ScalaPropertyData] =
    constructor.termParamss.flatten
      .filter { param =>
        val propertyName = param.name.toString
        hasFlag(param.symbol, Flags.ParamAccessor) ||
          hasFlag(param.symbol, Flags.CaseAccessor) ||
          methods.containsKey(propertyName) ||
          fields.exists(_.name == propertyName)
      }
      .map { param =>
        val propertyName = param.name.toString
        val readMethod = methods.get(propertyName)
        val writeMethod = methods.get(propertyName + "_=")
        val field = fields.find(_.name == propertyName).orNull
        ScalaPropertyData(
          propertyName,
          typeData(param.tpt.tpe),
          readMethod,
          writeMethod,
          field,
          annotations(param.symbol).asJava,
          modifiers(param.symbol).asJava,
          param
        )
      }

  private def bodyProperties(
      declarations: List[Symbol],
      methods: LinkedHashMap[String, ScalaMethodData],
      fields: List[ScalaFieldData],
      knownProperties: Set[String]
  )(using Context): List[ScalaPropertyData] =
    val fieldsByName = fields.map(field => field.name -> field).toMap
    val added = LinkedHashSet[String]()
    knownProperties.foreach(added.add)
    val properties = ListBuffer.empty[ScalaPropertyData]
    declarations.foreach { declaration =>
      val propertyName = declaration.name.toString
      if isPropertyDeclaration(declaration, propertyName) && !added.contains(propertyName) then
        val readMethod = methods.get(propertyName)
        val writeMethod = methods.get(propertyName + "_=")
        if readMethod != null then
          val field = fieldsByName.getOrElse(propertyName, null)
          properties += ScalaPropertyData(
            propertyName,
            if field == null then readMethod.returnType() else field.`type`(),
            readMethod,
            writeMethod,
            field,
            propertyAnnotations(readMethod, field),
            propertyModifiers(readMethod, writeMethod, field),
            declaration
          )
          added.add(propertyName)
      }
    properties.toList

  private def isPropertyDeclaration(symbol: Symbol, name: String)(using Context): Boolean =
    name.nonEmpty &&
      !name.contains("$") &&
      !name.endsWith("_=") &&
      !name.startsWith("<") &&
      symbol.isTerm &&
      !symbol.denot.isConstructor &&
      !hasFlag(symbol, Flags.Method) &&
      !hasFlag(symbol, Flags.Module) &&
      !hasFlag(symbol, Flags.Artifact)

  private def isPropertySetterDeclaration(symbol: Symbol, name: String)(using Context): Boolean =
    name.endsWith("_=") &&
      symbol.isTerm &&
      hasFlag(symbol, Flags.Accessor)

  private def propertyAnnotations(readMethod: ScalaMethodData, field: ScalaFieldData | Null): java.util.List[ScalaAnnotationData] =
    if readMethod != null && !readMethod.annotations().isEmpty then
      readMethod.annotations()
    else if field != null then
      field.annotations()
    else
      java.util.List.of()

  private def propertyModifiers(
      readMethod: ScalaMethodData,
      writeMethod: ScalaMethodData | Null,
      field: ScalaFieldData | Null
  ): java.util.Set[ElementModifier] =
    if readMethod != null then
      readMethod.modifiers()
    else if writeMethod != null then
      writeMethod.modifiers()
    else if field != null then
      field.modifiers()
    else
      java.util.Set.of(ElementModifier.PUBLIC)

  private def methodData(method: tpd.DefDef, constructor: Boolean, owner: Symbol)(using Context, AnnotationDefaults): ScalaMethodData =
    val returnType =
      if constructor then
        ScalaTypeData(className(owner), primitive = false, arrayDimensions = 0, interfaceType = false, java.util.Map.of())
      else
        methodReturnType(method.name.toString, method.tpt.tpe)
    ScalaMethodData(
      if constructor then "<init>" else methodName(method.name.toString),
      returnType,
      method.termParamss.flatten.map(parameterData).asJava,
      annotations(method.symbol).asJava,
      modifiers(method.symbol).asJava,
      constructor,
      method
    )

  private def methodData(symbol: Symbol)(using Context, AnnotationDefaults): ScalaMethodData =
    symbol.info match
      case methodType: MethodType =>
        ScalaMethodData(
          methodName(symbol.name.toString),
          methodReturnType(symbol.name.toString, methodType.resultType),
          methodType.paramNames.zip(methodType.paramInfos)
            .map { case (name, info) => parameterData(name.toString, info, symbol) }
            .asJava,
          annotations(symbol).asJava,
          modifiers(symbol).asJava,
          constructor = false,
          symbol
        )
      case info =>
        ScalaMethodData(
          methodName(symbol.name.toString),
          methodReturnType(symbol.name.toString, info),
          java.util.List.of(),
          annotations(symbol).asJava,
          modifiers(symbol).asJava,
          constructor = false,
          symbol
        )

  private def methodName(name: String): String =
    if name.endsWith("_=") then
      name.stripSuffix("_=") + "_$eq"
    else
      name

  private def methodReturnType(name: String, tpe: Type)(using Context, AnnotationDefaults): ScalaTypeData =
    if name.endsWith("_=") then
      VoidTypeData
    else
      typeData(tpe)

  private def fieldData(field: tpd.ValDef)(using Context, AnnotationDefaults): ScalaFieldData =
    ScalaFieldData(
      field.name.toString,
      typeData(field.tpt.tpe),
      annotations(field.symbol).asJava,
      modifiers(field.symbol).asJava,
      isEnumConstant(field.symbol),
      field
    )

  private def enumConstantFieldData(symbol: Symbol, owner: Symbol)(using Context, AnnotationDefaults): ScalaFieldData =
    ScalaFieldData(
      symbol.name.toString,
      ScalaTypeData(className(owner), primitive = false, arrayDimensions = 0, interfaceType = false, java.util.Map.of()),
      annotations(symbol).asJava,
      java.util.Set.of(ElementModifier.PUBLIC, ElementModifier.STATIC, ElementModifier.FINAL),
      true,
      symbol
    )

  private def enumConstantSymbols(symbol: Symbol)(using Context): List[Symbol] =
    val symbols = if hasFlag(symbol, Flags.Enum) then
      val companion = symbol.companionModule
      if companion == Symbols.NoSymbol then symbol.info.decls.toList else companion.info.decls.toList
    else
      symbol.info.decls.toList
    symbols.filter(isEnumConstantField)

  private def isEnumConstantField(symbol: Symbol)(using Context): Boolean =
    isEnumConstant(symbol) &&
      symbol.isTerm &&
      !symbol.denot.isConstructor &&
      !symbol.name.toString.startsWith("<") &&
      !hasFlag(symbol, Flags.Method)

  private def parameterData(parameter: tpd.ValDef)(using Context, AnnotationDefaults): ScalaParameterData =
    ScalaParameterData(
      parameter.name.toString,
      typeData(parameter.tpt.tpe),
      annotations(parameter.symbol).asJava,
      parameter
    )

  private def parameterData(name: String, tpe: Type, nativeType: Object)(using Context, AnnotationDefaults): ScalaParameterData =
    ScalaParameterData(
      name,
      typeData(tpe),
      java.util.List.of(),
      nativeType
    )

  private def typeData(tpe: Type)(using Context, AnnotationDefaults): ScalaTypeData =
    typeData(tpe, Set.empty)

  private def typeData(tpe: Type, visitedTypes: Set[String])(using Context, AnnotationDefaults): ScalaTypeData =
    val widened = tpe.widenDealias
    widened match
      case applied: AppliedType if typeName(applied.tycon) == "scala.Array" && applied.args.nonEmpty =>
        val componentType = typeData(applied.args.head, visitedTypes)
        componentType.withArrayDimensions(componentType.arrayDimensions + 1).asInstanceOf[ScalaTypeData]
      case applied: AppliedType =>
        val rawName = typeName(applied.tycon)
        val primitiveName = ScalaPrimitiveNames.get(rawName)
        val name = primitiveName.getOrElse(rawName)
        val symbol = applied.tycon.classSymbol
        val interfaceType = isInterfaceSymbol(symbol)
        val hierarchy = typeHierarchy(symbol, name, primitiveName.isDefined, visitedTypes)
        ScalaTypeData(name, primitiveName.isDefined, 0, interfaceType, typeArguments(symbol, applied.args, visitedTypes), hierarchy.superType, hierarchy.interfaces.asJava, annotations(symbol).asJava, symbol)
      case _ =>
        val rawName = typeName(widened)
        val primitiveName = ScalaPrimitiveNames.get(rawName)
        val name = primitiveName.getOrElse(rawName)
        val symbol = widened.classSymbol
        val interfaceType = isInterfaceSymbol(symbol)
        val hierarchy = typeHierarchy(symbol, name, primitiveName.isDefined, visitedTypes)
        ScalaTypeData(name, primitiveName.isDefined, 0, interfaceType, java.util.Map.of(), hierarchy.superType, hierarchy.interfaces.asJava, annotations(symbol).asJava, symbol)

  private def typeHierarchy(symbol: Symbol, name: String, primitive: Boolean, visitedTypes: Set[String])(using Context, AnnotationDefaults): TypeHierarchy =
    if primitive || symbol == Symbols.NoSymbol || visitedTypes.contains(name) then
      TypeHierarchy(null, Nil)
    else
      val nextVisited = visitedTypes + name
      val parents = symbol.info.parents
        .filterNot(parent => typeName(parent) == classOf[Object].getName)
        .map(parent => typeData(parent, nextVisited))
      TypeHierarchy(
        parents.find(parent => !parent.interfaceType()).orNull,
        parents.filter(_.interfaceType())
      )

  private def typeArguments(symbol: Symbol, arguments: List[Type], visitedTypes: Set[String])(using Context, AnnotationDefaults): java.util.Map[String, ScalaTypeData] =
    if symbol == Symbols.NoSymbol || arguments.isEmpty then
      java.util.Map.of()
    else
      val converted = LinkedHashMap[String, ScalaTypeData]()
      symbol.typeParams.zip(arguments).foreach { case (parameter, argument) =>
        converted.put(parameter.name.toString, typeData(argument, visitedTypes))
      }
      converted

  private def typeName(tpe: Type)(using Context): String =
    val symbol = tpe.classSymbol
    if symbol != Symbols.NoSymbol then
      className(symbol)
    else
      tpe.show

  private def className(symbol: Symbol)(using Context): String =
    val binaryName = symbol.denot.binaryClassName
    if binaryName == null || binaryName.isBlank then
      symbol.showFullName
    else
      binaryName

  private def companionClassName(symbol: Symbol)(using Context): Option[String] =
    if hasFlag(symbol, Flags.ModuleClass) then
      val name = className(symbol)
      if name.endsWith("$") then Some(name.stripSuffix("$")) else None
    else
      None

  private def annotations(symbol: Symbol)(using Context, AnnotationDefaults): List[ScalaAnnotationData] =
    if symbol == Symbols.NoSymbol then
      Nil
    else
      symbol.denot.annotations.map(annotationData(_, Set.empty))

  private def annotationData(
      annotation: dotty.tools.dotc.core.Annotations.Annotation,
      visitedAnnotationTypes: Set[String]
  )(using Context, AnnotationDefaults): ScalaAnnotationData =
    val symbol = annotation.symbol
    val name = className(symbol)
    val annotationType = annotationTypeData(symbol, visitedAnnotationTypes)
    ScalaAnnotationData(
      name,
      annotationValues(annotation, annotationType).asInstanceOf[JMap[CharSequence, Object]],
      annotationType
    )

  private def annotationTypeData(symbol: Symbol, visitedAnnotationTypes: Set[String])(using Context, AnnotationDefaults): ScalaAnnotationTypeData | Null =
    if !isAnnotationSymbol(symbol) then
      null
    else
      val name = className(symbol)
      if visitedAnnotationTypes.contains(name) then
        ScalaAnnotationTypeData(name, java.util.List.of(), java.util.Map.of(), null, null, symbol)
      else
        val nextVisited = visitedAnnotationTypes + name
        val annotations = symbol.denot.annotations
          .filterNot(annotation => className(annotation.symbol) == name)
          .map(annotationData(_, nextVisited))
        val members = annotationMembers(symbol)
        ScalaAnnotationTypeData(
          name,
          annotations.asJava,
          members,
          retentionPolicyName(annotations).orNull,
          repeatableContainerName(annotations).orNull,
          symbol
        )

  private def annotationMembers(symbol: Symbol)(using Context, AnnotationDefaults): LinkedHashMap[String, ScalaAnnotationMemberData] =
    val members = LinkedHashMap[String, ScalaAnnotationMemberData]()
    val defaults = summon[AnnotationDefaults].values.getOrElse(className(symbol), Map.empty)
    symbol.info.decls.toList.foreach { member =>
      val memberName = member.name.toString
      if isAnnotationMember(member, memberName) then
        val memberType = annotationMemberType(member)
        members.put(
          memberName,
          ScalaAnnotationMemberData(
            memberName,
            annotations(member).asJava,
            defaults.getOrElse(memberName, null),
            memberType.name,
            memberType.array,
            memberType.classType,
            memberType.enumType,
            memberType.annotationType,
            member
          )
        )
    }
    members

  private def isAnnotationMember(symbol: Symbol, name: String)(using Context): Boolean =
    name.nonEmpty &&
      !name.contains("$") &&
      !name.startsWith("<") &&
      symbol.isTerm &&
      !symbol.denot.isConstructor &&
      !hasFlag(symbol, Flags.Module) &&
      !hasFlag(symbol, Flags.Synthetic) &&
      !hasFlag(symbol, Flags.Artifact)

  private def annotationMemberType(symbol: Symbol)(using Context): AnnotationMemberType =
    val resultType = symbol.info match
      case methodType: MethodType => methodType.resultType
      case info => info
    annotationMemberType(resultType, array = false)

  private def annotationMemberType(tpe: Type, array: Boolean)(using Context): AnnotationMemberType =
    val widened = tpe.widenDealias
    widened match
      case applied: AppliedType if typeName(applied.tycon) == "scala.Array" && applied.args.nonEmpty =>
        annotationMemberType(applied.args.head, array = true)
      case _ =>
        val name = typeName(widened)
        val symbol = widened.classSymbol
        AnnotationMemberType(
          name,
          array,
          name == classOf[Class[?]].getName,
          symbol != Symbols.NoSymbol && hasFlag(symbol, Flags.Enum),
          isAnnotationSymbol(symbol)
        )

  private def retentionPolicyName(annotations: List[ScalaAnnotationData]): Option[String] =
    annotations.find(_.name() == classOf[java.lang.annotation.Retention].getName)
      .flatMap(annotation => annotationValue(annotation, "value"))
      .map(_.toString)

  private def repeatableContainerName(annotations: List[ScalaAnnotationData]): Option[String] =
    annotations.find(_.name() == classOf[java.lang.annotation.Repeatable].getName)
      .flatMap(annotation => annotationValue(annotation, "value"))
      .map(classValueName)

  private def annotationValue(annotation: ScalaAnnotationData, memberName: String): Option[Object] =
    annotation.values().asScala.collectFirst {
      case (key, value) if memberName.contentEquals(key) => value
    }

  private def classValueName(value: Object): String =
    value match
      case classValueData: ScalaClassValueData => classValueData.name()
      case other => other.toString

  private def annotationValues(
      annotation: dotty.tools.dotc.core.Annotations.Annotation,
      annotationType: ScalaAnnotationTypeData | Null
  )(using Context, AnnotationDefaults): JMap[String, Object] =
    normalizeAnnotationArgumentValues(annotationArgumentValues(annotation.arguments), annotationType)

  private def annotationArgumentValues(arguments: List[tpd.Tree])(using Context, AnnotationDefaults): JMap[String, Object] =
    val values = LinkedHashMap[String, Object]()
    var positionalIndex = 0
    arguments.foreach {
      case named: tpd.NamedArg =>
        val value = annotationValue(named.arg)
        if value != null then
          values.put(named.name.toString, value)
      case tree =>
        val memberName = PositionalAnnotationMemberPrefix + positionalIndex
        val value = annotationValue(tree)
        if value != null then
          values.put(memberName, value)
        positionalIndex += 1
    }
    values

  private def normalizeAnnotationArgumentValues(
      values: JMap[String, Object],
      annotationType: ScalaAnnotationTypeData | Null
  ): JMap[String, Object] =
    if values.isEmpty then
      values
    else if annotationType == null then
      val normalized = LinkedHashMap[String, Object]()
      values.asScala.foreach { case (key, value) =>
        normalized.put(legacyPositionalAnnotationMemberName(key).getOrElse(key), value)
      }
      normalized
    else
      val normalized = LinkedHashMap[String, Object]()
      val memberNames = annotationType.members().keySet().asScala.toList
      values.asScala.foreach { case (key, value) =>
        val defaultIndex = defaultGetterReferenceIndex(value, annotationType.name())
        val memberName = defaultIndex
          .flatMap(index => memberNames.lift(index - 1))
          .orElse(positionalAnnotationMemberName(key, memberNames))
          .getOrElse(key)
        val memberValue = defaultIndex
          .map(_ => annotationType.members().get(memberName))
          .filter(_ != null)
          .map(_.defaultValue())
          .filter(_ != null)
          .getOrElse(value)
        normalized.put(memberName, memberValue)
      }
      normalized

  private def positionalAnnotationMemberName(key: String, memberNames: List[String]): Option[String] =
    if memberNames.isEmpty || !key.startsWith(PositionalAnnotationMemberPrefix) then
      None
    else
      key.stripPrefix(PositionalAnnotationMemberPrefix).toIntOption.flatMap(index => memberNames.lift(index))

  private def legacyPositionalAnnotationMemberName(key: String): Option[String] =
    if !key.startsWith(PositionalAnnotationMemberPrefix) then
      None
    else
      key.stripPrefix(PositionalAnnotationMemberPrefix).toIntOption.map {
        case 0 => "value"
        case index => "value" + index
      }

  private def defaultGetterReferenceIndex(value: Object, annotationName: String): Option[Int] =
    value match
      case rendered: String =>
        val prefix = annotationName + ".$lessinit$greater$default$"
        if rendered.startsWith(prefix) then
          rendered.stripPrefix(prefix).toIntOption
        else
          None
      case _ =>
        None

  private def annotationValue(tree: tpd.Tree)(using Context, AnnotationDefaults): Object | Null =
    arrayLiteralValues(tree) match
      case Some(values) =>
        values
      case None =>
        classLiteralValue(tree) match
          case Some(value) =>
            value
          case None =>
            nestedAnnotationValue(tree) match
              case Some(value) =>
                value
              case None =>
                tree match
                  case literal: tpd.Literal =>
                    val value = literal.const.value
                    if value == null then
                      null
                    else
                      renderedClassLiteralValue(value.toString).map(name => classValueData(name)).getOrElse(value.asInstanceOf[Object])
                  case select: tpd.Select if isEnumConstant(select.symbol) =>
                    select.name.toString
                  case ident: tpd.Ident if ident.name.toString == "_" =>
                    null
                  case ident: tpd.Ident if isEnumConstant(ident.symbol) =>
                    ident.name.toString
                  case _ =>
                    renderedClassLiteralValue(tree.show).map(name => classValueData(name)).getOrElse(tree.show)

  private def arrayLiteralValues(tree: tpd.Tree)(using Context, AnnotationDefaults): Option[Object] =
    tree match
      case seq: tpd.SeqLiteral =>
        Some(annotationArray(seq.elems.map(annotationValue).filter(_ != null)))
      case typed: tpd.Typed =>
        arrayLiteralValues(typed.expr)
      case apply: tpd.Apply =>
        apply.args.iterator.map(arrayLiteralValues).collectFirst { case Some(values) => values }
          .orElse(arrayLiteralValues(apply.fun))
      case typeApply: tpd.TypeApply =>
        arrayLiteralValues(typeApply.fun)
      case _ =>
        None

  private def annotationArray(values: List[Object])(using Context, AnnotationDefaults): Object =
    val normalized = values.map {
      case value: String => renderedClassLiteralValue(value).map(name => classValueData(name)).getOrElse(value)
      case value => value
    }
    if normalized.forall(_.isInstanceOf[String]) then
      normalized.map(_.asInstanceOf[String]).toArray[String]
    else if normalized.forall(_.isInstanceOf[ScalaAnnotationData]) then
      normalized.map(_.asInstanceOf[ScalaAnnotationData]).toArray[ScalaAnnotationData]
    else
      normalized.toArray

  private def nestedAnnotationValue(tree: tpd.Tree)(using Context, AnnotationDefaults): Option[ScalaAnnotationData] =
    tree match
      case typed: tpd.Typed =>
        nestedAnnotationValue(typed.expr)
      case apply: tpd.Apply =>
        val symbol = annotationClassSymbol(apply)
        if isAnnotationSymbol(symbol) then
          val annotationType = annotationTypeData(symbol, Set.empty)
          Some(
            ScalaAnnotationData(
              className(symbol),
              annotationValuesFromArguments(apply.args, annotationType).asInstanceOf[JMap[CharSequence, Object]],
              annotationType
            )
          )
        else
          nestedAnnotationValue(apply.fun)
      case typeApply: tpd.TypeApply =>
        nestedAnnotationValue(typeApply.fun)
      case _ =>
        None

  private def annotationClassSymbol(tree: tpd.Tree)(using Context): Symbol =
    tree match
      case typed: tpd.Typed =>
        annotationClassSymbol(typed.expr)
      case apply: tpd.Apply =>
        annotationClassSymbol(apply.fun)
      case typeApply: tpd.TypeApply =>
        annotationClassSymbol(typeApply.fun)
      case select: tpd.Select =>
        annotationClassSymbol(select.qualifier)
      case newTree: tpd.New =>
        newTree.tpt.tpe.classSymbol
      case _ =>
        tree.tpe.classSymbol

  private def isClassOf(typeApply: tpd.TypeApply)(using Context): Boolean =
    typeApply.args.nonEmpty && typeApply.fun.symbol != Symbols.NoSymbol && typeApply.fun.symbol.showFullName == "scala.Predef.classOf"

  private def annotationValuesFromArguments(
      arguments: List[tpd.Tree],
      annotationType: ScalaAnnotationTypeData | Null
  )(using Context, AnnotationDefaults): JMap[String, Object] =
    normalizeAnnotationArgumentValues(annotationArgumentValues(arguments), annotationType)

  private def classLiteralValue(tree: tpd.Tree)(using Context, AnnotationDefaults): Option[ScalaClassValueData] =
    tree match
      case typeApply: tpd.TypeApply if isClassOf(typeApply) =>
        val name = typeName(typeApply.args.head.tpe)
        Some(classValueData(name, typeApply.args.head.tpe.classSymbol))
      case typed: tpd.Typed =>
        classLiteralValue(typed.expr)
      case apply: tpd.Apply =>
        classLiteralValue(apply.fun)
      case typeApply: tpd.TypeApply =>
        classLiteralValue(typeApply.fun)
      case _ =>
        renderedClassLiteralValue(tree.show).map(name => classValueData(name))

  private def classValueData(name: String, fallback: Symbol = Symbols.NoSymbol)(using Context, AnnotationDefaults): ScalaClassValueData =
    val classSymbol =
      if isAnnotationSymbol(fallback) then fallback
      else classSymbolForName(name)
    val annotationType =
      if isAnnotationSymbol(classSymbol) then annotationTypeData(classSymbol, Set.empty)
      else null
    ScalaClassValueData(name, annotationType)

  private def renderedClassLiteralValue(rendered: String): Option[String] =
    val start = rendered.indexOf("classOf")
    val open = if start > -1 then rendered.indexOf('[', start) else -1
    val end = if open > -1 then rendered.indexOf(']', open) else -1
    if start > -1 && open > start && end > open then
      val rawName = rendered.substring(open + 1, end)
      val name = rawName
        .replaceAll("\u001B\\[[;\\d]*m", "")
        .replaceAll("\\[[;\\d]*m", "")
        .replace('/', '.')
        .replaceAll("[^A-Za-z0-9_.$]", "")
      Some(ScalaClassLiteralAliases.getOrElse(name, name))
    else
      None

  private def isEnumConstant(symbol: Symbol)(using Context): Boolean =
    symbol != Symbols.NoSymbol &&
      (hasFlag(symbol, Flags.EnumValue) ||
        hasFlag(symbol, Flags.JavaEnumValue) ||
        hasFlag(symbol, Flags.EnumCase))

  private def modifiers(symbol: Symbol)(using Context): Set[ElementModifier] =
    val modifiers = LinkedHashSet[ElementModifier]()
    if hasFlag(symbol, Flags.Private) then modifiers.add(ElementModifier.PRIVATE)
    if hasFlag(symbol, Flags.Protected) then modifiers.add(ElementModifier.PROTECTED)
    if hasFlag(symbol, Flags.Deferred) || hasFlag(symbol, Flags.Abstract) then modifiers.add(ElementModifier.ABSTRACT)
    if hasFlag(symbol, Flags.Final) then modifiers.add(ElementModifier.FINAL)
    if hasFlag(symbol, Flags.JavaStatic) || hasFlag(symbol, Flags.Module) then modifiers.add(ElementModifier.STATIC)
    if !modifiers.contains(ElementModifier.PRIVATE) && !modifiers.contains(ElementModifier.PROTECTED) then modifiers.add(ElementModifier.PUBLIC)
    modifiers.asScala.toSet

  private def isAnnotationSymbol(symbol: Symbol)(using Context): Boolean =
    symbol != Symbols.NoSymbol &&
      (symbol.denot.isAnnotation || hasFlag(symbol, Flags.JavaAnnotation))

  private def isInterfaceSymbol(symbol: Symbol)(using Context): Boolean =
    symbol != Symbols.NoSymbol &&
      (hasFlag(symbol, Flags.Trait) || (hasFlag(symbol, Flags.JavaDefined) && hasFlag(symbol, Flags.JavaInterface)))

  private def classSymbolForName(name: String)(using Context): Symbol =
    val symbol = Symbols.getClassIfDefined(name)
    if symbol != Symbols.NoSymbol then
      symbol
    else
      Symbols.getClassIfDefined(name.replace('$', '.'))

  private def skipClass(symbol: Symbol)(using Context): Boolean =
    symbol == Symbols.NoSymbol ||
      hasFlag(symbol, Flags.ModuleClass) ||
      hasFlag(symbol, Flags.PackageClass) ||
      hasFlag(symbol, Flags.Synthetic) ||
      hasFlag(symbol, Flags.Artifact) ||
      symbol.denot.isAnnotation

  private def skipMethod(symbol: Symbol)(using Context): Boolean =
    symbol == Symbols.NoSymbol ||
      symbol.denot.isConstructor ||
      hasFlag(symbol, Flags.Synthetic) ||
      hasFlag(symbol, Flags.Artifact) ||
      hasFlag(symbol, Flags.Accessor)

  private def skipAccessorCandidate(symbol: Symbol)(using Context): Boolean =
    symbol == Symbols.NoSymbol ||
      symbol.denot.isConstructor ||
      hasFlag(symbol, Flags.Synthetic) ||
      hasFlag(symbol, Flags.Artifact)

  private def skipField(symbol: Symbol)(using Context): Boolean =
    symbol == Symbols.NoSymbol ||
      hasFlag(symbol, Flags.Synthetic) ||
      hasFlag(symbol, Flags.Artifact)

  private def hasFlag(symbol: Symbol, flag: Flags.FlagSet)(using Context): Boolean =
    symbol != Symbols.NoSymbol && symbol.denot.isOneOf(flag)
