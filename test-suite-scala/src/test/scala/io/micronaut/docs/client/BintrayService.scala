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

// tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import reactor.core.publisher.Flux
// end::imports[]

@Requires(property = "spec.name", value = "ThirdPartyClientFilterSpec")
// tag::bintrayService[]
@Singleton
class BintrayService(
    @Client(BintrayApi.URL) client: HttpClient, // <1>
    @Value("${bintray.organization}") org: String
):

  def fetchRepositories(): Flux[HttpResponse[String]] =
    Flux.from(client.exchange(HttpRequest.GET[AnyRef](s"/repos/$org"), classOf[String])) // <2>

  def fetchPackages(repo: String): Flux[HttpResponse[String]] =
    Flux.from(client.exchange(HttpRequest.GET[AnyRef](s"/repos/$org/$repo/packages"), classOf[String])) // <2>
// end::bintrayService[]
