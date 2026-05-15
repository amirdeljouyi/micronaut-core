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
import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;

import java.util.Optional;

/**
 * Scala property element.
 */
public final class ScalaPropertyElement extends AbstractScalaMemberElement implements PropertyElement {

    private final ScalaClassElement declaringType;
    private final ScalaVisitorContext visitorContext;
    private final ScalaPropertyData propertyData;

    ScalaPropertyElement(ScalaClassElement declaringType, ScalaPropertyData propertyData, ScalaVisitorContext visitorContext) {
        this(declaringType, propertyData, visitorContext, visitorContext.getScalaAnnotationMetadataBuilder().buildMetadata(propertyData));
    }

    private ScalaPropertyElement(
        ScalaClassElement declaringType,
        ScalaPropertyData propertyData,
        ScalaVisitorContext visitorContext,
        AnnotationMetadata annotationMetadata) {
        super(
            declaringType,
            propertyData.name(),
            propertyData.nativeType(),
            propertyData.modifiers(),
            MutableAnnotationMetadata.of(annotationMetadata)
        );
        this.declaringType = declaringType;
        this.visitorContext = visitorContext;
        this.propertyData = propertyData;
    }

    @Override
    public ClassElement getType() {
        return visitorContext.getElementFactory().newClassElement(propertyData.type());
    }

    @Override
    public Optional<FieldElement> getField() {
        if (propertyData.field() == null) {
            return Optional.empty();
        }
        return Optional.of(new ScalaFieldElement(declaringType, propertyData.field(), visitorContext));
    }

    @Override
    public Optional<MethodElement> getWriteMethod() {
        if (propertyData.writeMethod() == null) {
            return Optional.empty();
        }
        return Optional.of(new ScalaMethodElement(declaringType, propertyData.writeMethod(), visitorContext));
    }

    @Override
    public Optional<MethodElement> getReadMethod() {
        if (propertyData.readMethod() == null) {
            return Optional.empty();
        }
        return Optional.of(new ScalaMethodElement(declaringType, propertyData.readMethod(), visitorContext));
    }

    @Override
    public PropertyElement withAnnotationMetadata(AnnotationMetadata annotationMetadata) {
        return new ScalaPropertyElement(declaringType, propertyData, visitorContext, annotationMetadata);
    }
}
