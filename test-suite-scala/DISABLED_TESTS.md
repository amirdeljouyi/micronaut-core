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
  case class.
- `io.micronaut.docs.config.properties.EngineConfigSpec` verifies mutable
  `@ConfigurationProperties` binding.
- `io.micronaut.docs.config.env.EachPropertyTest` verifies map-style and
  list-style `@EachProperty` binding.
- `io.micronaut.docs.events.factory.VehicleSpec` verifies factory-created
  beans and bean-initialization listeners.
- Scala docs snippets are wired through the root `testsuitescala` property.

## Backlog

- IOC examples: scopes once the Scala adapter supports the matching feature.
- Introspection examples: property metadata beyond the basic case class proof
  of concept.
- Configuration examples: validation metadata.
- HTTP examples: controllers, clients, filters, and executable methods.
- AOP examples: around advice, introduction advice, and proxy generation.

Scala examples should stay idiomatic: primary constructors, case classes,
traits, `val`, and `var` should be preferred over JavaBean-style code unless
the documented feature specifically requires Java interop.
