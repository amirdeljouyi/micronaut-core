# Scala 3 Support for Micronaut Core

Last updated: 2026-05-21

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

### `micronaut-build` Docs Tooling

Add Scala support to the docs snippet tooling in the sibling `micronaut-build` repository before relying on Scala snippets from Micronaut Core docs. Treat this as a companion branch in `/Users/graemerocher/dev/micronaut/build` or the active local `micronaut-build` checkout, and keep its commits separate from Micronaut Core commits so the required build-plugin changes are reviewable on their own.

- Extend `LanguageSnippetMacro` to treat `scala` as a first-class snippet language.
- Use `test-suite-scala` as the default Scala snippet project, `src/<source>/scala` as the source folder, and `.scala` as the file extension.
- Keep existing Java, Groovy, Kotlin, and Python snippet behavior unchanged.
- Add focused `LanguageSnippetMacroSpec` coverage that proves Scala snippets resolve from `test-suite-scala` and from explicit `project` / `project-base` attributes.
- Add a Scala-only snippet selection mechanism, such as a `language="scala"` or `languages="scala"` attribute, so Scala-only guide pages do not emit missing-snippet warnings while the macro checks Java, Python, Kotlin, and Groovy fallbacks.
- Validate Micronaut Core docs against the updated build plugin with `--include-build ../build` when working from the standard sibling checkout, then publish or otherwise consume the updated `micronaut-build` snapshot before validating without the included build.
- Do not hard-code Scala feature examples in `.adoc` files as a fallback. If a Scala feature is documented with code, it must be backed by a real snippet source.
- Treat build-tool setup examples the same way: Maven, Gradle, sbt, and Mill examples should come from maintained fixture files or docs example sources. If the current snippet macro cannot address those files, add the minimal docs-tooling support instead of inlining stale configuration blocks.

Reference:

- https://docs.gradle.org/current/userguide/scala_plugin.html

## Implementation Waves

### Wave 0: Build Scaffolding

- Add Scala 3 catalog entries, module includes, build files, package metadata, plugin descriptor, and minimal compile tasks.
- Prefer `managed-scala3` in the version catalog and use one Scala 3 line only.
- Add the required `micronaut-build` docs-snippet support for Scala so documentation can use source-backed `snippet::` macros from the start.
- Commit the companion `micronaut-build` work in focused checkpoints before the Core docs commits that depend on it.

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

Current grounding pass: Java `194` specs / `1065` features, Groovy `86`
specs / `473` features, Kotlin `19` specs / `194` features, and Scala `11`
specs / `168` features. Scala already covers the proof of concept, many
Element API cases, Scala collections, explicit nulls, core DI, configuration
properties, basic AOP, introspections, and annotation stereotypes. Refresh the
Scala disabled-test catalog before porting more tests so it reflects the
current checkout rather than the original proof-of-concept gap list.

#### Scala Test Parity Gap Catalog

Priority 0 is the catalog refresh. Update
`inject-scala-test/DISABLED_TESTS.md` from the current checkout and reclassify
each entry as `covered`, `candidate`, `scala-specific`, or `unsupported`. Keep
`BeanImportSpec` as `unsupported`. Skip Kotlin suspend/coroutine cases, Java
record-only cases, Groovy dynamic/singleton-only cases, and exact Java
package-private behavior unless there is a Scala-native equivalent.

Priority 0 also includes Element API and annotation parity tests:

- Maintain `ScalaReconstructionSpec` for field, method, parameter, and return
  reconstruction; arrays; wildcards; type variables on classes and methods;
  inherited type arguments; traits/interfaces; enums; and inner/nested classes.
- Maintain `ScalaVisitorContextSpec` for `getClassElement`, `getClassElements`, enum
  lookup, nested-class lookup, missing-class behavior, and no classloading
  assumptions.
- Maintain `ScalaElementMutationParitySpec` for visitor-added annotations on class,
  method, field, property, parameter, return type, field type, and type
  arguments; repeatables; empty arrays; and stereotypes.
- Maintain `ScalaAnnotationMetadataParitySpec` for annotation defaults, nested
  annotations, class literals, enum constants, arrays, retention/target
  filtering, source-defined stereotypes, aliasing,
  mapper/transformer/remapper behavior, removal behavior, and
  `ProcessingException` messages with originating elements.
- Visitor-added annotations on Scala return/parameter type `ClassElement`
  wrappers and generic type arguments remain tracked as pending feature tests
  because mutation metadata is not preserved when those wrapper copies are
  re-read from the captured Scala element.

Priority 1 extends introspection, bean definition, and configuration parity:

- Extend `ScalaBeanIntrospectionSpec` for property include/exclude/access-kind
  rules, covariant properties, numbered property names, creator selection,
  executable methods, validation metadata, generic placeholders, enum creator
  behavior, interface/trait inheritance, and external-class introspection where
  Scala can model it.
- Extend `ScalaBeanDefinitionSpec` for unresolved-type diagnostics, provider
  and `BeanProvider` injection, optional property injection,
  repeatable/non-binding qualifiers, `@Replaces`, inherited qualifier negative
  cases, abstract parent injection, factory field/val/method beans, generic
  factories, enum-returning factories, null-return factories, and factory
  method name collisions.
- Extend configuration coverage for interface/trait config props, nested config
  props, validation cascades, inherited prefixes/aliases, raw maps, primitives,
  `@EachProperty` nesting/replacement, and factory-backed config props.

Priority 2 adds AOP, lifecycle, and executable parity:

- Add `ScalaAopParitySpec` for around construct, around advice on inherited
  trait/default methods, introduction with around, mapped introduction,
  additional interfaces, abstract class/trait introduction, final-method
  errors, named AOP target lookup, adapter methods, and factory-level advice.
- Extend lifecycle coverage for inherited `@PostConstruct`/`@PreDestroy`,
  hooks on `@Bean` factory members, hooks with AOP/proxy-target, and
  private/protected hook behavior where Scala can express it.
- Extend executable coverage for inherited executable trait methods,
  overloaded methods, generics, annotation metadata inheritance, and executable
  factory methods.

Priority 3 covers visitor-generated beans and build-time behavior:

- Add `ScalaBeanElementBuilderParitySpec` for visitor-created beans, associated
  factory beans, multiple generated factories, generated methods, and AOP on
  generated beans.
- Add visitor-order and postponed-visitor coverage only where Scala's
  compiler-plugin phase model can reproduce the Java/Groovy/Kotlin intent.
- Add evaluated-expression parity for `@Requires` expressions,
  context/property/environment expressions, expression injection, and
  annotation-level expressions.

Iteration rules:

- Add tests in focused commits by priority group; commit after each completed
  group.
- Prefer real passing tests. Use `@PendingFeature` only for an understood Scala
  gap and record the same item in `inject-scala-test/DISABLED_TESTS.md`.
- Keep Scala snippets idiomatic: case classes, traits, constructor params,
  `val`/`var`, `Option`, explicit nulls, and Scala collections.
- Do not add production-code changes while refreshing the catalog; production
  fixes should follow failed or pending tests.

### Wave 3: Element and Annotation Completeness

- Implement annotation values, defaults, nested annotations, repeatables, stereotypes, aliases, retention and targets, nullability, class literals, enum constants, arrays, and constants.
- Implement generics, bounds, variance, trait and interface inheritance, companion and synthetic filtering, enums, arrays, primitives, and Scala property semantics.
- Scala package element metadata now reports nested package simple names through a Scala-specific `PackageElement` wrapper, and primitive Scala field types compare equal to shared `PrimitiveElement` constants.
- Scala inherited trait method generic substitution preserves annotations from Scala interface type arguments, including validation annotations on inherited method parameter types.
- Scala inherited method `ElementQuery` filtering is covered for abstract, concrete, and accessible methods across source-defined class and trait hierarchies.
- Scala emitted field `ElementQuery` filtering is covered for all, private, and accessible field selection, preserving the Scala-specific model that emitted fields are private and reflection-required.
- Scala wildcard generic metadata now resolves unbounded wildcard arguments through the enclosing type parameter bounds for bean-definition generic metadata, including bounded forms such as `NumberThing[?]` and `NumberThing[? <: Double]`.
- Recursive Scala generic type parameter bounds such as `T <: Test[T]` now terminate during compiler-symbol conversion while preserving a useful bounded placeholder chain for the Element API.
- Scala bean introspection preserves generic placeholders with upper bounds on generic bean properties and nested Scala collection property type arguments.
- Scala bean introspection handles protobuf-style generic superclass shapes without recursive generic traversal failures.

### Wave 4: Micronaut Features

- Incrementally enable DI, qualifiers, `@Requires`, lifecycle methods, factories, `@ConfigurationProperties`, `@EachProperty`, executable methods, introspections, validation metadata, and AOP around and introduction advice.
- Support idiomatic Scala collection injection where possible. Scala users should be able to request common Scala collection abstractions such as `scala.collection.Seq` and immutable collection implementations such as `scala.collection.immutable.List` for multi-bean injection instead of being forced to use `java.util` collection types.
- Treat Scala collection support as more than assignability modelling. The generated injection path must supply values that are assignable to the actual Scala constructor, field, or method signature, likely by adding Scala-specific collection conversion or generated adaptation rather than pretending Scala collections are `java.util.Collection`.
- Scala collection injection currently supports constructor, method, and field injection for common Scala collection types, including idiomatic `List[Foo]` source usage, `Set`, `Seq`, `IndexedSeq`, `Vector`, mutable `Buffer`/`Seq`/`Set`/`Iterable`, and string-keyed Scala `Map[String, Foo]` including mutable maps, by generating Java-to-Scala collection adaptation code without adding a compile-time Scala dependency to `core-processor`. Scala collection injection also preserves `BeanRegistration[T]` element semantics instead of injecting raw `T` beans.
- Scala collection configuration binding currently supports common Scala collection targets, including `scala.collection.immutable.List[T]`, mutable `Buffer[T]`, mutable `Set[T]`, string-keyed `scala.collection.immutable.Map[String, T]`, and string-keyed `scala.collection.mutable.Map[String, T]`, through Scala collection converters and the existing sub-map property binding path.
- Scala optional bean injection currently supports `Option[T]` for constructor, method, and field injection by reusing Micronaut's existing optional bean lookup and adapting the generated value to `scala.Option`.
- Scala field-access introspection should use Micronaut's shared bean-property resolution pattern where possible. Scala source properties still need Scala-native modelling for idiomatic accessors, but `@Introspected(accessKind = FIELD, visibility = ANY)` now resolves emitted Scala fields through the shared `AstBeanPropertiesUtils` path rather than changing core introspection writers.
- Scala bean-definition `@Order` metadata is covered for top-level beans and companion-object nested beans.
- Scala bean definitions are covered for packages with uppercase path segments.
- Scala bean-definition type-variable resolution is covered for concrete array type arguments inherited through parent generic interfaces.
- Scala enum introspection currently supports instantiation through Scala's emitted `valueOf(String)` method and enum constructor properties. Enum constant values remain a known gap because the public `EnumBeanIntrospection.EnumConstant` contract is bound to Java `Enum` values.
- Scala introduction proxies resolve inherited generic method metadata through source-defined trait type arguments, including nested generics, method type variables, and arrays. Visitor-added metadata is covered on inherited generated introduction methods for generic return types, generic publisher parameters, resolved generic parameters, and `@InterceptorBean` bindings.
- Add focused Scala regressions before each implementation fix.
- Re-enable parity tests as support lands.

### Wave 5: Docs and Examples

- Add a dedicated Scala section under `src/main/docs/guide/languageSupport`.
- Wire the Scala section into `src/main/docs/guide/toc.yml` under `languageSupport`.
- Explain how Micronaut Scala support is implemented: Scala 3 compiler plugin, typed compiler trees, Scala Element API wrappers, shared annotation metadata builder model, shared bean-definition writers, and reflection-free compiler-symbol processing where possible.
- Explain build setup for Maven, Gradle, sbt, and Mill. Keep these examples backed by real fixture files or source snippets rather than hard-coded `.adoc` listings.
- Document Scala-specific support:
  - Scala collection injection and configuration binding for common immutable and mutable collection types.
  - `Option[T]` bean injection.
  - Scala explicit nulls / union type nullability such as `T | Null`.
  - Case classes for DTOs, bean introspections, immutable configuration properties, and request/response bodies.
  - Scala annotation target syntax such as `@(Constraint @field)` when Java annotations need to land on generated fields.
  - Traits for interfaces and AOP introduction advice.
  - `val` and `var` property semantics, including when JavaBean-style mutable properties are still required.
- Start with simple IOC, introspection, and config examples.
- Add HTTP, controller, and AOP examples after the basics are working.
- Scala docs examples now cover simple IOC, introspection, config binding,
  HTTP/client/filter examples, and AOP examples including around, introduction,
  lifecycle, retry, proxy-target, and reactive around advice.
- Scala retry docs examples now use idiomatic Scala `List` return types for
  ordinary application methods instead of Java collection types.
- Scala introduction docs examples now use idiomatic Scala `List` return types
  for generic repository-style traits instead of Java collection types.
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

- After refreshing the Scala parity catalog, run:

```bash
./gradlew :micronaut-inject-scala-test:test --tests '*Scala*'
```

- For each new parity spec group, run the focused Scala spec first.
- When porting behavior from another language, run the source comparison spec
  if practical, for example the focused Java, Groovy, or Kotlin `--tests`
  selector plus the matching Scala spec.
- Before finishing a parity wave, run:

```bash
./gradlew :micronaut-inject-scala-test:test :test-suite-scala:test
```

- For Scala docs changes, first run the focused `micronaut-build` `LanguageSnippetMacroSpec` after adding Scala snippet support.
- Publish or include the updated `micronaut-build` snapshot before running Micronaut Core docs validation; when using the sibling checkout, prefer `./gradlew --include-build ../build publishGuide` or `./gradlew --include-build ../build docs` for local proof.
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
