# Scala 3 Support for Micronaut Core

Last updated: 2026-05-19

## Summary

Add Scala 3 support as a compile-time language adapter, following the existing Java, Groovy, Kotlin, and Python model. The first proof of concept should create `inject-scala`, `inject-scala-test`, and `test-suite-scala`, compile Scala 3 snippets with a Micronaut Scala compiler plugin, expose Scala symbols through Micronaut's Element API, and generate working bean definitions and introspections.

Scala 2.x is explicitly out of scope. The Scala 3 compiler plugin should be a standard compiler plugin: a jar with `plugin.properties`, a main class extending `StandardPlugin`, and ordered `PluginPhase`s inserted into the compilation pipeline. Scala 3 standard plugins add phases but do not replace normal type checking, so the Micronaut plugin should operate on typed compiler trees after type checking.

Reference:

- https://docs.scala-lang.org/scala3/reference/changed-features/compiler-plugins.html

## Key Changes

### `inject-scala`

Add `inject-scala` as the Scala 3 compiler plugin and Element API implementation.

- Implement `MicronautScalaCompilerPlugin` with two ordered phases: a type visitor phase and a bean definition generation phase.
- Implement `ScalaVisitorContext`, `ScalaElementFactory`, `ScalaAnnotationMetadataBuilder`, and Scala `ClassElement`, `MethodElement`, `FieldElement`, `PropertyElement`, `ConstructorElement`, and `ParameterElement` wrappers.
- Reuse `TypeElementVisitor`, `BeanDefinitionCreatorFactory`, and the existing bytecode writers.
- Do not fork core processing logic.
- Add `VisitorContext.Language.SCALA` as the only public core API addition.

### `inject-scala-test`

Add `inject-scala-test` as the test harness module.

- Provide `AbstractScalaTypeElementSpec` with `buildClassElement`, `buildBeanDefinition`, `buildBeanIntrospection`, and `buildContext`.
- Compile inline Scala sources to a temp class output with the Scala 3 compiler and the Micronaut compiler plugin enabled.
- Use Spock and `@PendingFeature` for known gaps so unexpected passes are visible.

### `test-suite-scala`

Add `test-suite-scala`.

- Apply Gradle's `scala` plugin and wire `scalaCompilerPlugins(projects.micronautInjectScala)`.
- Use Gradle's dedicated `scalaCompilerPlugins` configuration and Scala/Java joint compilation support.
- Mirror the docs snippet layout used by `test-suite-groovy`, `test-suite-kotlin`, and `test-suite-python`.
- Add Scala snippet sources to the root docs `languageSnippetSources` input.

Reference:

- https://docs.gradle.org/current/userguide/scala_plugin.html

## Implementation Waves

### Wave 0: Build Scaffolding

- Add Scala 3 catalog entries, module includes, build files, package metadata, plugin descriptor, and minimal compile tasks.
- Prefer `managed-scala3` in the version catalog and use one Scala 3 line only.

### Wave 1: Proof of Concept

- Support simple Scala classes, primary constructors, methods, `val` and `var` properties, Java-visible annotations, and basic type resolution.
- Passing proof-of-concept tests:
  - `@Singleton` constructor injection.
  - `@Introspected case class`.
  - Generated bean definition loading.
  - A basic `TypeElementVisitor` seeing class, method, and property metadata.

### Wave 2: Parity Inventory and Harness Hardening

- Create `inject-scala-test/DISABLED_TESTS.md`.
- Inventory direct subclasses from the current checkout:
  - Java `AbstractTypeElementSpec` tests.
  - Groovy `AbstractBeanDefinitionSpec` tests.
  - Kotlin `AbstractKotlinCompilerSpec` tests.
- Classify each as portable, Scala-specific, already covered, or blocked.
- Port portable tests first.
- Skip language-specific Java, Groovy, and Kotlin syntax tests.

### Wave 3: Element and Annotation Completeness

- Implement annotation values, defaults, nested annotations, repeatables, stereotypes, aliases, retention and targets, nullability, class literals, enum constants, arrays, and constants.
- Implement generics, bounds, variance, trait and interface inheritance, companion and synthetic filtering, enums, arrays, primitives, and Scala property semantics.
- Scala package element metadata now reports nested package simple names through a Scala-specific `PackageElement` wrapper, and primitive Scala field types compare equal to shared `PrimitiveElement` constants.
- Scala inherited trait method generic substitution preserves annotations from Scala interface type arguments, including validation annotations on inherited method parameter types.
- Scala inherited method `ElementQuery` filtering is covered for abstract, concrete, and accessible methods across source-defined class and trait hierarchies.
- Scala emitted field `ElementQuery` filtering is covered for all, private, and accessible field selection, preserving the Scala-specific model that emitted fields are private and reflection-required.
- Scala wildcard generic metadata now resolves unbounded wildcard arguments through the enclosing type parameter bounds for bean-definition generic metadata, including bounded forms such as `NumberThing[?]` and `NumberThing[? <: Double]`.
- Recursive Scala generic type parameter bounds such as `T <: Test[T]` now terminate during compiler-symbol conversion while preserving a useful bounded placeholder chain for the Element API.

### Wave 4: Micronaut Features

- Incrementally enable DI, qualifiers, `@Requires`, lifecycle methods, factories, `@ConfigurationProperties`, `@EachProperty`, executable methods, introspections, validation metadata, and AOP around and introduction advice.
- Support idiomatic Scala collection injection where possible. Scala users should be able to request common Scala collection abstractions such as `scala.collection.Seq` and immutable collection implementations such as `scala.collection.immutable.List` for multi-bean injection instead of being forced to use `java.util` collection types.
- Treat Scala collection support as more than assignability modelling. The generated injection path must supply values that are assignable to the actual Scala constructor, field, or method signature, likely by adding Scala-specific collection conversion or generated adaptation rather than pretending Scala collections are `java.util.Collection`.
- Scala collection injection currently supports constructor, method, and field injection for common Scala collection types, including idiomatic `List[Foo]` source usage, `Set`, `Seq`, `IndexedSeq`, `Vector`, and string-keyed Scala `Map[String, Foo]`, by generating Java-to-Scala collection adaptation code without adding a compile-time Scala dependency to `core-processor`. Scala collection injection also preserves `BeanRegistration[T]` element semantics instead of injecting raw `T` beans.
- Scala collection configuration binding currently supports common Scala collection targets, including `scala.collection.immutable.List[T]` and string-keyed `scala.collection.immutable.Map[String, T]`, through Scala collection converters and the existing sub-map property binding path.
- Scala optional bean injection currently supports `Option[T]` for constructor, method, and field injection by reusing Micronaut's existing optional bean lookup and adapting the generated value to `scala.Option`.
- Scala field-access introspection should use Micronaut's shared bean-property resolution pattern where possible. Scala source properties still need Scala-native modelling for idiomatic accessors, but `@Introspected(accessKind = FIELD, visibility = ANY)` now resolves emitted Scala fields through the shared `AstBeanPropertiesUtils` path rather than changing core introspection writers.
- Scala enum introspection currently supports instantiation through Scala's emitted `valueOf(String)` method and enum constructor properties. Enum constant values remain a known gap because the public `EnumBeanIntrospection.EnumConstant` contract is bound to Java `Enum` values.
- Scala introduction proxies resolve inherited generic method metadata through source-defined trait type arguments, including nested generics, method type variables, and arrays. Visitor-added metadata is covered on inherited generated introduction methods for generic return types, generic publisher parameters, resolved generic parameters, and `@InterceptorBean` bindings.
- Add focused Scala regressions before each implementation fix.
- Re-enable parity tests as support lands.

### Wave 5: Docs and Examples

- Start with simple IOC, introspection, and config examples.
- Add HTTP, controller, and AOP examples after the basics are working.
- Scala docs examples now cover simple IOC, introspection, config binding,
  HTTP/client/filter examples, and AOP examples including around, introduction,
  lifecycle, retry, proxy-target, and reactive around advice.
- Scala retry docs examples now use idiomatic Scala `List` return types for
  ordinary application methods instead of Java collection types.
- Scala lifecycle advice docs examples now use Scala `mutable.Map` and `Option`
  for ordinary application state lookup instead of Java collection/optional types.
- Keep Scala examples idiomatic: constructor parameters, case classes, traits, `val`, and `var`.
- Do not force JavaBean-style code unless the feature specifically requires Java interop.
- Maintain `test-suite-scala/DISABLED_TESTS.md` as the docs backlog.

## Test Plan

### Proof-of-Concept Validation

```bash
./gradlew :micronaut-inject-scala:compileScala
./gradlew :micronaut-inject-scala-test:test --tests '*Scala*PoC*'
./gradlew :test-suite-scala:test --tests '*HelloControllerSpec*'
```

### Per-Feature Validation

- Run the focused `inject-scala-test` spec first.
- Run the equivalent Java, Groovy, or Kotlin source spec when useful for behavioral comparison.
- Run `:micronaut-inject-scala-test:test` after each feature group.
- Run `:test-suite-scala:test` after each docs batch.

### Final Validation

```bash
./gradlew :micronaut-inject-scala:test :micronaut-inject-scala-test:test :test-suite-scala:test
./gradlew docs
./gradlew japiCmp
```

Run `japiCmp` because `VisitorContext.Language.SCALA` is public API.

## Assumptions

- Scala 3 only; no Scala 2.x support or cross-build.
- No `context-scala` runtime module is needed for the first implementation because Scala runs as JVM bytecode.
- The first plugin does not attempt typer replacement, macro integration, Scala.js, or Scala Native.
- Unsupported or blocked parity and docs cases stay tracked in disabled-test inventories rather than being silently dropped.
