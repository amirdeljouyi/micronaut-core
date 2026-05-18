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
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.AnnotationMetadataHierarchy;
import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.annotation.ElementAnnotationMetadata;
import io.micronaut.inject.ast.annotation.MutableAnnotationMetadataDelegate;
import io.micronaut.inject.ast.annotation.PropertyElementAnnotationMetadata;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scala property element.
 */
public final class ScalaPropertyElement extends AbstractScalaMemberElement implements PropertyElement {

    private static final String VALUE_ANNOTATION = "io.micronaut.context.annotation.Value";

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
                .map(this::writeMember);
        }
        return PropertyElement.super.getWriteMember();
    }

    private MethodElement writeMember(MethodElement methodElement) {
        MethodElement writeMember = methodElement.withAnnotationMetadata(writeMemberAnnotationMetadata());
        return writeMemberParameters(writeMember)
            .map(writeMember::withParameters)
            .orElse(writeMember);
    }

    private Optional<ParameterElement[]> writeMemberParameters(MethodElement methodElement) {
        ParameterElement[] parameters = methodElement.getParameters();
        if (parameters.length != 1 || propertyData.field() == null) {
            return Optional.empty();
        }
        AnnotationMetadata qualifierAnnotationMetadata = qualifierAnnotationMetadata(visitorContext.annotationMetadata(propertyData.field()));
        if (qualifierAnnotationMetadata.isEmpty()) {
            return Optional.empty();
        }
        ParameterElement[] newParameters = parameters.clone();
        ParameterElement parameter = parameters[0];
        AnnotationMetadata annotationMetadata = new AnnotationMetadataHierarchy(
            parameter,
            qualifierAnnotationMetadata
        ).merge();
        newParameters[0] = parameter.withAnnotationMetadata(annotationMetadata);
        return Optional.of(newParameters);
    }

    private AnnotationMetadata qualifierAnnotationMetadata(AnnotationMetadata annotationMetadata) {
        List<AnnotationValue<Annotation>> qualifierValues = annotationMetadata.getAnnotationValuesByStereotype(AnnotationUtil.QUALIFIER);
        if (qualifierValues.isEmpty()) {
            return AnnotationMetadata.EMPTY_METADATA;
        }
        MutableAnnotationMetadata qualifierMetadata = new MutableAnnotationMetadata();
        for (AnnotationValue<Annotation> qualifierValue : qualifierValues) {
            String annotationName = qualifierValue.getAnnotationName();
            if (VALUE_ANNOTATION.equals(annotationName)) {
                continue;
            }
            qualifierMetadata.addDeclaredAnnotation(annotationName, qualifierValue.getValues(), qualifierValue.getRetentionPolicy());
            qualifierMetadata.addDeclaredStereotype(
                List.of(annotationName),
                AnnotationUtil.QUALIFIER,
                Map.of(),
                qualifierValue.getRetentionPolicy()
            );
        }
        return qualifierMetadata.isEmpty() ? AnnotationMetadata.EMPTY_METADATA : qualifierMetadata;
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
