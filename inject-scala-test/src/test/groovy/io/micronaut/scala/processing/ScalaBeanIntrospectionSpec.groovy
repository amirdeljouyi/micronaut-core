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

import io.micronaut.core.beans.BeanIntrospection
import io.micronaut.core.beans.BeanIntrospectionReference
import io.micronaut.core.beans.EnumBeanIntrospection
import io.micronaut.scala.processing.test.AbstractScalaTypeElementSpec
import spock.lang.PendingFeature

class ScalaBeanIntrospectionSpec extends AbstractScalaTypeElementSpec {

    void "exposes Scala constructor argument generics"() {
        when:
        def introspection = buildBeanIntrospection('test.Test', '''
package test

import io.micronaut.core.annotation.Introspected

@Introspected
class Test(val properties: java.util.Map[String, String])
''')

        then:
        introspection.constructorArguments[0].getTypeVariable("K").get().type == String
        introspection.constructorArguments[0].getTypeVariable("V").get().type == String
    }

    void "exposes Scala generic array introspection types"() {
        when:
        def introspection = buildBeanIntrospection('arraygenerics.Test', '''
package arraygenerics

import io.micronaut.context.annotation.Executable
import io.micronaut.core.annotation.Introspected

@Introspected
class Test[T <: CharSequence](
  var array: Array[T],
  var starArray: Array[_],
  var stringArray: Array[String]
):
  @Executable
  def myMethod(): Array[T] = array
''')

        then:
        introspection.beanProperties.size() == 3
        introspection.getRequiredProperty("array", CharSequence[].class).type == CharSequence[].class
        introspection.getRequiredProperty("starArray", Object[].class).type == Object[].class
        introspection.getRequiredProperty("stringArray", String[].class).type == String[].class
        introspection.beanMethods.first().returnType.type == CharSequence[].class
    }

    void "exposes Scala multi-dimensional array introspection types"() {
        when:
        def introspection = buildBeanIntrospection('arraygenerics.Test', '''
package arraygenerics

import io.micronaut.core.annotation.Introspected

@Introspected
class Test(
  var oneDimension: Array[Int],
  var twoDimensions: Array[Array[Int]],
  var stringMatrix: Array[Array[String]]
)
''')

        then:
        introspection.getRequiredProperty("oneDimension", int[].class).type == int[].class
        introspection.getRequiredProperty("twoDimensions", int[][].class).type == int[][].class
        introspection.getRequiredProperty("stringMatrix", String[][].class).type == String[][].class
    }

    void "exposes annotation metadata on deep Scala introspection property type parameters"() {
        when:
        def introspection = buildBeanIntrospection('test.Test', '''
package test

import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Introspected
class Test(
  var deepList: java.util.List[
    java.util.List[
      java.util.List[String @NotNull] @NotEmpty
    ] @Size(min = 1, max = 2)
  ],
  var deepList2: java.util.List[
    java.util.List[
      java.util.List[
        java.util.List[
          java.util.List[
            java.util.List[String]
          ]
        ]
      ]
    ]
  ]
)
''')

        then:
        introspection != null
        def property = introspection.getProperty("deepList").get().asArgument()
        property.getTypeParameters().length == 1
        def param1 = property.getTypeParameters()[0]
        param1.getTypeParameters().length == 1
        def param2 = param1.getTypeParameters()[0]
        param2.getTypeParameters().length == 1
        def param3 = param2.getTypeParameters()[0]
        param1.getAnnotationMetadata().getAnnotationNames().contains('jakarta.validation.constraints.Size$List')
        param2.getAnnotationMetadata().getAnnotationNames().contains('jakarta.validation.constraints.NotEmpty$List')
        param3.getAnnotationMetadata().getAnnotationNames().contains('jakarta.validation.constraints.NotNull$List')
    }

    void "supports Scala field access bean introspection"() {
        when:
        def introspection = buildBeanIntrospection('test.Test', '''
package test

import io.micronaut.core.annotation.Introspected

@Introspected(
  accessKind = Array(Introspected.AccessKind.FIELD),
  visibility = Array(Introspected.Visibility.ANY)
)
class Test:
  private var secret: String = "hidden"
  var visible: String = "shown"
  def reveal: String = secret
''')
        def bean = introspection.instantiate()
        def properties = introspection.beanProperties.collectEntries { [(it.name): it] }

        then:
        properties.keySet() == ['secret', 'visible'] as Set
        properties.secret.get(bean) == 'hidden'
        properties.visible.get(bean) == 'shown'
    }

    void "builds Scala introspection for companion nested class"() {
        when:
        def introspection = buildBeanIntrospection('test.Test$Foo', '''
package test

import io.micronaut.core.annotation.Introspected

object Test:
  @Introspected
  class Foo(val name: String)
''')

        then:
        introspection != null
        introspection.beanType.simpleName == 'Foo'
        introspection.getRequiredProperty("name", String).get(introspection.instantiate("Fred")) == "Fred"
    }

    void "writes Scala introspection to custom target package"() {
        when:
        def classLoader = buildClassLoader('test.Test', '''
package test

import io.micronaut.core.annotation.Introspected

@Introspected(targetPackage = "test.introspections")
class Test(val name: String)
''')
        def introspectionName = 'test.introspections.$Test$Introspection'
        def introspection = classLoader.loadClass(introspectionName).getDeclaredConstructor().newInstance() as BeanIntrospection
        def introspectionRef = classLoader.loadClass(introspectionName).getDeclaredConstructor().newInstance() as BeanIntrospectionReference

        then:
        introspection.beanType.name == 'test.Test'
        introspectionRef.beanType.name == 'test.Test'
        introspection.getRequiredProperty("name", String).get(introspection.instantiate("Fred")) == "Fred"
    }

    @PendingFeature
    void "builds Scala enum bean introspection"() {
        when:
        def introspection = buildBeanIntrospection('test.Status', '''
package test

import io.micronaut.core.annotation.Introspected

@Introspected
enum Status:
  case Active, Disabled
''')
        def active = introspection.instantiate("Active")

        then:
        introspection instanceof EnumBeanIntrospection
        introspection.beanProperties.empty
        active.name() == "Active"
        introspection.constants*.name == ['Active', 'Disabled']
    }

    void "instantiates Scala introspection with byte array constructor forwarding"() {
        when:
        def introspection = buildBeanIntrospection('test.FormulaDto', '''
package test

import io.micronaut.core.annotation.Introspected

@Introspected
class FormulaDto(val otherColumns: java.util.List[String], bytesValue: Array[Byte])
    extends FormulaCreationDto(bytesValue)

@Introspected
class FormulaCreationDto(val bytes: Array[Byte])
''')
        def bytes = new byte[] { 123 }
        def bean = introspection.instantiate(List.of("total"), bytes)

        then:
        bean.otherColumns() == List.of("total")
        bean.bytes().is(bytes)
    }

    void "instantiates Scala introspection with boxed Boolean constructor forwarding"() {
        when:
        def introspection = buildBeanIntrospection('test.FormulaDto', '''
package test

import io.micronaut.core.annotation.Introspected

@Introspected
class FormulaDto(val otherColumns: java.util.List[String], percentValue: java.lang.Boolean)
    extends FormulaCreationDto(java.util.Optional.of(percentValue))

@Introspected
class FormulaCreationDto(percentValue: java.util.Optional[java.lang.Boolean]):
  val percent: Boolean = percentValue.orElse(false)
''')
        def bean = introspection.instantiate(List.of("percent"), Boolean.TRUE)

        then:
        bean.otherColumns() == List.of("percent")
        bean.percent()
    }
}
