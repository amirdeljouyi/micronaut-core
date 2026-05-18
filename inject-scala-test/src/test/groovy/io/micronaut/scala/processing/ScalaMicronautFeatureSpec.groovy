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

import io.micronaut.aop.Intercepted
import io.micronaut.context.annotation.ConfigurationInject
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.inject.ValidatedBeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.scala.processing.test.AbstractScalaTypeElementSpec
import io.micronaut.scala.processing.test.ScalaAnnotatingVisitor
import jakarta.inject.Singleton

import java.util.function.Supplier

class ScalaMicronautFeatureSpec extends AbstractScalaTypeElementSpec {

    void "supports named qualifier constructor injection"() {
        when:
        def context = buildContext('''
package example

import jakarta.inject.Named
import jakarta.inject.Singleton

trait Engine:
  def name(): String

@Singleton
@Named("v6")
class V6Engine extends Engine:
  override def name(): String = "v6"

@Singleton
@Named("v8")
class V8Engine extends Engine:
  override def name(): String = "v8"

@Singleton
class Vehicle(@Named("v8") val engine: Engine)
''')

        then:
        getBean(context, 'example.Vehicle').engine.name() == 'v8'

        cleanup:
        context?.close()
    }

    void "supports array constructor injection"() {
        when:
        def source = '''
package example

import jakarta.inject.Singleton

trait Engine:
  def name(): String

@Singleton
class V6Engine extends Engine:
  override def name(): String = "v6"

@Singleton
class V8Engine extends Engine:
  override def name(): String = "v8"

@Singleton
class Vehicle(val engines: Array[Engine])
'''
        def definition = buildBeanDefinition('example.Vehicle', source)
        def context = buildContext(source)
        def vehicle = getBean(context, 'example.Vehicle')

        then:
        definition.constructor.arguments.size() == 1
        vehicle.engines()*.name() as Set == ['v6', 'v8'] as Set

        cleanup:
        context?.close()
    }

    void "supports bean registration injection"() {
        when:
        def source = '''
package beanreg

import io.micronaut.context.BeanRegistration
import io.micronaut.context.annotation.Primary
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.Collection
import java.util.List
import scala.annotation.meta.field

@Singleton
class Test(
  val registrations: Collection[BeanRegistration[Foo]],
  val primaryBean: BeanRegistration[Foo],
  @Named("two") val secondaryBean: BeanRegistration[Foo]
):
  @(Inject @field)
  var fieldRegistrations: Collection[BeanRegistration[Foo]] = _

  @(Inject @field)
  var fieldArrayRegistrations: Array[BeanRegistration[Foo]] = _

  var methodRegistrations: List[BeanRegistration[Foo]] = _

  @Inject
  def setRegs(registrations: List[BeanRegistration[Foo]]): Unit =
    methodRegistrations = registrations

trait Foo

@Singleton
@Primary
class Foo1 extends Foo

@Singleton
@Named("two")
class Foo2 extends Foo
'''
        def context = buildContext(source)
        def bean = getBean(context, 'beanreg.Test')
        def registrations = bean.registrations()
        def fieldRegistrations = bean.fieldRegistrations()
        def methodRegistrations = bean.methodRegistrations()
        def fieldArrayRegistrations = bean.fieldArrayRegistrations().toList()

        then:
        bean.primaryBean().bean.getClass().name == 'beanreg.Foo1'
        bean.secondaryBean().bean.getClass().name == 'beanreg.Foo2'
        registrations.size() == 2
        fieldRegistrations.size() == 2
        fieldRegistrations == registrations
        fieldRegistrations as List == methodRegistrations
        fieldRegistrations as List == fieldArrayRegistrations
        registrations.any { it.bean.getClass().name == 'beanreg.Foo1' }
        registrations.any { it.bean.getClass().name == 'beanreg.Foo2' }

        cleanup:
        context?.close()
    }

    void "supports requires conditions on Scala beans"() {
        when:
        def disabled = buildContext('''
package example

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@Singleton
@Requires(property = "feature.enabled", value = "true")
class FeatureBean
''')
        def enabled = buildContext('''
package example

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@Singleton
@Requires(property = "feature.enabled", value = "true")
class FeatureBean
''', ['feature.enabled': 'true'])

        then:
        !disabled.containsBean(disabled.classLoader.loadClass('example.FeatureBean'))
        enabled.containsBean(enabled.classLoader.loadClass('example.FeatureBean'))

        cleanup:
        disabled?.close()
        enabled?.close()
    }

    void "supports value constructor injection"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Singleton
class Vehicle(@Value("${vehicle.name}") val name: String)
''', ['vehicle.name': 'roadster'])

        then:
        getBean(context, 'example.Vehicle').name() == 'roadster'

        cleanup:
        context?.close()
    }

    void "supports field and method injection"() {
        when:
        def context = buildContext('''
package example

import jakarta.inject.Inject
import jakarta.inject.Singleton
import scala.annotation.meta.field

trait Engine:
  def name(): String

@Singleton
class V8Engine extends Engine:
  override def name(): String = "v8"

@Singleton
class Garage:
  @(Inject @field)
  var fieldEngine: Engine = _

  var methodEngine: Engine = _

  @Inject
  def install(engine: Engine): Unit =
    methodEngine = engine
''')
        def garage = getBean(context, 'example.Garage')

        then:
        garage.fieldEngine().name() == 'v8'
        garage.methodEngine().name() == 'v8'

        cleanup:
        context?.close()
    }

    void "supports post construct lifecycle methods"() {
        when:
        def context = buildContext('''
package example

import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton

@Singleton
class Worker:
  var started: Boolean = false

  @PostConstruct
  def init(): Unit =
    started = true
''')

        then:
        getBean(context, 'example.Worker').started()

        cleanup:
        context?.close()
    }

    void "supports pre destroy lifecycle methods"() {
        when:
        def context = buildContext('''
package example

import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton

@Singleton
class Worker:
  var stopped: Boolean = false

  @PreDestroy
  def stop(): Unit =
    stopped = true
''')
        def worker = getBean(context, 'example.Worker')

        then:
        !worker.stopped()

        when:
        context.close()

        then:
        worker.stopped()
    }

    void "supports inject scope dependencies"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.InjectScope
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.ArrayList

trait Connection extends AutoCloseable:
  override def close(): Unit
  def isOpen(): Boolean

@Bean
class TestConnection(val other: Other) extends Connection:
  var open: Boolean = true

  override def isOpen(): Boolean = open && other.isOpen

  @PreDestroy
  override def close(): Unit =
    open = false

@Bean
class Other:
  var isOpen: Boolean = true

  @PreDestroy
  def close(): Unit =
    isOpen = false

@Singleton
class Test(@InjectScope conn1: Connection, @InjectScope conn2: Connection):
  val createdConnections = new ArrayList[Connection]()
  createdConnections.add(conn1)
  createdConnections.add(conn2)

  @Inject
  def init(@InjectScope conn3: Connection): Unit =
    createdConnections.add(conn3)
''')
        def bean = getBean(context, 'example.Test')
        def connections = bean.createdConnections()

        then:
        connections.size() == 3
        connections.every { !it.isOpen() }
        connections.every { !it.other().isOpen() }

        cleanup:
        context?.close()
    }

    void "supports simple factory methods"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

case class Engine(name: String)

@Factory
class EngineFactory:
  @Singleton
  def engine(): Engine = Engine("v8")
''')

        then:
        getBean(context, 'example.Engine').name() == 'v8'

        cleanup:
        context?.close()
    }

    void "supports factory val beans"() {
        when:
        def source = '''
package example

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

case class Engine(name: String)

@Factory
class EngineFactory:
  @Bean
  @Singleton
  val engine: Engine = Engine("v8")
'''
        def element = buildClassElement('example.EngineFactory', source)
        def property = element.syntheticBeanProperties.find { it.name == 'engine' }
        def context = buildContext(source)

        then:
        property != null
        property.hasStereotype('jakarta.inject.Singleton')
        property.readMember.get().hasDeclaredStereotype('jakarta.inject.Scope')
        property.field.get().hasStereotype('jakarta.inject.Singleton')
        property.readMember.get().hasDeclaredStereotype('io.micronaut.context.annotation.Bean')
        getBean(context, 'example.Engine').name() == 'v8'

        cleanup:
        context?.close()
    }

    void "type element visitors can annotate Scala elements"() {
        when:
        def definition = ScalaAnnotatingVisitor.withAnnotations({
            buildBeanDefinition('example.TestListener', '''
package example

import io.micronaut.context.annotation.Executable
import jakarta.inject.Singleton

@Singleton
class TestListener:
  @Executable
  def receive(value: String): Unit = ()
''')
        } as Supplier)
        def receiveMethod = definition.findMethod('receive', String).get()
        def valueArgument = receiveMethod.arguments[0]

        then:
        definition.stringValue(ScalaAnnotatingVisitor.ANN, 'target').get() == 'class'
        receiveMethod.stringValue(ScalaAnnotatingVisitor.ANN, 'target').get() == 'method'
        valueArgument.annotationMetadata.stringValue(ScalaAnnotatingVisitor.ANN, 'target').get() == 'parameter'
    }

    void "type element visitor annotation mutations expand Scala annotation stereotypes"() {
        when:
        def element = ScalaAnnotatingVisitor.withClassAnnotation('example.MySingleton', {
            buildClassElement('example.Engine', '''
package example

import jakarta.inject.Singleton
import scala.annotation.StaticAnnotation

@Singleton
class MySingleton extends StaticAnnotation

@MySingleton
class Registered

class Engine
''')
        } as Supplier)

        then:
        element.hasAnnotation('example.MySingleton')
        element.hasStereotype(Singleton)
    }

    void "type element visitors can annotate Scala introspection properties"() {
        when:
        def introspection = ScalaAnnotatingVisitor.withAnnotations({
            buildBeanIntrospection('example.Test', '''
package example

import io.micronaut.core.annotation.Introspected

@Introspected
class Test(var name: String)
''')
        } as Supplier)

        then:
        introspection.getRequiredProperty('name', String)
            .stringValue(ScalaAnnotatingVisitor.ANN, 'target').get() == 'property'
    }

    void "supports source-defined annotation aliases on Scala annotation members"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.AliasFor
import io.micronaut.context.annotation.Executable
import io.micronaut.context.annotation.Factory
import jakarta.inject.Named
import jakarta.inject.Singleton
import scala.annotation.StaticAnnotation
import scala.annotation.meta.getter

@Singleton
@Executable
class TestAnnotation(
  @(AliasFor @getter)(annotation = classOf[Named], member = "value")
  val value: String = ""
) extends StaticAnnotation

@Factory
class Test:
  @TestAnnotation("foo")
  def myFunc(): java.util.function.Function[String, java.lang.Integer] =
    (value: String) => java.lang.Integer.valueOf(10)
''', true)
        def definition = context.getBeanDefinition(java.util.function.Function, Qualifiers.byName('foo'))

        then:
        definition.getValue('jakarta.inject.Named', String).get() == 'foo'
        context.getBean(java.util.function.Function, Qualifiers.byName('foo')).apply('test') == 10

        cleanup:
        context?.close()
    }

    void "supports executable methods"() {
        when:
        def definition = buildBeanDefinition('example.Calculator', '''
package example

import io.micronaut.context.annotation.Executable
import jakarta.inject.Singleton

@Singleton
class Calculator:
  @Executable
  def add(left: Int, right: Int): Int = left + right
''')

        then:
        definition.findMethod('add', Integer.TYPE, Integer.TYPE).present
    }

    void "supports around advice on Scala methods"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.retry.annotation.Retryable
import jakarta.inject.Singleton

@Singleton
class FlakyService:
  var attempts: Int = 0

  @Retryable(attempts = "2", delay = "1ms")
  def call(): String =
    attempts = attempts + 1
    if attempts == 1 then throw RuntimeException("boom")
    "ok"
''', true)
        def service = getBean(context, 'example.FlakyService')

        then:
        service.call() == 'ok'
        service.attempts() == 2

        cleanup:
        context?.close()
    }

    void "supports introduction advice on Scala traits"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.aop.Interceptor
import io.micronaut.aop.Introduction
import io.micronaut.aop.InvocationContext
import io.micronaut.context.annotation.Type
import jakarta.inject.Singleton
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import scala.annotation.StaticAnnotation

@Singleton
class StubIntroduction extends Interceptor[AnyRef, Object]:
  var invoked: Int = 0

  override def intercept(context: InvocationContext[AnyRef, Object]): Object =
    invoked = invoked + 1
    Integer.valueOf(context.getParameterValues()(0).asInstanceOf[String].length)

@Introduction
@Type(Array(classOf[StubIntroduction]))
@Retention(RetentionPolicy.RUNTIME)
@Target(Array(ElementType.TYPE))
class Stub extends StaticAnnotation

@Stub
trait TextService:
  def length(value: String): Int
''')
        def service = getBean(context, 'example.TextService')
        def interceptor = getBean(context, 'example.StubIntroduction')

        then:
        service instanceof Intercepted
        service.length('test') == 4
        interceptor.invoked() == 1

        cleanup:
        context?.close()
    }

    void "exposes inherited Java interfaces for Scala interceptor beans"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import jakarta.inject.Singleton

@Singleton
class TimingInterceptor extends MethodInterceptor[AnyRef, Object]:
  override def intercept(context: MethodInvocationContext[AnyRef, Object]): Object =
    context.proceed()
''')

        then:
        context.getBean(io.micronaut.aop.Interceptor)

        cleanup:
        context?.close()
    }

    void "supports inherited source-defined singleton stereotypes"() {
        when:
        def context = buildContext('''
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

@Singleton
@Retention(RetentionPolicy.RUNTIME)
@Target(Array(ElementType.TYPE))
class LocalSingleton extends StaticAnnotation

@InheritedSingleton
class Machine

@LocalSingleton
class LocalMachine

class Engine extends Machine

class LocalEngine extends LocalMachine
''')
        def engineType = context.classLoader.loadClass('example.Engine')
        def localEngineType = context.classLoader.loadClass('example.LocalEngine')

        then:
        context.containsBean(engineType)
        !context.containsBean(localEngineType)

        cleanup:
        context?.close()
    }

    void "supports inherited classpath singleton stereotypes"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.scala.processing.fixtures.ExternalLocalMachine
import io.micronaut.scala.processing.fixtures.ExternalMachine

class Engine extends ExternalMachine

class LocalEngine extends ExternalLocalMachine
''')
        def engineType = context.classLoader.loadClass('example.Engine')
        def localEngineType = context.classLoader.loadClass('example.LocalEngine')

        then:
        context.containsBean(engineType)
        !context.containsBean(localEngineType)

        cleanup:
        context?.close()
    }

    void "rejects singleton Scala enum beans"() {
        when:
        buildBeanDefinition('example.Status', '''
package example

import jakarta.inject.Singleton

@Singleton
enum Status:
  case Active
''')

        then:
        def e = thrown(RuntimeException)
        e.message.contains('Enum types cannot be defined as beans')
    }

    void "supports source-defined default scopes"() {
        when:
        def definition = buildBeanDefinition('example.Engine', '''
package example

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.DefaultScope
import jakarta.inject.Singleton
import scala.annotation.StaticAnnotation

@Bean
@DefaultScope(classOf[Singleton])
class DefaultSingleton extends StaticAnnotation

@DefaultSingleton
class Engine
''')

        then:
        definition.isSingleton()
        definition.hasDeclaredStereotype(Singleton)
    }

    void "explicit Scala bean scope overrides source-defined default scope"() {
        when:
        def definition = buildBeanDefinition('example.Engine', '''
package example

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.DefaultScope
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Singleton
import scala.annotation.StaticAnnotation

@Bean
@DefaultScope(classOf[Singleton])
class DefaultSingleton extends StaticAnnotation

@DefaultSingleton
@Prototype
class Engine
''')

        then:
        !definition.isSingleton()
        !definition.hasDeclaredStereotype(Singleton)
        definition.hasDeclaredStereotype(Prototype)
        definition.scopeName.get() == Prototype.NAME
    }

    void "explicit Scala factory scope overrides source-defined default scope"() {
        when:
        def definition = buildBeanDefinition('example.MyBeanFactory', '''
package example

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.DefaultScope
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Singleton
import scala.annotation.StaticAnnotation

@Bean
@DefaultScope(classOf[Singleton])
class DefaultSingleton extends StaticAnnotation

@Factory
@Prototype
class MyBeanFactory:
  @DefaultSingleton
  @Prototype
  def myBean(): MyBean = MyBean()

class MyBean
''')

        then:
        !definition.isSingleton()
        definition.scopeName.get() == Prototype.NAME
    }

    void "explicit Scala factory method scope overrides source-defined default scope"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.DefaultScope
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Singleton
import scala.annotation.StaticAnnotation

@Bean
@DefaultScope(classOf[Singleton])
class DefaultSingleton extends StaticAnnotation

@Factory
class MyBeanFactory:
  @DefaultSingleton
  @Prototype
  def myBean(): MyBean = MyBean()

class MyBean
''')

        then:
        !getBeanDefinition(context, 'example.MyBean').isSingleton()

        cleanup:
        context?.close()
    }

    void "Scala bean factory method without explicit scope remains unscoped"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory

@Factory
class MyBeanFactory:
  @Bean
  def myBean(): MyBean = MyBean()

class MyBean
''')

        then:
        !getBeanDefinition(context, 'example.MyBean').isSingleton()

        cleanup:
        context?.close()
    }

    void "supports mutable configuration properties"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("app")
class AppConfig:
  var name: String = _
  var port: Int = 0
''', ['app.name': 'demo', 'app.port': 8080], true)
        def config = getBean(context, 'example.AppConfig')

        then:
        config.name() == 'demo'
        config.port() == 8080

        cleanup:
        context?.close()
    }

    void "supports immutable case class configuration properties"() {
        when:
        def source = '''
package example

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("app")
case class AppConfig(name: String, port: Int)
'''
        def element = buildClassElement('example.AppConfig', source)
        def constructor = element.primaryConstructor.get()
        def definition = buildBeanDefinition('example.AppConfig', source)
        def arguments = definition.constructor.arguments

        then:
        constructor.hasAnnotation(ConfigurationInject)
        constructor.parameters[0].stringValue(Property, 'name').get() == 'app.name'
        constructor.parameters[1].stringValue(Property, 'name').get() == 'app.port'
        arguments[0].annotationMetadata.stringValue(Property, 'name').get() == 'app.name'
        arguments[1].annotationMetadata.stringValue(Property, 'name').get() == 'app.port'

        when:
        def context = buildContext(source, ['app.name': 'demo', 'app.port': 8080], true)
        def config = getBean(context, 'example.AppConfig')

        then:
        config.name() == 'demo'
        config.port() == 8080

        cleanup:
        context?.close()
    }

    void "supports configuration inject constructors with bean dependencies"() {
        when:
        def source = '''
package example

import io.micronaut.context.annotation.ConfigurationInject
import io.micronaut.context.annotation.ConfigurationProperties
import jakarta.inject.Singleton

@Singleton
class Engine:
  def name(): String = "v8"

@ConfigurationProperties("app")
class AppConfig @ConfigurationInject (val name: String, val engine: Engine)
'''
        def definition = buildBeanDefinition('example.AppConfig', source)
        def arguments = definition.constructor.arguments

        then:
        arguments[0].annotationMetadata.stringValue(Property, 'name').get() == 'app.name'
        !arguments[1].annotationMetadata.hasAnnotation(Property)

        when:
        def context = buildContext(source, ['app.name': 'demo'], true)
        def config = getBean(context, 'example.AppConfig')

        then:
        config.name() == 'demo'
        config.engine().name() == 'v8'

        cleanup:
        context?.close()
    }

    void "supports validation on mutable configuration properties"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.ConfigurationProperties
import jakarta.validation.constraints.Min
import scala.annotation.meta.field

@ConfigurationProperties("app")
class AppConfig:
  @(Min @field)(1L)
  var port: Int = 0
''', ['app.port': 0], true)
        def configType = context.classLoader.loadClass('example.AppConfig')
        def definition = context.getBeanDefinition(configType)

        then:
        definition instanceof ValidatedBeanDefinition

        when:
        context.getBean(configType)

        then:
        thrown(BeanInstantiationException)

        cleanup:
        context?.close()
    }

    void "supports nested configuration properties"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("app")
class AppConfig:
  var name: String = _
  var engine: AppConfig.EngineConfig = AppConfig.EngineConfig()

object AppConfig:
  @ConfigurationProperties("engine")
  class EngineConfig:
    var cylinders: Int = 0
''', ['app.name': 'demo', 'app.engine.cylinders': 6], true)
        def config = getBean(context, 'example.AppConfig')

        then:
        config.name() == 'demo'
        config.engine().cylinders() == 6

        cleanup:
        context?.close()
    }

    void "supports each property configuration beans"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.EachProperty

@EachProperty("engines")
class EngineConfig:
  var cylinders: Int = 0
''', [
            'engines.small.cylinders': 6,
            'engines.large.cylinders': 8
        ], true)
        assert context.environment.containsProperties('engines.large')
        assert context.environment.getPropertyEntries('engines').containsAll(['small', 'large'])
        def engineType = context.classLoader.loadClass('example.EngineConfig')
        def engines = context.getBeansOfType(engineType)
        def large = getBean(context, 'example.EngineConfig', Qualifiers.byName('large'))

        then:
        engines.size() == 2
        large.cylinders() == 8

        cleanup:
        context?.close()
    }
}
