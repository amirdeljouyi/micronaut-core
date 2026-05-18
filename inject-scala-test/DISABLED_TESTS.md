# Disabled Scala Tests

This file tracks parity tests that are not yet ported to the Scala 3 adapter.
It was generated from the current checkout by scanning direct subclasses of Java `AbstractTypeElementSpec`, Groovy `AbstractBeanDefinitionSpec`, and Kotlin `AbstractKotlinCompilerSpec`.

## Current Scope

Wave 1 covers simple Scala classes, primary constructors, methods, constructor `val`/`var` properties, Java-visible annotations, basic type resolution, generated bean definitions, generated bean introspections, and a basic `TypeElementVisitor`.

Additional Wave 4 smoke coverage now exists for named qualifiers, `@Requires`,
constructor array injection, constructor `@Value` injection, field and method injection, post-construct and
pre-destroy lifecycle methods, `@InjectScope` dependencies, `BeanRegistration`
injection for constructor, field, method, collection, array, and named parameters,
simple `@Factory` methods, and executable methods, mutable
and immutable case-class `@ConfigurationProperties`, mixed configuration/bean
constructor injection, `@EachProperty`, and factory `val` property beans.
Nested `@ConfigurationProperties` are covered for Scala companion-object
nested classes. Source-defined default scopes, explicit scope overrides, factory
method overrides, and unscoped `@Bean` factory methods are covered for Scala
annotation stereotypes. Singleton Scala enum beans are rejected with the core
bean-definition error. Abstract bean collection filtering, abstract bean
definitions with injection points, qualifier-only beans, and AOP-only beans are
covered for Scala. Bean-definition type-string formatting and class-level
`@Bean(typed=...)` exposed type validation, including subclass rejection, are
partially covered for Scala.
Required and optional `@Autowired` field and method injection are covered for
Scala. Dynamic `RuntimeBeanDefinition` registration from Scala source is
covered. Qualifier metadata on field-targeted Scala `var` injection is covered
for property setter injection. Evaluated expressions on Scala bean definitions
and executable methods are covered for Graal build-time initialization. Constructor-copy
introspection through an abstract Scala superclass is covered. Bean-introspection
constructor argument generics are covered; superclass introspection constructor
forwarding for byte arrays and boxed Boolean values is tracked with
`@PendingFeature`. Executable route methods inherited from source-defined
Scala traits are covered.

Recent Wave 3 coverage also includes class-body `val`/`var` properties,
generic type arguments, annotation arrays/class literals/enum constants,
repeatable annotations, field constant values, trait/interface assignability,
and Scala inner-class and companion-object nested-class enclosing type metadata,
and Scala enum declarations through `EnumElement`. Source-defined Scala annotation defaults,
stereotypes, and member aliases are now covered with compiler-symbol/typed-tree
metadata, including inherited source annotations through source class
hierarchies, classpath supertypes, bean definition processing, and
`TypeElementVisitor` annotation mutations, including mutation-added stereotypes
for source-defined Scala annotations. Source-defined Scala annotation classes
are exposed as `ClassElement`s; Java `@Retention` and `@Target` metadata on
those source-defined `StaticAnnotation` classes remains tracked with
`@PendingFeature` until it can be recovered from supported compiler APIs.
`TypeElementVisitor` annotation mutations are covered for generated introduction
proxy methods.
Bean import is tracked as unsupported for Scala and should be documented as
unsupported in a future docs pass.

## Classification Rules

- `already covered`: a narrow Scala PoC assertion already exercises the core scenario, although broader parity can still be added later.
- `portable`: suitable for an early Scala port once the test harness grows beyond the Wave 1 smoke tests.
- `blocked`: useful parity coverage, but blocked on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support.
- `unsupported`: useful to document as unsupported for Scala rather than silently dropping from the backlog.
- `scala-specific`: Java, Groovy, or Kotlin syntax/compiler behavior that should be skipped or replaced with Scala-native coverage.

## Inventory Summary

| Source | already covered | portable | blocked | unsupported | scala-specific | total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Java AbstractTypeElementSpec | 14 | 2 | 167 | 1 | 11 | 195 |
| Groovy AbstractBeanDefinitionSpec | 2 | 7 | 67 | 0 | 11 | 87 |
| Kotlin AbstractKotlinCompilerSpec | 1 | 3 | 12 | 0 | 3 | 19 |
| Total | 17 | 12 | 246 | 1 | 25 | 301 |

## First Portable Ports

Start with small tests that exercise already-supported Scala forms before enabling blocked feature groups. Suggested first ports:

- `inject-java-test/src/test/groovy/io/micronaut/inject/visitor/beans/BeanIntrospectionSpec.groovy`
- `inject-java/src/test/groovy/io/micronaut/inject/beans/BeanDefinitionSpec.groovy`
- `inject-groovy-test/src/main/groovy/io/micronaut/ast/transform/test/AbstractEvaluatedExpressionsSpec.groovy`
- `inject-groovy/src/test/groovy/io/micronaut/inject/beans/AbstractBeanSpec.groovy`
- `inject-groovy/src/test/groovy/io/micronaut/inject/beans/BeanDefinitionSpec.groovy`
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/BeanIntrospectionSpec.groovy`
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/ClassElementSpec.groovy`

## Detailed Inventory

### Java AbstractTypeElementSpec

- `inject-java-test/src/main/groovy/io/micronaut/annotation/processing/test/AbstractEvaluatedExpressionsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java-test/src/test/groovy/io/micronaut/inject/annotation/AnnotationMetadataBuilderSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java-test/src/test/groovy/io/micronaut/inject/annotation/AnnotationTransformerSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java-test/src/test/groovy/io/micronaut/inject/annotation/InheritedNullableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java-test/src/test/groovy/io/micronaut/inject/beanimport/BeanImportSpec.groovy` - unsupported: bean import is not implemented for Scala and should be documented as unsupported in a future docs pass
- `inject-java-test/src/test/groovy/io/micronaut/inject/visitor/ElementAnnotateSpec.groovy` - already covered: Scala `TypeElementVisitor` annotation mutations are covered for classes, methods, parameters, introspection properties, source-defined stereotypes, and introduction proxy methods
- `inject-java-test/src/test/groovy/io/micronaut/inject/visitor/InheritanceVisitorSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java-test/src/test/groovy/io/micronaut/inject/visitor/beans/AnnotatedIntrospectedSpec.groovy` - already covered: basic @Introspected class metadata is covered by ScalaPoCSpec
- `inject-java-test/src/test/groovy/io/micronaut/inject/visitor/beans/BeanIntrospectionGenericsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java-test/src/test/groovy/io/micronaut/inject/visitor/beans/BeanIntrospectionSpec.groovy` - portable: partially covered for constructor argument generics; byte array and boxed Boolean superclass constructor forwarding are tracked with `@PendingFeature`; remaining introspection, access-kind, creator, generics, metadata, enum, interface, and external-class cases should be ported incrementally
- `inject-java-test/src/test/groovy/io/micronaut/inject/visitor/beans/BuildClassElementSpec.groovy` - already covered: basic buildClassElement coverage is present in ScalaPoCSpec
- `inject-java/src/test/groovy/io/micronaut/annotation/AnnotateClassSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/AnnotateFieldSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/AnnotateFieldTypeSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/AnnotateMethodParameterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/AnnotateMethodReturnSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/AnnotateMethodSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/AnnotatePropertySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/AnnotateTypeArgSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/JavaEnumElementSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/annotation/NonNullabilityAnnotationsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/NullabilityAnnotationsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/NullabilityFutureAnnotationsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/AddsRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/AddsUnseenInnerRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/AddsUnseenRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/AnnotationMappingSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/MapToRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/MappedValueHasDefaultSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/RemapToRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/ReplacesRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/SourceAnnotationHasDefaultsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/TransformNotInheritedAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/TransformToInheritedAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/mapping/TransformsToRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/annotation/processing/visitor/JavaReconstructionSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/annotation/processing/visitor/JavaVisitorContextSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/annotation/processing/visitor/JavaVisitorSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/aop/adapter/MethodAdapterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/adapter/intercepted/InterceptedAdapterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/AbstractClassIntroductionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/AnnotatedConstructorArgumentSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/AroundCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/AroundConstructCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/ExecutableFactoryMethodSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/FinalModifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/GeneratedAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/InheritedAnnotationMetadataSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/InjectFieldAbstractIntroductionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/IntroductionAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/IntroductionCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/IntroductionGenericTypesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/IntroductionInnerInterfaceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/IntroductionWithAroundSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/LifeCycleWithProxySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/LifeCycleWithProxyTargetSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/OriginatingElementsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/PostConstructInterceptorCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/compile/ValidatedNonBeanSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/factory/AdviceDefinedOnFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/factory/SessionProxySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/factory/mapped/FactoryMappedAdviceReflectionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/factory/mapped/FactoryMappedAdviceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/introduction/InterfaceIntroductionAdviceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/introduction/IntroductionAdviceWithNewInterfaceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/introduction/MappedIntroductionOnConcreteClassSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/introduction/MyAbstractRepoSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/introduction/beans/IntroducedBeanVisitorSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/introduction/repeatable/IntroducedWithRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/introduction/with_around/IntroductionInnerInterfaceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/introduction/with_around/IntroductionWithAroundOnConcreteClassSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/aop/named2/NamedAopAdviceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/core/io/service/ServiceLoaderFeatureSpec.groovy` - already covered: Scala evaluated expressions on bean definitions and executable methods are registered for Graal build-time initialization
- `inject-java/src/test/groovy/io/micronaut/inject/aliasfor/AliasForQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AddStereotypesFromVisitorSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AnnotatedFieldWithSetterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AnnotationDefaultValuesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AnnotationInheritanceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AnnotationMapperSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AnnotationMetadataHierarchySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AnnotationMetadataWriterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AnnotationRemapperSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/AnnotationsOnGenericTypesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/ArgumentAnnotationMetadataSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/BeanDefinitionAnnotationMetadataSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/JavaAnnotationMetadataBuilderSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/JavaxMapperSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/PriorityAnnotationMapperSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/RemoveAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/RetentionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/repeatable/MapToRepeatableSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/repeatable/RepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/annotation/repeatable/TransformToRepeatableSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/ast/beans/BeanElementVisitorSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/autowired/AutowiredSpec.groovy` - already covered: Scala field and method `@Autowired` injection is covered for required and optional dependencies, including optional value injection and multi-argument method skipping
- `inject-java/src/test/groovy/io/micronaut/inject/beanbuilder/BeanElementBuilderFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/beanbuilder/BeanElementBuilderMultipleFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/beanbuilder/BeanElementBuilderSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/beanbuilder/BuildElementBuilderAopOnMethodSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/beanbuilder/BuildElementBuilderAopOnTypeSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/beanbuilder/BuildElementBuilderProcessedMethodsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/beans/AbstractBeanSpec.groovy` - already covered: Scala source-level abstract bean scenarios are covered for collection filtering, abstract definitions with injection points, qualifier-only beans, and AOP-only beans
- `inject-java/src/test/groovy/io/micronaut/inject/beans/BeanDefinitionSpec.groovy` - portable: partially covered for type-string formatting and class-level `@Bean(typed=...)` exposed type validation, including subclass rejection; remaining factory, generic, qualifier, and metadata cases should be ported incrementally
- `inject-java/src/test/groovy/io/micronaut/inject/beans/BeanRegistrationSpec.groovy` - already covered: Scala constructor, field, method, collection, array, and named `BeanRegistration` injection are covered
- `inject-java/src/test/groovy/io/micronaut/inject/beans/RuntimeBeanDefinitionSpec.groovy` - already covered: Scala source-level dynamic bean definition registration is covered; the remaining runtime builder assertions are not language-adapter-specific
- `inject-java/src/test/groovy/io/micronaut/inject/beans/concopy/ConstructorCopySpec.groovy` - already covered: Scala introspection handles constructor forwarding through an abstract superclass
- `inject-java/src/test/groovy/io/micronaut/inject/beans/visitor/MapperVisitorSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/ConfigPropertiesParseSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/ConfigurationPropertiesBuilderSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/ConfigurationPropertiesInjectSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/ImmutableConfigurationPropertiesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/InheritedConfigurationReaderPrefixSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/InterfaceConfigurationPropertiesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/ValidatedConfigurationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/VisibilityIssuesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/eachbeaninterceptor/EachBeanInterceptorSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/eachbeanparameter/EachBeanParameterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/eachbeanreplaces/EachBeanReplacesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/itfce/InterfaceNestingSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/nesting/EachPropertyNestingSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configproperties/records/RecordNestingSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/inject/configuration/ConfigurationBuilderSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configuration/ConfigurationBuilderSpec2.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configuration/ConfigurationJsonSchemaDefaultsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configuration/ConfigurationJsonSchemaSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configuration/ConfigurationJsonSchemaValidationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configuration/ConfigurationMetadataSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configuration/ExternalConfigurationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/configurations/RequiresBeanCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/constructor/arrayinjection/ConstructorArrayInjectionSpec.groovy` - already covered: Scala array constructor injection is covered for bean definition parsing and runtime injection
- `inject-java/src/test/groovy/io/micronaut/inject/context/NoPackageSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/inject/defaultimpl/DefaultImplementationSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/inject/errors/SingletonOnEnumSpec.groovy` - already covered: singleton Scala enum beans are rejected with the core bean-definition error
- `inject-java/src/test/groovy/io/micronaut/inject/executable/ExecutableBeanSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/executable/ExecutableSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/executable/inheritance/InheritedExecutableSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/ExecutableAnnotationOnFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/FactoryBeanDefinitionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/FactoryOfBeanWithUnresolvedClassSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/beanfield/FactoryBeanFieldSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/beanfield/FactoryFieldArraySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/beanmethod/FactoryBeanMethodSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/collection/FactoryArraySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/collection/FactoryCollectionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/enummethod/FactoryEnumSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/generics/GenericFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/inheritance/FactoryAbstractInheritanceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/lifecycle/PreDestroyOnBeanAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/multiple/MethodSameNameSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/named/ImplicitNamedSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/nullreturn/NullReturnFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/factory/proxytarget/FactoryWithScopedProxySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/field/inheritance/FieldInheritanceInjectionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/field/simpleinjection/FieldInjectionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/foreach/EachPropertyParseSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/generics/GenericTypeArgumentsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/generics/TypeArgumentsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/inheritance/AbstractInheritanceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/injectscope/InjectScopeSpec.groovy` - already covered: Scala constructor and method parameters support `@InjectScope` scoped dependency cleanup
- `inject-java/src/test/groovy/io/micronaut/inject/lifecycle/PostConstructCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/lifecycle/PreDestroyCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/lifecycle/beanwithpostconstruct/BeanWithPostConstructSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/lifecycle/beanwithprivatepostconstruct/BeanWithPostConstructSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/method/arrayinjection/SetterArrayInjectionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/method/builderinjection/BuilderStyleInjectionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/method/qualifierinjection/SetterWithQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/optional/OptionalPropertySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/property/PropertyAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/provider/BeanProviderSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/provider/DisableErrorOnMissingBeanProviderSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/qualifiers/annotation/AnnotationQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/qualifiers/annotationmember/NonBindingQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/qualifiers/named/NamedQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/qualifiers/repeatable/RepeatableQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/qualifiers/replaces/AnnotateReplacesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/qualifiers/replaces/ReplacesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/records/RecordBeansSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/inject/requires/RequiresBeanPropertiesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/requires/RequiresSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/inject/scope/DefaultScopeSpec.groovy` - already covered: source-defined default scope, explicit scope override, factory method override, and unscoped `@Bean` factory method variants are covered
- `inject-java/src/test/groovy/io/micronaut/inject/value/factorywithvalue/FactoryWithValueSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/ClassElementAnnotationsRetaining.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/ClassElementSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/CustomVisitorSpec.groovy` - already covered: basic TypeElementVisitor class/method/property observation is covered by ScalaPoCSpec
- `inject-java/src/test/groovy/io/micronaut/visitors/DocumentationSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-java/src/test/groovy/io/micronaut/visitors/ImportTypeElementSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/InternalVisitor1Spec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/InternalVisitor2Spec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/InternalVisitor3Spec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/IntroductionVisitorSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/MixinSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/NullableElementSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/PostponedVisitorsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/PropertyElementSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-java/src/test/groovy/io/micronaut/visitors/query/TypeElementQuerySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support

### Groovy AbstractBeanDefinitionSpec

- `inject-groovy-test/src/main/groovy/io/micronaut/ast/transform/test/AbstractEvaluatedExpressionsSpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-groovy/src/test/groovy/io/micronaut/aop/adapter/MethodAdapterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/AbstractClassIntroductionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/AroundCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/ExecutableDefaultMethodSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/ExecutableSuperclassSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/FinalModifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/InheritedAnnotationMetadataSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/IntroductionGenericTypesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/IntroductionWithAroundSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/LifeCycleWithProxySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/PropertyAdviceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/compile/ValidatedNonBeanSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/factory/SessionProxySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/introduction/InterfaceIntroductionAdviceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/introduction/IntroductionAdviceWithNewInterfaceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/introduction/with_around/IntroductionInnerInterfaceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/aop/introduction/with_around/IntroductionWithAroundOnConcreteClassSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/ast/groovy/annotation/GroovyAnnotationMetadataBuilderSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/ast/groovy/visitor/GroovyBeanPropertiesSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/ast/groovy/visitor/GroovyDocumentationSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/ast/groovy/visitor/GroovyEnclosedElementsSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/ast/groovy/visitor/GroovyEnumElementSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/ast/groovy/visitor/GroovyReconstructionSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/expressions/TestExpressionsInjectionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/expressions/TestExpressionsUsageSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/aliasfor/AliasForQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/AnnotationMetadataWriterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/GroovyAnnotationInheritanceSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/GroovyMappedStereotypesSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/RemoveAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/RetentionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/modify/AnnotateFieldSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/modify/AnnotateFieldTypeSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/modify/AnnotateMethodParameterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/modify/AnnotateMethodReturnSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/modify/AnnotateMethodSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/modify/AnnotatePropertySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/repeatable/AddsRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/repeatable/RepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/annotation/repeatable/ReplacesRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/beanbuilder/BeanElementBuilderFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/beanbuilder/BeanElementBuilderMultipleFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/beanbuilder/BeanElementBuilderSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/beans/AbstractBeanSpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-groovy/src/test/groovy/io/micronaut/inject/beans/BeanDefinitionSpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-groovy/src/test/groovy/io/micronaut/inject/configproperties/ConfigPropertiesParseSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/configproperties/ConfigurationPropertiesBuilderSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/configproperties/ImmutableConfigurationPropertiesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/configproperties/InheritedConfigurationReaderPrefixSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/configproperties/InterfaceConfigurationPropertiesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/configproperties/ValidatedConfigurationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/configproperties/VisibilityIssuesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/configuration/GroovyConfigBuilderSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/inject/context/NoPackageSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/inject/errors/GroovySingletonSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-groovy/src/test/groovy/io/micronaut/inject/errors/RouteTraitSpec.groovy` - already covered: Scala route methods inherited from source-defined traits are exposed as executable methods
- `inject-groovy/src/test/groovy/io/micronaut/inject/executable/ExecutableBeanSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/executable/inheritance/InheritedExecutableSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/factory/FactoryBeanDefinitionSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/factory/FactoryBeanFieldSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/factory/FactoryEnumSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/factory/generics/GenericFactorySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/factory/inheritance/FactoryAbstractInheritanceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/factory/named/ImplicitNamedSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/factory/proxytarget/FactoryWithScopedProxySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/generics/GenericTypeArgumentsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/generics/TypeArgumentsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/inheritance/AbstractInheritanceSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/lifecyle/BeanWithPreDestroySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/lifecyle/PostConstructCompileSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/lifecyle/PreDestroyOnBeanAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/property/PropertyWithQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/qualifiers/MultipleQualifiersSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/qualifiers/NamedSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/qualifiers/repeatable/RepeatableQualifierSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/requires/RequiresBeanPropertiesSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/value/ValueParseSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/AnnotationMetadataSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/BeanIntrospectionSpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/ClassElementSpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/CustomVisitorSpec.groovy` - already covered: basic TypeElementVisitor class/method/property observation is covered by ScalaPoCSpec
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/ElementAnnotateSpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/IntroductionVisitorSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/PropertyElementSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-groovy/src/test/groovy/io/micronaut/inject/visitor/TypeElementQuerySpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-groovy/src/test/groovy/io/micronaut/validation/ValidatedParseSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support

### Kotlin AbstractKotlinCompilerSpec

- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AddsRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AddsUnseenInnerRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AddsUnseenRepeatableAnnotationSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AnnotateArraySpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AnnotateFieldSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AnnotateFieldTypeSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AnnotateMethodParameterSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AnnotateMethodReturnSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AnnotateMethodSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/annotations/AnnotatePropertySpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/aop/introduction/MyIsEnumInTypeArgumentSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/aop/introduction/MyRepo3Spec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/ast/visitor/KotlinEnumElementSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/beans/SingletonSpec.groovy` - already covered: basic singleton constructor injection is covered by ScalaPoCSpec
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/inject/ast/ClassElementSpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/inject/generics/GenericTypeArgumentsSpec.groovy` - blocked: depends on Wave 3 Element/annotation completeness or Wave 4 Micronaut feature support
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/visitor/BeanIntrospectionSpec.groovy` - portable: candidate for early Scala port after the harness grows beyond the Wave 1 smoke coverage
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/visitor/KotlinReconstructionSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
- `inject-kotlin/src/test/groovy/io/micronaut/kotlin/processing/visitor/order/VisitorOrderSpec.groovy` - scala-specific: language-specific Java/Groovy/Kotlin syntax or compiler behavior; replace with Scala-native coverage when relevant
