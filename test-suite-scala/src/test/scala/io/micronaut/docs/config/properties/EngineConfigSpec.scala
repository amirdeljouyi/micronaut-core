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
package io.micronaut.docs.config.properties

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.inject.ValidatedBeanDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import scala.jdk.CollectionConverters.*

class EngineConfigSpec:

  @Test
  def bindsMutableConfigurationProperties(): Unit =
    val context = ApplicationContext.run(
      Map[String, Object](
        "engine.cylinders" -> Integer.valueOf(8),
        "engine.manufacturer" -> "Ford"
      ).asJava
    )
    try
      val config = context.getBean(classOf[EngineConfig])
      assertEquals(8, config.cylinders)
      assertEquals("Ford", config.manufacturer)
    finally
      context.close()

  @Test
  def bindsImmutableCaseClassConfigurationProperties(): Unit =
    val context = ApplicationContext.run(
      Map[String, Object](
        "immutable.engine.cylinders" -> Integer.valueOf(6),
        "immutable.engine.manufacturer" -> "Honda"
      ).asJava
    )
    try
      val config = context.getBean(classOf[ImmutableEngineConfig])
      assertEquals(6, config.cylinders)
      assertEquals("Honda", config.manufacturer)
    finally
      context.close()

  @Test
  def validatesMutableConfigurationProperties(): Unit =
    val context = ApplicationContext.run(
      Map[String, Object](
        "engine.cylinders" -> Integer.valueOf(0),
        "engine.manufacturer" -> "Ford"
      ).asJava
    )
    try
      val definition = context.getBeanDefinition(classOf[EngineConfig])
      assertTrue(definition.isInstanceOf[ValidatedBeanDefinition[?]])
      val error = assertThrows(
        classOf[BeanInstantiationException],
        () => context.getBean(classOf[EngineConfig])
      )
      assertTrue(error.getMessage.contains("must be greater than or equal to 1"))
    finally
      context.close()
