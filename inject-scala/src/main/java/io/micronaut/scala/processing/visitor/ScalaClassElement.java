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
package io.micronaut.scala.processing.visitor;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import io.micronaut.inject.ast.ArrayableClassElement;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ConstructorElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.PropertyElementQuery;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Scala class element backed by compiler plugin model data.
 */
public class ScalaClassElement extends AbstractScalaElement implements ArrayableClassElement {

    private final ScalaVisitorContext visitorContext;
    private final ScalaTypeData typeData;
    private final @Nullable ScalaClassData classData;
    private final ScalaElementFactory elementFactory;

    ScalaClassElement(ScalaClassData classData, ScalaVisitorContext visitorContext) {
        this(classData, visitorContext, visitorContext.getScalaAnnotationMetadataBuilder().buildMetadata(classData));
    }

    private ScalaClassElement(ScalaClassData classData, ScalaVisitorContext visitorContext, AnnotationMetadata annotationMetadata) {
        super(
            classData.name(),
            classData.nativeType(),
            classData.modifiers(),
            MutableAnnotationMetadata.of(annotationMetadata)
        );
        this.visitorContext = visitorContext;
        this.classData = classData;
        this.typeData = new ScalaTypeData(classData.name(), false, 0, classData.interfaceType(), Map.of());
        this.elementFactory = visitorContext.getElementFactory();
    }

    ScalaClassElement(ScalaTypeData typeData, ScalaVisitorContext visitorContext, AnnotationMetadata annotationMetadata) {
        super(
            typeData.name(),
            typeData.name(),
            Set.of(ElementModifier.PUBLIC),
            MutableAnnotationMetadata.of(annotationMetadata)
        );
        this.visitorContext = visitorContext;
        this.typeData = typeData;
        this.classData = null;
        this.elementFactory = visitorContext.getElementFactory();
    }

    @Override
    public boolean isInterface() {
        return classData == null ? typeData.interfaceType() : classData.interfaceType();
    }

    @Override
    public boolean isEnum() {
        return classData != null && classData.enumType();
    }

    @Override
    public boolean isAssignable(String type) {
        if (getName().equals(type) || Object.class.getName().equals(type)) {
            return true;
        }
        return classData != null && isAssignableTo(type, classData, Set.of(getName()));
    }

    private boolean isAssignableTo(String type, ScalaClassData data, Set<String> visited) {
        if (data.superType() != null && isTypeAssignable(type, data.superType(), visited)) {
            return true;
        }
        for (ScalaTypeData interfaceType : data.interfaces()) {
            if (isTypeAssignable(type, interfaceType, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTypeAssignable(String type, ScalaTypeData candidate, Set<String> visited) {
        if (type.equals(candidate.name()) || Object.class.getName().equals(type)) {
            return true;
        }
        if (!visited.contains(candidate.name())) {
            Set<String> nextVisited = new java.util.HashSet<>(visited);
            nextVisited.add(candidate.name());
            Optional<ScalaClassElement> sourceElement = visitorContext.sourceClassElement(candidate.name());
            if (sourceElement.isPresent() && sourceElement.get().classData != null) {
                return isAssignableTo(type, sourceElement.get().classData, nextVisited);
            }
            if (sourceElement.isPresent()) {
                return false;
            }
        }
        return visitorContext.getClassElement(candidate.name())
            .filter(classElement -> classElement != this && classElement.isAssignable(type))
            .isPresent();
    }

    @Override
    public Optional<ClassElement> getSuperType() {
        if (classData == null || classData.superType() == null) {
            return Optional.empty();
        }
        return Optional.of(elementFactory.newClassElement(classData.superType()));
    }

    @Override
    public Collection<ClassElement> getInterfaces() {
        if (classData == null) {
            return List.of();
        }
        return classData.interfaces().stream()
            .map(elementFactory::newClassElement)
            .toList();
    }

    @Override
    public boolean isInner() {
        return classData != null && classData.enclosingTypeName() != null;
    }

    @Override
    public Optional<ClassElement> getEnclosingType() {
        if (classData == null || classData.enclosingTypeName() == null) {
            return Optional.empty();
        }
        return visitorContext.sourceClassElement(classData.enclosingTypeName())
            .map(ClassElement.class::cast);
    }

    @Override
    public Map<String, ClassElement> getTypeArguments() {
        return elementFactory.typeArguments(typeData);
    }

    @Override
    public List<PropertyElement> getBeanProperties() {
        return getBeanProperties(PropertyElementQuery.of(getAnnotationMetadata()));
    }

    @Override
    public List<PropertyElement> getSyntheticBeanProperties() {
        return List.of();
    }

    @Override
    public List<PropertyElement> getBeanProperties(PropertyElementQuery propertyElementQuery) {
        if (classData == null) {
            return List.of();
        }
        return classData.properties().stream()
            .map(property -> new ScalaPropertyElement(this, property, visitorContext))
            .map(PropertyElement.class::cast)
            .toList();
    }

    @Override
    public Optional<MethodElement> getPrimaryConstructor() {
        if (classData == null || classData.constructors().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ScalaConstructorElement(this, classData.constructors().get(0), visitorContext));
    }

    @Override
    public Optional<MethodElement> getDefaultConstructor() {
        if (classData == null) {
            return Optional.empty();
        }
        return classData.constructors().stream()
            .filter(constructor -> constructor.parameters().isEmpty())
            .findFirst()
            .map(constructor -> new ScalaConstructorElement(this, constructor, visitorContext));
    }

    @Override
    public <T extends Element> List<T> getEnclosedElements(ElementQuery<T> query) {
        if (classData == null) {
            return List.of();
        }
        ElementQuery.Result<T> result = query.result();
        List<Element> elements = new ArrayList<>();
        Class<T> elementType = result.getElementType();
        if (elementType == ConstructorElement.class) {
            classData.constructors().forEach(constructor -> elements.add(new ScalaConstructorElement(this, constructor, visitorContext)));
        } else if (elementType == MethodElement.class) {
            classData.methods().forEach(method -> elements.add(new ScalaMethodElement(this, method, visitorContext)));
        } else if (elementType == FieldElement.class) {
            classData.fields().forEach(field -> elements.add(new ScalaFieldElement(this, field, visitorContext)));
        } else if (elementType == PropertyElement.class) {
            classData.properties().forEach(property -> elements.add(new ScalaPropertyElement(this, property, visitorContext)));
        } else if (elementType == ClassElement.class) {
            elements.addAll(visitorContext.sourceClassElementsEnclosedBy(getName()));
        } else if (elementType == MemberElement.class) {
            classData.fields().forEach(field -> elements.add(new ScalaFieldElement(this, field, visitorContext)));
            classData.methods().forEach(method -> elements.add(new ScalaMethodElement(this, method, visitorContext)));
            if (!result.isExcludePropertyElements()) {
                classData.properties().forEach(property -> elements.add(new ScalaPropertyElement(this, property, visitorContext)));
            }
        }
        return elements.stream()
            .filter(element -> matches(result, element))
            .map(elementType::cast)
            .toList();
    }

    private <T extends Element> boolean matches(ElementQuery.Result<T> result, Element element) {
        if (result.isOnlyAbstract() && !element.isAbstract()) {
            return false;
        }
        if (result.isOnlyConcrete() && element.isAbstract()) {
            return false;
        }
        if (result.isOnlyStatic() && !element.isStatic()) {
            return false;
        }
        if (result.isOnlyInstance() && element.isStatic()) {
            return false;
        }
        if (result.isOnlyAccessible() && element instanceof MemberElement memberElement) {
            ClassElement fromType = result.getOnlyAccessibleFromType().orElse(this);
            if (!memberElement.isAccessible(fromType)) {
                return false;
            }
        }
        for (Predicate<String> predicate : result.getNamePredicates()) {
            if (!predicate.test(element.getName())) {
                return false;
            }
        }
        if (element instanceof io.micronaut.inject.ast.TypedElement typedElement) {
            for (Predicate<ClassElement> predicate : result.getTypePredicates()) {
                if (!predicate.test(typedElement.getType())) {
                    return false;
                }
            }
        }
        for (Predicate<AnnotationMetadata> predicate : result.getAnnotationPredicates()) {
            if (!predicate.test(element.getAnnotationMetadata())) {
                return false;
            }
        }
        for (Predicate<Set<ElementModifier>> predicate : result.getModifierPredicates()) {
            if (!predicate.test(element.getModifiers())) {
                return false;
            }
        }
        for (Predicate<T> predicate : result.getElementPredicates()) {
            if (!predicate.test(result.getElementType().cast(element))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getPackageName() {
        return NameUtils.getPackageName(getName());
    }

    @Override
    public ClassElement withArrayDimensions(int arrayDimensions) {
        return elementFactory.newClassElement(typeData.withArrayDimensions(arrayDimensions));
    }

    @Override
    public int getArrayDimensions() {
        return typeData.arrayDimensions();
    }

    @Override
    public boolean isPrimitive() {
        return typeData.primitive();
    }

    @Override
    public ClassElement withTypeArguments(Map<String, ClassElement> typeArguments) {
        return this;
    }

    @Override
    public ClassElement withAnnotationMetadata(AnnotationMetadata annotationMetadata) {
        if (classData == null) {
            return new ScalaClassElement(typeData, visitorContext, annotationMetadata);
        }
        return new ScalaClassElement(classData, visitorContext, annotationMetadata);
    }
}
