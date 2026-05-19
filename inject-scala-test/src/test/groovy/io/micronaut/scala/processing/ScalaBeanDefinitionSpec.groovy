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

import io.micronaut.core.type.TypeInformation
import io.micronaut.scala.processing.test.AbstractScalaTypeElementSpec

class ScalaBeanDefinitionSpec extends AbstractScalaTypeElementSpec {

    void "formats Scala bean definition type strings"() {
        given:
        def definition = buildBeanDefinition('typestring.Test', '''
package typestring

import jakarta.inject.Singleton

@Singleton
class Test
''')

        expect:
        definition.asArgument().getTypeString(format) == result

        where:
        format                                    | result
        TypeInformation.TypeFormat.SIMPLE         | "Test"
        TypeInformation.TypeFormat.QUALIFIED      | "typestring.Test"
        TypeInformation.TypeFormat.SHORTENED      | "t.Test"
        TypeInformation.TypeFormat.ANSI_SIMPLE    | "\u001B[0;36mTest\u001B[0m"
        TypeInformation.TypeFormat.ANSI_QUALIFIED | "\u001B[0;36mtypestring.Test\u001B[0m"
        TypeInformation.TypeFormat.ANSI_SHORTENED | "\u001B[0;36mt.Test\u001B[0m"
    }

    void "limits Scala exposed bean types"() {
        given:
        def definition = buildBeanDefinition('limittypes.Test', '''
package limittypes

import io.micronaut.context.annotation.Bean
import jakarta.inject.Singleton

@Singleton
@Bean(typed = Array(classOf[Runnable]))
class Test extends Runnable:
  override def run(): Unit = ()
''')

        expect:
        definition.exposedTypes == [Runnable] as Set
    }

    void "fails compilation for invalid Scala exposed bean type"() {
        when:
        buildBeanDefinition('limittypes.Test', '''
package limittypes

import io.micronaut.context.annotation.Bean
import jakarta.inject.Singleton

@Singleton
@Bean(typed = Array(classOf[Runnable]))
class Test
''')

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Bean defines an exposed type [java.lang.Runnable] that is not implemented by the bean type")
    }

    void "fails compilation for exposed Scala subclass bean type"() {
        when:
        buildBeanDefinition('limittypes.Test', '''
package limittypes

import io.micronaut.context.annotation.Bean
import jakarta.inject.Singleton

@Singleton
@Bean(typed = Array(classOf[X]))
class Test

class X extends Test
''')

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Bean defines an exposed type [limittypes.X] that is not implemented by the bean type")
    }

    void "limits Scala factory exposed bean types"() {
        when:
        def context = buildContext('''
package limittypes

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

trait X
class Y extends X

@Factory
class TestFactory:
  @Singleton
  @Bean(typed = Array(classOf[X]))
  def method(): Y = Y()
''')

        then:
        getBean(context, 'limittypes.X') != null

        cleanup:
        context?.close()
    }

    void "fails compilation for invalid Scala factory exposed bean type"() {
        when:
        buildContext('''
package limittypes

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

trait Z
trait X
class Y extends X

@Factory
class TestFactory:
  @Singleton
  @Bean(typed = Array(classOf[Z]))
  def method(): X = Y()
''')

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Bean defines an exposed type [limittypes.Z] that is not implemented by the bean type")
    }

    void "passes Scala type arguments through parent hierarchy"() {
        given:
        def source = '''
package typearguments

import jakarta.inject.Singleton

@Singleton
class ChainA extends ChainB[java.lang.Boolean]

class ChainB[A] extends ChainC[A, Number, Integer]

abstract class ChainC[A, B, E] extends ChainD[A, B, String, E]

trait ChainD[A, B, C, E] extends ChainE[A, B, C, Byte]

trait ChainE[A, B, C, D]
'''
        def element = buildClassElement('typearguments.ChainA', source)
        def definition = buildBeanDefinition('typearguments.ChainA', source)

        expect:
        element.superType.get().typeArguments*.value*.name == ['java.lang.Boolean']
        element.getAllTypeArguments()["typearguments.ChainB"]*.value*.name == ['java.lang.Boolean']
        definition.getTypeArguments("typearguments.ChainB")*.type == [Boolean]
        definition.getTypeArguments("typearguments.ChainC")*.type == [Boolean, Number, Integer]
        definition.getTypeArguments("typearguments.ChainD")*.type == [Boolean, Number, String, Integer]
        definition.getTypeArguments("typearguments.ChainE")*.type == [Boolean, Number, String, Byte]
    }
}
