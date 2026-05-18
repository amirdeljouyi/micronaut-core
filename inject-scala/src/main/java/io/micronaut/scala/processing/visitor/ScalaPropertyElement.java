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
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.annotation.ElementAnnotationMetadata;
import io.micronaut.inject.ast.annotation.MutableAnnotationMetadataDelegate;
import io.micronaut.inject.ast.annotation.PropertyElementAnnotationMetadata;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Scala property element.
 */
public final class ScalaPropertyElement extends AbstractScalaMemberElement implements PropertyElement {

    private final ScalaClassElement declaringType;
    private final ScalaVisitorContext visitorContext;
    private final ScalaPropertyData propertyData;
    private final ElementAnnotationMetadata annotationMetadata;

    ScalaPropertyElement(ScalaClassElement declaringType, ScalaPropertyData propertyData, ScalaVisitorContext visitorContext) {
        this(declaringType, propertyData, visitorContext, null);
    }

    private ScalaPropertyElement(
        ScalaClassElement declaringType,
        ScalaPropertyData propertyData,
        ScalaVisitorContext visitorContext,
        @Nullable
        AnnotationMetadata annotationMetadata) {
        super(
            declaringType,
            propertyData.name(),
            propertyData.nativeType(),
            propertyData.modifiers(),
            annotationMetadata == null ? visitorContext.annotationMetadata(propertyData) : MutableAnnotationMetadata.of(annotationMetadata),
            visitorContext.getScalaAnnotationMetadataBuilder()
        );
        this.declaringType = declaringType;
        this.visitorContext = visitorContext;
        this.propertyData = propertyData;
        this.annotationMetadata = annotationMetadata == null
            ? new PropertyElementAnnotationMetadata(
                this,
                getReadMethod().orElse(null),
                getWriteMethod().orElse(null),
                getField().orElse(null),
                null,
                visitorContext.annotationMetadata(propertyData),
                true
            )
            : new SimpleElementAnnotationMetadata(MutableAnnotationMetadata.of(annotationMetadata), false, visitorContext.getScalaAnnotationMetadataBuilder());
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
        return Optional.of(declaringType.fieldElement(propertyData.field()));
    }

    @Override
    public Optional<MethodElement> getWriteMethod() {
        if (propertyData.writeMethod() == null) {
            return Optional.empty();
        }
        return Optional.of(declaringType.methodElement(propertyData.writeMethod()));
    }

    @Override
    public Optional<MethodElement> getReadMethod() {
        if (propertyData.readMethod() == null) {
            return Optional.empty();
        }
        return Optional.of(declaringType.methodElement(propertyData.readMethod()));
    }

    @Override
    public Optional<? extends MemberElement> getReadMember() {
        if (propertyData.readMethod() == null) {
            return getField();
        }
        return getReadMethod()
            .map(methodElement -> methodElement.withAnnotationMetadata(readMemberAnnotationMetadata()));
    }

    @Override
    public Optional<? extends MemberElement> getWriteMember() {
        if (propertyData.writeMethod() != null) {
            return getWriteMethod()
                .map(methodElement -> methodElement.withAnnotationMetadata(writeMemberAnnotationMetadata()));
        }
        return PropertyElement.super.getWriteMember();
    }

    private AnnotationMetadata readMemberAnnotationMetadata() {
        if (propertyData.readMethod() != null && !propertyData.readMethod().annotations().isEmpty()) {
            return visitorContext.annotationMetadata(propertyData.readMethod());
        }
        if (propertyData.field() != null) {
            return visitorContext.annotationMetadata(propertyData.field());
        }
        return getAnnotationMetadata();
    }

    private AnnotationMetadata writeMemberAnnotationMetadata() {
        if (propertyData.writeMethod() != null && !propertyData.writeMethod().annotations().isEmpty()) {
            return visitorContext.annotationMetadata(propertyData.writeMethod());
        }
        if (propertyData.field() != null) {
            return visitorContext.annotationMetadata(propertyData.field());
        }
        return getAnnotationMetadata();
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return annotationMetadata.getAnnotationMetadata();
    }

    @Override
    protected MutableAnnotationMetadataDelegate<?> getAnnotationMetadataToWrite() {
        return annotationMetadata;
    }

    @Override
    public PropertyElement withAnnotationMetadata(AnnotationMetadata annotationMetadata) {
        return new ScalaPropertyElement(declaringType, propertyData, visitorContext, annotationMetadata);
    }
}
