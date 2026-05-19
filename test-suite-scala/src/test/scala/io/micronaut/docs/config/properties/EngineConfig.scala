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

import io.micronaut.context.annotation.ConfigurationProperties
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import scala.annotation.meta.field
import scala.compiletime.uninitialized

@ConfigurationProperties("engine")
class EngineConfig:
  @(Min @field)(value = 1L)
  var cylinders: Int = 0

  @(NotBlank @field)
  var manufacturer: String = uninitialized

@ConfigurationProperties("immutable.engine")
case class ImmutableEngineConfig(
    manufacturer: String,
    cylinders: Int
)

@ConfigurationProperties("app")
case class AppConfig(
    names: List[String],
    labels: Map[String, String]
)
