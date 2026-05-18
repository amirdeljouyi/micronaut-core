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

    @PendingFeature(reason = "Superclass introspection constructor forwarding currently needs Scala-specific read-member coverage without core-processor changes")
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

    @PendingFeature(reason = "Superclass introspection constructor forwarding currently needs Scala-specific read-member coverage without core-processor changes")
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
