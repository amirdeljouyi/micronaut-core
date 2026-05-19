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
import io.micronaut.scala.processing.test.AbstractScalaTypeElementSpec

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
