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
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.annotation.ClientFilter
import io.micronaut.http.annotation.RequestFilter
// end::imports[]

@Requires(property = "spec.name", value = "ThirdPartyClientFilterSpec")
// tag::bintrayFilter[]
@ClientFilter(Array("/repos/**")) // <1>
class BintrayFilter(
    @Value("${bintray.username}") username: String, // <2>
    @Value("${bintray.token}") token: String // <2>
):

  @RequestFilter
  def filter(request: MutableHttpRequest[AnyRef]): Unit =
    request.basicAuth(username, token) // <3>
// end::bintrayFilter[]
