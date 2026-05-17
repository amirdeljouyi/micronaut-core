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
import java.util.IdentityHashMap;
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
    private final IdentityHashMap<ScalaMethodData, ScalaConstructorElement> constructorElements = new IdentityHashMap<>();
    private final IdentityHashMap<ScalaMethodData, ScalaMethodElement> methodElements = new IdentityHashMap<>();
    private final IdentityHashMap<ScalaFieldData, ScalaFieldElement> fieldElements = new IdentityHashMap<>();
    private final IdentityHashMap<ScalaFieldData, ScalaEnumConstantElement> enumConstantElements = new IdentityHashMap<>();
    private final IdentityHashMap<ScalaPropertyData, ScalaPropertyElement> propertyElements = new IdentityHashMap<>();

    ScalaClassElement(ScalaClassData classData, ScalaVisitorContext visitorContext) {
        this(classData, visitorContext, visitorContext.annotationMetadata(classData));
    }

    ScalaClassElement(ScalaClassData classData, ScalaVisitorContext visitorContext, AnnotationMetadata annotationMetadata) {
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
        if (classData != null) {
            return isAssignableTo(type, classData, Set.of(getName()));
        }
        return isTypeAssignable(type, typeData, Set.of(getName()));
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
            if (candidate.superType() != null && isTypeAssignable(type, candidate.superType(), nextVisited)) {
                return true;
            }
            for (ScalaTypeData interfaceType : candidate.interfaces()) {
                if (isTypeAssignable(type, interfaceType, nextVisited)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Optional<ClassElement> getSuperType() {
        ScalaTypeData superType = classData == null ? typeData.superType() : classData.superType();
        if (superType == null) {
            return Optional.empty();
        }
        return Optional.of(elementFactory.newClassElement(superType));
    }

    @Override
    public Collection<ClassElement> getInterfaces() {
        Collection<ScalaTypeData> interfaces = classData == null ? typeData.interfaces() : classData.interfaces();
        return interfaces.stream()
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
        if (classData == null) {
            return List.of();
        }
        return classData.properties().stream()
            .map(this::propertyElement)
            .map(PropertyElement.class::cast)
            .toList();
    }

    @Override
    public List<PropertyElement> getBeanProperties(PropertyElementQuery propertyElementQuery) {
        if (classData == null) {
            return List.of();
        }
        return classData.properties().stream()
            .map(this::propertyElement)
            .map(PropertyElement.class::cast)
            .toList();
    }

    @Override
    public Optional<MethodElement> getPrimaryConstructor() {
        if (classData == null || classData.constructors().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(constructorElement(classData.constructors().get(0)));
    }

    @Override
    public Optional<MethodElement> getDefaultConstructor() {
        if (classData == null) {
            return Optional.empty();
        }
        return classData.constructors().stream()
            .filter(constructor -> constructor.parameters().isEmpty())
            .findFirst()
            .map(this::constructorElement);
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
            classData.constructors().forEach(constructor -> elements.add(constructorElement(constructor)));
        } else if (elementType == MethodElement.class) {
            classData.methods().forEach(method -> elements.add(methodElement(method)));
        } else if (elementType == FieldElement.class) {
            addFieldElements(result, elements);
        } else if (elementType == PropertyElement.class) {
            classData.properties().forEach(property -> elements.add(propertyElement(property)));
        } else if (elementType == ClassElement.class) {
            elements.addAll(visitorContext.sourceClassElementsEnclosedBy(getName()));
        } else if (elementType == MemberElement.class) {
            addFieldElements(result, elements);
            classData.methods().forEach(method -> elements.add(methodElement(method)));
            if (!result.isExcludePropertyElements()) {
                classData.properties().forEach(property -> elements.add(propertyElement(property)));
            }
        }
        return elements.stream()
            .filter(element -> matches(result, element))
            .map(elementType::cast)
            .toList();
    }

    private <T extends Element> void addFieldElements(ElementQuery.Result<T> result, List<Element> elements) {
        ScalaClassData data = classData;
        if (data == null) {
            return;
        }
        data.fields().forEach(field -> {
            if (field.enumConstant()) {
                if (result.isIncludeEnumConstants() && this instanceof ScalaEnumElement enumElement) {
                    elements.add(enumElement.enumConstantElement(field));
                }
            } else {
                elements.add(fieldElement(field));
            }
        });
    }

    final ScalaConstructorElement constructorElement(ScalaMethodData constructor) {
        return constructorElements.computeIfAbsent(constructor, ignored -> new ScalaConstructorElement(this, constructor, visitorContext));
    }

    final ScalaMethodElement methodElement(ScalaMethodData method) {
        return methodElements.computeIfAbsent(method, ignored -> new ScalaMethodElement(this, method, visitorContext));
    }

    final ScalaFieldElement fieldElement(ScalaFieldData field) {
        return fieldElements.computeIfAbsent(field, ignored -> new ScalaFieldElement(this, field, visitorContext));
    }

    final ScalaEnumConstantElement enumConstantElement(ScalaFieldData field) {
        if (this instanceof ScalaEnumElement enumElement) {
            return enumConstantElements.computeIfAbsent(field, ignored -> new ScalaEnumConstantElement(enumElement, field, visitorContext));
        }
        throw new IllegalStateException("Declaring class must be a ScalaEnumElement");
    }

    final ScalaPropertyElement propertyElement(ScalaPropertyData property) {
        return propertyElements.computeIfAbsent(property, ignored -> new ScalaPropertyElement(this, property, visitorContext));
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

    @Override
    protected Class<?> equalityType() {
        return ScalaClassElement.class;
    }

    @Override
    protected Object equalityKey() {
        return new ClassElementKey(getName(), getArrayDimensions());
    }

    private record ClassElementKey(String name, int arrayDimensions) {
    }
}
