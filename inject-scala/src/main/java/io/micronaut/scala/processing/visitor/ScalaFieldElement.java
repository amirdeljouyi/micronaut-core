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

/**
 * Scala field element.
 */
public final class ScalaFieldElement extends AbstractScalaMemberElement implements FieldElement {

    private final ScalaClassElement declaringType;
    private final ScalaVisitorContext visitorContext;
    private final ScalaFieldData fieldData;

    ScalaFieldElement(ScalaClassElement declaringType, ScalaFieldData fieldData, ScalaVisitorContext visitorContext) {
        this(declaringType, fieldData, visitorContext, visitorContext.getScalaAnnotationMetadataBuilder().buildMetadata(fieldData));
    }

    private ScalaFieldElement(
        ScalaClassElement declaringType,
        ScalaFieldData fieldData,
        ScalaVisitorContext visitorContext,
        AnnotationMetadata annotationMetadata) {
        super(
            declaringType,
            fieldData.name(),
            fieldData.nativeType(),
            fieldData.modifiers(),
            MutableAnnotationMetadata.of(annotationMetadata)
        );
        this.declaringType = declaringType;
        this.visitorContext = visitorContext;
        this.fieldData = fieldData;
    }

    @Override
    public ClassElement getType() {
        return visitorContext.getElementFactory().newClassElement(fieldData.type());
    }

    @Override
    public FieldElement withAnnotationMetadata(AnnotationMetadata annotationMetadata) {
        return new ScalaFieldElement(declaringType, fieldData, visitorContext, annotationMetadata);
    }
}
