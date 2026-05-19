# Disabled Scala Docs Tests

This file tracks Scala documentation examples that are not yet ported to
`test-suite-scala`.

## Current Scope

- `io.micronaut.docs.basics.HelloControllerSpec` verifies the initial HTTP
  controller docs smoke test.
- `io.micronaut.docs.inject.intro.VehicleSpec` verifies basic IOC constructor
  injection with a Scala trait and implementation.
- `io.micronaut.docs.inject.qualifiers.named.VehicleSpec` verifies named
  qualifier constructor injection.
- `io.micronaut.docs.lifecycle.VehicleSpec` verifies post-construct lifecycle
  initialization.
- `io.micronaut.docs.core.beans.UserSpec` verifies `@Introspected` on a Scala
  case class, including construction and property metadata.
- `io.micronaut.docs.config.properties.EngineConfigSpec` verifies mutable
  `@ConfigurationProperties` binding, immutable case-class binding, Scala
  collection binding, and validation metadata.
- `io.micronaut.docs.config.env.EachPropertyTest` verifies map-style and
  list-style `@EachProperty` binding.
- `io.micronaut.docs.events.factory.VehicleSpec` verifies factory-created
  beans and bean-initialization listeners.
- `io.micronaut.docs.ioc.scopes.DriverSpec` verifies a Scala custom annotation
  carrying scope stereotypes.
- `io.micronaut.docs.aop.retry.RetrySpec` verifies around advice through
  retry interception.
- `io.micronaut.docs.aop.retry.BookService` compiles the annotation-style retry
  docs snippets, including simple, configured, circuit-breaker, and reactive
  retry examples.
- `io.micronaut.docs.aop.retry.ProgrammaticRetrySpec` verifies programmatic
  retry and circuit breaker operations.
- `io.micronaut.docs.aop.around.AroundSpec` verifies a Scala `@Around`
  stereotype annotation, method interceptor, and generated proxy.
- `io.micronaut.docs.aop.introduction.IntroductionSpec` verifies introduction
  advice and generated proxies for a Scala trait, including resolved metadata
  for inherited generic introduction methods.
- `io.micronaut.docs.aop.lifecycle.LifeCycleAdviseSpec` verifies constructor,
  post-construct, and pre-destroy lifecycle advice.
- `io.micronaut.docs.aop.scheduled.ScheduledExample` and
  `TaskSchedulerInjectExample` compile the scheduled method and scheduler
  injection docs snippets.
- `io.micronaut.docs.server.routes.IssuesControllerTest` verifies HTTP
  controller URI variable binding, explicit `@PathVariable` binding, default
  URI variable values, and conversion/not-found error handling.
- `io.micronaut.docs.annotation.PetControllerSpec` verifies HTTP client
  introduction against a Scala trait, controller implementation, reactive return
  type, and method validation metadata.
- `io.micronaut.docs.server.filters.filtermethods.TraceFilterMethodsSpec`
  verifies `@ServerFilter` filter methods, request/response filters, constructor
  injection, and response header mutation.
- `io.micronaut.docs.server.filters.TraceFilterSpec` verifies legacy
  `HttpServerFilter` usage, constructor injection, reactive chaining, and
  response header mutation.
- `io.micronaut.docs.client.ThirdPartyClientFilterSpec` verifies
  `@ClientFilter` request filters, Scala object constants in annotation values,
  constructor injection, and outbound request header mutation.
- `io.micronaut.docs.client.versioning.HelloClientSpec` verifies generated
  executable client method metadata and API versioning annotations.
- `io.micronaut.docs.aop.proxytarget.ProxyTargetSpec` verifies
  `@Around(proxyTarget = true)` and `@Around(proxyTarget = true, hotswap = true)`
  Scala stereotypes, generated proxy-target beans, interceptor invocation,
  target lifecycle, and target swapping.
- Scala docs snippets are wired through the root `testsuitescala` property.

## Backlog

- Additional AOP examples: reactive around advice.

Scala examples should stay idiomatic: primary constructors, case classes,
traits, `val`, and `var` should be preferred over JavaBean-style code unless
the documented feature specifically requires Java interop.
