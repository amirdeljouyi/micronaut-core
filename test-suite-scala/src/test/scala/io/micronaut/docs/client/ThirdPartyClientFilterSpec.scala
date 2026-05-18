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
package io.micronaut.docs.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.util.Base64
import scala.jdk.CollectionConverters.*

class ThirdPartyClientFilterSpec:

  private val token = "XXXX"
  private val username = "john"

  @Test
  def aClientFilterIsAppliedToTheRequestAndAddsTheAuthorizationHeader(): Unit =
    val server = ApplicationContext.run(
      classOf[EmbeddedServer],
      Map[String, Object](
        "spec.name" -> classOf[ThirdPartyClientFilterSpec].getSimpleName,
        "bintray.username" -> username,
        "bintray.token" -> token,
        "bintray.organization" -> "grails"
      ).asJava
    )
    try
      val client = server.getApplicationContext.createBean(classOf[HttpClient], server.getURL)
      try
        val bintrayService = server.getApplicationContext.getBean(classOf[BintrayService])

        val result = bintrayService.fetchRepositories().blockFirst().body()

        val encoded = Base64.getEncoder.encodeToString(s"$username:$token".getBytes)
        val expected = "Basic " + encoded

        assertEquals(expected, result)
      finally
        client.close()
    finally
      server.close()

@Requires(property = "spec.name", value = "ThirdPartyClientFilterSpec")
@Controller("/repos")
class HeaderController:

  @Get("/grails")
  def echoAuthorization(@Header authorization: String): String =
    authorization
