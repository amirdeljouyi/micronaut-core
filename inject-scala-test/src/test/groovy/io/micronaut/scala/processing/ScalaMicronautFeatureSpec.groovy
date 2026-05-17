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
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.inject.ValidatedBeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.scala.processing.test.AbstractScalaTypeElementSpec

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
