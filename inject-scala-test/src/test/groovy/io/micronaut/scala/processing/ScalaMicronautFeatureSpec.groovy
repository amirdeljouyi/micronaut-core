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

import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.scala.processing.test.AbstractScalaTypeElementSpec
import spock.lang.PendingFeature

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

    @PendingFeature(reason = "Nested Scala configuration properties still need Scala-specific binding semantics.")
    void "supports nested configuration properties"() {
        when:
        def context = buildContext('''
package example

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("app")
class AppConfig:
  var name: String = _

  @ConfigurationProperties("engine")
  var engine: EngineConfig = EngineConfig()

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
