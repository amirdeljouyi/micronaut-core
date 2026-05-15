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
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ConstructorElement;
import io.micronaut.inject.ast.ElementFactory;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PrimitiveElement;
import io.micronaut.inject.ast.annotation.ElementAnnotationMetadataFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory for Scala Element API wrappers.
 */
public final class ScalaElementFactory implements ElementFactory<Object, ScalaClassData, ScalaMethodData, ScalaFieldData> {

    private final ScalaVisitorContext visitorContext;

    ScalaElementFactory(ScalaVisitorContext visitorContext) {
        this.visitorContext = visitorContext;
    }

    @Override
    public ClassElement newClassElement(ScalaClassData type, ElementAnnotationMetadataFactory annotationMetadataFactory) {
        return newClassElement(type);
    }

    @Override
    public ClassElement newSourceClassElement(ScalaClassData type, ElementAnnotationMetadataFactory elementAnnotationMetadataFactory) {
        return newClassElement(type);
    }

    ClassElement newClassElement(ScalaClassData type) {
        return visitorContext.sourceClassElement(type.name())
            .orElseGet(() -> new ScalaClassElement(type, visitorContext));
    }

    ClassElement newClassElement(ScalaTypeData type) {
        if (type.primitive()) {
            return switch (type.name()) {
                case "boolean" -> PrimitiveElement.BOOLEAN.withArrayDimensions(type.arrayDimensions());
                case "byte" -> PrimitiveElement.BYTE.withArrayDimensions(type.arrayDimensions());
                case "char" -> PrimitiveElement.CHAR.withArrayDimensions(type.arrayDimensions());
                case "double" -> PrimitiveElement.DOUBLE.withArrayDimensions(type.arrayDimensions());
                case "float" -> PrimitiveElement.FLOAT.withArrayDimensions(type.arrayDimensions());
                case "int" -> PrimitiveElement.INT.withArrayDimensions(type.arrayDimensions());
                case "long" -> PrimitiveElement.LONG.withArrayDimensions(type.arrayDimensions());
                case "short" -> PrimitiveElement.SHORT.withArrayDimensions(type.arrayDimensions());
                case "void" -> PrimitiveElement.VOID.withArrayDimensions(type.arrayDimensions());
                default -> new ScalaClassElement(type, visitorContext, AnnotationMetadata.EMPTY_METADATA);
            };
        }
        return visitorContext.sourceClassElement(type.name())
            .map(classElement -> type.arrayDimensions() == 0 ? classElement : classElement.withArrayDimensions(type.arrayDimensions()))
            .orElseGet(() -> new ScalaClassElement(type, visitorContext, AnnotationMetadata.EMPTY_METADATA));
    }

    Map<String, ClassElement> typeArguments(ScalaTypeData type) {
        if (type.typeArguments().isEmpty()) {
            return Map.of();
        }
        Map<String, ClassElement> converted = new LinkedHashMap<>();
        type.typeArguments().forEach((name, typeData) -> converted.put(name, newClassElement(typeData)));
        return converted;
    }

    @Override
    public MethodElement newSourceMethodElement(ClassElement owningClass, ScalaMethodData method, ElementAnnotationMetadataFactory elementAnnotationMetadataFactory) {
        return newMethodElement(owningClass, method, elementAnnotationMetadataFactory);
    }

    @Override
    public MethodElement newMethodElement(ClassElement owningClass, ScalaMethodData method, ElementAnnotationMetadataFactory elementAnnotationMetadataFactory) {
        return new ScalaMethodElement((ScalaClassElement) owningClass, method, visitorContext);
    }

    @Override
    public ConstructorElement newConstructorElement(ClassElement owningClass, ScalaMethodData constructor, ElementAnnotationMetadataFactory elementAnnotationMetadataFactory) {
        return new ScalaConstructorElement((ScalaClassElement) owningClass, constructor, visitorContext);
    }

    @Override
    public io.micronaut.inject.ast.EnumConstantElement newEnumConstantElement(ClassElement owningClass, ScalaFieldData enumConstant, ElementAnnotationMetadataFactory elementAnnotationMetadataFactory) {
        throw new UnsupportedOperationException("Scala enum constants are not implemented yet");
    }

    @Override
    public FieldElement newFieldElement(ClassElement owningClass, ScalaFieldData field, ElementAnnotationMetadataFactory elementAnnotationMetadataFactory) {
        return new ScalaFieldElement((ScalaClassElement) owningClass, field, visitorContext);
    }
}
