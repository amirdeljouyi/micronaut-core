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

import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Requirements
import io.micronaut.core.annotation.TypeHint
import io.micronaut.inject.ast.ClassElement
import io.micronaut.inject.ast.ElementQuery
import io.micronaut.inject.ast.EnumElement
import io.micronaut.inject.validation.RequiresValidation
import io.micronaut.scala.processing.test.AbstractScalaTypeElementSpec
import io.micronaut.scala.processing.test.ScalaEnumConstantCaptureVisitor
import jakarta.inject.Named
import jakarta.inject.Qualifier
import jakarta.inject.Singleton
import jakarta.validation.Constraint
import jakarta.validation.constraints.Min

class ScalaElementCompletenessSpec extends AbstractScalaTypeElementSpec {

    void "exposes class body val and var properties"() {
        when:
        def element = buildClassElement('example.Worker', '''
package example

class Worker:
  val name: String = "worker"
  var started: Boolean = false
''')
        def properties = element.getBeanProperties().collectEntries { [(it.name): it] }

        then:
        properties.keySet() == ['name', 'started'] as Set
        properties.name.type.name == String.name
        properties.name.readMethod.present
        !properties.name.writeMethod.present
        properties.started.type.name == Boolean.TYPE.name
        properties.started.readMethod.present
        !properties.started.readMethod.get().returnType.isVoid()
        properties.started.writeMethod.present
        properties.started.writeMethod.get().returnType.isVoid()
    }

    void "propagates field-targeted validation annotations to Scala properties"() {
        when:
        def element = buildClassElement('example.EngineConfig', '''
package example

import jakarta.validation.constraints.Min
import scala.annotation.meta.field

class EngineConfig:
  @(Min @field)(1L)
  var cylinders: Int = 0
''')
        def property = element.getBeanProperties().find { it.name == 'cylinders' }

        then:
        property != null
        property.field.present
        property.field.get().hasAnnotation(Min)
        property.field.get().hasStereotype(Constraint)
        property.field.get().hasAnnotation(RequiresValidation)
        property.hasAnnotation(Min)
        property.hasStereotype(Constraint)
        property.hasAnnotation(RequiresValidation)
    }

    void "exposes generic property type arguments"() {
        when:
        def element = buildClassElement('example.Holder', '''
package example

class Holder(
  val names: java.util.List[String],
  val lookup: java.util.Map[String, java.lang.Integer]
)
''')
        def properties = element.getBeanProperties().collectEntries { [(it.name): it] }

        then:
        properties.names.type.name == 'java.util.List'
        properties.names.type.typeArguments.keySet() == ['E'] as Set
        properties.names.type.typeArguments.E.name == String.name
        properties.lookup.type.name == 'java.util.Map'
        properties.lookup.type.typeArguments.keySet() == ['K', 'V'] as Set
        properties.lookup.type.typeArguments.K.name == String.name
        properties.lookup.type.typeArguments.V.name == Integer.name
    }

    void "exposes traits as interfaces and resolves inherited assignability"() {
        when:
        def element = buildClassElement('example.Vehicle', '''
package example

trait Machine
trait Engine extends Machine
class Vehicle extends Engine
''')

        then:
        !element.superType.present
        element.interfaces*.name == ['example.Engine']
        element.isAssignable('example.Engine')
        element.isAssignable('example.Machine')
    }

    void "exposes inherited Java interfaces from compiler symbols"() {
        when:
        def element = buildClassElement('example.TimingInterceptor', '''
package example

import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext

class TimingInterceptor extends MethodInterceptor[AnyRef, Object]:
  override def intercept(context: MethodInvocationContext[AnyRef, Object]): Object =
    context.proceed()
''')
        def methodInterceptor = element.interfaces.first()

        then:
        !element.interface
        element.interfaces*.name == ['io.micronaut.aop.MethodInterceptor']
        methodInterceptor.interfaces*.name == ['io.micronaut.aop.Interceptor']
        element.isAssignable('io.micronaut.aop.MethodInterceptor')
        element.isAssignable('io.micronaut.aop.Interceptor')
    }

    void "exposes Scala inner classes through enclosed elements"() {
        when:
        def element = buildClassElement('example.Outer', '''
package example

class Outer:
  class Inner
''')
        def inner = element.getEnclosedElements(ElementQuery.of(ClassElement)).first()

        then:
        inner.name == 'example.Outer$Inner'
        inner.inner
        inner.enclosingType.get().name == 'example.Outer'
    }

    void "exposes companion object nested classes through companion enclosed elements"() {
        when:
        def element = buildClassElement('example.Outer', '''
package example

class Outer
object Outer:
  class Nested
''')
        def nested = element.getEnclosedElements(ElementQuery.of(ClassElement)).first()

        then:
        nested.name == 'example.Outer$Nested'
        nested.inner
        nested.enclosingType.get().name == 'example.Outer'
    }

    void "exposes Scala enum constants through enum elements"() {
        when:
        def element = buildClassElement('example.Color', '''
package example

enum Color:
  case Red, Blue
''')
        def enumElement = (EnumElement) element

        then:
        element.enum
        enumElement.values() == ['Red', 'Blue']
        enumElement.elements()*.name == ['Red', 'Blue']
        enumElement.elements()*.type*.name == ['example.Color', 'example.Color']
    }

    void "type element visitors see Scala enum constants"() {
        given:
        def constants = []

        when:
        ScalaEnumConstantCaptureVisitor.withConsumer(constants::add, {
            buildClassLoader('example.Color', '''
package example

enum Color:
  case Red, Blue
''')
        })

        then:
        constants*.name == ['Red', 'Blue']
    }

    void "exposes array class literal and enum annotation values"() {
        when:
        def element = buildClassElement('example.Hints', '''
package example

import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.TypeHint

@Requires(env = Array("test", "dev"))
@TypeHint(value = Array(classOf[String]), accessType = Array(TypeHint.AccessType.ALL_PUBLIC))
class Hints
''')
        def requires = element.getAnnotation(Requires)
        def typeHint = element.getAnnotation(TypeHint)

        then:
        requires.stringValues('env').toList() == ['test', 'dev']
        typeHint.annotationClassValues('value').collect { it.name } == [String.name]
        typeHint.enumValues('accessType', TypeHint.AccessType).toList() == [TypeHint.AccessType.ALL_PUBLIC]
    }

    void "expands nested repeatable annotation container values"() {
        when:
        def element = buildClassElement('example.ConditionalBean', '''
package example

import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Requirements

@Requirements(Array(
  new Requires(env = Array("test")),
  new Requires(property = "feature.enabled", value = "true")
))
class ConditionalBean
''')

        def requirements = element.getAnnotationValuesByType(Requires)

        then:
        element.hasAnnotation(Requirements)
        requirements.size() == 2
        requirements[0].stringValues('env').toList() == ['test']
        requirements[1].stringValue('property').get() == 'feature.enabled'
        requirements[1].stringValue('value').get() == 'true'
    }

    void "expands direct repeatable annotations"() {
        when:
        def element = buildClassElement('example.DirectConditionalBean', '''
package example

import io.micronaut.context.annotation.Requires

@Requires(env = Array("test"))
@Requires(property = "feature.enabled", value = "true")
class DirectConditionalBean
''')

        def requirements = element.getAnnotationValuesByType(Requires)
        def envRequirement = requirements.find { it.stringValues('env').toList() == ['test'] }
        def propertyRequirement = requirements.find { it.stringValue('property').orElse(null) == 'feature.enabled' }

        then:
        requirements.size() == 2
        envRequirement != null
        propertyRequirement != null
        propertyRequirement.stringValue('value').get() == 'true'
    }

    void "traverses source-defined Scala annotation stereotypes"() {
        when:
        def element = buildClassElement('example.Engine', '''
package example

import jakarta.inject.Singleton
import scala.annotation.StaticAnnotation

@Singleton
class MySingleton extends StaticAnnotation

@MySingleton
class Engine
''')

        then:
        element.hasAnnotation('example.MySingleton')
        element.hasStereotype(Singleton)
        element.hasStereotype('jakarta.inject.Scope')
    }

    void "reads source-defined Scala annotation defaults from compiler symbols"() {
        when:
        def element = buildClassElement('example.Engine', '''
package example

import jakarta.inject.Singleton
import scala.annotation.StaticAnnotation

@Singleton
class MySingleton(val value: String = "engine", val enabled: Boolean = true) extends StaticAnnotation

@MySingleton
class Engine
''')
        def annotation = element.getAnnotation('example.MySingleton')

        then:
        annotation.stringValue().get() == 'engine'
        annotation.booleanValue('enabled').get()
        element.hasStereotype(Singleton)
    }

    void "resolves alias target annotation stereotypes from class literal symbols"() {
        when:
        def element = buildClassElement('example.Engine', '''
package example

import io.micronaut.context.annotation.AliasFor
import jakarta.inject.Named
import scala.annotation.StaticAnnotation
import scala.annotation.meta.getter

class NamedAlias(
  @(AliasFor @getter)(annotation = classOf[Named], member = "value")
  val value: String = ""
) extends StaticAnnotation

@NamedAlias("main")
class Engine
''')

        then:
        element.hasStereotype(Named)
        element.hasStereotype(Qualifier)
        element.getAnnotationValuesByStereotype(Named.name).find {
            it.annotationName == 'example.NamedAlias'
        }.stringValue().get() == 'main'
    }

    void "inherits source-defined annotations through class hierarchy"() {
        when:
        def element = buildClassElement('example.Engine', '''
package example

import jakarta.inject.Singleton
import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import scala.annotation.StaticAnnotation

@Inherited
@Singleton
@Retention(RetentionPolicy.RUNTIME)
@Target(Array(ElementType.TYPE))
class InheritedSingleton extends StaticAnnotation

@InheritedSingleton
class Machine

class Engine extends Machine
''')

        then:
        element.hasAnnotation('example.InheritedSingleton')
        !element.hasDeclaredAnnotation('example.InheritedSingleton')
        element.hasStereotype(Singleton)
        !element.hasDeclaredStereotype(Singleton)
    }
}
