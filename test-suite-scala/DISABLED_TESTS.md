# Disabled Scala Docs Tests

This file tracks Scala documentation examples that are not yet ported to
`test-suite-scala`.

## Current Scope

- `io.micronaut.docs.basics.HelloControllerSpec` verifies the initial HTTP
  controller docs smoke test.
- Scala docs snippets are wired through the root `testsuitescala` property.

## Backlog

- IOC examples: constructor injection, qualifiers, factories, scopes, and
  lifecycle examples once the Scala adapter supports the matching feature.
- Introspection examples: case classes and property metadata beyond the basic
  proof of concept.
- Configuration examples: `@ConfigurationProperties`, `@EachProperty`, and
  validation metadata.
- HTTP examples: controllers, clients, filters, and executable methods.
- AOP examples: around advice, introduction advice, and proxy generation.

Scala examples should stay idiomatic: primary constructors, case classes,
traits, `val`, and `var` should be preferred over JavaBean-style code unless
the documented feature specifically requires Java interop.
